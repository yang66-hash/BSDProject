package com.yang.apm.springplugin.services.db;

import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IntervalWindowMappingServiceTest {


    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;
    @Test
    void getValueByName() {
        intervalWindowMappingService.getValueByName("bsd_dynamic_interval");

    }

}