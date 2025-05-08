package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class WrongCutContext extends ParentContext {
    public Map<String,Map<String,Integer>> wrongCutMap;

    public WrongCutContext(){
        this.wrongCutMap = new HashMap<>();
    }
}
