package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.pojo.AntiPatternItem;

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
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        detectionResItem.setTimestamp(df.format(time));
        detectionResItem.setContext(context);
        detectionResItem.setStatus(context.getStatus());
    }
}
