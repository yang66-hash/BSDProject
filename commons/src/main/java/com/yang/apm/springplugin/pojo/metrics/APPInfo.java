package com.yang.apm.springplugin.pojo.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yang.apm.springplugin.pojo.Host;
import com.yang.apm.springplugin.pojo.span.TracesEvent;
import com.yang.apm.springplugin.pojo.system.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class APPInfo {

    /**
     * unit: us
     */
    @JsonProperty("@timestamp")
    private Date timeStamp;

    @JsonProperty("data_stream")
    private DataStream dataStream;

    @JsonProperty("agent")
    private Agent agent;

    @JsonProperty("event")
    private TracesEvent event;

    private Host host;

    @JsonProperty("metricset")
    private Metricset metricset;

    private Observer observer;

    private Processor processor;

    private Service service;
}
