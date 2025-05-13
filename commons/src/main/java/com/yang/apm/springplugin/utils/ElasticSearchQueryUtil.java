package com.yang.apm.springplugin.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;

import java.util.Date;

public class ElasticSearchQueryUtil {

    /**
     * @param field 时间戳属性字段
     * @param start 开始时间
     * @param end 结束时间
     * [startTime, endTIme)
     * @return 返回构造的查询
     */
    public static Query createTimeRangeQuery(String field, Date start, Date end) {
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
    public static Query createGTRangeQuery(String field, long bound) {
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
    public static Query createMatchQuery(String field, String value) {
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
    public static Query createAllExistQuery(String... fields) {
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
    public static Query createOneExistQuery(String... fields) {
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
    public static Query createAllCombinedQuery(Query... queries) {
        BoolQuery.Builder combinedQuery = new BoolQuery.Builder();
        for (Query q : queries) {
            combinedQuery.must(q);
        }
        return combinedQuery.build()._toQuery();
    }

}
