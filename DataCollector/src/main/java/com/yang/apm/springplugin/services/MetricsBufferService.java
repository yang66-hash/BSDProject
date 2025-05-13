package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import com.yang.apm.springplugin.sevices.db.IntervalWindowMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class MetricsBufferService {

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;
    //直接定义一个阻塞队列的map，用于存储各类数据，key就是index的文件名 value为阻塞队列
    private final ConcurrentHashMap<String, LinkedBlockingDeque<SvcRes>> bufferMap = new ConcurrentHashMap<>();
    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;
    @Autowired
    private TaskScheduler taskScheduler;

    private ScheduledFuture<?> future;

    public <T extends SvcRes> void addItem2Index(T svcRes, String indexName) {
        LinkedBlockingDeque<SvcRes> deque = bufferMap.computeIfAbsent(indexName, k -> new LinkedBlockingDeque<>(3001));
        //当队列满时，将最久的数据丢弃 不使其
        if (deque.remainingCapacity()==0){
            deque.pollFirst();
        }
        deque.add(svcRes);
    }

    private void scheduledNext(){
        int interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        //动态的定时调度
        future = taskScheduler.schedule(this::sendItem2ES, new Date(System.currentTimeMillis() + interval*1000));
    }

    public void sendItem2ES(){
        //获取本地缓存中的数据
        bufferMap.forEach((indexName, buffer)->{
            List<SvcRes> svcResList = new LinkedList<>();
            buffer.drainTo(svcResList);
            if (indexName.contains(ConstantUtil.METRICS_EXTERNAL_INDEX_PREFIX)){
                BulkRequest.Builder builder = elasticsearchClientManager.constructBulkOps(indexName, svcResList, SvcExternalMetricsRes.class);
                try {
                    elasticsearchClientManager.getElasticsearchClient().bulk(builder.build());
                } catch (IOException e) {
                    log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
                    buffer.addAll(svcResList);
                }finally {
                    scheduledNext();
                }
            }

        });
    }

}
