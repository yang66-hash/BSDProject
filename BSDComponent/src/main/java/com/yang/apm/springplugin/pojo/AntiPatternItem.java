package com.yang.apm.springplugin.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取相应异味数据（mysql中）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("anti_pattern_info")
public class AntiPatternItem {
    private String name;
    private String categoryName;
    private String typeName;
    private Boolean detectable;
    private String detectMethod;
}
