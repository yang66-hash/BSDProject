package com.yang.apm.springplugin.constant;

public class ConstantUtil {
    //300000us
    public final static Integer SQL_EXEC_TIME_BOUND = 300000;


    public static final String NO_TARGET_INSTANCE = "No Specific Instance";

    public static final String REDIS_DETECTION_RECORD_LIST = "bs-detection-record-list";


    public static final String ELASTIC_SEARCH_DETECTION_RES_INDEX = "bs_detection_res_set";

    public static final String INTERVAL_OF_DYNAMIC_KEY = "bsd_dynamic_interval";
    public static final String TIME_WINDOW_OF_DYNAMIC_KEY = "bsd_dynamic_time_window";
    public static final String INCREMENT_WINDOW_OF_DYNAMIC_KEY = "bsd_dynamic_cur_window";
    public static final String BSD_INTEGER_CACHE_NAME = "integerCache";


    //用于数据external metrics的存储分为windows 和 interval 两部分数据
    public static final String METRICS_EXTERNAL_INDEX_PREFIX = "bsd.analysis.metrics.external";

//    //不同的JVM使用可能使用不同的垃圾回收器 同时不同JDK版本 默认的JVM GC也有所不同
//    public final static String OLD_GENERATION = "Old Generation";
//    public final static String YOUNG_GENERATION = "Young Generation";
//
//    public final static String HEAP_EDEN_SPACE = "Eden Space";
//    public final static String HEAP_SURVIVOR_GENERATION = "Survivor Space";
//    public final static String HEAP_OLD_GEN = "Old Gen";
//
//    public final static String NON_HEAP_META_SPACE = "Metaspace";
//    public final static String NON_HEAP_COMPRESSED_CLASS_SPACE = "Compressed Class Space";
//    public final static String NON_HEAP_CODE_CACHE = "CodeCache";
//
//
//    /**
//     * 判断A是否包含字符串B
//     * @return
//     */
//    public static boolean isAContainsB(String A,String B){
//        if (A==null||B==null)
//            return false;
//        if(A.toLowerCase().contains(B.toLowerCase())){
//            return true;
//        }
//        return false;
//    }
}
