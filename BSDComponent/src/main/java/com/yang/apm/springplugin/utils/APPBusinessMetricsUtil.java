package com.yang.apm.springplugin.utils;

import com.yang.apm.springplugin.pojo.metrics.business.SpringBusinessInfo;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.pojo.system.Service;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class APPBusinessMetricsUtil {


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

    public static void dealWithBusinessData(List<BusinessMetricsRes> businessMetricsResList, List<SpringBusinessInfo> springBusinessInfoList, Date startTime, Date endTime, Integer interval) {

        Map<String, List<SpringBusinessInfo>> map = groupByServiceInstance(springBusinessInfoList, springBusinessInfo -> generateUniqueNote(springBusinessInfo.getService()));
        map.forEach((uniqueNote,springSubBusInfoList)->{
            BusinessMetricsRes businessMetricsRes = createBusinessRes(uniqueNote,startTime,endTime,interval);

            Map<String, Long> businessCallMap = new HashMap<>();
            springSubBusInfoList.forEach(springBusinessInfo->{
                String methodName = springBusinessInfo.getLabels().getMethodName();
                businessCallMap.put(methodName,businessCallMap.getOrDefault(methodName,0L) + springBusinessInfo.getMethodCallCounter());
            });
            businessMetricsRes.setBusinessCallMap(businessCallMap);

            businessMetricsResList.add(businessMetricsRes);
        });
    }

    private static BusinessMetricsRes createBusinessRes(String uniqueNote, Date startTime, Date endTime, Integer interval) {
        BusinessMetricsRes businessMetricsRes = new BusinessMetricsRes();
        String[] split = uniqueNote.split("\\|");
        businessMetricsRes.setLanguage(split[0]);
        businessMetricsRes.setServiceName(split[1]);
        businessMetricsRes.setPodName(split[2]);
        businessMetricsRes.setStartTime(startTime);
        businessMetricsRes.setInterval(interval);
        businessMetricsRes.setEndTime(endTime);
        businessMetricsRes.setCollector(ManagementFactory.getRuntimeMXBean().getName());
        return businessMetricsRes;

    }
}
