package com.yang.apm.springplugin.services.datacollector;

import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.result.SvcTransRes;
import com.yang.apm.springplugin.pojo.result.chain.RequestChain;
import com.yang.apm.springplugin.pojo.result.chain.TraceChainNode;
import com.yang.apm.springplugin.pojo.traces.TraceServiceInfo;
import com.yang.apm.springplugin.utils.IndexUtil;
import com.yang.apm.springplugin.utils.TransactionUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 循环依赖检测服务
 * 负责异步检测链路中的循环依赖并缓存异常数据
 */
@Service
@Slf4j
public class CircularDependencyDetectionService {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MetricsBufferService metricsBufferService;

    /**
     * 异步检测循环依赖
     * @param traceListMap 原始 trace 数据
     * @param endTime 结束时间
     * @param startTime 开始时间
     * @param interval 时间间隔
     * @return CompletableFuture<Integer> 返回检测到的循环依赖数量
     */
    @Async("circularDependencyTaskExecutor")
    public CompletableFuture<Integer> detectCircularDependencyAsync(
            Map<String, List<TraceServiceInfo>> traceListMap, 
            Date endTime, Date startTime, Integer interval) {
        
        log.info("开始异步检测循环依赖，trace 数量: {}", traceListMap.size());
        
        try {
            // 1. 分析 trace 数据生成链路信息
            List<SvcTransRes> traces = TransactionUtils.analyzeTransaction4Trace(
                traceListMap, endTime, startTime, interval);
            log.info("分析得到 {} 条链路信息", traces.size());

            // 2. 检测循环依赖
            int circularDependencyCount = 0;
            List<SvcTransRes> anomalousTraces = new ArrayList<>();
            List<Map<String, Object>> detectionResults = new ArrayList<>();
            
            for (SvcTransRes trace : traces) {
                Map<String, Object> traceResult = new LinkedHashMap<>();
                traceResult.put("serviceName", trace.getServiceName());
                traceResult.put("podName", trace.getPodName());
                traceResult.put("language", trace.getLanguage());
                traceResult.put("interval", trace.getInterval());
                traceResult.put("traceId", trace.getRequestChain() != null ? trace.getRequestChain().getTraceId() : "unknown");
                
                if (hasCircularDependency(trace)) {
                    anomalousTraces.add(trace);
                    circularDependencyCount++;
                    traceResult.put("hasCircularDependency", true);
                    traceResult.put("circularPaths", extractCircularPaths(trace));
                    
                    log.warn("检测到循环依赖 - 服务: {}, Pod: {}, TraceId: {}",
                        trace.getServiceName(), trace.getPodName(),
                        trace.getRequestChain() != null ? trace.getRequestChain().getTraceId() : "unknown");
                } else {
                    traceResult.put("hasCircularDependency", false);
                }
                
                detectionResults.add(traceResult);
            }
            

            // 3. 仅缓存和存储异常数据
            if (!anomalousTraces.isEmpty()) {
                // 缓存数据
                cacheService.saveT2Cache(anomalousTraces, ResType.CIRCULAR_DEPENDENCY_TRACES.name());
                //将数据也往bsd.detection.results索引中发送一份

                // 发送到 ES - 这是关键步骤！
                String indexName = IndexUtil.getCircularDependencyTracesIndex();
                metricsBufferService.addList2Index(anomalousTraces,indexName);
                
                log.info("缓存了 {} 条循环依赖异常数据，并发送到索引: {}", anomalousTraces.size(), indexName);
            }

            // 4. 记录检测统计信息
            recordDetectionStats(traces.size(), circularDependencyCount);

            return CompletableFuture.completedFuture(circularDependencyCount);
            
        } catch (Exception e) {
            log.error("循环依赖检测过程中发生异常", e);
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * 检测单个链路是否存在循环依赖
     * @param trace 链路数据
     * @return true 如果存在循环依赖
     */
    private boolean hasCircularDependency(SvcTransRes trace) {
        if (trace.getRequestChain() == null) {
            return false;
        }
        
        // 直接检测单个请求链
        return detectCycleInChain(trace.getRequestChain());
    }

    /**
     * 在单个请求链中检测循环
     * @param chain 请求链
     * @return true 如果存在循环
     */
    private boolean detectCycleInChain(RequestChain chain) {
        if (chain.getChain() == null) {
            return false;
        }
        
        // 使用 DFS + 路径记录来检测环
        Set<String> visited = new HashSet<>();
        Set<String> path = new HashSet<>();
        
        return dfsDetectCycle(chain.getChain(), visited, path);
    }

        /**
     * 使用深度优先搜索检测环
     * @param node 当前节点
     * @param visited 已访问节点（全局）
     * @param path 当前路径中的节点（用于检测循环）
     * @return true 如果发现环
     */
    private boolean dfsDetectCycle(TraceChainNode node, Set<String> visited, Set<String> path) {
        if (node == null) {
            return false;
        }
        
        String nodeId = generateNodeId(node);
        
        // 关键检测点：如果当前路径中已包含此节点，说明找到了环
        if (path.contains(nodeId)) {
            log.warn("发现循环依赖: 节点 {} 在当前路径中重复出现", nodeId);
            log.warn("循环路径: {}", String.join(" -> ", path) + " -> " + nodeId);
            return true;
        }
        
        // 优化：如果已经访问过且没有找到环，直接返回
        if (visited.contains(nodeId)) {
            return false;
        }
        
        // 标记为已访问和当前路径中
        visited.add(nodeId);
        path.add(nodeId);
        
        log.debug("访问节点: {} (路径深度: {})", nodeId, path.size());
        
        // 递归检查子节点
        if (node.getSubNodes() != null && !node.getSubNodes().isEmpty()) {
            for (TraceChainNode child : node.getSubNodes()) {
                if (dfsDetectCycle(child, visited, path)) {
                    return true;
                }
            }
        }
        
        // 回溯：从当前路径中移除
        path.remove(nodeId);
        
        return false;
    }

        /**
     * 生成节点唯一标识
     * @param node 链路节点
     * @return 节点ID
     */
    private String generateNodeId(TraceChainNode node) {
        // 从你的数据结构看，serviceName 格式是 "Java|cloud-user-service|win-20230627bhi"
        // 我们主要关注服务名部分来检测循环依赖
        String serviceName = extractServiceName(node.getServiceName());
        String apiName = node.getAPIName() != null ? node.getAPIName() : "unknown";
        
        // 根据检测粒度选择不同的标识策略
        return generateNodeIdByLevel(serviceName, apiName, "SERVICE_LEVEL"); // 可配置检测级别
    }
    
    /**
     * 从完整服务名中提取核心服务名
     * 例如: "Java|cloud-user-service|win-20230627bhi" -> "cloud-user-service"
     */
    private String extractServiceName(String fullServiceName) {
        if (fullServiceName == null || fullServiceName.isEmpty()) {
            return "unknown";
        }
        
        String[] parts = fullServiceName.split("\\|");
        if (parts.length >= 2) {
            return parts[1]; // 返回服务名部分
        }
        
        return fullServiceName;
    }
    
    /**
     * 根据检测级别生成节点ID
     * @param serviceName 服务名
     * @param apiName API名
     * @param level 检测级别
     * @return 节点ID
     */
    private String generateNodeIdByLevel(String serviceName, String apiName, String level) {
        switch (level) {
            case "SERVICE_LEVEL":
                // 服务级别：只关注服务间的循环调用
                return serviceName;
                
            case "API_LEVEL":
                // API级别：关注具体API间的循环调用
                return String.format("%s:%s", serviceName, apiName);
                
            case "STRICT_LEVEL":
                // 严格级别：同一服务的不同API也视为不同节点
                return String.format("%s::%s", serviceName, apiName);
                
            default:
                return String.format("%s:%s", serviceName, apiName);
        }
    }

    /**
     * 记录检测统计信息
     * @param totalTraces 总链路数
     * @param circularDependencyCount 循环依赖数量
     */
    private void recordDetectionStats(int totalTraces, int circularDependencyCount) {
        double percentage = totalTraces > 0 ? (double) circularDependencyCount / totalTraces * 100 : 0;
        
        log.info("循环依赖检测统计 - 总链路: {}, 循环依赖: {}, 比例: {:.2f}%", 
            totalTraces, circularDependencyCount, percentage);
        
        // 可以添加更多统计信息到监控系统
        // metricsRegistry.counter("circular.dependency.detected").increment(circularDependencyCount);
        // metricsRegistry.gauge("circular.dependency.percentage").set(percentage);
    }

    /**
     * 同步版本的检测方法（用于测试或特殊场景）
     * @param traces 链路列表
     * @return 检测到的循环依赖数量
     */
    public int detectCircularDependencySync(List<SvcTransRes> traces) {
        int count = 0;
        for (SvcTransRes trace : traces) {
            if (hasCircularDependency(trace)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 提取循环路径信息（用于 JSON 输出）
     * @param trace 链路数据
     * @return 循环路径列表
     */
    private List<Map<String, Object>> extractCircularPaths(SvcTransRes trace) {
        List<Map<String, Object>> circularPaths = new ArrayList<>();

        if (trace.getRequestChain() == null) {
            return circularPaths;
        }

        // 直接处理单个 RequestChain
        RequestChain chain = trace.getRequestChain();
        if (chain.getChain() != null) {
            List<String> path = new ArrayList<>();
            Set<String> visited = new HashSet<>();

            List<String> cyclePath = findCyclePath(chain.getChain(), visited, path);
            if (!cyclePath.isEmpty()) {
                Map<String, Object> cycleInfo = new LinkedHashMap<>();
                cycleInfo.put("traceId", chain.getTraceId());
                cycleInfo.put("sourceSvc", chain.getSourceSvc());
                cycleInfo.put("cyclePath", cyclePath);
                cycleInfo.put("cycleLength", cyclePath.size());

                circularPaths.add(cycleInfo);
            }
        }

        return circularPaths;
    }

    /**
     * 查找并返回循环路径
     * @param node 起始节点
     * @param visited 已访问节点
     * @param currentPath 当前路径
     * @return 循环路径（如果存在）
     */
    private List<String> findCyclePath(TraceChainNode node, Set<String> visited, List<String> currentPath) {
        if (node == null) {
            return new ArrayList<>();
        }

        String nodeId = generateNodeId(node);

        // 如果在当前路径中找到重复节点，返回循环路径
        int cycleStartIndex = currentPath.indexOf(nodeId);
        if (cycleStartIndex != -1) {
            List<String> cyclePath = new ArrayList<>(currentPath.subList(cycleStartIndex, currentPath.size()));
            cyclePath.add(nodeId); // 添加重复节点完成循环
            return cyclePath;
        }

        if (visited.contains(nodeId)) {
            return new ArrayList<>();
        }

        visited.add(nodeId);
        currentPath.add(nodeId);

        // 递归检查子节点
        if (node.getSubNodes() != null) {
            for (TraceChainNode child : node.getSubNodes()) {
                List<String> result = findCyclePath(child, visited, currentPath);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        // 回溯
        currentPath.remove(currentPath.size() - 1);

        return new ArrayList<>();
    }

} 