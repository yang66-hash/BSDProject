package com.yang.apm.springplugin.services.datacollector.trace;

import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;

import com.yang.apm.springplugin.services.datacollector.metricscollector.MetricsService;
import com.yang.apm.springplugin.services.datacollector.metricscollector.TraceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MetricsServiceTest {

    @Autowired
    private MetricsService metricsService;
    @Autowired
    private TraceService traceService;

    @Test
    public void test() {

        List<SvcMetricsRes> internalMetrics = metricsService.getInternalMetrics("2025-09-12 07:49:46", 60);
        System.out.println(internalMetrics);
//        redisAsyncService.saveT2Redis(internalMetrics, ResType.INTERNAL_METRICS.name());
    }

    @Test
    public void testTrace() {
        Integer interval = 60;
        List<SvcExternalMetricsRes> traces0 = traceService.getMetricsInTraces("2025-09-12 07:49:46",interval);
        List<SvcExternalMetricsRes> traces1 = traceService.getMetricsInTraces("2025-09-12 07:48:46",interval);
        List<SvcExternalMetricsRes> traces2 = traceService.getMetricsInTraces("2025-09-12 07:47:46",interval);
        List<SvcExternalMetricsRes> traces3 = traceService.getMetricsInTraces("2025-09-12 07:46:46",interval);
        List<SvcExternalMetricsRes> traces4 = traceService.getMetricsInTraces("2025-09-12 07:45:46",interval);

    }

    @Test
    public void testTraceByName() {

//        List<SvcTransRes> traces = traceService.getTracesByServiceName("2025-04-29 16:05:00",5,"cloud-user-service");
//        redisAsyncService.saveT2Redis(traces, ResType.TRACE.name());
    }



    @Test
    public void matchedPatternSetTest(){

    }

//
//    @Test
//    public void incrTest(){
//
//        redisAsyncService.incrUntilTen();
//    }
}