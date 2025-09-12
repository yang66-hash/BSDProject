package com.yang.apm.springplugin.controller.datacollector;

import static com.yang.apm.springplugin.constant.ConstantUtil.INTERVAL_OF_DYNAMIC_KEY;
import static com.yang.apm.springplugin.constant.ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY;

import com.yang.apm.springplugin.model.ResponseDTO;
import com.yang.apm.springplugin.pojo.IntervalWindowMapping;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class APIController {

    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;

    @PostMapping("interval-update")
    public ResponseDTO<String> setInterval4Dynamic(@RequestParam("interval") Integer interval){
        IntervalWindowMapping intervalWindowMapping = new IntervalWindowMapping();
        intervalWindowMapping.setName(INTERVAL_OF_DYNAMIC_KEY);
        intervalWindowMapping.setValue(interval);
        intervalWindowMappingService.save(intervalWindowMapping);
        return ResponseDTO.success("时间间隔设置成功");
    }


    @PostMapping("time-window-update")
    @Operation(description = "设置进行历史数据统计的时间窗口大小,默认10min")
    public ResponseDTO<String> setTimeWindow4Dynamic(@RequestParam("timeWindow") Integer timeWindow){
        IntervalWindowMapping intervalWindowMapping = new IntervalWindowMapping();
        intervalWindowMapping.setName(TIME_WINDOW_OF_DYNAMIC_KEY);
        intervalWindowMapping.setValue(timeWindow);
        return ResponseDTO.success("时间窗口设置成功");
    }

}
