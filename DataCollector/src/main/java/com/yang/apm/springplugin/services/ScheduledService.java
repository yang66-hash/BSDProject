package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import com.yang.apm.springplugin.manager.indexmapping.ExternalMetricsMappingStrategy;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import com.yang.apm.springplugin.utils.IndexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private PersistentIntegerCacheService persistentIntegerCacheService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private CalculateService calculateService;
    // 选择PostConstruct，在完成构造函数注入和依赖注入之后、Bean被使用之前执行相关索引文件是否创建的检测
    @Autowired
    private TaskScheduler taskScheduler;
    @PostConstruct
    public void init() {
        //检测所有的索引文件是否存在
        Integer interval = persistentIntegerCacheService.get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        Integer timeWindow = persistentIntegerCacheService.get(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);

        String externalMetricsIndex = IndexUtil.getExternalMetricsIndex(interval);
        String externalMetricsHistoryIndex = IndexUtil.getExternalMetricsIndex(timeWindow);
        if (!elasticsearchClientManager.isIndexExisted(externalMetricsIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(externalMetricsIndex,new ExternalMetricsMappingStrategy());
        }
        if (!elasticsearchClientManager.isIndexExisted(externalMetricsHistoryIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(externalMetricsHistoryIndex,new ExternalMetricsMappingStrategy());
        }
        scheduledNext();
    }

    //定时拉取数据 每隔interval的时间拉取一次数据
    //用interval作为数据拉取的时间间隔，同时也是历史平均数据计算的时间间隔
    private void scheduledNext(){
        log.info("scheduled next task to collect and  calculate data");
        Integer interval = persistentIntegerCacheService.get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        taskScheduler.schedule(this::getMetricsScheduled, new Date(System.currentTimeMillis() + interval*1000));
    }

    //历史数据的计算
    public void getMetricsScheduled(){
        try {
            scheduledTimeIncr();
            int interval = persistentIntegerCacheService.get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formatTime = simpleDateFormat.format(new Date());
//            String  formatTime = "2025-04-29 16:05:00";
            List<SvcExternalMetricsRes> metricsInTraces = traceService.getMetricsInTraces(formatTime, interval);

            log.info("收集到的指标信息条数{}", metricsInTraces.size());
            log.info("收集到的指标信息：{}", metricsInTraces);
            cacheService.saveT2Cache(metricsInTraces, ResType.EXTERNAL_METRICS.name());
            //计算window时间范围内的数据平均值 从缓存中加载所有的external metrics计算并发送到es中
            calculateService.calculateAvg4ExternalMetrics();
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
        Integer interval = persistentIntegerCacheService.get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        Integer window = persistentIntegerCacheService.get(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
        if (cacheService.isCacheEmpty()){
            persistentIntegerCacheService.put(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY, 0);
        }
        Integer inrWindow = persistentIntegerCacheService.get(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY);
        int min = Math.min(inrWindow + interval, window);
        persistentIntegerCacheService.put(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY, min);
        log.info("INCREMENT_WINDOW_OF_DYNAMIC_KEY incremented, {}", min);
    }

}
