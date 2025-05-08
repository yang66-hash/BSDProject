package com.yang.apm.springplugin.pojo.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
    @JsonProperty("agent_id_status")
    private String agentIdStatus;
    @JsonProperty("ingested")
    private Date ingested;
    @JsonProperty("success_count")
    private SuccessCount successCount;
}
