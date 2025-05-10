package com.yang.apm.springplugin.controller.staticanalysis;

import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.services.DetectionItemBufferService;
import com.yang.apm.springplugin.services.staticdetect.UnVersionedApiService;
import com.yang.model.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal-design")
@Slf4j
public class InternalDesignController {

    @Autowired
    private DetectionItemBufferService detectionItemBufferService;


    @Autowired
    private UnVersionedApiService unVersionedApiService;

    @RequestMapping(path = "/no-api-versioning",
            method = RequestMethod.POST)
    @Operation(description = "检测代码异味————缺少API版本")
    public ResponseDTO<String> unVersionedApis(@RequestBody RequestItem requestItem){
        log.info("No api version detection for "+ requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = unVersionedApiService.getUnVersionedApis(requestItem);
        detectionItemBufferService.addResItem(detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }


}
