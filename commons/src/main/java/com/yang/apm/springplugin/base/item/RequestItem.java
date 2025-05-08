package com.yang.apm.springplugin.base.item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestItem {
    private String serviceName;
    private String detectionID;
}
