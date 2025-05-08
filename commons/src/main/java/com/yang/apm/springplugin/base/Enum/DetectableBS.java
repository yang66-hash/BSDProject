package com.yang.apm.springplugin.base.Enum;

public enum DetectableBS {
    HARDCODED_ENDPOINTS("Hardcoded Endpoints"),
    INAPPROPRIATE_SERVICE_INTIMACY("Inappropriate Service Intimacy"),
    NO_API_VERSIONING("No API Versioning"),
    MEGA_MICROSERVICE("Mega Microservice"),
    ESB_USAGE("ESB Usage"),
    No_API_GATEWAY("No API-Gateway"),
    HUB_LIKE_DEPENDENCY("Hub-like Dependency"),
    MICROSWRVICE_GREEDY("Microservice Greedy"),
    WRONG_CUTS("Wrong Cuts"),
    SCATTERED_FUNCTIONALITY("Scattered Functionality"),
    SHARING_PERSISTENCE("Sharing Persistence"),
    TOO_MANY_STANDARDS("Too Many Standards"),
    SHARED_LIBRARIES("Shared Libraries"),
    CYCLE_DEPENDENCY("Cyclic Dependency"),
    CYCLIC_REFERENCES("Cyclic References");




    private final String name;

    DetectableBS(String name) {
        this.name = name;
    }

    public String getValue() {
        return name;
    }
}
