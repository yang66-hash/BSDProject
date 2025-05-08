package com.yang.apm.springplugin.base.item;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * 用于封装检测结果（ES）
 */
@Data
@NoArgsConstructor
@Document(indexName = "bs_detection_res_set")
public class DetectionResItem {

    @Field(name = "status")
    private Boolean status;
    @Field(name = "timestamp")
    private String timestamp;
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
