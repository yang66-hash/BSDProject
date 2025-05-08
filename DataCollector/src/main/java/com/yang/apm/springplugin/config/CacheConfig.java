package com.yang.apm.springplugin.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {


    @Bean
    public Cache<String, SvcRes> svcResCache(){
        return CacheBuilder.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(610, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

}
