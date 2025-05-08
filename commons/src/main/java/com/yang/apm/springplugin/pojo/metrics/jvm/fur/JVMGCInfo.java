package com.yang.apm.springplugin.pojo.metrics.jvm.fur;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JVMGCInfo {
    @JsonProperty("gc")
    private GC gc;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GC{
        private Long count;
        private Long time;
    }
}
