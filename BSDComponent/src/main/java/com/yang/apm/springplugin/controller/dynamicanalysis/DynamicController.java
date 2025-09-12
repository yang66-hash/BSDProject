package com.yang.apm.springplugin.controller.dynamicanalysis;


import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.services.DetectionItemBufferService;
import com.yang.apm.springplugin.services.dynamicdetect.FragileServiceService;
import com.yang.apm.springplugin.services.dynamicdetect.UnevenLoadDistributionService;
import com.yang.apm.springplugin.services.dynamicdetect.InconsistentServiceResponseService;
import com.yang.apm.springplugin.services.dynamicdetect.ResourceWasteService;
import com.yang.apm.springplugin.services.dynamicdetect.CallRateAnomalyService;
import com.yang.apm.springplugin.services.dynamicdetect.UnevenApiUsageService;
import com.yang.apm.springplugin.services.dynamicdetect.ChattyServiceService;
import com.yang.apm.springplugin.services.dynamicdetect.ServiceChainService;
import com.yang.apm.springplugin.services.dynamicdetect.HighFreSlowQueriesService;
import com.yang.apm.springplugin.services.dynamicdetect.N1QueriesService;
import com.yang.apm.springplugin.services.dynamicdetect.FrequentGcService;
import com.yang.apm.springplugin.services.dynamicdetect.LongTimeGcService;
import com.yang.apm.springplugin.services.dynamicdetect.MemJitterService;
import com.yang.apm.springplugin.services.dynamicdetect.UnevenLogicProService;
import com.yang.apm.springplugin.services.dynamicdetect.FallingDominoesService;
import com.yang.apm.springplugin.services.dynamicdetect.UnnecessaryProService;
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
    private ResourceWasteService resourceWasteService;

    @Autowired
    private CallRateAnomalyService callRateAnomalyService;

    @Autowired
    private UnevenApiUsageService unevenApiUsageService;

    @Autowired
    private ChattyServiceService chattyServiceService;

    @Autowired
    private ServiceChainService serviceChainService;

    @Autowired
    private HighFreSlowQueriesService highFreSlowQueriesService;

    @Autowired
    private N1QueriesService n1QueriesService;

    @Autowired
    private FrequentGcService frequentGcService;

    @Autowired
    private LongTimeGcService longTimeGcService;

    @Autowired
    private MemJitterService memJitterService;

    @Autowired
    private UnevenLogicProService unevenLogicProService;

    @Autowired
    private FallingDominoesService fallingDominoesService;

    @Autowired
    private UnnecessaryProService unnecessaryProService;

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


    //循环依赖的判断





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

    @RequestMapping(path = "/resource-waste",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Resource Waste")
    public ResponseDTO<String> resourceWaste(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = resourceWasteService.resourceWasteDetect(requestItem);
        log.info("resource waste detection finished for " + requestItem.getServiceName());
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

    @RequestMapping(path = "/chatty-service",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Chatty Service")
    public ResponseDTO<String> chattyService(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = chattyServiceService.chattyServiceDetect(requestItem);
        log.info("chatty service detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/service-chain",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Service Chain")
    public ResponseDTO<String> serviceChain(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = serviceChainService.serviceChainDetect(requestItem);
        log.info("service chain detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
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

    @RequestMapping(path = "/n+1-queries",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————N+1 Queries")
    public ResponseDTO<String> n1Queries(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = n1QueriesService.n1QueriesDetect(requestItem);
        log.info("n+1 queries detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
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

    @RequestMapping(path = "/falling-dominoes",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Falling Dominoes")
    public ResponseDTO<String> fallingDominoes(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = fallingDominoesService.fallingDominoesDetect(requestItem);
        log.info("falling dominoes detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
    }

    @RequestMapping(path = "/unnecessary-processing",
        method = RequestMethod.POST,
        produces = "application/json; charset=UTF-8",
        consumes = {"text/plain", "application/*"})
    @Operation(description = "检测运行时态下异味————Unnecessary Processing")
    public ResponseDTO<String> unnecessaryProcessing(@RequestBody RequestItem requestItem){

        DetectionResItem detectionResItem = unnecessaryProService.unnecessaryProDetect(requestItem);
        log.info("unnecessary processing detection finished for " + requestItem.getServiceName());
        detectionItemBufferService.addResItem(detectionResItem);
        //从es中拉取历史数据和当前数据作分析
        return ResponseDTO.success("Detect command reached.");
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
