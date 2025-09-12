package com.yang.apm.springplugin.indexmapping;

import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;

import java.util.function.Function;

/**
 * 循环依赖链路信息的索引映射策略
 * 用于存储检测到循环依赖的 SvcTransRes 数据
 */
public class CircularDependencyTracesMappingStrategy implements IndexMappingStrategy {
    
    @Override
    public Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> createMapping() {
        return mappings -> mappings
                //对于循环依赖数据，开启动态映射以适应复杂的链路结构
                .dynamic(DynamicMapping.True)
                
                // SvcRes 基础字段
                .properties("startTime",p->p.date(d->d.format("yyyy-MM-dd HH:mm:ss")))
                .properties("endTime",p->p.date(d->d.format("yyyy-MM-dd HH:mm:ss")))
                .properties("interval",p->p.keyword(k->k))
                .properties("language",p->p.keyword(k->k))
                .properties("serviceName",p->p.keyword(k->k))
                .properties("podName",p->p.keyword(k->k))
                
                // SvcTransRes 特有字段 - RequestChain 类型
                .properties("requestChain",p->p.object(obj->obj.enabled(true)))
                
                // 添加循环依赖检测相关的元数据字段
                .properties("detectionMetadata",p->p.object(obj->obj.enabled(true)));
    }
} 