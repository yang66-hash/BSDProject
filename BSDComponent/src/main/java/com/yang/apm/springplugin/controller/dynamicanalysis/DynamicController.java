package com.yang.apm.springplugin.controller.dynamicanalysis;


import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.services.DetectionItemBufferService;
import com.yang.apm.springplugin.services.dynamicdetect.FragileServiceService;
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

    @Autowired
    private DetectionItemBufferService detectionItemBufferService;


    //动态检测依旧按照服务名来进行检测，不具体到指定实例进行检测 而是直接全部检测

    @RequestMapping(path = "/fragile-service",
            method = RequestMethod.POST,
            produces = "application/json; charset=UTF-8",
            consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————是否是脆弱服务")
    public ResponseDTO<String> isFragileService(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = fragileServiceService.fragileServiceDetect(requestItem);
        detectionItemBufferService.addResItem(detectionResItem);

        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }


}
