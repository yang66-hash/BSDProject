package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.ResourceWasteContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.ResourceWasteItem;
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
public class ResourceWasteService implements IDetectConvert {

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
  public DetectionResItem resourceWasteDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.RESOURCE_WASTE.getValue());
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
    //判断是否是Resource Waste

    ResourceWasteContext resourceWasteContext = resourceWasteDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(resourceWasteContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Resource Waste的具体检测逻辑
   * 检测资源浪费问题
   */
  private ResourceWasteContext resourceWasteDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    ResourceWasteContext resourceWasteContext = new ResourceWasteContext();
    Map<String, ResourceWasteItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及Resource Waste

      ResourceWasteItem resourceWasteItem = new ResourceWasteItem();
      resourceWasteItem.setRequestCount(entry.getValue().getRequestCount());
      
      // 模拟资源使用数据（实际应从监控系统获取）
      double cpuUsage = Math.random() * 100;
      double memoryUsage = Math.random() * 100;
      double networkUsage = Math.random() * 100;
      
      resourceWasteItem.setCpuUsage(cpuUsage);
      resourceWasteItem.setMemoryUsage(memoryUsage);
      resourceWasteItem.setNetworkUsage(networkUsage);
      
      // 计算资源利用效率和浪费程度
      double requestDensity = entry.getValue().getRequestCount() / (double) interval;
      double resourceEfficiency = requestDensity / ((cpuUsage + memoryUsage + networkUsage) / 3.0 + 1);
      double wasteLevel = 100.0 - resourceEfficiency * 100;
      
      resourceWasteItem.setResourceEfficiency(resourceEfficiency);
      resourceWasteItem.setWasteLevel(wasteLevel);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceResourceWaste = isInstanceResourceWaste(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), resourceWasteItem);
      if (isInstanceResourceWaste) {
        flag = true;
      }
      resourceWasteItem.setStatus(isInstanceResourceWaste);
      map.put(entry.getKey(), resourceWasteItem);
    }

    resourceWasteContext.setStatus(flag);
    log.info("map: {}",map);
    resourceWasteContext.setInstanceStatus(map);
    resourceWasteContext.setMetricsInterval(interval);
    return resourceWasteContext;
  }

  private boolean isInstanceResourceWaste(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, ResourceWasteItem resourceWasteItem) {
    if (windowMetrics == null || intervalMetrics == null) {
      return false;
    }
    
    // Resource Waste检测阈值
    double lowResourceUtilizationThreshold = 30.0; // 资源利用率阈值30%
    double lowRequestDensityThreshold = 10.0; // 请求密度阈值
    double highWasteLevelThreshold = 70.0; // 浪费程度阈值70%
    
    // 检测是否资源利用率过低
    double avgResourceUsage = (resourceWasteItem.getCpuUsage() + resourceWasteItem.getMemoryUsage() + resourceWasteItem.getNetworkUsage()) / 3.0;
    boolean lowResourceUtilization = avgResourceUsage < lowResourceUtilizationThreshold;
    
    // 检测是否请求密度过低
    double requestDensity = intervalMetrics.getRequestCount() / 60.0; // 每分钟请求数
    boolean lowRequestDensity = requestDensity < lowRequestDensityThreshold;
    
    // 检测是否浪费程度过高
    boolean highWasteLevel = resourceWasteItem.getWasteLevel() > highWasteLevelThreshold;
    
    // 满足任意两个条件就认为存在Resource Waste
    int conditionCount = 0;
    if (lowResourceUtilization) conditionCount++;
    if (lowRequestDensity) conditionCount++;
    if (highWasteLevel) conditionCount++;
    
    return conditionCount >= 2;
  }

} 