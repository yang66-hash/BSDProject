package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class ServiceContext extends ParentContext {
    public Set<String> serviceList;
    public ServiceContext(){
        serviceList = new HashSet<>();
    }
}
