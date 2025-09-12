package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.N1QueriesContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.N1QueriesItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.services.ESService;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import com.yang.apm.springplugin.utils.IndexUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class N1QueriesService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem n1QueriesDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.N1QUERIES.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    N1QueriesContext n1QueriesContext = n1QueriesDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(n1QueriesContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private N1QueriesContext n1QueriesDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    N1QueriesContext n1QueriesContext = new N1QueriesContext();
    Map<String, N1QueriesItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      N1QueriesItem n1QueriesItem = new N1QueriesItem();
      
      // 模拟N+1查询相关数据
      int totalQueryCount = 50 + (int)(Math.random() * 200); // 50-250次查询
      double avgQueriesPerOperation = 2.0 + Math.random() * 10.0; // 2-12次查询每操作
      int duplicateQueryCount = (int)(totalQueryCount * (0.1 + Math.random() * 0.3)); // 10%-40%重复查询
      double batchOptimizationRate = Math.random() * 0.6; // 0%-60%批量优化率
      double queryEfficiencyScore = 50 + Math.random() * 50; // 50-100效率评分
      double ormPerformanceMetric = 0.3 + Math.random() * 0.7; // 0.3-1.0性能指标
      
      n1QueriesItem.setTotalQueryCount(totalQueryCount);
      n1QueriesItem.setAvgQueriesPerOperation(avgQueriesPerOperation);
      n1QueriesItem.setDuplicateQueryCount(duplicateQueryCount);
      n1QueriesItem.setBatchOptimizationRate(batchOptimizationRate);
      n1QueriesItem.setQueryEfficiencyScore(queryEfficiencyScore);
      n1QueriesItem.setOrmPerformanceMetric(ormPerformanceMetric);

      boolean isInstanceN1Queries = isInstanceN1Queries(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), n1QueriesItem);
      if (isInstanceN1Queries) {
        flag = true;
      }
      n1QueriesItem.setStatus(isInstanceN1Queries);
      map.put(entry.getKey(), n1QueriesItem);
    }

    n1QueriesContext.setStatus(flag);
    log.info("map: {}",map);
    n1QueriesContext.setInstanceStatus(map);
    n1QueriesContext.setMetricsInterval(interval);
    return n1QueriesContext;
  }

  private boolean isInstanceN1Queries(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, N1QueriesItem n1QueriesItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    double highAvgQueriesThreshold = 8.0; // 平均查询次数阈值
    double highDuplicateRatioThreshold = 0.25; // 重复查询比例阈值25%
    double lowBatchOptimizationThreshold = 0.3; // 批量优化率阈值30%
    double lowEfficiencyScoreThreshold = 60.0; // 效率评分阈值
    
    boolean highAvgQueries = n1QueriesItem.getAvgQueriesPerOperation() > highAvgQueriesThreshold;
    double duplicateRatio = (double) n1QueriesItem.getDuplicateQueryCount() / n1QueriesItem.getTotalQueryCount();
    boolean highDuplicateRatio = duplicateRatio > highDuplicateRatioThreshold;
    boolean lowBatchOptimization = n1QueriesItem.getBatchOptimizationRate() < lowBatchOptimizationThreshold;
    boolean lowEfficiencyScore = n1QueriesItem.getQueryEfficiencyScore() < lowEfficiencyScoreThreshold;
    
    int conditionCount = 0;
    if (highAvgQueries) conditionCount++;
    if (highDuplicateRatio) conditionCount++;
    if (lowBatchOptimization) conditionCount++;
    if (lowEfficiencyScore) conditionCount++;
    
    return conditionCount >= 3;
  }
} 