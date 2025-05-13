package com.yang.apm.springplugin.manager;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;
import com.yang.apm.springplugin.constant.ConstantUtil;

import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.services.TraceService;
import com.yang.apm.springplugin.services.ESQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.function.Function;

@SpringBootTest
class ElasticsearchClientManagerTest {

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;

    @Autowired
    private ESQueryService ESQueryService;

    @Autowired
    private TraceService traceService;

    @Test
    public void createExternalMetrics10min(){
        Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> fn = mappings -> mappings
                .properties("timestamp",p->p.date(d->d.format("strict_date_time")))
                .properties("interval",p->p.keyword(k->k))
                .properties("language",p->p.keyword(k->k))
                .properties("serviceName",p->p.keyword(k->k))
                .properties("podName",p->p.keyword(k->k))
                .properties("requestCount",p->p.keyword(k->k))
                .properties("reqSucCount",p->p.keyword(k->k))
                .properties("reqServerFailCount",p->p.keyword(k->k))
                .properties("resClientFailCount",p->p.keyword(k->k))
                .properties("failPercent",p->p.double_(d->d))
                .properties("avgLatency",p->p.double_(d->d))
                .properties("throughput",p->p.double_(d->d))
                .properties("sucThroughput",p->p.double_(d->d))
                //设置当前Object对象的存储是自动将map的key转换为子字段名的
                .properties("instanceAPICallNumMap",p->p.object(obj->obj.enabled(true)))
                .properties("APIExecTimeMap",p->p.object(obj->obj.enabled(true)))
                .properties("sqlQueryCount",p->p.keyword(k->k))
                .properties("slowQueryCount",p->p.keyword(k->k))
                .properties("serviceCallNumMap",p->p.object(obj->obj.enabled(true)));
        elasticsearchClientManager.createIndexByName(ConstantUtil.METRICS_EXTERNAL_INDEX_PREFIX+ ".6000s",fn);
    }

    @Test
    public void createExternalMetrics1min(){
        Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> fn = mappings -> mappings
                .properties("timestamp",p->p.date(d->d.format("strict_date_time")))
                .properties("interval",p->p.keyword(k->k))
                .properties("language",p->p.keyword(k->k))
                .properties("serviceName",p->p.keyword(k->k))
                .properties("podName",p->p.keyword(k->k))
                .properties("requestCount",p->p.keyword(k->k))
                .properties("reqSucCount",p->p.keyword(k->k))
                .properties("reqServerFailCount",p->p.keyword(k->k))
                .properties("resClientFailCount",p->p.keyword(k->k))
                .properties("failPercent",p->p.double_(d->d))
                .properties("avgLatency",p->p.double_(d->d))
                .properties("throughput",p->p.double_(d->d))
                .properties("sucThroughput",p->p.double_(d->d))
                //设置当前Object对象的存储是自动将map的key转换为子字段名的
                .properties("instanceAPICallNumMap",p->p.object(obj->obj.enabled(true)))
                .properties("APIExecTimeMap",p->p.object(obj->obj.enabled(true)))
                .properties("sqlQueryCount",p->p.keyword(k->k))
                .properties("slowQueryCount",p->p.keyword(k->k))
                .properties("serviceCallNumMap",p->p.object(obj->obj.enabled(true)));
        elasticsearchClientManager.createIndexByName(ConstantUtil.METRICS_EXTERNAL_INDEX_PREFIX+ ".60s",fn);
    }

    @Test
    public void saveData2IndexTest(){
        List<SvcExternalMetricsRes> metricsInTraces = traceService.getMetricsInTraces("2025-04-29 16:05:00", 1);
        elasticsearchClientManager.bulkData2Index("bsd.analysis.metrics.external.1min",metricsInTraces);
    }

    @Test
    public void isIndexExistedTest(){
        boolean indexExisted = elasticsearchClientManager.isIndexExisted(ConstantUtil.METRICS_EXTERNAL_INDEX_PREFIX + ".1min");
        System.out.println(indexExisted);
    }



}