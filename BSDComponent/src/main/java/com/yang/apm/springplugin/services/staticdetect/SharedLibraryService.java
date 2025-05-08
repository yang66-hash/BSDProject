package com.yang.apm.springplugin.services.staticdetect;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.SharedLibraryContext;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.base.item.SharedLibraryItem;
import com.yang.apm.springplugin.factory.FileFactory;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Service
@NoArgsConstructor
@Slf4j
public class SharedLibraryService implements IDetectConvert {
    @Autowired
    public FileFactory fileFactory;


    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;


    /**
    * @Description: collect data of sharedLibraries
    *
    */
    public DetectionResItem getSharedLibraries(RequestItem requestItem){

        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.SHARED_LIBRARIES.getValue());
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


        String servicesDirectory = new File(pathInLocalRepository).getAbsolutePath();
        List<String> pomFiles= null;
        try {
            pomFiles = fileFactory.getPomFiles(servicesDirectory);
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            SharedLibraryContext libraryContext = new SharedLibraryContext();
            int num = pomFiles.size();
            for(int i = 0;i < num - 1; i++){
                for(int j = i +1; j<num; j++){
                    Model svc1 = mavenReader.read(new FileReader(pomFiles.get(i)));
                    Model svc2 = mavenReader.read(new FileReader(pomFiles.get(j)));
                    for(Dependency dependency1: svc1.getDependencies()){
                        for(Dependency dependency2:svc2.getDependencies()){
                            if (dependency1.getGroupId().equals(dependency2.getGroupId()) && dependency1.getArtifactId().equals(dependency2.getArtifactId())){
                                String sharedLibrary = "";
                                if(dependency1.getVersion() != null && dependency2.getVersion() != null)
                                    sharedLibrary = dependency1.getGroupId() + "." + dependency1.getArtifactId() +"." + dependency1.getVersion();
                                else
                                    sharedLibrary = dependency1.getGroupId() + "." + dependency1.getArtifactId();
                                if(sharedLibrary.startsWith("org.springframework.boot") || sharedLibrary.startsWith("org.springframework.cloud."))
                                    continue;
                                String services1 = svc1.getGroupId() +"." +svc1.getArtifactId();
                                String services2 = svc2.getGroupId() +"." +svc2.getArtifactId();
                                SharedLibraryItem libraryItem= libraryContext.getSharedLibraries().getOrDefault(sharedLibrary,new SharedLibraryItem(sharedLibrary));
                                libraryItem.addSvc(services1, services2);
                                libraryContext.addItem(libraryItem);
                                break;
                            }
                        }
                    }
                }
            }
            if(!libraryContext.getSharedLibraries().isEmpty())
                libraryContext.setStatus(true);
            addNew2ResItem(libraryContext, detectionResItem, requestItem);
            return detectionResItem;
        }catch (XmlPullParserException e) {
            log.error(e.getMessage());
            log.error("Error occurred when analyzing pom files of " +requestItem.getServiceName());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Some errors occurred when analyzing shared libraries of " +requestItem.getServiceName());
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }


    }
}
