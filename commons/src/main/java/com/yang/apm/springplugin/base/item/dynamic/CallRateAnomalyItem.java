package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Call Rate Anomaly涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallRateAnomalyItem {
    @Schema(description = "当前服务实例是否涉及Call Rate Anomaly")
    private Boolean status;
    @Schema(description = "当前时间间隔内的调用频率")
    private Double callRate;
    @Schema(description = "历史平均调用频率")
    private Double avgCallRate;
    @Schema(description = "调用频率变化率")
    private Double callRateChangeRate;
    @Schema(description = "调用频率标准差")
    private Double callRateStdDev;
    @Schema(description = "异常程度评分")
    private Double anomalyScore;
    @Schema(description = "当前时间间隔内的请求次数")
    private Integer requestCount;
} 