package com.yang.apm.springplugin.indexmapping;

import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;

import java.util.function.Function;

public class InternalMetricsMappingStrategy implements IndexMappingStrategy{
    @Override
    public Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> createMapping() {
        return mappings -> mappings
                //对于复杂嵌套结构，开启动态映射但保持关键字段的类型定义
                .dynamic(DynamicMapping.True)
                // SvcRes 基础字段
                .properties("startTime",p->p.date(d->d.format("strict_date_time")))
                .properties("endTime",p->p.date(d->d.format("strict_date_time")))
                .properties("interval",p->p.keyword(k->k))
                .properties("collector",p->p.keyword(k->k))
                .properties("language",p->p.keyword(k->k))
                .properties("serviceName",p->p.keyword(k->k))
                .properties("podName",p->p.keyword(k->k))
                // SvcMetricsRes 特有字段 - 完全使用动态映射处理复杂嵌套结构
                //设置当前Object对象的存储是自动将map的key转换为子字段名的
                .properties("memoryRes",p->p.object(obj->obj.enabled(true)))
                .properties("gcRes",p->p.object(obj->obj.enabled(true)))
                .properties("jvmSummaryList",p->p.object(obj->obj.enabled(true)));
    }
} 