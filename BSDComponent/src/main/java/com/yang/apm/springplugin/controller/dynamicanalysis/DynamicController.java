package com.yang.apm.springplugin.controller.dynamicanalysis;


import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.services.RedisAsyncService;
import com.yang.apm.springplugin.services.dynamicdetect.detect.FragileServiceService;
import com.yang.model.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/dynamic")
@Slf4j
public class DynamicController {


    @Autowired
    private FragileServiceService fragileServiceService;

    //基础想法是将数据实时的拉取到Redis中，保证Redis中的数据始终是近30s的数据？

    @Autowired
    private RedisAsyncService redisAsyncService;


    @RequestMapping(path = "/fragile-service",
            method = RequestMethod.POST,
            produces = "application/json; charset=UTF-8",
            consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————是否是脆弱服务")
    public ResponseDTO<String> isFragileService(@RequestBody RequestItem requestItem){

        //基于历史数据做分析
//        fragileServiceService

        //还是针对微服务做检测
        //从redis中拉取数据，将数据进行分析


        return null;
    }


}
