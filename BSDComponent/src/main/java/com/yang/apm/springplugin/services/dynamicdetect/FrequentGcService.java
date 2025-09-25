package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.FrequentGcContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.FrequentGcItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.JVMGCRes;
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
public class FrequentGcService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;
  @Autowired
  private CacheService cacheService;

  public DetectionResItem frequentGcDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.FREQUENT_GC.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    //使用CacheService获取缓存数据，参考其他服务的实现方式
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();
    
    //获取历史JVM数据用于统计分析（最多4个历史时间间隔）
    Map<String, List<SvcMetricsRes>> historicalJVMMetricsMap = cacheService.getResInServiceLevel(
        ResType.INTERNAL_METRICS.name(), serviceInternal, 4, SvcMetricsRes.class);
    
    //获取时间窗口内的平均JVM数据
    Map<String, SvcMetricsRes> avgJVMMetricsMap = cacheService.getAvgDataInServiceLevel(
        ResType.INTERNAL_METRICS.name(), serviceIntervalAVG, SvcMetricsRes.class);

    //执行Frequent GC检测
    FrequentGcContext frequentGcContext = frequentGcDetectDetail(
        historicalJVMMetricsMap, avgJVMMetricsMap, interval);
    addNew2ResItem(frequentGcContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Frequent GC的具体检测逻辑
   * 针对每个服务实例进行独立的GC检测分析
   */
  private FrequentGcContext frequentGcDetectDetail(
      Map<String, List<SvcMetricsRes>> historicalMetrics, 
      Map<String, SvcMetricsRes> avgMetrics, 
      Integer interval) {
    FrequentGcContext frequentGcContext = new FrequentGcContext();
    Map<String, FrequentGcItem> map = new HashMap<>();
    Boolean flag = false;
    
    //依据每一个实例的历史数据进行独立的GC检测
    for (Map.Entry<String, List<SvcMetricsRes>> entry : historicalMetrics.entrySet()) {
      String instanceKey = entry.getKey();
      List<SvcMetricsRes> instanceHistoricalData = entry.getValue();
      
      // 解析实例名（去掉时间戳后缀）
      String[] split = instanceKey.split("\\|");
      String realInstanceKey = split[0];

      if (instanceHistoricalData == null || instanceHistoricalData.size() < 2) {
        continue;
      }
      
      // 执行针对单个实例的频繁GC检测
      FrequentGcItem gcItem = detectInstanceFrequentGC(
          instanceHistoricalData, realInstanceKey, interval);
      
      if (gcItem.getStatus()) {
        flag = true;
        log.warn("检测到实例 {} 存在频繁GC问题", realInstanceKey);
      }
      
      map.put(realInstanceKey, gcItem);
    }

    frequentGcContext.setStatus(flag);
    log.info("Frequent GC检测完成，发现 {} 个异常实例",
        map.values().stream().mapToInt(item -> item.getStatus() ? 1 : 0).sum());
    frequentGcContext.setInstanceStatus(map);
    frequentGcContext.setMetricsInterval(interval);
    return frequentGcContext;
  }

  /**
   * 针对单个实例进行频繁GC检测
   * 基于四维度检测：Minor GC时间异常、Minor GC频率异常、Major GC时间异常、Major GC频率异常
   */
  private FrequentGcItem detectInstanceFrequentGC(
      List<SvcMetricsRes> historicalData, 
      String instanceKey, 
      Integer interval) {
    
    FrequentGcItem item = new FrequentGcItem();
    item.setStatus(false);
    
    // 1. 数据验证
    if (historicalData == null || historicalData.size() < 2) {
      return item;
    }
    
    // 2. 提取GC数据
    GCMetrics gcMetrics = extractGCMetrics(historicalData);
    
    // 3. 填充基础指标信息
    populateBasicGCMetrics(item, gcMetrics, interval);
    
    // 4. 四维度检测
    List<String> detectedIssues = new ArrayList<>();
    boolean isDetected = false;
    
    // 维度1: Minor GC时间异常检测
    if (gcMetrics.getMinorGCCount() > 0) {
      double currentMinorAvgTime = (double) gcMetrics.getMinorGCTime() / gcMetrics.getMinorGCCount();
      if (currentMinorAvgTime > gcMetrics.getMinorAvgGCTime() * 1.2) { // 超过历史平均20%
        detectedIssues.add(String.format("Minor GC平均时间异常: 当前%.1fms vs 历史平均%.1fms", 
            currentMinorAvgTime, gcMetrics.getMinorAvgGCTime()));
        isDetected = true;
      }
    }
    
    // 维度2: Minor GC频率检测
    double minorGCFrequency = gcMetrics.getMinorGCCount() / (interval / 60.0); // 次/分钟
    if (minorGCFrequency >= 10.0) { // 10次/分钟阈值
      detectedIssues.add(String.format("Minor GC频率过高: %.1f次/分钟", minorGCFrequency));
      isDetected = true;
    }
    
    // 维度3: Major GC时间异常检测
    if (gcMetrics.getMajorGCCount() > 0) {
      double currentMajorAvgTime = (double) gcMetrics.getMajorGCTime() / gcMetrics.getMajorGCCount();
      if (currentMajorAvgTime > gcMetrics.getMajorAvgGCTime() * 1.2) { // 超过历史平均20%
        detectedIssues.add(String.format("Major GC平均时间异常: 当前%.1fms vs 历史平均%.1fms", 
            currentMajorAvgTime, gcMetrics.getMajorAvgGCTime()));
        isDetected = true;
      }
    }
    
    // 维度4: Major GC频率检测
    double majorGCFrequency = gcMetrics.getMajorGCCount() / (interval / 60.0); // 次/分钟
    if (majorGCFrequency >= 2.0) { // 2次/分钟阈值
      detectedIssues.add(String.format("Major GC频率过高: %.1f次/分钟", majorGCFrequency));
      isDetected = true;
    }
    
    // 5. 设置检测结果
    item.setStatus(isDetected);
    item.setGcFrequency(minorGCFrequency + majorGCFrequency);
    item.setMinorGcCount((int)gcMetrics.getMinorGCCount());
    item.setMajorGcCount((int)gcMetrics.getMajorGCCount());
    item.setAvgGcTime((gcMetrics.getMinorGCTime() + gcMetrics.getMajorGCTime()) / 
        (double)(gcMetrics.getMinorGCCount() + gcMetrics.getMajorGCCount()));
    item.setGcTimeRatio((gcMetrics.getMinorGCTime() + gcMetrics.getMajorGCTime()) / 
        (interval * 1000.0));
    item.setDetectedIssues(detectedIssues);
    
    // 设置检测原因
    if (isDetected) {
      item.setDetectionReason(String.format("检测到 %d 个问题: %s", 
          detectedIssues.size(), String.join("; ", detectedIssues)));
    } else {
      item.setDetectionReason("未检测到频繁GC异常");
    }
    
    return item;
  }
  
  /**
   * 从SvcMetricsRes中提取GC相关指标
   * 通过比较时间间隔内的首尾数据计算实际发生的GC次数和时间
   */
  private GCMetrics extractGCMetrics(List<SvcMetricsRes> historicalData) {
    GCMetrics gcMetrics = new GCMetrics();
    
    if (historicalData == null || historicalData.size() < 2) {
      return gcMetrics;
    }
    
    // 获取时间间隔内的首尾数据点
    SvcMetricsRes startData = historicalData.get(historicalData.size() - 2); // 前一个数据点
    SvcMetricsRes endData = historicalData.get(historicalData.size() - 1);   // 最新数据点
    
    if (startData.getGcRes() == null || endData.getGcRes() == null) {
      return gcMetrics;
    }
    
    // 遍历不同GC类型的数据
    for (Map.Entry<String, List<JVMGCRes>> entry : endData.getGcRes().entrySet()) {
      String gcType = entry.getKey();
      List<JVMGCRes> endGCList = entry.getValue();
      List<JVMGCRes> startGCList = startData.getGcRes().get(gcType);
      
      if (endGCList != null && !endGCList.isEmpty() && startGCList != null && !startGCList.isEmpty()) {
        JVMGCRes endGC = endGCList.get(endGCList.size() - 1);
        JVMGCRes startGC = startGCList.get(startGCList.size() - 1);
        
        // 计算这个时间间隔内实际发生的GC次数和时间
        long gcCountDiff = (endGC.getGcCount() != null ? endGC.getGcCount() : 0) 
            - (startGC.getGcCount() != null ? startGC.getGcCount() : 0);
        long gcTimeDiff = (endGC.getGcTime() != null ? endGC.getGcTime() : 0) 
            - (startGC.getGcTime() != null ? startGC.getGcTime() : 0);
        
        // 如果GC次数没有变化，说明这段时间内没有发生此类GC
        if (gcCountDiff == 0) {
          continue;
        }
        
        // 计算这个GC类型的历史平均GC时间（使用最早的数据点）
        double avgGCTime = calculateHistoricalAvgGCTime(gcType, startData);
        
        // 累加不同类型的GC统计
        if (gcType.contains("Scavenge") || gcType.contains("ParNew") || gcType.contains("G1 Young")) {
          gcMetrics.setMinorGCCount(gcMetrics.getMinorGCCount() + gcCountDiff);
          gcMetrics.setMinorGCTime(gcMetrics.getMinorGCTime() + gcTimeDiff);
          gcMetrics.setMinorAvgGCTime(avgGCTime);
        } else {
          gcMetrics.setMajorGCCount(gcMetrics.getMajorGCCount() + gcCountDiff);
          gcMetrics.setMajorGCTime(gcMetrics.getMajorGCTime() + gcTimeDiff);
          gcMetrics.setMajorAvgGCTime(avgGCTime);
        }
      }
    }
    
    // 计算总计
    gcMetrics.setTotalGCCount(gcMetrics.getMinorGCCount() + gcMetrics.getMajorGCCount());
    gcMetrics.setTotalGCTime(gcMetrics.getMinorGCTime() + gcMetrics.getMajorGCTime());
    
    return gcMetrics;
  }
  
  /**
   * 计算某个GC类型的历史平均GC时间
   * 使用最早的数据点计算，因为它包含了JVM启动到那个时刻的所有GC信息
   */
  private double calculateHistoricalAvgGCTime(String gcType, SvcMetricsRes earliestData) {
    if (earliestData.getGcRes() == null) {
      return 0.0;
    }
    
    List<JVMGCRes> gcList = earliestData.getGcRes().get(gcType);
    if (gcList == null || gcList.isEmpty()) {
      return 0.0;
    }
    
    JVMGCRes firstGC = gcList.get(0);
    if (firstGC.getGcCount() == null || firstGC.getGcCount() == 0 || firstGC.getGcTime() == null) {
      return 0.0;
    }
    
    return (double) firstGC.getGcTime() / firstGC.getGcCount();
  }
  
  /**
   * 填充基础GC指标信息
   */
  private void populateBasicGCMetrics(FrequentGcItem item, GCMetrics gcMetrics, Integer interval) {
    // 计算总GC频率（次/分钟）
    double minorGCFrequency = gcMetrics.getMinorGCCount() / (interval / 60.0);
    double majorGCFrequency = gcMetrics.getMajorGCCount() / (interval / 60.0);
    item.setGcFrequency(minorGCFrequency + majorGCFrequency);
    
    // 设置GC次数
    item.setMinorGcCount((int)gcMetrics.getMinorGCCount());
    item.setMajorGcCount((int)gcMetrics.getMajorGCCount());
    
    // 计算平均GC时间
    double totalGCCount = gcMetrics.getMinorGCCount() + gcMetrics.getMajorGCCount();
    double avgGcTime = totalGCCount > 0 ? 
        (double)(gcMetrics.getMinorGCTime() + gcMetrics.getMajorGCTime()) / totalGCCount : 0.0;
    item.setAvgGcTime(avgGcTime);
    
    // 计算GC时间占比
    double gcTimeRatio = (gcMetrics.getMinorGCTime() + gcMetrics.getMajorGCTime()) / (interval * 1000.0);
    item.setGcTimeRatio(gcTimeRatio);
    
    // 堆内存使用率需要从JVM Summary中获取，这里暂时设置为null
    item.setHeapMemoryUsage(null);
  }
  
  /**
   * GC指标封装类
   */
  private static class GCMetrics {
    private long totalGCCount = 0;
    private long totalGCTime = 0;
    private long minorGCCount = 0;
    private long minorGCTime = 0;
    private double minorAvgGCTime = 0.0;
    private long majorGCCount = 0;
    private long majorGCTime = 0;
    private double majorAvgGCTime = 0.0;
    
    // getters and setters
    public long getTotalGCCount() { return totalGCCount; }
    public void setTotalGCCount(long totalGCCount) { this.totalGCCount = totalGCCount; }
    public long getTotalGCTime() { return totalGCTime; }
    public void setTotalGCTime(long totalGCTime) { this.totalGCTime = totalGCTime; }
    
    public long getMinorGCCount() { return minorGCCount; }
    public void setMinorGCCount(long minorGCCount) { this.minorGCCount = minorGCCount; }
    public long getMinorGCTime() { return minorGCTime; }
    public void setMinorGCTime(long minorGCTime) { this.minorGCTime = minorGCTime; }
    public double getMinorAvgGCTime() { return minorAvgGCTime; }
    public void setMinorAvgGCTime(double minorAvgGCTime) { this.minorAvgGCTime = minorAvgGCTime; }
    
    public long getMajorGCCount() { return majorGCCount; }
    public void setMajorGCCount(long majorGCCount) { this.majorGCCount = majorGCCount; }
    public long getMajorGCTime() { return majorGCTime; }
    public void setMajorGCTime(long majorGCTime) { this.majorGCTime = majorGCTime; }
    public double getMajorAvgGCTime() { return majorAvgGCTime; }
    public void setMajorAvgGCTime(double majorAvgGCTime) { this.majorAvgGCTime = majorAvgGCTime; }
  }
} 