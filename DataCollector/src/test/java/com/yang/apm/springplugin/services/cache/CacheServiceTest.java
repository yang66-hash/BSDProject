package com.yang.apm.springplugin.services.cache;

import com.yang.apm.springplugin.services.CacheService;
import com.yang.apm.springplugin.services.TraceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = com.yang.apm.springplugin.DataCollectApplication.class)
class CacheServiceTest {
    
    @Autowired
    private TraceService traceService;
    
    @Autowired
    private CacheService cacheService;
    
    @Test
    public void test(){
        Integer interval = 1; 
//        List<SvcExternalMetricsRes> traces0 = traceService.getMetricsInTraces("2025-04-29 16:05:00",interval);
//        List<SvcExternalMetricsRes> traces1 = traceService.getMetricsInTraces("2025-04-29 16:04:00",interval);
//        List<SvcExternalMetricsRes> traces2 = traceService.getMetricsInTraces("2025-04-29 16:03:00",interval);
//        List<SvcExternalMetricsRes> traces3 = traceService.getMetricsInTraces("2025-04-29 16:02:00",interval);
//        List<SvcExternalMetricsRes> traces4 = traceService.getMetricsInTraces("2025-04-29 16:01:00",interval);
//        cacheService.saveT2Cache(traces0, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:05:00");
//        cacheService.saveT2Cache(traces1, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:04:00");
//        cacheService.saveT2Cache(traces2, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:03:00");
//        cacheService.saveT2Cache(traces3, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:02:00");
//        cacheService.saveT2Cache(traces4, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:01:00");
    }
}