package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.LongTimeGcContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.LongTimeGcItem;
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
public class LongTimeGcService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem longTimeGcDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.LONG_TIME_GC.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    LongTimeGcContext longTimeGcContext = longTimeGcDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(longTimeGcContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private LongTimeGcContext longTimeGcDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    LongTimeGcContext longTimeGcContext = new LongTimeGcContext();
    Map<String, LongTimeGcItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      LongTimeGcItem longTimeGcItem = new LongTimeGcItem();
      
      // 模拟长时间GC相关数据
      double maxGcTime = 100 + Math.random() * 900; // 100-1000ms
      double avgFullGcTime = 200 + Math.random() * 800; // 200-1000ms
      double totalGcPauseTime = maxGcTime * (1 + Math.random() * 3); // 总暂停时间
      int longGcCount = (int)(Math.random() * 5); // 0-5次长时间GC
      double gcEfficiencyScore = 50 + Math.random() * 50; // 50-100效率评分
      double memoryRecoveryRate = 0.6 + Math.random() * 0.4; // 60%-100%内存回收率
      
      longTimeGcItem.setMaxGcTime(maxGcTime);
      longTimeGcItem.setAvgFullGcTime(avgFullGcTime);
      longTimeGcItem.setTotalGcPauseTime(totalGcPauseTime);
      longTimeGcItem.setLongGcCount(longGcCount);
      longTimeGcItem.setGcEfficiencyScore(gcEfficiencyScore);
      longTimeGcItem.setMemoryRecoveryRate(memoryRecoveryRate);

      boolean isInstanceLongTimeGc = isInstanceLongTimeGc(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), longTimeGcItem);
      if (isInstanceLongTimeGc) {
        flag = true;
      }
      longTimeGcItem.setStatus(isInstanceLongTimeGc);
      map.put(entry.getKey(), longTimeGcItem);
    }

    longTimeGcContext.setStatus(flag);
    log.info("map: {}",map);
    longTimeGcContext.setInstanceStatus(map);
    longTimeGcContext.setMetricsInterval(interval);
    return longTimeGcContext;
  }

  private boolean isInstanceLongTimeGc(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, LongTimeGcItem longTimeGcItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    double maxGcTimeThreshold = 500.0; // 最大GC时间阈值500ms
    double avgFullGcTimeThreshold = 600.0; // 平均Full GC时间阈值600ms
    double lowEfficiencyThreshold = 70.0; // 效率评分阈值
    double lowRecoveryRateThreshold = 0.8; // 内存回收率阈值80%
    
    boolean longMaxGcTime = longTimeGcItem.getMaxGcTime() > maxGcTimeThreshold;
    boolean longAvgFullGcTime = longTimeGcItem.getAvgFullGcTime() > avgFullGcTimeThreshold;
    boolean lowEfficiency = longTimeGcItem.getGcEfficiencyScore() < lowEfficiencyThreshold;
    boolean lowRecoveryRate = longTimeGcItem.getMemoryRecoveryRate() < lowRecoveryRateThreshold;
    
    int conditionCount = 0;
    if (longMaxGcTime) conditionCount++;
    if (longAvgFullGcTime) conditionCount++;
    if (lowEfficiency) conditionCount++;
    if (lowRecoveryRate) conditionCount++;
    
    return conditionCount >= 2;
  }
} 