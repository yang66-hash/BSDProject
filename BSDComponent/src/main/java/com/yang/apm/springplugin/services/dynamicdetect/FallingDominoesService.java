package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.FallingDominoesContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.FallingDominoesItem;
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
public class FallingDominoesService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem fallingDominoesDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.FALLING_DOMINOES.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    FallingDominoesContext fallingDominoesContext = fallingDominoesDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(fallingDominoesContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private FallingDominoesContext fallingDominoesDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    FallingDominoesContext fallingDominoesContext = new FallingDominoesContext();
    Map<String, FallingDominoesItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      FallingDominoesItem fallingDominoesItem = new FallingDominoesItem();
      
      // 模拟级联失败相关数据
      int cascadeFailureCount = (int)(entry.getValue().getFailPercent() * 10); // 基于失败率模拟级联失败次数
      int errorPropagationDepth = 2 + (int)(Math.random() * 5); // 2-7层错误传播深度
      int affectedDownstreamServices = 1 + (int)(Math.random() * 8); // 1-9个受影响的下游服务
      double failureRecoveryTime = entry.getValue().getAvgLatency() * (2 + Math.random() * 8); // 故障恢复时间
      double systemResilienceScore = 100.0 - (cascadeFailureCount * 10 + errorPropagationDepth * 5); // 系统弹性评分
      double faultIsolationEffectiveness = Math.max(0, 1.0 - (affectedDownstreamServices / 10.0)); // 故障隔离效果
      
      fallingDominoesItem.setCascadeFailureCount(cascadeFailureCount);
      fallingDominoesItem.setErrorPropagationDepth(errorPropagationDepth);
      fallingDominoesItem.setAffectedDownstreamServices(affectedDownstreamServices);
      fallingDominoesItem.setFailureRecoveryTime(failureRecoveryTime);
      fallingDominoesItem.setSystemResilienceScore(systemResilienceScore);
      fallingDominoesItem.setFaultIsolationEffectiveness(faultIsolationEffectiveness);

      boolean isInstanceFallingDominoes = isInstanceFallingDominoes(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), fallingDominoesItem);
      if (isInstanceFallingDominoes) {
        flag = true;
      }
      fallingDominoesItem.setStatus(isInstanceFallingDominoes);
      map.put(entry.getKey(), fallingDominoesItem);
    }

    fallingDominoesContext.setStatus(flag);
    log.info("map: {}",map);
    fallingDominoesContext.setInstanceStatus(map);
    fallingDominoesContext.setMetricsInterval(interval);
    return fallingDominoesContext;
  }

  private boolean isInstanceFallingDominoes(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, FallingDominoesItem fallingDominoesItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    int highCascadeFailureThreshold = 3; // 级联失败次数阈值3次
    int deepErrorPropagationThreshold = 5; // 错误传播深度阈值5层
    int manyAffectedServicesThreshold = 5; // 受影响服务数量阈值5个
    double lowResilienceThreshold = 50.0; // 系统弹性评分阈值50
    double lowIsolationThreshold = 0.5; // 故障隔离效果阈值50%
    
    boolean highCascadeFailure = fallingDominoesItem.getCascadeFailureCount() > highCascadeFailureThreshold;
    boolean deepErrorPropagation = fallingDominoesItem.getErrorPropagationDepth() > deepErrorPropagationThreshold;
    boolean manyAffectedServices = fallingDominoesItem.getAffectedDownstreamServices() > manyAffectedServicesThreshold;
    boolean lowResilience = fallingDominoesItem.getSystemResilienceScore() < lowResilienceThreshold;
    boolean lowIsolation = fallingDominoesItem.getFaultIsolationEffectiveness() < lowIsolationThreshold;
    
    int conditionCount = 0;
    if (highCascadeFailure) conditionCount++;
    if (deepErrorPropagation) conditionCount++;
    if (manyAffectedServices) conditionCount++;
    if (lowResilience) conditionCount++;
    if (lowIsolation) conditionCount++;
    
    return conditionCount >= 3;
  }
} 