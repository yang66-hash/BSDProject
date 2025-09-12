package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.TheRampContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.TheRampItem;
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
public class TheRampService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem theRampDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.THE_RAMP.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    TheRampContext theRampContext = theRampDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(theRampContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private TheRampContext theRampDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    TheRampContext theRampContext = new TheRampContext();
    Map<String, TheRampItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      TheRampItem theRampItem = new TheRampItem();
      
      // 模拟坡道效应相关数据
      SvcExternalMetricsRes windowData = windowMetrics.getOrDefault(entry.getKey(), null);
      
      // 计算负载爬升速率
      double loadRampUpRate = 0.0;
      if (windowData != null && windowData.getRequestCount() > 0) {
        loadRampUpRate = (entry.getValue().getRequestCount() - windowData.getRequestCount()) / (double) windowData.getRequestCount();
      }
      
      // 计算性能退化速率
      double performanceDegradationRate = 0.0;
      if (windowData != null && windowData.getAvgLatency() > 0) {
        performanceDegradationRate = (entry.getValue().getAvgLatency() - windowData.getAvgLatency()) / windowData.getAvgLatency();
      }
      
      // 计算响应时间增长趋势
      double responseTimeGrowthTrend = performanceDegradationRate * 100; // 转为百分比
      
      // 计算系统承载能力 (简化计算)
      double systemCapacity = Math.max(0, 100.0 - (loadRampUpRate * 50 + performanceDegradationRate * 50));
      
      // 计算负载承受阈值
      double loadToleranceThreshold = 80.0 - Math.abs(loadRampUpRate) * 30; // 基于当前负载变化
      
      // 计算性能稳定性评分
      double performanceStabilityScore = 100.0 - Math.abs(performanceDegradationRate) * 100;
      
      theRampItem.setLoadRampUpRate(loadRampUpRate);
      theRampItem.setPerformanceDegradationRate(performanceDegradationRate);
      theRampItem.setResponseTimeGrowthTrend(responseTimeGrowthTrend);
      theRampItem.setSystemCapacity(systemCapacity);
      theRampItem.setLoadToleranceThreshold(loadToleranceThreshold);
      theRampItem.setPerformanceStabilityScore(performanceStabilityScore);

      boolean isInstanceTheRamp = isInstanceTheRamp(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), theRampItem);
      if (isInstanceTheRamp) {
        flag = true;
      }
      theRampItem.setStatus(isInstanceTheRamp);
      map.put(entry.getKey(), theRampItem);
    }

    theRampContext.setStatus(flag);
    log.info("map: {}",map);
    theRampContext.setInstanceStatus(map);
    theRampContext.setMetricsInterval(interval);
    return theRampContext;
  }

  private boolean isInstanceTheRamp(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, TheRampItem theRampItem) {
    if (intervalMetrics == null || windowMetrics == null) {
      return false;
    }
    
    double highLoadRampUpThreshold = 0.5; // 负载爬升速率阈值50%
    double highPerformanceDegradationThreshold = 0.3; // 性能退化速率阈值30%
    double lowSystemCapacityThreshold = 60.0; // 系统承载能力阈值60
    double lowStabilityThreshold = 70.0; // 性能稳定性评分阈值70
    
    boolean highLoadRampUp = theRampItem.getLoadRampUpRate() > highLoadRampUpThreshold;
    boolean highPerformanceDegradation = theRampItem.getPerformanceDegradationRate() > highPerformanceDegradationThreshold;
    boolean lowSystemCapacity = theRampItem.getSystemCapacity() < lowSystemCapacityThreshold;
    boolean lowStability = theRampItem.getPerformanceStabilityScore() < lowStabilityThreshold;
    
    int conditionCount = 0;
    if (highLoadRampUp) conditionCount++;
    if (highPerformanceDegradation) conditionCount++;
    if (lowSystemCapacity) conditionCount++;
    if (lowStability) conditionCount++;
    
    return conditionCount >= 3;
  }
} 