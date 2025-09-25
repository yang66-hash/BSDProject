package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.TheRampContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.TheRampItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.datacollector.CacheService;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TheRampService implements IDetectConvert {

    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;
    @Autowired
    private AntiPatternItemService antiPatternItemService;
    @Autowired
    private CacheService cacheService;

    // 检测阈值配置
    private static final double GROWTH_THRESHOLD = 0.2; // 连续增长阈值：20%
    private static final int MIN_CONSECUTIVE_INTERVALS = 3; // 最少连续增长区间数
    private static final double SIGNIFICANT_GROWTH_THRESHOLD = 0.5; // 显著增长阈值：50%

    public DetectionResItem theRampDetect(RequestItem requestItem) {
        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.THE_RAMP.getValue());
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
        
        //使用CacheService获取缓存数据
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        String serviceInternal = requestItem.getServiceName() + "|" + interval;
        
        //获取前4个时间窗口的外部指标数据
        Map<String, List<SvcExternalMetricsRes>> externalMetricsMap = cacheService.getResInServiceLevel(
            ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);

        //执行The Ramp检测
        TheRampContext theRampContext = theRampDetectDetail(externalMetricsMap, interval);
        addNew2ResItem(theRampContext, detectionResItem, requestItem);
        return detectionResItem;
    }

    /**
     * The Ramp的具体检测逻辑
     * 分析每个API在连续时间窗口下的处理时间变化趋势
     */
    private TheRampContext theRampDetectDetail(
        Map<String, List<SvcExternalMetricsRes>> externalMetrics, 
        Integer interval) {
        TheRampContext theRampContext = new TheRampContext();
        Map<String, TheRampItem> instanceStatus = new HashMap<>();
        boolean hasRampIssue = false;

        for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : externalMetrics.entrySet()) {
            String instanceKey = entry.getKey();
            List<SvcExternalMetricsRes> metrics = entry.getValue();
            
            // 解析实例名（去掉时间后缀）
            String[] split = instanceKey.split("\\|");
            String realInstanceKey = split[0];
            
            if (metrics == null || metrics.size() < MIN_CONSECUTIVE_INTERVALS) {
                log.warn("实例 {} 的数据点不足 {} 个，跳过检测", realInstanceKey, MIN_CONSECUTIVE_INTERVALS);
                continue;
            }

            // 按时间顺序排序（最新的在最后）
            metrics.sort(Comparator.comparing(SvcExternalMetricsRes::getEndTime));
            
            // 检测实例级别的The Ramp问题
            TheRampItem rampItem = detectInstanceRamp(metrics);
            
            if (rampItem.getStatus()) {
                hasRampIssue = true;
                log.warn("检测到实例 {} 存在The Ramp问题: {}", realInstanceKey, rampItem.getDetectionReason());
            }
            
            instanceStatus.put(realInstanceKey, rampItem);
        }

        theRampContext.setStatus(hasRampIssue);
        theRampContext.setInstanceStatus(instanceStatus);
        theRampContext.setMetricsInterval(interval);
        
        return theRampContext;
    }

    /**
     * 检测单个实例的The Ramp问题
     */
    private TheRampItem detectInstanceRamp(List<SvcExternalMetricsRes> metrics) {
        TheRampItem item = new TheRampItem();
        item.setStatus(false);
        
        // 1. 提取每个时间窗口的平均响应时间
        List<Double> avgLatencies = metrics.stream()
            .map(SvcExternalMetricsRes::getAvgLatency)
            .collect(Collectors.toList());
        
        // 2. 计算连续增长情况
        List<Double> growthRates = new ArrayList<>();
        List<String> rampDetails = new ArrayList<>();
        int consecutiveGrowth = 0;
        double totalGrowthRate = 0;
        
        for (int i = 1; i < avgLatencies.size(); i++) {
            double current = avgLatencies.get(i);
            double previous = avgLatencies.get(i - 1);
            double growthRate = (current - previous) / previous;
            growthRates.add(growthRate);
            
            String detail = String.format("区间%d→%d: %.2fms → %.2fms (增长%.1f%%)", 
                i, i+1, previous, current, growthRate * 100);
            rampDetails.add(detail);
            
            if (growthRate > GROWTH_THRESHOLD) {
                consecutiveGrowth++;
                totalGrowthRate += growthRate;
            } else {
                consecutiveGrowth = 0;
                totalGrowthRate = 0;
            }
        }
        
        // 3. 判断是否存在The Ramp问题
        boolean hasConsecutiveGrowth = consecutiveGrowth >= MIN_CONSECUTIVE_INTERVALS - 1;
        boolean hasSignificantGrowth = totalGrowthRate > SIGNIFICANT_GROWTH_THRESHOLD;
        
        if (hasConsecutiveGrowth && hasSignificantGrowth) {
            item.setStatus(true);
            item.setConsecutiveGrowthCount(consecutiveGrowth);
            item.setTotalGrowthRate(totalGrowthRate);
            item.setLatencyTrend(rampDetails);
            item.setDetectionReason(String.format(
                "检测到The Ramp问题：连续%d个区间响应时间增长超过%.0f%%，总增长率%.1f%%",
                consecutiveGrowth + 1,
                GROWTH_THRESHOLD * 100,
                totalGrowthRate * 100));
        } else {
            item.setConsecutiveGrowthCount(0);
            item.setTotalGrowthRate(0.0);
            item.setLatencyTrend(rampDetails);
            item.setDetectionReason("未检测到The Ramp问题");
        }
        
        return item;
    }
} 