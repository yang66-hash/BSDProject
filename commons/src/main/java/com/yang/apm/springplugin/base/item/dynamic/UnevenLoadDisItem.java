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
  @Schema(description = "当前时间间隔中使用CPU占系统总cpu核数比例")
  private Double pct;
  @Schema(description = "当前时间间隔内的请求次数")
  private Long memory;
  @Schema(description = "当前时间间隔内的所有请求的平均时延")
  private Double avgLatency;
}
