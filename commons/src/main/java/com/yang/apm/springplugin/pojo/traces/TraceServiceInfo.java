package com.yang.apm.springplugin.pojo.traces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yang.apm.springplugin.pojo.span.Span;
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
public class TraceServiceInfo {

    /**
     * unit: us
     */
    @JsonProperty("parent")
    private Parent parent;
    @JsonProperty("@timestamp")
    private Date timeStamp;

    @JsonProperty("agent")
    private Agent agent;

    private Destination destination;

    private Processor processor;

    @JsonProperty("url")
    private RequestURL url;

    private Observer observer;

    @JsonProperty("trace")
    private Trace trace;

    @JsonProperty("data_stream")
    private DataStream dataStream;

    private Service service;

    private Host host;

    @JsonProperty("event")
    private TracesEvent event;

    @JsonProperty("transaction")
    private TraceTransaction transaction;

    @JsonProperty("span")
    private Span span;

    @Override
    public String toString() {
        return "TraceService{" + "\n" +
                "parent=" + parent +"\n" +
                ", timeStamp=" + timeStamp +"\n" +
                ", agent=" + agent +"\n" +
                ", destination=" + destination +"\n" +
                ", processor=" + processor +"\n" +
                ", observer=" + observer +"\n" +
                ", trace=" + trace +"\n" +
                ", dataStream=" + dataStream +"\n" +
                ", service=" + service +"\n" +
                ", host=" + host +"\n" +
                ", event=" + event +"\n" +
                ", transaction=" + transaction +"\n" +
                ", span=" + span +"\n" +
                '}';
    }
}
