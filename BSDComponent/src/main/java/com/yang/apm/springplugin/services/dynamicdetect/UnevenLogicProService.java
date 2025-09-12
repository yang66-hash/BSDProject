package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.UnevenLogicProContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.UnevenLogicProItem;
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
public class UnevenLogicProService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem unevenLogicProDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.UNEVEN_LOGIC_PRO.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    UnevenLogicProContext unevenLogicProContext = unevenLogicProDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(unevenLogicProContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private UnevenLogicProContext unevenLogicProDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    UnevenLogicProContext unevenLogicProContext = new UnevenLogicProContext();
    Map<String, UnevenLogicProItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      UnevenLogicProItem unevenLogicProItem = new UnevenLogicProItem();
      
      // 模拟不均衡逻辑处理相关数据
      double maxProcessingTime = entry.getValue().getAvgLatency() * (1.5 + Math.random() * 2); // 最长处理时间
      double minProcessingTime = entry.getValue().getAvgLatency() * (0.3 + Math.random() * 0.4); // 最短处理时间
      double processingTimeImbalance = (maxProcessingTime - minProcessingTime) / entry.getValue().getAvgLatency(); // 不均衡度
      double processingTimeVariance = Math.pow(maxProcessingTime - entry.getValue().getAvgLatency(), 2) + 
                                     Math.pow(minProcessingTime - entry.getValue().getAvgLatency(), 2); // 方差
      double complexLogicRatio = 0.2 + Math.random() * 0.5; // 20%-70%复杂逻辑占比
      double logicEfficiencyScore = 100.0 - (processingTimeImbalance * 50); // 效率评分
      
      unevenLogicProItem.setProcessingTimeImbalance(processingTimeImbalance);
      unevenLogicProItem.setMaxProcessingTime(maxProcessingTime);
      unevenLogicProItem.setMinProcessingTime(minProcessingTime);
      unevenLogicProItem.setProcessingTimeVariance(processingTimeVariance);
      unevenLogicProItem.setComplexLogicRatio(complexLogicRatio);
      unevenLogicProItem.setLogicEfficiencyScore(logicEfficiencyScore);

      boolean isInstanceUnevenLogicPro = isInstanceUnevenLogicPro(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), unevenLogicProItem);
      if (isInstanceUnevenLogicPro) {
        flag = true;
      }
      unevenLogicProItem.setStatus(isInstanceUnevenLogicPro);
      map.put(entry.getKey(), unevenLogicProItem);
    }

    unevenLogicProContext.setStatus(flag);
    log.info("map: {}",map);
    unevenLogicProContext.setInstanceStatus(map);
    unevenLogicProContext.setMetricsInterval(interval);
    return unevenLogicProContext;
  }

  private boolean isInstanceUnevenLogicPro(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, UnevenLogicProItem unevenLogicProItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    double highImbalanceThreshold = 2.0; // 不均衡度阈值2.0
    double highComplexLogicRatioThreshold = 0.6; // 复杂逻辑占比阈值60%
    double lowEfficiencyThreshold = 60.0; // 效率评分阈值60
    double highVarianceThreshold = 10000.0; // 方差阈值
    
    boolean highImbalance = unevenLogicProItem.getProcessingTimeImbalance() > highImbalanceThreshold;
    boolean highComplexLogicRatio = unevenLogicProItem.getComplexLogicRatio() > highComplexLogicRatioThreshold;
    boolean lowEfficiency = unevenLogicProItem.getLogicEfficiencyScore() < lowEfficiencyThreshold;
    boolean highVariance = unevenLogicProItem.getProcessingTimeVariance() > highVarianceThreshold;
    
    int conditionCount = 0;
    if (highImbalance) conditionCount++;
    if (highComplexLogicRatio) conditionCount++;
    if (lowEfficiency) conditionCount++;
    if (highVariance) conditionCount++;
    
    return conditionCount >= 3;
  }
} 