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
public class JVMMemHeap {
    private Memory memory;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Memory{
        @JsonProperty("heap")
        private Heap heap;
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public class Heap{
            @JsonProperty("pool")
            private Pool pool;
            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public class Pool{
                @JsonProperty("committed")
                private Long committed;
                @JsonProperty("used")
                private Long used;
            }
        }
    }

}
