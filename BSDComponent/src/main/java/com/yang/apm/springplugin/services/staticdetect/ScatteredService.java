package com.yang.apm.springplugin.services.staticdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.ScatteredContext;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.utils.ESBParserUtils;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.yang.apm.springplugin.utils.PathUtil.extractPathPart;

@Service
@Slf4j
public class ScatteredService implements IDetectConvert {

    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;
    @Autowired
    public ESBParserUtils esbParserUtils;

    public DetectionResItem getSFServices(RequestItem requestItem){
        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.SCATTERED_FUNCTIONALITY.getValue());
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

        //同时解析当前子服务父一级下的所有微服务的调用关系
        String path = extractPathPart(pathInLocalRepository);

        ScatteredContext scatteredContext = new ScatteredContext();


        Map<String, HashMap<String,Integer>> callMap =  esbParserUtils.ScatteredAnalysis(path);
        int threshold = 3;
        for(String serviceName: callMap.keySet()){
            HashMap<String,Integer> serviceMap = callMap.get(serviceName);
            for(String calledService: serviceMap.keySet()){
                // if called num > threshold, this two svcs are scattered
                if(serviceMap.get(calledService) > threshold){
                    boolean isExist = false;
                    for(Set<String> scatteredSet: scatteredContext.getScatteredList()){
                        if(scatteredSet.contains(serviceName) && !scatteredSet.contains(calledService)){
                            scatteredSet.add(calledService);
                            isExist = true;
                            break;
                        }
                        else if(!scatteredSet.contains(serviceName) && scatteredSet.contains(calledService)){
                            scatteredSet.add(serviceName);
                            isExist = true;
                            break;
                        }
                        else if(scatteredSet.contains(serviceName) && scatteredSet.contains(calledService)){
                            isExist = true;
                            break;
                        }
                    }
                    // two svcs do not exist in result list
                    if(!isExist){
                        Set<String> scatteredSet = new HashSet<>();
                        scatteredSet.add(serviceName);
                        scatteredSet.add(calledService);
                        scatteredContext.getScatteredList().add(scatteredSet);
                    }

                }
            }
        }
//        Remove duplicates，gather functionality scattered services
        ScatteredContext sc =new ScatteredContext();
        for(Set<String> set: scatteredContext.getScatteredList()){
            boolean isExist= false;
            if(sc.getScatteredList().size() == 0){
                sc.getScatteredList().add(new HashSet<>(set));
                continue;
            }
            for(Set<String> set1: sc.getScatteredList()){
                for(String svc: set){
                    if(set1.contains(svc)){
                        set1.addAll(set);
                        isExist = true;
                        break;
                    }
                }
                if(isExist)
                    break;
            }
            if(!isExist)
                sc.getScatteredList().add(new HashSet<>(set));
        }
        if(!sc.getScatteredList().isEmpty())
            sc.setStatus(true);
        addNew2ResItem(sc,detectionResItem,requestItem);
        return detectionResItem;


    }


}
