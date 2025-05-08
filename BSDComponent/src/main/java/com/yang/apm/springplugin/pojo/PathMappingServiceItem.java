package com.yang.apm.springplugin.pojo.codemapping;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("path_mapping_service_item")
public class PathMappingServiceItem {


    @Schema(description = "微服务名称", example = "cloud-admin-service")
    private String serviceName;
    @Schema(description = "对应到本地仓库的绝对路径", example = "/usr/local/repository/xxx")
    private String pathInLocalRepository;
    @Schema(description = "所属仓库名", example = "xxx")
    private String repositoryBelongTo;

    public String flatItem(){
        return serviceName + "|" + pathInLocalRepository + "|" + repositoryBelongTo + "\n";
    }
}
