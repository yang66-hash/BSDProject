package com.yang.apm.springplugin.pojo.span;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBInstance {
    @JsonProperty("instance")
    private String instance;
    @JsonProperty("statement")
    private String statement;
    @JsonProperty("type")
    private String type;
    @JsonProperty("user.name")
    private String userName;
}
