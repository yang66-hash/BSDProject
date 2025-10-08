package com.yang.apm.springplugin.controller.dynamicanalysis;


import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.services.DetectionItemBufferService;
import com.yang.apm.springplugin.services.dynamicdetect.FragileServiceService;
import com.yang.apm.springplugin.services.dynamicdetect.UnevenLoadDistributionService;
import com.yang.apm.springplugin.services.dynamicdetect.InconsistentServiceResponseService;
import com.yang.apm.springplugin.services.dynamicdetect.CallRateAnomalyService;
import com.yang.apm.springplugin.services.dynamicdetect.UnevenApiUsageService;
import com.yang.apm.springplugin.services.dynamicdetect.HighFreSlowQueriesService;
import com.yang.apm.springplugin.services.dynamicdetect.FrequentGcService;
import com.yang.apm.springplugin.services.dynamicdetect.LongTimeGcService;
import com.yang.apm.springplugin.services.dynamicdetect.MemJitterService;
import com.yang.apm.springplugin.services.dynamicdetect.UnevenLogicProService;
import com.yang.apm.springplugin.services.dynamicdetect.TheRampService;
import com.yang.apm.springplugin.model.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/dynamic")
@Slf4j
public class DynamicController {


    @Autowired
    private FragileServiceService fragileServiceService;

    @Autowired
    private UnevenLoadDistributionService unevenLoadDistributionService;

    @Autowired
    private InconsistentServiceResponseService inconsistentServiceResponseService;


    @Autowired
    private CallRateAnomalyService callRateAnomalyService;

    @Autowired
    private UnevenApiUsageService unevenApiUsageService;

    @Autowired
    private HighFreSlowQueriesService highFreSlowQueriesService;


    @Autowired
    private FrequentGcService frequentGcService;

    @Autowired
    private LongTimeGcService longTimeGcService;

    @Autowired
    private MemJitterService memJitterService;

    @Autowired
    private UnevenLogicProService unevenLogicProService;

    @Autowired
    private TheRampService theRampService;

    @Autowired
    private DetectionItemBufferService detectionItemBufferService;


    //动态检测依旧按照服务名来进行检测，不具体到指定实例进行检测 而是直接全部检测

    @RequestMapping(path = "/fragile-service",
            method = RequestMethod.POST,
            produces = "application/json; charset=UTF-8",
            consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————是否是脆弱服务")
    public ResponseDTO<String> isFragileService(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = fragileServiceService.fragileServiceDetect(requestItem);
        log.info("fragile service detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }


    @RequestMapping(path = "/uneven-load-distribution",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————uneven load distribution")
    public ResponseDTO<String> unevenLoadDistribution(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = unevenLoadDistributionService.unevenLoadDistributionDetect(requestItem);
        log.info("uneven load distribution detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/inconsistent-service-response",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Inconsistent Service Response")
    public ResponseDTO<String> inconsistentServiceResponse(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = inconsistentServiceResponseService.inconsistentSvcResponseDetect(requestItem);
        log.info("inconsistent service response detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/call-rate-anomaly",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Call Rate Anomaly")
    public ResponseDTO<String> callRateAnomaly(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = callRateAnomalyService.callRateAnomalyDetect(requestItem);
        log.info("call rate anomaly detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/uneven-api-usage",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Uneven API Usage")
    public ResponseDTO<String> unevenApiUsage(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = unevenApiUsageService.unevenApiUsageDetect(requestItem);
        log.info("uneven api usage detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    public ResponseDTO<String> chattyService(@RequestBody RequestItem requestItem){
        return ResponseDTO.success("No detection provided.");
    }

    public ResponseDTO<String> serviceChain(@RequestBody RequestItem requestItem){
        return ResponseDTO.success("No detection provided.");
    }


    @RequestMapping(path = "/high-frequency-of-slow-queries",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————High Frequency Of Slow Queries")
    public ResponseDTO<String> highFrequencySlowQueries(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = highFreSlowQueriesService.highFreSlowQueriesDetect(requestItem);
        log.info("high frequency slow queries detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    public ResponseDTO<String> n1Queries(@RequestBody RequestItem requestItem){
        return ResponseDTO.success("No detection provided.");
    }


    @RequestMapping(path = "/frequent-gc",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Frequent GC")
    public ResponseDTO<String> frequentGc(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = frequentGcService.frequentGcDetect(requestItem);
        log.info("frequent gc detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/long-time-gc",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Long Time GC")
    public ResponseDTO<String> longTimeGc(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = longTimeGcService.longTimeGcDetect(requestItem);
        log.info("long time gc detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/memory-jitter-of-service",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Memory Jitter Of Service")
    public ResponseDTO<String> memoryJitter(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = memJitterService.memJitterDetect(requestItem);
        log.info("memory jitter detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/uneven-logic-processing",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Uneven Logic Processing")
    public ResponseDTO<String> unevenLogicProcessing(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = unevenLogicProService.unevenLogicProDetect(requestItem);
        log.info("uneven logic processing detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    public ResponseDTO<String> fallingDominoes(@RequestBody RequestItem requestItem){
        return ResponseDTO.success("No detection provided.");
    }

    public ResponseDTO<String> unnecessaryProcessing(@RequestBody RequestItem requestItem){
        return ResponseDTO.success("No detection provided.");
    }

    @RequestMapping(path = "/the-ramp",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————The Ramp")
    public ResponseDTO<String> theRamp(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = theRampService.theRampDetect(requestItem);
        log.info("the ramp detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }




}
