package com.yang.apm.springplugin.services.staticdetect;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.Enum.GateWayType;
import com.yang.apm.springplugin.base.context.staticres.GateWayContext;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
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
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;


@Service
@NoArgsConstructor
@Slf4j
public class NoGateWayService implements IDetectConvert {
    @Autowired
    public FileFactory fileFactory;

    @Autowired
    private PathMappingService pathMappingService;

    @Autowired
    private AntiPatternItemService antiPatternItemService;
    
    public DetectionResItem isExistGateWay(RequestItem request){

        QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", DetectableBS.No_API_GATEWAY.getValue());
        //相关异味信息
        AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
        DetectionResItem detectionResItem = convertToResItem(antiPatternItem);
        long cur =System.currentTimeMillis();
        //解析对应是哪一个微服务
        QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
        serviceQueryWrapper.eq("service_name", request.getServiceName());
        //获取本地仓库地址
        PathMappingServiceItem mapping = pathMappingService.getOne(serviceQueryWrapper);
        String pathInLocalRepository = mapping.getPathInLocalRepository();
        log.info(pathInLocalRepository);

        String servicesDirectory = new File(pathInLocalRepository).getAbsolutePath();


        try {

            List<String> pomFiles = fileFactory.getPomFiles(servicesDirectory);
            GateWayContext gateWayContext = new GateWayContext();
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            boolean hasDependency = false;
            boolean hasProperity = false;
            Yaml yaml = new Yaml();
            for (String pom: pomFiles) {
                System.out.println("POM"+pom);
                Model svc1 = mavenReader.read(new FileReader(pom));
                for (Dependency dependency1 : svc1.getDependencies()) {
                    System.out.println("TEST"+dependency1.toString());
                    if (dependency1.getGroupId().equals("org.springframework.cloud") && dependency1.getArtifactId().equals("spring-cloud-starter-gateway")) {
                        gateWayContext.setType(GateWayType.SpringCloudGateWay);
                        hasDependency = true;

                    } else if (dependency1.getGroupId().equals("org.springframework.cloud") && dependency1.getArtifactId().equals("spring-cloud-starter-netflix-zuul")) {
                        gateWayContext.setType(GateWayType.Zuul);
                        hasDependency = true;
                    }
                    if (hasDependency) {
                        break;
                    }
                }
                if(hasDependency){
                    break;
                }
            }
            List<String> applicationYamlOrProperties = fileFactory.getApplicationYamlOrProperties(servicesDirectory);
            Map gateway = null;
            Map zuul = null;
            for (String app : applicationYamlOrProperties) {

                if (app.endsWith("yaml") || app.endsWith("yml")) {
                    Map map = yaml.load(new FileInputStream(app));
                    Optional<Map> springMap = Optional.ofNullable((Map) map.get("spring"));
                    Optional<Map> cloudMap = springMap.flatMap(s->Optional.ofNullable((Map) s.get("cloud")));
                    String s = cloudMap.map(a -> (String) a.get("gateway")).orElse(null);

                    if(s != null || map.get("zuul") != null){
                        hasProperity = true;
                        break;
                    }
                } else {
                    InputStream in = new BufferedInputStream(new FileInputStream(app));
                    Properties p = new Properties();
                    p.load(in);
                    for(String key: p.stringPropertyNames()){
                        if(key.contains("spring.cloud.gateway") || key.contains("zuul.routes")){
                            hasProperity = true;
                            break;
                        }
                    }

                }

            }
            if(hasDependency || hasProperity){
                gateWayContext.setHasGateWay(true);
                gateWayContext.setStatus(false);
            }
            else {
                gateWayContext.setType(null);
            }
            addNew2ResItem(gateWayContext, detectionResItem, request);
        } catch (XmlPullParserException e) {
            log.error("parse pom file error");
            throw new RuntimeException(e);
        }catch (FileNotFoundException e) {
            log.error("Pom files not found.");
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("some unknown errors occurred when detecting No Gate Way Bad Smell.");
            throw new RuntimeException(e);
        }

        return detectionResItem;

    }

}
