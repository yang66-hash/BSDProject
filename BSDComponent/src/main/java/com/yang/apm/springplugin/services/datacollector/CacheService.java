package com.yang.apm.springplugin.services.datacollector;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 缓存time_window时间内的所有数据，便于本地计算
 */
@Service
@Slf4j
public class CacheService {


    //（假想1000实例）设计本地缓存 因为本地数据计算的并发最高不可能超过1000 times per minute
    /**
     * 三级结构：
     *   dataType -->
     *      serviceName|interval -->
     *         Cache<podName, List<SvcRes>>   // 第三层用 Guava Cache，实现过期回收
     */
    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;


    private ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                    String,
                    Cache<String, List<SvcRes>>>> svcCache = new ConcurrentHashMap<>();

    /**
     * 存储相应type类型的在时间窗口内的平均数据
     *      * 三级结构：
     *      *   dataType -->
     *      *      serviceName -->   //不需要时间间隔数据 每一份服务的实例都只存储最新计算的平均值
     *      *           Cache<podName, SvcRes>   // 第三层用 Guava Cache，实现过期回收
     */
    private ConcurrentHashMap<
        String,
        ConcurrentHashMap<String,
            Cache<String,SvcRes>>> svcCalAvgCache = new ConcurrentHashMap<>();

    // ========================= svcCalAvgCache 操作方法 =========================
    
    /**
     * 存储平均数据到缓存
     */
    public <T extends SvcRes> void saveT2AvgCache( SvcRes svcRes, String resType) {
        if (svcRes == null) {
            return;
        }

        //设置第一层的key
        var byDataType = svcCalAvgCache.computeIfAbsent(resType, k -> new ConcurrentHashMap<>());

        //遍历数据

        //构造第二层的key
        Integer expiredTime = intervalWindowMappingService.getValueByName(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY);
        String serviceInterval = svcRes.getServiceName();
        var podCache = byDataType.computeIfAbsent(
            serviceInterval,
            si -> CacheBuilder.newBuilder()
                .expireAfterWrite(expiredTime, TimeUnit.SECONDS)
                .build());

        String podName = svcRes.getPodName();

        podCache.put(podName, svcRes);
        log.info("cached [resType={}, serviceInterval={}, pod={}] ",
            resType, serviceInterval, podCache);
    }

    /**
     * 从服务层面取出所有实例的平均值（泛型版本）
     * @param resType 资源类型
     * @param serviceInterval 服务间隔键
     * @param clazz 期望的返回类型
     * @return 指定类型的数据映射
     */
    public <T extends SvcRes> Map<String, T> getAvgDataInServiceLevel(
        String resType,
        String serviceInterval,
        Class<T> clazz
        ){
        var secondLevelCache = svcCalAvgCache.get(resType);
        if (secondLevelCache == null) {
            return Collections.emptyMap();
        }
        var podCache = secondLevelCache.get(serviceInterval);
        if (podCache == null) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new HashMap<>();
        for (String podName : podCache.asMap().keySet()) {
            SvcRes svcRes = podCache.getIfPresent(podName);

            if (clazz.isInstance(svcRes)) {
                result.put(podName, clazz.cast(svcRes));
            } else if (svcRes != null) {
                log.warn("跳过类型不匹配的数据: podName={}, 期望类型={}, 实际类型={}", 
                    podName, clazz.getSimpleName(), svcRes.getClass().getSimpleName());
            }
        }

        return result;
    }
    

    //将缓存中的检测结果发送到ES中
    /**
     * 用于存储当前缓存中数据 时间跨度的end
     * 因为如果存在某一个interval内没有数据收集到，那么cache不会记录这一段的空数据
     */
    @Setter
    @Getter
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date curTime;

//    private final HashSet<DetectionResItem>

