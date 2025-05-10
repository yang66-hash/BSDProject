package com.yang.apm.springplugin.services;


import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.yang.apm.springplugin.constant.ConstantUtil.BSD_INTEGER_CACHE_NAME;

/**
 * 用于存储历史数据计算相关的时间窗口大小
 */
@Service
@Slf4j
public class PersistentIntegerCacheService {
    private final DB db;
    private final ConcurrentMap<String, Integer> map;
    private final String FILE_NAME = "cache.db";


    public PersistentIntegerCacheService() {
        this.db = DBMaker
                .fileDB(new File(FILE_NAME))
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make();
        this.map = db.hashMap(BSD_INTEGER_CACHE_NAME,
                Serializer.STRING,
                Serializer.INTEGER)
                .createOrOpen();

    }
    public Integer get(String key) {
        return map.get(key);
    }

    public void put(String key, Integer value) {
        map.put(key, value);
        db.commit();  // 提交到文件
        log.info("set data successfully [key:{}->value:{}]",key,value);
    }

}
