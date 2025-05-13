package com.yang.apm.springplugin.services.dynamicdetect;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.ElasticsearchSettings;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;

import com.yang.apm.springplugin.utils.ElasticSearchQueryUtil;
import com.yang.apm.springplugin.utils.IndexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FragileServiceService {


    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;


    public DetectionResItem fragileServiceDetect(RequestItem requestItem) {
        //针对所有的数据计算历史平均值，将TimeWindow下的历史数据计算出平均值，上传至ES
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String formatted = dateFormat.format(date);
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        Integer window = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
        String indexNameForWindow = IndexUtil.getExternalMetricsIndex(window);
        String indexNameForInterval = IndexUtil.getExternalMetricsIndex(interval);

        Query matchQuery = ElasticSearchQueryUtil.createMatchQuery("serviceName", requestItem.getServiceName());
//        TermsAggregation podName = queryManager.createTermsAggregation("podName");


        // 查询数据
        // 按照服务实例分组，取出每一个实例的最新计算结果
        //
//        SearchRequest summaryRequest = new SearchRequest.Builder()
//                .index(indexNameForWindow)
//                .query(matchQuery)
//                .sort(sort-> sort.field(f-> f.field("endTime").order(SortOrder.Desc)))
//                .aggregations("by_podName", a -> a.terms(podName))
//                .build();

        return null;
    }


    /**
     * 脆弱服务的具体检测逻辑
     * 依据数据svc transaction results进行检测
     * 三个检测维度：
     *   1.请求失败率
     *   2.请求时延
     *   3.服务实例吞吐量
     *   通过正则表达式匹配key值为"TRACE:5:cloud-user-service:Java*"的微服务实例，针对每一个微服务实例进行检测
     */
    private void fragileServiceDetectDetail(List<SvcExternalMetricsRes> transResList) {




        transResList.forEach(transRes->{
            //分析每一个实例的情况


        });
    }

}
