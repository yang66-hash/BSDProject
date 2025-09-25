package com.yang.apm.springplugin.base.context.dynamicres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.dynamic.UnevenApiUsageItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Uneven API Usage 检测上下文
 * 针对微服务级别的API使用不均检测
 */
@Data
public class UnevenApiUsageContext extends ParentContext {

    //时间间隔
    private Integer metricsInterval;
    
    //微服务级别的API使用不均检测结果（聚合所有实例数据）
    private UnevenApiUsageItem microserviceApiUsage;

} 