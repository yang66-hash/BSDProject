package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.FrequentGcContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.FrequentGcItem;
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
public class FrequentGcService implements IDetectConvert {

  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;
  @Autowired
  private ESService eSService;
  @Autowired
  private AntiPatternItemService antiPatternItemService;

  public DetectionResItem frequentGcDetect(RequestItem requestItem) {
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.FREQUENT_GC.getValue());
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
    String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

    Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, requestItem.getServiceName());
    Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, requestItem.getServiceName());

    FrequentGcContext frequentGcContext = frequentGcDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(frequentGcContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  private FrequentGcContext frequentGcDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    FrequentGcContext frequentGcContext = new FrequentGcContext();
    Map<String, FrequentGcItem> map = new HashMap<>();
    Boolean flag = false;
    
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      FrequentGcItem frequentGcItem = new FrequentGcItem();
      
      // 模拟GC相关数据
      double gcFrequency = 2.0 + Math.random() * 20.0; // 2-22次/分钟
      int minorGcCount = (int)(gcFrequency * interval / 60.0 * 0.8); // 80%是Minor GC
      int majorGcCount = (int)(gcFrequency * interval / 60.0 * 0.2); // 20%是Major GC
      double avgGcTime = 10 + Math.random() * 100; // 10-110ms
      double gcTimeRatio = (gcFrequency * avgGcTime) / (interval * 1000.0); // GC时间占比
      double heapMemoryUsage = 0.4 + Math.random() * 0.5; // 40%-90%堆内存使用率
      
      frequentGcItem.setGcFrequency(gcFrequency);
      frequentGcItem.setMinorGcCount(minorGcCount);
      frequentGcItem.setMajorGcCount(majorGcCount);
      frequentGcItem.setAvgGcTime(avgGcTime);
      frequentGcItem.setGcTimeRatio(gcTimeRatio);
      frequentGcItem.setHeapMemoryUsage(heapMemoryUsage);

      boolean isInstanceFrequentGc = isInstanceFrequentGc(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), frequentGcItem);
      if (isInstanceFrequentGc) {
        flag = true;
      }
      frequentGcItem.setStatus(isInstanceFrequentGc);
      map.put(entry.getKey(), frequentGcItem);
    }

    frequentGcContext.setStatus(flag);
    log.info("map: {}",map);
    frequentGcContext.setInstanceStatus(map);
    frequentGcContext.setMetricsInterval(interval);
    return frequentGcContext;
  }

  private boolean isInstanceFrequentGc(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, FrequentGcItem frequentGcItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    double highGcFrequencyThreshold = 15.0; // GC频率阈值15次/分钟
    double highGcTimeRatioThreshold = 0.05; // GC时间占比阈值5%
    double highHeapUsageThreshold = 0.85; // 堆内存使用率阈值85%
    int highMajorGcThreshold = 2; // Major GC次数阈值
    
    boolean highGcFrequency = frequentGcItem.getGcFrequency() > highGcFrequencyThreshold;
    boolean highGcTimeRatio = frequentGcItem.getGcTimeRatio() > highGcTimeRatioThreshold;
    boolean highHeapUsage = frequentGcItem.getHeapMemoryUsage() > highHeapUsageThreshold;
    boolean highMajorGc = frequentGcItem.getMajorGcCount() > highMajorGcThreshold;
    
    int conditionCount = 0;
    if (highGcFrequency) conditionCount++;
    if (highGcTimeRatio) conditionCount++;
    if (highHeapUsage) conditionCount++;
    if (highMajorGc) conditionCount++;
    
    return conditionCount >= 3;
  }
} 