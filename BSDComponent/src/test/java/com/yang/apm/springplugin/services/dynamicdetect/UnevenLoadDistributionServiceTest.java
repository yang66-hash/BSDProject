package com.yang.apm.springplugin.services.dynamicdetect;

import com.yang.apm.springplugin.base.context.dynamicres.UnevenLoadDistributionContext;
import com.yang.apm.springplugin.base.item.dynamic.UnevenLoadDisItem;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.JVMSummaryRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 不均衡负载检测服务测试
 */
public class UnevenLoadDistributionServiceTest {

    @InjectMocks
    private UnevenLoadDistributionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUnevenLoadDistributionDetection() throws Exception {
        // 准备测试数据
        Map<String, SvcMetricsRes> windowMetrics = createTestWindowMetrics();
        Map<String, SvcMetricsRes> intervalMetrics = createTestIntervalMetrics();
        Map<String, SvcExternalMetricsRes> windowExternalMetrics = createTestWindowExternalMetrics();
        Map<String, SvcExternalMetricsRes> intervalExternalMetrics = createTestIntervalExternalMetrics();

        // 使用反射调用私有方法
        Method method = UnevenLoadDistributionService.class.getDeclaredMethod(
                "unevenLoadDistributionDetectDetail",
                Map.class, Map.class, Map.class, Map.class, Integer.class);
        method.setAccessible(true);

        UnevenLoadDistributionContext result = (UnevenLoadDistributionContext) method.invoke(
                service, windowMetrics, intervalMetrics, windowExternalMetrics, intervalExternalMetrics, 300);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getInstanceStatus());
        
        // 验证实例检测结果
        Map<String, UnevenLoadDisItem> instanceStatus = result.getInstanceStatus();
        
        // 实例1：正常实例
        UnevenLoadDisItem normalInstance = instanceStatus.get("instance1");
        assertNotNull(normalInstance);
        assertFalse(normalInstance.getStatus(), "正常实例不应该被标记为不均衡负载");
        
        // 实例2：不均衡负载实例
        UnevenLoadDisItem unevenInstance = instanceStatus.get("instance2");
        assertNotNull(unevenInstance);
        assertTrue(unevenInstance.getStatus(), "不均衡负载实例应该被正确识别");
        
