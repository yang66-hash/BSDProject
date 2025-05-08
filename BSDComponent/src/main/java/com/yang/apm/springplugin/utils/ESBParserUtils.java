package com.yang.apm.springplugin.utils;


import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.yang.apm.springplugin.base.item.ParseStorage;
import com.yang.apm.springplugin.base.item.SvcCallDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 实现将传进来的某个具体微服务进行分析，判断其中的restTemplate的调用情况
 */
@Component
@Slf4j
public class ESBParserUtils {

    private static String regex = "(service|Service|-)";

    private static String regex2 = "\\S*(service|Service|SERVICE)";

    private static Double percent = 0.95;


    public  HashMap<String, SvcCallDetail> ESBUsageAnalysis(String path){

        HashMap<String,String> hashMap = getServicesList(path);
        if (hashMap.size()==0){
            System.out.println("test--------");
            return null;
        }
        HashMap<String,HashMap<String,Integer>> mappings = new HashMap<>();
        for (Map.Entry<String,String> entry:
                hashMap.entrySet()) {
            HashMap<String,Integer> result = parseWebService(hashMap, entry.getValue());
            mappings.put(entry.getKey(),result);
        }
        HashMap<String, SvcCallDetail> result = isESBUsageExist(mappings);
        return result;
    }

    //used to get call services num and called services num of per service
    public  Map<String,HashMap<String,Integer>> ScatteredAnalysis(String path){
        HashMap<String,String> hashMap = getServicesList(path);
        if (hashMap.size()==0){
            return new HashMap<>();
        }
        HashMap<String,HashMap<String,Integer>> mappings = new HashMap<>();
        for (Map.Entry<String,String> entry:
                hashMap.entrySet()) {
            HashMap<String,Integer> result = parseWebService(hashMap, entry.getValue());
            System.out.println("result"+result.toString());
            mappings.put(entry.getKey(),result);
        }
        checkCallResult(mappings);
        return mappings;
    }

    private static HashMap<String, SvcCallDetail> isESBUsageExist(HashMap<String, HashMap<String, Integer>> mappings) {
        Set<String> serviceNameSet = mappings.keySet();
        checkCallResult(mappings);
        HashMap<String,SvcCallDetail> svcCallDetailHashMap = new HashMap<>();
        //build a map of service call and service called
        for (Map.Entry<String, HashMap<String, Integer>> entry : mappings.entrySet()) {
            HashMap<String,Integer> call = entry.getValue();
            if (!svcCallDetailHashMap.containsKey(entry.getKey()))
                svcCallDetailHashMap.put(entry.getKey(),new SvcCallDetail());

            Iterator<Map.Entry<String,Integer>> iterator = call.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, Integer> map = iterator.next();
                svcCallDetailHashMap.get(entry.getKey()).getCalledService().add(map.getKey());
                if (!svcCallDetailHashMap.containsKey(map.getKey())){
                    svcCallDetailHashMap.put(map.getKey(),new SvcCallDetail());
                }
                svcCallDetailHashMap.get(map.getKey()).getCallService().add(entry.getKey());

            }
        }

        for (Map.Entry<String,SvcCallDetail> service:
             svcCallDetailHashMap.entrySet()) {
            SvcCallDetail svcCallDetail = service.getValue();
            Integer callNum = svcCallDetail.getCallService().size();
            Integer calledNum = svcCallDetail.getCalledService().size();
            Integer servicesNum = svcCallDetailHashMap.size();

            if (calledNum==callNum && callNum>=percent*servicesNum){
                service.getValue().setIsESBUsage(true);
            }
            else service.getValue().setIsESBUsage(false);
        }

