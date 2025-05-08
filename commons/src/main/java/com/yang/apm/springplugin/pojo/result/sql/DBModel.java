package com.yang.apm.springplugin.pojo.result.sql;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBModel {

    /**
     * 记录访问数据库名称
     */
    @Schema(description = "记录访问数据库名称")
    private String instance;
    /**
     * 记录执行语句
     */
    @Schema(description = "记录执行语句")
    private String statement;
    /**
     * 记录数据库类型
     */
    @Schema(description = "记录数据库类型")
    private String dbType;
    /**
     * 数据库详细类型
     */
    @Schema(description = "    @Schema(description = \"记录执行语句\")\n")
    private String  dbFurType;
    /**
     * 记录执行时间
     */
    @Schema(description = "记录执行时间")
    private Integer execTime;

    /**
     * 记录执行次数
     */
    @Schema(description = "记录执行次数")
    private Integer dataQueryNum;

}
