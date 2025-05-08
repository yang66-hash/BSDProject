package com.yang.apm.springplugin.services.staticdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.GreedyContext;

import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.ServiceItem;
import com.yang.apm.springplugin.factory.FileFactory;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * @description:
 * @author: xyc
 * @date: 2023-03-13 15:31
 */
@Service
@Slf4j
public class GreedyService implements IDetectConvert {
    @Autowired
    public FileFactory fileFactory;
    @Autowired
    public WrongServiceCutService wrongServiceCutService;

    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;

    public DetectionResItem getGreedySvc(RequestItem requestItem) throws IOException {
        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.MICROSWRVICE_GREEDY.getValue());
        //相关异味信息
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
        //解析对应是哪一个微服务
        QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
        serviceQueryWrapper.eq("service_name", requestItem.getServiceName());
        //获取本地仓库地址
        PathMappingServiceItem mapping = pathMappingService.getOne(serviceQueryWrapper);
        String pathInLocalRepository = mapping.getPathInLocalRepository();
        log.info(pathInLocalRepository);
        GreedyContext greedyContext = new GreedyContext();

        Boolean isControllerFileExists = fileFactory.isControllerFileExists(pathInLocalRepository);
        List<String> staticFiles = fileFactory.getStaticFiles(pathInLocalRepository);
        String serviceName = fileFactory.getServiceName(pathInLocalRepository);
        if(!staticFiles.isEmpty() && staticFiles.size() <= 2 && isControllerFileExists){
            System.out.println("staticFiles"+staticFiles);
            greedyContext.addGreedySvc(new ServiceItem(serviceName, staticFiles));
        }
        else if(staticFiles.isEmpty() && wrongServiceCutService.getServicesEntityCount(requestItem).getWrongCutMap().containsKey(serviceName) && wrongServiceCutService.getServicesEntityCount(requestItem).getWrongCutMap().get(serviceName).get("entityCount") <1 && isControllerFileExists)
            greedyContext.addGreedySvc(new ServiceItem(serviceName, staticFiles));

        if(!greedyContext.getGreedySvc().isEmpty())
            greedyContext.setStatus(true);
        addNew2ResItem(greedyContext, detectionResItem, requestItem);
        return detectionResItem;
    }
}
