package com.yang.apm.springplugin.base.context.staticres;


import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.FileAnalysisItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Data
public class HardCodeContext extends ParentContext {
    private Integer record = 0;
    private Map<String, List<FileAnalysisItem>> analysisResult = new HashMap();

    public HardCodeContext() {
    }

    public void add(String serviceName, FileAnalysisItem fileAnalysisResult) {
        if(!this.analysisResult.containsKey(serviceName))
                this.analysisResult.put(serviceName,new ArrayList<>());
        this.analysisResult.get(serviceName).add(fileAnalysisResult);
        this.record++;
    }



}
