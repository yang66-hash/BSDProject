package com.yang.apm.springplugin.services.datacollector;

import com.yang.apm.springplugin.BSDComponentApplication;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.indexmapping.ExternalMetricsMappingStrategy;
import com.yang.apm.springplugin.manager.datacollector.ElasticsearchClientManager;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import com.yang.apm.springplugin.utils.IndexUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BSDComponentApplication.class)
class ScheduledServiceTest {

    @Autowired
    private ScheduledService scheduledService;

    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;


    @Test
    public void testScheduledExternalMetrics(){
        scheduledService.getMetricsScheduled();
    }

    @Test
    public void createIndexByStrategyTest(){
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        Integer timeWindow = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);

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