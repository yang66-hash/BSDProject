package com.yang.apm.springplugin.services.dynamicdetect;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.UnevenLoadDistributionContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.UnevenLoadDisItem;
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

@Service
@Slf4j
public class UnevenLoadDistributionService implements IDetectConvert {


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
  public DetectionResItem unevenLoadDistributionDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.UNEVEN_LOAD_DISTRIBUTION.getValue());
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
    //判断是否是不均衡负载


    UnevenLoadDistributionContext unevenLoadDistributionContext = unevenLoadDistributionDetectDetail(windowMetrics, intervalMetrics,interval);
    addNew2ResItem(unevenLoadDistributionContext, detectionResItem, requestItem);
    return detectionResItem;
  }


  /**
   * 资源不均衡
   */
  private UnevenLoadDistributionContext unevenLoadDistributionDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    UnevenLoadDistributionContext unevenLoadDistributionContext = new UnevenLoadDistributionContext();
    Map<String, UnevenLoadDisItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及uneven load distribution

      UnevenLoadDisItem unevenLoadDisItem = new UnevenLoadDisItem();
      unevenLoadDisItem.setAvgLatency(entry.getValue().getAvgLatency());
      unevenLoadDisItem.setPct(0.8);
      unevenLoadDisItem.setMemory(1050L);


      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceFragileService = isInstanceUnevenLoadDistribution(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue());
      if (isInstanceFragileService) {
        flag = true;
      }
      unevenLoadDisItem.setStatus(isInstanceFragileService);
    }

    unevenLoadDistributionContext.setStatus(flag);
    log.info("map: {}",map);
    unevenLoadDistributionContext.setInstanceStatus(map);
    unevenLoadDistributionContext.setMetricsInterval(interval);
    return unevenLoadDistributionContext;
  }

  private boolean isInstanceUnevenLoadDistribution(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics) {
    double FTRTH = 0.05;
    double coefficient = 0.2;
    boolean existFailPercent = false;
    boolean existExecTimeOver = false;
    boolean existThroughputLow = false;
    if (intervalMetrics.getFailPercent()>FTRTH) {
      existFailPercent = true;
    }
    double ETThreshold = windowMetrics.getAvgLatency()*(1+coefficient);
    if (intervalMetrics.getAvgLatency()>ETThreshold){
      existExecTimeOver = true;
    }
    //request time per minute
    double TPThreshold = ((windowMetrics.getRequestCount()/(double)(windowMetrics.getInterval())*60.0))*coefficient;
    double intervalTP = (intervalMetrics.getRequestCount()/(double)(windowMetrics.getInterval())*60.0);
    if (intervalTP<TPThreshold){
      existThroughputLow = true;
    }
    return existFailPercent && existExecTimeOver && existThroughputLow;
  }

}
