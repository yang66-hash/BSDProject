package com.yang.apm.springplugin.manager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.ObjectBuilder;
import com.yang.apm.springplugin.pojo.ElasticsearchSettings;
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
import java.util.function.Function;

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

}
