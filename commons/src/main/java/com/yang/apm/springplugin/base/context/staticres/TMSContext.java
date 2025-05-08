package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.TMSAnalysisResult;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TMSContext extends ParentContext {
    public Map<String, Double> languageCount;
    public Map<String, List<String>> fileList;
    public TMSAnalysisResult valueMap;

}
