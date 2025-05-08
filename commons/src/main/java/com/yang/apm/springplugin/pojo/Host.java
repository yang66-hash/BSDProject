package com.yang.apm.springplugin.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Host {
    private String hostName;
    private Integer port;
}
