package com.yang.apm.springplugin.services.dynamicdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.dynamicres.UnevenLogicProContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.dynamic.UnevenLogicProItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
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
public class UnevenLogicProService implements IDetectConvert {

    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;
    @Autowired
    private AntiPatternItemService antiPatternItemService;
    @Autowired
    private CacheService cacheService;

    // 检测阈值配置
    private static final double EXTREME_CONCENTRATION_THRESHOLD = 0.8; // 极端集中阈值：单个方法占比超过80%
    private static final double HIGH_CONCENTRATION_THRESHOLD = 0.6; // 高度集中阈值：单个方法占比超过60%
    private static final double PARETO_ANOMALY_THRESHOLD = 0.95; // 帕累托异常阈值：前20%方法占比超过95%
    private static final double CONCENTRATION_SCORE_THRESHOLD = 70.0; // 集中度评分阈值：超过70分表示分布不均匀

    public DetectionResItem unevenLogicProDetect(RequestItem requestItem) {
        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.UNEVEN_LOGIC_PRO.getValue());
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
        
        //使用CacheService获取缓存数据
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        String serviceInternal = requestItem.getServiceName() + "|" + interval;
        
        //获取业务指标数据
        Map<String, List<BusinessMetricsRes>> businessMetricsMap = cacheService.getResInServiceLevel(
            ResType.BUSINESS_METRICS.name(), serviceInternal, 1, BusinessMetricsRes.class);

