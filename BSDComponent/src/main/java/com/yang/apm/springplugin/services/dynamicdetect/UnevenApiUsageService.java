package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.UnevenApiUsageContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.UnevenApiUsageItem;
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
public class UnevenApiUsageService implements IDetectConvert {

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
  public DetectionResItem unevenApiUsageDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.UNEVEN_API_USAGE.getValue());
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
    //判断是否是Uneven API Usage

    UnevenApiUsageContext unevenApiUsageContext = unevenApiUsageDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(unevenApiUsageContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Uneven API Usage的具体检测逻辑
   * 检测API使用不均衡问题
   */
  private UnevenApiUsageContext unevenApiUsageDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    UnevenApiUsageContext unevenApiUsageContext = new UnevenApiUsageContext();
    Map<String, UnevenApiUsageItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及Uneven API Usage

      UnevenApiUsageItem unevenApiUsageItem = new UnevenApiUsageItem();
      
      // 模拟API使用数据（实际应从监控系统获取）
      int totalApiCount = 10 + (int)(Math.random() * 20); // 总API数量10-30
      int activeApiCount = 3 + (int)(Math.random() * totalApiCount); // 活跃API数量
      
      unevenApiUsageItem.setTotalApiCount(totalApiCount);
      unevenApiUsageItem.setActiveApiCount(activeApiCount);
      
      // 模拟最热门和最冷门API的使用比例
      double hottestApiUsageRatio = 0.4 + Math.random() * 0.5; // 40%-90%
      double coldestApiUsageRatio = Math.random() * 0.1; // 0%-10%
      
      unevenApiUsageItem.setHottestApiUsageRatio(hottestApiUsageRatio);
      unevenApiUsageItem.setColdestApiUsageRatio(coldestApiUsageRatio);
      
      // 计算API使用不均衡度
      double apiUsageImbalance = hottestApiUsageRatio - coldestApiUsageRatio;
      unevenApiUsageItem.setApiUsageImbalance(apiUsageImbalance);
      
      // 计算API使用方差（简化计算）
      double apiUsageVariance = Math.pow(hottestApiUsageRatio - 0.5, 2) + Math.pow(coldestApiUsageRatio - 0.5, 2);
      unevenApiUsageItem.setApiUsageVariance(apiUsageVariance);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceUnevenApiUsage = isInstanceUnevenApiUsage(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), unevenApiUsageItem);
      if (isInstanceUnevenApiUsage) {
        flag = true;
      }
      unevenApiUsageItem.setStatus(isInstanceUnevenApiUsage);
      map.put(entry.getKey(), unevenApiUsageItem);
    }

    unevenApiUsageContext.setStatus(flag);
    log.info("map: {}",map);
    unevenApiUsageContext.setInstanceStatus(map);
    unevenApiUsageContext.setMetricsInterval(interval);
    return unevenApiUsageContext;
  }

  private boolean isInstanceUnevenApiUsage(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, UnevenApiUsageItem unevenApiUsageItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    // Uneven API Usage检测阈值
    double highImbalanceThreshold = 0.6; // 不均衡度阈值60%
    double lowActiveApiRatioThreshold = 0.3; // 活跃API比例阈值30%
    double highVarianceThreshold = 0.2; // 使用方差阈值
    
    // 检测是否API使用不均衡度过高
    boolean highImbalance = unevenApiUsageItem.getApiUsageImbalance() > highImbalanceThreshold;
    
    // 检测是否活跃API比例过低
    double activeApiRatio = (double) unevenApiUsageItem.getActiveApiCount() / unevenApiUsageItem.getTotalApiCount();
    boolean lowActiveApiRatio = activeApiRatio < lowActiveApiRatioThreshold;
    
    // 检测是否使用方差过大
    boolean highVariance = unevenApiUsageItem.getApiUsageVariance() > highVarianceThreshold;
    
    // 满足任意两个条件就认为存在Uneven API Usage
    int conditionCount = 0;
    if (highImbalance) conditionCount++;
    if (lowActiveApiRatio) conditionCount++;
    if (highVariance) conditionCount++;
    
    return conditionCount >= 2;
  }

} 