package com.yang.apm.springplugin.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("interval_window_mapping")
public class IntervalWindowMapping {
    private Integer id;
    private String name;
    private Integer value;
}
