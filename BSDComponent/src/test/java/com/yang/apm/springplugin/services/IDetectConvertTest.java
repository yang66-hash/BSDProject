package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * IDetectConvert接口的空指针异常处理测试
 */
public class IDetectConvertTest implements IDetectConvert {

    private DetectionResItem detectionResItem;
    private RequestItem requestItem;

    @BeforeEach
    void setUp() {
        detectionResItem = new DetectionResItem();
        requestItem = new RequestItem();
        requestItem.setServiceName("test-service");
        requestItem.setDetectionID("test-detection-123");
    }

    @Test
    void testAddNew2ResItemWithNullContext() {
        // 测试context为null的情况
        assertDoesNotThrow(() -> {
            addNew2ResItem(null, detectionResItem, requestItem);
        });
        
        // 验证status被正确设置为false
        assertFalse(detectionResItem.getStatus());
        assertEquals("test-service", detectionResItem.getTargetInstance());
        assertEquals("test-detection-123", detectionResItem.getDetectionID());
        assertNotNull(detectionResItem.getTimestamp());
        assertNull(detectionResItem.getContext());
    }

    @Test
    void testAddNew2ResItemWithValidContext() {
        // 创建一个正常的context
        ParentContext context = new ParentContext();
        context.setStatus(true);
        
        assertDoesNotThrow(() -> {
            addNew2ResItem(context, detectionResItem, requestItem);
        });
        
        // 验证status被正确设置
        assertTrue(detectionResItem.getStatus());
        assertEquals("test-service", detectionResItem.getTargetInstance());
        assertEquals("test-detection-123", detectionResItem.getDetectionID());
        assertNotNull(detectionResItem.getTimestamp());
        assertNotNull(detectionResItem.getContext());
    }

    @Test
    void testAddNew2ResItemWithContextHavingNullStatus() {
        // 创建一个status为null的context
        ParentContext context = new ParentContext();
        context.setStatus(null);
        
        assertDoesNotThrow(() -> {
            addNew2ResItem(context, detectionResItem, requestItem);
        });
        
        // 验证status被设置为默认值false
        assertFalse(detectionResItem.getStatus());
    }

    @Test
    void testAddNew2ResItemWithNullDetectionResItem() {
        ParentContext context = new ParentContext();
        
        // 测试detectionResItem为null的情况
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            addNew2ResItem(context, null, requestItem);
        });
        
        assertEquals("DetectionResItem不能为null", exception.getMessage());
    }

    @Test
    void testAddNew2ResItemWithNullRequestItem() {
        ParentContext context = new ParentContext();
        
        // 测试requestItem为null的情况
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            addNew2ResItem(context, detectionResItem, null);
        });
        
        assertEquals("RequestItem不能为null", exception.getMessage());
    }

    @Test
    void testGetSafeStatusWithNullContext() {
        // 测试getSafeStatus方法对null context的处理
        Boolean status = getSafeStatus(null);
        assertFalse(status);
    }

    @Test
    void testGetSafeStatusWithValidContext() {
        ParentContext context = new ParentContext();
        context.setStatus(true);
        
        Boolean status = getSafeStatus(context);
        assertTrue(status);
    }

    @Test
    void testGetSafeStatusWithContextHavingNullStatus() {
        ParentContext context = new ParentContext();
        context.setStatus(null);
        
        Boolean status = getSafeStatus(context);
        assertFalse(status);
    }
} 