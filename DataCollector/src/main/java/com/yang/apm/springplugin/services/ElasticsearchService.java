package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.services.dynamicdetect.BusinessService;
import com.yang.apm.springplugin.services.dynamicdetect.MetricsService;
import com.yang.apm.springplugin.services.dynamicdetect.TraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ElasticsearchService {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private TraceService traceService;

    @Autowired
    private CalculateService calculateService;

    @Autowired
    private RedisAsyncService redisAsyncService;

//    每隔1min收集一次external metrics、internal metrics、business metrics||| traces的数据当需要统计时才统计
//    @Scheduled(fixedRate = 60000)
//    @Async
//    public void pullDataFromEs(){
//        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Date curTime = new Date();
//        String formattedDate = dateTimeFormatter.format(curTime);
////        String formattedDate = "2025-04-29 16:05:00";
//        List<BusinessMetricsRes> businessMetrics = businessService.getBusinessMetrics(formattedDate, 1);
//        List<SvcMetricsRes> internalMetrics = metricsService.getInternalMetrics(formattedDate, 1);
//        List<SvcExternalMetricsRes> externalMetricsRes = traceService.getMetricsInTraces(formattedDate, 1);
//        //异步发送数据到redis中
//        redisAsyncService.saveT2Redis(businessMetrics, ResType.BUSINESS_METRICS.name(),formattedDate);
//        redisAsyncService.saveT2Redis(internalMetrics,ResType.INTERNAL_METRICS.name(),formattedDate);
//        redisAsyncService.saveT2Redis(externalMetricsRes,ResType.EXTERNAL_METRICS.name(),formattedDate);
//        redisAsyncService.incrUntilTen();
//
//        //将过往的十分钟的数据进行一次计算，统计总的平均值，并作为结果存储到redis中，同时发送到elasticsearch中
//        calculateService.calculateAvg4ExternalMetrics();
//    }


}