        return svcCallDetailHashMap;
    }

    /**
     * @param mappings 将可能错误统计服务名称筛选出来
     */
    private static void checkCallResult(HashMap<String, HashMap<String, Integer>> mappings) {
        Set<String> serviceNameSet = mappings.keySet();
        for (Map.Entry<String, HashMap<String, Integer>> entry : mappings.entrySet()) {
            HashMap<String,Integer> call = entry.getValue();
            Iterator<Map.Entry<String,Integer>> iterator = call.entrySet().iterator();
            while (iterator.hasNext()){
                if (!serviceNameSet.contains(iterator.next().getKey())){
                    iterator.remove();
                }
            }
        }
        log.info(mappings.toString());
    }

    /**
     * @param path 微服务系统的路径，本地
     *
     * @return 返回对应具体微服务以及其本地路径的键值对
     */
    public static HashMap<String,String> getServicesList(String path){
        HashMap<String,String> serviceWithPath = new HashMap<>();
        File file = new File(path);
        File[] files = file.listFiles();
        if (files!=null){
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()){
                    String filePath = files[i].toString();
                    String serviceName = filePath.substring(filePath.lastIndexOf(File.separator)+1);
                    System.out.println("serviceName"+serviceName);
                    Matcher matcher = Pattern.compile(regex).matcher(serviceName);
                    if (matcher.find()){
                        serviceWithPath.put(serviceName,filePath);
                    }
                }
            }
        }
        return serviceWithPath;
    }

    /**
     * @param path 具体微服务的目录，子模块路径
     */
    private static HashMap<String,Integer> parseWebService(HashMap<String,String> hashMap, String path){
        HashMap<String,Integer> resultOfService = new HashMap<>();
        Path serviceRoot = Paths.get(path);
        ProjectRoot projectRoot = new ParserCollectionStrategy().collect(serviceRoot);

        projectRoot.getSourceRoots().forEach(sourceRoot -> {
            try {
                // parse source root
                sourceRoot.tryToParse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 获取解析后的编译单元列表
            List<CompilationUnit> cuList = sourceRoot.getCompilationUnits();
            for (int i = 0; i < cuList.size(); i++) {
                HashMap<String,Integer> resultOfFile = parseFile(hashMap, cuList.get(i));
                resultOfService.putAll(resultOfFile);
            }
        });
        return resultOfService;
    }


    /**
     * 解析单个Java文件
     * @param cu 编译单元
     */
    public static HashMap<String,Integer> parseFile(HashMap<String,String> hashMap,CompilationUnit cu) {
        HashMap<String,Integer> result = new HashMap<>();
        //types将文件中的每个class作为单个元素，保存在List中
        NodeList<TypeDeclaration<?>> types = cu.getTypes();

        for (TypeDeclaration<?> type : types) {

            // member指的是其中的类中的所有信息,属性、方法和内部类都可以成为list列表成员
            NodeList<BodyDeclaration<?>> members = type.getMembers();
            //针对每个类进行分析
            ParseStorage parseStorage = new ParseStorage();
            for (int i = 0; i < members.size(); i++) {

                if("FieldDeclaration".equals(judgeNode(members.get(i)))){
                    parseStorage.getFieldDeclarations().add((FieldDeclaration)members.get(i));
                }else if (("MethodDeclaration").equals(judgeNode(members.get(i)))){
                    parseStorage.getMethodDeclarations().add((MethodDeclaration) members.get(i));
                }
            }

            HashMap<String,Integer> callNum =  statisticalUsage(hashMap,parseStorage);

            result.putAll(callNum);

        }

        return result;
    }

    /**
     * @param node 类内成员，通过TypeDeclaration<?> .getMembers()获取
     *             只判断成员变量和方法
     * @return
     */
    private static String judgeNode(BodyDeclaration<?> node){
        if (node instanceof FieldDeclaration) {
            //类内声明的属性变量，字段
            return "FieldDeclaration";
        }else if (node instanceof MethodDeclaration) {
            return "MethodDeclaration";
        }
        return null;
    }


    private static HashMap<String, Integer> statisticalUsage(HashMap<String,String> hashMap, ParseStorage parseStorage) {

        String restTemplateName = "" ;
        HashMap<String, Integer> result = new HashMap<>();
        for (int i = 0; i < parseStorage.getFieldDeclarations().size(); i++) {
            String tmpType = parseStorage.getFieldDeclarations().get(i).getElementType().toString();
            if (tmpType.equals("RestTemplate")){
                restTemplateName =  parseStorage.getFieldDeclarations().get(i).getVariable(0).toString();
                System.out.println("restTemplateName"+restTemplateName);
            }
        }
        //有定义RestTemplate
        if (!"".equals(restTemplateName)){
            result = processMethods(hashMap, parseStorage,restTemplateName);
            System.out.println("tttttttttttttt");
        }
        return result;
    }

    /**
     * @param parseStorage
     * 从方法中提取restTemplate的方法调用
     * @return
     */
    private static HashMap<String, Integer> processMethods(HashMap<String,String> hashMap, ParseStorage parseStorage,String restTemplateName) {

        HashMap<String, Integer> result = new HashMap<>();
        //处理函数体
        //每一个方法单独做处理
        for (int i = 0; i < parseStorage.getMethodDeclarations().size(); i++) {
            Set<Expression> callPathSetInit = new HashSet<>();

            //获取初始url参数
            argProcessMethod(parseStorage.getMethodDeclarations().get(i),callPathSetInit,restTemplateName);

            //解析，将其解析成形如"http:// + ... + ..." 的形式
//            parseRightExprs(callPathSetInit,parseStorage,i);
            for (Expression set:
                    callPathSetInit) {
                try {
                    parseRightExpr(set,parseStorage,i);
                }catch (Exception e){
                    //logger.info(e.getClass().getName()+"解析错误！！！");
                    continue;
                }

            }
            Set<Expression> callPathSetFinal = new HashSet<>();

            //针对字符串进行分割获取服务名
            argProcessMethod(parseStorage.getMethodDeclarations().get(i),callPathSetFinal, restTemplateName);

            for (Expression set:
                    callPathSetFinal) {
                //如果是字符串
                String serviceName;
                if (set.isStringLiteralExpr()){
                    System.out.println("set"+set.toString());
                    serviceName =  getNameFromURL(hashMap,set);
                    System.out.println("serviceName"+serviceName);
                    if (serviceName!=null){
                        System.out.println("okkkkkkkkkkk");
                        result.put(serviceName,result.getOrDefault(serviceName,0)+1);
                    }
                }
                else {
                    //形如"http:// + ... + ..." 的形式
                    serviceName = getSvcNameFromStandaloneExpr(set);
                    result.put(serviceName,result.getOrDefault(serviceName,0)+1);
                }
            }

        }

        return result;
    }



    /**
     * @param set  形如"http："+"..."+"..."
     * @return 返回Service名称
     */
    private static String getSvcNameFromStandaloneExpr(Expression set) {

        String[] strings = set.toString().split("\\+|/|\"");
        for (int i = 0; i < strings.length; i++) {
            Matcher matcher = Pattern.compile(regex2).matcher(strings[i]);
            if (matcher.find()){
                return matcher.group();
            }
        }
        return null;

    }

    /**
     * @param node
     * @param callPathArgSet 存储http调用微服务的url参数集合
     * @param restTemplateName
     *
     * 解析出restTemplate调用微服务的第一个参数，可能是URL、String、URI等，也可能是是其他类型参数，但若是请求路径参数，只能是在第一个参数位置
     */
    private static void argProcessMethod(Node node, Set<Expression> callPathArgSet, String restTemplateName) {
        if (node instanceof MethodCallExpr){
            //获取方法调用所在实例对象名称

            if (((MethodCallExpr)node).getScope().isPresent()){
                String callScope = ((MethodCallExpr)node).getScope().get().toString();
                if(restTemplateName.equals(callScope)){
                    System.out.println("(MethodCallExpr)node).getArguments().get(0)"+((MethodCallExpr)node).getArguments().get(0));
                    callPathArgSet.add(((MethodCallExpr)node).getArguments().get(0));
                }
            }
            //找到restTemplate所调用的函数的第一个参数（如果找URL，只能是在第一个参数位置）

            return;
        }
        for (Node child:node.getChildNodes()){
            argProcessMethod(child,callPathArgSet,restTemplateName);
        }
    }


    private static String getNameFromURL(HashMap<String,String> hashMap,Expression set) {
        String[] strings = set.toString().split("/");
        for (int j = 0; j < strings.length; j++) {
            System.out.println("string j"+strings[j]);
            System.out.println("length"+strings[j].length());
            System.out.println("hashMap"+hashMap);
            if((strings[j].startsWith("\"")) && strings[j].endsWith("\""))
                strings[j] = strings[j].substring(1, strings[j].length() - 1);
            Matcher matcher = Pattern.compile(regex2).matcher(strings[j]);
            if (matcher.find() || hashMap.containsKey(strings[j])) {
                System.out.println("zzzzzzzzzzzzzzz");
                return strings[j];
            }
        }
        return null;
    }

    /**
     * @param parseStorage 当前分析类
     * @param methodCurParse 当前分析方法在parseStorage的中list的位置
     * @param set 要转换的表达式 形如"http://"+serviceName+ "/.../..." or 变量 or 方法
     * @return
     * 将获取的类中的信息（ParseStorage）中的restTemplate调用第一个参数进行替换
     */
    private static void parseRightExpr(Node set,ParseStorage parseStorage,Integer methodCurParse) {
        //针对变量、方法、多加号连接字符串，形如"http://"+serviceName+ "/.../..."的表达式进行解析
        if (set instanceof NameExpr){
            Expression rightExpr = getRightExprFromMethod(parseStorage.getMethodDeclarations().get(methodCurParse),(Expression) set);
            if (rightExpr==null){
                rightExpr = getRightExprFromField(parseStorage.getFieldDeclarations(),(Expression) set);

            }
            if (rightExpr!=null){
                //将其替代，在方法中
                set.replace(rightExpr);
                //将当前的set也赋值为右部
                set = rightExpr;

                if (rightExpr instanceof MethodCallExpr){
                    parseRightExpr(set, parseStorage,methodCurParse);
                }
            }
        }
        else if (set instanceof MethodCallExpr){
            //只能解析带serviceName参数的函数 如
            // String getService(serviceName)
            // {
            // return "..."+serviceName+"...";
            // }
            if (((MethodCallExpr) set).getArguments().size()!=0){
                String args ="";
                for (int i = 0; i < ((MethodCallExpr) set).getArguments().size(); i++) {
                    args += ((MethodCallExpr) set).getArguments().get(i).toString();
                }
//                //logger.info(args+"==========+++++++++++============");
                StringLiteralExpr node = new StringLiteralExpr(args);
                set.replace(node);
                set = node;
            }
            //不带参数默认是本类中get方法，进行处理
            else {
                Expression expression = getReturnStat(set,parseStorage.getMethodDeclarations());
                Expression rightExpr =  getRightExprFromField(parseStorage.getFieldDeclarations(),expression);
                set.replace(rightExpr);
                set = rightExpr;
                log.info(rightExpr.toString());
            }
        }

        //logger.info(set.getChildNodes().size()+"   "+set.getChildNodes().toString()+"==========+++++++=======");

        for (Node child:set.getChildNodes()){
            parseRightExpr(child,parseStorage,methodCurParse);
        }
    }

    /**
     * @param node 待寻找的函数
     * @param set  函数参数名称
     * @return 找到函数参数 返回位置position 否则返回0
     */
    private static Integer getMethodArgument(Node node, Node set) {
        //丢掉最后一项，函数体
        Integer argNum = 0;
        Integer argPosition = 0;
        if (node instanceof MethodDeclaration){
            for (int i = 0; i < node.getChildNodes().size(); i++) {
                if (node.getChildNodes().get(i) instanceof Parameter ){
                    argNum++;
                    if (((Parameter) node.getChildNodes().get(i)).getName().toString().equals(set.toString())){
                        argPosition = argNum;
                    }
                }
            }
        }
        return argPosition;
    }


    /**
     * @param node
     * @param set
     * @return 从方法中根据变量名表达式函数参数set找到对应的赋值右部表达式
     */
    private static Expression getRightExprFromMethod(Node node, Expression set) {
        //查找声明语句

        if (node instanceof VariableDeclarator){
            //直接初始化参数的声明语句 查找
            if (((VariableDeclarator)node).getInitializer().isPresent()
                    && ((VariableDeclarator)node).getName().toString().equals(set.toString())
                    && !((VariableDeclarator)node).getInitializer().get().toString().equals("")){

                Expression rightExpr = ((VariableDeclarator)node).getInitializer().get();
                if (rightExpr != null){
                    return rightExpr;
                }
            }
            return null;
        }
        //查找赋值语句
        else if (node instanceof AssignExpr  && ((AssignExpr)node).getTarget().toString().equals(set.toString())){
            Expression rightExpr = ((AssignExpr)node).getValue();
            if (rightExpr!=null){
                return rightExpr;
            }
            return null;
        }
        Expression rightExpr =null;
        for (Node child : node.getChildNodes()){
            Expression rightExprFromMethod = getRightExprFromMethod(child,set);
            if (rightExprFromMethod!=null)
                rightExpr = rightExprFromMethod;
        }
        if (rightExpr!=null)
            return rightExpr;
        return null;
    }

    private static Expression getRightExprFromField(List<FieldDeclaration> fieldDeclarations, Expression set) {

        for (int i = 0; i < fieldDeclarations.size(); i++) {

            FieldDeclaration field = fieldDeclarations.get(i);
            for (int j = 0; j < field.getVariables().size(); j++) {
                if (field.getVariables().get(j).getName().toString().equals(set.toString()) && field.getVariables().get(j).getInitializer().isPresent()){
                    return field.getVariables().get(j).getInitializer().get();
                }
            }
        }
        return null;
    }


    /**
     * @param set 方法名表达式
     * @param methodDeclarations 本类方法集合
     * @return
     */
    private static Expression getReturnStat(Node set, List<MethodDeclaration> methodDeclarations) {
        MethodCallExpr methodCallExpr = (MethodCallExpr) set;
        Expression expression =null;
        for (int i = 0; i < methodDeclarations.size(); i++) {
            if (methodDeclarations.get(i).getName().toString().equals(methodCallExpr.getName().toString())){
                expression = getMethodReturn(methodDeclarations.get(i));
            }
        }
        return expression;
    }

    private static Expression getMethodReturn(Node node) {
        Expression expression = null;
        if (node instanceof ReturnStmt){
            expression = ((ReturnStmt) node).getExpression().get();
            return expression;
        }
        for (Node child:node.getChildNodes()){
            Expression expr = getMethodReturn(child);
            if (expr!=null)
                expression = expr;
        }
        if (expression!=null)
            return expression;
        return null;
    }


}
