package com.yang.apm.springplugin.pojo.result.jvm;

import com.yang.apm.springplugin.pojo.result.SvcRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


/**
 * author: yang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SvcMetricsRes extends SvcRes {


    //先统计heap三部分内存池的内存信息eden survivor old


    private Map<String,List<JVMMemoryRes>> memoryRes;

    private Map<String, List<JVMGCRes>> gcRes;

    @Schema(description = "JVM相关数据总结列表")
    private List<JVMSummaryRes> jvmSummaryList;

}
