package com.yang.apm.springplugin.services.datacollector.metricscollector;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.expection.ServiceException;
import com.yang.apm.springplugin.pojo.metrics.business.SpringBusinessInfo;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.services.datacollector.ESQueryService;
import com.yang.apm.springplugin.utils.APPBusinessMetricsUtil;
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
public class BusinessMetricsService {

    //将数据的收集拆分为每1min进行一次

    private static final String BUSINESS_INDEX_NAME = ".ds-metrics-apm.app.*";
    private static final String TIME_OUT = "30s";
    private static final Integer MAX_RECORD_VALUE = 2500;

    @Autowired
    private ESQueryService ESQueryService;

    /**
     * @param endTimeString  format：yyyy-MM-dd HH:mm:ss
     * @param interval unit:秒
     */
    public List<BusinessMetricsRes> getBusinessMetrics(String endTimeString, Integer interval) {
        if (endTimeString == null || endTimeString.isEmpty() || interval == null) {
            return new LinkedList<>();
        }
        try {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = dateTimeFormatter.parse(endTimeString);
            Date startTime = TimeUtil.calculateStartTime(endTime, interval);
            log.info("fetching business metrics from {} to {}", startTime, endTime);

            Query rangeQuery = ElasticSearchQueryUtil.createTimeRangeQuery("@timestamp", startTime, endTime);
            Query filterQuery = ElasticSearchQueryUtil.createAllExistQuery("labels.method","method_call_counter","service.name");
            Query gtRangeQuery = ElasticSearchQueryUtil.createGTRangeQuery("method_call_counter", 0);
            Query summaryCombinedQuery = ElasticSearchQueryUtil.createAllCombinedQuery(rangeQuery, filterQuery, gtRangeQuery);

            SearchRequest request = new SearchRequest.Builder()
                    .index(BUSINESS_INDEX_NAME)
                    .query(summaryCombinedQuery)
                    .timeout(TIME_OUT)
                    .size(MAX_RECORD_VALUE)
                    .sort(sort-> sort.field(f-> f.field("@timestamp").order(SortOrder.Asc)))
                    .build();

            List<SpringBusinessInfo> springBusinessInfoList = ESQueryService.executeSearch(request, SpringBusinessInfo.class);
            
            log.info("收集到的business metrics条数:{}", springBusinessInfoList.size());
            log.info("收集到的business metrics:{}", springBusinessInfoList);
            
            List<BusinessMetricsRes> businessMetricsResList = new ArrayList<>();
            //获取总结性条目
            APPBusinessMetricsUtil.dealWithBusinessData(businessMetricsResList, springBusinessInfoList,startTime,endTime,interval);

            log.info("Fetched {} business metrics.", businessMetricsResList.size());
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
