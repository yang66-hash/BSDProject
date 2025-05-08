package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.Set;
import java.util.TreeSet;

@Data
public class SvcCallDetail {
    private Boolean isESBUsage;
    private Set<String> callService;
    private Set<String> calledService;

    public SvcCallDetail(){
        callService = new TreeSet<>();
        calledService = new TreeSet<>();
    }
}
