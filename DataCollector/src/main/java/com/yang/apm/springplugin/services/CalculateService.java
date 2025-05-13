package com.yang.apm.springplugin.services;


import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import com.yang.apm.springplugin.utils.IndexUtil;
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
    private CacheService cacheService;

    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;
    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;

    /**
     * @param list
     * @param clazz 需要是SvcRes的子类
     * @param <T>
     * @return 将指定的List<SvcRes>转为指定子类的list列表
     */
    private <T extends SvcRes> List<T> convertCache2SpecSubClaList(List<SvcRes> list,Class<T> clazz){
        return list.stream()
                .filter(clazz::isInstance).map(clazz::cast)
                .collect(Collectors.toList());
    }

    /**
     * 计算历史数据的平均值，并将数据发送到es中进行存储，以便展示
     */
    public void calculateAvg4ExternalMetrics(){

        //从cache中依据服务实例计算每一个服务实例的平均值 按照服务名-interval一个从缓存中复制计算
        //其中的key是service|interval
        Set<String> keySetInSvcIvlLevel = cacheService.getKeySetInSvcIvlLevel(ResType.EXTERNAL_METRICS.name());

        List<SvcExternalMetricsRes> historyCalList = new LinkedList<>();
        //针对每一个服务进行相应计算
        keySetInSvcIvlLevel.forEach(key->{
            Map<String, List<SvcRes>> resInServiceLevel = cacheService.getResInServiceLevel(ResType.EXTERNAL_METRICS.name(), key);
            //针对每一个实例进行计算
            resInServiceLevel.forEach((podName,svcResList)->{
                //依据每一个实例进行计算 计算完后将数据上传到ES
                List<SvcExternalMetricsRes> externalMetricsRes = convertCache2SpecSubClaList(svcResList, SvcExternalMetricsRes.class);
                SvcExternalMetricsRes svcExternalMetricsRes = calculateExternalMetricsByInstance(externalMetricsRes);
                historyCalList.add(svcExternalMetricsRes);
            });
        });

        String externalMetricsHistoryIndex = IndexUtil.getExternalMetricsIndex(intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY));

        //将数据存储到本地，然后统一发送， 不然数据发送失败 数据就消失了
        elasticsearchClientManager.bulkData2Index(externalMetricsHistoryIndex, historyCalList);


    }

    private SvcExternalMetricsRes calculateExternalMetricsByInstance(List<SvcExternalMetricsRes> externalMetricsResList) {
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

        //获取窗口大小,若是收集到的数据还不足用户设置的大小,那么窗口设置为实际大小
        result.setInterval(Math.min(intervalWindowMappingService.getValueByName(ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY),intervalWindowMappingService.getValueByName(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY)));
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

}
