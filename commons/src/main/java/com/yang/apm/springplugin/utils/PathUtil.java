package com.yang.apm.springplugin.utils;

import java.io.File;

public class PathUtil {
    /**
     * 将一个路径字符串从头截取到到倒数第一个分隔符
     * @return
     */
    public static String extractPathPart(String path) {
        // 获取文件系统分隔符
        String separator = File.separator;

        // 查找最后一个分隔符的位置
        int lastSeparatorIndex = path.lastIndexOf(separator);

        // 如果找到了分隔符，截取路径，否则返回原始路径
        if (lastSeparatorIndex != -1) {
            return path.substring(0, lastSeparatorIndex);
        } else {
            return path;  // 如果没有找到分隔符，返回整个路径
        }
    }
}
