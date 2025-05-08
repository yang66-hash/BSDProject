package com.yang.apm.springplugin.services.dynamicdetect.detect;

import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.services.RedisAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FragileServiceService {


    @Autowired
    private RedisAsyncService redisAsyncService;
    public void fragileServiceDetect(RequestItem requestItem) {

        Integer interval = redisAsyncService.searchInterval();
        String traceKeyPattern = ResType.EXTERNAL_METRICS.name() + "|" + interval + "|"  + requestItem.getServiceName()  +"|" + ConstantUtil.LANGUAGE + "|*";
        //默认监测该服务下的所有实例 针对所有实例的所有数据进行计算 将数据按照时间顺序升序
        List<String> keyList = redisAsyncService.matchedPatternSet(traceKeyPattern).stream().sorted().collect(Collectors.toList());
        log.info(keyList.toString());
        //针对所有的数据计算历史平均值，将TimeWindow下的历史数据计算出平均值，上传至ES




        //先检测redis中是否存在相关的traces信息，若有解析 进行检测
//        if (!keySet.isEmpty()){
//            //执行检测
////            List<SvcExternalMetricsRes> dataFromRedis = getDataFromRedis(keySet);
////            fragileServiceDetectDetail(dataFromRedis);
//        }
    }
//

    /**
     * 脆弱服务的具体检测逻辑
     * 依据数据svc transaction results进行检测
     * 三个检测维度：
     *   1.请求失败率
     *   2.请求时延
     *   3.服务实例吞吐量
     *   通过正则表达式匹配key值为"TRACE:5:cloud-user-service:Java*"的微服务实例，针对每一个微服务实例进行检测
     */
    private void fragileServiceDetectDetail(List<SvcExternalMetricsRes> transResList) {




        transResList.forEach(transRes->{
            //分析每一个实例的情况


        });
    }

}
