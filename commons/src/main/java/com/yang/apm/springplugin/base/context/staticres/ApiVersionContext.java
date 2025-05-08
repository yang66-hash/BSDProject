package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;


@Data
public class ApiVersionContext extends ParentContext {
    public Map<String, Map<String, String>> unversionedMap;
    public Map<String, Map<String, String>> missingUrlMap;
    public ApiVersionContext(){
        this.unversionedMap = new HashMap<>();
        this.missingUrlMap = new HashMap<>();
    }

    public void addUnversionedApis(String serviceName, Map<String, String> methodAndApi){
        this.unversionedMap.put(serviceName, methodAndApi);
    }

    public void addMissingUrlMap(String serviceName, Map<String, String> methodAndApi){
        this.missingUrlMap.put(serviceName, methodAndApi);
    }

}
