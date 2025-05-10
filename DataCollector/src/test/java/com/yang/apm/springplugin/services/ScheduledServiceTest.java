package com.yang.apm.springplugin.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.yang.apm.springplugin.DataCollectApplication.class)
class ScheduledServiceTest {

    @Autowired
    private ScheduledService scheduledService;


    @Test
    public void testScheduledExternalMetrics(){
        scheduledService.getMetricsScheduled();
    }
}