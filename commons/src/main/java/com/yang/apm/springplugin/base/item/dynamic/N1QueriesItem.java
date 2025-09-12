package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 * 封装N+1 Queries涉及到的指标信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class N1QueriesItem {
    @Schema(description = "当前服务实例是否涉及N+1 Queries")
    private Boolean status;
    @Schema(description = "数据库查询次数")
    private Integer totalQueryCount;
    @Schema(description = "单次操作平均查询次数")
    private Double avgQueriesPerOperation;
    @Schema(description = "重复查询次数")
    private Integer duplicateQueryCount;
    @Schema(description = "批量查询优化率")
    private Double batchOptimizationRate;
    @Schema(description = "查询效率评分")
    private Double queryEfficiencyScore;
    @Schema(description = "ORM查询性能指标")
    private Double ormPerformanceMetric;
} 