package com.yang.apm.springplugin.controller;

import com.yang.apm.springplugin.model.ResponseDTO;
import com.yang.apm.springplugin.services.PersistentIntegerCacheService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static com.yang.apm.springplugin.constant.ConstantUtil.INTERVAL_OF_DYNAMIC_KEY;
import static com.yang.apm.springplugin.constant.ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY;

@RestController
public class APIController {

    @Autowired
    private PersistentIntegerCacheService persistentIntegerCacheService;

    @GetMapping("/health")
    @Operation(description = "提供健康检查")
    public ResponseDTO<String> healthCheck() {
        return ResponseDTO.success("Data collector running successfully!");
    }

    @PostMapping("interval-update")
    public ResponseDTO<String> setInterval4Dynamic(@RequestParam("interval") Integer interval){
        persistentIntegerCacheService.put(INTERVAL_OF_DYNAMIC_KEY, interval);
        return ResponseDTO.success("时间间隔设置成功");
    }


    @PostMapping("time-window-update")
    @Operation(description = "设置进行历史数据统计的时间窗口大小,默认10min")
    public ResponseDTO<String> setTimeWindow4Dynamic(@RequestParam("timeWindow") Integer timeWindow){
        persistentIntegerCacheService.put(TIME_WINDOW_OF_DYNAMIC_KEY, timeWindow);
        return ResponseDTO.success("时间窗口设置成功");
    }

}
