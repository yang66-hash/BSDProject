package com.yang.apm.springplugin.base.item;

/**
 * @description:
 * @author: xyc
 * @date: 2023-02-25 17:06
 */
import lombok.Data;

@Data
public class FileAnalysisItem {
    private String fileName;

    private String filePath;

    private String hardCode;

    private Integer place;  //the row position of the hardCode in the file
}
