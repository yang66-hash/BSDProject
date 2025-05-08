package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: used to store static file of service
 * @author: xyc
 * @date: 2023-03-13 15:18
 */
@Data
public class ServiceItem {
    public String serviceName;
    public List<String> staticFilesList;

    public ServiceItem(){
        this.staticFilesList = new ArrayList<>();
    }
    public ServiceItem(String serviceName, List<String> staticFilesList){
        this.serviceName = serviceName;
        this.staticFilesList = staticFilesList;
    }
}
