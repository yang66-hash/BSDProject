package com.yang.apm.springplugin.pojo.metrics.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yang.apm.springplugin.pojo.metrics.APPInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpringBusinessInfo extends APPInfo {

    @JsonProperty("labels")
    private CustomLabels labels;
    @JsonProperty("method_call_counter")
    private Long methodCallCounter;



}
