package com.yang.apm.springplugin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有路径跨域请求
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 允许来自 任意客户端 请求
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // 允许的请求方法
                .allowedHeaders("*")  // 允许的请求头
                .allowCredentials(true);  // 允许携带认证信息
    }
}
