package com.yang.apm.springplugin.controller;

import com.yang.apm.springplugin.model.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class HealthCheckController {
    @GetMapping("/health")
    @Operation(description = "提供健康检查")
    public ResponseDTO<String> healthCheck() {
        return ResponseDTO.success("Detection tool running successfully!");
    }
}
