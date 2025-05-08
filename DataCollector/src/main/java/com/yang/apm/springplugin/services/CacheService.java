package com.yang.apm.springplugin.services;

import com.google.common.cache.Cache;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CacheService {

    private final Cache<String, SvcRes> cache;

    @Autowired
    public CacheService(Cache<String, SvcRes> cache) {
        this.cache = cache;

    }

    public <T extends SvcRes> void saveT2Cache(List<T> svcResList, String resType, String collectTime) {
        if (svcResList == null || svcResList.isEmpty()) {
            return;
        }
        String keyPrefix = resType + "|" + svcResList.get(0).getInterval();
        //将数据转为Map<>

        svcResList.forEach(sr->{
            String key = keyPrefix
                    + "|" + sr.getServiceName()
                    + "|" + sr.getLanguage()
                    + "|" + sr.getPodName()
                    + "|" + collectTime;
            cache.put(key, sr);
            log.info("cached[{}] -> {}", key, sr);
        });

        log.info(keyPrefix + " data written to in‑JVM cache");
    }

    /**
     * @param key 依据实例的唯一设定的key获取相应的数据
     * @return
     */
    public Optional<SvcRes> getFromCache(String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

}
