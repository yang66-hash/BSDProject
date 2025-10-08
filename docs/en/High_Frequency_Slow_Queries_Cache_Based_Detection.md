# Cache-Based High Frequency Slow Queries Detection Implementation

## Modification Description

Based on your suggestions, I have changed the data acquisition method from ES to using `CacheService` to get data from local cache, referencing the implementation patterns of `UnevenApiUsageService`, `InconsistentServiceResponseService`, and `CallRateAnomalyService`.

## Data Acquisition Method Comparison

### Before Modification (ES Method)
```java
// Get data from ES
Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, serviceName);
Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, serviceName);
```

### After Modification (Cache Method)
```java
// Get historical data from cache (reference CallRateAnomalyService)
Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = cacheService.getResInServiceLevel(
    ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);

// Get average data (reference UnevenApiUsageService/InconsistentServiceResponseService)
Map<String, SvcExternalMetricsRes> avgExternalMetricsMap = cacheService.getAvgDataInServiceLevel(
    ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);
```

## Core Implementation Improvements

### 1. Data Acquisition Logic
```java
public DetectionResItem highFreSlowQueriesDetect(RequestItem requestItem) {
    // Use CacheService to get cached data, referencing implementation patterns of other services
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();
    
    // Get historical data for statistical analysis (up to 4 historical time intervals)
    Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = cacheService.getResInServiceLevel(
        ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);
    
    // Get average data within time window
    Map<String, SvcExternalMetricsRes> avgExternalMetricsMap = cacheService.getAvgDataInServiceLevel(
        ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);

    // Execute detection
    HighFreSlowQueriesContext context = highFreSlowQueriesDetectDetail(
        historicalExternalMetricsMap, avgExternalMetricsMap, interval);
}
```

### 2. Detection Logic Optimization

#### Historical Data Analysis
```java
// Analyze historical slow query ratios
List<Double> historicalSlowQueryRatios = new ArrayList<>();
for (List<SvcExternalMetricsRes> instanceMetrics : historicalExternalMetricsMap.values()) {
    for (SvcExternalMetricsRes metrics : instanceMetrics) {
        double slowQueryRatio = calculateSlowQueryRatio(metrics);
        historicalSlowQueryRatios.add(slowQueryRatio);
    }
}

// Calculate historical baseline
double historicalAvgSlowQueryRatio = historicalSlowQueryRatios.stream()
    .mapToDouble(Double::doubleValue)
    .average()
    .orElse(0.0);
```

#### Current Data Analysis
```java
// Analyze current slow query ratios
for (Map.Entry<String, SvcExternalMetricsRes> entry : avgExternalMetricsMap.entrySet()) {
    String instanceName = entry.getKey();
    SvcExternalMetricsRes metrics = entry.getValue();
    
    double currentSlowQueryRatio = calculateSlowQueryRatio(metrics);
    int slowQueryCount = getSlowQueryCount(metrics);
    
    // Detection logic
    boolean isAnomaly = detectSlowQueryAnomaly(currentSlowQueryRatio, slowQueryCount, historicalAvgSlowQueryRatio);
    
    if (isAnomaly) {
        // Record anomaly
        recordAnomaly(instanceName, currentSlowQueryRatio, slowQueryCount);
    }
}
```

### 3. Performance Improvements

#### Cache Efficiency
- **Local Cache**: Faster data access compared to ES queries
- **Reduced Network**: Eliminates network latency for data retrieval
- **Batch Processing**: More efficient batch data processing

#### Memory Optimization
- **Data Reuse**: Reuse cached data across multiple detection calls
- **Memory Efficiency**: Reduced memory allocation for data structures
- **Garbage Collection**: Less frequent GC due to reduced object creation

## Detection Algorithm

### Core Detection Logic

```java
private boolean detectSlowQueryAnomaly(double currentRatio, int slowQueryCount, double historicalAvg) {
    // Rule 1: High slow query ratio
    boolean highRatio = currentRatio >= SLOW_QUERY_RATIO_THRESHOLD;
    
    // Rule 2: High slow query count
    boolean highCount = slowQueryCount >= SLOW_QUERY_COUNT_THRESHOLD;
    
    // Rule 3: Significant increase from historical baseline
    boolean significantIncrease = currentRatio > historicalAvg * GROWTH_THRESHOLD;
    
    // Combined detection logic
    return highRatio || (highCount && significantIncrease);
}
```

