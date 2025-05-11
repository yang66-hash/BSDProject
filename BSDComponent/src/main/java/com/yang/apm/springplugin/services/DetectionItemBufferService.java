package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 用于将缓存检测结果同时定时将检测结果发送到es中
 */
@Service
@Slf4j
public class DetectionItemBufferService {

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;

    private final LinkedBlockingDeque<DetectionResItem> buffer = new LinkedBlockingDeque<>(100);

    /**
     * 有界双端队列，容量固定。当满时，我们在插入新元素前先丢掉最旧的一个
     * @param item
     */
    public void addResItem(DetectionResItem item){
        if (buffer.remainingCapacity() == 0){
            buffer.pollFirst();
        }
        buffer.offerLast(item);
    }

    //定时发送并清空数据

    @Scheduled(fixedRate = 5000)
    @Async
    public void sendItem2ES(){
        //获取本地缓存中的数据
        List<DetectionResItem> batch = new ArrayList<>();
        buffer.drainTo(batch,100);
        if (batch.isEmpty()){
            return;
        }
        //构建bulk request
        BulkRequest.Builder builder = new BulkRequest.Builder();
        for(Object obj : batch){
            //将数据转换成JSON字符串，并进行反序列化
            String jsonString = JSON.toJSONString(obj);
            DetectionResItem item = JSONObject.parseObject(jsonString, DetectionResItem.class);
            BulkOperation bulkOperation = new BulkOperation.Builder().create(d->d.document(item).index(ConstantUtil.ELASTIC_SEARCH_DETECTION_RES_INDEX)).build();
            builder.operations(bulkOperation);
        }

        try {
            elasticsearchClientManager.getElasticsearchClient().bulk(builder.build());
        } catch (IOException e) {
            buffer.addAll(batch);
            log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
        }
    }

}
