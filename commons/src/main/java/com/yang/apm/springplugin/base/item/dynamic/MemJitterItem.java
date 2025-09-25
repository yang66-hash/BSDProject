package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class MemJitterItem {
    @Schema(description = "当前服务实例是否存在内存抖动问题")
    private Boolean status;
    
    @Schema(description = "堆内存使用率波动范围")
    private Double heapUsedRateRange;
    
    @Schema(description = "最大堆内存使用率")
    private Double maxHeapUsedRate;
    
    @Schema(description = "最小堆内存使用率")
    private Double minHeapUsedRate;
    
    @Schema(description = "GC频率（次/分钟）")
    private Integer gcFrequency;
    
    @Schema(description = "平均GC时间(ms)")
    private Double avgGcTime;
    
    @Schema(description = "检测到的问题列表")
    private List<String> detectedIssues;
    
    @Schema(description = "检测原因说明")
    private String detectionReason;
} 