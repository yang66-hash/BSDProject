package com.yang.apm.springplugin.services.datacollector.metricscollector;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMGCInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMMemInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMSummaryInfo;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.services.datacollector.ESQueryService;
import com.yang.apm.springplugin.utils.APPMetricsUtil;
import com.yang.apm.springplugin.utils.ElasticSearchQueryUtil;
import com.yang.apm.springplugin.utils.TimeUtil;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MetricsService {

    private static final String INDEX_NAME = ".ds-metrics-apm.internal*";
    private static final String TIME_OUT = "30s";
    private static final Integer MAX_RECORD_VALUE = 1000;

    @Autowired
    private ESQueryService ESQueryService;

    /**
     * @param endTimeString  format：yyyy-MM-dd'T'HH:mm:ss.SSS'Z' (ISO 8601 format)
     * @param interval unit:秒
     */
    public List<SvcMetricsRes> getInternalMetrics(String endTimeString, Integer interval) {
        if (endTimeString == null || endTimeString.isEmpty() || interval == null) {
            return new LinkedList<>();
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            log.info("fetching metrics from {} to {}", startTime, endTime);

            Query rangeQuery = ElasticSearchQueryUtil.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query jvmQuery = ElasticSearchQueryUtil.createAllExistQuery("jvm","system","service.name");
            Query jvmMemQuery = ElasticSearchQueryUtil.createOneExistQuery("jvm.memory.heap.pool","jvm.memory.non_heap.pool");
            Query jvmGCQuery = ElasticSearchQueryUtil.createAllExistQuery("jvm.gc.count");
            Query summaryCombinedQuery = ElasticSearchQueryUtil.createAllCombinedQuery(rangeQuery,jvmQuery);
            Query memCombinedQuery = ElasticSearchQueryUtil.createAllCombinedQuery(rangeQuery,jvmMemQuery);
            Query gcCombinedQuery = ElasticSearchQueryUtil.createAllCombinedQuery(rangeQuery,jvmGCQuery);

            SearchRequest summaryRequest = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .query(summaryCombinedQuery)
                    .timeout(TIME_OUT)
                    .size(MAX_RECORD_VALUE)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            SearchRequest memRequest = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .query(memCombinedQuery)
                    .timeout(TIME_OUT)
                    .size(MAX_RECORD_VALUE)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            SearchRequest gcRequest = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .query(gcCombinedQuery)
                    .timeout(TIME_OUT)
                    .size(MAX_RECORD_VALUE)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            List<SvcMetricsRes> svcMetricsResList = new ArrayList<>();
            
            //获取总结性条目
            List<APPJVMSummaryInfo> appSummaryList = ESQueryService.executeSearch(summaryRequest, APPJVMSummaryInfo.class);
            APPMetricsUtil.dealWithSummaryData(svcMetricsResList, appSummaryList,startTime,endTime,interval);

            //获取jvm gc详细条目
            List<APPJVMGCInfo> appjvmgcInfoList = ESQueryService.executeSearch(gcRequest, APPJVMGCInfo.class);
            APPMetricsUtil.dealWithGCData(svcMetricsResList,appjvmgcInfoList);

            //获取jvm mem详细条目
            List<APPJVMMemInfo> appjvmMemInfoList = ESQueryService.executeSearch(memRequest, APPJVMMemInfo.class);
            APPMetricsUtil.dealWithMemData(svcMetricsResList, appjvmMemInfoList);

            log.info("收集到的internal metrics条数:{}", svcMetricsResList.size());
            log.info("收集到的internal metrics:{}", svcMetricsResList);
            
            return svcMetricsResList;
        } catch (IOException e) {
            log.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }

}
