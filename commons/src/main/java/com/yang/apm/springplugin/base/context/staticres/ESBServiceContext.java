package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import com.yang.apm.springplugin.base.item.SvcCallDetail;
import lombok.Data;

import java.util.HashMap;

@Data
public class ESBServiceContext extends ParentContext {
    public HashMap<String, SvcCallDetail> result;

}
