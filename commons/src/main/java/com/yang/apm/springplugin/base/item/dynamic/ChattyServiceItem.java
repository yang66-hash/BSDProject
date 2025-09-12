package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Chatty Service涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChattyServiceItem {
    @Schema(description = "当前服务实例是否涉及Chatty Service")
    private Boolean status;
    @Schema(description = "服务间调用频率")
    private Double interServiceCallRate;
    @Schema(description = "单次操作的平均调用次数")
    private Double avgCallsPerOperation;
    @Schema(description = "外部服务依赖数量")
    private Integer externalDependencyCount;
    @Schema(description = "网络调用开销比例")
    private Double networkOverheadRatio;
    @Schema(description = "调用链长度")
    private Integer callChainLength;
    @Schema(description = "通信密集度")
    private Double communicationIntensity;
} 