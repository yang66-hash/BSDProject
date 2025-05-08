package com.yang.apm.springplugin.pojo.metrics.jvm.fur;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yang.apm.springplugin.pojo.metrics.jvm.JVMMemory;
import com.yang.apm.springplugin.pojo.metrics.jvm.JVMThread;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JVMSummaryInfo {
    @JsonProperty("gc")
    private JVMGC jvmgc;
    @JsonProperty("memory")
    private JVMMemory jvmmemory;
    @JsonProperty("thread")
    private JVMThread jvmthread;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JVMGC {
        @JsonProperty("alloc")
        private Long alloc;
    }

}