    public <T extends SvcRes> void saveT2Cache(List<T> svcResList, String resType) {
        if (svcResList == null || svcResList.isEmpty()) {
            return;
        }

        //设置第一层的key
        var byDataType = svcCache.computeIfAbsent(resType, k -> new ConcurrentHashMap<>());

        //遍历数据

        for(T sr: svcResList) {
            //构造第二层的key
            Integer expiredTime = intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
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

//    /**
//     *  按 resType + serviceInterval 批量获取所有实例的列表
//     * @param resType 数据类型
//     * @param serviceInterval 时间窗口
//     * @return
//     */
//    public Map<String, List<SvcRes>> getResInServiceLevel(
//            String resType,
//            String serviceInterval
//    ) {
//        var byDataType = svcCache.get(resType);
//        if (byDataType == null) {
//            return Collections.emptyMap();
//        }
//        var podCache = byDataType.get(serviceInterval);
//        if (podCache == null) {
//            return Collections.emptyMap();
//        }
//
//        Map<String, List<SvcRes>> result = new HashMap<>();
//        for (String podName : podCache.asMap().keySet()) {
//            List<SvcRes> list = podCache.getIfPresent(podName);
//            if (list != null) {
//                result.put(podName, list);
//            }
//        }
//        return result;
//    }

    /**
     *  按 resType + serviceInterval 批量获取所有实例的列表 (泛型版本)
     * @param resType 数据类型
     * @param serviceInterval 时间窗口
     * @param latestTimes 要获取最新的几条数据（最多10条）
     * @param clazz 期望返回的类型
     * @return 指定类型的Map
     */
    public <T extends SvcRes> Map<String, List<T>> getResInServiceLevel(
            String resType,
            String serviceInterval,
            int latestTimes,
            Class<T> clazz
    ) {
        var byDataType = svcCache.get(resType);
        if (byDataType == null) {
            return Collections.emptyMap();
        }
        var podCache = byDataType.get(serviceInterval);
        if (podCache == null) {
            return Collections.emptyMap();
        }

        Map<String, List<T>> result = new HashMap<>();


        for (String podName : podCache.asMap().keySet()) {
            List<SvcRes> list = podCache.getIfPresent(podName);
            //缓存最多存储10条记录
            if (list != null && (latestTimes > 10||latestTimes<0)) {
                List<T> typedList = list.stream()
                    .map(clazz::cast)
                    .collect(Collectors.toList());
                if (!typedList.isEmpty()) {
                    result.put(podName, typedList);
                }
            }else if (list!=null ) {
                List<T> last3 = list.stream()
                    .map(clazz::cast)
                    .skip(Math.max(list.size() - 3, 0))
                    .toList();
                if (!last3.isEmpty()) {
                    result.put(podName, last3);
                }
            }
        }
        return result;
    }
//
//    /**
//     * 只获取最新一条数据（基础版本）
//     * @param resType 资源类型
//     * @param serviceInterval 服务间隔键
//     * @return 基础版本，返回 SvcRes
//     */
//    public Map<String, SvcRes> getLatestResInServiceLevel(
//        String resType,
//        String serviceInterval
//    ) {
//        var byDataType = svcCache.get(resType);
//        if (byDataType == null) {
//            return Collections.emptyMap();
//        }
//        var podCache = byDataType.get(serviceInterval);
//        if (podCache == null) {
//            return Collections.emptyMap();
//        }
//
//        Map<String, SvcRes> result = new HashMap<>();
//        for (String podName : podCache.asMap().keySet()) {
//            List<SvcRes> list = podCache.getIfPresent(podName);
//            if (list != null && !list.isEmpty()) {
//                result.put(podName, list.get(list.size()-1));
//            }
//        }
//        return result;
//    }

    /**
     * 只获取最新一条数据（泛型版本）
     * @param resType 资源类型
     * @param serviceInterval 服务间隔键
     * @param clazz 期望的返回类型
     * @return 指定类型的最新数据映射
     */
    public <T extends SvcRes> Map<String, T> getLatestResInServiceLevel(
        String resType,
        String serviceInterval,
        Class<T> clazz
    ) {
        var byDataType = svcCache.get(resType);
        if (byDataType == null) {
            return Collections.emptyMap();
        }
        var podCache = byDataType.get(serviceInterval);
        if (podCache == null) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new HashMap<>();
        for (String podName : podCache.asMap().keySet()) {
            List<SvcRes> list = podCache.getIfPresent(podName);
            if (list != null && !list.isEmpty()) {
                SvcRes latestRes = list.get(list.size()-1);
                if (clazz.isInstance(latestRes)) {
                    result.put(podName, clazz.cast(latestRes));
                } else {
                    log.warn("跳过类型不匹配的最新数据: podName={}, 期望类型={}, 实际类型={}", 
                        podName, clazz.getSimpleName(), latestRes.getClass().getSimpleName());
                }
            }
        }
        
        log.debug("获取最新数据: resType={}, serviceInterval={}, 期望类型={}, 匹配数量={}/{}", 
            resType, serviceInterval, clazz.getSimpleName(), result.size(), podCache.asMap().size());
        
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
        int interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
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

    /**
     * 缓存异常数据（如循环依赖异常）
     * @param trace 异常链路数据
     * @param cacheKey 缓存键
     */
    public void saveAnomalousTrace(SvcRes trace, String cacheKey) {
        try {
            String serviceKey = generateServiceKey(trace, cacheKey);
            String podKey = trace.getPodName() != null ? trace.getPodName() : "unknown";
            
            // 获取或创建缓存
            Cache<String, List<SvcRes>> cache = getOrCreateAnomalousCache(cacheKey, serviceKey);
            
            // 获取现有列表或创建新列表
            List<SvcRes> existingList = cache.getIfPresent(podKey);
            if (existingList == null) {
                existingList = new ArrayList<>();
            }
            
            existingList.add(trace);
            cache.put(podKey, existingList);
            
            log.debug("缓存异常数据: cacheKey={}, serviceKey={}, podKey={}", 
                cacheKey, serviceKey, podKey);
                
        } catch (Exception e) {
            log.error("缓存异常数据失败", e);
        }
    }

    /**
     * 获取或创建异常数据缓存
     */
    private Cache<String, List<SvcRes>> getOrCreateAnomalousCache(String cacheKey, String serviceKey) {
        return svcCache
            .computeIfAbsent(cacheKey, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(serviceKey, k -> CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(24, TimeUnit.HOURS) // 异常数据保存24小时
                .build());
    }

    /**
     * 生成服务级别的缓存键
     */
    private String generateServiceKey(SvcRes trace, String cacheKey) {
        return String.format("%s_%s_%d", 
            trace.getServiceName() != null ? trace.getServiceName() : "unknown",
            cacheKey,
            trace.getInterval() != null ? trace.getInterval() : 0);
    }

    /**
     * 获取异常数据
     * @param cacheKey 缓存键
     * @return 异常数据列表
     */
    public List<SvcRes> getAnomalousTraces(String cacheKey) {
        List<SvcRes> result = new ArrayList<>();
        
        ConcurrentHashMap<String, Cache<String, List<SvcRes>>> typeCache = svcCache.get(cacheKey);
        if (typeCache != null) {
            for (Cache<String, List<SvcRes>> cache : typeCache.values()) {
                for (List<SvcRes> traces : cache.asMap().values()) {
                    if (traces != null) {
                        result.addAll(traces);
                    }
                }
            }
        }
        
        return result;
    }
}
