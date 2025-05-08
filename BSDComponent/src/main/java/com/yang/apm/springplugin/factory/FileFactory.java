package com.yang.apm.springplugin.factory;

import com.yang.apm.springplugin.base.context.staticres.ServiceContext;
import com.yang.apm.springplugin.pojo.PathMappingServiceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class FileFactory {

    @Value("${mbst.repository.local-repository}")
    private String localRepositoryPath;

    public List<String> getPomFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> pomFiles = new ArrayList<>();
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).contains("pom.xml"));

        pomFiles = stream.sorted().map(String::valueOf).filter(filepath ->{
            if(String.valueOf(filepath).toLowerCase().contains(".mvn") || String.valueOf(filepath).toLowerCase().contains("gradle")){
                return false;
            }
            else {
                return true;
            }
        }).collect(Collectors.toList());
        return  pomFiles;
    }

    /**
     * 匹配服务文件夹下的配置文件路径
     */
    public List<String> getApplicationYamlOrProperties(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> applicationYamlOrProperities;
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> true);
        applicationYamlOrProperities = stream.sorted().map(String::valueOf).filter(filepath ->{
            if((String.valueOf(filepath).toLowerCase().matches("(.*)application(-?.*).yml") ||
                    String.valueOf(filepath).toLowerCase().matches("(.*)application(-?.*).yaml") ||
                    String.valueOf(filepath).toLowerCase().matches("(.*)application(-?.*).properties") ||
                    String.valueOf(filepath).toLowerCase().matches("(.*)bootstrap.yml")) &&
                    !String.valueOf(filepath).toLowerCase().contains(File.separator+"target"+File.separator) &&
                    !String.valueOf(filepath).toLowerCase().contains(File.separator+"test"+File.separator)){
                return true;
            }
            else {
                return false;
            }
        }).collect(Collectors.toList());
        return  applicationYamlOrProperities;
    }

    public boolean isControllerFileExists(String servicesDirectory) throws IOException {
        Path start = Paths.get(servicesDirectory);
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start, maxDepth, (filePath, attributes) -> true);

        return stream
                .map(Path::getFileName)
                .map(Path::toString)
                .anyMatch(fileName -> { return fileName.toLowerCase().contains("controller") || fileName.toLowerCase().contains("web");});
    }

    public List<String> getStaticFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> staticFileList = new ArrayList<>();
        int maxDepth = 15;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> (String.valueOf(filepath).contains("html") ||String.valueOf(filepath).contains("js")));

        staticFileList = stream.sorted().map(String::valueOf).filter(filepath ->{
            if(String.valueOf(filepath).contains("\\resources\\") || String.valueOf(filepath).contains("/resources/")){
                return true;
            }
            else{
                return false;
            }
        }).collect(Collectors.toList());
        return  staticFileList;
    }
    public List<String> getJarFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> jarFiles = new ArrayList<>();
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).contains(".jar"));

        jarFiles = stream.sorted().map(String::valueOf).filter(filepath ->{
            if(String.valueOf(filepath).toLowerCase().contains(".jar.original") || String.valueOf(filepath).toLowerCase().contains("wrapper")){
                return false;
            }
            else {
                return true;
            }
        }).collect(Collectors.toList());
        return  jarFiles;
    }

    public List<String> getJavaFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> javaFiles;
        int maxDepth = 15;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).endsWith(".java"));
        //ignore .java files in package test,but classes outside this package can have "test" or "Test" in their names
        javaFiles= stream.sorted().map(String::valueOf)
                .filter(filepath ->
                            (!String.valueOf(filepath).contains("\\test\\") && !String.valueOf(filepath).contains("/test/"))
                    )
                .collect(Collectors.toList());
        return javaFiles;
    }

    public String getPackageName(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> javaFiles;
        int maxDepth = 10;
        String packageName = "";
        List<String> javaFilePaths = Files.find(start, maxDepth, (filepath, attributes) -> String.valueOf(filepath).endsWith("Application.java"))
                .map(Path::toString)
                .collect(Collectors.toList());
        for(String filepath : javaFilePaths){
            if(filepath.contains("/src/main/java/"))
                packageName = filepath.substring(filepath.indexOf("java/") + 5, filepath.lastIndexOf("/")).replace('/','.');
                break;
        }
        return packageName;
    }

    /**
     * 获取 servicesDirectory下的子文件夹路径 除去.git等文件夹名称
     */
    public List<String> getSubServicePathWithFilter(String servicesDirectory) throws IOException {
        File dir = new File(servicesDirectory);
        File[] files = dir.listFiles();
        List<String> servicesPath = Arrays.stream(files)
                .filter(s -> !s.getName().startsWith("."))
                .filter(s->s.isDirectory())
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        return  servicesPath;
    }


    /**
     * servicePath：具体到具体的子微服务路径
     * 获取其设计的微服务名称，没有的话默认选择文件夹名称
     */
    public  String getServiceName(String servicePath) throws IOException {
        List<String> applicationYamlOrProperties = this.getApplicationYamlOrProperties(servicePath);
        String serviceName = "";
        for (String app : applicationYamlOrProperties) {
            serviceName  = parseYamlOrPropertiesGetSvcName(servicePath,app);
        }
        return serviceName;
    }


    /**
     * 获取servicesDirectory下所有子微服务路径
     */
    public  List<String> getSubServicePaths(String servicesDirectory) throws IOException {
        List<String> servicesPaths = this.getSubServicePathWithFilter(servicesDirectory);
        List<String> servicePathFiltered = new LinkedList<>();
        for (String svc : servicesPaths) {
            List<String> getApplicationYamlOrProperties = this.getApplicationYamlOrProperties(svc);
            if (getApplicationYamlOrProperties.size() != 0){
                servicePathFiltered.add(svc);
            }

        }
        return servicePathFiltered;

    }
    public List<String> getServicesCount(String servicesDirectory) throws IOException {
        File dir = new File(servicesDirectory);
        File[] files = dir.listFiles();
        List<String> servicesPath = new ArrayList<>();
        for(File file: files){
            if(file.isDirectory() && getApplicationYamlOrProperties(file.getAbsolutePath()).size() != 0){
                servicesPath.add(file.getAbsolutePath());
            }
        }
        return  servicesPath;
    }

    /**
     * 获取serviceDirectory下的所有子微服务名称
     */
    public ServiceContext getServiceList(String serviceDirectory) throws IOException {

        ServiceContext serviceContext = new ServiceContext();
        String servicesDirectory = new File(serviceDirectory).getAbsolutePath();
        List<String> servicesPath = this.getSubServicePathWithFilter(servicesDirectory);
        log.info(servicesPath.toString());
        for (String svc : servicesPath) {

            List<String> getApplicationYamlOrProperties = this.getApplicationYamlOrProperties(svc);
            String serviceName = new File(svc).getName();
            if (getApplicationYamlOrProperties.size() == 0)
                continue;
            for (String app : getApplicationYamlOrProperties) {
                serviceName = parseYamlOrPropertiesGetSvcName(svc,app);
            }
            serviceContext.getServiceList().add(serviceName);
        }
        log.info("service list as below: ",serviceContext.getServiceList().toString());
        return serviceContext;
    }


    /**
     * @param servicePath 子微服务绝对路径
     * @param configurationFilePath 其下配置文件的绝对路径
     * 解析获取配置文件中设置的文件名
     */
    private String parseYamlOrPropertiesGetSvcName(String servicePath, String configurationFilePath)  throws IOException {
        String serviceName = "";
        String directoryName = new File(servicePath).getName();
        //先解析yaml yml 文件
        if (configurationFilePath.endsWith("yaml") || configurationFilePath.endsWith("yml") ) {
            Yaml yaml = new Yaml();
            Map map = yaml.load(new FileInputStream(configurationFilePath));
            Optional<Map> springMap = Optional.ofNullable((Map) map.get("spring"));
            Optional<Map> applicationMap = springMap.flatMap(s->Optional.ofNullable((Map) s.get("application")));
            serviceName = applicationMap.map(a->(String) a.get("name")).orElse(directoryName);
        }
        //若是有properties，覆盖上述解析
        if (configurationFilePath.endsWith("properties")){
            InputStream in = new BufferedInputStream(new FileInputStream(configurationFilePath));
            Properties p = new Properties();
            p.load(in);
            if(p.get("spring.application.name")!=null){
                serviceName = (String) p.get("spring.application.name");
            }else serviceName = directoryName;
        }
        return serviceName;
    }

    /**
     * 依据服务名称获取相应的源代码路径
     */
    public List<PathMappingServiceItem>  parsePathAndSvcMapping(String specificReposName){
        log.info("localRepositoryPath: " + localRepositoryPath);
        log.info("specificReposName: " + specificReposName);
        File repository = new File(localRepositoryPath);
        File specificRepos = findFolder(repository,specificReposName);
        if (specificRepos != null) {
            //解析这个文件源代码 获取微服务名-文件夹路径-所属仓库的映射关系
            List<PathMappingServiceItem> pathMappingServiceItems = new ArrayList<>();
            try {
                pathMappingServiceItems = getPathAndSvcMapping(specificRepos);
            } catch (IOException e) {
                log.error(e.getMessage());
                return null;
            }
            return pathMappingServiceItems;
        }


        return null;
    }

    private List<PathMappingServiceItem> getPathAndSvcMapping(File specificRepos) throws IOException {
        List<PathMappingServiceItem> pathMappingServiceItems = new LinkedList<>();

        List<String> servicesPath = this.getSubServicePathWithFilter(specificRepos.getAbsolutePath());

        log.info("servicesPath in getPathAndSvcMapping: "+ servicesPath);
        for (String svc : servicesPath) {

            List<String> getApplicationYamlOrProperties = this.getApplicationYamlOrProperties(svc);
            String serviceName = new File(svc).getName();
            if (getApplicationYamlOrProperties.size() == 0)
                continue;
            for (String app : getApplicationYamlOrProperties) {
                log.info("================================================================");
                serviceName = parseYamlOrPropertiesGetSvcName(svc,app);
            }
            PathMappingServiceItem pathMappingServiceItem = new PathMappingServiceItem();
            pathMappingServiceItem.setServiceName(serviceName);
            pathMappingServiceItem.setPathInLocalRepository(svc);
            pathMappingServiceItem.setRepositoryBelongTo(specificRepos.getName());
            log.info(pathMappingServiceItem.flatItem());
            pathMappingServiceItems.add(pathMappingServiceItem);
        }

        return pathMappingServiceItems;
    }


    /**
     * 查找指定parentFolder文件夹下名为folderName的子文件夹
     */
    private File findFolder(File parentFolder, String folderName){

        File[] subDirectories = parentFolder.listFiles(File::isDirectory);
        if (subDirectories != null) {
            for (File file : subDirectories) {
                if (file.getName().equals(folderName)) {
                    return file;
                }
            }
        }
        return null;
    }


    public static void main(String[] args) {
        FileFactory fileFactory = new FileFactory();

        try {
            ServiceContext context = fileFactory.getServiceList("E:\\javaworkplace\\PropertyManagementCloud");
            System.out.println(context.getServiceList().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }


}