        // 整体状态应该为true（存在不均衡负载）
        assertTrue(result.getStatus(), "检测到不均衡负载时，整体状态应该为true");
    }

    /**
     * 创建测试用的窗口内部指标数据
     */
    private Map<String, SvcMetricsRes> createTestWindowMetrics() {
        Map<String, SvcMetricsRes> windowMetrics = new HashMap<>();
        
        // 创建平均水平的JVM指标
        JVMSummaryRes avgJvm1 = new JVMSummaryRes();
        avgJvm1.setCpuProPCT(50.0); // 50% CPU
        avgJvm1.setHeapUsed(1024L * 1024L * 512L); // 512MB
        
        JVMSummaryRes avgJvm2 = new JVMSummaryRes();
        avgJvm2.setCpuProPCT(60.0); // 60% CPU  
        avgJvm2.setHeapUsed(1024L * 1024L * 600L); // 600MB
        
        SvcMetricsRes metrics1 = new SvcMetricsRes();
        metrics1.setJvmSummaryList(Arrays.asList(avgJvm1));
        
        SvcMetricsRes metrics2 = new SvcMetricsRes();
        metrics2.setJvmSummaryList(Arrays.asList(avgJvm2));
        
        windowMetrics.put("instance1", metrics1);
        windowMetrics.put("instance2", metrics2);
        
        return windowMetrics;
    }

    /**
     * 创建测试用的当前间隔内部指标数据
     */
    private Map<String, SvcMetricsRes> createTestIntervalMetrics() {
        Map<String, SvcMetricsRes> intervalMetrics = new HashMap<>();
        
        // 实例1：正常实例 - CPU和内存正常
        JVMSummaryRes normalJvm = new JVMSummaryRes();
        normalJvm.setCpuProPCT(45.0); // 45% CPU (低于阈值)
        normalJvm.setHeapUsed(1024L * 1024L * 400L); // 400MB (低于阈值)
        
        SvcMetricsRes normalMetrics = new SvcMetricsRes();
        normalMetrics.setJvmSummaryList(Arrays.asList(normalJvm));
        
        // 实例2：不均衡负载实例 - CPU和内存都高
        JVMSummaryRes overloadedJvm = new JVMSummaryRes();
        overloadedJvm.setCpuProPCT(80.0); // 80% CPU (高于阈值 55*1.2=66%)
        overloadedJvm.setHeapUsed(1024L * 1024L * 800L); // 800MB (高于阈值 556*1.2=667MB)
        
        SvcMetricsRes overloadedMetrics = new SvcMetricsRes();
        overloadedMetrics.setJvmSummaryList(Arrays.asList(overloadedJvm));
        
        intervalMetrics.put("instance1", normalMetrics);
        intervalMetrics.put("instance2", overloadedMetrics);
        
        return intervalMetrics;
    }

    /**
     * 创建测试用的窗口外部指标数据
     */
    private Map<String, SvcExternalMetricsRes> createTestWindowExternalMetrics() {
        Map<String, SvcExternalMetricsRes> windowExternalMetrics = new HashMap<>();
        
        SvcExternalMetricsRes external1 = new SvcExternalMetricsRes();
        external1.setRequestCount(1000); // 1000 requests
        external1.setAvgLatency(100.0); // 100ms
        
        SvcExternalMetricsRes external2 = new SvcExternalMetricsRes();
        external2.setRequestCount(1200); // 1200 requests
        external2.setAvgLatency(120.0); // 120ms
        
        windowExternalMetrics.put("instance1", external1);
        windowExternalMetrics.put("instance2", external2);
        
        return windowExternalMetrics;
    }

    /**
     * 创建测试用的当前间隔外部指标数据
     */
    private Map<String, SvcExternalMetricsRes> createTestIntervalExternalMetrics() {
        Map<String, SvcExternalMetricsRes> intervalExternalMetrics = new HashMap<>();
        
        // 实例1：正常实例 - 请求数正常，延时正常
        SvcExternalMetricsRes normalExternal = new SvcExternalMetricsRes();
        normalExternal.setRequestCount(1100); // 正常请求数
        normalExternal.setAvgLatency(95.0); // 正常延时
        
        // 实例2：不均衡负载实例 - 请求数低，延时高
        SvcExternalMetricsRes unevenExternal = new SvcExternalMetricsRes();
        unevenExternal.setRequestCount(800); // 低请求数 (低于阈值 1100*0.8=880)
        unevenExternal.setAvgLatency(150.0); // 高延时 (高于阈值 110*1.2=132ms)
        
        intervalExternalMetrics.put("instance1", normalExternal);
        intervalExternalMetrics.put("instance2", unevenExternal);
        
        return intervalExternalMetrics;
    }

    @Test
    void testEmptyMetrics() throws Exception {
        // 测试空指标数据
        Map<String, SvcMetricsRes> emptyMetrics = new HashMap<>();
        
        Method method = UnevenLoadDistributionService.class.getDeclaredMethod(
                "unevenLoadDistributionDetectDetail",
                Map.class, Map.class, Map.class, Map.class, Integer.class);
        method.setAccessible(true);

        UnevenLoadDistributionContext result = (UnevenLoadDistributionContext) method.invoke(
                service, emptyMetrics, emptyMetrics, new HashMap<>(), new HashMap<>(), 300);

        assertNotNull(result);
        assertFalse(result.getStatus());
        assertTrue(result.getInstanceStatus() == null || result.getInstanceStatus().isEmpty());
    }

    @Test
    void testSingleInstance() throws Exception {
        // 测试单实例情况（应该返回空结果）
        Map<String, SvcMetricsRes> singleInstance = new HashMap<>();
        singleInstance.put("instance1", new SvcMetricsRes());
        
        Method method = UnevenLoadDistributionService.class.getDeclaredMethod(
                "unevenLoadDistributionDetectDetail",
                Map.class, Map.class, Map.class, Map.class, Integer.class);
        method.setAccessible(true);

        UnevenLoadDistributionContext result = (UnevenLoadDistributionContext) method.invoke(
                service, singleInstance, singleInstance, new HashMap<>(), new HashMap<>(), 300);

        assertNotNull(result);
        assertFalse(result.getStatus());
    }
} 