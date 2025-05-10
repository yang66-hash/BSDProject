package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.pojo.metrics.business.SpringBusinessInfo;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.utils.APPBusinessMetricsUtil;
import com.yang.apm.springplugin.utils.ElasticSearchQueryManager;
import com.yang.apm.springplugin.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class BusinessService {

    //将数据的收集拆分为每1min进行一次

    private static final String BUSINESS_INDEX_NAME = ".ds-metrics-apm.app.*";
    private static final String TIME_OUT = "30s";
    private static final Integer MAX_RECORD_VALUE = 2500;

    @Autowired
    private ElasticSearchQueryManager elasticSearchQueryManager;


    public List<BusinessMetricsRes> getBusinessMetrics(String endTimeString, Integer interval) {
        if (endTimeString == null || interval == null) {
            return new LinkedList<>();
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            log.info("fetching metrics from {} to {}",startTime,endTime);

            Query rangeQuery = elasticSearchQueryManager.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query filterQuery = elasticSearchQueryManager.createAllExistQuery("labels.method","method_call_counter","service.name");
            Query gtRangeQuery = elasticSearchQueryManager.createGTRangeQuery("method_call_counter", 0);

            Query summaryCombinedQuery = elasticSearchQueryManager.createAllCombinedQuery(rangeQuery, filterQuery, gtRangeQuery);

            SearchRequest request = new SearchRequest.Builder()
                    .index(BUSINESS_INDEX_NAME)
                    .query(summaryCombinedQuery)
                    .timeout(TIME_OUT)
                    .size(MAX_RECORD_VALUE)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            List<BusinessMetricsRes> businessMetricsResList = new ArrayList<>();
            log.info(businessMetricsResList.toString());
            //获取总结性条目
            List<SpringBusinessInfo> springBusinessInfoList = elasticSearchQueryManager.executeSearch(request, SpringBusinessInfo.class);
            APPBusinessMetricsUtil.dealWithBusinessData(businessMetricsResList, springBusinessInfoList,startTime,endTime,interval);

            log.info("Fetched {} business metrics.", springBusinessInfoList.size());
            return businessMetricsResList;

        } catch (IOException e) {
            log.error("Error executing Elasticsearch query", e);
            throw new ServiceException("Error executing Elasticsearch query", e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new ServiceException("Unexpected error occurred while fetching metrics", e);
        }
    }
    
}
