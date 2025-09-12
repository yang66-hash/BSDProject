package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Frequent GC涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrequentGcItem {
    @Schema(description = "当前服务实例是否涉及Frequent GC")
    private Boolean status;
    @Schema(description = "GC触发频率（次/分钟）")
    private Double gcFrequency;
    @Schema(description = "Minor GC次数")
    private Integer minorGcCount;
    @Schema(description = "Major GC次数")
    private Integer majorGcCount;
    @Schema(description = "平均GC时间")
    private Double avgGcTime;
    @Schema(description = "GC时间占比")
    private Double gcTimeRatio;
    @Schema(description = "堆内存使用率")
    private Double heapMemoryUsage;
} 