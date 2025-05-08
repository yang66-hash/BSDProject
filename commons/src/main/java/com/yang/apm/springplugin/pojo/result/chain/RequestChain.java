package com.yang.apm.springplugin.pojo.result.chain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestChain {
    @Schema(description = "当前链路ID",example = "0223801868e0c17466c5ddcbb3437c72")
    private String traceId;
    @Schema(description = "源服务名称，即请求头上的第一个微服务")
    private String sourceSvc;
    @Schema(description = "请求的方法名",example = "UserController#getDataByPage")
    private String APIName;

    /**
     * 存储当前链路对第一层级服务的调用次数
     * key : language|serviceName|podName|APIName
     * value: times
     */
    @Schema(description = "存储当前链路对第一层级服务的调用次数 key : language|serviceName|podName|APIName value: 次数")
    private Map<String,Integer> targetSvcNumMap;

    @Schema(description = "当前请求的请求链路")
    private TraceChainNode chain;
}
