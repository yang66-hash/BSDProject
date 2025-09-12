package com.yang.apm.springplugin.services.datacollector.dynamicdetect;

import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.services.datacollector.metricscollector.BusinessMetricsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BusinessServiceTest {

    @Autowired
    private BusinessMetricsService businessService;

    @Test
    public void testBusinessService() {

        List<BusinessMetricsRes> businessMetrics = businessService.getBusinessMetrics("2025-09-12 07:49:46", 300);
        System.out.println(businessMetrics);
    }

}