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
        persistentIntegerCacheService.put(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY,60);
    }

    @Test
    public void putWindow() {
        persistentIntegerCacheService.put(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY,600);
    }
    @Test
    public void putCurWindow() {
        persistentIntegerCacheService.put(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY,60);
    }

    @Test
    public void delInterval() {
        persistentIntegerCacheService.delete(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    }

    @Test
    public void delWindow() {
        persistentIntegerCacheService.delete(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    }
    @Test
    public void delCurWindow() {
        persistentIntegerCacheService.delete(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY);
    }

}