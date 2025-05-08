package com.yang.apm.springplugin.pojo.traces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Destination {
    @JsonProperty("address")
    private String address;
    @JsonProperty("port")
    private Integer port;
    @JsonProperty("ip")
    private String ip;
}
