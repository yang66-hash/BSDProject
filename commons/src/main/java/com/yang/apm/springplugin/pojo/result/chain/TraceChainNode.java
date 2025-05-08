package com.yang.apm.springplugin.pojo.result.chain;

import com.yang.apm.springplugin.pojo.result.sql.DBModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class TraceChainNode {
    @Schema(description = "当前服务实例名称",example = "Java|cloud-user-service|win-20230627bhi")
    private String serviceName;
    @Schema(description = "当前请求的接口名称",example = "UserController#getDataById")
    private String APIName;
    /**
     * 当前请求是否包含关系型数据库查询
     */
    @Schema(description = "当前请求是否包含关系型数据库查询[MYSQL,oracle,SQLSERVER,POSTGRESQL,MariaDB,SQLITE,SYBASE]")

    private Boolean containSQL;
    /**
     * 若是containSQL为false 此属性为null
     * 每一条数据库请求记录为一个DBModel
     */
    @Schema(description = "若是containSQL为false 此属性为null,否则每一条数据库请求记录为一个DBModel")
    private List<DBModel> sqlModelList;
    @Schema(description = "当前请求的子请求")
    private List<TraceChainNode> subNodes;

    public TraceChainNode() {
        subNodes = new ArrayList<>();
    }

    public void addSubNode(TraceChainNode traceChainNode) {
        subNodes.add(traceChainNode);
    }
}
