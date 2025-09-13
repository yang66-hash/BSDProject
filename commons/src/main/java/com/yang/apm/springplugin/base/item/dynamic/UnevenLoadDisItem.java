package com.yang.apm.springplugin.base.item.dynamic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对服务的单一实例
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnevenLoadDisItem {
  @Schema(description = "当前服务实例是否涉及Uneven Load Distribution")
  private Boolean status;
  @Schema(description = "当前时间间隔中使用CPU占系统总cpu核数最大比例")
  private Double pct;
  @Schema(description = "当前实例时间间隔内使用的最大内存")
  private Long memoryUsed;
  @Schema(description = "当前整个时间窗口内实例使用的最大内存 字节")
  private Long memoryMaxed;
  @Schema(description = "当前时间间隔内的所有请求的平均时延")
  private Double avgLatency;


}
