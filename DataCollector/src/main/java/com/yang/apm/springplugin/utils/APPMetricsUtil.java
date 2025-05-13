package com.yang.apm.springplugin.utils;


import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMGCInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMMemInfo;
import com.yang.apm.springplugin.pojo.metrics.jvm.last.APPJVMSummaryInfo;
import com.yang.apm.springplugin.pojo.result.jvm.JVMGCRes;
import com.yang.apm.springplugin.pojo.result.jvm.JVMMemoryRes;
import com.yang.apm.springplugin.pojo.result.jvm.JVMSummaryRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.pojo.system.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class APPMetricsUtil {

    /** 将数据按照服务实例进行划分
     * @param dataList 待划分的数据
     * @param uniqueNoteExtractor
     * @param <T>
     * @return 按照服务实例划分后的数据map，key为uniqueNote, value为其下的数据集合
     */
    public static <T> Map<String, List<T>> groupByServiceInstance(List<T> dataList, Function<T, String> uniqueNoteExtractor){
        return dataList.stream().collect(Collectors.groupingBy(uniqueNoteExtractor));
    }

    /**
     * @param service 包含的service字段
     * 生成服务实例的唯一标识
     * @return
     */
    private static String generateUniqueNote(Service service) {
        return service.getLanguage().getName() + "|" +
                service.getName() + "|" +
                service.getNode().getName();
    }

    /**
     * @param svcMetricsResList 存储结果
     * @param appSummaryList    获取的有关 jvm system的数据条目
     * @param startTime
     * @param endTime
     * @param interval
     */
    public static void dealWithSummaryData(List<SvcMetricsRes> svcMetricsResList, List<APPJVMSummaryInfo> appSummaryList, Date startTime, Date endTime, Integer interval){
        //按服务实例划分数据
        Map<String, List<APPJVMSummaryInfo>> map = groupByServiceInstance(appSummaryList, appjvmSummaryInfo -> generateUniqueNote(appjvmSummaryInfo.getService()));

        //将划分的数据按条目进行处理
        map.forEach((uniqueNote, appjvmSummaryInfoList)->{
            SvcMetricsRes svcMetricsRes = createSvcMetricsRes(uniqueNote, startTime, endTime, interval);

            List<JVMSummaryRes> jvmSummaryResList = appjvmSummaryInfoList
                    .stream()
                    .map(APPJVMSummaryInfo::toJVMSummaryRes)
                    .collect(Collectors.toList());

            svcMetricsRes.setJvmSummaryList(jvmSummaryResList);
            svcMetricsResList.add(svcMetricsRes);
        });

    }

    /**
     * @param svcMetricsResList 存储结果
     * @param appjvmgcInfoList 待处理的数据
     */
    public static void dealWithGCData(List<SvcMetricsRes> svcMetricsResList, List<APPJVMGCInfo> appjvmgcInfoList) {
        Map<String, List<APPJVMGCInfo>> map = groupByServiceInstance(appjvmgcInfoList, appjvmgcInfo -> generateUniqueNote(appjvmgcInfo.getService()));
        //用于存储GC相关指标
        Map<String, List<JVMGCRes>> gcMap = new HashMap<>();
        map.forEach((uniqueNote, appjvmGCInfoList)->{
            SvcMetricsRes svcMetricsRes = findOrCreateSvcMetricsRes(svcMetricsResList, uniqueNote);
            appjvmGCInfoList.forEach(appjvmgcInfo -> {
                JVMGCRes jvmGCRes = new JVMGCRes();
                jvmGCRes.setTimestamp(appjvmgcInfo.getTimeStamp());
                jvmGCRes.setGcCount(appjvmgcInfo.getJvmgcInfo().getGc().getCount());
                jvmGCRes.setGcTime(appjvmgcInfo.getJvmgcInfo().getGc().getTime());
                String labelName = appjvmgcInfo.getLabels().getName();
                //获取labelName对应的list
                List<JVMGCRes> jvmgcResList = gcMap.computeIfAbsent(labelName, k -> new ArrayList<>());
                jvmgcResList.add(jvmGCRes);
                gcMap.put(labelName,jvmgcResList);
            });
            svcMetricsRes.setGcRes(gcMap);
        });
    }

    /**
     * 找到uniqueNote对应的SvcMetricsRes，没找到就直接创建新的
     * @param svcMetricsResList
     * @param uniqueNote
     * @return
     */
    private static SvcMetricsRes findOrCreateSvcMetricsRes(List<SvcMetricsRes> svcMetricsResList, String uniqueNote) {
        return svcMetricsResList.stream()
                .filter(svcMetricsRes -> {
                    String unique = svcMetricsRes.getLanguage() + "|" +
                            svcMetricsRes.getServiceName() + "|" +
                            svcMetricsRes.getPodName();
                    return unique.equals(uniqueNote);
                }).findFirst().orElse(new SvcMetricsRes());
    }

    public static void dealWithMemData(List<SvcMetricsRes> svcMetricsResList, List<APPJVMMemInfo> appjvmMemInfoList) {


        Map<String, List<APPJVMMemInfo>> heapMap = groupByServiceInstance(appjvmMemInfoList.stream()
                .filter(info -> info.getJvmMemInfo().getMemory().getHeap() != null)
                .collect(Collectors.toList()), appjvmMemInfo -> generateUniqueNote(appjvmMemInfo.getService()));

        System.out.println(heapMap.toString());

        Map<String, List<APPJVMMemInfo>> nonHeapMap = groupByServiceInstance(appjvmMemInfoList.stream()
                .filter(info -> info.getJvmMemInfo().getMemory().getNonHeap() != null)
                .collect(Collectors.toList()), appjvmMemInfo -> generateUniqueNote(appjvmMemInfo.getService()));


        heapMap.forEach((uniqueNote, appjVMMemInfoList)->{
            SvcMetricsRes svcMetricsRes = findOrCreateSvcMetricsRes(svcMetricsResList, uniqueNote);
            //声明eden survivor old的heap存储
            Map<String, List<JVMMemoryRes>> jvmMemMap = new HashMap<>();
//
//            List<JVMMemoryRes> edenPool = new ArrayList<>();
//            List<JVMMemoryRes> survivorPool = new ArrayList<>();
//            List<JVMMemoryRes> oldPool = new ArrayList<>();

            appjVMMemInfoList.forEach(appjvmMemInfo -> {
                JVMMemoryRes jvmMemoryRes = new JVMMemoryRes();
                jvmMemoryRes.setTimestamp(appjvmMemInfo.getTimeStamp());
                jvmMemoryRes.setCommitted(appjvmMemInfo.getJvmMemInfo().getMemory().getHeap().getPool().getCommitted());
                jvmMemoryRes.setUsed(appjvmMemInfo.getJvmMemInfo().getMemory().getHeap().getPool().getUsed());
                jvmMemoryRes.setMax(appjvmMemInfo.getJvmMemInfo().getMemory().getHeap().getPool().getMax());
                String labelName = appjvmMemInfo.getLabels().getName();
                List<JVMMemoryRes> jvmMemoryResList = jvmMemMap.computeIfAbsent(labelName, k -> new ArrayList<>());
                jvmMemoryResList.add(jvmMemoryRes);
            });
            svcMetricsRes.setMemoryRes(jvmMemMap);

        });

        nonHeapMap.forEach((uniqueNote, appjVMMemInfoList)->{
            SvcMetricsRes svcMetricsRes = findOrCreateSvcMetricsRes(svcMetricsResList, uniqueNote);
            //声明metaspace|codeCache|compressedClassSpace存储

            Map<String, List<JVMMemoryRes>> jvmMemMap = new HashMap<>();

//            List<JVMMemoryRes> metaSpacePool = new ArrayList<>();
//            List<JVMMemoryRes> codeCachePool = new ArrayList<>();
//            List<JVMMemoryRes> compressedClaSacPool = new ArrayList<>();

            appjVMMemInfoList.forEach(appjvmMemInfo -> {
                JVMMemoryRes jvmMemoryRes = new JVMMemoryRes();
                jvmMemoryRes.setTimestamp(appjvmMemInfo.getTimeStamp());
                jvmMemoryRes.setCommitted(appjvmMemInfo.getJvmMemInfo().getMemory().getNonHeap().getPool().getCommitted());
                jvmMemoryRes.setUsed(appjvmMemInfo.getJvmMemInfo().getMemory().getNonHeap().getPool().getUsed());
                jvmMemoryRes.setMax(appjvmMemInfo.getJvmMemInfo().getMemory().getNonHeap().getPool().getMax());
                String labelName = appjvmMemInfo.getLabels().getName();
                List<JVMMemoryRes> jvmMemoryResList = jvmMemMap.computeIfAbsent(labelName, k -> new ArrayList<>());
                jvmMemoryResList.add(jvmMemoryRes);

            });
            //加到MemRes中
            Map<String, List<JVMMemoryRes>> memoryRes = svcMetricsRes.getMemoryRes();
            memoryRes.putAll(jvmMemMap);
            svcMetricsRes.setMemoryRes(memoryRes);
        });
    }

    private static SvcMetricsRes createSvcMetricsRes(String uniqueNote, Date startTime, Date endTime, Integer interval) {
        SvcMetricsRes svcMetricsRes = new SvcMetricsRes();
        String[] split = uniqueNote.split("\\|");
        svcMetricsRes.setLanguage(split[0]);
        svcMetricsRes.setServiceName(split[1]);
        svcMetricsRes.setPodName(split[2]);
        svcMetricsRes.setStartTime(startTime);
        svcMetricsRes.setInterval(interval);
        svcMetricsRes.setEndTime(endTime);
        return svcMetricsRes;
    }
}
