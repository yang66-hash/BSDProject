package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装The Ramp涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TheRampItem {
    @Schema(description = "当前服务实例是否涉及The Ramp")
    private Boolean status;
    @Schema(description = "负载爬升速率")
    private Double loadRampUpRate;
    @Schema(description = "性能退化速率")
    private Double performanceDegradationRate;
    @Schema(description = "响应时间增长趋势")
    private Double responseTimeGrowthTrend;
    @Schema(description = "系统承载能力")
    private Double systemCapacity;
    @Schema(description = "负载承受阈值")
    private Double loadToleranceThreshold;
    @Schema(description = "性能稳定性评分")
    private Double performanceStabilityScore;
} 