package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.manager.BSDESClientManager;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ESService {

    @Autowired
    private BSDESClientManager bsdesClientManager;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取外部服务的统计数据
     * @param indexName 索引文件名
     * @param serviceName 服务名
     * @return key 为 podName，value 为 SvcExternalMetricsRes
     */
    public Map<String, SvcExternalMetricsRes> getExternalMetrics(String indexName, String serviceName) {
        // 构建搜索请求
        SearchRequest searchRequest = buildSearchRequest(indexName, serviceName);

        Map<String, SvcExternalMetricsRes> result = new HashMap<>();
        try {
            // 执行搜索请求
            SearchResponse<JsonData> searchResponse = bsdesClientManager.getElasticsearchClient().search(searchRequest, JsonData.class);

            // 获取聚合结果
            List<StringTermsBucket> byPodNameBuckets = searchResponse.aggregations()
                    .get("by_podName")
                    .sterms()
                    .buckets()
                    .array();
            System.out.println(byPodNameBuckets.size());
            System.out.println(byPodNameBuckets);
            // 处理每个 podName 的聚合结果
            for (StringTermsBucket bucket : byPodNameBuckets) {
                Hit<JsonData> latestHit = bucket.aggregations().get("latest_hits").topHits().hits().hits().get(0);
                JsonData source = latestHit.source();

                // 将 JsonData 转换为 SvcExternalMetricsRes
                SvcExternalMetricsRes svcExternalMetricsRes = objectMapper.readValue(source.toJson().toString(), SvcExternalMetricsRes.class);

                // 将结果存入 map
                result.put(svcExternalMetricsRes.getPodName(), svcExternalMetricsRes);
                log.info("Retrieve external metrics from Elasticsearch for service {} successfully!", serviceName);
            }
        } catch (IOException e) {
            log.error("Error retrieving external metrics from Elasticsearch for service {}: {}", serviceName, e.getMessage(), e);
        }
        return result;
    }





    /**
     * 构建搜索请求
     * @param indexName 索引文件名
     * @param serviceName 服务名
     * @return SearchRequest
     */
    private SearchRequest buildSearchRequest(String indexName, String serviceName) {
        return SearchRequest.of(builder -> builder
                .index(indexName)
                .size(0)  // No need for hits data, only aggregations
                .query(q -> q
                        .match(m -> m
                                .field("serviceName")
                                .query(serviceName)
                        )
                )
                .aggregations("by_podName", a -> a
                        .terms(t -> t.field("podName").size(ConstantUtil.MAX_INSTANCE_NUM))
                        .aggregations("latest_hits", th -> th
                                .topHits(t -> t.sort(s -> s.field(f -> f
                                                .field("endTime")
                                                .order(SortOrder.Desc)))
                                        .size(1)
                                )
                        )
                )
        );
    }
}