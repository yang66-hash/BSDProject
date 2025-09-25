package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 针对服务的单一实例
 * 封装High Frequency Of Slow Queries涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HighFreSlowQueriesItem {
    @Schema(description = "当前服务实例是否涉及High Frequency Of Slow Queries")
    private Boolean status;
    @Schema(description = "慢查询次数")
    private Integer slowQueryCount;
    @Schema(description = "慢查询频率（次/分钟）")
    private Double slowQueryFrequency;
    @Schema(description = "慢查询占比")
    private Double slowQueryRatio;
    
    // 新增字段用于实例级检测
    @Schema(description = "总SQL查询次数")
    private Integer totalQueryCount;
    @Schema(description = "检测到的问题列表")
    private List<String> detectedIssues;
    @Schema(description = "问题SQL语句列表")
    private List<String> problematicQueries;
    @Schema(description = "检测原因说明")
    private String detectionReason;
} 