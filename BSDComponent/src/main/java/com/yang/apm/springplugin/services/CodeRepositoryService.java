package com.yang.apm.springplugin.services;


import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.factory.FileFactory;
import com.yang.apm.springplugin.services.dbimpl.IPathMappingService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CodeRepositoryService {


    @Value("${mbst.repository.local-repository}")
    private String localRepositoryPath;

    @Value("${mbst.repository.username}")
    private String remoteRepositoryUsername;

    @Value("${mbst.repository.password}")
    private String remoteRepositoryPassword;

    @Autowired
    private FileFactory fileFactory;

    @Autowired
    private IPathMappingService pathMappingService;

    //fetch pull设置定时任务实时保证本地仓库与远程仓库保持一致

    /**
     * @param localReposPath 本地代码仓库的总地址
     * @param repositoryName 需要检测的仓库名称
     * 检测本地是否存在repositoryName仓库
     */
    public boolean isLocalExistRepos(String localReposPath, String repositoryName){

        File directory = new File(localReposPath);
        if (directory.isDirectory()){
            File[] files = directory.listFiles(File::isDirectory);
            if (files != null && files.length > 0){
                for (File file : files){
                    if (file.getName().equals(repositoryName)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 将远端仓库代码克隆到本地 用于静态检测
     * 举例： https://github.com/yang66-hash/PropertyManagementCloud.git,xxxx,xxxx
     * -1：克隆失败
     * 0： 已经克隆过
     * 1：克隆成功
     */
    public int cloneRemoteRepository(String remoteReposURL){

        String[] repositories = remoteReposURL.split(",");

        for (String repository : repositories){
            String[] remoteReposSplit = repository.split("/");
            String remoteReposName = remoteReposSplit[remoteReposSplit.length - 1].split("\\.")[0];
            log.info("localRepositoryPath in cloneRemoteRepository: "+ localRepositoryPath);
            String path = localRepositoryPath+File.separator+remoteReposName;
            log.info("path:"+path);
            File localPath = new File(path);

            if(!localPath.exists()){
                localPath.mkdir();
            }else {
                deleteDirectoryRecursively(localPath);
                Map<String, Object> map = new HashMap<>();
                map.put("repository_belong_to", remoteReposName);
                pathMappingService.removeByMap(map);
                localPath.mkdir();
            }
            log.info("Clone repository from " + repository + " to " + localRepositoryPath);
            //如果仓库存在则不进行克隆
            File[] subFiles = localPath.listFiles(File::isDirectory);
            if (subFiles != null && subFiles.length > 0){
                for (File subFile : subFiles){
                    if (subFile.getName().equals(remoteReposName)){
                        return 0;
                    }
                }
            }

            //仓库不存在
            try (Git result = Git.cloneRepository()
                    .setURI(repository)
                    .setDirectory(localPath)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(remoteRepositoryUsername, remoteRepositoryPassword))
                    .call()){

                log.info("Having repository: " + result.getRepository().getDirectory());
                List<PathMappingServiceItem> pathMappingServiceItems = fileFactory.parsePathAndSvcMapping(remoteReposName);
                log.info(pathMappingServiceItems.toString());
                boolean saveFlag = pathMappingService.saveBatch(pathMappingServiceItems);
                if (saveFlag){
                    log.info("Mapping between path and service name of "+ remoteReposName + " is saved!");
                }else log.error("Mapping between path and service name of "+ remoteReposName + " save error! May be the number of saved entries is wrong!");
            } catch (GitAPIException e) {
                localPath.delete();
                e.printStackTrace();
                log.error(e.getMessage());
                return -1;
            }
        }


        return 1;
    }

    /**
     * @param localPath
     * 删除localPath文件夹以及其下的所有子内容
     */
    private void deleteDirectoryRecursively(File localPath) {
        if (localPath.exists()) {
            // 如果是目录，则递归删除子文件和子目录
            File[] files = localPath.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 如果是子文件夹，则递归删除
                        deleteDirectoryRecursively(file);
                    } else {
                        // 如果是文件，直接删除
                        file.delete();
                    }
                }
            }
            // 删除空目录
            localPath.delete();
        }
    }


    /**
     * 展示当前微服务系统的所有版本标签
     */
    public List<String> repositoryTagsList(String LocalRepositoryPath){
        List<String> tags = new LinkedList<>();

        return tags;
    }

    /**
     * 切换版本到指定版本
     */
    public boolean checkoutSpecTag(String tag){

        return true;
    }

    public static void main(String[] args) {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
        CodeRepositoryService codeRepositoryService = new CodeRepositoryService();
        codeRepositoryService.cloneRemoteRepository("https://github.com/yang66-hash/PropertyManagementCloud.git");


    }

}
