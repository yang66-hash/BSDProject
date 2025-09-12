package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.InconsistentServiceResponseContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.InconsistentServiceResponseItem;
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
public class InconsistentServiceResponseService implements IDetectConvert {


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
  public DetectionResItem inconsistentSvcResponseDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.INCONSISTENT_SVC_RES.getValue());
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
    //判断是否是Inconsistent Service Response

    InconsistentServiceResponseContext inconsistentServiceResponseContext = inconsistentServiceResponseDetectDetail(windowMetrics, intervalMetrics,interval);
    addNew2ResItem(inconsistentServiceResponseContext, detectionResItem, requestItem);
    return detectionResItem;
  }


  /**
   * Inconsistent Service Response的具体检测逻辑
   * 检测服务响应的一致性问题
   */
  private InconsistentServiceResponseContext inconsistentServiceResponseDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    InconsistentServiceResponseContext inconsistentServiceResponseContext = new InconsistentServiceResponseContext();
    Map<String, InconsistentServiceResponseItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及Inconsistent Service Response

      InconsistentServiceResponseItem inconsistentServiceResponseItem = new InconsistentServiceResponseItem();
      inconsistentServiceResponseItem.setAvgLatency(entry.getValue().getAvgLatency());
      inconsistentServiceResponseItem.setFailPercent(entry.getValue().getFailPercent());
      inconsistentServiceResponseItem.setRequestCount(entry.getValue().getRequestCount());
      
      // 计算响应时间变异系数和成功率
      double successRate = 1.0 - entry.getValue().getFailPercent();
      inconsistentServiceResponseItem.setSuccessRate(successRate);
      
      // 计算响应时间变异系数（这里使用简化计算，实际可根据具体需求调整）
      SvcExternalMetricsRes windowData = windowMetrics.getOrDefault(entry.getKey(), null);
      double responseTimeVariability = 0.0;
      if (windowData != null && windowData.getAvgLatency() > 0) {
        responseTimeVariability = Math.abs(entry.getValue().getAvgLatency() - windowData.getAvgLatency()) / windowData.getAvgLatency();
      }
      inconsistentServiceResponseItem.setResponseTimeVariability(responseTimeVariability);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceInconsistentResponse = isInstanceInconsistentServiceResponse(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue());
      if (isInstanceInconsistentResponse) {
        flag = true;
      }
      inconsistentServiceResponseItem.setStatus(isInstanceInconsistentResponse);
      map.put(entry.getKey(), inconsistentServiceResponseItem);
    }

    inconsistentServiceResponseContext.setStatus(flag);
    log.info("map: {}",map);
    inconsistentServiceResponseContext.setInstanceStatus(map);
    inconsistentServiceResponseContext.setMetricsInterval(interval);
    return inconsistentServiceResponseContext;
  }

  private boolean isInstanceInconsistentServiceResponse(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics) {
    if (windowMetrics == null || intervalMetrics == null) {
      return false;
    }
    
    // Inconsistent Service Response检测阈值
    double failRateThreshold = 0.05; // 失败率阈值5%
    double responseTimeVariationThreshold = 0.3; // 响应时间变化阈值30%
    double successRateThreshold = 0.95; // 成功率阈值95%
    
    // 检测失败率是否过高
    boolean highFailureRate = intervalMetrics.getFailPercent() > failRateThreshold;
    
    // 检测响应时间是否波动过大
    boolean highResponseTimeVariation = false;
    if (windowMetrics.getAvgLatency() > 0) {
      double variation = Math.abs(intervalMetrics.getAvgLatency() - windowMetrics.getAvgLatency()) / windowMetrics.getAvgLatency();
      highResponseTimeVariation = variation > responseTimeVariationThreshold;
    }
    
    // 检测成功率是否过低
    double currentSuccessRate = 1.0 - intervalMetrics.getFailPercent();
    boolean lowSuccessRate = currentSuccessRate < successRateThreshold;
    
    // 满足任意两个条件就认为存在Inconsistent Service Response
    int conditionCount = 0;
    if (highFailureRate) conditionCount++;
    if (highResponseTimeVariation) conditionCount++;
    if (lowSuccessRate) conditionCount++;
    
    return conditionCount >= 2;
  }

}
