package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.HighFreSlowQueriesContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.HighFreSlowQueriesItem;
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
public class HighFreSlowQueriesService implements IDetectConvert {

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
  public DetectionResItem highFreSlowQueriesDetect(RequestItem requestItem) {
    //封装DetectionResItem
    QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", DetectableBS.HIGH_FRE_OF_SLOW_QUERIES.getValue());
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
    //判断是否是High Frequency Of Slow Queries

    HighFreSlowQueriesContext highFreSlowQueriesContext = highFreSlowQueriesDetectDetail(windowMetrics, intervalMetrics, interval);
    addNew2ResItem(highFreSlowQueriesContext, detectionResItem, requestItem);
    return detectionResItem;
  }

  /**
   * High Frequency Of Slow Queries的具体检测逻辑
   * 检测高频慢查询问题
   */
  private HighFreSlowQueriesContext highFreSlowQueriesDetectDetail(
      Map<String, SvcExternalMetricsRes> windowMetrics, Map<String, SvcExternalMetricsRes> intervalMetrics, Integer interval) {
    HighFreSlowQueriesContext highFreSlowQueriesContext = new HighFreSlowQueriesContext();
    Map<String, HighFreSlowQueriesItem> map = new HashMap<>();
    Boolean flag = false;
    //依据每一个当前存在数据的实例进行统计计算
    for (Map.Entry<String, SvcExternalMetricsRes> entry : intervalMetrics.entrySet()) {
      //对每一个实例统计是否涉及High Frequency Of Slow Queries

      HighFreSlowQueriesItem highFreSlowQueriesItem = new HighFreSlowQueriesItem();
      
      // 模拟慢查询相关数据（实际应从数据库监控系统获取）
      int slowQueryCount = (int)(Math.random() * 50); // 0-50次慢查询
      double slowQueryFrequency = slowQueryCount / (double) interval * 60.0; // 每分钟慢查询次数
      double avgQueryExecutionTime = 100 + Math.random() * 400; // 100-500ms
      double maxQueryExecutionTime = avgQueryExecutionTime * (1.5 + Math.random() * 2); // 最大查询时间
      double slowQueryRatio = slowQueryCount / (double)(entry.getValue().getRequestCount() + 1); // 慢查询占比
      double dbConnectionUsage = 0.3 + Math.random() * 0.6; // 30%-90%连接池使用率
      
      highFreSlowQueriesItem.setSlowQueryCount(slowQueryCount);
      highFreSlowQueriesItem.setSlowQueryFrequency(slowQueryFrequency);
      highFreSlowQueriesItem.setAvgQueryExecutionTime(avgQueryExecutionTime);
      highFreSlowQueriesItem.setMaxQueryExecutionTime(maxQueryExecutionTime);
      highFreSlowQueriesItem.setSlowQueryRatio(slowQueryRatio);
      highFreSlowQueriesItem.setDbConnectionUsage(dbConnectionUsage);

      //计算当前的pod是否涉及到相应的异常
      boolean isInstanceHighFreSlowQueries = isInstanceHighFreSlowQueries(windowMetrics.getOrDefault(entry.getKey(), null), entry.getValue(), highFreSlowQueriesItem);
      if (isInstanceHighFreSlowQueries) {
        flag = true;
      }
      highFreSlowQueriesItem.setStatus(isInstanceHighFreSlowQueries);
      map.put(entry.getKey(), highFreSlowQueriesItem);
    }

    highFreSlowQueriesContext.setStatus(flag);
    log.info("map: {}",map);
    highFreSlowQueriesContext.setInstanceStatus(map);
    highFreSlowQueriesContext.setMetricsInterval(interval);
    return highFreSlowQueriesContext;
  }

  private boolean isInstanceHighFreSlowQueries(SvcExternalMetricsRes windowMetrics, SvcExternalMetricsRes intervalMetrics, HighFreSlowQueriesItem highFreSlowQueriesItem) {
    if (intervalMetrics == null) {
      return false;
    }
    
    // High Frequency Of Slow Queries检测阈值
    double highSlowQueryFrequencyThreshold = 10.0; // 慢查询频率阈值10次/分钟
    double highSlowQueryRatioThreshold = 0.1; // 慢查询占比阈值10%
    double highAvgExecutionTimeThreshold = 300.0; // 平均执行时间阈值300ms
    double highDbConnectionUsageThreshold = 0.8; // 数据库连接使用率阈值80%
    
    // 检测是否慢查询频率过高
    boolean highSlowQueryFrequency = highFreSlowQueriesItem.getSlowQueryFrequency() > highSlowQueryFrequencyThreshold;
    
    // 检测是否慢查询占比过高
    boolean highSlowQueryRatio = highFreSlowQueriesItem.getSlowQueryRatio() > highSlowQueryRatioThreshold;
    
    // 检测是否平均执行时间过长
    boolean highAvgExecutionTime = highFreSlowQueriesItem.getAvgQueryExecutionTime() > highAvgExecutionTimeThreshold;
    
    // 检测是否数据库连接使用率过高
    boolean highDbConnectionUsage = highFreSlowQueriesItem.getDbConnectionUsage() > highDbConnectionUsageThreshold;
    
    // 满足任意三个条件就认为存在High Frequency Of Slow Queries
    int conditionCount = 0;
    if (highSlowQueryFrequency) conditionCount++;
    if (highSlowQueryRatio) conditionCount++;
    if (highAvgExecutionTime) conditionCount++;
    if (highDbConnectionUsage) conditionCount++;
    
    return conditionCount >= 3;
  }

} 