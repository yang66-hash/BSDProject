package com.yang.apm.springplugin.controller;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.base.Enum.ResponseStatusEnum;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.model.ResponseDTO;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.traces.TraceServiceInfo;
import com.yang.apm.springplugin.utils.ElasticSearchQueryManager;
import com.yang.apm.springplugin.utils.TimeUtil;
import com.yang.apm.springplugin.utils.TransactionUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traces")
@CrossOrigin
public class TraceController {

    private static final Logger logger = LoggerFactory.getLogger(TraceController.class);

    @Autowired
    private ElasticSearchQueryManager elasticSearchQueryManager;

    /**
     * @param endTimeString 资源获取时间末点
     * @param interval 时间间隔
     * @return
     */
    @PostMapping("/traceList")
    @ApiOperation("用于获取一段时间内的traces数据")
    @Parameters({
            @Parameter(name = "endTime", description = "获取资源的时间末点 yyyy-MM-dd HH:mm:ss", required = true),
            @Parameter(name = "interval", description = "获取资源的时间间隔，单位：分", required = true),
    })
    public ResponseDTO<List<SvcExternalMetricsRes>> getTraces(@RequestParam("endTime") String endTimeString,
                                                              @RequestParam("interval") Integer interval) {
        if (endTimeString == null || endTimeString.equals("")) {
            return ResponseDTO.failure(ResponseStatusEnum.PARAM_IS_BLANK);
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            logger.info("fetching traces from {} to {}",startTime,endTime);

            Query rangeQuery = elasticSearchQueryManager.createTimeRangeQuery("@timestamp", startTime, endTime);

            SearchRequest request = new SearchRequest.Builder()
                    .index(".ds-traces-apm*")
                    .query(rangeQuery).timeout("10s")
                    .size(5000)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();
            List<TraceServiceInfo> traceServiceInfoList = elasticSearchQueryManager.executeSearch(request, TraceServiceInfo.class);

            logger.info("收集到的traces条数:{}", traceServiceInfoList.size());
            logger.info("收集到的traces:{}", traceServiceInfoList);
            //存储每一条链路信息
            Map<String, List<TraceServiceInfo>> stringListMap = TransactionUtils.parseData2TraceMap(traceServiceInfoList);
            logger.info("链路条数:{}",stringListMap.size());
            logger.info("链路输出:{}",stringListMap);
            List<SvcExternalMetricsRes> resList = TransactionUtils.analyzeTransaction4Metrics(stringListMap, endTime, startTime, interval);

            return ResponseDTO.success(resList);
        } catch (IOException e) {
            logger.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }
}
