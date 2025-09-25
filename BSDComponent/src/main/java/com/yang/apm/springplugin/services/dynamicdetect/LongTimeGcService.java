package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.LongTimeGcContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.LongTimeGcItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.JVMGCRes;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.datacollector.CacheService;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LongTimeGcService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;
  @Autowired
  private CacheService cacheService;

  // 固定GC时间阈值（毫秒）
  private static final long MINOR_GC_TIME_THRESHOLD = 200; // Minor GC超过200ms警告
  private static final long MAJOR_GC_TIME_THRESHOLD = 1000; // Major GC超过1s警告
  
  // 动态阈值增长比例
  private static final double GC_TIME_GROWTH_RATIO = 0.2; // 超过历史平均20%警告

  public DetectionResItem longTimeGcDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.LONG_TIME_GC.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    //使用CacheService获取缓存数据
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    
    //获取所有缓存的JVM数据
    Map<String, List<SvcMetricsRes>> historicalJVMMetricsMap = cacheService.getResInServiceLevel(
        ResType.INTERNAL_METRICS.name(), serviceInternal, 10, SvcMetricsRes.class);

    //执行Long Time GC检测
    LongTimeGcContext longTimeGcContext = longTimeGcDetectDetail(historicalJVMMetricsMap, interval);
    addNew2ResItem(longTimeGcContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Long Time GC的具体检测逻辑
   * 针对每个服务实例检测最长GC时间
   */
  private LongTimeGcContext longTimeGcDetectDetail(
      Map<String, List<SvcMetricsRes>> historicalMetrics, 
      Integer interval) {
    LongTimeGcContext longTimeGcContext = new LongTimeGcContext();
    Map<String, LongTimeGcItem> map = new HashMap<>();
    Boolean flag = false;
    
    //依据每一个实例的数据进行独立的GC检测
    for (Map.Entry<String, List<SvcMetricsRes>> entry : historicalMetrics.entrySet()) {
      String instanceKey = entry.getKey();
      List<SvcMetricsRes> instanceHistoricalData = entry.getValue();
      
      // 解析实例名（去掉时间戳后缀）
      String[] split = instanceKey.split("\\|");
      String realInstanceKey = split[0];

      if (instanceHistoricalData == null || instanceHistoricalData.isEmpty()) {
        continue;
      }
      
      // 执行针对单个实例的GC时间检测
      LongTimeGcItem gcItem = detectInstanceLongTimeGC(instanceHistoricalData);
      
      if (gcItem.getStatus()) {
        flag = true;
        log.warn("检测到实例 {} 存在GC时间过长问题", realInstanceKey);
      }
      
      map.put(realInstanceKey, gcItem);
    }

    longTimeGcContext.setStatus(flag);
    log.info("Long Time GC检测完成，发现 {} 个异常实例",
        map.values().stream().mapToInt(item -> item.getStatus() ? 1 : 0).sum());
    longTimeGcContext.setInstanceStatus(map);
    longTimeGcContext.setMetricsInterval(interval);
    return longTimeGcContext;
  }

  /**
   * 针对单个实例进行GC时间检测
   * 根据历史数据量采用不同的检测策略
   */
  private LongTimeGcItem detectInstanceLongTimeGC(List<SvcMetricsRes> historicalData) {
    LongTimeGcItem item = new LongTimeGcItem();
    item.setStatus(false);
    
    // 1. 数据验证
    if (historicalData == null || historicalData.isEmpty()) {
      return item;
    }
    
    // 2. 获取最新数据点
    SvcMetricsRes latestData = historicalData.get(historicalData.size() - 1);
    if (latestData.getGcRes() == null) {
      return item;
    }
    
    // 3. 提取最新时间窗口的GC时间
    List<Long> latestMinorGCTimes = new ArrayList<>();
    List<Long> latestMajorGCTimes = new ArrayList<>();
    extractGCTimes(latestData, latestMinorGCTimes, latestMajorGCTimes);
    
    // 4. 根据历史数据量选择检测策略
    List<String> detectedIssues = new ArrayList<>();
    boolean isDetected = false;
    
    if (historicalData.size() < 5) {
      // 数据量少于5个时使用固定阈值
      isDetected = detectWithFixedThreshold(latestMinorGCTimes, latestMajorGCTimes, detectedIssues);
    } else {
      // 数据量大于等于5个时使用动态阈值
      isDetected = detectWithDynamicThreshold(historicalData, latestMinorGCTimes, latestMajorGCTimes, detectedIssues);
    }
    
    // 5. 设置检测结果
    item.setStatus(isDetected);
    item.setMaxMinorGcTime(Collections.max(latestMinorGCTimes));
    item.setMaxMajorGcTime(Collections.max(latestMajorGCTimes));
    item.setDetectedIssues(detectedIssues);
    
    // 设置检测原因
    if (isDetected) {
      item.setDetectionReason(String.format("检测到 %d 个问题: %s", 
          detectedIssues.size(), String.join("; ", detectedIssues)));
    } else {
      item.setDetectionReason("未检测到GC时间异常");
    }
    
    return item;
  }
  
  /**
   * 从单个数据点提取GC时间
   */
  private void extractGCTimes(SvcMetricsRes metricsRes, 
      List<Long> minorGCTimes, List<Long> majorGCTimes) {
    for (Map.Entry<String, List<JVMGCRes>> entry : metricsRes.getGcRes().entrySet()) {
      String gcType = entry.getKey();
      List<JVMGCRes> gcResList = entry.getValue();
      
      if (gcResList == null || gcResList.isEmpty()) {
        continue;
      }
      
      // 获取最新的GC数据
      JVMGCRes latestGC = gcResList.get(gcResList.size() - 1);
      if (latestGC.getGcCount() == null || latestGC.getGcCount() == 0 || latestGC.getGcTime() == null) {
        continue;
      }
      
      // 计算单次GC的平均时间
      long avgGCTime = latestGC.getGcTime() / latestGC.getGcCount();
      
      // 根据GC类型分类
      if (gcType.contains("Scavenge") || gcType.contains("ParNew") || gcType.contains("G1 Young")) {
        minorGCTimes.add(avgGCTime);
      } else {
        majorGCTimes.add(avgGCTime);
      }
    }
  }
  
  /**
   * 使用固定阈值进行检测
   */
  private boolean detectWithFixedThreshold(
      List<Long> minorGCTimes, List<Long> majorGCTimes, List<String> detectedIssues) {
    boolean isDetected = false;
    
    // 检查Minor GC时间
    if (!minorGCTimes.isEmpty()) {
      long maxMinorGCTime = Collections.max(minorGCTimes);
      if (maxMinorGCTime > MINOR_GC_TIME_THRESHOLD) {
        detectedIssues.add(String.format("Minor GC时间过长: %dms (阈值%dms)", 
            maxMinorGCTime, MINOR_GC_TIME_THRESHOLD));
        isDetected = true;
      }
    }
    
    // 检查Major GC时间
    if (!majorGCTimes.isEmpty()) {
      long maxMajorGCTime = Collections.max(majorGCTimes);
      if (maxMajorGCTime > MAJOR_GC_TIME_THRESHOLD) {
        detectedIssues.add(String.format("Major GC时间过长: %dms (阈值%dms)", 
            maxMajorGCTime, MAJOR_GC_TIME_THRESHOLD));
        isDetected = true;
      }
    }
    
    return isDetected;
  }
  
  /**
   * 使用动态阈值进行检测
   */
  private boolean detectWithDynamicThreshold(
      List<SvcMetricsRes> historicalData, 
      List<Long> latestMinorGCTimes, 
      List<Long> latestMajorGCTimes, 
      List<String> detectedIssues) {
    boolean isDetected = false;
    
    // 1. 收集历史GC时间（除最新数据点外）
    List<Long> historicalMinorGCTimes = new ArrayList<>();
    List<Long> historicalMajorGCTimes = new ArrayList<>();
    
    for (int i = 0; i < historicalData.size() - 1; i++) {
      extractGCTimes(historicalData.get(i), historicalMinorGCTimes, historicalMajorGCTimes);
    }
    
    // 2. 计算正常GC的平均时间（剔除超过固定阈值的极端值）
    double avgMinorGCTime = historicalMinorGCTimes.stream()
        .filter(time -> time <= MINOR_GC_TIME_THRESHOLD)
        .mapToLong(Long::longValue)
        .average()
        .orElse(MINOR_GC_TIME_THRESHOLD);
    
    double avgMajorGCTime = historicalMajorGCTimes.stream()
        .filter(time -> time <= MAJOR_GC_TIME_THRESHOLD)
        .mapToLong(Long::longValue)
        .average()
        .orElse(MAJOR_GC_TIME_THRESHOLD);
    
    // 3. 计算动态阈值
    double minorGCThreshold = avgMinorGCTime * (1 + GC_TIME_GROWTH_RATIO);
    double majorGCThreshold = avgMajorGCTime * (1 + GC_TIME_GROWTH_RATIO);
    
    // 4. 检查最新的GC时间是否超过动态阈值
    if (!latestMinorGCTimes.isEmpty()) {
      long maxMinorGCTime = Collections.max(latestMinorGCTimes);
      if (maxMinorGCTime > minorGCThreshold) {
        detectedIssues.add(String.format("Minor GC时间异常增长: %dms (历史平均%.1fms，动态阈值%.1fms)", 
            maxMinorGCTime, avgMinorGCTime, minorGCThreshold));
        isDetected = true;
      }
    }
    
    if (!latestMajorGCTimes.isEmpty()) {
      long maxMajorGCTime = Collections.max(latestMajorGCTimes);
      if (maxMajorGCTime > majorGCThreshold) {
        detectedIssues.add(String.format("Major GC时间异常增长: %dms (历史平均%.1fms，动态阈值%.1fms)", 
            maxMajorGCTime, avgMajorGCTime, majorGCThreshold));
        isDetected = true;
      }
    }
    
    return isDetected;
  }
} 