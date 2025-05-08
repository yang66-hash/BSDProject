package com.yang.apm.springplugin.services.staticdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.WrongCutContext;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.utils.ApiParserUtils;
import com.yang.apm.springplugin.factory.FileFactory;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import com.yang.apm.springplugin.utils.PathUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2022-12-26 21:02
 */

@Service
@NoArgsConstructor
@Slf4j
public class WrongServiceCutService implements IDetectConvert {

    @Autowired
    public FileFactory fileFactory;
    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;

    public WrongCutContext getServicesEntityCount(RequestItem requestItem) throws IOException {
        WrongCutContext wrongCutContext = new WrongCutContext();
        QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
        serviceQueryWrapper.eq("service_name", requestItem.getServiceName());
        PathMappingServiceItem mapping = pathMappingService.getOne(serviceQueryWrapper);
        String pathInLocalRepository = mapping.getPathInLocalRepository();
        //父路径
        String path = PathUtil.extractPathPart(pathInLocalRepository);

        String servicesDirectory = new File(path).getAbsolutePath();
        log.info(servicesDirectory);
        List<String> servicesPath = fileFactory.getSubServicePaths(servicesDirectory);
        ApiParserUtils apiParserUtils = new ApiParserUtils();
        for (String svc : servicesPath) {
            //解析配置文件获取服务名
            String serviceName = fileFactory.getServiceName(svc);
            Set<String> entitySet = new HashSet<>();
            List<String> javaFiles = fileFactory.getJavaFiles(svc);
            for (String javafile : javaFiles) {
                File file = new File(javafile);
                if(javafile.toLowerCase().contains(File.separator + "entity" + File.separator)
                        || javafile.toLowerCase().contains(File.separator + "domain" + File.separator)
                        || javafile.toLowerCase().contains(File.separator + "pojo" + File.separator)){
                    entitySet.add(javafile);
                }
                Set<String> count = new HashSet<>();
                if (apiParserUtils.isEntityClass(file, count, serviceName) == 1){
                    entitySet.add(javafile);
                }
            }
            if(!serviceName.equals("")){
                Map<String, Integer> map = new HashMap<>();
                if(entitySet.size() != 0){
                    map.put("entityCount",entitySet.size());
                    wrongCutContext.getWrongCutMap().put(serviceName,map);
                }

            }
        }
        return  wrongCutContext;
    }

    public double getAvgEntityCount(WrongCutContext wrongCutContext){
        double avgEntityCount = 0;
        int size = wrongCutContext.getWrongCutMap().size();
        for (String svc : wrongCutContext.getWrongCutMap().keySet()) {
            avgEntityCount += wrongCutContext.getWrongCutMap().get(svc).get("entityCount").intValue();
        }
        avgEntityCount = avgEntityCount / size;
        return avgEntityCount;

    }
    public DetectionResItem getWrongServiceCutServices(RequestItem requestItem){

        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.WRONG_CUTS.getValue());
        //相关异味信息
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
        long cur =System.currentTimeMillis();
        //解析对应是哪一个微服务
        QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
        serviceQueryWrapper.eq("service_name", requestItem.getServiceName());
        //获取本地仓库地址
        WrongCutContext wrongCutContext= null;
        try {
            wrongCutContext = this.getServicesEntityCount(requestItem);
        } catch (IOException e) {
            log.error("Error occurred when parse the mapping between entities number and microservices.");
            log.error(e.getMessage());
        }


        int size = wrongCutContext.getWrongCutMap().size();
        double avgEntityCount = this.getAvgEntityCount(wrongCutContext);
        double quadraticSum = 0.0;
        for (String svc : wrongCutContext.getWrongCutMap().keySet()) {
            quadraticSum += Math.pow(wrongCutContext.getWrongCutMap().get(svc).get("entityCount").intValue() - avgEntityCount, 2);
        }
        double std = Math.sqrt(quadraticSum / size);
        WrongCutContext wrongCutResult = new WrongCutContext();
        for (String svc : wrongCutContext.getWrongCutMap().keySet()) {
            int entityCount = wrongCutContext.getWrongCutMap().get(svc).get("entityCount").intValue();
            if(Math.abs(entityCount - avgEntityCount) >= 3* std && (size != 1) && std != 0) {
                if(entityCount > avgEntityCount && entityCount <= 2)
                    continue;
                wrongCutResult.getWrongCutMap().put(svc,wrongCutContext.getWrongCutMap().get(svc));
            }
        }
        if(!wrongCutResult.getWrongCutMap().isEmpty())
            wrongCutResult.setStatus(true);
        addNew2ResItem(wrongCutContext,detectionResItem,requestItem);
        return detectionResItem;
    }


}
