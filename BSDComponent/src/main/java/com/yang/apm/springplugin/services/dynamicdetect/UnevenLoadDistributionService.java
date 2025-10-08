package com.yang.apm.springplugin.services.dynamicdetect;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.UnevenLoadDistributionContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.UnevenLoadDisItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.JVMSummaryRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.services.ESService;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.datacollector.CacheService;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UnevenLoadDistributionService implements IDetectConvert {


  @Autowired
  private IntervalWindowMappingService intervalWindowMappingService;

  @Autowired
  private AntiPatternItemService antiPatternItemService;
  @Autowired
  private CacheService cacheService;

  /**
   * 比较维度：统一服务下的不同实例 因为不同服务的业务内容不同，CPU密集度、IO密集度不尽相同，不合适做横向对比
   * @param requestItem 传入服务名称
   * @return 返回封装好的检测结果
   */
  public DetectionResItem unevenLoadDistributionDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.UNEVEN_LOAD_DISTRIBUTION.getValue());
    //相关异味信息
    AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
    DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
    //针对所有的数据计算历史平均值，将TimeWindow下的历史数据计算出平均值
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName()  + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();
    //时间窗口内的数据
    Map<String, SvcMetricsRes> latestIntervalMap = cacheService.getLatestResInServiceLevel(
        ResType.INTERNAL_METRICS.name(), serviceInternal, SvcMetricsRes.class);
    Map<String, SvcMetricsRes> windowMetricsMap = cacheService.getAvgDataInServiceLevel(
        ResType.INTERNAL_METRICS.name(), serviceIntervalAVG, SvcMetricsRes.class);
    Map<String, SvcExternalMetricsRes> windowExternalMetricsMap = cacheService.getAvgDataInServiceLevel(
        ResType.EXTERNAL_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);
    Map<String, SvcExternalMetricsRes> intervalExternalMetricsMap = cacheService.getLatestResInServiceLevel(
        ResType.EXTERNAL_METRICS.name(), serviceInternal, SvcExternalMetricsRes.class);

    //判断是否存在不均衡负载

    UnevenLoadDistributionContext unevenLoadDistributionContext = unevenLoadDistributionDetectDetail(windowMetricsMap, latestIntervalMap,windowExternalMetricsMap,intervalExternalMetricsMap,interval);
    addNew2ResItem(unevenLoadDistributionContext, detectionResItem, requestItem);
    return detectionResItem;
  }


  /**
   * 资源不均衡
   * //针对所有的实例计算对应的cpu ram 吞吐量和延迟的平均值
   */
  private UnevenLoadDistributionContext unevenLoadDistributionDetectDetail(
      Map<String, SvcMetricsRes> windowMetrics, Map<String, SvcMetricsRes> intervalMetrics,
      Map<String, SvcExternalMetricsRes> windowExternalMetricsMap,
      Map<String, SvcExternalMetricsRes> intervalExternalMetricsMap
      , Integer interval) {
    if (intervalMetrics.size()<=1){
      UnevenLoadDistributionContext unevenLoadDistributionContext = new UnevenLoadDistributionContext();
      unevenLoadDistributionContext.setMessage("Service with a single instance an not detect bad smell uneven load distribution.");
      return unevenLoadDistributionContext;
    }
    UnevenLoadDistributionContext unevenLoadDistributionContext = new UnevenLoadDistributionContext();
    Map<String, UnevenLoadDisItem> map = new HashMap<>();
    Boolean flag = false;
    //计算平均值
    List<JVMSummaryRes> internalMetricsList = windowMetrics.values().stream()
        .flatMap(metric -> metric.getJvmSummaryList().stream())
        .toList();

    OptionalDouble cpuAverage = internalMetricsList.stream()
        .map(JVMSummaryRes::getCpuProPCT)
        .mapToDouble(Double::doubleValue)
        .average();


    OptionalDouble memAverage = internalMetricsList.stream()
        .map(JVMSummaryRes::getHeapUsed)
        .mapToLong(Long::longValue)
        .average();

    //todo: 这些都需要除以时间窗口，这样之后才能和时间间隔一个维度比较
    int requestsum = windowExternalMetricsMap.values().stream()
        .mapToInt(SvcExternalMetricsRes::getRequestCount)
        .sum();
    //请求总延时
    double latencySum = windowExternalMetricsMap.values().stream()
        .mapToDouble(
            externalMetrics -> externalMetrics.getAvgLatency() * externalMetrics.getRequestCount())
        .sum();
    double avgLatency = 0;
    if (requestsum!=0){
      avgLatency = latencySum / requestsum;
    }

         //cpu ram 使用率高于实例平均值，但是请求数量低于平均值 延时高于平均值 设定为不均衡负载
     
     // 计算平均值和阈值
     double avgCpu = cpuAverage.orElse(0.0);
     double avgMemory = memAverage.orElse(0.0);
     double avgRequestCount = requestsum > 0 ? (double) requestsum / windowExternalMetricsMap.size() : 0.0;
     
     // 设置阈值 (平均值 * 1.2)
     double coefficient = 0.2;
     double cpuThreshold = avgCpu * (1 + coefficient);
     double memoryThreshold = avgMemory * (1 + coefficient);
     double requestThreshold = avgRequestCount * (1 - coefficient); // 请求数低于平均值
     double latencyThreshold = avgLatency * (1 + coefficient);
     
     log.info("实例平均指标 - CPU: {:.2f}%, 内存: {:.0f} bytes, 请求数: {:.0f}, 延时: {:.2f}ms", 
              avgCpu, avgMemory, avgRequestCount, avgLatency);
     log.info("检测阈值 - CPU: {:.2f}%, 内存: {:.0f} bytes, 请求数: {:.0f}, 延时: {:.2f}ms", 
              cpuThreshold, memoryThreshold, requestThreshold, latencyThreshold);

     // 检测每个实例
     for (Map.Entry<String, SvcMetricsRes> entry : intervalMetrics.entrySet()) {
       String instanceKey = entry.getKey();
       SvcMetricsRes currentIntervalMetrics = entry.getValue();
       SvcExternalMetricsRes currentExternalMetrics = intervalExternalMetricsMap.get(instanceKey);
       
       if (currentExternalMetrics == null || currentIntervalMetrics.getJvmSummaryList() == null || 
           currentIntervalMetrics.getJvmSummaryList().isEmpty()) {
         continue;
       }
       
       UnevenLoadDisItem unevenLoadDisItem = new UnevenLoadDisItem();
       
       // 获取当前实例的指标
       JVMSummaryRes jvmSummary = currentIntervalMetrics.getJvmSummaryList().get(0);
       double currentCpuPct = jvmSummary.getCpuProPCT() != null ? jvmSummary.getCpuProPCT() : 0.0;
       long currentMemoryUsed = jvmSummary.getHeapUsed() != null ? jvmSummary.getHeapUsed() : 0L;
       int currentRequestCount = currentExternalMetrics.getRequestCount() != null ? currentExternalMetrics.getRequestCount() : 0;
       double currentLatency = currentExternalMetrics.getAvgLatency() != null ? currentExternalMetrics.getAvgLatency() : 0.0;
       
       // 设置指标值到UnevenLoadDisItem
       unevenLoadDisItem.setPct(currentCpuPct);
       unevenLoadDisItem.setMemoryUsed(currentMemoryUsed);
       unevenLoadDisItem.setAvgLatency(currentLatency);
       
       // 检测是否满足不均衡负载条件：
       // 1. CPU使用率高于阈值
       boolean highCpuUsage = currentCpuPct > cpuThreshold;
       
       // 2. 内存使用率高于阈值  
       boolean highMemoryUsage = currentMemoryUsed > memoryThreshold;
       
       // 3. 请求数量低于阈值
       boolean lowRequestCount = currentRequestCount < requestThreshold;
       
       // 4. 延时高于阈值
       boolean highLatency = currentLatency > latencyThreshold;
       
       // 所有条件都满足才判定为不均衡负载
       boolean isUnevenLoad = highCpuUsage && highMemoryUsage && lowRequestCount && highLatency;
       
       unevenLoadDisItem.setStatus(isUnevenLoad);
       map.put(instanceKey, unevenLoadDisItem);
       
       if (isUnevenLoad) {
         flag = true;
         log.warn("检测到不均衡负载实例: {} - CPU: {:.2f}% > {:.2f}%, 内存: {:.0f}MB > {:.0f}MB, 请求数: {} < {:.0f}, 延时: {:.2f}ms > {:.2f}ms",
                  instanceKey, currentCpuPct, cpuThreshold, 
                  currentMemoryUsed / (1024.0 * 1024.0), memoryThreshold / (1024.0 * 1024.0),
                  currentRequestCount, requestThreshold, currentLatency, latencyThreshold);
       } else {
         log.debug("实例 {} 正常 - CPU: {:.2f}%({}/{}), 内存: {:.0f}MB({}/{}), 请求数: {}({}/{}), 延时: {:.2f}ms({}/{})",
                   instanceKey, currentCpuPct, highCpuUsage ? "高" : "正常", cpuThreshold,
                   currentMemoryUsed / (1024.0 * 1024.0), highMemoryUsage ? "高" : "正常", memoryThreshold / (1024.0 * 1024.0),
                   currentRequestCount, lowRequestCount ? "低" : "正常", requestThreshold,
                   currentLatency, highLatency ? "高" : "正常", latencyThreshold);
       }
     }

     unevenLoadDistributionContext.setStatus(flag);
     unevenLoadDistributionContext.setInstanceStatus(map);
     unevenLoadDistributionContext.setMetricsInterval(interval);
     
     log.info("不均衡负载检测完成，发现 {} 个异常实例", map.values().stream().mapToInt(item -> item.getStatus() ? 1 : 0).sum());

     return unevenLoadDistributionContext;
  }


}
