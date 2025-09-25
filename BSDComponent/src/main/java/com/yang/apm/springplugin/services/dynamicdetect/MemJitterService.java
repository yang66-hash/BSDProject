package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.MemJitterContext;
import com.yang.apm.springplugin.base.context.dynamicres.FrequentGcContext;
import com.yang.apm.springplugin.base.context.dynamicres.LongTimeGcContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.MemJitterItem;
import com.yang.apm.springplugin.base.item.dynamic.FrequentGcItem;
import com.yang.apm.springplugin.base.item.dynamic.LongTimeGcItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.datacollector.CacheService;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemJitterService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;
  @Autowired
  private CacheService cacheService;
  @Autowired
  private FrequentGcService frequentGcService;
  @Autowired
  private LongTimeGcService longTimeGcService;

  // 堆内存使用率波动阈值
  private static final double HEAP_USED_RATE_THRESHOLD = 0.1; // 10%波动

  public DetectionResItem memJitterDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.MEM_JITTER_OF_SVC.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    //使用CacheService获取缓存数据
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    
    //获取所有缓存的JVM数据
    Map<String, List<SvcMetricsRes>> historicalJVMMetricsMap = cacheService.getResInServiceLevel(
        ResType.INTERNAL_METRICS.name(), serviceInternal, 10, SvcMetricsRes.class);

    //执行内存抖动检测
    MemJitterContext memJitterContext = memJitterDetectDetail(historicalJVMMetricsMap, interval, requestItem);
    addNew2ResItem(memJitterContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * 内存抖动的具体检测逻辑
   * 针对每个服务实例检测内存抖动情况
   */
  private MemJitterContext memJitterDetectDetail(
      Map<String, List<SvcMetricsRes>> historicalMetrics, 
      Integer interval,
      RequestItem requestItem) {
    MemJitterContext memJitterContext = new MemJitterContext();
    Map<String, MemJitterItem> map = new HashMap<>();
    Boolean flag = false;
    
    //依据每一个实例的数据进行独立的检测
    for (Map.Entry<String, List<SvcMetricsRes>> entry : historicalMetrics.entrySet()) {
      String instanceKey = entry.getKey();
      List<SvcMetricsRes> instanceHistoricalData = entry.getValue();
      
      // 解析实例名（去掉时间后缀）
      String[] split = instanceKey.split("\\|");
      String realInstanceKey = split[0];

      if (instanceHistoricalData == null || instanceHistoricalData.isEmpty()) {
        continue;
      }
      
      // 执行针对单个实例的内存抖动检测
      MemJitterItem jitterItem = detectInstanceMemJitter(
          instanceHistoricalData, interval, requestItem, realInstanceKey);
      
      if (jitterItem.getStatus()) {
        flag = true;
        log.warn("检测到实例 {} 存在内存抖动问题", realInstanceKey);
      }
      
      map.put(realInstanceKey, jitterItem);
    }

    memJitterContext.setStatus(flag);
    log.info("Memory Jitter检测完成，发现 {} 个异常实例",
        map.values().stream().mapToInt(item -> item.getStatus() ? 1 : 0).sum());
    memJitterContext.setInstanceStatus(map);
    memJitterContext.setMetricsInterval(interval);
    return memJitterContext;
  }

  /**
   * 针对单个实例进行内存抖动检测
   * 基于三个维度：频繁GC、长时间GC和堆内存波动
   */
  private MemJitterItem detectInstanceMemJitter(
      List<SvcMetricsRes> historicalData, 
      Integer interval,
      RequestItem requestItem,
      String instanceKey) {
    
    MemJitterItem item = new MemJitterItem();
    item.setStatus(false);
    
    // 1. 检测GC异常
    boolean hasGCIssue = false;
    List<String> detectedIssues = new ArrayList<>();
    
    // 1.1 检测频繁GC
    DetectionResItem frequentGCResult = frequentGcService.frequentGcDetect(requestItem);
    FrequentGcItem frequentGCItem = null;
    if (frequentGCResult.getContext() instanceof FrequentGcContext) {
      FrequentGcContext context = (FrequentGcContext) frequentGCResult.getContext();
      frequentGCItem = context.getInstanceStatus().get(instanceKey);
      
      if (frequentGCItem != null && frequentGCItem.getStatus()) {
        hasGCIssue = true;
        detectedIssues.addAll(frequentGCItem.getDetectedIssues());
      }
    }
    
    // 1.2 检测GC时间过长
    DetectionResItem longGCResult = longTimeGcService.longTimeGcDetect(requestItem);
    LongTimeGcItem longGCItem = null;
    if (longGCResult.getContext() instanceof LongTimeGcContext) {
      LongTimeGcContext context = (LongTimeGcContext) longGCResult.getContext();
      longGCItem = context.getInstanceStatus().get(instanceKey);
      
      if (longGCItem != null && longGCItem.getStatus()) {
        hasGCIssue = true;
        detectedIssues.addAll(longGCItem.getDetectedIssues());
      }
    }
    
    // 2. 检测堆内存波动
    List<Double> heapUsedRates = new ArrayList<>();
    List<Double> heapCommittedRates = new ArrayList<>();
    
    for (SvcMetricsRes metrics : historicalData) {
      if (metrics.getJvmSummaryList() != null && !metrics.getJvmSummaryList().isEmpty()) {
        var summary = metrics.getJvmSummaryList().get(metrics.getJvmSummaryList().size() - 1);
        if (summary.getHeapMaxed() != null && summary.getHeapMaxed() > 0) {
          // 计算堆内存使用率
          double heapUsedRate = (double)summary.getHeapUsed() / summary.getHeapMaxed();
          heapUsedRates.add(heapUsedRate);
          
          // 计算堆内存提交率
          double heapCommittedRate = (double)summary.getHeapCommitted() / summary.getHeapMaxed();
          heapCommittedRates.add(heapCommittedRate);
        }
      }
    }
    
    // 2.1 检查堆内存使用率波动
    double maxHeapUsedRate = heapUsedRates.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    double minHeapUsedRate = heapUsedRates.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    double heapUsedRateRange = maxHeapUsedRate - minHeapUsedRate;
    
    boolean hasMemoryJitter = false;
    if (heapUsedRateRange > HEAP_USED_RATE_THRESHOLD) {
      hasMemoryJitter = true;
      detectedIssues.add(String.format("堆内存使用率波动过大: %.1f%% (最大%.1f%%, 最小%.1f%%)", 
          heapUsedRateRange * 100, maxHeapUsedRate * 100, minHeapUsedRate * 100));
    }
    
    // 2.2 检查堆内存提交率波动
    double maxHeapCommittedRate = heapCommittedRates.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    double minHeapCommittedRate = heapCommittedRates.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    double heapCommittedRateRange = maxHeapCommittedRate - minHeapCommittedRate;
    
    if (heapCommittedRateRange > HEAP_USED_RATE_THRESHOLD) {
      hasMemoryJitter = true;
      detectedIssues.add(String.format("堆内存提交率波动过大: %.1f%% (最大%.1f%%, 最小%.1f%%)", 
          heapCommittedRateRange * 100, maxHeapCommittedRate * 100, minHeapCommittedRate * 100));
    }
    
    // 3. 综合判断
    boolean isDetected = hasGCIssue && hasMemoryJitter;
    
    // 4. 设置检测结果
    item.setStatus(isDetected);
    item.setHeapUsedRateRange(heapUsedRateRange);
    item.setMaxHeapUsedRate(maxHeapUsedRate);
    item.setMinHeapUsedRate(minHeapUsedRate);
    
    // 设置GC相关指标
    if (frequentGCItem != null) {
      item.setGcFrequency(frequentGCItem.getGcFrequency().intValue());
    }
    if (longGCItem != null) {
      item.setAvgGcTime(Double.valueOf(longGCItem.getMaxMinorGcTime()));
    }
    
    item.setDetectedIssues(detectedIssues);
    
    if (isDetected) {
      item.setDetectionReason(String.format("检测到 %d 个问题: %s", 
          detectedIssues.size(), String.join("; ", detectedIssues)));
    } else {
      item.setDetectionReason("未检测到内存抖动异常");
    }
    
    return item;
  }
} 