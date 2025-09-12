package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.UnnecessaryProContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.UnnecessaryProItem;
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
public class UnnecessaryProService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem unnecessaryProDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.UNNECESSARY_PRO.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    UnnecessaryProContext unnecessaryProContext = unnecessaryProDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(unnecessaryProContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private UnnecessaryProContext unnecessaryProDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    UnnecessaryProContext unnecessaryProContext = new UnnecessaryProContext();
    Map<String, UnnecessaryProItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      UnnecessaryProItem unnecessaryProItem = new UnnecessaryProItem();
      
      // 模拟不必要处理相关数据
      int redundantProcessingCount = (int)(entry.getValue().getRequestCount() * (0.1 + Math.random() * 0.3)); // 10%-40%重复处理
      double ineffectiveComputationRatio = 0.15 + Math.random() * 0.35; // 15%-50%无效计算比例
      double resourceWasteRate = ineffectiveComputationRatio + (redundantProcessingCount / (double)entry.getValue().getRequestCount()) * 0.5; // 资源浪费率
      double processingEfficiencyScore = 100.0 - (resourceWasteRate * 100); // 处理效率评分
      double cacheHitRate = 0.4 + Math.random() * 0.5; // 40%-90%缓存命中率
      double optimizationPotentialScore = resourceWasteRate * 80 + (1 - cacheHitRate) * 20; // 优化潜力评分
      
      unnecessaryProItem.setRedundantProcessingCount(redundantProcessingCount);
      unnecessaryProItem.setIneffectiveComputationRatio(ineffectiveComputationRatio);
      unnecessaryProItem.setResourceWasteRate(resourceWasteRate);
      unnecessaryProItem.setProcessingEfficiencyScore(processingEfficiencyScore);
      unnecessaryProItem.setCacheHitRate(cacheHitRate);
      unnecessaryProItem.setOptimizationPotentialScore(optimizationPotentialScore);

      boolean isInstanceUnnecessaryPro = isInstanceUnnecessaryPro(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), unnecessaryProItem);
      if (isInstanceUnnecessaryPro) {
        flag = true;
      }
      unnecessaryProItem.setStatus(isInstanceUnnecessaryPro);
      map.put(entry.getKey(), unnecessaryProItem);
    }

    unnecessaryProContext.setStatus(flag);
    log.info("map: {}",map);
    unnecessaryProContext.setInstanceStatus(map);
    unnecessaryProContext.setMetricsInterval(interval);
    return unnecessaryProContext;
  }

  private boolean isInstanceUnnecessaryPro(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, UnnecessaryProItem unnecessaryProItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    double highRedundantRatioThreshold = 0.25; // 重复处理比例阈值25%
    double highIneffectiveComputationThreshold = 0.3; // 无效计算比例阈值30%
    double highResourceWasteThreshold = 0.4; // 资源浪费率阈值40%
    double lowEfficiencyThreshold = 60.0; // 处理效率评分阈值60
    double lowCacheHitRateThreshold = 0.6; // 缓存命中率阈值60%
    double highOptimizationPotentialThreshold = 50.0; // 优化潜力评分阈值50
    
    double redundantRatio = unnecessaryProItem.getRedundantProcessingCount() / (double)intervalMetrics.getRequestCount();
    boolean highRedundantRatio = redundantRatio > highRedundantRatioThreshold;
    boolean highIneffectiveComputation = unnecessaryProItem.getIneffectiveComputationRatio() > highIneffectiveComputationThreshold;
    boolean highResourceWaste = unnecessaryProItem.getResourceWasteRate() > highResourceWasteThreshold;
    boolean lowEfficiency = unnecessaryProItem.getProcessingEfficiencyScore() < lowEfficiencyThreshold;
    boolean lowCacheHitRate = unnecessaryProItem.getCacheHitRate() < lowCacheHitRateThreshold;
    boolean highOptimizationPotential = unnecessaryProItem.getOptimizationPotentialScore() > highOptimizationPotentialThreshold;
    
    int conditionCount = 0;
    if (highRedundantRatio) conditionCount++;
    if (highIneffectiveComputation) conditionCount++;
    if (highResourceWaste) conditionCount++;
    if (lowEfficiency) conditionCount++;
    if (lowCacheHitRate) conditionCount++;
    if (highOptimizationPotential) conditionCount++;
    
    return conditionCount >= 4;
  }
} 