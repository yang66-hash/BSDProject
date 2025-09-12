package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.ServiceChainContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.ServiceChainItem;
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
public class ServiceChainService implements IDetectConvert {

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
  public DetectionResItem serviceChainDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.SERVICE_CHAIN.getValue());
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
    //判断是否是Service Chain

    ServiceChainContext serviceChainContext = serviceChainDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(serviceChainContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Service Chain的具体检测逻辑
   * 检测服务链过长问题
   */
  private ServiceChainContext serviceChainDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    ServiceChainContext serviceChainContext = new ServiceChainContext();
    Map<String, ServiceChainItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及Service Chain

      ServiceChainItem serviceChainItem = new ServiceChainItem();
      
      // 模拟服务链相关数据（实际应从链路追踪系统获取）
      int chainLength = 2 + (int)(Math.random() * 8); // 2-10个链长度
      int nodeCount = chainLength + (int)(Math.random() * 3); // 节点数量
      double maxChainLatency = entry.getValue().getAvgLatency() * (1.2 + Math.random() * 0.8); // 最大链延迟
      double totalChainLatency = entry.getValue().getAvgLatency() * chainLength * (0.8 + Math.random() * 0.4); // 总链延迟
      
      serviceChainItem.setChainLength(chainLength);
      serviceChainItem.setNodeCount(nodeCount);
      serviceChainItem.setMaxChainLatency(maxChainLatency);
      serviceChainItem.setTotalChainLatency(totalChainLatency);
      
      // 计算失败传播风险
      double failurePropagationRisk = (chainLength * 0.1) + (nodeCount * 0.05); // 基于链长度和节点数量
      serviceChainItem.setFailurePropagationRisk(failurePropagationRisk);
      
      // 计算复杂度评分
      double complexityScore = (chainLength * 2.0) + (nodeCount * 1.5) + (failurePropagationRisk * 10);
      serviceChainItem.setComplexityScore(complexityScore);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceServiceChain = isInstanceServiceChain(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), serviceChainItem);
      if (isInstanceServiceChain) {
        flag = true;
      }
      serviceChainItem.setStatus(isInstanceServiceChain);
      map.put(entry.getKey(), serviceChainItem);
    }

    serviceChainContext.setStatus(flag);
    log.info("map: {}",map);
    serviceChainContext.setInstanceStatus(map);
    serviceChainContext.setMetricsInterval(interval);
    return serviceChainContext;
  }

  private boolean isInstanceServiceChain(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, ServiceChainItem serviceChainItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    // Service Chain检测阈值
    int longChainLengthThreshold = 5; // 链长度阈值5个
    int highNodeCountThreshold = 8; // 节点数量阈值8个
    double highFailureRiskThreshold = 1.0; // 失败传播风险阈值
    double highComplexityThreshold = 20.0; // 复杂度评分阈值
    
    // 检测是否链长度过长
    boolean longChainLength = serviceChainItem.getChainLength() > longChainLengthThreshold;
    
    // 检测是否节点数量过多
    boolean highNodeCount = serviceChainItem.getNodeCount() > highNodeCountThreshold;
    
    // 检测是否失败传播风险过高
    boolean highFailureRisk = serviceChainItem.getFailurePropagationRisk() > highFailureRiskThreshold;
    
    // 检测是否复杂度过高
    boolean highComplexity = serviceChainItem.getComplexityScore() > highComplexityThreshold;
    
    // 满足任意三个条件就认为存在Service Chain问题
    int conditionCount = 0;
    if (longChainLength) conditionCount++;
    if (highNodeCount) conditionCount++;
    if (highFailureRisk) conditionCount++;
    if (highComplexity) conditionCount++;
    
    return conditionCount >= 3;
  }

} 