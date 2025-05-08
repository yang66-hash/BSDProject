package com.yang.apm.springplugin.base.context.dynamicres;

import com.yang.apm.springplugin.base.context.ParentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FragileServiceContext extends ParentContext {

    //展示脆弱服务相关的阈值指标，封装成数据集合 用于可视化展示？
    //统计过去30s（或者是单位时间内的失败率）

    //时间间隔
    private Integer metricsInterval;
    //当前时间间隔内
    @Schema(description = "当前时间间隔内的请求失败率")
    private Double failPercent;
    @Schema(description = "当前时间间隔内的请求次数")
    private Integer requestCount;
    @Schema(description = "当前时间间隔内的所有请求的平均时延")
    private Double avgLatency;


}
