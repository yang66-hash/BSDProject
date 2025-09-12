package com.yang.apm.springplugin.utils;

import com.yang.apm.springplugin.constant.ConstantUtil;

public class IndexUtil {

    /**
     * @param value 指定的索引文件中收集的时间的间隔 interval 或者 window
     * @return
     */
    // 提供索引名生成的静态方法
    public static String getExternalMetricsIndex(Integer value) {
        return ConstantUtil.METRICS_EXTERNAL_INDEX_PREFIX + "." + value + "s";
    }

    public static String getBusinessMetricsIndex(Integer value) {
        return ConstantUtil.METRICS_BUSINESS_INDEX_PREFIX + "." + value + "s";
    }

    public static String getInternalMetricsIndex(Integer value) {
        return ConstantUtil.METRICS_INTERVAL_INDEX_PREFIX + "." + value + "s";
    }
    public static String getDetectionResIndex() {
        return ConstantUtil.DETECTION_RESULT_INDEX;
    }

    /**
     * 获取循环依赖链路索引名称
     * @return 循环依赖索引名
     */
    public static String getCircularDependencyTracesIndex() {
        return ConstantUtil.CYCLE_DEPENDENCY_BUSINESS_INDEX_PREFIX;
    }

}
