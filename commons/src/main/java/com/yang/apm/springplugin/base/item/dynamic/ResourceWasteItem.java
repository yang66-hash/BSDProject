package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Resource Waste涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceWasteItem {
    @Schema(description = "当前服务实例是否涉及Resource Waste")
    private Boolean status;
    @Schema(description = "CPU使用率")
    private Double cpuUsage;
    @Schema(description = "内存使用率")
    private Double memoryUsage;
    @Schema(description = "网络使用率")
    private Double networkUsage;
    @Schema(description = "当前时间间隔内的请求次数")
    private Integer requestCount;
    @Schema(description = "资源利用效率")
    private Double resourceEfficiency;
    @Schema(description = "资源浪费程度")
    private Double wasteLevel;
} 