package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * @description: includes a class having references of its direct subclasses
 * @field: cyclicReferenceHolder:the name of the super class
 * @field: subClasses: a set of subclasses that appear in their superclass
 */
@Data
public class CyclicReferenceItem {
    public String cyclicReferenceHolder;
    public String serviceName;
    public Set<String> subclasses;
    public int subclassNum;
    public CyclicReferenceItem(){
        this.subclasses =new HashSet<>();
    }
    public CyclicReferenceItem(String serviceName, String cyclicReferenceHolder){
        this.serviceName = serviceName;
        this.cyclicReferenceHolder=cyclicReferenceHolder;
        this.subclasses =new HashSet<>();
    }
    public void addSubclass(String subclass){
        this.subclasses.add(subclass);
        this.subclassNum=subclasses.size();
    }
}
