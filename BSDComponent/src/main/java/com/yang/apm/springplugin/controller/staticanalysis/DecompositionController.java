package com.yang.apm.springplugin.controller.staticanalysis;


import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.services.DetectionItemBufferService;

import com.yang.apm.springplugin.services.staticdetect.GreedyService;
import com.yang.apm.springplugin.services.staticdetect.ScatteredService;
import com.yang.apm.springplugin.services.staticdetect.WrongServiceCutService;
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
@RequestMapping("/decomposition")
@Slf4j
public class DecompositionController {

    @Autowired
    private ScatteredService scatteredService;

    @Autowired
    private DetectionItemBufferService detectionItemBufferService;

    @Autowired
    public WrongServiceCutService wrongServiceCutService;
    @Autowired
    private GreedyService greedyService;

    /**
     * 传入的文件路径为单个微服务路径
     */
    @RequestMapping(path = "/scattered-functionality",
            method = RequestMethod.POST,
            produces = "application/json; charset=UTF-8",
            consumes = {"text/plain", "application/*"})
    @Operation(description = "检测代码异味————Scattered Functionality")
    public ResponseDTO<String> scatteredFunctionality(@RequestBody RequestItem requestItem) {
        log.info("Scattered Functionality for "+ requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = scatteredService.getSFServices(requestItem);
        detectionItemBufferService.addResItem(detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/wrong-cuts", method = RequestMethod.POST)
    @Operation(description = "检测代码异味————Wrong cuts")
    public ResponseDTO<String> wrongCuts(@RequestBody RequestItem requestItem) {
        log.info("Wrong cuts for "+ requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = wrongServiceCutService.getWrongServiceCutServices(requestItem);
        detectionItemBufferService.addResItem(detectionResItem);
        return ResponseDTO.success("Detect command reached.");

    }

    @RequestMapping(path = "/microservice-greedy",method = RequestMethod.POST,
                    produces = "application/json; charset=UTF-8",
                    consumes = {"text/plain", "application/*"})
    @Operation(description = "检测代码异味————Microservice greedy")
    public ResponseDTO<String> microserviceGreedy(@RequestBody RequestItem requestItem) throws IOException {
        log.info("Microservice greedy for "+ requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = greedyService.getGreedySvc(requestItem);
        detectionItemBufferService.addResItem(detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }



}
