package com.yang.apm.springplugin.services.datacollector.metricscollector;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.traces.TraceServiceInfo;
import com.yang.apm.springplugin.services.datacollector.CircularDependencyDetectionService;
import com.yang.apm.springplugin.services.datacollector.ESQueryService;
import com.yang.apm.springplugin.utils.ElasticSearchQueryUtil;
import com.yang.apm.springplugin.utils.TimeUtil;
import com.yang.apm.springplugin.utils.TransactionUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TraceService {

    private static final String TRACE_INDEX_NAME = ".ds-traces-apm*";
    private static final String TIME_OUT = "10s";
    private static final Integer MAX_RECORD_VALUE = 5000;


    @Autowired
    private ESQueryService ESQueryService;
    @Autowired
    private CircularDependencyDetectionService circularDependencyDetectionService;


    /**
     * @param endTimeString  format：yyyy-MM-dd HH:mm:ss
     * @param interval unit:秒
     */
    public List<SvcExternalMetricsRes> getMetricsInTraces(String endTimeString, Integer interval) {
        if (endTimeString == null || endTimeString.isEmpty()) {
            return new LinkedList<>();
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            log.info("fetching traces from {} to {}", startTime, endTime);

            Query rangeQuery = ElasticSearchQueryUtil.createTimeRangeQuery("@timestamp", startTime,
                endTime);
            Query booleanQuery = ElasticSearchQueryUtil.createAllExistQuery("transaction.id");
            Query allCombinedQuery = ElasticSearchQueryUtil.createAllCombinedQuery(rangeQuery,
                booleanQuery);
            SearchRequest request = new SearchRequest.Builder()
                .index(TRACE_INDEX_NAME)
                .query(allCombinedQuery)
                .timeout(TIME_OUT)
                .size(MAX_RECORD_VALUE)
                .sort(sort -> sort.field(f -> f.field("@timestamp").order(SortOrder.Asc)))
                .build();
            List<TraceServiceInfo> traceServiceInfoList = ESQueryService.executeSearch(request,
                TraceServiceInfo.class);

            log.info("收集到的traces条数:{}", traceServiceInfoList.size());
            log.info("收集到的traces:{}", traceServiceInfoList);
            //存储每一条链路信息 key为traceId
            Map<String, List<TraceServiceInfo>> stringListMap = TransactionUtils.parseData2TraceMap(
                traceServiceInfoList);
            log.info("链路条数:{}", stringListMap.size());
            log.info("链路输出:{}", stringListMap);
            //分析指标数据
            List<SvcExternalMetricsRes> resList = TransactionUtils.analyzeTransaction4Metrics(
                stringListMap, endTime, startTime, interval);
            
            //异步分析链路，判断循环依赖
            CompletableFuture<Integer> circularDependencyFuture = circularDependencyDetectionService
                .detectCircularDependencyAsync(stringListMap, endTime, startTime, interval);
            
            // 异步处理，不阻塞主流程
            circularDependencyFuture.thenAccept(count -> {
                if (count > 0) {
                    log.warn("在当前时间窗口 [{} - {}] 内检测到 {} 个循环依赖", 
                        startTime, endTime, count);
                } else {
                    log.debug("在当前时间窗口 [{} - {}] 内未检测到循环依赖", startTime, endTime);
                }
            }).exceptionally(throwable -> {
                log.error("循环依赖检测异步任务执行失败", throwable);
                return null;
            });

            return resList;
        } catch (IOException e) {
            log.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }



}