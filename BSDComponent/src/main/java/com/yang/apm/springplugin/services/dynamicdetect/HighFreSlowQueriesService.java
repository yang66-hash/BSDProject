package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.HighFreSlowQueriesContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.HighFreSlowQueriesItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.datacollector.CacheService;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HighFreSlowQueriesService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;
  @Autowired
  private CacheService cacheService;

  /**
   * @param requestItem 传入服务名称
   * @return 返回封装好的检测结果
   */
  public DetectionResItem highFreSlowQueriesDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.HIGH_FRE_OF_SLOW_QUERIES.getValue());
    //相关异味信息
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    //使用CacheService获取缓存数据，参考其他服务的实现方式
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();
    
    //获取历史数据用于统计分析（最多4个历史时间间隔）
    Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = cacheService.getResInServiceLevel(
        ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);
    
    //获取时间窗口内的平均数据
    Map<String, SvcExternalMetricsRes> avgExternalMetricsMap = cacheService.getAvgDataInServiceLevel(
        ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);

    //执行High Frequency Of Slow Queries检测
    HighFreSlowQueriesContext highFreSlowQueriesContext = highFreSlowQueriesDetectDetail(
        historicalExternalMetricsMap, avgExternalMetricsMap, interval);
    addNew2ResItem(highFreSlowQueriesContext, detectionResItem, requestItem);
    return detectionResItem;
  }

    /**
   * High Frequency Of Slow Queries的具体检测逻辑
   * 针对每个服务实例进行独立的慢查询检测分析
   */
  private HighFreSlowQueriesContext highFreSlowQueriesDetectDetail(
      Map<String, List<SvcExternalMetricsRes>> historicalMetrics, 
      Map<String, SvcExternalMetricsRes> avgMetrics, 
      Integer interval) {
    HighFreSlowQueriesContext highFreSlowQueriesContext = new HighFreSlowQueriesContext();
    Map<String, HighFreSlowQueriesItem> map = new HashMap<>();
    Boolean flag = false;
    
    //依据每一个实例的历史数据进行独立的慢查询检测
    for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : historicalMetrics.entrySet()) {
      String instanceKey = entry.getKey();
      List<SvcExternalMetricsRes> instanceHistoricalData = entry.getValue();
      
      // 解析实例名（去掉时间戳后缀）
      String[] split = instanceKey.split("\\|");
      String realInstanceKey = split[0];

      if (instanceHistoricalData == null || instanceHistoricalData.isEmpty()) {
        continue;
      }
      
      // 执行针对单个实例的高频慢查询检测
      HighFreSlowQueriesItem slowQueryItem = detectInstanceSlowQueries(
          instanceHistoricalData, realInstanceKey, interval);
      
      if (slowQueryItem.getStatus()) {
        flag = true;
        log.warn("检测到实例 {} 存在高频慢查询问题: {}", realInstanceKey, slowQueryItem.getDetectionReason());
      }
      
      map.put(realInstanceKey, slowQueryItem);
    }

    highFreSlowQueriesContext.setStatus(flag);
    log.info("High Frequency Of Slow Queries检测完成，发现 {} 个异常实例",
        map.values().stream().mapToInt(item -> item.getStatus() ? 1 : 0).sum());
    highFreSlowQueriesContext.setInstanceStatus(map);
    highFreSlowQueriesContext.setMetricsInterval(interval);
    return highFreSlowQueriesContext;
  }

    /**
   * 针对单个实例进行高频慢查询检测
   * 基于历史数据进行简化的检测逻辑
   */
  private HighFreSlowQueriesItem detectInstanceSlowQueries(
      List<SvcExternalMetricsRes> historicalData, 
      String instanceKey, 
      Integer interval) {
    
    HighFreSlowQueriesItem item = new HighFreSlowQueriesItem();
    item.setStatus(false);
    
    // 1. 数据验证
    if (historicalData == null || historicalData.isEmpty()) {
      item.setDetectionReason("无历史数据");
      return item;
    }
    
    // 2. 获取最新数据（最后一个数据点）
    SvcExternalMetricsRes latestData = historicalData.get(historicalData.size() - 1);
    
    // 3. 提取基础指标
    int currentSlowQueryCount = latestData.getSlowQueryCount() != null ? latestData.getSlowQueryCount() : 0;
    int currentTotalQueryCount = latestData.getSqlQueryCount() != null ? latestData.getSqlQueryCount() : 0;
    
    // 4. 填充基础指标信息
    populateBasicMetrics(item, latestData, interval);
    
    // 5. 计算当前慢查询比例
    double currentSlowQueryRatio = calculateSlowQueryRatio(currentSlowQueryCount, currentTotalQueryCount);
    item.setSlowQueryRatio(currentSlowQueryRatio);
    
    // 6. 主要检测逻辑
    List<String> detectedIssues = new ArrayList<>();
    boolean isDetected = false;
    
    // 6.1 当前时间间隔慢查询比例检测
    if (currentSlowQueryRatio >= 0.20) { // 20%阈值
      detectedIssues.add(String.format("当前慢查询比例过高: %.1f%%", currentSlowQueryRatio * 100));
      isDetected = true;
    }
    
    // 6.2 慢查询绝对频率检测
    double slowQueryFrequency = currentSlowQueryCount / (interval / 60.0); // 次/分钟
    if (slowQueryFrequency >= 10.0) { // 10次/分钟阈值
      detectedIssues.add(String.format("慢查询频率过高: %.1f次/分钟", slowQueryFrequency));
      isDetected = true;
    }
    
    // 6.3 历史趋势分析（如果有足够的历史数据）
    if (historicalData.size() >= 2) {
      SlowQueryTrendResult trendResult = analyzeSlowQueryTrend(historicalData);
      if (trendResult.isTrendDeterioration()) {
        detectedIssues.add(trendResult.getTrendDescription());
        isDetected = true;
      }
    }
    
    // 6.4 SQL指纹分析
    List<String> problematicQueries = analyzeSlowQueryFingerprints(latestData);
    if (!problematicQueries.isEmpty()) {
      detectedIssues.add(String.format("发现 %d 个高频慢查询SQL", problematicQueries.size()));
      item.setProblematicQueries(problematicQueries);
      isDetected = true;
    }
    
    // 7. 设置检测结果
    item.setStatus(isDetected);
    item.setDetectedIssues(detectedIssues);
    
    if (isDetected) {
      item.setDetectionReason(String.format("检测到 %d 个问题: %s", 
          detectedIssues.size(), String.join("; ", detectedIssues)));
    } else {
      item.setDetectionReason("未检测到高频慢查询异常");
    }
    
    return item;
  }
  
  /**
   * 分析慢查询趋势变化
   */
  private SlowQueryTrendResult analyzeSlowQueryTrend(List<SvcExternalMetricsRes> historicalData) {
    if (historicalData.size() < 2) {
      return new SlowQueryTrendResult(false, "历史数据不足");
    }
    
    // 计算历史慢查询比例
    List<Double> slowQueryRatios = new ArrayList<>();
    for (SvcExternalMetricsRes data : historicalData) {
      int slowCount = data.getSlowQueryCount() != null ? data.getSlowQueryCount() : 0;
      int totalCount = data.getSqlQueryCount() != null ? data.getSqlQueryCount() : 0;
      double ratio = calculateSlowQueryRatio(slowCount, totalCount);
      slowQueryRatios.add(ratio);
    }
    
    // 简单趋势判断：最新值与前面平均值对比
    double currentRatio = slowQueryRatios.get(slowQueryRatios.size() - 1);
    double historicalAvg = slowQueryRatios.subList(0, slowQueryRatios.size() - 1)
        .stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    
    boolean isTrendDeterioration = currentRatio > historicalAvg * 1.5 && currentRatio > 0.10;
    String description = isTrendDeterioration ? 
        String.format("慢查询比例趋势恶化: 当前%.1f%% vs 历史平均%.1f%%", 
            currentRatio * 100, historicalAvg * 100) : 
        "趋势正常";
    
    return new SlowQueryTrendResult(isTrendDeterioration, description);
  }
  
  /**
   * 分析慢查询SQL指纹
   */
  private List<String> analyzeSlowQueryFingerprints(SvcExternalMetricsRes data) {
    List<String> problematicQueries = new ArrayList<>();
    
    if (data.getSqlSlowFingerPrintMap() == null || data.getSqlFingerPrintMap() == null) {
      return problematicQueries;
    }
    
    Map<String, Integer> slowFingerprintMap = data.getSqlSlowFingerPrintMap();
    Map<String, Integer> totalFingerprintMap = data.getSqlFingerPrintMap();
    
    for (Map.Entry<String, Integer> entry : slowFingerprintMap.entrySet()) {
      String sqlFingerprint = entry.getKey();
      int slowCount = entry.getValue();
      int totalCount = totalFingerprintMap.getOrDefault(sqlFingerprint, slowCount);
      
      // 条件：慢查询比例 > 50% 且 慢查询次数 >= 3
      if (slowCount >= 3 && totalCount > 0) {
        double slowRatio = (double) slowCount / totalCount;
        if (slowRatio > 0.50) {
          problematicQueries.add(String.format("%s (慢查询: %d/%d, %.1f%%)", 
              sqlFingerprint, slowCount, totalCount, slowRatio * 100));
        }
      }
    }
    
    return problematicQueries;
  }
  
  /**
   * 慢查询趋势分析结果
   */
  private static class SlowQueryTrendResult {
    private final boolean trendDeterioration;
    private final String trendDescription;
    
    public SlowQueryTrendResult(boolean trendDeterioration, String trendDescription) {
      this.trendDeterioration = trendDeterioration;
      this.trendDescription = trendDescription;
    }
    
    public boolean isTrendDeterioration() {
      return trendDeterioration;
    }
    
    public String getTrendDescription() {
      return trendDescription;
    }
  }
  
  /**
   * 计算慢查询比例
   */
  private double calculateSlowQueryRatio(int slowQueryCount, int totalQueryCount) {
    if (totalQueryCount <= 0) {
      return 0.0;
    }
    return (double) slowQueryCount / totalQueryCount;
  }
  

  
  /**
   * 填充基础指标信息
   */
  private void populateBasicMetrics(HighFreSlowQueriesItem item, SvcExternalMetricsRes metrics, Integer interval) {
    Integer slowQueryCount = metrics.getSlowQueryCount() != null ? metrics.getSlowQueryCount() : 0;
    Integer totalQueryCount = metrics.getSqlQueryCount() != null ? metrics.getSqlQueryCount() : 0;
    
    item.setSlowQueryCount(slowQueryCount);
    item.setTotalQueryCount(totalQueryCount);
    
    // 计算慢查询频率（次/分钟）
    double slowQueryFrequency = slowQueryCount / (interval / 60.0);
    item.setSlowQueryFrequency(slowQueryFrequency);
    
    // 注意：不设置SQL查询执行时间，因为metrics.getAvgLatency()是整个请求延迟而非SQL执行时间
    
  }
  


} 