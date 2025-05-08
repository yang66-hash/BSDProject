package com.yang.apm.springplugin.services.staticdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.javaparser.ParseException;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.ApiVersionContext;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.utils.ApiParserUtils;
import com.yang.apm.springplugin.factory.FileFactory;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
@NoArgsConstructor
@Slf4j
public class UnVersionedApiService implements IDetectConvert {

    @Autowired
    public FileFactory fileFactory;

    @Autowired
    private PathMappingService pathMappingService;
    @Autowired
    private AntiPatternItemService antiPatternItemService;


    /**
     * @param requestItem
     * 检测单个微服务
     */
    public DetectionResItem getUnVersionedApis(RequestItem requestItem) {
        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.NO_API_VERSIONING.getValue());
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);

        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
        long cur =System.currentTimeMillis();
        //解析对应是哪一个微服务
        QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
        serviceQueryWrapper.eq("service_name", requestItem.getServiceName());
        //获取本地仓库地址
        PathMappingServiceItem mapping = pathMappingService.getOne(serviceQueryWrapper);
        String pathInLocalRepository = mapping.getPathInLocalRepository();

        ApiParserUtils apiParserUtils = new ApiParserUtils();
        ApiVersionContext apiVersionContext = new ApiVersionContext();
        apiVersionContext.getUnversionedMap().put(requestItem.getServiceName(), new HashMap<>());
        apiVersionContext.getMissingUrlMap().put(requestItem.getServiceName(),new HashMap<>());

        List<String> javaFiles = null;
        try {
            javaFiles = fileFactory.getJavaFiles(pathInLocalRepository);
            for (String javafile : javaFiles) {
                File file = new File(javafile);
                apiParserUtils.inspectJavaFile(file, apiVersionContext, requestItem.getServiceName());
            }
        } catch (IOException e) {
            log.error("io exception when detecting no api versioning ...");
            throw new RuntimeException(e);
        } catch (ParseException e) {
            log.error("file parse exception when detecting no api versioning ...");
            throw new RuntimeException(e);
        }

        boolean status =false;
        for(String s: apiVersionContext.getUnversionedMap().keySet()) {
            if(!apiVersionContext.getUnversionedMap().get(s).isEmpty()){
                status=true;
                break;
            }

        }
        apiVersionContext.setStatus(status);

        addNew2ResItem(apiVersionContext,detectionResItem, requestItem);

        return detectionResItem;
    }


}
