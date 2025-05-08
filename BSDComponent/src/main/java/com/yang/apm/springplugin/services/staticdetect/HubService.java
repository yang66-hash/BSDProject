package com.yang.apm.springplugin.services.staticdetect;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.HubContext;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.base.item.DependCount;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.utils.ApiParserUtils;
import com.yang.apm.springplugin.factory.FileFactory;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;


@Service
@Slf4j
public class HubService implements IDetectConvert {
    @Autowired
    public FileFactory fileFactory;

    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;

    @Autowired
    private ApiParserUtils apiParserUtils;

    public DetectionResItem getHubClass(RequestItem requestItem) {

        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.HUB_LIKE_DEPENDENCY.getValue());
        //相关异味信息
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
        long cur =System.currentTimeMillis();
        //解析对应是哪一个微服务
        QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
        serviceQueryWrapper.eq("service_name", requestItem.getServiceName());
        //获取本地仓库地址
        PathMappingServiceItem mapping = pathMappingService.getOne(serviceQueryWrapper);
        String pathInLocalRepository = mapping.getPathInLocalRepository();
        log.info(pathInLocalRepository);

        HubContext hubContext = new HubContext();

        Map<String, DependCount> imOutMap = new HashMap<>();
        //get all classes' qualified names

        String serviceName = "";
        try {
            serviceName = fileFactory.getServiceName(pathInLocalRepository);
            List<String> javaList = fileFactory.getJavaFiles(pathInLocalRepository);
            for(String file: javaList){
                apiParserUtils.getAllQualifiedName(new File(file), imOutMap, serviceName);
            }
            // to count imports and outports of per class
            for(String file: javaList){
                apiParserUtils.getImports(new File(file), imOutMap);
            }

        } catch (IOException e) {
            log.error("Error occurred when parsing Java files in Hub Service.");
            log.error(e.getMessage());
        }

        for(String cla: imOutMap.keySet()){
            int out = imOutMap.get(cla).getOutputCount();
            int in = imOutMap.get(cla).getImportCount();
            String belongsService = imOutMap.get(cla).getBelongsService();
            if(out >= 10 && in >= 10 && Math.max(out, in) * 0.9 <= Math.min(out, in)){
                hubContext.getHubclass().put(belongsService, new DependCount(imOutMap.get(cla)));
            }
        }
        hubContext.setSystemClassCount(imOutMap.size());
        if(!hubContext.getHubclass().isEmpty())
            hubContext.setStatus(true);

        addNew2ResItem(hubContext, detectionResItem, requestItem);
        return detectionResItem;
    }
}
