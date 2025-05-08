package com.yang.apm.springplugin.base.utils;


import com.yang.apm.springplugin.base.context.staticres.TMSContext;
import com.yang.apm.springplugin.base.item.LangDetail;
import com.yang.apm.springplugin.base.item.TMSAnalysisResult;
import kong.unirest.Unirest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TooMuchStandardAnalysisUtils {


    private static String LangAnalysisServer = "172.16.17.37";
    private static String LangAnalysisPort = "3000";

    private static List<String> LangNeedDetect = Arrays.asList("Java","JavaScript","C#","Python","TypeScript","Go","PHP","Shell","Ruby");

    private static Map<String,Double> getLanguages(String url, TMSContext tmsContext) {



        Map<String,Double> reBody = Unirest.get("http://"+LangAnalysisServer+":"+LangAnalysisPort+"/languists/langanalysis")
                .queryString("path",url)
                .asObject(Map.class)
                .getBody();
        tmsContext.setLanguageCount(reBody);
        return reBody;
    }

    private static Map<String, List<String>> getFileList(String url, TMSContext tmsContext){
        Map<String,List<String>> reBody = Unirest.get("http://"+LangAnalysisServer+":"+LangAnalysisPort+"/languists/langfilelist")
                .queryString("path",url)
                .asObject(Map.class)
                .getBody();
        tmsContext.setFileList(reBody);
        return reBody;
    }

    public static void analysisTMSABS(String path, TMSContext tmsContext){

        if (path.equals("")||null == path){
            return ;
        }
        TMSAnalysisResult tmsAnalysisResult = new TMSAnalysisResult();

        //判断这个项目里用到了多少种编程语言，主要检测主流的微服务编程语言（LangNeedDetect列表）
        Map<String,Double> langAndSize = getLanguages(path,tmsContext);
        Map<String,List<String>> langAndFiles = getFileList(path,tmsContext);
        for (Map.Entry<String,Double> entry :
             langAndSize.entrySet()) {
            if (LangNeedDetect.contains(entry.getKey())){
                LangDetail langDetail = new LangDetail();
                Number bytes = entry.getValue();

                langDetail.setBytes(bytes.intValue());
                langDetail.setFilePath(langAndFiles.get(entry.getKey()));
                tmsAnalysisResult.getProgramingLang().put(entry.getKey(),langDetail);
            }
        }

        tmsAnalysisResult.setLangUsedNum(tmsAnalysisResult.getProgramingLang().size());
        tmsAnalysisResult.setProjectName(path.substring(path.lastIndexOf(File.separator)+1));
        if (tmsAnalysisResult.getLangUsedNum()>=2){
            tmsAnalysisResult.setIsMussy(true);
        }
        else {
            tmsAnalysisResult.setIsMussy(false);
        }


        tmsContext.setValueMap(tmsAnalysisResult);
        return ;
    }

}
