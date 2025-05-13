package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.yang.apm.springplugin.BSDComponentApplication.class)
class ESServiceTest {


    @Autowired
    private ESService esService;

    @Test
    void getExternalMetrics() {
        Map<String, SvcExternalMetricsRes> externalMetrics = esService.getExternalMetrics("bsd.analysis.metrics.external.600s", "cloud-user-service");

        System.out.println(externalMetrics);
    }
}