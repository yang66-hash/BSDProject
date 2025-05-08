package com.yang.apm.springplugin.pojo.result.jvm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JVMMemoryRes {

    @Schema(description = "时间戳")
    private Date timestamp;
    @Schema(description = "JVM向操作系统请求并获得的内存量，是 JVM 堆的物理内存，不一定是 JVM 实际正在使用的内存 单位byte")
    private Long committed;
    @Schema(description = "JVM 实际已使用的内存 单位byte")
    private Long used;
    @Schema(description = "JVM可使用的最大内存量 单位byte")
    private Long max;
}
