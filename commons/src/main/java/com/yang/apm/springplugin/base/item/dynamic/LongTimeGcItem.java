package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class LongTimeGcItem {
    @Schema(description = "当前服务实例是否存在GC时间过长问题")
    private Boolean status;
    @Schema(description = "最长Minor GC时间(ms)")
    private Long maxMinorGcTime;
    @Schema(description = "最长Major GC时间(ms)")
    private Long maxMajorGcTime;
    @Schema(description = "检测到的问题列表")
    private List<String> detectedIssues;
    @Schema(description = "检测原因说明")
    private String detectionReason;
} 