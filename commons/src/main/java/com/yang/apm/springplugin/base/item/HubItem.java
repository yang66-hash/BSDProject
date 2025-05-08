package com.yang.apm.springplugin.base.item;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: xyc
 * @date: 2023-03-13 17:29
 */
@Data
public class HubItem {
    public String serviceName;
    public List<String> hubLikeClass;
}
