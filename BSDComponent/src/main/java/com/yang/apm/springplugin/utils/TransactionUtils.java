package com.yang.apm.springplugin.utils;

import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.constant.SQLType;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcTransRes;
import com.yang.apm.springplugin.pojo.result.chain.RequestChain;
import com.yang.apm.springplugin.pojo.result.chain.TraceChainNode;
import com.yang.apm.springplugin.pojo.result.sql.DBModel;
import com.yang.apm.springplugin.pojo.span.Span;
import com.yang.apm.springplugin.pojo.traces.TraceServiceInfo;
import com.yang.apm.springplugin.pojo.traces.TraceTransaction;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TransactionUtils {


    /**
     * 服务调用链的形成逻辑
     * 每一个服务的正式http请求都对应着一个transaction，且其transaction.type=request
     * 每一服务对外的请求的http,都对应着一个子span，且其span.id是对应服务相应的http的transaction的parent.id
     * 在按照trace.id分类之后，针对每一条调用链进行分析；
     * 将transaction request 和 external span分开，就能够依据每一个request去匹配对应的子请求有哪些
     */


    public static List<SvcExternalMetricsRes> analyzeTransaction4Metrics(Map<String, List<TraceServiceInfo>> traceListMap, Date endTime, Date startTime, Integer interval) {
        //记录某一服务实例下所有请求的时延（包括失败）
        Map<String,List<Integer>> latencyMap = new HashMap<>();
        //记录每一个服务实例的失败请求数量（不包含客户端请求错误 4xx）
        Map<String, Integer> serverFailRequestMap = new HashMap<>();
        //统计客户端错误
        Map<String, Integer> clientFailRequestMap = new HashMap<>();

        //记录每一服务实例API调用次数 <curSvcInstance,<APIName, APICallNum>>
        Map<String,Map<String, Integer>> APICallNumMap = new HashMap<>();
        //记录每一服务实例对其他服务的调用次数 <curSvcInstance,<calledSvcInstance, APICallNum>>
        Map<String, Map<String, Integer>> serviceCallNumMap = new HashMap<>();
        //记录每一服务实例下API的总执行时间以及调用次数  <curSvcInstance,<APIName, >> 最后计算得出API平均执行时间
        Map<String, Map<String, List<Integer>>> execTimeMap = new HashMap<>();


        //记录每一服务实例这段时间间隔内对关系型数据库的访问情况
        Map<String,Integer> sqlQueryNumMap = new HashMap<>();
        //记录每一服务实例下慢查询次数
        Map<String,Integer> sqlSlowQueryNumMap = new HashMap<>();
        //记录每一服务实例下SQL指纹及其查询次数
        Map<String, Map<String, Integer>> sqlFingerPrintMap = new HashMap<>();
        //记录每一服务实例下慢查询SQL指纹及其次数
        Map<String, Map<String, Integer>> sqlSlowFingerPrintMap = new HashMap<>();



        traceListMap.forEach((traceId,traceList)->{
            //将数据按照transaction和span划分开  transaction type=request 对应着一个具体的服务请求
            List<TraceServiceInfo> transactionList = traceList.stream().filter(traceService ->
                            traceService.getProcessor().getEvent()!=null &&
                                    "transaction".equals(traceService.getProcessor().getEvent()) &&
                                    "request".equals(traceService.getTransaction().getType()))
                    .sorted(Comparator.comparing(TraceServiceInfo::getTimeStamp)) //升序排列
                    .collect(Collectors.toList());

            List<TraceServiceInfo> spanList = traceList.stream().filter(traceService ->
                            traceService.getProcessor().getEvent()!=null &&
                                    "span".equals(traceService.getProcessor().getEvent()))
                    .sorted(Comparator.comparing(TraceServiceInfo::getTimeStamp)) //升序排列
                    .collect(Collectors.toList());
            //存储关系型数据库相关的span
            List<TraceServiceInfo> sqlSpanList = spanList.stream().filter(traceService ->
                    //找到关系型数据库的子类型
                            "db".equals(traceService.getSpan().getType())&&
                            SQLType.contains(traceService.getSpan().getSubtype())).collect(Collectors.toList());


            //计算每个服务实例interval间隔内所有请求的[平均时延, 请求失败率, 吞吐量, API调用次数, 对其他服务的调用次数, API平均执行时间]
            transactionList.forEach(traceService -> {
                String uniqueNote =  traceService.getService().getLanguage().getName()+ "|"
                        + traceService.getService().getName() + "|"
                        + traceService.getService().getNode().getName();
                //latencyMap的key必定包含所有的检测到的instance
                if (!latencyMap.containsKey(uniqueNote)) {
                    TraceTransaction transaction = traceService.getTransaction();
                    List<Integer> latencyList = new ArrayList<>();
                    latencyList.add(transaction.getDuration().getUs());
                    latencyMap.put(uniqueNote, latencyList);

                    //统计各类服务器端错误5xx 用于请求失败率
                    if (transaction.getResult().contains("5xx")){
                        serverFailRequestMap.put(uniqueNote, 1);
                        clientFailRequestMap.put(uniqueNote, 0);
                    }
                    else if (transaction.getResult().contains("4xx")) {
                        clientFailRequestMap.put(uniqueNote, 1);
                        serverFailRequestMap.put(uniqueNote, 0);
                    } else {
                        serverFailRequestMap.put(uniqueNote, 0);
                        clientFailRequestMap.put(uniqueNote, 0);
                    }

                    //API调用次数
                    Map<String, Integer> subAPICallNumMap = new HashMap<>();
                    String APIName = traceService.getTransaction().getName();
                    subAPICallNumMap.put(APIName, 1);
                    APICallNumMap.put(uniqueNote, subAPICallNumMap);

                    //对其他服务的调用次数
                    // 1.获取单一transaction对应的external span
                    Set<String> externalSpanIds = TransactionUtils.getReqSpansIdByParentId(spanList,transaction.getId());

                    //2.分析对应的request的transaction
                    Map<String,Integer> callMap = TransactionUtils.getExternalHttpCall(transactionList, externalSpanIds,uniqueNote);
                    //3.这部分数据进行存储
                    if (callMap!=null)
                        serviceCallNumMap.put(uniqueNote, callMap);
                    else
                        serviceCallNumMap.put(uniqueNote,new HashMap<>());
                    //API平均执行时间
                    Map<String, List<Integer>> subExecTimeMap = new HashMap<>();
                    List<Integer> execTimeList = new ArrayList<>();
                    execTimeList.add(transaction.getDuration().getUs());
                    subExecTimeMap.put(APIName, execTimeList);
                    execTimeMap.put(uniqueNote, subExecTimeMap);
                }else {
                    TraceTransaction transaction = traceService.getTransaction();
                    //时延
                    List<Integer> latencyList = latencyMap.get(uniqueNote);
                    latencyList.add(transaction.getDuration().getUs());
                    latencyMap.put(uniqueNote, latencyList);

                    //统计各类服务器端错误5xx 4xx 用于请求失败率
                    if (transaction.getResult().contains("5xx"))
                        serverFailRequestMap.put(uniqueNote, serverFailRequestMap.getOrDefault(uniqueNote, 0) + 1);
                    else if (transaction.getResult().contains("4xx")) {
                        clientFailRequestMap.put(uniqueNote, clientFailRequestMap.getOrDefault(uniqueNote, 0) + 1);
                    }

                    //吞吐量

                    long intervalToEnd = traceService.getTimeStamp().getTime() - startTime.getTime();

                    //API调用次数
                    Map<String, Integer> subAPICallNumMap = APICallNumMap.get(uniqueNote);
                    String APIName = traceService.getTransaction().getName();
                    subAPICallNumMap.put(APIName, subAPICallNumMap.getOrDefault(APIName, 0) + 1);
                    APICallNumMap.put(uniqueNote, subAPICallNumMap);


                    //对其他服务的调用次数
                    // 1.获取单一transaction对应的external span
                    Set<String> externalSpanIds = TransactionUtils.getReqSpansIdByParentId(spanList,transaction.getId());

                    //2.分析对应的request的transaction
                    Map<String,Integer> callMap = TransactionUtils.getExternalHttpCall(transactionList, externalSpanIds,uniqueNote);
                    //3.这部分数据并入已存在的Map中
                    if (callMap!=null){
                        Map<String,Integer> preCallMap = serviceCallNumMap.get(uniqueNote);
                        mergeData2PreCallMap(preCallMap,callMap);
                        serviceCallNumMap.put(uniqueNote, preCallMap);
                    }
                    //API平均执行时间
                    Map<String, List<Integer>> subExecTimeMap = execTimeMap.get(uniqueNote);
                    List<Integer> execTimeList;
                    if (!subExecTimeMap.containsKey(APIName)){
                        execTimeList = new ArrayList<>();
                    }else {
                        execTimeList = subExecTimeMap.get(APIName);
                    }
                    execTimeList.add(transaction.getDuration().getUs());
                    subExecTimeMap.put(APIName, execTimeList);
                    execTimeMap.put(uniqueNote, subExecTimeMap);
                }

                //统计当前请求的数据库查询次数和指纹信息
                dealWithSQL(traceService,uniqueNote,sqlSpanList,sqlQueryNumMap,sqlSlowQueryNumMap,sqlFingerPrintMap,sqlSlowFingerPrintMap);

            });


        });
        List<SvcExternalMetricsRes> resList = dealWithData(
                latencyMap,
                serverFailRequestMap,
                clientFailRequestMap,
                APICallNumMap,
                serviceCallNumMap,
                execTimeMap,
                sqlQueryNumMap,
                sqlSlowQueryNumMap,
                sqlFingerPrintMap,
                sqlSlowFingerPrintMap,
                endTime,
                startTime,
                interval);

        return resList;
    }


    /**
     * 因为trace涉及到的数据量很大，不能直接大量缓存在本地，而是直接设计每分钟获取每一个interval中的数据，进行结果分析
     * 或者允许设置采样率去控制采样trace的占比
     * 返回对应的结果信息
     */
    public static List<SvcTransRes> analyzeTransaction4Trace(Map<String, List<TraceServiceInfo>> traceListMap, Date endTime, Date startTime, Integer interval){
        //记录每一服务实例下每一条链路的相关信息 整条trace记录在请求头所在服务实例下
        Map<String, List<RequestChain>> requestChainMap = new HashMap<>();
        List<SvcTransRes> list = new LinkedList<>();
        traceListMap.forEach((traceId,traceList)->{
            //将数据按照transaction和span划分开  transaction type=request 对应着一个具体的服务请求
            List<TraceServiceInfo> transactionList = traceList.stream().filter(traceService ->
                            traceService.getProcessor().getEvent()!=null &&
                                    "transaction".equals(traceService.getProcessor().getEvent()) &&
                                    "request".equals(traceService.getTransaction().getType()))
                    .sorted(Comparator.comparing(TraceServiceInfo::getTimeStamp)) //升序排列
                    .collect(Collectors.toList());

            List<TraceServiceInfo> spanList = traceList.stream().filter(traceService ->
                            traceService.getProcessor().getEvent()!=null &&
                                    "span".equals(traceService.getProcessor().getEvent()))
                    .sorted(Comparator.comparing(TraceServiceInfo::getTimeStamp)) //升序排列
                    .collect(Collectors.toList());
            //存储关系型数据库相关的span
            List<TraceServiceInfo> sqlSpanList = spanList.stream().filter(traceService ->
                    //找到关系型数据库的子类型
                    "db".equals(traceService.getSpan().getType())&&
                            SQLType.contains(traceService.getSpan().getSubtype())).collect(Collectors.toList());


            RequestChain requestchain =  analyzeTraceChain(transactionList,spanList,sqlSpanList);
            dealWithTraceChain(requestchain,requestChainMap);
        });

        //处理链路信息

        //链路具体调用情况
        requestChainMap.forEach((uniqueNote, requestChainList) -> {
            String[] strings = uniqueNote.split("\\|");
            System.out.println(Arrays.toString(strings));
            
            // 为每个 RequestChain 创建一个 SvcTransRes
            requestChainList.forEach(chain -> {
                SvcTransRes svcTransRes = new SvcTransRes();
                svcTransRes.setStartTime(startTime);
                svcTransRes.setEndTime(endTime);
                svcTransRes.setInterval(interval);
                svcTransRes.setLanguage(strings[0]);
                svcTransRes.setServiceName(strings[1]);
                svcTransRes.setPodName(strings[2]);
                svcTransRes.setCollector(ManagementFactory.getRuntimeMXBean().getName());
                svcTransRes.setRequestChain(chain); // 设置单个 RequestChain
                
                // 统计链路属性
                calculateChainMetrics(svcTransRes, chain);
                
                list.add(svcTransRes);
            });
        });
        return list;
    }

    /**
     * 计算链路指标
     * @param svcTransRes 服务传输结果对象
     * @param chain 请求链
     */
    private static void calculateChainMetrics(SvcTransRes svcTransRes, RequestChain chain) {
        if (chain == null || chain.getChain() == null) {
            svcTransRes.setChainDepth(0);
            svcTransRes.setSqlCount(0);
            svcTransRes.setTotalExecTime(0.0);
            return;
        }
        
        // 计算链路深度
        int chainDepth = calculateChainDepth(chain.getChain());
        svcTransRes.setChainDepth(chainDepth);
        
        // 计算SQL请求次数
        int sqlCount = calculateSqlCount(chain.getChain());
        svcTransRes.setSqlCount(sqlCount);
        
        // 计算总执行时间
        double totalExecTime = calculateTotalExecTime(chain.getChain());
        svcTransRes.setTotalExecTime(totalExecTime);
    }
    
    /**
     * 计算链路深度
     * @param node 链路节点
     * @return 链路深度
     */
    private static int calculateChainDepth(TraceChainNode node) {
        if (node == null) {
            return 0;
        }
        
        int maxDepth = 1; // 当前节点深度为1
        
        if (node.getSubNodes() != null && !node.getSubNodes().isEmpty()) {
            for (TraceChainNode subNode : node.getSubNodes()) {
                int subDepth = calculateChainDepth(subNode);
                maxDepth = Math.max(maxDepth, 1 + subDepth);
            }
        }
        
        return maxDepth;
    }
    
    /**
     * 计算SQL请求次数
     * @param node 链路节点
     * @return SQL请求总次数
     */
    private static int calculateSqlCount(TraceChainNode node) {
        if (node == null) {
            return 0;
        }
        
        int sqlCount = 0;
        
        // 统计当前节点的SQL请求次数
        if (node.getContainSQL() != null && node.getContainSQL() && node.getSqlModelList() != null) {
            for (DBModel dbModel : node.getSqlModelList()) {
                if (dbModel.getDataQueryNum() != null) {
                    sqlCount += dbModel.getDataQueryNum();
                } else {
                    sqlCount += 1; // 如果没有查询次数，默认为1
                }
            }
        }
        
        // 递归统计子节点的SQL请求次数
        if (node.getSubNodes() != null && !node.getSubNodes().isEmpty()) {
            for (TraceChainNode subNode : node.getSubNodes()) {
                sqlCount += calculateSqlCount(subNode);
            }
        }
        
        return sqlCount;
    }
    
    /**
     * 计算总执行时间
     * @param node 链路节点
     * @return 总执行时间（微秒）
     */
    private static double calculateTotalExecTime(TraceChainNode node) {
        if (node == null) {
            return 0.0;
        }
        
        double totalTime = 0.0;
        
        // 统计当前节点的SQL执行时间
        if (node.getContainSQL() != null && node.getContainSQL() && node.getSqlModelList() != null) {
            for (DBModel dbModel : node.getSqlModelList()) {
                if (dbModel.getExecTime() != null && dbModel.getDataQueryNum() != null) {
                    totalTime += dbModel.getExecTime() * dbModel.getDataQueryNum();
                } else if (dbModel.getExecTime() != null) {
                    totalTime += dbModel.getExecTime();
                }
            }
        }
        
        // 递归统计子节点的执行时间
        if (node.getSubNodes() != null && !node.getSubNodes().isEmpty()) {
            for (TraceChainNode subNode : node.getSubNodes()) {
                totalTime += calculateTotalExecTime(subNode);
            }
        }
        
        return totalTime;
    }


    /**
     * 统计数据库查询的次数和指纹信息
     */
    private static void dealWithSQL(TraceServiceInfo traceService,
                                    String uniqueNote, List<TraceServiceInfo> sqlSpanList,
                                    Map<String, Integer> sqlQueryNumMap,
                                    Map<String, Integer> sqlSlowQueryNumMap,
                                    Map<String, Map<String, Integer>> sqlFingerPrintMap,
                                    Map<String, Map<String, Integer>> sqlSlowFingerPrintMap) {
        //找到属于请求traceService的sql语句执行span
        List<TraceServiceInfo> subSqlSpan = sqlSpanList.stream().filter(spanService ->
                spanService.getParent()!=null &&
                spanService.getParent().getId().equals(traceService.getSpan().getId())).collect(Collectors.toList());
        subSqlSpan.forEach(sqlSpan->{
            Span span = sqlSpan.getSpan();
            
            // 生成SQL指纹 - 使用SQL语句作为指纹，如果没有则使用表名
            String sqlFingerprint = generateSqlFingerprint(span);
            
            //单次的统计
            if (span.getComposite()==null){
                //是慢查询
                if (span.getDuration()!=null &&
                        span.getDuration().getUs().compareTo(ConstantUtil.SQL_EXEC_TIME_BOUND)>0){
                    sqlSlowQueryNumMap.put(uniqueNote,sqlSlowQueryNumMap.getOrDefault(uniqueNote, 0) + 1);
                    // 记录慢查询指纹
                    updateSqlFingerprint(sqlSlowFingerPrintMap, uniqueNote, sqlFingerprint, 1);
                }
                sqlQueryNumMap.put(uniqueNote,sqlQueryNumMap.getOrDefault(uniqueNote, 0) + 1);
                // 记录所有查询指纹
                updateSqlFingerprint(sqlFingerPrintMap, uniqueNote, sqlFingerprint, 1);
            //存在短时间内执行相同语句的情况
            }else{
                //总执行时间
                Integer totalTime = span.getComposite().getSum().getUs();
                //取出总执行次数
                Integer count = span.getComposite().getCount()==null?1:span.getComposite().getCount();
                Integer averageExecTime =  totalTime/count;
                if (averageExecTime.compareTo(ConstantUtil.SQL_EXEC_TIME_BOUND)>0){
                    sqlSlowQueryNumMap.put(uniqueNote,sqlSlowQueryNumMap.getOrDefault(uniqueNote, 0) + count);
                    // 记录慢查询指纹
                    updateSqlFingerprint(sqlSlowFingerPrintMap, uniqueNote, sqlFingerprint, count);
                }
                sqlQueryNumMap.put(uniqueNote,sqlQueryNumMap.getOrDefault(uniqueNote, 0) + count);
                // 记录所有查询指纹
                updateSqlFingerprint(sqlFingerPrintMap, uniqueNote, sqlFingerprint, count);
            }
        });
    }

    /**
     * 将requestChain加入到Map中
     */
    private static void dealWithTraceChain(RequestChain requestchain, Map<String, List<RequestChain>> requestChainMap) {
        if (requestchain==null)
            return;
        if (!requestChainMap.containsKey(requestchain.getSourceSvc())){
            List<RequestChain> requestChainList = new ArrayList<>();
            requestChainList.add(requestchain);
            requestChainMap.put(requestchain.getSourceSvc(), requestChainList);
        }else{
            List<RequestChain> requestChainList = requestChainMap.get(requestchain.getSourceSvc());
            requestChainList.add(requestchain);
            requestChainMap.put(requestchain.getSourceSvc(), requestChainList);
        }
    }


    /**
     * @param latencyMap           计算延迟
     * @param serverFailRequestMap 计算请求失败率 5xx
     * @param clientFailRequestMap 4xx
     * @param apiCallNumMap        每一个服务实例下被调用的API的调用次数
     * @param serviceCallNumMap    调用其他服务的次数
     * @param execTimeMap          平均执行时间
     * @param sqlQueryNumMap       SQL查询总次数统计
     * @param sqlSlowQueryNumMap   慢查询次数统计
     * @param sqlFingerPrintMap    SQL指纹及其查询次数映射
     * @param sqlSlowFingerPrintMap 慢查询SQL指纹及其次数映射
     * @param endTime              结束时间
     * @param startTime            开始时间
     * @param interval             时间间隔
     * @return                     处理后的指标结果列表
     */
    private static List<SvcExternalMetricsRes> dealWithData(Map<String, List<Integer>> latencyMap,
                                                            Map<String, Integer> serverFailRequestMap,
                                                            Map<String, Integer> clientFailRequestMap,
                                                            Map<String, Map<String, Integer>> apiCallNumMap,
                                                            Map<String, Map<String, Integer>> serviceCallNumMap,
                                                            Map<String, Map<String, List<Integer>>> execTimeMap,
                                                            Map<String, Integer> sqlQueryNumMap,
                                                            Map<String, Integer> sqlSlowQueryNumMap,
                                                            Map<String, Map<String, Integer>> sqlFingerPrintMap,
                                                            Map<String, Map<String, Integer>> sqlSlowFingerPrintMap,
                                                            Date endTime,
                                                            Date startTime,
                                                            Integer interval
                                                  ) {
        List<SvcExternalMetricsRes> resList = new ArrayList<>();
        Set<String> uniqueNoteSet = latencyMap.keySet();
        uniqueNoteSet.forEach(uniqueNote -> {
            //[平均时延, 请求失败率, 吞吐量, API调用次数, 对其他服务的调用次数, API平均执行时间]
            String[] strings = uniqueNote.split("\\|");
            System.out.println(Arrays.toString(strings));
            SvcExternalMetricsRes svcExternalMetricsRes = new SvcExternalMetricsRes();
            List<Integer> latencyList = latencyMap.get(uniqueNote);
            svcExternalMetricsRes.setStartTime(startTime);
            svcExternalMetricsRes.setEndTime(endTime);
            svcExternalMetricsRes.setInterval(interval);
            svcExternalMetricsRes.setLanguage(strings[0]);
            svcExternalMetricsRes.setServiceName(strings[1]);
            svcExternalMetricsRes.setPodName(strings[2]);
            svcExternalMetricsRes.setCollector(ManagementFactory.getRuntimeMXBean().getName());
            //记录请求数量
            svcExternalMetricsRes.setRequestCount(latencyList.size());
            Integer failReqCount = serverFailRequestMap.get(uniqueNote) ==null?0:serverFailRequestMap.get(uniqueNote);
            Integer clientFailReqCount = clientFailRequestMap.get(uniqueNote) == null?0:clientFailRequestMap.get(uniqueNote);
            svcExternalMetricsRes.setReqServerFailCount(failReqCount);
            svcExternalMetricsRes.setResClientFailCount(clientFailReqCount);
            svcExternalMetricsRes.setReqSucCount(latencyList.size()-failReqCount-clientFailReqCount);
            //计算平均时延
            Double avgLatency = latencyList.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            svcExternalMetricsRes.setAvgLatency(avgLatency);
            //计算失败率
            Double failPercent = failReqCount/(double)latencyList.size();
            svcExternalMetricsRes.setFailPercent(failPercent);
            //API调用次数
            svcExternalMetricsRes.setInstanceAPICallNumMap(apiCallNumMap.get(uniqueNote));
            //对其他服务调用次数
            svcExternalMetricsRes.setServiceCallNumMap(serviceCallNumMap.get(uniqueNote));
            //API 平均执行时间
            Map<String,List<Integer>> subExecTimeMap = execTimeMap.get(uniqueNote);
            Map<String,Double> subAvgExecTImeMap = calculateAPIAvgExecTime(subExecTimeMap);
            svcExternalMetricsRes.setAPIExecTimeMap(subAvgExecTImeMap);

            //sql数据统计
            svcExternalMetricsRes.setSqlQueryCount(sqlQueryNumMap.get(uniqueNote) == null ? 0 : sqlQueryNumMap.get(uniqueNote));
            svcExternalMetricsRes.setSlowQueryCount(sqlSlowQueryNumMap.get(uniqueNote) == null ? 0 : sqlSlowQueryNumMap.get(uniqueNote));
            
            //sql指纹数据统计
            svcExternalMetricsRes.setSqlFingerPrintMap(sqlFingerPrintMap.get(uniqueNote));
            svcExternalMetricsRes.setSqlSlowFingerPrintMap(sqlSlowFingerPrintMap.get(uniqueNote));
            
            resList.add(svcExternalMetricsRes);
        });

        return resList;

    }

    /**
     * 计算每一个服务实例的API的平均执行时间
     */
    private static Map<String, Double> calculateAPIAvgExecTime(Map<String, List<Integer>> subExecTimeMap) {
        if (subExecTimeMap == null)
            return null;
        Map<String, Double> map = new HashMap<>();
        subExecTimeMap.forEach((uniqueNote,execTimeList)->{
            Double execTime = execTimeList.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            map.put(uniqueNote, execTime);
        });
        return map;
    }

    /**
     * 将数据并入preCallMap
     */
    private static void mergeData2PreCallMap(Map<String, Integer> preCallMap, Map<String, Integer> callMap) {
        callMap.forEach((key,value)->{
            if (preCallMap.containsKey(key)){
                preCallMap.put(key, preCallMap.get(key)+value);
            }else
                preCallMap.put(key, value);
        });
    }

    /**
     * 将服务调用的关系存储输出
     *
     * @param transactionList request对应的transaction
     * @param externalSpanIds 外部请求的external http span id，与transaction的parentId对应
     * @param sourceSvcInstance 原服务实例
     * @return
     */
    private static Map<String, Integer> getExternalHttpCall(List<TraceServiceInfo> transactionList, Set<String> externalSpanIds, String sourceSvcInstance) {
        if (externalSpanIds.isEmpty())
            return null;
        Map<String, Integer> map = new HashMap<>();
        transactionList.forEach(traceService -> {
            if (traceService.getParent()!=null && externalSpanIds.contains(traceService.getParent().getId())){
                String targetUniqueNote = traceService.getService().getLanguage().getName() + "|" +
                        traceService.getService().getName() + "|" +
                        traceService.getService().getNode().getName();
                map.put(targetUniqueNote, map.getOrDefault(targetUniqueNote, 0) + 1);
            }
        });

        return map;
    }


    /**
     * 用于分析这个请求链路下的服务对各类服务的调用次数
     *
     * @param transactionList 是对当前链路下所有transaction的集合
     * @param spanList        当前链路下所有span的集合
     * @param sqlSpanList
     * @return
     */
    private static RequestChain analyzeTraceChain(List<TraceServiceInfo> transactionList,
                                                  List<TraceServiceInfo> spanList,
                                                  List<TraceServiceInfo> sqlSpanList) {
        //分析形成服务请求的链路图
        int traceHeadNum = 0;
        int pos = 0;
        for (int i= 0;i<transactionList.size();i++) {
            if (transactionList.get(i).getParent()==null){
                traceHeadNum++;
                pos = i;
            }
        }
        //说明收集的链路有问题，不存在链路头节点或者是多个头节点
        if (traceHeadNum!=1)
            return null;
        RequestChain traceHead = new RequestChain();
        TraceServiceInfo headService = transactionList.get(pos);
        //设置trace id
        traceHead.setTraceId(headService.getTrace().getId());
        //实例唯一标识
        String uniqueNote = headService.getService().getLanguage().getName()+ "|" +
                headService.getService().getName() + "|" +
                headService.getService().getNode().getName();
        //设置链路请求请求头的服务实例
        traceHead.setSourceSvc(uniqueNote);
        traceHead.setAPIName(headService.getTransaction().getName());
        //计算对外部请求的API的调用次数
        Map<String, Integer> targetSvcNumMap = getTargetSvcCallNumDetail(headService,transactionList,spanList);
        traceHead.setTargetSvcNumMap(targetSvcNumMap);
        TraceChainNode headNode = buildRequestChainNode(headService,transactionList,spanList,sqlSpanList);
        traceHead.setChain(headNode);
        return traceHead;
    }


    /**
     * @param headService 链路的头请求
     * @param transactionList 链路中的transaction集合
     * @param spanList 链路中的span集合
     * 获取每一条链路下的第一层外部请求 该source服务对target服务下API的调用次数统计
     */
    private static Map<String, Integer> getTargetSvcCallNumDetail(TraceServiceInfo headService, List<TraceServiceInfo> transactionList, List<TraceServiceInfo> spanList) {

        Set<String> externalHTTPIds = getReqSpansIdByParentId(spanList,headService.getTransaction().getId());
        List<TraceServiceInfo> subTranList = filterReqTransactions(transactionList,externalHTTPIds);
        if (subTranList==null)
            return null;
        Map<String, Integer> map = new HashMap<>();
        subTranList.forEach(subTran->{
            String uniqueAPINote = subTran.getService().getLanguage().getName()+ "|" +
                    subTran.getService().getName() + "|" +
                    subTran.getService().getNode().getName() + "|" +
                    subTran.getTransaction().getName();
            map.put(uniqueAPINote, map.getOrDefault(uniqueAPINote, 0) + 1);
        });
        return map;
    }

    private static TraceChainNode buildRequestChainNode(TraceServiceInfo traceService,
                                                        List<TraceServiceInfo> transactionList,
                                                        List<TraceServiceInfo> spanList,
                                                        List<TraceServiceInfo> sqlSpanList) {
        TraceChainNode node = new TraceChainNode();
        String instanceUniqueNote = traceService.getService().getLanguage().getName()+ "|" +
                traceService.getService().getName() + "|" +
                traceService.getService().getNode().getName();
        node.setServiceName(instanceUniqueNote);
        node.setAPIName(traceService.getTransaction().getName());

        //数据库相关操作统计
        List<DBModel> dbModels = getReqSqlList(traceService, sqlSpanList);
        if (dbModels==null){
            node.setContainSQL(false);
            node.setSqlModelList(null);
        }else {
            node.setContainSQL(true);
            node.setSqlModelList(dbModels);
        }
        Set<String> externalHTTPIds = getReqSpansIdByParentId(spanList,traceService.getTransaction().getId());
        List<TraceServiceInfo> subTranList = filterReqTransactions(transactionList,externalHTTPIds);
        if (subTranList!=null)
            subTranList.forEach(subTran->{
                TraceChainNode subNode = buildRequestChainNode(subTran,transactionList,spanList, sqlSpanList);
                node.addSubNode(subNode);
            });
        return node;
    }

    private static List<DBModel> getReqSqlList(TraceServiceInfo traceService, List<TraceServiceInfo> sqlSpanList) {
        //找到属于请求traceService的sql语句执行span
        List<TraceServiceInfo> subSqlSpan = sqlSpanList.stream().filter(spanService ->
                spanService.getParent()!=null &&
                        spanService.getParent().getId().equals(traceService.getSpan().getId())).collect(Collectors.toList());
        //表示当前请求没有关系型数据库的操作
        if (subSqlSpan.isEmpty())
            return null;
        List<DBModel> dbModels = new ArrayList<>();
        subSqlSpan.forEach(spanService->{
            DBModel dbModel = buildSQLDBModel(spanService);
            dbModels.add(dbModel);
        });
        return dbModels;
    }

    private static DBModel buildSQLDBModel(TraceServiceInfo spanService) {
        DBModel dbModel = new DBModel();
        Span span = spanService.getSpan();
        dbModel.setStatement(span.getDb().getStatement());
        dbModel.setInstance(span.getDb().getInstance());
        dbModel.setDbType(span.getDb().getType());
        dbModel.setDbFurType(span.getSubtype());
        //多次操作，apm将数据库操作压缩存储
        if (span.getComposite()!=null){
            Integer count = span.getComposite().getCount()==null?1:span.getComposite().getCount();
            Integer totalTime = span.getComposite().getSum().getUs();
            dbModel.setDataQueryNum(span.getComposite().getCount());
            Integer avgExecTime = totalTime/count;
            dbModel.setExecTime(avgExecTime);
        }else {
            dbModel.setExecTime(span.getDuration().getUs());
            dbModel.setDataQueryNum(1);
        }
        return dbModel;
    }


    /*
     * 依据传入的 transaction id 获取其external request请求的子span id 集合
     *
     */
    private static Set<String> getReqSpansIdByParentId(List<TraceServiceInfo> spanList, String transactionId){
        return spanList.stream()
                .filter(spanTrace ->
                        "span".equals(spanTrace.getProcessor().getEvent()) &&
                                "external".equals(spanTrace.getSpan().getType()) &&
                                spanTrace.getSpan().getSubtype().contains("http") &&
                                spanTrace.getParent().getId()!=null &&
                                spanTrace.getParent().getId().equals(transactionId))
                .map(traceService -> traceService.getSpan().getId())
                .collect(Collectors.toSet());
    }

    /**
     * 获取对外http请求对应的transaction 记录
     */
    private static List<TraceServiceInfo> filterReqTransactions(List<TraceServiceInfo> transactionList, Set<String> externalSpanIds){
        if (externalSpanIds==null || externalSpanIds.isEmpty())
            return null;
       return transactionList.stream().filter(traceService ->
               traceService.getParent()!=null &&
               externalSpanIds.contains(traceService.getParent().getId())).collect(Collectors.toList());
    }

    /**
     * 将es中获取的数据依据Trace ID进行划分
     */
    public static Map<String,List<TraceServiceInfo>> parseData2TraceMap(List<TraceServiceInfo> traceServiceList){
        Map<String,List<TraceServiceInfo>> tracesListMap = new HashMap<>();

        traceServiceList.forEach(traceService -> {
            if (!tracesListMap.containsKey(traceService.getTrace().getId())){
                List<TraceServiceInfo> list = new ArrayList<>();
                list.add(traceService);
                tracesListMap.put(traceService.getTrace().getId(),list);
            }else {
                List<TraceServiceInfo> list = tracesListMap.get(traceService.getTrace().getId());
                list.add(traceService);
                tracesListMap.put(traceService.getTrace().getId(),list);
            }
        });
        return tracesListMap;
    }

    /**
     * 生成SQL指纹 - 直接使用DBModel中的statement字段值作为key
     */
    private static String generateSqlFingerprint(Span span) {
        if (span.getDb() == null) {
            return "UNKNOWN_SQL";
        }
        
        // 直接使用statement字段的值作为指纹key
        String statement = span.getDb().getStatement();
        if (statement != null && !statement.trim().isEmpty()) {
            return statement.trim();
        }
        
        // 如果没有SQL语句，使用备用标识
        String type = span.getDb().getType();
        String instance = span.getDb().getInstance();
        String subtype = span.getSubtype();
        
        // 构建备用指纹：数据库类型.子类型.实例名
        StringBuilder fingerprint = new StringBuilder();
        if (type != null) {
            fingerprint.append(type);
        } else {
            fingerprint.append("UNKNOWN");
        }
        
        fingerprint.append(".");
        
        if (subtype != null) {
            fingerprint.append(subtype);
        } else {
            fingerprint.append("UNKNOWN");
        }
        
        fingerprint.append(".");
        
        if (instance != null) {
            fingerprint.append(instance);
        } else {
            fingerprint.append("UNKNOWN");
        }
        
        return fingerprint.toString();
    }

    
    /**
     * 更新SQL指纹映射
     */
    private static void updateSqlFingerprint(Map<String, Map<String, Integer>> fingerprintMap, 
                                           String uniqueNote, 
                                           String fingerprint, 
                                           Integer count) {
        fingerprintMap.computeIfAbsent(uniqueNote, k -> new HashMap<>())
                     .merge(fingerprint, count, Integer::sum);
    }

}
