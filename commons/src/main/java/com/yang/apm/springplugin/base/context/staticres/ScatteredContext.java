package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.context.ParentContext;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2023-03-14 15:53
 */
@Data
public class ScatteredContext extends ParentContext {

    public List<Set<String>> scatteredList;

    public ScatteredContext(){
        scatteredList = new ArrayList<>();
    }
}
