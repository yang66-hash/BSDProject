package com.yang.apm.springplugin.services.trace;

import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.services.MetricsService;
import com.yang.apm.springplugin.services.TraceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MetricsServiceTest {

    @Autowired
    private MetricsService metricsService;
    @Autowired
    private TraceService traceService;

    @Test
    public void test() {

        List<SvcMetricsRes> internalMetrics = metricsService.getInternalMetrics("2025-04-29 16:05:00", 60);
        System.out.println(internalMetrics);
//        redisAsyncService.saveT2Redis(internalMetrics, ResType.INTERNAL_METRICS.name());
    }

    @Test
    public void testTrace() {
        Integer interval = 1;
        List<SvcExternalMetricsRes> traces0 = traceService.getMetricsInTraces("2025-04-29 16:05:00",interval);
        List<SvcExternalMetricsRes> traces1 = traceService.getMetricsInTraces("2025-04-29 16:04:00",interval);
        List<SvcExternalMetricsRes> traces2 = traceService.getMetricsInTraces("2025-04-29 16:03:00",interval);
        List<SvcExternalMetricsRes> traces3 = traceService.getMetricsInTraces("2025-04-29 16:02:00",interval);
        List<SvcExternalMetricsRes> traces4 = traceService.getMetricsInTraces("2025-04-29 16:01:00",interval);

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