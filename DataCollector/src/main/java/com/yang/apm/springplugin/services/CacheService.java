package com.yang.apm.springplugin.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class CacheService {


    //（假想1000实例）设计本地缓存 因为本地数据计算的并发最高不可能超过1000 times per minute
    /**
     * 三级结构：
     *   dataType -->
     *      serviceName_interval -->
     *         Cache<podName, List<SvcRes>>   // 第三层用 Guava Cache，实现过期回收
     */

    @Autowired
    private PersistentIntegerCacheService persistentIntegerCacheService;

    private final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                    String,
                    Cache<String, List<SvcRes>>>> svcCache = new ConcurrentHashMap<>();

//    private final HashSet<DetectionResItem>

    //将缓存中的检测结果发送到ES中


    public <T extends SvcRes> void saveT2Cache(List<T> svcResList, String resType) {
        if (svcResList == null || svcResList.isEmpty()) {
            return;
        }

        //设置第一层的key
        var byDataType = svcCache.computeIfAbsent(resType, k -> new ConcurrentHashMap<>());

        //遍历数据

        for(T sr: svcResList) {
            //构造第二层的key
            Integer expiredTime = persistentIntegerCacheService.get(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
            String serviceInterval = sr.getServiceName() + "|" + sr.getInterval();
            var podCache = byDataType.computeIfAbsent(
                    serviceInterval,
                    si -> CacheBuilder.newBuilder()
                            .expireAfterWrite(expiredTime, TimeUnit.SECONDS)
                            .build());
            
            String podName = sr.getPodName();
            List<SvcRes> list = podCache.getIfPresent(podName);
            if (list == null) {
                list = new LinkedList<>();
            }
            list.add(sr);
            podCache.put(podName, list);
            log.info("cached [resType={}, serviceInterval={}, pod={}] -> list size={}",
                    resType, serviceInterval, podCache, list.size());

        }
    }

    /**
     * @return 依据唯一的服务实例映射相应的数据
     *  resType -> serviceName|interval -> podName
     */
    public Optional<List<SvcRes>> getResInPodLevel(
            String resType,
            String serviceName,
            String interval,
            String podName) {
        String serviceInterval = serviceName + "|" + interval;
        var byDataType = svcCache.get(resType);
        if (byDataType == null) {
            return Optional.empty();
        }
        var podCache = byDataType.get(serviceInterval);
        if (podCache == null) {
            return Optional.empty();
        }
        return Optional.of(podCache.getIfPresent(podName));
    }

    /**
     *  按 resType + serviceInterval 批量获取所有实例的列表
     * @param resType 数据类型
     * @param serviceInterval 时间窗口
     * @return
     */
    public Map<String, List<SvcRes>> getResInServiceLevel(
            String resType,
            String serviceInterval
    ) {
        var byDataType = svcCache.get(resType);
        if (byDataType == null) {
            return Collections.emptyMap();
        }
        var podCache = byDataType.get(serviceInterval);
        if (podCache == null) {
            return Collections.emptyMap();
        }

        Map<String, List<SvcRes>> result = new HashMap<>();
        for (String podName : podCache.asMap().keySet()) {
            List<SvcRes> list = podCache.getIfPresent(podName);
            if (list != null) {
                result.put(podName, list);
            }
        }
        return result;
    }

    /**
     * 按照数据类型resType直接将所有服务的所有实例的所有数据返回
     * @return
     */
    public Map<String,Map<String,List<SvcRes>>> getResInTypeLevel(String resType) {
        var byTypeData = svcCache.get(resType);
        if (byTypeData == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, List<SvcRes>>>  result = new HashMap<>();
        for (Map.Entry<String,Cache<String,List<SvcRes>>> entry :byTypeData.entrySet()){
            String serviceInterval = entry.getKey();
            Cache<String, List<SvcRes>> podCache = entry.getValue();
            //将缓存中的所有实例的所有数据全部输出

            Map<String, List<SvcRes>> podMap = new HashMap<>();
            for (String podName : podCache.asMap().keySet()) {
                List<SvcRes> list = podCache.getIfPresent(podName);
                if (list != null && !list.isEmpty()) {
                    podMap.put(podName, list);
                }
            }
            if (!podMap.isEmpty()){
                result.put(serviceInterval, podMap);
            }
        }
        return result;
    }


    /**
     * @return 按照类型，获取某一类型下某一服务的所有实例的key值集合
     */
    public Set<String> getKeySetInSvcIvlLevel(String resType){
        AbstractMap<String, Cache<String, List<SvcRes>>> byDataType = svcCache.get(resType);
        if (byDataType == null) {
            return Collections.emptySet();
        }
        Set<String> keySet = byDataType.keySet();
        int interval = persistentIntegerCacheService.get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        //过滤数据，只获取当前数据收集时间窗口的正常数据
        Set<String> filteredKeySet = keySet.stream().filter(sr -> sr.endsWith("|" + interval)).collect(Collectors.toSet());
        return filteredKeySet;
    }

    public boolean isCacheEmpty(){
        if (svcCache.isEmpty()){
            return true;
        }

        for (Map.Entry<String, ConcurrentHashMap<String, Cache<String, List<SvcRes>>>> entry : svcCache.entrySet()){
            AbstractMap<String, Cache<String, List<SvcRes>>> innerMap = entry.getValue();
            if (innerMap == null || innerMap.isEmpty()){
                continue;
            }

            for (Map.Entry<String, Cache<String, List<SvcRes>>> innerEntry : innerMap.entrySet()){
                Cache<String, List<SvcRes>> cache = innerEntry.getValue();
                if (cache == null || cache.asMap().isEmpty()){
                    continue;
                }
                for (List<SvcRes> list : cache.asMap().values()) {
                    if (list != null && !list.isEmpty()) {
                        return false;  // 如果有非空的 List，表示 map 有数据
                    }
                }

            }

        }
        return true;
    }
}
