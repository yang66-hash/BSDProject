package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.InconsistentServiceResponseContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.InconsistentServiceResponseItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.datacollector.CacheService;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InconsistentServiceResponseService implements IDetectConvert {


  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;

  @Autowired
  private AntiPatternItemService antiPatternItemService;
  
  @Autowired
  private CacheService cacheService;


  /**
   *
   * 检测请求量相对稳定的情况下，请求延迟波动大，失败率上升，检测每一个实例
   * @param requestItem 传入服务名称
   * @return 返回封装好的检测结果
   */
  public DetectionResItem inconsistentSvcResponseDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.INCONSISTENT_SVC_RES.getValue());
    //相关异味信息
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    //使用CacheService获取缓存数据，仿照UnevenLoadDistribution的实现
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();

    //获取历史数据用于分析失败率趋势（获取所有缓存的历史数据）
    Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = cacheService.getResInServiceLevel(
        ResType.EXTERNAL_METRICS.name(), serviceInternal,4, SvcExternalMetricsRes.class);
    Map<String, SvcExternalMetricsRes> latestExternalMap = cacheService.getLatestResInServiceLevel(
        ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);

    //判断是否是Inconsistent Service Response
    InconsistentServiceResponseContext inconsistentServiceResponseContext = inconsistentServiceResponseDetectDetail(historicalExternalMetricsMap, latestExternalMap ,interval);
    addNew2ResItem(inconsistentServiceResponseContext, detectionResItem, requestItem);
    return detectionResItem;
  }


  /**
   * Inconsistent Service Response的具体检测逻辑
   * 检测服务响应的一致性问题
   * 基于历史数据检测：请求延迟波动大，失败率过去三个时间间隔不断增长，或存在失败率高于5%
   */
  private InconsistentServiceResponseContext inconsistentServiceResponseDetectDetail(
      Map<String, List<SvcExternalMetricsRes>> historicalMetrics,
      Map<String, SvcExternalMetricsRes> avgExternalMap, Integer interval) {
    InconsistentServiceResponseContext inconsistentServiceResponseContext = new InconsistentServiceResponseContext();
    Map<String, InconsistentServiceResponseItem> map = new HashMap<>();
    Boolean flag = false;

    //依据每一个实例的历史数据进行统计计算
    for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : historicalMetrics.entrySet()) {
      String instanceKey = entry.getKey();
      List<SvcExternalMetricsRes> instanceHistoricalData = entry.getValue();
      String[] split = entry.getKey().split("|");
      SvcExternalMetricsRes  avgExternalMetrics = avgExternalMap.get(split[0]);

      if (instanceHistoricalData == null || instanceHistoricalData.isEmpty()) {
        continue;
      }

      // 按结束时间排序历史数据  其实已经默认升序排列了
      List<SvcExternalMetricsRes> sortedMetrics = instanceHistoricalData.stream()
          .sorted((a, b) -> {
              Date aTime = a.getEndTime() != null ? a.getEndTime() : new Date(0);
              Date bTime = b.getEndTime() != null ? b.getEndTime() : new Date(0);
              return aTime.compareTo(bTime);
          })
          .toList();
      
      // 获取最新的数据作为当前指标
      SvcExternalMetricsRes currentMetrics = sortedMetrics.get(sortedMetrics.size() - 1);
      
      //对每一个实例统计是否涉及Inconsistent Service Response
      InconsistentServiceResponseItem inconsistentServiceResponseItem = new InconsistentServiceResponseItem();
      inconsistentServiceResponseItem.setAvgLatency(currentMetrics.getAvgLatency());
      inconsistentServiceResponseItem.setFailPercent(currentMetrics.getFailPercent());
      inconsistentServiceResponseItem.setRequestCount(currentMetrics.getRequestCount());
      
      // 计算响应时间变异系数和成功率
      double successRate = 1.0 - currentMetrics.getFailPercent();
      inconsistentServiceResponseItem.setSuccessRate(successRate);
      
      // 计算响应时间变异系数（基于历史数据的波动）
      double responseTimeVariability = calculateResponseTimeVariability(sortedMetrics,avgExternalMetrics.getAvgLatency());
      inconsistentServiceResponseItem.setResponseTimeVariability(responseTimeVariability);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceInconsistentResponse = isInstanceInconsistentServiceResponse(sortedMetrics, avgExternalMetrics);
      if (isInstanceInconsistentResponse) {
        flag = true;
      }
      inconsistentServiceResponseItem.setStatus(isInstanceInconsistentResponse);
      map.put(instanceKey, inconsistentServiceResponseItem);
      
      log.debug("实例 {} 检测结果: 平均延迟={:.2f}ms, 失败率={:.2f}%, 响应时间变异系数={:.3f}, 状态={}",
          instanceKey, currentMetrics.getAvgLatency(), currentMetrics.getFailPercent() * 100, 
          responseTimeVariability, isInstanceInconsistentResponse);
    }

    inconsistentServiceResponseContext.setStatus(flag);
    log.info("Inconsistent Service Response检测完成，发现 {} 个异常实例", 
        map.values().stream().mapToInt(item -> item.getStatus() ? 1 : 0).sum());
    inconsistentServiceResponseContext.setInstanceStatus(map);
    inconsistentServiceResponseContext.setMetricsInterval(interval);
    return inconsistentServiceResponseContext;
  }

  /**
   * 判断单个实例是否存在Inconsistent Service Response问题
   * 检测方法：
   * 1. 请求延迟存在很大波动
   * 2. 请求失败率过去三个时间间隔不断增长，或者存在一个请求失败率高于5%
   */
  private boolean isInstanceInconsistentServiceResponse(List<SvcExternalMetricsRes> historicalMetrics,
      SvcExternalMetricsRes avgExternalMap) {
    if (historicalMetrics == null || historicalMetrics.isEmpty()) {
      return false;
    }
    
    // Inconsistent Service Response检测阈值
    double failRateThreshold = 0.05; // 失败率阈值5%
    double responseTimeVariationThreshold = 0.3; // 响应时间变化阈值30%

    // 条件1: 检测是否存在失败率高于5%的情况
    boolean hasHighFailureRate = historicalMetrics.stream()
        .anyMatch(metrics -> metrics.getFailPercent() != null && metrics.getFailPercent() > failRateThreshold);
    
    // 条件2: 检测请求延迟是否存在很大波动
    boolean hasHighLatencyVariation = hasHighResponseTimeVariation(historicalMetrics, avgExternalMap ,responseTimeVariationThreshold);
    
    // 条件3: 检测过去三个时间间隔失败率是否不断增长
    boolean failureRateIncreasing = isFailureRateIncreasing(historicalMetrics);
    
    // 记录检测详情
    log.debug("实例检测详情: 存在高失败率={} (>{}%), 高延迟波动={} (变异系数>{:.1f}), 失败率增长趋势={}",
        hasHighFailureRate, failRateThreshold * 100,
        hasHighLatencyVariation, responseTimeVariationThreshold,
        failureRateIncreasing);
    
    // 满足以下任一条件就认为存在Inconsistent Service Response:
    // 1. 存在失败率高于5% + 请求延迟存在很大波动
    // 2. 失败率过去三个时间间隔不断增长 + 请求延迟存在很大波动
    return (hasHighFailureRate && hasHighLatencyVariation) ||
           (failureRateIncreasing && hasHighLatencyVariation);
  }
  
  /**
   * 计算响应时间变异系数
   * @param historicalMetrics 历史指标数据
   * @return 响应时间变异系数
   */
  private double calculateResponseTimeVariability(List<SvcExternalMetricsRes> historicalMetrics, double avgLatency) {
    if (historicalMetrics == null || historicalMetrics.size() < 2) {
      return 0.0;
    }
    
    // 计算平均延迟

    if (avgLatency == 0.0) {
      return 0.0;
    }
    
    // 计算标准差
    double variance = historicalMetrics.stream()
        .filter(m -> m.getAvgLatency() != null)
        .mapToDouble(m -> Math.pow(m.getAvgLatency() - avgLatency, 2))
        .average()
        .orElse(0.0);
    
    double stdDev = Math.sqrt(variance);
    
    // 返回变异系数 (标准差/平均值)
    return stdDev / avgLatency;
  }
  
  /**
   * 检测是否存在高响应时间波动
   *
   * @param historicalMetrics 历史指标数据
   * @param avgExternalMetrics
   * @param threshold         阈值
   * @return 是否存在高波动
   */
  private boolean hasHighResponseTimeVariation(List<SvcExternalMetricsRes> historicalMetrics,
      SvcExternalMetricsRes avgExternalMetrics, double threshold) {
    double variability = calculateResponseTimeVariability(historicalMetrics, avgExternalMetrics.getAvgLatency());
    return variability > threshold;
  }
  
  /**
   * 检测过去三个时间间隔失败率是否不断增长
   * @param historicalMetrics 历史指标数据
   * @return 是否存在失败率增长趋势
   */
  private boolean isFailureRateIncreasing(List<SvcExternalMetricsRes> historicalMetrics) {
    if (historicalMetrics == null || historicalMetrics.size() < 3) {
      return false;
    }
    
    // 按结束时间排序，获取最近的三个数据点
    List<SvcExternalMetricsRes> sortedMetrics = historicalMetrics.stream()
        .sorted((a, b) -> {
            Date aTime = a.getEndTime() != null ? a.getEndTime() : new Date(0);
            Date bTime = b.getEndTime() != null ? b.getEndTime() : new Date(0);
            return aTime.compareTo(bTime);
        })
        .toList();
    
    int size = sortedMetrics.size();
    if (size < 3) {
      return false;
    }
    
    // 获取最近三个数据点
    SvcExternalMetricsRes third = sortedMetrics.get(size - 3);
    SvcExternalMetricsRes second = sortedMetrics.get(size - 2);
    SvcExternalMetricsRes first = sortedMetrics.get(size - 1);
    
    // 检查是否存在连续增长的趋势
    double thirdFailRate = third.getFailPercent() != null ? third.getFailPercent() : 0.0;
    double secondFailRate = second.getFailPercent() != null ? second.getFailPercent() : 0.0;
    double firstFailRate = first.getFailPercent() != null ? first.getFailPercent() : 0.0;
    
    boolean increasing = thirdFailRate < secondFailRate && secondFailRate < firstFailRate;
    
    if (increasing) {
      log.info("检测到失败率增长趋势: {:.2f}% -> {:.2f}% -> {:.2f}%", 
          thirdFailRate * 100, secondFailRate * 100, firstFailRate * 100);
    }
    
    return increasing;
  }

}
