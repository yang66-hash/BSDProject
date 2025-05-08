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
public class JVMMemInfo {

    @JsonProperty("memory")
    private Memory memory;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Memory {
        @JsonProperty("heap")
        private Heap heap;

        @JsonProperty("non_heap")
        private NonHeap nonHeap;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Heap {
        @JsonProperty("pool")
        private Pool pool;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NonHeap {
        @JsonProperty("pool")
        private Pool pool;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Pool {
        private Long committed;
        private Long max;
        private Long used;
    }
}
