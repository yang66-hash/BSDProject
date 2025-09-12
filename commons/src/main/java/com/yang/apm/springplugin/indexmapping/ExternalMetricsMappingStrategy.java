package com.yang.apm.springplugin.indexmapping;

import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;

import java.util.function.Function;

public class ExternalMetricsMappingStrategy implements IndexMappingStrategy{
    @Override
    public Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> createMapping() {
        return mappings -> mappings
                //设置无需动态推断字段类型
                .dynamic(DynamicMapping.False)
                .properties("startTime",p->p.date(d->d.format("yyyy-MM-dd HH:mm:ss")))
                .properties("endTime",p->p.date(d->d.format("yyyy-MM-dd HH:mm:ss")))
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
                //设置当前Object对象的存储是自动将map的key转换为子字段名的
                .properties("instanceAPICallNumMap",p->p.object(obj->obj.enabled(true)))
                .properties("APIExecTimeMap",p->p.object(obj->obj.enabled(true)))
                .properties("sqlQueryCount",p->p.keyword(k->k))
                .properties("slowQueryCount",p->p.keyword(k->k))
                .properties("serviceCallNumMap",p->p.object(obj->obj.enabled(true)));
    }
}
