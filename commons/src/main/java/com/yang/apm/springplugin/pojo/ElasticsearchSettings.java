package com.yang.apm.springplugin.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("elasticsearchSettings")
@ConfigurationProperties(prefix = "spring.elasticsearch")
@Data
public class ElasticsearchSettings {
    @Schema(description = "用户名", example = "elastic")
    private String username;
    @Schema(description = "密码", example = "changeme")
    private String password;
    @Schema(description = "Elasticsearch集群/单实例的主机地址", example = "http://10.245.1.233:9200,http://10.245.1.135:9200")
    private List<String> uris;

}
