package com.yang.apm.springplugin.base.context.staticres;

import com.yang.apm.springplugin.base.Enum.GateWayType;
import com.yang.apm.springplugin.base.context.ParentContext;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class GateWayContext extends ParentContext {
    public boolean hasGateWay;
    public GateWayType type;

    public GateWayContext(){
        this.hasGateWay = false;
        this.status = true;
    }

}
