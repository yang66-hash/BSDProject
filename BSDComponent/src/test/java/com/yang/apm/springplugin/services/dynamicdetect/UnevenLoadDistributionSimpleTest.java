package com.yang.apm.springplugin.services.dynamicdetect;

import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.JVMSummaryRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 不均衡负载检测简单测试
 */
@SpringBootTest
public class UnevenLoadDistributionSimpleTest {

    @Test
    public void testDataPreparation() {
        // 创建测试数据
        Map<String, SvcMetricsRes> windowMetrics = createWindowMetrics();
        Map<String, SvcExternalMetricsRes> windowExternalMetrics = createWindowExternalMetrics();
        
        System.out.println("=== 窗口平均指标计算测试 ===");
        
        // 计算CPU平均值
        double avgCpu = windowMetrics.values().stream()
                .flatMap(metric -> metric.getJvmSummaryList().stream())
                .mapToDouble(JVMSummaryRes::getCpuProPCT)
                .average()
                .orElse(0.0);
        
        // 计算内存平均值
        double avgMemory = windowMetrics.values().stream()
                .flatMap(metric -> metric.getJvmSummaryList().stream())
                .mapToLong(JVMSummaryRes::getHeapUsed)
                .average()
                .orElse(0.0);
        
        // 计算请求数总和和平均值
        int requestSum = windowExternalMetrics.values().stream()
                .mapToInt(SvcExternalMetricsRes::getRequestCount)
                .sum();
        double avgRequestCount = (double) requestSum / windowExternalMetrics.size();
        
        // 计算加权平均延时
        double latencySum = windowExternalMetrics.values().stream()
                .mapToDouble(external -> external.getAvgLatency() * external.getRequestCount())
                .sum();
        double avgLatency = latencySum / requestSum;
        
        System.out.printf("CPU平均值: %.2f%%\n", avgCpu);
        System.out.printf("内存平均值: %.0f bytes (%.0f MB)\n", avgMemory, avgMemory / (1024 * 1024));
        System.out.printf("请求数平均值: %.0f\n", avgRequestCount);
        System.out.printf("延时平均值: %.2f ms\n", avgLatency);
        
        // 计算阈值
        double coefficient = 0.2;
        double cpuThreshold = avgCpu * (1 + coefficient);
        double memoryThreshold = avgMemory * (1 + coefficient);
        double requestThreshold = avgRequestCount * (1 - coefficient);
        double latencyThreshold = avgLatency * (1 + coefficient);
        
        System.out.println("\n=== 检测阈值 ===");
        System.out.printf("CPU阈值: %.2f%% (平均值 * 1.2)\n", cpuThreshold);
        System.out.printf("内存阈值: %.0f MB (平均值 * 1.2)\n", memoryThreshold / (1024 * 1024));
        System.out.printf("请求数阈值: %.0f (平均值 * 0.8)\n", requestThreshold);
        System.out.printf("延时阈值: %.2f ms (平均值 * 1.2)\n", latencyThreshold);
        
        // 测试实例检测
        System.out.println("\n=== 实例检测结果 ===");
        testInstanceDetection("正常实例", 45.0, 400 * 1024 * 1024L, 1100, 95.0,
                cpuThreshold, memoryThreshold, requestThreshold, latencyThreshold);
        
        testInstanceDetection("不均衡负载实例", 80.0, 800 * 1024 * 1024L, 700, 150.0,
                cpuThreshold, memoryThreshold, requestThreshold, latencyThreshold);
    }
    
    private void testInstanceDetection(String instanceName, double cpu, long memory, int requests, double latency,
                                     double cpuThreshold, double memoryThreshold, double requestThreshold, double latencyThreshold) {
        
        boolean highCpu = cpu > cpuThreshold;
        boolean highMemory = memory > memoryThreshold;
        boolean lowRequests = requests < requestThreshold;
        boolean highLatency = latency > latencyThreshold;
        
        boolean isUnevenLoad = highCpu && highMemory && lowRequests && highLatency;
        
        System.out.printf("\n%s:\n", instanceName);
        System.out.printf("  CPU: %.2f%% %s (阈值: %.2f%%)\n", cpu, highCpu ? "高" : "正常", cpuThreshold);
        System.out.printf("  内存: %.0f MB %s (阈值: %.0f MB)\n", memory / (1024.0 * 1024.0), 
                highMemory ? "高" : "正常", memoryThreshold / (1024.0 * 1024.0));
        System.out.printf("  请求数: %d %s (阈值: %.0f)\n", requests, lowRequests ? "低" : "正常", requestThreshold);
        System.out.printf("  延时: %.2f ms %s (阈值: %.2f ms)\n", latency, highLatency ? "️高" : "正常", latencyThreshold);
        System.out.printf("  结果: %s\n", isUnevenLoad ? " 不均衡负载" : "正常");
    }
    
    private Map<String, SvcMetricsRes> createWindowMetrics() {
        Map<String, SvcMetricsRes> windowMetrics = new HashMap<>();
        
        // 实例1: 中等负载
        JVMSummaryRes jvm1 = new JVMSummaryRes();
        jvm1.setCpuProPCT(50.0);
        jvm1.setHeapUsed(512L * 1024L * 1024L); // 512MB
        
        SvcMetricsRes metrics1 = new SvcMetricsRes();
        metrics1.setJvmSummaryList(Arrays.asList(jvm1));
        
        // 实例2: 较高负载
        JVMSummaryRes jvm2 = new JVMSummaryRes();
        jvm2.setCpuProPCT(60.0);
        jvm2.setHeapUsed(600L * 1024L * 1024L); // 600MB
        
        SvcMetricsRes metrics2 = new SvcMetricsRes();
        metrics2.setJvmSummaryList(Arrays.asList(jvm2));
        
        windowMetrics.put("instance1", metrics1);
        windowMetrics.put("instance2", metrics2);
        
        return windowMetrics;
    }
    
    private Map<String, SvcExternalMetricsRes> createWindowExternalMetrics() {
        Map<String, SvcExternalMetricsRes> windowExternalMetrics = new HashMap<>();
        
        SvcExternalMetricsRes external1 = new SvcExternalMetricsRes();
        external1.setRequestCount(1000);
        external1.setAvgLatency(100.0);
        
        SvcExternalMetricsRes external2 = new SvcExternalMetricsRes();
        external2.setRequestCount(1200);
        external2.setAvgLatency(120.0);
        
        windowExternalMetrics.put("instance1", external1);
        windowExternalMetrics.put("instance2", external2);
        
        return windowExternalMetrics;
    }
} 