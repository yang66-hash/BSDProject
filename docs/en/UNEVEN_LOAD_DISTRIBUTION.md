# Uneven Load Distribution Detection Algorithm and Usage Examples

## 📋 Overview

The uneven load distribution detection algorithm is used to identify resource utilization imbalance issues in microservice instances. The algorithm primarily detects the following anomaly patterns:

**Detection Condition: CPU/RAM usage higher than instance average (1+20%), but request count lower than average (1-20%), latency higher than average (1+20%)**

## 🔧 Algorithm Logic

### 1. Data Collection

The algorithm requires the following two types of metric data:

#### Internal Metrics (SvcMetricsRes)
- **CPU Usage Rate**: `JVMSummaryRes.cpuProPCT` - Process CPU usage percentage
- **Memory Usage**: `JVMSummaryRes.heapUsed` - JVM heap memory usage (bytes)

#### External Metrics (SvcExternalMetricsRes)  
- **Request Count**: `requestCount` - Total number of requests within time interval
- **Average Latency**: `avgLatency` - Average response time for all requests (milliseconds)

### 2. Average Calculation

The algorithm calculates average metrics for all instances:

```java
// CPU average
avgCpuPct = allInstances.stream()
    .mapToDouble(JVMSummaryRes::getCpuProPCT)
    .average()

// Memory average  
avgMemoryUsed = allInstances.stream()
    .mapToLong(JVMSummaryRes::getHeapUsed)
    .average()

// Request count average
avgRequestCount = allInstances.stream()
    .mapToInt(SvcExternalMetricsRes::getRequestCount)
    .average()

// Weighted average latency
avgLatency = sum(latency * requestCount) / totalRequests
```

### 3. Threshold Calculation

Based on averages, calculate detection thresholds (20% tolerance):

```java
// CPU threshold
cpuThreshold = avgCpuPct * 1.2

// Memory threshold  
memoryThreshold = avgMemoryUsed * 1.2

// Request count threshold
requestThreshold = avgRequestCount * 0.8

// Latency threshold
latencyThreshold = avgLatency * 1.2
```

### 4. Anomaly Detection

For each instance, check if it meets the uneven load conditions:

```java
boolean isUnevenLoad = (instanceCpuPct > cpuThreshold) && 
                      (instanceMemoryUsed > memoryThreshold) &&
                      (instanceRequestCount < requestThreshold) &&
                      (instanceLatency > latencyThreshold);
```

## 💡 Algorithm Implementation

### Data Structure

```java
public class UnevenLoadDisItem {
    private Boolean status;                    // Whether uneven load detected
    private Double cpuPct;                    // CPU usage percentage
    private Long memoryUsed;                  // Memory usage (bytes)
    private Integer requestCount;             // Request count
    private Double avgLatency;                // Average latency (ms)
    private Double cpuThreshold;              // CPU threshold
    private Double memoryThreshold;           // Memory threshold
    private Integer requestThreshold;         // Request threshold
    private Double latencyThreshold;          // Latency threshold
    private String detectionReason;           // Detection reason
}
```

### Core Detection Logic

```java
public UnevenLoadDistributionContext detectUnevenLoadDistribution(String serviceName) {
    // 1. Get all service instances
    List<ServiceInstance> instances = getServiceInstances(serviceName);
    
    // 2. Calculate average metrics
    double avgCpu = calculateAverageCpu(instances);
    double avgMemory = calculateAverageMemory(instances);
    int avgRequests = calculateAverageRequests(instances);
    double avgLatency = calculateAverageLatency(instances);
    
    // 3. Calculate thresholds
    double cpuThreshold = avgCpu * 1.2;
    double memoryThreshold = avgMemory * 1.2;
    int requestThreshold = (int)(avgRequests * 0.8);
    double latencyThreshold = avgLatency * 1.2;
    
    // 4. Detect anomalies for each instance
    Map<String, UnevenLoadDisItem> instanceResults = new HashMap<>();
    boolean hasAnomaly = false;
    
    for (ServiceInstance instance : instances) {
        UnevenLoadDisItem item = detectInstanceAnomaly(instance, 
            cpuThreshold, memoryThreshold, requestThreshold, latencyThreshold);
        instanceResults.put(instance.getName(), item);
        
        if (item.getStatus()) {
            hasAnomaly = true;
        }
    }
    
    // 5. Build result context
    return new UnevenLoadDistributionContext(hasAnomaly, instanceResults);
}
```

## 🔍 Detection Conditions

### Main Detection Logic

An instance is considered to have uneven load distribution when **all** of the following conditions are met:

1. **High Resource Usage**
   - CPU usage > Average CPU * 1.2
   - Memory usage > Average Memory * 1.2

2. **Low Request Throughput**
   - Request count < Average Request Count * 0.8

3. **High Latency**
   - Average latency > Average Latency * 1.2

### Business Interpretation

This pattern indicates that an instance is consuming high resources but processing fewer requests with higher latency, suggesting:

- **Resource Waste**: High resource consumption with low efficiency
- **Performance Issues**: Possible bottlenecks or configuration problems
- **Load Imbalance**: Uneven distribution of work across instances

## 📊 Usage Examples

### API Request

