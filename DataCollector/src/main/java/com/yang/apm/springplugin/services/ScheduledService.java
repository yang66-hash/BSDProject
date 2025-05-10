package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

    //定时拉取数据
    public void getMetricsScheduled(){
        int interval = persistentIntegerCacheService.get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String formatTime = simpleDateFormat.format(new Date());
        String  formatTime = "2025-04-29 16:05:00";
        List<SvcExternalMetricsRes> metricsInTraces = traceService.getMetricsInTraces(formatTime, interval);
        log.info("收集到的指标信息条数{}", metricsInTraces.size());
        log.info("收集到的指标信息：{}", metricsInTraces);
        cacheService.saveT2Cache(metricsInTraces, ResType.EXTERNAL_METRICS.name());
        log.info("getMetricsScheduled定时调用任务执行完成");
    }


}
