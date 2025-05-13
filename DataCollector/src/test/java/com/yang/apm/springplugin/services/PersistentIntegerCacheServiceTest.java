package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PersistentIntegerCacheServiceTest {

    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;

    @Test
    public void putInterval() {
        System.out.println(System.getProperty("user.dir"));
    }

    @Test
    public void putWindow() {
        intervalWindowMappingService.setValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY,600);
    }
    @Test
    public void putCurWindow() {
        intervalWindowMappingService.setValueByName(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY,60);
    }

    @Test
    public void get() {
        System.out.println(intervalWindowMappingService.getValueByName(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY));
        System.out.println(intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY));
        System.out.println(intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY));
    }

}