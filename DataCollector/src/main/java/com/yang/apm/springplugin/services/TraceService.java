package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcTransRes;
import com.yang.apm.springplugin.pojo.traces.TraceServiceInfo;
import com.yang.apm.springplugin.utils.ElasticSearchQueryUtil;
import com.yang.apm.springplugin.utils.TimeUtil;
import com.yang.apm.springplugin.utils.TransactionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TraceService {

    private static final String TRACE_INDEX_NAME = ".ds-traces-apm*";
    private static final String TIME_OUT = "10s";
    private static final Integer MAX_RECORD_VALUE = 5000;


    @Autowired
    private ESQueryService ESQueryService;


    public List<SvcExternalMetricsRes> getMetricsInTraces(String endTimeString, Integer interval) {
        if (endTimeString == null || endTimeString.isEmpty()) {
            return new LinkedList<>();
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            log.info("fetching traces from {} to {}",startTime,endTime);

            Query rangeQuery = ElasticSearchQueryUtil.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query booleanQuery = ElasticSearchQueryUtil.createAllExistQuery("transaction.id");
            Query allCombinedQuery = ElasticSearchQueryUtil.createAllCombinedQuery(rangeQuery, booleanQuery);
            SearchRequest request = new SearchRequest.Builder()
                    .index(TRACE_INDEX_NAME)
                    .query(allCombinedQuery)
                    .timeout(TIME_OUT)
                    .size(MAX_RECORD_VALUE)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();
            List<TraceServiceInfo> traceServiceInfoList = ESQueryService.executeSearch(request, TraceServiceInfo.class);

            log.info("收集到的traces条数:{}", traceServiceInfoList.size());
            log.info("收集到的traces:{}", traceServiceInfoList);
            //存储每一条链路信息
            Map<String, List<TraceServiceInfo>> stringListMap = TransactionUtils.parseData2TraceMap(traceServiceInfoList);
            log.info("链路条数:{}",stringListMap.size());
            log.info("链路输出:{}",stringListMap);
            List<SvcExternalMetricsRes> resList = TransactionUtils.analyzeTransaction4Metrics(stringListMap, endTime, startTime, interval);

            return resList;
        } catch (IOException e) {
            log.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }

    public List<SvcTransRes> getTraces(String endTimeString, Integer interval) {
        if (endTimeString == null || endTimeString.isEmpty()) {
            return new LinkedList<>();
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            log.info("fetching traces from {} to {}",startTime,endTime);

            Query rangeQuery = ElasticSearchQueryUtil.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query booleanQuery = ElasticSearchQueryUtil.createAllExistQuery("transaction.id");
            Query allCombinedQuery = ElasticSearchQueryUtil.createAllCombinedQuery(rangeQuery, booleanQuery);
            SearchRequest request = new SearchRequest.Builder()
                    .index(TRACE_INDEX_NAME)
                    .query(allCombinedQuery)
                    .timeout(TIME_OUT)
                    .size(MAX_RECORD_VALUE)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();
            List<TraceServiceInfo> traceServiceInfoList = ESQueryService.executeSearch(request, TraceServiceInfo.class);

            log.info("收集到的traces条数:{}", traceServiceInfoList.size());
            log.info("收集到的traces:{}", traceServiceInfoList);
            //存储每一条链路信息
            Map<String, List<TraceServiceInfo>> stringListMap = TransactionUtils.parseData2TraceMap(traceServiceInfoList);
            log.info("链路条数:{}",stringListMap.size());
            log.info("链路输出:{}",stringListMap);
            List<SvcTransRes> traceList = TransactionUtils.analyzeTransaction4Trace(stringListMap, endTime, startTime, interval);

            return traceList;
        } catch (IOException e) {
            log.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }






}
