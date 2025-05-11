package com.yang.apm.springplugin.manager.indexmapping;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;


import java.util.function.Function;

public interface IndexMappingStrategy {
    Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> createMapping();
}

