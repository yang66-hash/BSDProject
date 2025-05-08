package com.yang.apm.springplugin.base.utils;

import com.yang.apm.springplugin.base.Enum.NodeType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: multiple tree
 * @author: xyc
 * @date: 2023-02-20 10:24
 */
@Data
public class Node {
    private int id;
    private String name;
    private NodeType nodeType;
    private String serviceName;
    private List<Node> childList;

    public Node(int id, String name, NodeType nodeType, String serviceName) {
        this.id = id;
        this.name = name;
        this.nodeType = nodeType;
        this.serviceName = serviceName;
        this.childList = new ArrayList<>();
    }

    public Node(String name, String serviceName) {

        this.name = name;
        this.serviceName = serviceName;

    }
    public Node(int id, String name, NodeType nodeType) {
        this.id = id;
        this.name = name;
        this.nodeType = nodeType;
        this.childList = new ArrayList<>();
    }
    public Node(String name, NodeType nodeType, String serviceName) {
        this.name = name;
        this.nodeType = nodeType;
        this.serviceName = serviceName;
        this.childList = new ArrayList<>();
    }

    public static void main(String[] args) {
        List<String> test =new ArrayList<>();
        test.add("a");
        test.add("b");
        test.add("c");
        test.remove("a");
        for(String str: test){
            System.out.println("---"+str);
            test.remove(str);
        }
        System.out.println(test.size());

    }
}
