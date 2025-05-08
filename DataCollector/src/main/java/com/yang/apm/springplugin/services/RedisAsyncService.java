package com.yang.apm.springplugin.services;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.constant.ConstantUtil;
import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.yang.apm.springplugin.constant.ConstantUtil.INCREMENT_WINDOW_OF_DYNAMIC_KEY;

@Service
@Slf4j
public class RedisAsyncService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;



    @Async
    public void pushToRedis(String key, DetectionResItem value) {
        log.info("add detection results to redis asynchronously ...");
        redisTemplate.opsForList().rightPush(key, value);
    }

    @Scheduled(fixedRate = 5000)
    @Async
    public void sendCacheToES(){
        //获取redis中的数据
        List<Object> data = redisTemplate.opsForList().range(ConstantUtil.REDIS_DETECTION_RECORD_LIST, 0, -1);
        List<DetectionResItem> failedItems = new LinkedList<>();

        if (data==null||data.size()==0){
            return;
        }
        //构建bulk request
        BulkRequest.Builder builder = new BulkRequest.Builder();
        for(Object obj : data){
            //将数据转换成JSON字符串，并进行反序列化
            String jsonString = JSON.toJSONString(obj);
            DetectionResItem item = JSONObject.parseObject(jsonString, DetectionResItem.class);
            BulkOperation bulkOperation = new BulkOperation.Builder().create(d->d.document(item).index(ConstantUtil.ELASTIC_SEARCH_DETECTION_RES_INDEX)).build();
           builder.operations(bulkOperation);
        }

        try {
            //发生错误，记录未能插入的数据，准备回填redis
            BulkResponse bulkResponse  = elasticsearchClientManager.getElasticsearchClient().bulk(builder.build());
            if (bulkResponse.errors()){
                log.error("insert data with bulk api of es error ...");
                for (int i = 0; i < bulkResponse.items().size(); i++) {
                    BulkResponseItem item = bulkResponse.items().get(i);
                    if (item.error() != null) {
                        log.error("insert data with bulk api of es error: " + item.error());
                        // 失败的项将被保存下来以便重试
                        String jsonString = JSON.toJSONString(data.get(i));
                        DetectionResItem failedItem = JSONObject.parseObject(jsonString, DetectionResItem.class);
                        failedItems.add(failedItem);
                    }
                }
                redisTemplate.opsForList().trim(ConstantUtil.REDIS_DETECTION_RECORD_LIST, data.size(),-1);
                redisTemplate.opsForList().rightPush(ConstantUtil.REDIS_DETECTION_RECORD_LIST, failedItems);
                log.info("failed detection results items write back to redis");

            }else {
                //没有错误 删除redis中相关数据
                log.info("insert "+data.size()+" detection records successfully...");
                redisTemplate.opsForList().trim(ConstantUtil.REDIS_DETECTION_RECORD_LIST, data.size(),-1);
                log.info("The first "+ data.size() + " detection records in redis were removed");
            }
        } catch (IOException e) {
            log.error("send detection result to es error");
            log.error(e.getMessage());
        }
    }


    /**
     * 将从es中取出并经过整理的数据发送到redis中
     * @param <T>
     */
    @Async
    public <T extends SvcRes> void saveT2Redis(List<T> svcRes,String resType, String collectTime){
        if (svcRes==null|| svcRes.isEmpty()){
            return;
        }
        Integer interval =(Integer) redisTemplate.opsForValue().get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        ObjectMapper objectMapper = new ObjectMapper();
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String keyPrefix = resType + "|"  + interval;
        log.info("writing data "  + keyPrefix  + " to redis");
        Function<T, Map<String, Object>> toMap = sr -> objectMapper.convertValue(sr, new TypeReference<Map<String, Object>>(){});
        svcRes.forEach(sr->{
            String key = keyPrefix + "|" + sr.getServiceName() + "|" + sr.getLanguage() + "|" + sr.getPodName() + "|" + collectTime;
            Map<String, Object> data = toMap.apply(sr);
            hashOperations.putAll(key, data);
            //test
            redisTemplate.expire(key, 610, TimeUnit.MINUTES);
        });
        log.info(keyPrefix + " data write to redis successfully");
    }




    /**
     * @param key 所取的hash类型数据的键值
     * @param clazz 所需转换成的Java类的字节码
     * @param <T> 一般来说是com.yang.apm.springplugin.pojo.result.SvcRes的子类
     * @return
     */
    public <T> T getHashAsObject(String key, Class<T> clazz){

        ObjectMapper objectMapper = new ObjectMapper();
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        Map<String, Object> entries = hashOperations.entries(key);
        return objectMapper.convertValue(entries, clazz);
    }
    /**
     * @return 查询运行时态下数据收集的时间间隔
     */
    public Integer searchInterval(){
        return (Integer) redisTemplate.opsForValue().get(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    }
    /**
     * @return 查询运行时态下进行历史数据计算的时间窗口大小
     */
    public Integer searchTimeWindow(){
        return (Integer) redisTemplate.opsForValue().get(ConstantUtil.TIME_WINDOW_OF_DYNAMIC_KEY);
    }

    /**
     * @return 查询运行时态下进行历史数据计算的时间窗口大小
     */
    public Integer searchCurWindow(){
        return (Integer) redisTemplate.opsForValue().get(INCREMENT_WINDOW_OF_DYNAMIC_KEY);
    }

    /**
     * 记录从redis中拉取的历史数据的次数,最大到用户设置的TIME_WINDOW_OF_DYNAMIC_KEY
     */
    public void incrUntilTen(){
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        Object currentValue = operations.get(INCREMENT_WINDOW_OF_DYNAMIC_KEY);
        Integer timeWindow = searchTimeWindow();
        if (currentValue == null) {
            currentValue = 1;
            operations.set(INCREMENT_WINDOW_OF_DYNAMIC_KEY, currentValue);  // 如果 key 不存在，初始化为 0
        }else if ((Integer) currentValue<timeWindow){

            operations.increment(INCREMENT_WINDOW_OF_DYNAMIC_KEY);
            currentValue = operations.get(INCREMENT_WINDOW_OF_DYNAMIC_KEY);
            log.info("current window size:" + currentValue);
        }


    }


    /**
     * 获符合匹配模式pattern的所有key的集合
     */
    public Set<String> matchedPatternSet(String pattern){
        //进行模糊匹配 从redis中找出指定的微服务的所有实例
        Set<String> result = new HashSet<>();
        RedisSerializer serializer = redisTemplate.getKeySerializer();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
        Cursor<byte[]> cursor = redisTemplate.execute(connection -> connection.scan(options), true);
        while (cursor.hasNext()){
            result.add(String.valueOf(serializer.deserialize(cursor.next())));
        }
        return result;

    }

}