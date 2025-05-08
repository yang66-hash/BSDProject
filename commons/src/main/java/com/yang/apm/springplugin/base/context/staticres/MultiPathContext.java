package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.MultiPathItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class MultiPathContext extends ParentContext {
    public List<MultiPathItem> multiItems;

    public MultiPathContext(){
        this.multiItems = new ArrayList<>();
    }
}
