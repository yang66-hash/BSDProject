package com.yang.apm.springplugin.controller;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.base.Enum.ResponseStatusEnum;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.model.ResponseDTO;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMGCInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMMemInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMSummaryInfo;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.utils.APPMetricsUtil;
import com.yang.apm.springplugin.utils.ElasticSearchQueryManager;
import com.yang.apm.springplugin.utils.TimeUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/internal")
@CrossOrigin
public class InternalMetricsController {


    private static final Logger logger = LoggerFactory.getLogger(InternalMetricsController.class);
    @Autowired
    private ElasticSearchQueryManager elasticSearchQueryManager;

    /**
     * @param endTimeString 资源获取时间末点
     * @param interval 时间间隔
     * @return
     */
    @PostMapping("/metrics")
    @Operation(description = "用于获取业务逻辑层相关指标数据,默认30s收集一次数据")
    @Parameters({
            @Parameter(name = "endTime", description = "获取资源的时间末点 yyyy-MM-dd HH:mm:ss", required = true),
            @Parameter(name = "interval", description = "获取资源的时间间隔，单位：秒", required = true),
            @Parameter(name = "serviceName", description = "搜集对应的服务名称", required = true)
    })
    public ResponseDTO<List<SvcMetricsRes>> getInternalMetrics(@RequestParam("endTime") String endTimeString,
                                                               @RequestParam("interval") Integer interval,
                                                               @RequestParam("serviceName") String serviceName) {
        if (endTimeString == null || interval == null || serviceName == null) {
            return ResponseDTO.failure(ResponseStatusEnum.PARAM_IS_BLANK);
        }
        try {
            Date endTime = TimeUtil.parseStr2Date(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            logger.info("fetching metrics from {} to {}",startTime,endTime);
            
            Query rangeQuery = elasticSearchQueryManager.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query serviceQuery = elasticSearchQueryManager.createMatchQuery("service.name", serviceName);
            Query jvmQuery = elasticSearchQueryManager.createAllExistQuery("jvm","system");
            Query jvmMemQuery = elasticSearchQueryManager.createOneExistQuery("jvm.memory.heap.pool","jvm.memory.non_heap.pool");
            Query jvmGCQuery = elasticSearchQueryManager.createAllExistQuery("jvm.gc.count");

            Query summaryCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery,serviceQuery,jvmQuery);
            Query memCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery,serviceQuery,jvmMemQuery);
            Query gcCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery,serviceQuery,jvmGCQuery);
            

            SearchRequest summaryRequest = new SearchRequest.Builder()
                    .index(".ds-metrics-apm.internal*")
                    .query(summaryCombinedQuery)
                    .timeout("30s")
                    .size(500)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            SearchRequest memRequest = new SearchRequest.Builder()
                    .index(".ds-metrics-apm.internal*")
                    .query(memCombinedQuery)
                    .timeout("30s")
                    .size(500)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            SearchRequest gcRequest = new SearchRequest.Builder()
                    .index(".ds-metrics-apm.internal*")
                    .query(gcCombinedQuery)
                    .timeout("30s")
                    .size(500)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();
            List<SvcMetricsRes> svcMetricsResList = new ArrayList<>();
            //获取总结性条目
            List<APPJVMSummaryInfo> appSummaryList = elasticSearchQueryManager.executeSearch(summaryRequest, APPJVMSummaryInfo.class);
            APPMetricsUtil.dealWithSummaryData(svcMetricsResList, appSummaryList,startTime,endTime,interval);

            //获取jvm gc详细条目
            List<APPJVMGCInfo> appjvmgcInfoList = elasticSearchQueryManager.executeSearch(gcRequest, APPJVMGCInfo.class);
            APPMetricsUtil.dealWithGCData(svcMetricsResList,appjvmgcInfoList);

            //获取jvm mem详细条目
            List<APPJVMMemInfo> appjvmMemInfoList = elasticSearchQueryManager.executeSearch(memRequest, APPJVMMemInfo.class);
            APPMetricsUtil.dealWithMemData(svcMetricsResList, appjvmMemInfoList);

            return ResponseDTO.success(svcMetricsResList);
        } catch (IOException e) {
            logger.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }

}
