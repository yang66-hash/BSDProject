package com.yang.apm.springplugin.base.item;

import lombok.Data;

/**
 * @description:
 * @author: xyc
 * @date: 2023-03-13 20:38
 */
@Data
public class DependCount {
    public String belongsService;
    public int importCount;
    public int outputCount;

    public DependCount(String belongsService){
        this.belongsService = belongsService;
    }
    public DependCount(DependCount dependCount){
        this.belongsService = dependCount.getBelongsService();
        this.importCount = dependCount.getImportCount();
        this.outputCount = dependCount.getOutputCount();
    }
}
