package com.yang.apm.springplugin.services.staticdetect;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.TMSContext;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.utils.TooMuchStandardAnalysisUtils;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TooMuchStandardsService implements IDetectConvert {

    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;

    public DetectionResItem getTMSServices(RequestItem requestItem){
        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.TOO_MANY_STANDARDS.getValue());
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
        TMSContext tmsContext = new TMSContext();
        TooMuchStandardAnalysisUtils.analysisTMSABS(pathInLocalRepository, tmsContext);
        if(tmsContext.getValueMap().getIsMussy())
            tmsContext.setStatus(true);
        addNew2ResItem(tmsContext,detectionResItem,requestItem);
        return detectionResItem;


    }
}
