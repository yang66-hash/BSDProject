package com.yang.apm.springplugin.pojo.result.jvm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JVMSummaryRes {
    @Schema(description = "时间戳")
    private Date timestamp;
    @Schema(description = "JVM堆内存实际申请使用量 单位byte")
    private Long heapCommitted;
    @Schema(description = "JVM堆内存实际最大使用量 单位byte")
    private Long heapMaxed;
    @Schema(description = "JVM堆内存实际使用量 单位byte")
    private Long heapUsed;
    @Schema(description = "JVM非堆内存实际申请使用量 单位byte")
    private Long nonHeapCommitted;
    @Schema(description = "JVM非堆内存实际使用量 单位byte")
    private Long nonHeapUsed;
    /**
     * 内存分配量
     */
    @Schema(description = "统计在垃圾回收期间，JVM向内存池、堆空间等区域分配内存量 随着gc次数增加而增加 byte")
    private Long gcAllocated;

    /**
     * jvm 线程数量
     */
    @Schema(description = "jvm现有线程数量")
    private Long threadCount;

    /**
     * 表示当前系统使用cpu核心数量占所有核心数的比重
     * the percentage of used cpu cores out of the total numbers
     */
    @Schema(description = " 表示当前系统使用cpu核心数量占所有核心数的比重")
    private Double cpuSysPCT;

    /**
     * 系统总内存
     */
    @Schema(description = "系统总内存")
    private Long memSysTotal;
    /**
     * 实际可用内存
     * Actual free memory in bytes.
     * It is calculated based on the OS.
     * On Linux it consists of the free memory plus caches and buffers.
     * On OSX it is a sum of free memory and the inactive memory.
     * On Windows, it is equal to system.memory.free.
     */
    @Schema(description = "实际可用内存")
    private Long memSysFree;

    /**
     * 当前程序(即服务实例)此刻使用的cpu占总比重
     */
    @Schema(description = "当前程序(即服务实例)此刻使用的cpu占总比重")
    private Double cpuProPCT;

    /**
     * The Resident Set Size. The amount of memory the process occupied in main memory (RAM).
     * 当前程序（服务实例）在系统内存中的占用情况
     */
    @Schema(description = "当前程序（服务实例）在系统内存中的占用情况")
    private Long memProTotal;

}
