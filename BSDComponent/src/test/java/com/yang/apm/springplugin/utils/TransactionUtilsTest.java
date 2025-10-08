package com.yang.apm.springplugin.utils;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.alibaba.fastjson.JSON;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcTransRes;
import com.yang.apm.springplugin.pojo.traces.TraceServiceInfo;
import com.yang.apm.springplugin.services.datacollector.ESQueryService;
import com.yang.apm.springplugin.services.datacollector.metricscollector.TraceService;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * TransactionUtils.analyzeTransaction4Trace 方法的测试类
 */

@SpringBootTest
@Slf4j
class TransactionUtilsTest {

    @Autowired
    private TraceService traceService;

    @Autowired
    private ESQueryService  ESQueryService;
    @Test
    public void testTrace() {
        Integer interval = 300;
        List<SvcExternalMetricsRes> traces0 = traceService.getMetricsInTraces("2025-09-12 15:43:43",interval);

    }

    @Test
    public void testTrace2() throws ParseException, IOException {
        String endTimeString = "2025-09-12 15:43:43";
        Integer interval = 300;
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date endTime = dateTimeFormatter.parse(endTimeString);
        Date startTime = TimeUtil.calculateStartTime(endTime, interval);
        log.info("fetching traces from {} to {}",startTime,endTime);

        Query rangeQuery = ElasticSearchQueryUtil.createTimeRangeQuery("@timestamp", startTime, endTime);

        SearchRequest request = new SearchRequest.Builder()
            .index(".ds-traces-apm*")
            .query(rangeQuery).timeout("10s")
            .size(5000)
            .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
            .build();
        List<TraceServiceInfo> traceServiceInfoList = ESQueryService.executeSearch(request, TraceServiceInfo.class);

        log.info("收集到的traces条数:{}", traceServiceInfoList.size());
        log.info("收集到的traces:{}", traceServiceInfoList);
        //存储每一条链路信息
        Map<String, List<TraceServiceInfo>> stringListMap = TransactionUtils.parseData2TraceMap(traceServiceInfoList);

        List<SvcTransRes> list = TransactionUtils.analyzeTransaction4Trace(stringListMap, endTime,
            startTime, interval);

        // 将 List<SvcTransRes> 转换为 JSON 格式并输出到控制台
        String jsonOutput = JSON.toJSONString(list, true); // true 参数表示格式化输出
        System.out.println("=== SvcTransRes List JSON 输出 ===");
        System.out.println(jsonOutput);
        System.out.println("=== JSON 输出结束 ===");
        
        // 也可以输出到日志
        log.info("SvcTransRes List JSON: {}", jsonOutput);

    }
}
