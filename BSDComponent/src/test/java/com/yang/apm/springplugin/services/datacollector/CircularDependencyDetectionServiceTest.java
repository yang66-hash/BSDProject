package com.yang.apm.springplugin.services.datacollector;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.alibaba.fastjson.JSON;
import com.yang.apm.springplugin.pojo.result.SvcTransRes;
import com.yang.apm.springplugin.pojo.traces.TraceServiceInfo;
import com.yang.apm.springplugin.utils.ElasticSearchQueryUtil;
import com.yang.apm.springplugin.utils.TimeUtil;
import com.yang.apm.springplugin.utils.TransactionUtils;
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

@SpringBootTest
@Slf4j
class CircularDependencyDetectionServiceTest {

  @Autowired
  private ESQueryService esQueryService;
  @Autowired
  private CircularDependencyDetectionService circularDependencyDetectionService;
  @Test
  public void detectCircularDependencyAsync() throws ParseException, IOException {
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
    List<TraceServiceInfo> traceServiceInfoList = esQueryService.executeSearch(request, TraceServiceInfo.class);

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

    circularDependencyDetectionService.detectCircularDependencyAsync(stringListMap,endTime,startTime,interval);
  }

  /**
   * 测试链路统计功能
   */
  @Test
  public void testChainMetricsCalculation() throws ParseException, IOException {
    String endTimeString = "2025-09-12 15:43:43";
    Integer interval = 300;
    SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    Date endTime = dateTimeFormatter.parse(endTimeString);
    Date startTime = TimeUtil.calculateStartTime(endTime, interval);
    log.info("测试链路统计功能 - 获取时间范围: {} 到 {}", startTime, endTime);

    Query rangeQuery = ElasticSearchQueryUtil.createTimeRangeQuery("@timestamp", startTime, endTime);

    SearchRequest request = new SearchRequest.Builder()
        .index(".ds-traces-apm*")
        .query(rangeQuery).timeout("10s")
        .size(100) // 减少数据量用于测试
        .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
        .build();
    List<TraceServiceInfo> traceServiceInfoList = esQueryService.executeSearch(request, TraceServiceInfo.class);

    log.info("收集到的traces条数: {}", traceServiceInfoList.size());
    
    if (traceServiceInfoList.isEmpty()) {
      log.warn("没有找到测试数据，跳过统计功能测试");
      return;
    }

    // 解析数据并分析链路
    Map<String, List<TraceServiceInfo>> stringListMap = TransactionUtils.parseData2TraceMap(traceServiceInfoList);
    List<SvcTransRes> list = TransactionUtils.analyzeTransaction4Trace(stringListMap, endTime, startTime, interval);

    log.info("分析得到的链路数量: {}", list.size());

    // 验证统计功能
    for (SvcTransRes svcTransRes : list) {
      log.info("=== 链路统计信息 ===");
      log.info("服务名: {}", svcTransRes.getServiceName());
      log.info("Pod名: {}", svcTransRes.getPodName());
      log.info("链路深度: {}", svcTransRes.getChainDepth());
      log.info("SQL请求次数: {}", svcTransRes.getSqlCount());
      log.info("总执行时间: {} μs", svcTransRes.getTotalExecTime());
      log.info("TraceId: {}", svcTransRes.getRequestChain() != null ? svcTransRes.getRequestChain().getTraceId() : "unknown");
      
      // 验证数据合理性
      assert svcTransRes.getChainDepth() != null && svcTransRes.getChainDepth() >= 0 : "链路深度应该 >= 0";
      assert svcTransRes.getSqlCount() != null && svcTransRes.getSqlCount() >= 0 : "SQL次数应该 >= 0";
      assert svcTransRes.getTotalExecTime() != null && svcTransRes.getTotalExecTime() >= 0.0 : "总执行时间应该 >= 0";
      
      log.info("=== 统计信息验证通过 ===");
    }

    log.info("链路统计功能测试完成，共验证了 {} 条链路", list.size());
  }

}