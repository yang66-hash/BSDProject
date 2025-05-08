package com.yang.apm.springplugin.pojo.result.jvm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JVMGCRes {
    @Schema(description = "时间戳")
    private Date timestamp;
    @Schema(description = "垃圾回收的次数")
    private Long gcCount;
    /**
     * unit: ms
     */
    @Schema(description = "垃圾回收所用的时间总时间 单位：ms")
    private Long gcTime;

}
