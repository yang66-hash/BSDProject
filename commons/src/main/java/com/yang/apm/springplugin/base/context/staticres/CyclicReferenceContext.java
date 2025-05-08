package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.CyclicReferenceItem;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
@Data
public class CyclicReferenceContext  extends ParentContext {
    public Map<String, CyclicReferenceItem> cyclicReference;
    public CyclicReferenceContext(){
        this.cyclicReference=new HashMap<>();
    }
    public void addCyclicReference(String serviceName, String cyclicReferenceHolder, String subclass){
        if (cyclicReference.get(cyclicReferenceHolder)==null){
            cyclicReference.put(cyclicReferenceHolder,new CyclicReferenceItem(serviceName, cyclicReferenceHolder));
        }
        cyclicReference.get(cyclicReferenceHolder).addSubclass(subclass);
    }
}
