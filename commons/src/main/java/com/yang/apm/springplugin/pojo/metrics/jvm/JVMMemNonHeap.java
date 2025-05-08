package com.yang.apm.springplugin.pojo.metrics.jvm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JVMMemNonHeap {
    private Memory memory;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Memory{
        @JsonProperty("non_heap")
        private NonHeap nonHeap;
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class NonHeap{
            @JsonProperty("pool")
            private Pool pool;
            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class Pool{
                @JsonProperty("committed")
                private Long committed;
                @JsonProperty("maxed")
                private Long maxed;
                @JsonProperty("used")
                private Long used;
            }
        }
    }

}

