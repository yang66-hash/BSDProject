package com.yang.apm.springplugin.services;


import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CalculateService {

    @Autowired
    private RedisAsyncService redisAsyncService;

    /**
     * 计算历史数据的平均值，并将数据发送到es中进行存储，以便展示
     */
    public void calculateAvg4ExternalMetrics(){

        //从redis中拉取十分钟的数据进行计算，并将数据上传至es中
        String traceKeyPattern = ResType.EXTERNAL_METRICS.name() + "|" + redisAsyncService.searchInterval() + "|*";
        Set<String> keySet = redisAsyncService.matchedPatternSet(traceKeyPattern);
        log.info(String.valueOf(keySet.size()));
        log.info(keySet.toString());
        //将keySet按照实例进行划分,每一个实例下的所有数据在一个list中
        Map<String, List<String>> instanceDataMap = new HashMap<>();
        keySet.stream().forEach(key->{
            String instanceKey = key.substring(0, key.lastIndexOf("|"));
            log.info("instanceKey:" + instanceKey);
            instanceDataMap.computeIfAbsent(instanceKey, k->new ArrayList<>()).add(key);

        });

        // 对每个实例的数据进行计算并上传到 Elasticsearch 这里的uniqueNote: ResType|interval|serviceName|Language|podName
        instanceDataMap.forEach((UniqueNote,keys)->{
            List<SvcExternalMetricsRes> externalMetricsResList = new LinkedList<>();

            keys.forEach(key -> {
                // 从 Redis 中获取每个键对应的 hash 数据
                SvcExternalMetricsRes data = redisAsyncService.getHashAsObject(key, SvcExternalMetricsRes.class);
                externalMetricsResList.add(data);
            });
            externalMetricsResList.sort(Comparator.comparing(SvcExternalMetricsRes::getEndTime).reversed());

            SvcExternalMetricsRes calculatedData = calculateExternalMetrics(externalMetricsResList);

            //将平均值发送到es中
            System.out.println(calculatedData.toString());
        });
    }

    private SvcExternalMetricsRes calculateExternalMetrics(List<SvcExternalMetricsRes> externalMetricsResList) {
        SvcExternalMetricsRes result = new SvcExternalMetricsRes();

        // 初始化总和和计数器
        int totalRequestCount = 0;
        int totalReqSucCount = 0;
        int totalReqServerFailCount = 0;
        int totalResClientFailCount = 0;
        int totalFailPercent = 0;
        //times per minutes
        int totalThroughput = 0;
        int totalSucThroughput = 0;

        // 用来存储合并后的 Map 数据
        Map<String, Integer> totalInstanceAPICallNumMap = new HashMap<>();
        Map<String, Double> totalAPIExecTimeMap = new HashMap<>();
        int totalSqlQueryCount = 0;
        int totalSlowQueryCount = 0;

        Map<String, Integer> totalServiceCallNumMap = new HashMap<>();

        for (SvcExternalMetricsRes item : externalMetricsResList) {
            totalRequestCount += item.getRequestCount();
            totalReqSucCount += item.getReqSucCount();
            totalReqServerFailCount += item.getReqServerFailCount();
            totalResClientFailCount += item.getResClientFailCount();

            //基础的收集统计是在每一分钟的基础上的 所以直接吞吐量就是每一分钟内的请求量
            for (int i = 0; i < item.getThroughput().length; i++) {
                totalThroughput += item.getThroughput()[i];
            }
            for (int i = 0; i < item.getSucThroughput().length; i++) {
                totalSucThroughput += item.getSucThroughput()[i];
            }

            // 累加 instanceAPICallNumMap
            for (Map.Entry<String, Integer> entry : item.getInstanceAPICallNumMap().entrySet()) {
                totalInstanceAPICallNumMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            // 累加 APIExecTimeMap
            for (Map.Entry<String, Double> entry : item.getAPIExecTimeMap().entrySet()) {
                Integer apiCallCount = item.getInstanceAPICallNumMap().getOrDefault(entry.getKey(), 1);
                totalAPIExecTimeMap.merge(entry.getKey(), entry.getValue()*apiCallCount, Double::sum);
            }
            totalSqlQueryCount += item.getSqlQueryCount();
            totalSlowQueryCount += item.getSlowQueryCount();

            // 累加 serviceCallNumMap
            for (Map.Entry<String, Integer> entry : item.getServiceCallNumMap().entrySet()) {
                totalServiceCallNumMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        // 计算平均值
        double avgFailPercent = totalReqServerFailCount / (double) totalRequestCount;
        double totalLatency = totalAPIExecTimeMap.values().stream().mapToDouble(Double::doubleValue).sum();
        //计算所有接口调用的平均时延
        double avgLatency = totalLatency/totalInstanceAPICallNumMap.values().stream().mapToInt(Integer::intValue).sum();

        double avgThroughput = totalThroughput / (double)externalMetricsResList.size();
        double avgSucThroughput = totalSucThroughput / (double)externalMetricsResList.size();

        // 计算每个 API 的平均执行时间
        Map<String, Double> avgAPIExecTimeMap = new HashMap<>();
        for (Map.Entry<String, Double> entry : totalAPIExecTimeMap.entrySet()) {
            Integer apiCallCount = totalInstanceAPICallNumMap.getOrDefault(entry.getKey(), 1);
            avgAPIExecTimeMap.put(entry.getKey(), entry.getValue() / apiCallCount);
        }

        result.setRequestCount(totalRequestCount);
        result.setReqSucCount(totalReqSucCount);
        result.setReqServerFailCount(totalReqServerFailCount);
        result.setResClientFailCount(totalResClientFailCount);
        result.setFailPercent(avgFailPercent);
        result.setAvgLatency(avgLatency);
        result.setThroughput(new double[]{avgThroughput});
        result.setSucThroughput(new double[]{avgSucThroughput});
        result.setInstanceAPICallNumMap(totalInstanceAPICallNumMap);
        result.setAPIExecTimeMap(avgAPIExecTimeMap);
        result.setSqlQueryCount(totalSqlQueryCount);
        result.setSlowQueryCount( totalSlowQueryCount);
        result.setServiceCallNumMap(totalServiceCallNumMap);
        result.setEndTime(externalMetricsResList.get(0).getEndTime());
        result.setServiceName(externalMetricsResList.get(0).getServiceName());
        result.setLanguage(externalMetricsResList.get(0).getLanguage());
        result.setPodName(externalMetricsResList.get(0).getPodName());

        //获取窗口大小,若是redis中收集到的数据还不足用户设置的大小,那么窗口设置为实际大小
        result.setInterval(Math.min(redisAsyncService.searchTimeWindow(),redisAsyncService.searchCurWindow()));
        Date endTime = externalMetricsResList.get(0).getEndTime();
        // Convert Date to LocalDateTime
        LocalDateTime localEndTime = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        // Subtract 10 minutes
        LocalDateTime localStartTime = localEndTime.minusMinutes(externalMetricsResList.size());

        // Convert LocalDateTime back to Date
        Date startTime = Date.from(localStartTime.atZone(ZoneId.systemDefault()).toInstant());
        result.setStartTime(startTime);
        return result;
    }

    private List<SvcExternalMetricsRes> getDataFromRedis(Set<String> keySet) {
        //将keySet按照降序排列 取出全部或者是前十个
        keySet = keySet.stream().sorted(Comparator.reverseOrder())
                .limit(10)
                .collect(Collectors.toSet());
        List<SvcExternalMetricsRes> transResList = new LinkedList<>();
        for (String key : keySet) {
            SvcExternalMetricsRes transRes = redisAsyncService.getHashAsObject(key, SvcExternalMetricsRes.class);
            transResList.add(transRes);
        }
        return transResList;
    }



}
