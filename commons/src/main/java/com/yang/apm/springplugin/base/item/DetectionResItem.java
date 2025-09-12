package com.yang.apm.springplugin.base.item;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.yang.apm.springplugin.constant.ConstantUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.Date;

/**
 * 用于封装检测结果（ES）
 */
@Data
@NoArgsConstructor
@Document(indexName = ConstantUtil.DETECTION_RESULT_INDEX)
public class DetectionResItem {

    @Field(name = "status")
    private Boolean status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Field(name = "timestamp")
    private Date timestamp;
    @Field(name = "detectionID")
    private String detectionID;
    @Field(name = "detector")
    private String detector;
    @Field(name = "name")
    private String name;
    @Field(name = "categoryName")
    private String categoryName;
    @Field(name = "typeName")
    private String typeName;
    @Field(name = "detectMethod")
    private String detectMethod;
    @Field(name = "targetInstance")
    private String targetInstance;
    @Field(name = "context")
    private Object context;
}
