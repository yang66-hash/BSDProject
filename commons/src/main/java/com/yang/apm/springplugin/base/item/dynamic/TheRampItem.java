package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class TheRampItem {
    @Schema(description = "是否检测到The Ramp问题")
    private Boolean status;
    
    @Schema(description = "连续增长的区间数")
    private Integer consecutiveGrowthCount;
    
    @Schema(description = "总增长率")
    private Double totalGrowthRate;
    
    @Schema(description = "响应时间变化趋势")
    private List<String> latencyTrend;
    
    @Schema(description = "检测原因说明")
    private String detectionReason;
} 