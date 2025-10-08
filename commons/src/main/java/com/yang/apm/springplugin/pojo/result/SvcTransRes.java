package com.yang.apm.springplugin.pojo.result;

import com.yang.apm.springplugin.pojo.result.chain.RequestChain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 每一个服务实例对应的某一时间窗口下搜集到的数据
 * author: yang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SvcTransRes extends SvcRes {



    /**
     * record the number of API calls and the tree structure of the chain requests between this interval
     */
    @Schema(description = "当前服务实例在当前时间间隔内的存在循环依赖的请求链路以及相关API的调用次数（单条请求链路）")
    private RequestChain requestChain;

    @Schema(description = "当前请求链路深度")
    private Integer chainDepth;
    @Schema(description = "当前请求链路总的sql请求次数")
    private Integer sqlCount;
    @Schema(description = "当前请求链路各部分请求执行时间总和")
    private Double totalExecTime;

}
