package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Service Chain涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceChainItem {
    @Schema(description = "当前服务实例是否涉及Service Chain")
    private Boolean status;
    @Schema(description = "服务链长度")
    private Integer chainLength;
    @Schema(description = "链中的最大响应时间")
    private Double maxChainLatency;
    @Schema(description = "链的总响应时间")
    private Double totalChainLatency;
    @Schema(description = "链中服务节点数量")
    private Integer nodeCount;
    @Schema(description = "链的失败传播风险")
    private Double failurePropagationRisk;
    @Schema(description = "链的复杂度评分")
    private Double complexityScore;
} 