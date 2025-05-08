package com.yang.apm.springplugin.pojo.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataStream {
    @JsonProperty("dataset")
    private String dataset;
    @JsonProperty("namespace")
    private String namespace;
    @JsonProperty("type")
    private String type;

}
