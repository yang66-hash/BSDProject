package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TMSAnalysisResult {

    private Boolean isMussy;   //判断是否使用了多种编程语言
    private Integer langUsedNum;  //使用的语言总数量
    private String projectName;
    private Map<String,LangDetail> programingLang;

    public TMSAnalysisResult(){
        programingLang = new HashMap<>();
        langUsedNum = 0;
        projectName = null;
    }


}
