package com.yang.apm.springplugin.base.context.dynamicres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.dynamic.InconsistentServiceResponseItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;

@Data
public class InconsistentServiceResponseContext extends ParentContext {

  //时间间隔
  private Integer metricsInterval;
  //当前时间间隔内
  @Schema(description = "统计该服务的每一个实例是否涉及到Inconsistent Service Response异味")
  private Map<String, InconsistentServiceResponseItem> instanceStatus;

}

