package com.yang.apm.springplugin.services.datacollector;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.manager.datacollector.ElasticsearchClientManager;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import com.yang.apm.springplugin.pojo.result.SvcTransRes;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用于临时存储各类需要发往es的数据
 */
@Service
@Slf4j
@Aspect
public class MetricsBufferService {

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;
    //直接定义一个阻塞队列的map，用于存储各类数据，key就是index的文件名 value为阻塞队列
    private final ConcurrentHashMap<String, LinkedBlockingDeque<SvcRes>> bufferMap = new ConcurrentHashMap<>();

    public <T extends SvcRes> void addItem2Index(T svcRes, String indexName) {
        LinkedBlockingDeque<SvcRes> deque = bufferMap.computeIfAbsent(indexName, k -> new LinkedBlockingDeque<>(3001));
        //当队列满时，将最久的数据丢弃 不使其阻塞
        if (deque.remainingCapacity()==0){
            deque.pollFirst();
        }
        deque.add(svcRes);
    }

    public <T extends SvcRes> void addList2Index(List<T> svcRes, String indexName) {
        LinkedBlockingDeque<SvcRes> deque = bufferMap.computeIfAbsent(indexName, k -> new LinkedBlockingDeque<>(3001));
        while (deque.remainingCapacity()<svcRes.size()){
            deque.pollFirst();
        }
        deque.addAll(svcRes);
    }


    //监听CalculateService类下所有以calculate开头的函数
    @Pointcut("execution(* com.yang.apm.springplugin.services.datacollector.CalculateService.calculate*(..))")
    public void calculateMethods() {}


    @After("calculateMethods()")
    public void afterMethodExecution() {
        log.info("A method starting with 'calculate' has been executed.");
        log.info("After method execution of sending data to es starting.");
        sendItem2ES();
        log.info("send operation finished");

    }

    public void sendItem2ES() {
        bufferMap.forEach((indexName, buffer) -> {
            List<SvcRes> svcResList = new LinkedList<>();
            buffer.drainTo(svcResList);
            //跳过近段时间没有访问的实例，因为其没有收集到访问数据
            if (svcResList.isEmpty()) {
                return;
            }
            if (indexName.contains(ConstantUtil.METRICS_EXTERNAL_INDEX_PREFIX)) {
                BulkRequest.Builder builder = elasticsearchClientManager.constructBulkOps(indexName, svcResList, SvcExternalMetricsRes.class);
                try {
                    BulkResponse bulk = elasticsearchClientManager.getElasticsearchClient().bulk(builder.build());
                    if (!bulk.errors())
                        log.info("External metrics of service were collected and  sent to ES successfully.");
                    else log.error("External metrics of service were collected and sent to ES failed.");

                } catch (IOException e) {
                    log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
                    buffer.addAll(svcResList);
                }
            } else if (indexName.contains(ConstantUtil.METRICS_INTERVAL_INDEX_PREFIX)) {
                BulkRequest.Builder builder = elasticsearchClientManager.constructBulkOps(indexName, svcResList, SvcMetricsRes.class);
                try {
                    BulkResponse bulk = elasticsearchClientManager.getElasticsearchClient().bulk(builder.build());
                    if (!bulk.errors())
                        log.info("Internal metrics of service were collected and  sent to ES successfully.");
                    else {
                        log.error("Internal metrics of service were collected and sent to ES failed.");
                        log.error("Bulk response errors: {}", bulk.toString());
                    }

                } catch (IOException e) {
                    log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
                    buffer.addAll(svcResList);
                }
            } else if (indexName.contains(ConstantUtil.METRICS_BUSINESS_INDEX_PREFIX)) {
                BulkRequest.Builder builder = elasticsearchClientManager.constructBulkOps(indexName, svcResList, BusinessMetricsRes.class);
                try {
                    BulkResponse bulk = elasticsearchClientManager.getElasticsearchClient().bulk(builder.build());
                    if (!bulk.errors())
                        log.info("Business metrics of service were collected and  sent to ES successfully.");
                    else log.error("Business metrics of service were collected and sent to ES failed.");

                } catch (IOException e) {
                    log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
                    buffer.addAll(svcResList);
                }
            }else if (ConstantUtil.CYCLE_DEPENDENCY_BUSINESS_INDEX_PREFIX.equals(indexName)) {
                BulkRequest.Builder builder = elasticsearchClientManager.constructBulkOps(indexName, svcResList, SvcTransRes.class);
                try {
                    BulkResponse bulk = elasticsearchClientManager.getElasticsearchClient().bulk(builder.build());
                    if (!bulk.errors())
                        log.info("Anomalous traces of services were collected and  sent to ES successfully.");
                    else log.error("Anomalous traces of services were collected and sent to ES failed.");

                } catch (IOException e) {
                    log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
                    buffer.addAll(svcResList);
                }
            }


        });
    }
}
