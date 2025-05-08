package services.redis;

import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.services.RedisAsyncService;
import com.yang.apm.springplugin.services.dynamicdetect.MetricsService;
import com.yang.apm.springplugin.services.dynamicdetect.TraceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Set;

@SpringBootTest
class RedisAsyncServiceTest {


    @Autowired
    RedisAsyncService redisAsyncService;

    @Autowired
    private MetricsService metricsService;


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private TraceService traceService;

    @Test
    public void test() {

        List<SvcMetricsRes> internalMetrics = metricsService.getInternalMetrics("2025-04-29 16:05:00", 5);
//        redisAsyncService.saveT2Redis(internalMetrics, ResType.INTERNAL_METRICS.name());
    }

    @Test
    public void testTrace() {
        Integer interval = redisAsyncService.searchInterval();
        List<SvcExternalMetricsRes> traces0 = traceService.getMetricsInTraces("2025-04-29 16:05:00",interval);
        List<SvcExternalMetricsRes> traces1 = traceService.getMetricsInTraces("2025-04-29 16:04:00",interval);
        List<SvcExternalMetricsRes> traces2 = traceService.getMetricsInTraces("2025-04-29 16:03:00",interval);
        List<SvcExternalMetricsRes> traces3 = traceService.getMetricsInTraces("2025-04-29 16:02:00",interval);
        List<SvcExternalMetricsRes> traces4 = traceService.getMetricsInTraces("2025-04-29 16:01:00",interval);
        redisAsyncService.saveT2Redis(traces0, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:05:00");
        redisAsyncService.saveT2Redis(traces1, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:04:00");
        redisAsyncService.saveT2Redis(traces2, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:03:00");
        redisAsyncService.saveT2Redis(traces3, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:02:00");
        redisAsyncService.saveT2Redis(traces4, ResType.EXTERNAL_METRICS.name(),"2025-04-29 16:01:00");
    }

    @Test
    public void testTraceByName() {

//        List<SvcTransRes> traces = traceService.getTracesByServiceName("2025-04-29 16:05:00",5,"cloud-user-service");
//        redisAsyncService.saveT2Redis(traces, ResType.TRACE.name());
    }



    @Test
    public void matchedPatternSetTest(){

        String traceKeyPattern = "TRACE:5:cloud-user-service*";
        Set<String> stringSet = redisAsyncService.matchedPatternSet(traceKeyPattern);
        System.out.println(stringSet);
    }

//
//    @Test
//    public void incrTest(){
//
//        redisAsyncService.incrUntilTen();
//    }
}