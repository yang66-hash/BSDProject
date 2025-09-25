package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class UnevenLogicProItem {
    @Schema(description = "当前服务实例是否存在不均匀业务逻辑处理问题")
    private Boolean status;
    
    @Schema(description = "最大方法调用次数")
    private Integer maxCallCount;
    
    @Schema(description = "最小方法调用次数")
    private Integer minCallCount;
    
    @Schema(description = "平均方法调用次数")
    private Double avgCallCount;
    
    @Schema(description = "方法调用次数波动范围")
    private Double callCountRange;
    
    @Schema(description = "高负载方法列表")
    private List<String> highLoadMethods;
    
    @Schema(description = "检测原因说明")
    private String detectionReason;
} 