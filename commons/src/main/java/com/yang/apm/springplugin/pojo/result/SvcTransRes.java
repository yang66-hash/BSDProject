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
    @Schema(description = "当前服务实例在当前时间间隔内的所有请求链路以及相关API的调用次数")
    private List<RequestChain> requestChainList;



}
