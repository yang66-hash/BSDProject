package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMGCInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMMemInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMSummaryInfo;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.utils.APPMetricsUtil;
import com.yang.apm.springplugin.utils.ElasticSearchQueryManager;
import com.yang.apm.springplugin.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class MetricsService {

    private static final String INDEX_NAME = ".ds-metrics-apm.internal*";
    private static final String TIME_OUT = "30s";
    private static final Integer MAX_RECORD_VALUE = 1000;

    @Autowired
    private ElasticSearchQueryManager elasticSearchQueryManager;

    /**
     * 用于获取指定微服务的指标数据,并将数据写入redis中，设置过期时间expiredTime为45s.
     * 之后每一次的检测都从redis中先获取数据，若是没有再从Elasticsearch中拉取数据做分析再次存入redis中
     */
    public List<SvcMetricsRes> getInternalMetrics(String endTimeString, Integer interval) {
        if (endTimeString == null || interval == null) {
            return new LinkedList<>();
        }
        try {
            Date endTime = TimeUtil.parseStr2Date(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            log.info("fetching metrics from {} to {}",startTime,endTime);

            Query rangeQuery = elasticSearchQueryManager.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query jvmQuery = elasticSearchQueryManager.createAllExistQuery("jvm","system","service.name");
            Query jvmMemQuery = elasticSearchQueryManager.createOneExistQuery("jvm.memory.heap.pool","jvm.memory.non_heap.pool");
            Query jvmGCQuery = elasticSearchQueryManager.createAllExistQuery("jvm.gc.count");
            Query summaryCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery,jvmQuery);
            Query memCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery,jvmMemQuery);
            Query gcCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery,jvmGCQuery);


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
            List<APPJVMSummaryInfo> appSummaryList = elasticSearchQueryManager.executeSearch(summaryRequest, APPJVMSummaryInfo.class);
            APPMetricsUtil.dealWithSummaryData(svcMetricsResList, appSummaryList,startTime,endTime,interval);

            //获取jvm gc详细条目
            List<APPJVMGCInfo> appjvmgcInfoList = elasticSearchQueryManager.executeSearch(gcRequest, APPJVMGCInfo.class);
            APPMetricsUtil.dealWithGCData(svcMetricsResList,appjvmgcInfoList);

            //获取jvm mem详细条目
            List<APPJVMMemInfo> appjvmMemInfoList = elasticSearchQueryManager.executeSearch(memRequest, APPJVMMemInfo.class);
            APPMetricsUtil.dealWithMemData(svcMetricsResList, appjvmMemInfoList);

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
