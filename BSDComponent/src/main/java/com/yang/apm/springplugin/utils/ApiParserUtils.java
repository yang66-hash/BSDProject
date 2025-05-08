package com.yang.apm.springplugin.base.utils;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.yang.apm.springplugin.base.Enum.NodeType;
import com.yang.apm.springplugin.base.context.staticres.ApiVersionContext;
import com.yang.apm.springplugin.base.item.DependCount;
import com.yang.apm.springplugin.base.item.UnusedType;
import com.yang.apm.springplugin.base.item.UrlItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @description: used for parse .java file
 * @author: xyc
 * @date: 2022-12-22 14:09
 */
@Component
public class ApiParserUtils {

    public Set<String> implementInterfaces;

    @Autowired
    public NodeUtils nodeUtils;

    @Autowired
    public NodeService nodeService;

    public static int interfaceCount;
    public static int abstractCount;
    public  ApiParserUtils(){
        this.implementInterfaces = new HashSet<>();
    }

    public  void inspectJavaFile(File pFile, ApiVersionContext apiVersionContext, String serviceName)
            throws FileNotFoundException, ParseException, IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(pFile);
        try {
            cu = new JavaParser().parse(in);
        } finally {
            in.close();
        }
        UrlItem urlItem = new UrlItem();
        new ClassVisitor().visit(cu.getResult().get(), urlItem);
        new MethodVisitor().visit(cu.getResult().get(), urlItem);
        String preUrl = "";
        if(urlItem.getUrl1() != null) {
            preUrl = urlItem.getUrl1().substring(1, urlItem.getUrl1().length() - 1);
            System.out.println("urlItem"+urlItem.toString());
            System.out.println("urlItem.getUrl2().keySet()"+urlItem.getUrl2().keySet());
        }
        for(String methodName: urlItem.getUrl2().keySet()){
            String afterUrl= urlItem.getUrl2().get(methodName);
            System.out.println("methodName---"+methodName);
            System.out.println("afterUrl--"+afterUrl);
            if(afterUrl == null){
                apiVersionContext.getMissingUrlMap().get(serviceName).put(methodName,preUrl);
                continue;
            }
            if(afterUrl.equals("")){
                apiVersionContext.getMissingUrlMap().get(serviceName).put(methodName,preUrl);
                if(!this.apiPattern(preUrl)) {
                    apiVersionContext.getUnversionedMap().get(serviceName).put(methodName, preUrl);
                }
                continue;
            }
            afterUrl = afterUrl.substring(1,afterUrl.length()-1);
            String fullUrl = preUrl + afterUrl;
            if(!this.apiPattern(fullUrl)) {
                apiVersionContext.getUnversionedMap().get(serviceName).put(methodName, fullUrl);
            }
        }
    }

    public int isEntityClass(File pFile, Set<String> count, String serviceName) throws IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(pFile);
        try {
            cu = new JavaParser().parse(in);
        } finally {
            in.close();
        }
        new EntityClassVisitor().visit(cu.getResult().get(), count);
        return count.size();
    }

    public void getImports(File pFile, Map<String, DependCount> imOutMap) throws IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(pFile);
        try {
            cu = new JavaParser().parse(in);
            CompilationUnit compilationUnit = cu.getResult().get();
            int size = compilationUnit.getImports().size();
            if(size ==0)
                return;
            String qualifiedName = compilationUnit.getTypes().get(0).getFullyQualifiedName().get();
            for(int i=0; i<size; i++){
                String importName = compilationUnit.getImports().get(i).getName().asString();
                if(imOutMap.containsKey(importName)){
                    imOutMap.get(importName).setOutputCount(imOutMap.get(importName).getOutputCount() + 1);
                    imOutMap.get(qualifiedName).setImportCount(imOutMap.get(qualifiedName).getImportCount()+1);
                }
            }

        } finally {
            in.close();
        }
    }

    public void getAllQualifiedName(File pFile, Map<String, DependCount> ImOutMap, String belongsService) throws IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(pFile);
        try {
            cu = new JavaParser().parse(in);
            CompilationUnit compilationUnit = cu.getResult().get();
            com.github.javaparser.ast.NodeList<TypeDeclaration<?>>   classes = compilationUnit.getTypes();
            int classCount = compilationUnit.getTypes().size();
            for(int i=0; i<classCount; i++){
                ImOutMap.put(classes.get(i).getFullyQualifiedName().get(), new DependCount(belongsService));
            }

        } finally {
            in.close();
        }
    }

    public void getInterfaceAndAbstractClass(File pFile, Map<UnusedType,Map<String,Boolean>> unusedMap, String serviceName) throws IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(pFile);
        try {
            cu = new JavaParser().parse(in);

        } finally {
            in.close();
        }
        String name = pFile.getName().substring(0,pFile.getName().length()-5);
        new InterfaceAndAbstractVisitor().visit(cu.getResult().get(), unusedMap);

        for(UnusedType unusedType: unusedMap.keySet()){
            if(unusedType.getName().equals(name) && unusedType.isInterface()){
                new InterfaceMethodVisitor().visit(cu.getResult().get(), unusedMap.get(unusedType));
                break;
            }
            if(unusedType.getName().equals(name) && unusedType.isAbstractClass()){
                new AbstractMethodVisitor().visit(cu.getResult().get(), unusedMap.get(unusedType));
                break;
            }
        }
    }
    public void getInterface(File file,List<Node> ancestorInterfaceList, Set<String> abstractSet, String serviceName) throws IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(file);
        try {
            cu = new JavaParser().parse(in);
        } finally {
            in.close();
        }
        int beforeSize = ancestorInterfaceList.size();
        new InterfaceVisitor().visit(cu.getResult().get(), ancestorInterfaceList);
        int afterSize = ancestorInterfaceList.size();
        if(afterSize == beforeSize +1)
            ancestorInterfaceList.get(afterSize-1).setServiceName(serviceName);
        new AbstractVisitor().visit(cu.getResult().get(), abstractSet);
    }

    public void buildTree(File file, NodeList nodeList, String serviceName) throws IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(file);
        try {
            cu = new JavaParser().parse(in);
        } finally {
            in.close();
        }
        this.nodeService.setNodeList(nodeList);
        this.nodeService.setServiceName(serviceName);
        new RelationshipVisitor().visit(cu.getResult().get(), nodeService);

    }

    public void findMethodCall(File pFile, Map<String, Map<UnusedType, Map<String, Boolean>>> unusedMap) throws IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(pFile);
        try {
            cu = new JavaParser().parse(in);
        } finally {
            in.close();
        }
        new ImplementVisitor().visit(cu.getResult().get(), this.implementInterfaces);
        new MethodCallVisitor().visit(cu.getResult().get(), unusedMap);
    }

    public boolean apiPattern(String apiPath){
        String pattern = "^(?!.*v\\.\\d+).*\\/v([0-9]*[a-z]*\\.*)+([0-9]|[a-z])+\\/.*$";  //   .*/v([0-9]+\.)+[0-9]+/.*   .*/v([0-9]*[a-z]*\.)+([0-9]|[a-z])+/.*
        Pattern p= Pattern.compile(pattern);
        if(p.matcher(apiPath).matches()){
            return true;
        }
        else{
            return false;
        }

    }

    private static  class ImplementVisitor extends  VoidVisitorAdapter{
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            Set<String> implementInterfaces = (Set<String>) arg;
            for(ClassOrInterfaceType type: n.getImplementedTypes()){
                implementInterfaces.add(type.toString());
            }
        }
    }

    private static class ClassVisitor extends VoidVisitorAdapter{

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            if(n.getAnnotations() != null){
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    if(annotation.getClass().equals(SingleMemberAnnotationExpr.class)){
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")
                        ){
                            UrlItem urlItem = (UrlItem) arg;
                            urlItem.setUrl1(((SingleMemberAnnotationExpr) annotation).getMemberValue().toString());
                            return;
                        }
                    }
                    else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")) {
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    UrlItem urlItem = (UrlItem) arg;
                                    urlItem.setUrl1(pair.getValue().toString());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static class ImportVisitor extends VoidVisitorAdapter{

    }
    private static class EntityClassVisitor extends VoidVisitorAdapter{
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    if (annotation.getNameAsString().equals("Entity") || annotation.getNameAsString().equals("Document")) {
                        Set<String> countSet = (Set) arg;
                        countSet.add(annotation.getNameAsString());
                    }
                }
            }
        }

    }

    private static class InterfaceVisitor extends VoidVisitorAdapter{
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {

            if(n.isInterface() && n.getImplementedTypes().isEmpty() && n.getExtendedTypes().isEmpty()) {
                List<Node> ancestorInterfaceList = (List<Node>) arg;
                int id = ancestorInterfaceList.size()+1;
                ancestorInterfaceList.add(new Node(id, n.getNameAsString(), NodeType.Interface));
            }
        }
    }

    private static class AbstractVisitor extends VoidVisitorAdapter{
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {

            if(n.isAbstract()) {
                Set<String> abstractSet = (Set<String>) arg;
                abstractSet.add(n.getNameAsString());
            }
        }
    }

    private  class RelationshipVisitor extends VoidVisitorAdapter{
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            NodeService nodeService = (NodeService) arg;
            NodeList nodeList = nodeService.getNodeList();
            String serviceName = nodeService.getServiceName();
            NodeType nodeType = null;
            if(n.isInterface())
                nodeType = NodeType.Interface;
            else if (n.isAbstract()) {
                nodeType = NodeType.Abstract;
            }
            else nodeType = NodeType.Class;

            Node temporaayNode = new Node(nodeList.getNodeNum(), n.getNameAsString(), nodeType, serviceName);

            for(ClassOrInterfaceType type: n.getImplementedTypes()){
                boolean isChildExisted = false;
                boolean isFatherExisted = false;

                for(Node tempNode: nodeList.getTemporaryList()){
                    if(tempNode.getName().equals(n.getNameAsString())){
                        isChildExisted = true;
                        nodeUtils.num = 0;
                        nodeUtils.getTreeSize(tempNode);
                        nodeUtils.isExist = false;
                        for(Node node: nodeList.getAncestorList()) {
                            nodeUtils.connectNode(node, type.toString(), nodeList, tempNode);
                            }
                        if(nodeUtils.isExist) {
                            isFatherExisted = true;
                        }
                        if(!nodeUtils.isExist){
                            Node node1 = new Node(nodeList.getNodeNum(), type.toString(), NodeType.Interface, serviceName);
                            nodeList.getTemporaryList().remove(tempNode);
                            node1.getChildList().add(tempNode);
                            nodeList.getTemporaryList().add(node1);
                        }
                    }

                    // Implementation in ancestorList tree, class not in temporaryList
//                    if(nodeUtils.queryNodeByName(node, type.toString(), nodeList, temporaayNode, false))
//                        isExisted = true;
                }
                // child not in,father in
//                if(!isChildExisted){
//                    for(Node node: nodeList.getAncestorList()) {
//                        Node node1 = new Node(nodeList.getNodeNum(), n.getNameAsString(),nodeType, serviceName);
//                        nodeUtils.connectNode(node, type.toString(), nodeList, node1);
//                    }
//                }
                // child not find in temporaryList, but father is found in ancestor Tree || temporaryList is empty || child not find in temporaryList
                if((!isChildExisted && isFatherExisted) || nodeList.getTemporaryList().isEmpty() || !isChildExisted){
                    for(Node node: nodeList.getAncestorList()) {
                        Node node1 = new Node(nodeList.getNodeNum(), n.getNameAsString(),nodeType, serviceName);
                        nodeUtils.connectNode(node, type.toString(), nodeList, node1);
                    }
                }
                //not find in  all ancestor tree
                if(!isFatherExisted && !isChildExisted){
                    int num = nodeList.getNodeNum();
                    Node parentNode = new Node(num, type.toString(),NodeType.Interface, serviceName);
                    parentNode.getChildList().add(new Node(num+1, temporaayNode.getName(), temporaayNode.getNodeType(), serviceName));
                    nodeList.setNodeNum(num+2);
                    nodeList.getTemporaryList().add(parentNode);
                }
            }
            for(ClassOrInterfaceType type: n.getExtendedTypes()){
                boolean isChildExisted = false;
                boolean isFatherExisted = false;
                // judge temporaryList is exist this class
                for(Node tempNode: nodeList.getTemporaryList()){
                    if(tempNode.getName().equals(n.getNameAsString())){
                        isChildExisted = true;
                        nodeUtils.isExist = false;
                        nodeUtils.num = 0;
                        nodeUtils.getTreeSize(tempNode);
                        for(Node node: nodeList.getAncestorList()) {
                            nodeUtils.connectNode(node, type.toString(), nodeList, tempNode);
                        }
                        if(nodeUtils.isExist) {
                            isFatherExisted = true;
                        }
                        if(!nodeUtils.isExist){
                            Node node1 = new Node(nodeList.getNodeNum(), type.toString(), NodeType.Interface, serviceName);
                            nodeList.getTemporaryList().remove(tempNode);
                            node1.getChildList().add(tempNode);
                            nodeList.getTemporaryList().add(node1);
                        }
                    }

                    // Implementation in ancestorList tree, class not in temporaryList
//                    if(nodeUtils.queryNodeByName(node, type.toString(), nodeList, temporaayNode, false))
//                        isExisted = true;
                }
                // child not find in temporaryList, but father is found in ancestor Tree || temporaryList is empty || child not find in temporaryList
                if((!isChildExisted && isFatherExisted) || nodeList.getTemporaryList().isEmpty() || !isChildExisted){
                    for(Node node: nodeList.getAncestorList()) {
                        Node node1 = new Node(nodeList.getNodeNum(), n.getNameAsString(),nodeType, serviceName);
                        nodeUtils.connectNode(node, type.toString(), nodeList, node1);
                    }
                }
                //not find in  all ancestor tree
                if(!isFatherExisted && !isChildExisted){
                    int num = nodeList.getNodeNum();
                    Node parentNode = new Node(num, type.toString(),NodeType.Interface, serviceName);
                    parentNode.getChildList().add(new Node(num+1, temporaayNode.getName(), temporaayNode.getNodeType(), serviceName));
                    nodeList.setNodeNum(num+2);
                    nodeList.getTemporaryList().add(parentNode);
                }
            }

        }
    }
    private static class InterfaceAndAbstractVisitor extends VoidVisitorAdapter{
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            Map<UnusedType,Map<String,Boolean>> unusedMap = (Map) arg;
            if(n.isInterface()){
                interfaceCount++;
                UnusedType unusedType =new UnusedType();
                unusedType.setInterface(true);
                unusedType.setName(n.getNameAsString());
                unusedMap.put(unusedType,new HashMap<>());
                return;
            }
            if(n.isAbstract()){
                abstractCount++;
                UnusedType unusedType =new UnusedType();
                unusedType.setAbstractClass(true);
                unusedType.setName(n.getNameAsString());
                unusedMap.put(unusedType,new HashMap<>());
            }

        }
    }

    private  static  class InterfaceMethodVisitor extends VoidVisitorAdapter{
        @Override
        public void visit(MethodDeclaration n, Object arg) {
            Map<String, Boolean> unusedMap = (Map) arg;
            unusedMap.put(n.getNameAsString(),false);
            }

        }

    private static class AbstractMethodVisitor extends  VoidVisitorAdapter {
        @Override
        public void visit(MethodDeclaration n, Object arg) {

            Map<String, Boolean> unusedMap = (Map) arg;
            if (n.isAbstract()) {
                unusedMap.put(n.getNameAsString(),false);
            }
        }
    }

    private  class MethodCallVisitor extends VoidVisitorAdapter{
        @Override
        public void visit(MethodCallExpr n, Object arg) {
            super.visit(n, arg);

            List<Expression> arguments = n.getArguments();
            String methodName = "";
            for (Expression argument : arguments) {
                if(argument instanceof MethodCallExpr){
                    MethodCallExpr methodCallExpr = (MethodCallExpr) argument;
                    methodName = methodCallExpr.getNameAsString();
                }
            }
            Map<String,Map<UnusedType,Map<String,Boolean>>> unusedMap= (Map<String,Map<UnusedType,Map<String,Boolean>>>) arg;
            int count = 0;
            Map<String, UnusedType> countMap = new HashMap<>();
            for(String svc: unusedMap.keySet()){
                for(UnusedType unusedType: unusedMap.get(svc).keySet()){
                    if(unusedMap.get(svc).get(unusedType).containsKey(n.getNameAsString())){
                            unusedMap.get(svc).get(unusedType).put(n.getNameAsString(), true);
//                        count++;
//                        countMap.put(svc,unusedType);
//                        if(count == 2){
//                            for(String impl: implementInterfaces){
//                                for(String svc1: unusedMap.keySet()){
//                                    if(unusedMap.get(svc1).containsKey(impl) && unusedMap.get(svc1).get(impl).containsKey(n.getNameAsString())){
//                                        for(String svc2: countMap.keySet()){
//                                            if(!implementInterfaces.contains(countMap.get(svc2))){
//                                                unusedMap.get(svc2).get(countMap.get(svc2)).put(n.getNameAsString(),true);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }

                    }
                    if(unusedMap.get(svc).get(unusedType).containsKey(methodName)) {
                        unusedMap.get(svc).get(unusedType).put(methodName, true);
                    }
                }
            }


        }

    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    System.out.println("annotation.getClass()"+annotation.getClass());
                    System.out.println("annotation.getName().asString()"+annotation.getName().asString());
                    System.out.println("n.getName().asString()"+n.getName().asString());
                    if(annotation.getClass().equals(SingleMemberAnnotationExpr.class)){
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping")||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")){
                            UrlItem urlItem = (UrlItem) arg;
                            String url2 = ((SingleMemberAnnotationExpr) annotation).getMemberValue().toString();
                            System.out.println("n.getName().asString()"+n.getName().asString());
                            System.out.println("url2"+url2);
                            urlItem.getUrl2().put(n.getName().asString(), url2);
                        }
                    }
                    else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")) {
                            if(((NormalAnnotationExpr) annotation).getPairs().size() == 0){
                                UrlItem urlItem = (UrlItem) arg;
                                urlItem.getUrl2().put(n.getName().asString(),"");
                                return;
                            }
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    UrlItem urlItem = (UrlItem) arg;
                                    urlItem.getUrl2().put(n.getName().asString(), pair.getValue().toString());
                                    return;
                                }
                            }
                        }
                    } else if (annotation.getClass().equals(MarkerAnnotationExpr.class)) {
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping")||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")){
                            UrlItem urlItem = (UrlItem) arg;
                            String url2 = "";
                            System.out.println("n.getName().asString()"+n.getName().asString());
                            System.out.println("url2"+url2);
                            urlItem.getUrl2().put(n.getName().asString(), url2);
                            return;
                        }
                        
                    }
                }
            }
        }
    }

}
