package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.ServiceItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class GreedyContext extends ParentContext {

    public List<ServiceItem> greedySvc;

    public GreedyContext(){
        this.greedySvc = new ArrayList<>();
    }
    public void addGreedySvc(ServiceItem serviceItem){
        this.getGreedySvc().add(serviceItem);
    }
}
