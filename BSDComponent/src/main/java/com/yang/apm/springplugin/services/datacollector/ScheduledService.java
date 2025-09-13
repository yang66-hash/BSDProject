package com.yang.apm.springplugin.services.datacollector;

import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.indexmapping.BusinessMetricsMappingStrategy;
import com.yang.apm.springplugin.indexmapping.CircularDependencyTracesMappingStrategy;
import com.yang.apm.springplugin.indexmapping.ExternalMetricsMappingStrategy;
import com.yang.apm.springplugin.indexmapping.InternalMetricsMappingStrategy;
import com.yang.apm.springplugin.manager.datacollector.ElasticsearchClientManager;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.services.datacollector.metricscollector.BusinessMetricsService;
import com.yang.apm.springplugin.services.datacollector.metricscollector.MetricsService;
import com.yang.apm.springplugin.services.datacollector.metricscollector.TraceService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import com.yang.apm.springplugin.utils.IndexUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * 负责从es中拉取每min的数据 同时计算指定时间窗口下的历史数据的计算
 */
@Service
@Slf4j
public class ScheduledService {

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;

    @Autowired
    private TraceService traceService;
    @Autowired
    private MetricsService metricsService;
    @Autowired
    private BusinessMetricsService businessMetricsService;
    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private CalculateService calculateService;
    // 选择PostConstruct，在完成构造函数注入和依赖注入之后、Bean被使用之前执行相关索引文件是否创建的检测
    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private MetricsBufferService metricsBufferService;

    @PostConstruct
    public void init() {
        //检测所有的索引文件是否存在
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        Integer timeWindow = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);

        // External Metrics 索引创建
        String externalMetricsIndex = IndexUtil.getExternalMetricsIndex(interval);
        String externalMetricsHistoryIndex = IndexUtil.getExternalMetricsIndex(timeWindow);
        if (!elasticsearchClientManager.isIndexExisted(externalMetricsIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(externalMetricsIndex,new ExternalMetricsMappingStrategy());
        }
        System.out.println("=============================================");
        if (!elasticsearchClientManager.isIndexExisted(externalMetricsHistoryIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(externalMetricsHistoryIndex,new ExternalMetricsMappingStrategy());
        }

        // Internal Metrics 索引创建
        String internalMetricsIndex = IndexUtil.getInternalMetricsIndex(interval);
        if (!elasticsearchClientManager.isIndexExisted(internalMetricsIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(internalMetricsIndex,new InternalMetricsMappingStrategy());
        }

        // Business Metrics 索引创建
        String businessMetricsIndex = IndexUtil.getBusinessMetricsIndex(interval);
        if (!elasticsearchClientManager.isIndexExisted(businessMetricsIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(businessMetricsIndex,new BusinessMetricsMappingStrategy());
        }

        // Circular Dependency Traces 索引创建
        String circularDependencyIndex = IndexUtil.getCircularDependencyTracesIndex();
        if (!elasticsearchClientManager.isIndexExisted(circularDependencyIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(circularDependencyIndex, new CircularDependencyTracesMappingStrategy());
        }

        scheduledNext();
    }

    //定时拉取数据 每隔interval的时间拉取一次数据
    //用interval作为数据拉取的时间间隔，同时也是历史平均数据计算的时间间隔
    private void scheduledNext(){
        log.info("scheduled next task to collect and  calculate data");
        //todo: 修改测试
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        taskScheduler.schedule(this::getMetricsScheduled, new Date(System.currentTimeMillis() + interval*1000));
    }

    //历史数据的计算
    public void getMetricsScheduled(){
        try {
            String externalMetricsIndex = IndexUtil.getExternalMetricsIndex(intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY));
            String internalMetricsIndex = IndexUtil.getInternalMetricsIndex(intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY));
            String businessMetricsIndex = IndexUtil.getBusinessMetricsIndex(intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY));
            scheduledTimeIncr();
            int interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();

            String formatTime = simpleDateFormat.format(date);
            cacheService.setCurTime(date);
            //todo: 记得删这个测试数据
//            formatTime = "2025-09-12 15:43:43";
//            interval = 300;
            List<SvcExternalMetricsRes> metricsInTraces = traceService.getMetricsInTraces(formatTime, interval);
            List<SvcMetricsRes> internalMetrics = metricsService.getInternalMetrics(formatTime, interval);
            List<BusinessMetricsRes> businessMetrics = businessMetricsService.getBusinessMetrics(
                formatTime, interval);
            System.out.println("=========================================");
            System.out.println(metricsInTraces.toString());
            metricsBufferService.addList2Index(metricsInTraces, externalMetricsIndex);
            metricsBufferService.addList2Index(internalMetrics, internalMetricsIndex);
            metricsBufferService.addList2Index(businessMetrics, businessMetricsIndex);

            log.info("收集到的指标信息条数{}", metricsInTraces.size());
            log.info("收集到的指标信息：{}", metricsInTraces);
            cacheService.saveT2Cache(metricsInTraces, ResType.EXTERNAL_METRICS.name());
            cacheService.saveT2Cache(internalMetrics, ResType.INTERNAL_METRICS.name());
            cacheService.saveT2Cache(businessMetrics, ResType.BUSINESS_METRICS.name());
//            log.error(cacheService.getKeySetInSvcIvlLevel(ResType.EXTERNAL_METRICS.name()).toString());
//            log.error(cacheService.getKeySetInSvcIvlLevel(ResType.INTERNAL_METRICS.name()).toString());
//            log.error(cacheService.getKeySetInSvcIvlLevel(ResType.BUSINESS_METRICS.name()).toString());

            //计算window时间范围内的数据平均值 从缓存中加载所有的external metrics 回存到cache中
            calculateService.calculateAvg4ExternalMetrics();
            //计算window时间范围内的，internal metrics的指标平均值
            calculateService.calculate4InternalMetrics();
            log.info("getMetricsScheduled定时调用任务执行完成");
        }finally {
            scheduledNext();
        }

    }

    /**
     * 计算实际时间窗口的大小
     */
    private void scheduledTimeIncr(){
        //添加一个cache中是否为空的判断，如果是空，直接将INCREMENT_WINDOW_OF_DYNAMIC_KEY设置为0
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
        if (cacheService.isCacheEmpty()){
            intervalWindowMappingService.setValueByName(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY, 0);
        }
        Integer inrWindow = intervalWindowMappingService.getValueByName(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY);
        int min = Math.min(inrWindow + interval, window);
        intervalWindowMappingService.setValueByName(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY, min);
        log.info("INCREMENT_WINDOW_OF_DYNAMIC_KEY incremented, {}", min);
    }

}
