package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: store api url
 * @author: xyc
 * @date: 2022-12-22 19:32
 */
@Data
public class UrlItem {
    public String url1;
    public Map<String, String> url2;

    public UrlItem(){
        this.url2 = new HashMap<>();
    }
}
