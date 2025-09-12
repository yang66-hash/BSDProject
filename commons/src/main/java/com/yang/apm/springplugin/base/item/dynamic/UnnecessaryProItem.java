package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Unnecessary Processing涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnnecessaryProItem {
    @Schema(description = "当前服务实例是否涉及Unnecessary Processing")
    private Boolean status;
    @Schema(description = "重复处理次数")
    private Integer redundantProcessingCount;
    @Schema(description = "无效计算比例")
    private Double ineffectiveComputationRatio;
    @Schema(description = "资源浪费率")
    private Double resourceWasteRate;
    @Schema(description = "处理效率评分")
    private Double processingEfficiencyScore;
    @Schema(description = "缓存命中率")
    private Double cacheHitRate;
    @Schema(description = "优化潜力评分")
    private Double optimizationPotentialScore;
} 