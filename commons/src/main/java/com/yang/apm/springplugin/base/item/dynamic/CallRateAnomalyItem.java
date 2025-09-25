package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Call Rate Anomaly涉及到的指标信息
 * 基于业务导向的请求量激增检测
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallRateAnomalyItem {
    
    @Schema(description = "当前服务实例是否涉及Call Rate Anomaly")
    private Boolean status;
    
    @Schema(description = "当前时间间隔的请求量")
    private Integer currentRequestCount;
    
    @Schema(description = "历史平均请求量（前3个时间间隔的平均值）")
    private Double historicalAvgRequestCount;
    
    @Schema(description = "请求量增长倍数（当前/历史平均）")
    private Double growthMultiplier;
    
    @Schema(description = "请求量增长率（百分比形式）")
    private Double growthPercentage;
    
    @Schema(description = "激增程度评级（NORMAL, MODERATE, HIGH, EXTREME）")
    private String surgeLevel;
    
    @Schema(description = "历史请求量序列（用于显示趋势）")
    private String historicalTrend;
    
    @Schema(description = "检测触发的具体原因")
    private String detectionReason;
    
    @Schema(description = "检测时间间隔（秒）")
    private Integer detectionInterval;
} 
