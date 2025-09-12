package com.yang.apm.springplugin.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    public static Date calculateStartTime(Date endTime, int intervalInSecond) {
        return new Date(endTime.getTime() - intervalInSecond * 1000);
    }

    public static Date parseStr2Date(String timeStr) throws ParseException {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time = dateTimeFormatter.parse(timeStr);
        return time;
    }


    /**
     * @param date1
     * @param date2
     * @param timeValue 单位：min
     * @return 判断两个之间是否差了timeValue分钟以上 是返回true
     */
    public static boolean isDiffMoreThanCustomMin(Date date1, Date date2, Integer timeValue){
        // 获取两个Date的时间戳（毫秒数）
        long diffInMillis = Math.abs(date1.getTime() - date2.getTime());

        // 2分钟等于120,000毫秒
        long twoMinutesInMillis = timeValue * 60 * 1000;

        // 如果差值大于120,000毫秒，返回true
        return diffInMillis > twoMinutesInMillis;
    }
}
