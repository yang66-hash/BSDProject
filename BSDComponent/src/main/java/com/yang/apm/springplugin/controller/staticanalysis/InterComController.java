package com.yang.apm.springplugin.controller.staticanalysis;


import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.services.RedisAsyncService;
import com.yang.apm.springplugin.services.staticdetect.HardCodeService;
import com.yang.model.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/com-inter")
@Slf4j
public class InterComController {

    @Autowired
    private HardCodeService hardCodeService;

    @Autowired
    private RedisAsyncService redisAsyncService;

    /**
     * 传入的文件路径为单个微服务路径
     */
    @RequestMapping(path = "/hardcoded-endpoints",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=UTF-8",
                    consumes = {"text/plain", "application/*"})
    @Operation(description = "检测代码异味————硬编码")
    public ResponseDTO<String> hardCodeIPAndPort(@RequestBody RequestItem requestItem) throws IOException {
        log.info("Hard Code detection for "+ requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = hardCodeService.detectHardCode(requestItem);
        redisAsyncService.pushToRedis(ConstantUtil.REDIS_DETECTION_RECORD_LIST, detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }



}
