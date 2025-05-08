package com.yang.apm.springplugin.pojo.result.business;

import com.yang.apm.springplugin.pojo.result.SvcRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessMetricsRes extends SvcRes {

    @Schema(description = "业务层方法的调用统计，键为方法名称，值为调用次数",example = "public final java.lang.Class com.yang.xingdiancloud.service.impl.UserUnitRelationService$$SpringCGLIB$$0.getTargetClass():1")
    private Map<String, Long> businessCallMap;

}
