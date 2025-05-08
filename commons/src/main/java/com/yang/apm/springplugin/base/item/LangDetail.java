package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class LangDetail {
    private Integer bytes;
    private List<String> filePath;

    public LangDetail(){
        filePath = new LinkedList<>();
    }

}
