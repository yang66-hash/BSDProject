package com.yang.apm.springplugin.pojo.traces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestURL {
    private String path;
    private String scheme;
    private String port;
    private String domain;
    private String query;
    private String full;
}
