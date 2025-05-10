package com.yang.apm.springplugin.controller.staticanalysis;


import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.services.DetectionItemBufferService;
import com.yang.apm.springplugin.services.staticdetect.SharedLibraryService;
import com.yang.apm.springplugin.services.staticdetect.TooMuchStandardsService;
import com.yang.model.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/team-technology")
@Slf4j
public class TeamTechnologyController {



    @Autowired
    private DetectionItemBufferService detectionItemBufferService;


    @Autowired
    private SharedLibraryService sharedLibraryService;

    @Autowired
    private TooMuchStandardsService tooMuchStandardsService;

    /**
     * 传入的文件路径为单个微服务路径
     */
    @RequestMapping(path = "/shared-libraries",
            method = RequestMethod.POST,
            produces = "application/json; charset=UTF-8",
            consumes = {"text/plain", "application/*"})
    @Operation(description = "检测代码异味————Shared Libraries")
    public ResponseDTO<String> sharedLibraries(@RequestBody RequestItem requestItem) {
        log.info("Shared Libraries for "+ requestItem.getServiceName() + " start ...");
        DetectionResItem detectionResItem = sharedLibraryService.getSharedLibraries(requestItem);
        detectionItemBufferService.addResItem(detectionResItem);
        return ResponseDTO.success("Detect command reached.");
    }

    //语言检测服务器未准备好
//    @RequestMapping(path = "/too-many-standards",
//            method = RequestMethod.POST,
//            produces = "application/json; charset=UTF-8",
//            consumes = {"text/plain", "application/*"})
//    @Operation(description = "检测代码异味————Too Many Standards")
//    public ResponseDTO<String> tooManyStandards(@RequestBody RequestItem requestItem) {
//        log.info("Too Many Standards for "+ requestItem.getServiceName() + " start ...");
//        DetectionResItem detectionResItem = tooMuchStandardsService.getTMSServices(requestItem);
//        redisAsyncService.pushToRedis(ConstantUtil.REDIS_DETECTION_RECORD_LIST, detectionResItem);
//        return ResponseDTO.success("Detect command reached.");
//    }



}