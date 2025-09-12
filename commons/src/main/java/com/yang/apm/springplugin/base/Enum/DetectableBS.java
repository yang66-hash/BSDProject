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
    CYCLIC_REFERENCES("Cyclic References"),

    FRAGILE_SERVICE("Fragile Service"),
    UNEVEN_LOAD_DISTRIBUTION("Uneven Load Distribution"),
    INCONSISTENT_SVC_RES("Inconsistent Service Response"),
    RESOURCE_WASTE("Resource Waste"),
    CALL_RATE_ANOMALY("Call Rate Anomaly"),
    UNEVEN_API_USAGE("Uneven API Usage"),
    HIGH_FRE_OF_SLOW_QUERIES("High Frequency Of Slow Queries"),
    N1QUERIES("N+1 Queries"),
    FREQUENT_GC("Frequent GC"),
    LONG_TIME_GC("Long Time GC"),
    MEM_JITTER_OF_SVC("Memory Jitter Of Service"),
    UNEVEN_LOGIC_PRO("Uneven logic processing"),
    FALLING_DOMINOES("Falling Dominoes"),
    UNNECESSARY_PRO("Unnecessary Processing"),
    THE_RAMP("The Ramp"),
    CHATTY_SERVICE("Chatty Service"),
    SERVICE_CHAIN("Service Chain");




    private final String name;

    DetectableBS(String name) {
        this.name = name;
    }

    public String getValue() {
        return name;
    }
}