### Threshold Configuration

| Threshold | Value | Purpose |
|-----------|-------|---------|
| SLOW_QUERY_RATIO_THRESHOLD | 20% | High slow query ratio detection |
| SLOW_QUERY_COUNT_THRESHOLD | 10 | High slow query count detection |
| GROWTH_THRESHOLD | 1.5x | Significant growth from baseline |

## Advantages of Cache-Based Approach

### 1. **Performance Benefits**
- **Faster Response**: Local cache access is significantly faster than ES queries
- **Reduced Latency**: Eliminates network round-trip time
- **Higher Throughput**: Can handle more concurrent detection requests

### 2. **Reliability Improvements**
- **Reduced Dependencies**: Less dependent on ES availability
- **Better Error Handling**: More predictable error scenarios
- **Consistent Performance**: More consistent response times

### 3. **Resource Efficiency**
- **Lower CPU Usage**: Reduced processing overhead
- **Memory Optimization**: More efficient memory usage patterns
- **Network Savings**: Reduced network bandwidth consumption

### 4. **Scalability**
- **Horizontal Scaling**: Better support for multiple service instances
- **Load Distribution**: More even load distribution across services
- **Capacity Planning**: More predictable resource requirements

## Implementation Details

### Cache Service Integration

```java
@Service
public class HighFreSlowQueriesService {
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private IntervalWindowMappingService intervalWindowMappingService;
    
    public DetectionResItem highFreSlowQueriesDetect(RequestItem requestItem) {
        // Get interval configuration
        Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
        
        // Build service keys
        String serviceInternal = requestItem.getServiceName() + "|" + interval;
        String serviceIntervalAVG = requestItem.getServiceName();
        
        // Get data from cache
        Map<String, List<SvcExternalMetricsRes>> historicalData = cacheService.getResInServiceLevel(
            ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);
        
        Map<String, SvcExternalMetricsRes> avgData = cacheService.getAvgDataInServiceLevel(
            ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);
        
        // Perform detection
        return performDetection(historicalData, avgData, requestItem);
    }
}
```

### Data Processing Pipeline

```java
private HighFreSlowQueriesContext processData(
    Map<String, List<SvcExternalMetricsRes>> historicalData,
    Map<String, SvcExternalMetricsRes> avgData) {
    
    // 1. Process historical data
    List<Double> historicalRatios = extractSlowQueryRatios(historicalData);
    double baseline = calculateBaseline(historicalRatios);
    
    // 2. Process current data
    Map<String, HighFreSlowQueriesItem> results = new HashMap<>();
    for (Map.Entry<String, SvcExternalMetricsRes> entry : avgData.entrySet()) {
        String instanceName = entry.getKey();
        SvcExternalMetricsRes metrics = entry.getValue();
        
        HighFreSlowQueriesItem item = analyzeInstance(metrics, baseline);
        results.put(instanceName, item);
    }
    
    // 3. Build context
    boolean hasAnomaly = results.values().stream().anyMatch(HighFreSlowQueriesItem::getStatus);
    return new HighFreSlowQueriesContext(hasAnomaly, results);
}
```

## Migration Benefits

### 1. **Consistency**
- **Unified Pattern**: Follows the same pattern as other detection services
- **Code Reuse**: Leverages existing cache service infrastructure
- **Maintainability**: Easier to maintain and debug

### 2. **Performance**
- **Faster Execution**: Significantly improved response times
- **Resource Efficiency**: Better resource utilization
- **Scalability**: Better support for high-volume scenarios

### 3. **Reliability**
- **Reduced Failures**: Lower probability of detection failures
- **Better Error Handling**: More predictable error scenarios
- **Consistent Behavior**: More consistent detection behavior

## Summary

The migration to cache-based data acquisition for high frequency slow queries detection provides significant improvements in performance, reliability, and maintainability. By leveraging the existing cache service infrastructure and following established patterns, the system now provides faster, more reliable detection capabilities while maintaining the same detection accuracy.

The unified approach with other detection services ensures consistency across the entire detection system, making it easier to maintain and extend in the future.
