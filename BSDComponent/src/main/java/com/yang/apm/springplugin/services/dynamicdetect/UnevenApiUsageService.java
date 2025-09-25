package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.UnevenApiUsageContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.UnevenApiUsageItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.datacollector.CacheService;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UnevenApiUsageService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;
  @Autowired
  private CacheService cacheService;

  /**
   * API使用不均检测 - 基于instanceAPICallNumMap的真实数据检测
   * @param requestItem 传入服务名称
   * @return 返回封装好的检测结果
   */
  public DetectionResItem unevenApiUsageDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.UNEVEN_API_USAGE.getValue());
    //相关异味信息
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    //使用CacheService获取时间窗口内的平均数据
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceIntervalAVG = requestItem.getServiceName();
    
    //获取时间窗口内的AVGExternalMetrics数据，其中包含instanceAPICallNumMap
    Map<String, SvcExternalMetricsRes> avgExternalMetricsMap = cacheService.getAvgDataInServiceLevel(
        ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);

    //执行Uneven API Usage检测
    UnevenApiUsageContext unevenApiUsageContext = unevenApiUsageDetectDetail(avgExternalMetricsMap, interval);
    addNew2ResItem(unevenApiUsageContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Uneven API Usage的具体检测逻辑
   * 基于整个微服务的聚合API调用数据进行分布不均衡检测
   */
  private UnevenApiUsageContext unevenApiUsageDetectDetail(
      Map<String, SvcExternalMetricsRes> avgExternalMetrics, Integer interval) {
    UnevenApiUsageContext unevenApiUsageContext = new UnevenApiUsageContext();
    
    // 聚合所有实例的API调用数据到微服务级别
    AggregationResult aggregationResult = aggregateApiCallData(avgExternalMetrics);
    Map<String, Integer> aggregatedApiCallMap = aggregationResult.getAggregatedApiCallMap();
    List<String> validInstanceNames = aggregationResult.getValidInstanceNames();
    
    if (aggregatedApiCallMap.isEmpty()) {
      log.warn("未找到任何API调用数据，跳过API使用不均衡检测");
      unevenApiUsageContext.setStatus(false);
      unevenApiUsageContext.setMicroserviceApiUsage(null);
      unevenApiUsageContext.setMetricsInterval(interval);
      return unevenApiUsageContext;
    }
    
    // 对整个微服务进行API使用不均衡分析
    UnevenApiUsageItem microserviceApiUsageItem = analyzeApiUsageDistribution(aggregatedApiCallMap, validInstanceNames);
    
    log.info("微服务级别API使用分析: 总实例数={}, 有效实例数={}, 实例列表=[{}], 总API数={}, 总调用数={}, " +
        "最热API={}({:.1f}%), 帕累托比例={:.1f}%, 集中度评分={:.1f}, 状态={}, 触发规则=[{}]",
        avgExternalMetrics.size(), microserviceApiUsageItem.getInstanceCount(), microserviceApiUsageItem.getInstanceNames(),
        microserviceApiUsageItem.getTotalApiCount(), microserviceApiUsageItem.getTotalCallCount(),
        microserviceApiUsageItem.getHottestApiName(), 
        microserviceApiUsageItem.getHottestApiRatio() != null ? microserviceApiUsageItem.getHottestApiRatio() * 100 : 0,
        microserviceApiUsageItem.getTop20PercentApiRatio() != null ? microserviceApiUsageItem.getTop20PercentApiRatio() * 100 : 0,
        microserviceApiUsageItem.getConcentrationScore() != null ? microserviceApiUsageItem.getConcentrationScore() : 0,
        microserviceApiUsageItem.getStatus(),
        microserviceApiUsageItem.getTriggeredRules());

    // 设置微服务级别的检测结果
    unevenApiUsageContext.setStatus(microserviceApiUsageItem.getStatus());
    unevenApiUsageContext.setMicroserviceApiUsage(microserviceApiUsageItem);
    unevenApiUsageContext.setMetricsInterval(interval);
    
    log.info("微服务级别API使用不均衡检测完成: 检测到异常={}", microserviceApiUsageItem.getStatus());
    return unevenApiUsageContext;
  }
  
  /**
   * 聚合所有实例的API调用数据到微服务级别
   * 将相同API在不同实例上的调用次数进行累加
   */
  private AggregationResult aggregateApiCallData(Map<String, SvcExternalMetricsRes> avgExternalMetrics) {
    Map<String, Integer> aggregatedMap = new HashMap<>();
    List<String> validInstanceNames = new ArrayList<>();
    int totalInstances = 0;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : avgExternalMetrics.entrySet()) {
      String instanceKey = entry.getKey();
      SvcExternalMetricsRes metrics = entry.getValue();
      totalInstances++;
      
      if (metrics.getInstanceAPICallNumMap() == null || metrics.getInstanceAPICallNumMap().isEmpty()) {
        log.debug("实例 {} 没有API调用数据，跳过", instanceKey);
        continue;
      }
      
      validInstanceNames.add(instanceKey);
      
      // 累加每个API的调用次数
      for (Map.Entry<String, Integer> apiEntry : metrics.getInstanceAPICallNumMap().entrySet()) {
        String apiName = apiEntry.getKey();
        Integer callCount = apiEntry.getValue();
        
        if (callCount != null && callCount > 0) {
          aggregatedMap.merge(apiName, callCount, Integer::sum);
        }
      }
      
      log.debug("实例 {} 贡献了 {} 个API的调用数据", instanceKey, metrics.getInstanceAPICallNumMap().size());
    }
    
    log.info("API调用数据聚合完成: 总实例数={}, 有效实例数={}, 聚合得到 {} 个不同的API", 
        totalInstances, validInstanceNames.size(), aggregatedMap.size());
    
    return new AggregationResult(aggregatedMap, validInstanceNames);
  }
  
  /**
   * 聚合结果封装类
   */
  private static class AggregationResult {
    private final Map<String, Integer> aggregatedApiCallMap;
    private final List<String> validInstanceNames;
    
    public AggregationResult(Map<String, Integer> aggregatedApiCallMap, List<String> validInstanceNames) {
      this.aggregatedApiCallMap = aggregatedApiCallMap;
      this.validInstanceNames = validInstanceNames;
    }
    
    public Map<String, Integer> getAggregatedApiCallMap() {
      return aggregatedApiCallMap;
    }
    
    public List<String> getValidInstanceNames() {
      return validInstanceNames;
    }
  }

  /**
   * 分析API使用分布，检测不均衡模式
   * 基于聚合的API调用数据进行统计分析
   * 重点关注：集中度、帕累托分布、调用分布合理性
   */
  private UnevenApiUsageItem analyzeApiUsageDistribution(Map<String, Integer> apiCallNumMap, List<String> validInstanceNames) {
    UnevenApiUsageItem item = new UnevenApiUsageItem();
    
    if (apiCallNumMap == null || apiCallNumMap.isEmpty()) {
      item.setStatus(false);
      return item;
    }
    
    // 1. 基础统计信息
    int totalApiCount = apiCallNumMap.size();
    int totalCallCount = apiCallNumMap.values().stream().mapToInt(Integer::intValue).sum();
    double avgCallsPerApi = (double) totalCallCount / totalApiCount;
    
    // 设置实例统计信息
    item.setInstanceCount(validInstanceNames.size());
    item.setInstanceNames(String.join(", ", validInstanceNames));
    
    item.setTotalApiCount(totalApiCount);
    item.setTotalCallCount(totalCallCount);
    item.setAvgCallsPerApi(avgCallsPerApi);
    
    if (totalCallCount == 0) {
      item.setStatus(false);
      return item;
    }
    
    // 2. 找出最热门API及其统计信息
    Map.Entry<String, Integer> hottestApi = apiCallNumMap.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .orElse(null);
    
    if (hottestApi != null) {
      item.setHottestApiName(hottestApi.getKey());
      item.setHottestApiCallCount(hottestApi.getValue());
      item.setHottestApiRatio((double) hottestApi.getValue() / totalCallCount);
    }
    
    // 3. 计算帕累托分布（前20%API的调用占比）
    double top20PercentRatio = calculateTop20PercentRatio(apiCallNumMap, totalCallCount);
    item.setTop20PercentApiRatio(top20PercentRatio);
    
    // 4. 计算集中度评分（0-100分，越高越集中）
    double concentrationScore = calculateConcentrationScore(apiCallNumMap, totalCallCount);
    item.setConcentrationScore(concentrationScore);
    

    // 6. 应用检测规则判定是否存在不均衡
    ApiUsageAnalysisResult analysisResult = detectUnevenApiUsage(item);
    item.setStatus(analysisResult.isUneven);
    item.setTriggeredRules(analysisResult.triggeredRules);
    
    return item;
  }
  
  /**
   * API使用分析结果
   */
  private static class ApiUsageAnalysisResult {
    boolean isUneven;
    String triggeredRules;
  }
  
  /**
   * 计算前20%API的调用占比（帕累托分析）
   */
  private double calculateTop20PercentRatio(Map<String, Integer> apiCallNumMap, int totalCallCount) {
    if (apiCallNumMap.size() < 5) {
      // API数量太少，不适用帕累托分析
      return 0.0;
    }
    
    List<Integer> sortedCalls = apiCallNumMap.values().stream()
        .sorted((a, b) -> Integer.compare(b, a))
        .toList();
    
    int top20PercentCount = Math.max(1, apiCallNumMap.size() / 5);
    int top20PercentCalls = sortedCalls.subList(0, top20PercentCount).stream()
        .mapToInt(Integer::intValue)
        .sum();
    
    return (double) top20PercentCalls / totalCallCount;
  }
  
  /**
   * 计算API调用的集中度评分（0-100分）
   * 基于基尼系数的简化版本
   */
  private double calculateConcentrationScore(Map<String, Integer> apiCallNumMap, int totalCallCount) {
    if (apiCallNumMap.size() <= 1) {
      return 0.0;
    }
    
    List<Integer> sortedCalls = apiCallNumMap.values().stream()
        .sorted()
        .toList();
    
    int n = sortedCalls.size();
    double giniSum = 0.0;
    
    for (int i = 0; i < n; i++) {
      giniSum += (2 * (i + 1) - n - 1) * sortedCalls.get(i);
    }
    
    double giniCoeff = giniSum / (n * totalCallCount);
    
    // 转换为0-100的集中度评分
    return Math.abs(giniCoeff) * 100;
  }
  

  /**
   * 检测API使用不均衡的核心规则
   * 三维度检测：极端集中、帕累托异常、分布合理性
   */
  private ApiUsageAnalysisResult detectUnevenApiUsage(UnevenApiUsageItem item) {
    ApiUsageAnalysisResult result = new ApiUsageAnalysisResult();
    StringBuilder triggeredRules = new StringBuilder();
    
    // 检测阈值配置
    double EXTREME_CONCENTRATION_THRESHOLD = 0.8;   // 单个API占比80%以上为极端集中
    double HIGH_CONCENTRATION_THRESHOLD = 0.6;      // 单个API占比60%以上为高度集中
    double PARETO_VIOLATION_THRESHOLD = 0.95;       // 前20%API占比95%以上违反帕累托原则
    double HIGH_CONCENTRATION_SCORE = 70.0;         // 集中度评分70以上为高集中
    int MIN_API_COUNT = 3;                          // 最少API数量要求
    
    // 前置条件：至少要有足够的API才能检测不均衡
    if (item.getTotalApiCount() < MIN_API_COUNT) {
      result.isUneven = false;
      result.triggeredRules = "API数量不足，无需检测";
      return result;
    }
    
    int conditionCount = 0;
    
    // 规则1: 极端集中检测（单个API占绝大部分调用）
    boolean extremeConcentration = item.getHottestApiRatio() != null && 
        item.getHottestApiRatio() > EXTREME_CONCENTRATION_THRESHOLD;
    if (extremeConcentration) {
      conditionCount++;
      triggeredRules.append("极端集中(").append(String.format("%.1f%%", item.getHottestApiRatio() * 100)).append("); ");
    }
    
    // 规则2: 高度集中检测（单个API占比过高）
    boolean highConcentration = item.getHottestApiRatio() != null && 
        item.getHottestApiRatio() > HIGH_CONCENTRATION_THRESHOLD;
    if (highConcentration && !extremeConcentration) {
      conditionCount++;
      triggeredRules.append("高度集中(").append(String.format("%.1f%%", item.getHottestApiRatio() * 100)).append("); ");
    }
    
    // 规则3: 帕累托原则严重违反
    boolean severePareto = item.getTop20PercentApiRatio() != null && 
        item.getTop20PercentApiRatio() > PARETO_VIOLATION_THRESHOLD;
    if (severePareto) {
      conditionCount++;
      triggeredRules.append("帕累托违反(前20%占").append(String.format("%.1f%%", item.getTop20PercentApiRatio() * 100)).append("); ");
    }
    
    // 规则4: 集中度评分过高
    boolean highConcentrationScore = item.getConcentrationScore() != null && 
        item.getConcentrationScore() > HIGH_CONCENTRATION_SCORE;
    if (highConcentrationScore) {
      conditionCount++;
      triggeredRules.append("高集中度评分(").append(String.format("%.1f", item.getConcentrationScore())).append("); ");
    }
    
    // 综合判定：满足任意一个强条件或两个一般条件
    boolean strongCondition = extremeConcentration || severePareto;
    boolean generalCondition = conditionCount >= 2;
    
    result.isUneven = strongCondition || generalCondition;
    result.triggeredRules = !triggeredRules.isEmpty() ?
        triggeredRules.toString().trim() : "无触发规则";
    
    // 记录检测详情
    if (result.isUneven) {
      log.info("检测到API使用不均衡: {} - 最热API={}({:.1f}%), 帕累托比例={:.1f}%, " +
          "集中度评分={:.1f}, 触发规则=[{}]",
          item.getHottestApiName(), 
          item.getHottestApiName(), item.getHottestApiRatio() * 100,
          item.getTop20PercentApiRatio() * 100,
          item.getConcentrationScore(),
          result.triggeredRules);
    }
    
    return result;
  }
} 