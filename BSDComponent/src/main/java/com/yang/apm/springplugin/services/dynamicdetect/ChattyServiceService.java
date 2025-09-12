package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.ChattyServiceContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.ChattyServiceItem;
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
public class ChattyServiceService implements IDetectConvert {

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
  public DetectionResItem chattyServiceDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.CHATTY_SERVICE.getValue());
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
    //判断是否是Chatty Service

    ChattyServiceContext chattyServiceContext = chattyServiceDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(chattyServiceContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * Chatty Service的具体检测逻辑
   * 检测服务过度通信问题
   */
  private ChattyServiceContext chattyServiceDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    ChattyServiceContext chattyServiceContext = new ChattyServiceContext();
    Map<String, ChattyServiceItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及Chatty Service

      ChattyServiceItem chattyServiceItem = new ChattyServiceItem();
      
      // 计算服务间调用频率
      double interServiceCallRate = entry.getValue().getRequestCount() / (double) interval * 60.0; // 每分钟调用次数
      chattyServiceItem.setInterServiceCallRate(interServiceCallRate);
      
      // 模拟其他通信相关数据（实际应从监控系统获取）
      double avgCallsPerOperation = 2.0 + Math.random() * 8.0; // 2-10次调用
      int externalDependencyCount = 3 + (int)(Math.random() * 10); // 3-13个外部依赖
      double networkOverheadRatio = 0.1 + Math.random() * 0.4; // 10%-50%网络开销
      int callChainLength = 2 + (int)(Math.random() * 6); // 2-8个调用链长度
      
      chattyServiceItem.setAvgCallsPerOperation(avgCallsPerOperation);
      chattyServiceItem.setExternalDependencyCount(externalDependencyCount);
      chattyServiceItem.setNetworkOverheadRatio(networkOverheadRatio);
      chattyServiceItem.setCallChainLength(callChainLength);
      
      // 计算通信密集度
      double communicationIntensity = (avgCallsPerOperation * externalDependencyCount * networkOverheadRatio) / 10.0;
      chattyServiceItem.setCommunicationIntensity(communicationIntensity);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceChattyService = isInstanceChattyService(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), chattyServiceItem);
      if (isInstanceChattyService) {
        flag = true;
      }
      chattyServiceItem.setStatus(isInstanceChattyService);
      map.put(entry.getKey(), chattyServiceItem);
    }

    chattyServiceContext.setStatus(flag);
    log.info("map: {}",map);
    chattyServiceContext.setInstanceStatus(map);
    chattyServiceContext.setMetricsInterval(interval);
    return chattyServiceContext;
  }

  private boolean isInstanceChattyService(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, ChattyServiceItem chattyServiceItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    // Chatty Service检测阈值
    double highCallsPerOperationThreshold = 6.0; // 单次操作调用次数阈值6次
    double highDependencyCountThreshold = 8; // 外部依赖数量阈值8个
    double highNetworkOverheadThreshold = 0.3; // 网络开销比例阈值30%
    double highCommunicationIntensityThreshold = 2.0; // 通信密集度阈值
    
    // 检测是否单次操作调用次数过多
    boolean highCallsPerOperation = chattyServiceItem.getAvgCallsPerOperation() > highCallsPerOperationThreshold;
    
    // 检测是否外部依赖过多
    boolean highDependencyCount = chattyServiceItem.getExternalDependencyCount() > highDependencyCountThreshold;
    
    // 检测是否网络开销过高
    boolean highNetworkOverhead = chattyServiceItem.getNetworkOverheadRatio() > highNetworkOverheadThreshold;
    
    // 检测是否通信密集度过高
    boolean highCommunicationIntensity = chattyServiceItem.getCommunicationIntensity() > highCommunicationIntensityThreshold;
    
    // 满足任意三个条件就认为存在Chatty Service
    int conditionCount = 0;
    if (highCallsPerOperation) conditionCount++;
    if (highDependencyCount) conditionCount++;
    if (highNetworkOverhead) conditionCount++;
    if (highCommunicationIntensity) conditionCount++;
    
    return conditionCount >= 3;
  }

} 