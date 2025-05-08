package com.yang.apm.springplugin.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class ElasticSearchQueryManager {

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;


    /**
     * @param field 时间戳属性字段
     * @param start 开始时间
     * @param end 结束时间
     * [startTime, endTIme)
     * @return 返回构造的查询
     */
    public Query createTimeRangeQuery(String field, Date start, Date end) {
        return new RangeQuery.Builder()
                .field(field)
                .gte(JsonData.of(start)) // 使用 ISO-8601 格式
                .lt(JsonData.of(end))
                .build()._toQuery();
    }

    /**
     * 查询number属性大于某一值的情况
     * (bound,+∞)
     */
    public Query createGTRangeQuery(String field, long bound) {
        return new RangeQuery.Builder()
                .field(field)
                .gt(JsonData.of(bound)) // 使用 ISO-8601 格式
                .build()._toQuery();
    }




    /**
     * @param field
     * @param value
     * 构造匹配查询
     */
    public Query createMatchQuery(String field, String value) {
        return new MatchQuery.Builder()
                .field(field)
                .query(value)
                .build()._toQuery();
    }


    /**
     * 构造多个属性是否存在的查询（支持可变参数）
     * @param fields 必须存在的字段属性（可变参数）
     * @return 组合后的布尔查询
     */
    public Query createAllExistQuery(String... fields) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        for (String field : fields) {
            ExistsQuery existsQuery = new ExistsQuery.Builder()
                    .field(field)
                    .build();
            boolQuery.must(existsQuery._toQuery());
        }

        return boolQuery.build()._toQuery();
    }


    /**
     * 构造多个属性是否存在的查询（支持可变参数）
     * @param fields 可能存在的字段属性集合（可变参数）
     * @return 组合后的布尔查询
     */
    public Query createOneExistQuery(String... fields) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        for (String field : fields) {
            ExistsQuery existsQuery = new ExistsQuery.Builder()
                    .field(field)
                    .build();
            boolQuery.should(existsQuery._toQuery());
        }

        return boolQuery.build()._toQuery();
    }

    /**
     * 支持可变参数 所有Query必须均要满足
     * @param queries 必须满足的Query（可变参数）
     * @return 组合后的布尔查询
     */
    public Query createAllCombinedQuery(Query... queries) {
        BoolQuery.Builder combinedQuery = new BoolQuery.Builder();
        for (Query q : queries) {
            combinedQuery.must(q);
        }
        return combinedQuery.build()._toQuery();
    }


    /**
     * @param request 封装后的查询请求
     * @param clazz 接收查数据目的接收类的类对象
     * @return 查询到的数据集合
     * @throws IOException
     */
    public <T> List<T> executeSearch(SearchRequest request, Class<T> clazz) throws IOException {
        List<T> result = new ArrayList<>();
        SearchResponse<T> response =  elasticsearchClientManager.getElasticsearchClient().search(request, clazz);
        for (Hit<T> hit : response.hits().hits()) {
            result.add(hit.source());
        }
        return result;
    }


}
