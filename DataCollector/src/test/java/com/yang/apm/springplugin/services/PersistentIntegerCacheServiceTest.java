package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.constant.ConstantUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PersistentIntegerCacheServiceTest {

    @Autowired
    private PersistentIntegerCacheService persistentIntegerCacheService;

    @Test
    public void putInterval() {
        persistentIntegerCacheService.put(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY,1);
    }

    @Test
    public void putWindow() {
        persistentIntegerCacheService.put(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY,10);
    }
    @Test
    public void putCurWindow() {
        persistentIntegerCacheService.put(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY,5);
    }
}