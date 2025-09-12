package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.indexmapping.DetectionResMappingStrategy;
import com.yang.apm.springplugin.indexmapping.ExternalMetricsMappingStrategy;
import com.yang.apm.springplugin.manager.BSDESClientManager;
import com.yang.apm.springplugin.utils.IndexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
    private BSDESClientManager BSDESClientManager;

    private final LinkedBlockingDeque<DetectionResItem> buffer = new LinkedBlockingDeque<>(100);


    @PostConstruct
    public void init() {
        //查看索引文件是否创建
        String detectionResIndex = IndexUtil.getDetectionResIndex();
        if (!BSDESClientManager.isIndexExisted(detectionResIndex)) {
            boolean indexWithStrategy = BSDESClientManager.createIndexWithStrategy(detectionResIndex, new DetectionResMappingStrategy());
            if (indexWithStrategy){
                log.info("Index {} created successfully", detectionResIndex);
            }else {
                log.error("Index {} not created", detectionResIndex);
            }
        }
    }

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
        List<BulkOperation> bulkOperations = new ArrayList<>();
        for(Object obj : batch){
            //将数据转换成JSON字符串，并进行反序列化
            String jsonString = JSON.toJSONString(obj);
            DetectionResItem item = JSONObject.parseObject(jsonString, DetectionResItem.class);
            BulkOperation bulkOperation = new BulkOperation.Builder().create(d->d.document(item).index(ConstantUtil.DETECTION_RESULT_INDEX)).build();
            bulkOperations.add(bulkOperation);
        }
        builder.operations(bulkOperations);
        try {
            BulkResponse bulk = BSDESClientManager.getElasticsearchClient().bulk(builder.build());
            if (!bulk.errors())
                log.info("Detection results were collected and  sent to ES successfully.");
            else log.error("Detection results were collected and sent to ES failed." + bulk.toString());
        } catch (IOException e) {
            buffer.addAll(batch);
            log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
        }
    }

}
