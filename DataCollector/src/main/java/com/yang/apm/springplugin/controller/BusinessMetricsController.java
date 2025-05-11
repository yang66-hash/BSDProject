package com.yang.apm.springplugin.controller;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.base.Enum.ResponseStatusEnum;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.model.ResponseDTO;
import com.yang.apm.springplugin.pojo.metrics.business.SpringBusinessInfo;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.utils.APPBusinessMetricsUtil;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/api/business")
@CrossOrigin
public class BusinessMetricsController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessMetricsController.class);
    @Autowired
    private ElasticSearchQueryManager elasticSearchQueryManager;

    /**
     * @param endTimeString 资源获取时间末点
     * @param interval 时间间隔
     * @return
     */
    @PostMapping("/metrics")
    @Operation(description = "用于获取业务逻辑层相关指标数据")
    @Parameters({
            @Parameter(name = "endTime", description = "获取资源的时间末点 yyyy-MM-dd HH:mm:ss", required = true),
            @Parameter(name = "interval", description = "获取资源的时间间隔，单位：秒", required = true),
            @Parameter(name = "serviceName", description = "搜集对应的服务名称", required = true)
    })
    public ResponseDTO<List<BusinessMetricsRes>> getBusinessMetrics(@RequestParam("endTime") String endTimeString,
                                                                    @RequestParam("interval") Integer interval,
                                                                    @RequestParam("serviceName") String serviceName) {
        if (endTimeString == null || interval == null || serviceName == null) {
            return ResponseDTO.failure(ResponseStatusEnum.PARAM_IS_BLANK);
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            logger.info("fetching metrics from {} to {}",startTime,endTime);

            Query rangeQuery = elasticSearchQueryManager.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query filterQuery = elasticSearchQueryManager.createAllExistQuery("labels.method","method_call_counter");
            Query gtRangeQuery = elasticSearchQueryManager.createGTRangeQuery("method_call_counter", 0);
            Query serviceQuery = elasticSearchQueryManager.createMatchQuery("service.name", serviceName);

            Query summaryCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery, filterQuery, gtRangeQuery , serviceQuery);

            SearchRequest request = new SearchRequest.Builder()
                    .index(".ds-metrics-apm.app.*")
                    .query(summaryCombinedQuery)
                    .timeout("30s")
                    .size(2500)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            List<BusinessMetricsRes> businessMetricsResList = new ArrayList<>();
            //获取总结性条目
            List<SpringBusinessInfo> springBusinessInfoList = elasticSearchQueryManager.executeSearch(request, SpringBusinessInfo.class);
            APPBusinessMetricsUtil.dealWithBusinessData(businessMetricsResList, springBusinessInfoList,startTime,endTime,interval);


            logger.info("Fetched {} business metrics.", springBusinessInfoList.size());

            return ResponseDTO.success(businessMetricsResList);


        } catch (IOException e) {
            logger.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }

}
