package com.yang.apm.springplugin.pojo.metrics.jvm.last;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yang.apm.springplugin.pojo.metrics.APPInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.fur.JVMGCInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class APPJVMGCInfo extends APPInfo {


    @JsonProperty("labels")
    private Labels labels;

    @JsonProperty("jvm")
    private JVMGCInfo jvmgcInfo;

}
