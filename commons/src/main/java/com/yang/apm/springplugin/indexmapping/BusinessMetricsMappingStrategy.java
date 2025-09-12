package com.yang.apm.springplugin.indexmapping;

import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;

import java.util.function.Function;

public class BusinessMetricsMappingStrategy implements IndexMappingStrategy{
    @Override
    public Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> createMapping() {
        return mappings -> mappings
                //设置无需动态推断字段类型
                .dynamic(DynamicMapping.False)
                // SvcRes 基础字段
                .properties("startTime",p->p.date(d->d.format("yyyy-MM-dd HH:mm:ss")))
                .properties("endTime",p->p.date(d->d.format("yyyy-MM-dd HH:mm:ss")))
                .properties("interval",p->p.keyword(k->k))
                .properties("language",p->p.keyword(k->k))
                .properties("serviceName",p->p.keyword(k->k))
                .properties("podName",p->p.keyword(k->k))
                // BusinessMetricsRes 特有字段
                //设置当前Object对象的存储是自动将map的key转换为子字段名的
                .properties("businessCallMap",p->p.object(obj->obj.enabled(true)));
    }
} 