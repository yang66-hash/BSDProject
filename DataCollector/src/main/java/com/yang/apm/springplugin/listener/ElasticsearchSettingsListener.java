package com.yang.apm.springplugin.listener;

import com.yang.apm.springplugin.manager.ElasticsearchClientManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ElasticsearchSettingsListener {
    @Autowired
    private ElasticsearchClientManager elasticsearchClientManager;

    @EventListener
    public void onConfigChange(ElasticsearchSettingsChangeEvent event){
        log.info("elasticsearch settings changing...");
        elasticsearchClientManager.refreshElasticsearchClient();
    }

}
