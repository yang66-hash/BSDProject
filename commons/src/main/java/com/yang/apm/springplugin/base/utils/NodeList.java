package com.yang.apm.springplugin.base.utils;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2023-02-21 10:59
 */
@Data
public class NodeList {
    private List<Node> ancestorList;
    private List<Node> temporaryList;
    private int nodeNum;
    private Set<String> abstractSet;
}
