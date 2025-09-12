package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Inconsistent Service Response涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InconsistentServiceResponseItem {
    @Schema(description = "当前服务实例是否涉及Inconsistent Service Response")
    private Boolean status;
    @Schema(description = "当前时间间隔内的请求失败率")
    private Double failPercent;
    @Schema(description = "当前时间间隔内的请求次数")
    private Integer requestCount;
    @Schema(description = "当前时间间隔内的所有请求的平均时延")
    private Double avgLatency;
    @Schema(description = "响应时间变异系数")
    private Double responseTimeVariability;
    @Schema(description = "成功率")
    private Double successRate;
} 