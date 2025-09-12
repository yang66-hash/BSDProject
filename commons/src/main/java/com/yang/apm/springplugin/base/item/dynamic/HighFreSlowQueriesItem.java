package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Schema(description = "平均查询执行时间")
    private Double avgQueryExecutionTime;
    @Schema(description = "最大查询执行时间")
    private Double maxQueryExecutionTime;
    @Schema(description = "慢查询占比")
    private Double slowQueryRatio;
    @Schema(description = "数据库连接池使用率")
    private Double dbConnectionUsage;
} 