package com.yang.apm.springplugin.manager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

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

}
