package com.yang.apm.springplugin.services.datacollector;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.yang.apm.springplugin.manager.datacollector.ElasticsearchClientManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ESQueryService {

    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;

    /**
     * @param request 封装后的查询请求
     * @param clazz 接收查数据目的接收类的类对象
     * @return 查询到的数据集合
     * @throws IOException
     */
    public <T> List<T> executeSearch(SearchRequest request, Class<T> clazz) throws IOException {
        List<T> result = new ArrayList<>();
        SearchResponse<T> response =  elasticsearchClientManager.getElasticsearchClient().search(request, clazz);
        for (Hit<T> hit : response.hits().hits()) {
            result.add(hit.source());
        }
        return result;
    }


}