        //执行不均匀业务逻辑检测
        UnevenLogicProContext unevenLogicProContext = unevenLogicProDetectDetail(businessMetricsMap, interval);
        addNew2ResItem(unevenLogicProContext, detectionResItem, requestItem);
        return detectionResItem;
    }

    /**
     * 不均匀业务逻辑处理的具体检测逻辑
     * 将所有实例的方法调用数据聚合到服务级别进行分析
     */
    private UnevenLogicProContext unevenLogicProDetectDetail(
        Map<String, List<BusinessMetricsRes>> businessMetrics, 
        Integer interval) {
        UnevenLogicProContext unevenLogicProContext = new UnevenLogicProContext();
        
        // 1. 聚合所有实例的方法调用数据
        AggregationResult aggregationResult = aggregateMethodCallData(businessMetrics);
        Map<String, Long> aggregatedMethodCalls = aggregationResult.getAggregatedMethodCalls();
        List<String> validInstanceNames = aggregationResult.getValidInstanceNames();
        
        if (aggregatedMethodCalls.isEmpty()) {
            log.warn("未找到任何业务方法调用数据，跳过不均匀业务逻辑处理检测");
            unevenLogicProContext.setStatus(false);
            unevenLogicProContext.setInstanceStatus(new HashMap<>());
            unevenLogicProContext.setMetricsInterval(interval);
            return unevenLogicProContext;
        }
        
        // 2. 分析服务级别的方法调用分布
        UnevenLogicProItem serviceItem = analyzeMethodDistribution(aggregatedMethodCalls, validInstanceNames);
        
        // 3. 记录检测结果
        Map<String, UnevenLogicProItem> instanceStatus = new HashMap<>();
        instanceStatus.put("service-level", serviceItem); // 只记录服务级别的结果
        
        unevenLogicProContext.setStatus(serviceItem.getStatus());
        unevenLogicProContext.setInstanceStatus(instanceStatus);
        unevenLogicProContext.setMetricsInterval(interval);
        
        // 4. 记录详细日志
        logDetectionDetails(serviceItem);
        
        return unevenLogicProContext;
    }
    
    /**
     * 聚合所有实例的方法调用数据
     */
    private AggregationResult aggregateMethodCallData(Map<String, List<BusinessMetricsRes>> businessMetrics) {
        Map<String, Long> aggregatedMap = new HashMap<>();
        List<String> validInstanceNames = new ArrayList<>();
        
        for (Map.Entry<String, List<BusinessMetricsRes>> entry : businessMetrics.entrySet()) {
            String instanceKey = entry.getKey();
            List<BusinessMetricsRes> instanceData = entry.getValue();
            
            if (instanceData == null || instanceData.isEmpty()) {
                continue;
            }
            
            // 获取最新的指标数据
            BusinessMetricsRes latestMetrics = instanceData.get(instanceData.size() - 1);
            if (latestMetrics.getBusinessCallMap() == null || latestMetrics.getBusinessCallMap().isEmpty()) {
                continue;
            }
            
            // 记录有效实例
            String[] split = instanceKey.split("\\|");
            validInstanceNames.add(split[0]);
            
            // 累加方法调用次数
            for (Map.Entry<String, Long> methodEntry : latestMetrics.getBusinessCallMap().entrySet()) {
                String methodName = methodEntry.getKey();
                Long callCount = methodEntry.getValue();
                aggregatedMap.merge(methodName, callCount, Long::sum);
            }
        }
        
        return new AggregationResult(aggregatedMap, validInstanceNames);
    }
    
    /**
     * 分析方法调用分布情况
     */
    private UnevenLogicProItem analyzeMethodDistribution(
        Map<String, Long> aggregatedMethodCalls,
        List<String> validInstanceNames) {
        UnevenLogicProItem item = new UnevenLogicProItem();
        
        // 1. 基础统计
        int totalMethods = aggregatedMethodCalls.size();
        if (totalMethods < 3) {
            item.setStatus(false);
            item.setDetectionReason("方法数量过少（<3），无法进行不均衡检测");
            return item;
        }
        
        long totalCalls = aggregatedMethodCalls.values().stream().mapToLong(Long::longValue).sum();
        double avgCallCount = (double) totalCalls / totalMethods;
        
        // 2. 按调用次数降序排序
        List<Map.Entry<String, Long>> sortedMethods = aggregatedMethodCalls.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        // 3. 计算关键指标
        // 3.1 最热门方法占比
        Map.Entry<String, Long> hottestMethod = sortedMethods.get(0);
        double hottestMethodRatio = (double) hottestMethod.getValue() / totalCalls;
        
        // 3.2 帕累托指标（前20%方法的调用占比）
        int top20MethodCount = Math.max(1, (int) Math.ceil(totalMethods * 0.2));
        long top20Calls = sortedMethods.stream()
            .limit(top20MethodCount)
            .mapToLong(Map.Entry::getValue)
            .sum();
        double top20Ratio = (double) top20Calls / totalCalls;
        
        // 3.3 计算集中度评分
        double concentrationScore = calculateConcentrationScore(sortedMethods, totalCalls);
        
        // 4. 检测规则判定
        List<String> triggeredRules = new ArrayList<>();
        
        // 4.1 极端集中检测
        boolean hasExtremeConcentration = hottestMethodRatio > EXTREME_CONCENTRATION_THRESHOLD;
        if (hasExtremeConcentration) {
            triggeredRules.add(String.format("极端集中(%.1f%%)", hottestMethodRatio * 100));
        }
        
        // 4.2 高度集中检测
        boolean hasHighConcentration = hottestMethodRatio > HIGH_CONCENTRATION_THRESHOLD;
        if (hasHighConcentration) {
            triggeredRules.add(String.format("高度集中(%.1f%%)", hottestMethodRatio * 100));
        }
        
        // 4.3 帕累托异常检测
        boolean hasParetoAnomaly = top20Ratio > PARETO_ANOMALY_THRESHOLD;
        if (hasParetoAnomaly) {
            triggeredRules.add(String.format("帕累托异常(%.1f%%)", top20Ratio * 100));
        }
        
        // 4.4 集中度评分检测
        boolean hasHighScore = concentrationScore > CONCENTRATION_SCORE_THRESHOLD;
        if (hasHighScore) {
            triggeredRules.add(String.format("高集中度评分(%.1f)", concentrationScore));
        }
        
        // 5. 综合判定
        // 强条件：极端集中 或 帕累托异常
        boolean strongCondition = hasExtremeConcentration || hasParetoAnomaly;
        // 一般条件：满足任意两个条件
        boolean weakCondition = triggeredRules.size() >= 2;
        boolean isDetected = strongCondition || weakCondition;
        
        // 6. 设置检测结果
        item.setStatus(isDetected);
        item.setMaxCallCount(hottestMethod.getValue().intValue());
        item.setMinCallCount(sortedMethods.get(sortedMethods.size() - 1).getValue().intValue());
        item.setAvgCallCount(avgCallCount);
        item.setCallCountRange((double)(item.getMaxCallCount() - item.getMinCallCount()) / item.getMaxCallCount());
        
        // 记录前5个高负载方法
        List<String> highLoadMethods = sortedMethods.stream()
            .limit(5)
            .map(entry -> String.format("%s: %d次 (%.1f%%)", 
                entry.getKey(), 
                entry.getValue(), 
                (double) entry.getValue() / totalCalls * 100))
            .collect(Collectors.toList());
        item.setHighLoadMethods(highLoadMethods);
        
        if (isDetected) {
            item.setDetectionReason(String.format(
                "检测到不均匀业务逻辑处理问题 [参与统计实例数:%d] [总方法数:%d] [总调用量:%d] [触发规则:%s]",
                validInstanceNames.size(),
                totalMethods,
                totalCalls,
                String.join("; ", triggeredRules)));
        } else {
            item.setDetectionReason(String.format(
                "未检测到不均匀业务逻辑处理问题 [参与统计实例数:%d] [总方法数:%d] [总调用量:%d]",
                validInstanceNames.size(),
                totalMethods,
                totalCalls));
        }
        
        return item;
    }
    
    /**
     * 计算方法调用的集中度评分（基于基尼系数）
     * 评分范围：0-100，分数越高表示调用越集中
     */
    private double calculateConcentrationScore(List<Map.Entry<String, Long>> sortedMethods, long totalCalls) {
        List<Long> sortedCalls = sortedMethods.stream()
            .map(Map.Entry::getValue)
            .sorted()
            .collect(Collectors.toList());
            
        int n = sortedCalls.size();
        double giniSum = 0.0;
        
        for (int i = 0; i < n; i++) {
            giniSum += (2 * (i + 1) - n - 1) * sortedCalls.get(i);
        }
        
        double giniCoeff = giniSum / (n * totalCalls);
        return Math.abs(giniCoeff) * 100; // 转换为0-100分
    }
    
    /**
     * 记录详细的检测日志
     */
    private void logDetectionDetails(UnevenLogicProItem item) {
        if (item.getStatus()) {
            log.warn("服务级别业务逻辑分布检测结果: {}", item.getDetectionReason());
            log.warn("前5个高负载方法: {}", String.join("\n", item.getHighLoadMethods()));
        } else {
            log.info("服务级别业务逻辑分布检测结果: {}", item.getDetectionReason());
        }
    }
    
    /**
     * 聚合结果数据类
     */
    private static class AggregationResult {
        private final Map<String, Long> aggregatedMethodCalls;
        private final List<String> validInstanceNames;
        
        public AggregationResult(Map<String, Long> aggregatedMethodCalls, List<String> validInstanceNames) {
            this.aggregatedMethodCalls = aggregatedMethodCalls;
            this.validInstanceNames = validInstanceNames;
        }
        
        public Map<String, Long> getAggregatedMethodCalls() {
            return aggregatedMethodCalls;
        }
        
        public List<String> getValidInstanceNames() {
            return validInstanceNames;
        }
    }
} 