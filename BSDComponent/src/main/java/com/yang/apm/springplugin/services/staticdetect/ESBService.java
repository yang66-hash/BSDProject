package com.yang.apm.springplugin.services.staticdetect;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.ESBServiceContext;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.PathMappingServiceItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.pojo.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import com.yang.apm.springplugin.utils.ESBParserUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: xyc
 * @date: 2023-02-24 20:50
 */
@Service
@Slf4j
public class ESBService implements IDetectConvert {



    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;

    @Autowired
    public ESBParserUtils esbParserUtils;

    public DetectionResItem getESBServices(RequestItem requestItem) {

        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.ESB_USAGE.getValue());
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);

        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);


        QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
        serviceQueryWrapper.eq("service_name", requestItem.getServiceName());

        PathMappingServiceItem mapping = pathMappingService.getOne(serviceQueryWrapper);
        String pathInLocalRepository = mapping.getPathInLocalRepository();

        ESBServiceContext esbServiceContext = new ESBServiceContext();
        log.info("esbServiceContext--"+esbServiceContext);
        esbServiceContext.setResult(esbParserUtils.ESBUsageAnalysis(pathInLocalRepository));
        log.info("esbServiceContext.getResult()"+esbServiceContext.getResult());

        esbServiceContext.setStatus(false);
        if(esbServiceContext.getResult()!=null && esbServiceContext.getResult().keySet() != null) {
            for (String service : esbServiceContext.getResult().keySet()) {
                if (esbServiceContext.getResult().get(service).getIsESBUsage()) {
                    esbServiceContext.setStatus(true);
                    break;
                }
            }
        }

        addNew2ResItem(esbServiceContext,detectionResItem,requestItem);

        return detectionResItem;

    }
}
