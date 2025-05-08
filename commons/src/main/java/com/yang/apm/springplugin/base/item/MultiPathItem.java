package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: xyc
 * @date: 2023-02-24 15:37
 */
@Data
public class MultiPathItem {
    public String name;
    public String serviceName;
    public List<List<String>> routeList;

}
