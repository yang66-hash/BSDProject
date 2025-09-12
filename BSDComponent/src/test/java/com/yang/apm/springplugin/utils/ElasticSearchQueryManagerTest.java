package com.yang.apm.springplugin.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.manager.BSDESClientManager;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.yang.apm.springplugin.BSDComponentApplication.class)
class ElasticSearchQueryManagerTest {

    @Autowired
    private BSDESClientManager bsdesClientManager;

    @Test
    void createTermsAggregation() {
       Query matchQuery = ElasticSearchQueryUtil.createMatchQuery("serviceName", "cloud-user-service");

       SearchRequest summaryRequest = new SearchRequest.Builder()
               .index("bsd.analysis.metrics.external.600s")
               .query(matchQuery)
               .aggregations("by_podName", a->a.terms(t->t.field("podName")
                               .size(ConstantUtil.MAX_INSTANCE_NUM))
                               .aggregations("latest_hits",th->th.topHits(hit->hit.sort(s->s.field(f-> f.field("endTime").order(SortOrder.Desc))).size(1))))
               .build();
    }
    @Test
    void test(){

        //按照实例分组查询相应的数据进行计算统计
        SearchRequest searchRequest = SearchRequest.of(builder -> builder
                .index("bsd.analysis.metrics.external.600s")
                //无需hit数据，返回聚合数据
                .query(q->q
                        .match(m->m
                                .field("serviceName")
                                .query("cloud-admin-service")
                        )
                )
                .aggregations("by_podName", a->a
                        .terms(t->t.field("podName")
                                .size(100))
                        .aggregations("latest_hits",th->th
                                .topHits(t->t.sort(s->s.field(f-> f
                                        .field("endTime")
                                        .order(SortOrder.Desc)))
                                        .size(1)
                                )
                        )
                )
        );
        try {
            SearchResponse<JsonData> searchResponse = bsdesClientManager.getElasticsearchClient().search(searchRequest, JsonData.class);
            System.out.println(searchResponse);
            List<StringTermsBucket> byPodName = searchResponse.aggregations().get("by_podName").sterms().buckets().array();
            System.out.println(byPodName);
            for (StringTermsBucket byPodNameBucket : byPodName) {
                Hit<JsonData> latestHits = byPodNameBucket.aggregations().get("latest_hits").topHits().hits().hits().get(0);
                JsonData source = latestHits.source();
                System.out.println("------------------------------------------");
                System.out.println(source.toJson().toString());
                System.out.println("-----------------------------------------");
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //为什么取不出数据

    }
}