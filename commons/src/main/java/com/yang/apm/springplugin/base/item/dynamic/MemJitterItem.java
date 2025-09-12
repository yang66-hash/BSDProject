package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Memory Jitter Of Service涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemJitterItem {
    @Schema(description = "当前服务实例是否涉及Memory Jitter")
    private Boolean status;
    @Schema(description = "内存使用波动范围")
    private Double memoryJitterRange;
    @Schema(description = "内存使用标准差")
    private Double memoryUsageStdDev;
    @Schema(description = "内存峰值使用率")
    private Double peakMemoryUsage;
    @Schema(description = "内存谷值使用率")
    private Double minMemoryUsage;
    @Schema(description = "内存抖动频率")
    private Double jitterFrequency;
    @Schema(description = "内存稳定性评分")
    private Double memoryStabilityScore;
} 