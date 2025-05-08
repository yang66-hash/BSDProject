package com.yang.apm.springplugin.base.utils;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: xyc
 * @date: 2023-02-23 19:35
 *
 */
@Data
@Component
public class NodeService {
    NodeList nodeList;
    String serviceName;
}
