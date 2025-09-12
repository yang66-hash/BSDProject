package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.MemJitterContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.MemJitterItem;
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
public class MemJitterService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem memJitterDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.MEM_JITTER_OF_SVC.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    MemJitterContext memJitterContext = memJitterDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(memJitterContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private MemJitterContext memJitterDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    MemJitterContext memJitterContext = new MemJitterContext();
    Map<String, MemJitterItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      MemJitterItem memJitterItem = new MemJitterItem();
      
      // 模拟内存抖动相关数据
      double peakMemoryUsage = 0.6 + Math.random() * 0.3; // 60%-90%峰值使用率
      double minMemoryUsage = 0.3 + Math.random() * 0.3; // 30%-60%谷值使用率
      double memoryJitterRange = peakMemoryUsage - minMemoryUsage; // 抖动范围
      double memoryUsageStdDev = memoryJitterRange / 4.0; // 标准差
      double jitterFrequency = 2.0 + Math.random() * 10.0; // 2-12次/分钟抖动频率
      double memoryStabilityScore = 100.0 - (memoryJitterRange * 100); // 稳定性评分
      
      memJitterItem.setMemoryJitterRange(memoryJitterRange);
      memJitterItem.setMemoryUsageStdDev(memoryUsageStdDev);
      memJitterItem.setPeakMemoryUsage(peakMemoryUsage);
      memJitterItem.setMinMemoryUsage(minMemoryUsage);
      memJitterItem.setJitterFrequency(jitterFrequency);
      memJitterItem.setMemoryStabilityScore(memoryStabilityScore);

      boolean isInstanceMemJitter = isInstanceMemJitter(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), memJitterItem);
      if (isInstanceMemJitter) {
        flag = true;
      }
      memJitterItem.setStatus(isInstanceMemJitter);
      map.put(entry.getKey(), memJitterItem);
    }

    memJitterContext.setStatus(flag);
    log.info("map: {}",map);
    memJitterContext.setInstanceStatus(map);
    memJitterContext.setMetricsInterval(interval);
    return memJitterContext;
  }

  private boolean isInstanceMemJitter(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, MemJitterItem memJitterItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    double highJitterRangeThreshold = 0.3; // 抖动范围阈值30%
    double highJitterFrequencyThreshold = 8.0; // 抖动频率阈值8次/分钟
    double lowStabilityThreshold = 70.0; // 稳定性评分阈值
    double highStdDevThreshold = 0.08; // 标准差阈值
    
    boolean highJitterRange = memJitterItem.getMemoryJitterRange() > highJitterRangeThreshold;
    boolean highJitterFrequency = memJitterItem.getJitterFrequency() > highJitterFrequencyThreshold;
    boolean lowStability = memJitterItem.getMemoryStabilityScore() < lowStabilityThreshold;
    boolean highStdDev = memJitterItem.getMemoryUsageStdDev() > highStdDevThreshold;
    
    int conditionCount = 0;
    if (highJitterRange) conditionCount++;
    if (highJitterFrequency) conditionCount++;
    if (lowStability) conditionCount++;
    if (highStdDev) conditionCount++;
    
    return conditionCount >= 3;
  }
} 