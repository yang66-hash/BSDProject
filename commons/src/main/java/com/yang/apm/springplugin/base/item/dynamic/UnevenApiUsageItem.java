package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Uneven API Usage涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnevenApiUsageItem {
    @Schema(description = "当前服务实例是否涉及Uneven API Usage")
    private Boolean status;
    @Schema(description = "API调用分布不均衡度")
    private Double apiUsageImbalance;
    @Schema(description = "最热门API的使用比例")
    private Double hottestApiUsageRatio;
    @Schema(description = "最冷门API的使用比例")
    private Double coldestApiUsageRatio;
    @Schema(description = "API使用方差")
    private Double apiUsageVariance;
    @Schema(description = "活跃API数量")
    private Integer activeApiCount;
    @Schema(description = "总API数量")
    private Integer totalApiCount;
} 