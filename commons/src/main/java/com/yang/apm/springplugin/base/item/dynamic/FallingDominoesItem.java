package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Falling Dominoes涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FallingDominoesItem {
    @Schema(description = "当前服务实例是否涉及Falling Dominoes")
    private Boolean status;
    @Schema(description = "级联失败次数")
    private Integer cascadeFailureCount;
    @Schema(description = "错误传播深度")
    private Integer errorPropagationDepth;
    @Schema(description = "受影响的下游服务数量")
    private Integer affectedDownstreamServices;
    @Schema(description = "故障恢复时间")
    private Double failureRecoveryTime;
    @Schema(description = "系统弹性评分")
    private Double systemResilienceScore;
    @Schema(description = "故障隔离效果")
    private Double faultIsolationEffectiveness;
} 