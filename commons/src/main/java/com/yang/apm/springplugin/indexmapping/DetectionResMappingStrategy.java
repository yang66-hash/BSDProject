package com.yang.apm.springplugin.indexmapping;

import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;

import java.util.function.Function;

public class DetectionResMappingStrategy implements IndexMappingStrategy{
    @Override
    public Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> createMapping() {
        return mappings -> mappings
                //设置无需动态推断字段类型
                .dynamic(DynamicMapping.False)
                .properties("timestamp",p->p.date(d->d.format("yyyy-MM-dd HH:mm:ss")))
                .properties("status",p->p.boolean_(b->b))
                .properties("detectionID",p->p.keyword(k->k))
                .properties("detector",p->p.text(k->k))
                .properties("name",p->p.keyword(k->k))
                .properties("categoryName",p->p.keyword(k->k))
                .properties("typeName",p->p.keyword(k->k))
                .properties("detectMethod",p->p.keyword(k->k))
                .properties("targetInstance",p->p.keyword(k->k))
                .properties("context",p->p.object(obj->obj.enabled(true)));
    }
}
