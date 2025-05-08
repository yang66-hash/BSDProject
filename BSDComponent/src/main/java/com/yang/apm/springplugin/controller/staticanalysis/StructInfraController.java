package com.yang.apm.springplugin.controller.staticanalysis;


import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.services.RedisAsyncService;
import com.yang.apm.springplugin.services.staticdetect.ESBService;
import com.yang.apm.springplugin.services.staticdetect.HubService;
import com.yang.apm.springplugin.services.staticdetect.NoGateWayService;
import com.yang.model.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/struct-infra")
public class StructInfraController {



    @Autowired
    private RedisAsyncService redisAsyncService;

    @Autowired
    private ESBService esbService;

    @Autowired
    private NoGateWayService noGateWayService;


    @Autowired
    public HubService hubService;

    //    单体架构提出，暂不纳入检测
//    @RequestMapping(path = "/cyclic-references",
//            method = RequestMethod.POST)
//    @Operation(description = "检测代码异味————cyclic references")
//    public ResponseDTO<String> cyclicReferences(@RequestBody RequestItem requestItem){
//        log.info("Cyclic references for "+ requestItem.getServiceName() + " start ...");
//        DetectionResItem detectionResItem = cyclicReferenceService.getCyclicReference(requestItem);
//        log.info(detectionResItem.toString());
//        redisAsyncService.pushToRedis(ConstantUtil.REDIS_DETECTION_RECORD_LIST, detectionResItem);
//        return ResponseDTO.success("Detect command reached.");
//    }
    @RequestMapping(path = "/esb-usage",
            method = RequestMethod.POST)
    @Operation(description = "检测代码异味————ESB Usage")
    public ResponseDTO<String> ESBUsage(@RequestBody RequestItem requestItem) {
        log.info("ESB usage for " + requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = esbService.getESBServices(requestItem);
        log.info(detectionResItem.toString());
        redisAsyncService.pushToRedis(ConstantUtil.REDIS_DETECTION_RECORD_LIST, detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/no-api-gateway", method = RequestMethod.POST)
    @Operation(description = "检测代码异味———No API gateway")
    public ResponseDTO<String> noGateWay(@RequestBody RequestItem requestItem) {
        log.info("No API gateway for " + requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = noGateWayService.isExistGateWay(requestItem);
        log.info(detectionResItem.toString());
        redisAsyncService.pushToRedis(ConstantUtil.REDIS_DETECTION_RECORD_LIST, detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }


    @RequestMapping(path = "hub-like-dependency",method = RequestMethod.POST,
                    produces = "application/json; charset=UTF-8",
                    consumes = {"text/plain", "application/*"})
    @Operation(description = "检测代码异味————Hub-like Dependency")
    public ResponseDTO<String> hubLikeDependency(@RequestBody RequestItem requestItem) {
        log.info("Hub-like Dependency for " + requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = hubService.getHubClass(requestItem);
        redisAsyncService.pushToRedis(ConstantUtil.REDIS_DETECTION_RECORD_LIST, detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }

}


