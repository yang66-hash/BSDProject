package com.yang.apm.springplugin.pojo.result;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Map;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SvcExternalMetricsRes extends SvcRes{


    @Schema(description = "时间间隔内总API请求次数")
    private Integer requestCount;

    /**
     * successful request count
     */
    @Schema(description = "时间间隔内成功API请求次数")
    private Integer reqSucCount;

    /**
     * failure request count with 5xx
     */
    @Schema(description = "时间间隔失败API请求次数， 统计错误类型为5xx")
    private Integer reqServerFailCount;

    /**
     * failure request count with 4xx
     */
    @Schema(description = "时间间隔失败API请求次数， 统计错误类型为4xx")
    private Integer resClientFailCount;
    @Schema(description = "时间间隔失败API请求次数率 reqServerFailCount/requestCount")
    private Double failPercent;
    /**
     * average latency of all requests(processed successfully with error/fail requests) in this time window
     *
     */
    @Schema(description = "时间间隔内所有API请求的平均时延 总时延/所有请求请求次数（成功，失败） ")
    private Double avgLatency;

    /**
     * record call number of every API in this instance(service)
     * key: APIName recorded in Transaction
     * value: call number
     * 指标收集：callAPINumMap
     */
    @Schema(description = "当前服务实例下的每一个API的调用次数， key: APIName recorded in Transaction， value: call number")
    private Map<String,Integer> instanceAPICallNumMap;

    /**
     * record average execute time of each API invoked during this interval
     * key: APIName
     * value: average execution time
     * 指标收集器：executionTimeMapPer
     */
    @Schema(description = "当前服务实例下每一个API的平均执行时间， key:API名称， value: call number", example = " \"apiexecTimeMap\": {\n" +
            "      \"ResourceHttpRequestHandler\": 35372,\n" +
            "      \"OpenApiWebMvcResource#openapiJson\": 322086,\n" +
            "      \"SwaggerConfigResource#openapiJson\": 22099,\n" +
            "      \"AdminController#getDataByPage\": 1184437\n" +
            "    }")
    private Map<String,Double> APIExecTimeMap;


    @Schema(description = "当前服务实例此段时间间隔内的关系型数据库的查询总次数")
    private Integer sqlQueryCount;
    @Schema(description = "当前服务实例此段时间间隔内的关系型数据库的慢查询次数")
    private Integer slowQueryCount;
    @Schema(description = "key为当前时间窗口下当前实例的数据库查询的指纹，value为对应慢查询的次数")
    private Map<String, Integer> sqlSlowFingerPrintMap;
    @Schema(description = "key为当前时间窗口下当前实例的数据库查询的指纹，value为对应数据库查询次数")
    private Map<String, Integer> sqlFingerPrintMap;
    /**
     * record the service instance invoked by this service instance
     * key: language-serviceName-podName, unique identifier
     * value: call number
     * 指标收集器：serviceCall
     */
    @Schema(description = "当前服务实例调用的其他服务实例次数统计， key:唯一标识language-serviceName-podName， value: call number")
    private Map<String, Integer> serviceCallNumMap;

    @Override
    public String toString() {
        return super.toString() +  "SvcExternalMetricsRes{" +
                "requestCount=" + requestCount +
                ", reqSucCount=" + reqSucCount +
                ", reqServerFailCount=" + reqServerFailCount +
                ", resClientFailCount=" + resClientFailCount +
                ", failPercent=" + failPercent +
                ", avgLatency=" + avgLatency +
                ", instanceAPICallNumMap=" + instanceAPICallNumMap +
                ", APIExecTimeMap=" + APIExecTimeMap +
                ", sqlQueryCount=" + sqlQueryCount +
                ", slowQueryCount=" + slowQueryCount +
                ", serviceCallNumMap=" + serviceCallNumMap +
                '}';
    }
}
