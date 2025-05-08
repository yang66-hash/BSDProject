package com.yang.apm.springplugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DataCollectorEventPublisher {

    private ApplicationEventPublisher publisher;
    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }


    public void publishElasticsearchSettingsChangeEvent(){
        ElasticsearchSettingsChangeEvent elasticsearchSettingsChangeEvent = new ElasticsearchSettingsChangeEvent(this);
        log.info("publish elasticsearch settings change event ...");
        publisher.publishEvent(elasticsearchSettingsChangeEvent);
    }
}
