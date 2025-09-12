package com.yang.apm.springplugin.services.datacollector.calculate;

import com.yang.apm.springplugin.BSDComponentApplication;
import com.yang.apm.springplugin.services.datacollector.CalculateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BSDComponentApplication.class)
class CalculateServiceTest {

    @Autowired
    private CalculateService calculateService;

    @Test
    public void test10MinCalculate() {
        calculateService.calculateAvg4ExternalMetrics();
    }

}