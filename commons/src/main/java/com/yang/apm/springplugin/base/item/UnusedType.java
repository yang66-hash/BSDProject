package com.yang.apm.springplugin.base.item;

import lombok.Data;

/**
 * @description:
 * @author: xyc
 * @date: 2023-01-10 22:55
 */
@Data
public class UnusedType {
    public String name;
    public boolean isInterface;
    public boolean isAbstractClass;
}
