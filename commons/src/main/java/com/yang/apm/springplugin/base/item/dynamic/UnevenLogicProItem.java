package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Uneven Logic Processing涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnevenLogicProItem {
    @Schema(description = "当前服务实例是否涉及Uneven Logic Processing")
    private Boolean status;
    @Schema(description = "逻辑处理时间分布不均衡度")
    private Double processingTimeImbalance;
    @Schema(description = "最长处理时间")
    private Double maxProcessingTime;
    @Schema(description = "最短处理时间")
    private Double minProcessingTime;
    @Schema(description = "处理时间方差")
    private Double processingTimeVariance;
    @Schema(description = "复杂逻辑占比")
    private Double complexLogicRatio;
    @Schema(description = "逻辑处理效率评分")
    private Double logicEfficiencyScore;
} 