package com.yang.apm.springplugin.pojo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mbst.repository")
@Data
public class GitSettings {
    @Schema(description = "用户名-depend on which code repository is used", example = "yang66-hash")
    private String username;
    @Schema(description = "密码", example = "changeme")
    private String password;
    @Schema(description = "远程代码仓库所在地址", example = "https://github.com/yang66-hash/PropertyManagementCloud" )
    private String remoteRepositories;
    @Schema(description = "本地代码仓库所在地址", example = "/usr/local/repository")
    private String localRepository;

}



