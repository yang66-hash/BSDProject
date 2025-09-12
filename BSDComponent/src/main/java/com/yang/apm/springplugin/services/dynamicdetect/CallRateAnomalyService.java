package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.CallRateAnomalyContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.CallRateAnomalyItem;
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
public class CallRateAnomalyService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  /**
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
    //针对所有的数据计算历史平均值，将TimeWindow下的历史数据计算出平均值，上传至ES
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());
    //判断是否是Call Rate Anomaly

    CallRateAnomalyContext callRateAnomalyContext = callRateAnomalyDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(callRateAnomalyContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Call Rate Anomaly的具体检测逻辑
   * 检测调用频率异常问题
   */
  private CallRateAnomalyContext callRateAnomalyDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    CallRateAnomalyContext callRateAnomalyContext = new CallRateAnomalyContext();
    Map<String, CallRateAnomalyItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及Call Rate Anomaly

      CallRateAnomalyItem callRateAnomalyItem = new CallRateAnomalyItem();
      callRateAnomalyItem.setRequestCount(entry.getValue().getRequestCount());
      
      // 计算当前调用频率
      double currentCallRate = entry.getValue().getRequestCount() / (double) interval * 60.0; // 每分钟调用次数
      callRateAnomalyItem.setCallRate(currentCallRate);
      
      // 计算历史平均调用频率
      SvcExternalMetricsRes windowData = windowMetrics.getOrDefault(entry.getKey(), null);
      double avgCallRate = 0.0;
      if (windowData != null) {
        avgCallRate = windowData.getRequestCount() / (double) windowData.getInterval() * 60.0;
      }
      callRateAnomalyItem.setAvgCallRate(avgCallRate);
      
      // 计算调用频率变化率
      double callRateChangeRate = 0.0;
      if (avgCallRate > 0) {
        callRateChangeRate = Math.abs(currentCallRate - avgCallRate) / avgCallRate;
      }
      callRateAnomalyItem.setCallRateChangeRate(callRateChangeRate);
      
      // 计算调用频率标准差（简化计算）
      double callRateStdDev = Math.abs(currentCallRate - avgCallRate);
      callRateAnomalyItem.setCallRateStdDev(callRateStdDev);
      
      // 计算异常程度评分
      double anomalyScore = callRateChangeRate * 100;
      callRateAnomalyItem.setAnomalyScore(anomalyScore);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceCallRateAnomaly = isInstanceCallRateAnomaly(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), callRateAnomalyItem);
      if (isInstanceCallRateAnomaly) {
        flag = true;
      }
      callRateAnomalyItem.setStatus(isInstanceCallRateAnomaly);
      map.put(entry.getKey(), callRateAnomalyItem);
    }

    callRateAnomalyContext.setStatus(flag);
    log.info("map: {}",map);
    callRateAnomalyContext.setInstanceStatus(map);
    callRateAnomalyContext.setMetricsInterval(interval);
    return callRateAnomalyContext;
  }

  private boolean isInstanceCallRateAnomaly(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, CallRateAnomalyItem callRateAnomalyItem) {
    if (windowMetrics == null || intervalMetrics == null) {
      return false;
    }
    
    // Call Rate Anomaly检测阈值
    double highChangeRateThreshold = 0.5; // 变化率阈值50%
    double highAnomalyScoreThreshold = 80.0; // 异常评分阈值80
    double highStdDevThreshold = 20.0; // 标准差阈值
    
    // 检测是否调用频率变化过大
    boolean highChangeRate = callRateAnomalyItem.getCallRateChangeRate() > highChangeRateThreshold;
    
    // 检测是否异常评分过高
    boolean highAnomalyScore = callRateAnomalyItem.getAnomalyScore() > highAnomalyScoreThreshold;
    
    // 检测是否标准差过大
    boolean highStdDev = callRateAnomalyItem.getCallRateStdDev() > highStdDevThreshold;
    
    // 满足任意两个条件就认为存在Call Rate Anomaly
    int conditionCount = 0;
    if (highChangeRate) conditionCount++;
    if (highAnomalyScore) conditionCount++;
    if (highStdDev) conditionCount++;
    
    return conditionCount >= 2;
  }

} 