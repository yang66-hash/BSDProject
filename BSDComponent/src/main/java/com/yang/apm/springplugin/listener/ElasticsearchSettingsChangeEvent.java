package com.yang.apm.springplugin.listener;

import org.springframework.context.ApplicationEvent;

public class ElasticsearchSettingsChangeEvent extends ApplicationEvent {
    public ElasticsearchSettingsChangeEvent(Object source) {
        super(source);
    }
}
