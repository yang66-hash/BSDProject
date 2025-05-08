package com.yang.apm.springplugin.pojo.metrics.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CPU and memory info
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemInfo {
    @JsonProperty("cpu")
    private CPUInfo cpuInfo;
    @JsonProperty("memory")
    private Memory memory;
    @JsonProperty("process")
    private Process process;

    @Data
    @NoArgsConstructor
    public static class CPUInfo {
        @JsonProperty("total")
        private Total total;

        @Data
        @NoArgsConstructor
        public class Total{
            @JsonProperty("norm")
            private Norm norm;
            @Data
            @NoArgsConstructor
            public class Norm{
                @JsonProperty("pct")
                private Double pct;
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class Memory{
        @JsonProperty("actual")
        private Actual actual;
        @JsonProperty("total")
        private Long total;

        @Data
        @NoArgsConstructor
        public static class Actual{
            private Long free;
        }

    }
    @Data
    @NoArgsConstructor
    public static class Process{
        @JsonProperty("cpu")
        private CPUInfo cpuInfo;
        @JsonProperty("memory")
        private Memory memory;
        @Data
        @NoArgsConstructor
        public static class Memory {
            private Long size;
        }
    }
}