```bash
curl -X POST http://localhost:8080/dynamic/uneven-load-distribution \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "user-service",
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

### Response Example

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": true,
    "instanceStatus": {
      "user-service-1": {
        "status": true,
        "cpuPct": 85.5,
        "memoryUsed": 2147483648,
        "requestCount": 120,
        "avgLatency": 450.2,
        "detectionReason": "High resource usage with low throughput"
      },
      "user-service-2": {
        "status": false,
        "cpuPct": 45.2,
        "memoryUsed": 1073741824,
        "requestCount": 280,
        "avgLatency": 120.5,
        "detectionReason": "Normal load distribution"
      }
    }
  }
}
```

### Java Code Example

```java
@RestController
@RequestMapping("/dynamic")
public class DynamicController {

    @Autowired
    private UnevenLoadDistributionService unevenLoadDistributionService;

    @PostMapping("/uneven-load-distribution")
    public ResponseDTO<String> detectUnevenLoadDistribution(@RequestBody RequestItem requestItem) {
        DetectionResItem result = unevenLoadDistributionService.unevenLoadDistributionDetect(requestItem);
        // Process detection result
        return ResponseDTO.success("Detection completed");
    }
}
```

### Processing Detection Results

```java
// Get detection results
UnevenLoadDistributionContext context = (UnevenLoadDistributionContext) result.getContext();

if (context.getStatus()) {
    System.out.println("Uneven load distribution detected!");

    // Iterate through each instance's detection results
    Map<String, UnevenLoadDisItem> instanceStatus = context.getInstanceStatus();
    for (Map.Entry<String, UnevenLoadDisItem> entry : instanceStatus.entrySet()) {
        String instanceName = entry.getKey();
        UnevenLoadDisItem item = entry.getValue();

        if (item.getStatus()) {
            System.out.printf("Instance %s has uneven load distribution:\n", instanceName);
            System.out.printf("  CPU Usage: %.2f%%\n", item.getCpuPct());
            System.out.printf("  Memory Usage: %.0f MB\n", item.getMemoryUsed() / (1024.0 * 1024.0));
            System.out.printf("  Average Latency: %.2f ms\n", item.getAvgLatency());
        }
    }
} else {
    System.out.println("All instances have normal load distribution");
}
```

## ✅ Algorithm Advantages

### 1. **Comprehensive Detection**
- **Multi-dimensional Analysis**: Considers CPU, memory, requests, and latency
- **Instance-Level Detection**: Independent analysis for each instance
- **Comparative Analysis**: Based on relative performance across instances

### 2. **Business-Oriented**
- **Practical Thresholds**: 20% tolerance based on business experience
- **Clear Indicators**: Obvious performance anomaly patterns
- **Actionable Results**: Provides specific optimization directions

### 3. **Real-time Capability**
- **Fast Detection**: Simple arithmetic operations
- **Low Overhead**: Minimal computational requirements
- **Scalable**: Handles multiple service instances efficiently

### 4. **Flexible Configuration**
- **Adjustable Thresholds**: Can modify tolerance percentages
- **Service-Specific**: Different thresholds for different services
- **Context-Aware**: Considers service-specific characteristics

## ❌ Limitations

### 1. **Static Thresholds**
- **Fixed Tolerance**: 20% tolerance may not suit all scenarios
- **No Context**: Does not consider business-specific requirements
- **Seasonal Blind**: May not adapt to seasonal variations

### 2. **Simple Logic**
- **Boolean Detection**: Only detects presence/absence, not severity
- **No Trend Analysis**: Does not consider historical trends
- **Limited Granularity**: Coarse-grained detection

### 3. **Data Dependency**
- **Metric Quality**: Depends on accurate metric collection
- **Time Window**: Requires appropriate time window selection
- **Data Completeness**: Needs complete metric data

## 🔧 Optimization Recommendations

### 1. **Threshold Tuning**
- **Dynamic Thresholds**: Adjust based on historical data
- **Service-Specific**: Different thresholds for different service types
- **Seasonal Adjustment**: Consider seasonal business patterns

### 2. **Enhanced Detection**
- **Severity Levels**: Add severity classification
- **Trend Analysis**: Consider historical performance trends
- **Multi-Window**: Use multiple time windows for validation

### 3. **Integration**
- **Alert Integration**: Connect with monitoring systems
- **Automated Response**: Set up automated scaling responses
- **Dashboard Integration**: Visualize detection results

## 📈 Business Impact

### 1. **Performance Optimization**
- **Resource Efficiency**: Identify and resolve resource waste
- **Load Balancing**: Improve load distribution across instances
- **Cost Reduction**: Optimize resource utilization

### 2. **System Stability**
- **Early Warning**: Detect performance issues early
- **Proactive Management**: Prevent system degradation
- **Reliability Improvement**: Enhance system reliability

### 3. **Operational Efficiency**
- **Automated Detection**: Reduce manual monitoring effort
- **Standardized Process**: Consistent detection across services
- **Data-Driven Decisions**: Make decisions based on metrics

## 📈 Summary

The uneven load distribution detection algorithm provides a simple yet effective way to identify resource utilization imbalances in microservice instances. Through multi-dimensional analysis of CPU, memory, request count, and latency, it can accurately detect instances with poor efficiency and provide actionable optimization insights.

The algorithm's instance-level detection approach ensures precise identification of problematic instances, while the comparative analysis provides context-aware detection results. This makes it an essential tool for maintaining optimal resource utilization and performance in microservice architectures.
