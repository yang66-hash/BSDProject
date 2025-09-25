package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.CallRateAnomalyContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.CallRateAnomalyItem;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CallRateAnomalyService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;
  @Autowired
  private CacheService cacheService;

  /**
   * Call Rate Anomaly检测 - 检测短时间内的请求量激增
   * 基于Z-Score和变异系数的简化检测方案
   * @param requestItem 传入服务名称
   * @return 返回封装好的检测结果
   */
  public DetectionResItem callRateAnomalyDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.CALL_RATE_ANOMALY.getValue());
    //相关异味信息
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    //使用CacheService获取缓存数据
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();
    
    //获取历史数据用于统计分析（最多4个历史时间间隔）
    Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = cacheService.getResInServiceLevel(
        ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);

    //执行Call Rate Anomaly检测
    CallRateAnomalyContext callRateAnomalyContext = callRateAnomalyDetectDetail(
        historicalExternalMetricsMap, interval);
    addNew2ResItem(callRateAnomalyContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Call Rate Anomaly的具体检测逻辑
   * 专注于检测短时间内的请求量激增：使用Z-Score + 变异系数
   */
  private CallRateAnomalyContext callRateAnomalyDetectDetail(
      Map<String, List<SvcExternalMetricsRes>> historicalMetrics,
      Integer interval) {
    CallRateAnomalyContext callRateAnomalyContext = new CallRateAnomalyContext();
    Map<String, CallRateAnomalyItem> map = new HashMap<>();
    Boolean flag = false;
    
    //依据每一个实例的历史数据进行统计分析
    for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : historicalMetrics.entrySet()) {
      String instanceKey = entry.getKey();
      List<SvcExternalMetricsRes> instanceHistoricalData = entry.getValue();
      
      // 解析实例名（去掉时间戳后缀）
      String[] split = instanceKey.split("\\|");
      String realInstanceKey = split[0];

      if (instanceHistoricalData == null || instanceHistoricalData.isEmpty() ) {
        continue;
      }
      
      // 执行简化的请求量激增检测
      CallRateAnomalyItem callRateAnomalyItem = detectCallRateSurge(
          instanceHistoricalData, interval);
      
      if (callRateAnomalyItem.getStatus()) {
        flag = true;
      }
      
      map.put(realInstanceKey, callRateAnomalyItem);

    }

    callRateAnomalyContext.setStatus(flag);
    log.info("Call Rate Anomaly检测完成，发现 {} 个请求激增实例", 
        map.values().stream().mapToInt(item -> item.getStatus() ? 1 : 0).sum());
    callRateAnomalyContext.setInstanceStatus(map);
    callRateAnomalyContext.setMetricsInterval(interval);
    return callRateAnomalyContext;
  }

  /**
   * 基于业务导向的请求量激增检测
   * 简化逻辑：前3个时间间隔平均值 vs 当前时间间隔
   */
  private CallRateAnomalyItem detectCallRateSurge(
      List<SvcExternalMetricsRes> historicalData, 
      Integer interval) {
    
    CallRateAnomalyItem item = new CallRateAnomalyItem();
    item.setDetectionInterval(interval);
    
    // 1. 提取调用量序列（最多4个时间间隔数据）
    List<Integer> requestCounts = historicalData.stream()
        .map(SvcExternalMetricsRes::getRequestCount)
        .filter(Objects::nonNull)
        .limit(historicalData.size() - 1)  // 限制取前 size - 1 个元素
        .toList();
    
    if (requestCounts.isEmpty()) {
      item.setStatus(false);
      item.setDetectionReason("无足够的历史数据");
      return item;
    }
    
    // 2. 获取当前请求量（最新的数据点）
    int currentRequestCount = requestCounts.get(requestCounts.size() - 1);
    item.setCurrentRequestCount(currentRequestCount);
    
    // 3. 业务导向检测逻辑
    BusinessOrientedDetectionResult result = performBusinessOrientedDetection(requestCounts);
    
    // 4. 设置检测结果
    item.setHistoricalAvgRequestCount(result.historicalAvg);
    item.setGrowthMultiplier(result.growthMultiplier);
    item.setGrowthPercentage(result.growthPercentage);
    item.setSurgeLevel(result.surgeLevel);
    item.setHistoricalTrend(result.historicalTrend);
    item.setDetectionReason(result.detectionReason);
    item.setStatus(result.isAnomaly);
    
    return item;
  }
  
  /**
   * 业务导向检测结果封装
   */
  private static class BusinessOrientedDetectionResult {
    boolean isAnomaly;
    double historicalAvg;
    double growthMultiplier;
    double growthPercentage;
    String surgeLevel;
    String historicalTrend;
    String detectionReason;
  }
  
  /**
   * 执行业务导向的检测逻辑
   * 规则：
   * 1. 获取当前时刻往前的4个时间间隔数据
   * 2. 统计前3个时间间隔的请求量平均值
   * 3. 最新时间间隔请求量激增到平均值的1.8倍以上 → 异常
   * 4. 100%增长（翻倍）直接判定为异常
   */
  private BusinessOrientedDetectionResult performBusinessOrientedDetection(List<Integer> requestCounts) {
    BusinessOrientedDetectionResult result = new BusinessOrientedDetectionResult();
    
    int dataSize = requestCounts.size();
    int currentCount = requestCounts.get(dataSize - 1);
    
    // 构建历史趋势字符串
    result.historicalTrend = requestCounts.stream()
        .map(String::valueOf)
        .reduce((a, b) -> a + " → " + b)
        .orElse("无数据");
    
    if (dataSize < 2) {
      // 数据不足，无法检测
      result.isAnomaly = false;
      result.historicalAvg = currentCount;
      result.growthMultiplier = 1.0;
      result.growthPercentage = 0.0;
      result.surgeLevel = "INSUFFICIENT_DATA";
      result.detectionReason = "历史数据不足（需要至少2个时间间隔的数据）";
      return result;
    }
    
    // 计算历史平均值（前面的数据点，排除当前）
    List<Integer> historicalCounts = requestCounts.subList(0, dataSize - 1);
    double historicalAvg = historicalCounts.stream()
        .mapToInt(Integer::intValue)
        .average()
        .orElse(0.0);
    
    result.historicalAvg = historicalAvg;
    
    if (historicalAvg <= 0) {
      // 历史平均为0，特殊处理
      result.isAnomaly = currentCount > 0;
      result.growthMultiplier = currentCount > 0 ? Double.POSITIVE_INFINITY : 1.0;
      result.growthPercentage = currentCount > 0 ? Double.POSITIVE_INFINITY : 0.0;
      result.surgeLevel = currentCount > 0 ? "EXTREME" : "NORMAL";
      result.detectionReason = currentCount > 0 ? 
          "从零请求激增到" + currentCount + "次" : "请求量保持为零";
      return result;
    }
    
    // 计算增长倍数和百分比
    result.growthMultiplier = currentCount / historicalAvg;
    result.growthPercentage = (currentCount - historicalAvg) / historicalAvg * 100;
    
    // 业务导向判定逻辑
    boolean isSurge1_8x = result.growthMultiplier >= 1.8; // 1.8倍激增
    boolean isSurge100Percent = result.growthPercentage >= 100.0; // 100%增长（翻倍）
    
    // 激增程度评级
    if (result.growthMultiplier >= 3.0) {
      result.surgeLevel = "EXTREME"; // 极端激增（3倍以上）
    } else if (result.growthMultiplier >= 2.0) {
      result.surgeLevel = "HIGH"; // 高度激增（2-3倍）
    } else if (result.growthMultiplier >= 1.8) {
      result.surgeLevel = "MODERATE"; // 中度激增（1.8-2倍）
    } else if (result.growthMultiplier >= 1.5) {
      result.surgeLevel = "MILD"; // 轻度激增（1.5-1.8倍）
    } else {
      result.surgeLevel = "NORMAL"; // 正常范围
    }
    
    // 异常判定
    result.isAnomaly = isSurge1_8x || isSurge100Percent;
    
    // 检测原因
    if (result.isAnomaly) {
      if (isSurge100Percent) {
        result.detectionReason = String.format("请求量翻倍增长：从平均%.1f次激增到%d次（增长%.1f%%）", 
            historicalAvg, currentCount, result.growthPercentage);
      } else if (isSurge1_8x) {
        result.detectionReason = String.format("请求量激增：从平均%.1f次增长到%d次（%.2f倍增长）", 
            historicalAvg, currentCount, result.growthMultiplier);
      }
    } else {
      result.detectionReason = String.format("请求量正常：从平均%.1f次变化到%d次（%.2f倍，增长%.1f%%）", 
          historicalAvg, currentCount, result.growthMultiplier, result.growthPercentage);
    }
    
    // 记录检测详情
    if (result.isAnomaly) {
      log.info("检测到Call Rate Anomaly: 实例请求量激增 - 当前={}次, 历史平均={:.1f}次, " +
          "增长倍数={:.2f}, 增长率={:.1f}%, 激增等级={}", 
          currentCount, historicalAvg, result.growthMultiplier, 
          result.growthPercentage, result.surgeLevel);
    }
    
    return result;
  }
} 