package com.yang.apm.springplugin.base.utils;


import com.yang.apm.springplugin.base.Enum.NodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: used for find ancestors
 * @author: xyc
 * @date: 2023-02-20 10:19
 */
@Component
public class NodeUtils {

    public Map<String,List<Integer>> nodeMap;
    public int num;
    public Node firstIndex;
    public boolean isExist;
    public List<List<String>> routeResult;

    public NodeUtils(){
        nodeMap = new HashMap<>();
        routeResult = new ArrayList<>();
    }
    /**
    * @Description: insert new Node
    * @Param: [fatherName, potentialFather, newNode]
    * @return: boolean
    */
    public boolean insert(String fatherName, Node potentialFather, Node newNode){
        if(potentialFather.getName().equals(fatherName)){
            potentialFather.getChildList().add(newNode);
            return true;
        }
        return false;
    }

    public void findSameNode(Node node){
        String name = node.getName();
        if(nodeMap.containsKey(name)){
            nodeMap.get(name).add(node.getId());
        }
        else {
            nodeMap.put(name,new ArrayList<>());
        }
        if(node.getChildList().isEmpty())
            return;
        List<Node> childList = node.getChildList();
        for(int i = 0; i < childList.size(); i++){
            findSameNode(childList.get(i));
        }
    }

    public void getTreeSize(Node node){
        if(node!=null) this.num++;
        List<Node> childList = node.getChildList();
        if(childList.isEmpty())
            return;
        for(int i = 0; i < childList.size(); i++){
            getTreeSize(childList.get(i));
        }
        return;

    }

    public boolean queryNodeByName(Node node, String targetName, NodeList nodeList, Node childNode, Boolean isEXist){
        String name = node.getName();
        if(node.getName().contains(targetName)){
            isEXist = true;
            node.getChildList().add(new Node(nodeList.getNodeNum(), childNode.getName(), childNode.getNodeType()));
            nodeList.setNodeNum(nodeList.getNodeNum()+1);
            return isEXist;
        }
        if(node.getChildList().isEmpty())
            return isEXist;
        List<Node> childList = node.getChildList();
        for(int i = 0; i < childList.size(); i++){
            queryNodeByName(childList.get(i), targetName, nodeList, childNode, isEXist);
        }
        return isEXist;
    }

    public boolean connectNode(Node node, String targetName, NodeList nodeList, Node childNode){
        String name = node.getName();
        if(node.getName().equals(targetName)){
            this.isExist = true;
            this.clone(childNode, null);
            node.getChildList().add(firstIndex);
            nodeList.setNodeNum(nodeList.getNodeNum()+this.num);
            return this.isExist;
        }
        if(node.getChildList().isEmpty())
            return this.isExist;
        List<Node> childList = node.getChildList();
        for(int i = 0; i < childList.size(); i++){
            connectNode(childList.get(i), targetName, nodeList, childNode);
        }
        return this.isExist;
    }

    public void clone(Node node, Node cloneNode){
        if(cloneNode == null)
            this.firstIndex = new Node(node.getId(), node.getName(), node.getNodeType(), node.getServiceName());
            cloneNode = this.firstIndex;
        if(!node.getChildList().isEmpty()){
            for(Node child: node.getChildList()){
                Node node1 = new Node(child.getId(), child.getName(), child.getNodeType(), child.getServiceName());
                cloneNode.getChildList().add(node1);
                clone(child, node1);
            }
        }
        return;
    }

    public static void main(String[] args) {
        Node node = new Node(1,"a", NodeType.Interface);
        Node node2 = new Node(2,"b",NodeType.Class);
        node2.getChildList().add(new Node(3, "c", NodeType.Class));
        node.getChildList().add(node2);
        NodeUtils nodeUtils = new NodeUtils();
        nodeUtils.clone(node,null);
        nodeUtils.getTreeSize(nodeUtils.firstIndex);
        for(Node child: nodeUtils.firstIndex.getChildList()) {
            for (Node grandson: child.getChildList())
                System.out.println(grandson.getName());
        }
        System.out.println(nodeUtils.num);


    }

    public void bianLi(Node node){
        for(Node node1: node.getChildList()){
            bianLi(node1);
        }
    }
    public  void queryRouteByName(Node node, String name, List<String> routeWay){
            List<Node> childList = node.getChildList();
            routeWay.add(node.getName());
            if(name.equals(node.getName())){
                routeResult.add(new ArrayList<>(routeWay));
                routeWay.remove(routeWay.size()-1);
                return;
            }
            else{
                for(Node node1 : node.getChildList()){
                    queryRouteByName(node1, name, routeWay);
                }
                routeWay.remove(routeWay.remove(routeWay.size()-1));
            }

    }

    public void queryDifferentNode(Node node, Map<Node, Integer> nodeMap){
        boolean isExist = false;
        for(Node node1: nodeMap.keySet()){

            if(node1.getName().equals(node.getName()) && node1.getServiceName().equals(node.getServiceName())){
                isExist = true;
                nodeMap.put(node1, nodeMap.get(node1)+1);
                break;
            }
        }
        if(!isExist)
            nodeMap.put(new Node(node.getName(), node.getServiceName()), 1);
        for(Node node1: node.getChildList()){
            queryDifferentNode(node1, nodeMap);
        }

    }
    public void queryRouteByName(String name){

    }


}
