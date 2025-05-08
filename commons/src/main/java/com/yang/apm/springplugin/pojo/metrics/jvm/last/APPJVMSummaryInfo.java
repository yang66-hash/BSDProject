package com.yang.apm.springplugin.pojo.metrics.jvm.last;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yang.apm.springplugin.pojo.metrics.APPInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.fur.JVMSummaryInfo;
import com.yang.apm.springplugin.pojo.metrics.system.SystemInfo;
import com.yang.apm.springplugin.pojo.result.jvm.JVMSummaryRes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class APPJVMSummaryInfo extends APPInfo {

    @JsonProperty("labels")
    private Labels labels;

    @JsonProperty("jvm")
    private JVMSummaryInfo jvmInfo;

    @JsonProperty("system")
    private SystemInfo systemInfo;





    public JVMSummaryRes toJVMSummaryRes() {
        JVMSummaryRes jvmSummaryRes = new JVMSummaryRes();
        jvmSummaryRes.setTimestamp(this.getTimeStamp());
        //存储heap信息
        jvmSummaryRes.setHeapCommitted(this.getJvmInfo().getJvmmemory().getJvmHeap().getCommitted());
        jvmSummaryRes.setHeapUsed(this.getJvmInfo().getJvmmemory().getJvmHeap().getUsed());
        jvmSummaryRes.setHeapMaxed(this.getJvmInfo().getJvmmemory().getJvmHeap().getMax());
        //存储non-heap信息
        jvmSummaryRes.setNonHeapCommitted(this.getJvmInfo().getJvmmemory().getJvmNonHeap().getCommitted());
        jvmSummaryRes.setNonHeapUsed(this.getJvmInfo().getJvmmemory().getJvmNonHeap().getUsed());
        //存储thread
        jvmSummaryRes.setThreadCount(this.getJvmInfo().getJvmthread().getCount());
        //存储gc alloc
        jvmSummaryRes.setGcAllocated(this.getJvmInfo().getJvmgc().getAlloc());

        //存储system相关
        jvmSummaryRes.setCpuSysPCT(this.getSystemInfo().getCpuInfo().getTotal().getNorm().getPct());
        jvmSummaryRes.setMemSysTotal(this.getSystemInfo().getMemory().getTotal());
        jvmSummaryRes.setMemSysFree(this.getSystemInfo().getMemory().getActual().getFree());

        //存储进程相关
        jvmSummaryRes.setCpuProPCT(this.getSystemInfo().getProcess().getCpuInfo().getTotal().getNorm().getPct());
        jvmSummaryRes.setMemProTotal(this.getSystemInfo().getProcess().getMemory().getSize());

        return jvmSummaryRes;
    }
}
