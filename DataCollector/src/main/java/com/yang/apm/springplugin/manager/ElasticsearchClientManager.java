package com.yang.apm.springplugin.manager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.ObjectBuilder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yang.apm.springplugin.manager.indexmapping.IndexMappingStrategy;
import com.yang.apm.springplugin.pojo.ElasticsearchSettings;
import com.yang.apm.springplugin.pojo.result.SvcExternalMetricsRes;
import com.yang.apm.springplugin.pojo.result.SvcRes;
import com.yang.apm.springplugin.pojo.result.jvm.SvcMetricsRes;
import com.yang.apm.springplugin.utils.ElasticSearchQueryManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static org.bouncycastle.asn1.cms.CMSObjectIdentifiers.data;

@Component
@Slf4j
public class ElasticsearchClientManager {

    @Autowired
    private ElasticsearchSettings elasticsearchSettings;

    @Getter
    private volatile ElasticsearchClient elasticsearchClient;

    // 需要在ElasticsearchSettings完成依赖注入之后之后再去执行，选择PostConstruct，在完成构造函数注入和依赖注入之后、Bean被使用之前执行相关属性的初始化
    @PostConstruct
    public void init() {
        this.refreshElasticsearchClient();  // 初始化时创建 ElasticsearchClient


    }

    // 每次监听事件时，重新创建ElasticsearchClient
    public void refreshElasticsearchClient() {
        if (elasticsearchClient != null) {
            try {
                elasticsearchClient._transport().close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(elasticsearchSettings.getUsername(), elasticsearchSettings.getPassword()));

        RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchSettings.getUris().get(0)))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider))
                .build();

        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        elasticsearchClient = new ElasticsearchClient(transport);
        log.info("new elasticsearch client created...");

    }


    /**
     * 创建指定的索引存储文件
     * @param indexName 索引名称
     * @param fn mapping properties接口函数
     * @return
     */
    public boolean createIndexByName(String indexName, Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> fn) {
        CreateIndexRequest createIndexRequest = CreateIndexRequest.of(builder -> builder
                .index(indexName)
                .settings(s->s
                                .numberOfReplicas("1")
                        )
                .mappings(fn));
        try {
            CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(createIndexRequest);
            if (createIndexResponse.acknowledged() && createIndexResponse.shardsAcknowledged()){
                log.info("Index created successfully, and the shards is active.");
                return true;
            }else if (createIndexResponse.acknowledged()) {
                log.warn("Index created successfully, but some shards are not active.");
            } else {
                log.error("Index created failed.");
            }
        } catch (IOException e) {
            log.error("create index:{} in elasticsearch for BSD failed. Error message: {}",indexName,e.getMessage());
        }
        return false;
    }


    /**
     * @param indexName 索引文件名
     * @param strategy 自定义的策略，用于创建external metrics等
     * @return
     */
    public boolean createIndexWithStrategy(String indexName, IndexMappingStrategy strategy) {
        Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> fn = strategy.createMapping();
        log.info("Invoke createIndexByName in createIndexWithStrategy.");
        return createIndexByName(indexName, fn);
    }


    /**
     * @param indexName 索引文件名称
     * @param data 需要上传的数据
     * @param <T> 需要是SvcRes的子类
     * @return
     */
    public <T extends SvcRes> boolean saveData2Index(String indexName, T data){
        try {
            IndexRequest.Builder<Object> builder = new IndexRequest.Builder<>();
            IndexRequest<Object> indexRequest = builder
                    .index(indexName)
                    .document(data)
                    .build();

            IndexResponse index = elasticsearchClient.index(indexRequest);
            if (index.result() == Result.Created){
                log.info("Document indexed successfully!");
                return true;
            }else {
                log.error("Document indexed failed! {}", index.result());
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to add document.errors as follows: {}",e.getMessage());
            return false;
        }
    }

    /**
     * @param indexName 索引文件名
     * @param data 所需发送的数据
     * @return 调用Bulk API 批量发送数据
     */
    public <T extends SvcRes> boolean bulkData2Index(String indexName, List<T> data){
        if (data==null||data.isEmpty()){
            return true;
        }
        try {
            BulkRequest.Builder builder;
            if (data.get(0) instanceof SvcExternalMetricsRes){
                builder = constructBulkOps(indexName, data, SvcExternalMetricsRes.class);
                elasticsearchClient.bulk(builder.build());
                log.info("External metrics of service were collected and  sent to ES successfully.");

            }
            if (data.get(0) instanceof SvcMetricsRes){
                builder = constructBulkOps(indexName, data, SvcMetricsRes.class);
                elasticsearchClient.bulk(builder.build());
                log.info("Metrics of service were collected and  sent to ES successfully.");
            }
            //有待扩展
            return true;
        } catch (IOException e) {
            log.error("Bulk API Failed, data were resent to blocking queue. Try again later.", e);
            return false;
        }
    }

    /**
     * @param indexName 文件索引名
     * @param svcResList 待发送的数据列表
     * @param clazz 数据转换时类对象
     * @param
     * @return 构造指定数据svcResList上传到index的BulkRequest.Builder
     */
    public  <T extends SvcRes> BulkRequest.Builder constructBulkOps(String indexName, List<?> svcResList, Class<T> clazz ) {
        BulkRequest.Builder builder = new BulkRequest.Builder();

        for(Object obj : svcResList){
            //将数据转换成JSON字符串，并进行反序列化
            String jsonString = JSON.toJSONString(obj);
            T item = JSONObject.parseObject(jsonString, clazz);
            BulkOperation bulkOperation = new BulkOperation.Builder().create(d->d.document(item).index(indexName)).build();
            builder.operations(bulkOperation);
        }
        return builder;
    }

    /**
     * @param indexName 索引文件名
     * @return 判断某一个索引文件是否存在 存在返回true
     */
    public boolean isIndexExisted(String indexName){
        try {
            ExistsRequest existsRequest = ExistsRequest.of(builder -> builder.index(indexName));
            BooleanResponse exists = elasticsearchClient.indices().exists(existsRequest);
            return exists.value();
        } catch (IOException e) {
            log.error("index file in error status.error as below: {}", e.getMessage());
            return false;
        }
    }


}
