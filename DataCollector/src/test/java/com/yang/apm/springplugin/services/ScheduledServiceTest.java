package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import com.yang.apm.springplugin.manager.indexmapping.ExternalMetricsMappingStrategy;
import com.yang.apm.springplugin.utils.IndexUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.yang.apm.springplugin.DataCollectApplication.class)
class ScheduledServiceTest {

    @Autowired
    private ScheduledService scheduledService;

    @Autowired
    private PersistentIntegerCacheService persistentIntegerCacheService;

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;


    @Test
    public void testScheduledExternalMetrics(){
        scheduledService.getMetricsScheduled();
    }

    @Test
    public void createIndexByStrategyTest(){
        Integer interval = persistentIntegerCacheService.get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        Integer timeWindow = persistentIntegerCacheService.get(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);

        String externalMetricsIndex = IndexUtil.getExternalMetricsIndex(interval);
        String externalMetricsHistoryIndex = IndexUtil.getExternalMetricsIndex(timeWindow);
        if (!elasticsearchClientManager.isIndexExisted(externalMetricsIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(externalMetricsIndex,new ExternalMetricsMappingStrategy());
        }
        if (!elasticsearchClientManager.isIndexExisted(externalMetricsHistoryIndex)) {
            elasticsearchClientManager.createIndexWithStrategy(externalMetricsHistoryIndex,new ExternalMetricsMappingStrategy());
        }
    }
}