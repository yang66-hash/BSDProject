package com.yang.apm.springplugin.base.context.dynamicres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.dynamic.ChattyServiceItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
public class ChattyServiceContext extends ParentContext {

    //时间间隔
    private Integer metricsInterval;
    //当前时间间隔内
    @Schema(description = "统计该服务的每一个实例是否涉及到Chatty Service异味")
    private Map<String, ChattyServiceItem> instanceStatus;

} 