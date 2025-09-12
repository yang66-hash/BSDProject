package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装Long Time GC涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LongTimeGcItem {
    @Schema(description = "当前服务实例是否涉及Long Time GC")
    private Boolean status;
    @Schema(description = "最长GC时间")
    private Double maxGcTime;
    @Schema(description = "平均Full GC时间")
    private Double avgFullGcTime;
    @Schema(description = "GC暂停时间总计")
    private Double totalGcPauseTime;
    @Schema(description = "长时间GC次数")
    private Integer longGcCount;
    @Schema(description = "GC效率评分")
    private Double gcEfficiencyScore;
    @Schema(description = "内存回收率")
    private Double memoryRecoveryRate;
} 