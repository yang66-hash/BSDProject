package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对微服务级别的聚合检测
 * 封装Uneven API Usage涉及到的指标信息
 * 基于所有实例API调用数据的聚合分析
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnevenApiUsageItem {
    
    @Schema(description = "当前微服务是否涉及Uneven API Usage")
    private Boolean status;
    
    @Schema(description = "参与统计的实例数量")
    private Integer instanceCount;
    
    @Schema(description = "参与统计的实例名称列表（逗号分隔）")
    private String instanceNames;
    
    @Schema(description = "总API数量（去重后）")
    private Integer totalApiCount;
    
    @Schema(description = "总调用次数（所有实例聚合）")
    private Integer totalCallCount;
    
    @Schema(description = "最热门API名称")
    private String hottestApiName;
    
    @Schema(description = "最热门API调用次数")
    private Integer hottestApiCallCount;
    
    @Schema(description = "最热门API占比")
    private Double hottestApiRatio;
    
    @Schema(description = "前20%API的调用占比（帕累托检验）")
    private Double top20PercentApiRatio;
    
    @Schema(description = "平均每个API的调用次数")
    private Double avgCallsPerApi;
    
    @Schema(description = "API调用分布的集中度评分")
    private Double concentrationScore;
    
    @Schema(description = "检测触发的规则描述")
    private String triggeredRules;

} 