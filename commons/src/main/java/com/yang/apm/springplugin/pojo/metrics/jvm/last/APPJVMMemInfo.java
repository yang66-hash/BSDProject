package com.yang.apm.springplugin.pojo.metrics.jvm.last;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yang.apm.springplugin.pojo.metrics.APPInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.fur.JVMMemInfo;
import com.yang.apm.springplugin.pojo.result.jvm.JVMMemoryRes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class APPJVMMemInfo extends APPInfo {

    @JsonProperty("labels")
    private Labels labels;

    @JsonProperty("jvm")
    private JVMMemInfo jvmMemInfo;


    public JVMMemoryRes toJVMMemoryRes() {
        JVMMemoryRes res = new JVMMemoryRes();
        if (jvmMemInfo.getMemory().getHeap()!=null) {
            res.setCommitted(this.getJvmMemInfo().getMemory().getHeap().getPool().getCommitted());
            res.setUsed(this.getJvmMemInfo().getMemory().getHeap().getPool().getUsed());
            res.setMax(this.getJvmMemInfo().getMemory().getHeap().getPool().getMax());
        }else {
            res.setCommitted(this.getJvmMemInfo().getMemory().getNonHeap().getPool().getCommitted());
            res.setUsed(this.getJvmMemInfo().getMemory().getNonHeap().getPool().getUsed());
            res.setMax(this.getJvmMemInfo().getMemory().getNonHeap().getPool().getMax());
        }
        return res;
    }
}
