package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public interface IDetectConvert {
    default DetectionResItem convertToResItem(AntiPatternItem antiPatternItem) {
        DetectionResItem detectionResItem = new DetectionResItem();
        detectionResItem.setName(antiPatternItem.getName());
        detectionResItem.setCategoryName(antiPatternItem.getCategoryName());
        detectionResItem.setTypeName(antiPatternItem.getTypeName());
        detectionResItem.setDetector(ManagementFactory.getRuntimeMXBean().getName());
        detectionResItem.setDetectMethod(antiPatternItem.getDetectMethod());
        return detectionResItem;
    }

    default <T extends ParentContext> void addNew2ResItem(T context, DetectionResItem detectionResItem, RequestItem requestItem) {
        detectionResItem.setTargetInstance(requestItem.getServiceName());
        detectionResItem.setDetectionID(requestItem.getDetectionID());
        //设置ISO 8601格式日期
        Date time = new Date();
//        TimeZone tz = TimeZone.getTimeZone("UTC");
//        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
//        df.setTimeZone(tz);
        detectionResItem.setTimestamp(time);
        detectionResItem.setContext(context);
        
        // 防止空指针异常：安全地获取status
        Boolean status = getSafeStatus(context);
        detectionResItem.setStatus(status);
    }
    
    /**
     * 安全地获取context的status，防止空指针异常
     * @param context 可能为null的context对象
     * @return 安全的status值，默认为false
     */
    default <T extends ParentContext> Boolean getSafeStatus(T context) {
        if (context != null) {
            Boolean status = context.getStatus();
            // 如果context.getStatus()返回null，使用默认值false
            return (status != null) ? status : false;
        } else {
            // context为null时，默认状态为false
            return false;
        }
    }
}
