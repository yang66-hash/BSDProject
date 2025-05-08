package com.yang.apm.springplugin.controller.staticanalysis;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/overall")
public class OverallController {


    @RequestMapping(path = "/scattered-functionality")
    public String forwardScatteredFunctionality() {

        return "forward:/decomposition/scattered-functionality";
    }

    @RequestMapping(path = "/wrong-cuts")
    public String forwardWrongCuts() {

        return "forward:/decomposition/wrong-cuts";
    }

    @RequestMapping(path = "/microservice-greedy")
    public String forwardMicroserviceGreedy() {
        return "forward:/decomposition/microservice-greedy";
    }

    @RequestMapping(path = "/hardcoded-endpoints")
    public String forwardHardCodeIPAndPort() {
        return "forward:/com-inter/hardcoded-endpoints";
    }


    @RequestMapping(path = "/no-api-versioning")
    public String forwardUnVersionedApis() {
        return "forward:/internal-design/no-api-versioning";
    }

    @RequestMapping(path = "/esb-usage")
    public String forwardESBUsage() {
        return "forward:/struct-infra/esb-usage";
    }

    @RequestMapping(path = "/no-api-gateway")
    public String forwardNoGateWay() {
        return "forward:/struct-infra/no-api-gateway";
    }


    @RequestMapping(path = "/hub-like-dependency")
    public String forwardHubLikeDependency() {
        return "forward:/struct-infra/hub-like-dependency";
    }

    @RequestMapping(path = "/shared-libraries")
    public String forwardSharedLibraries() {
        return "forward:/team-technology/shared-libraries";
    }

}
