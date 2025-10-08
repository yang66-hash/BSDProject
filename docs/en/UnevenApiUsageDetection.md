# Uneven API Usage Detection - API Usage Imbalance Detection

## 📋 Bad Smell Definition

**Uneven API Usage (API Usage Imbalance, unified integration detection for all instances under microservice, aggregating all API call counts)** refers to the phenomenon in microservice instances where external clients or internal services have overly concentrated dependence on certain APIs while other APIs remain dormant and unpopular. This uneven usage pattern may lead to performance bottlenecks, security risks, and maintenance difficulties.

### Bad Smell Characteristics
- **Hotspot Concentration**: Few APIs bear most of the call volume
- **Cold APIs**: Many APIs are almost unused or have extremely low usage frequency
- **Distribution Imbalance**: API call distribution severely deviates from uniform distribution
- **Resource Waste**: Cold APIs consume system resources but have limited value

## 🎯 Detection Approach

### Data-Driven Detection Method

Based on **real API call data** for detection, using `instanceAPICallNumMap` statistics within time windows, identifying API usage distribution imbalance patterns through multi-dimensional analysis.

### Core Detection Dimensions

```java
Detection Dimensions:
1. Extreme Concentration Analysis - Detect if single API occupies vast majority of calls (>80%)
2. High Concentration Analysis - Detect if single API is overly concentrated (>60%)
3. Pareto Anomaly Detection - Detect if top 20% APIs occupy too many calls (>95%)
4. Concentration Score - Distribution concentration assessment based on Gini coefficient (>70 points)
```

## 🔍 Detection Algorithm

### Data Source
- **Data Source**: `instanceAPICallNumMap` in AVGExternalMetrics
- **Time Range**: Aggregated data within the entire time window
- **Detection Granularity**: Aggregated detection for the entire microservice (merging API call data from all instances)
- **Important Note**: Only count APIs with call records; APIs with 0 calls will not appear in statistics

### Core Algorithm Flow

```java
1. Data Aggregation and Preprocessing
   └── Aggregate all instances' instanceAPICallNumMap
   └── Accumulate call counts of same API across different instances
   └── Statistics microservice-level total API count, total call count, average call count
   └── Identify hottest API and its call information

2. Concentration Feature Calculation
   └── Hottest API ratio = Hottest API call count / Total call count
   └── Pareto ratio = Top 20% API call count / Total call count
   └── Concentration score = 0-100 score based on Gini coefficient
   └── API distribution details = Detailed distribution of top 10 APIs

3. Multi-dimensional Anomaly Detection
   └── Extreme concentration: Hottest API ratio > 80%
   └── High concentration: Hottest API ratio > 60%
   └── Pareto anomaly: Top 20% API ratio > 95%
   └── Concentration score: Gini coefficient > 70 points

4. Comprehensive Judgment
   └── Any dimension triggers anomaly → Uneven API usage detected
   └── Generate detailed detection results and recommendations
```

## 📊 Detection Metrics

### 1. Hottest API Ratio

**Formula**: `Hottest API Call Count / Total Call Count`

**Business Meaning**:
- **< 30%**: Normal distribution
- **30% - 60%**: Moderate concentration
- **60% - 80%**: High concentration
- **> 80%**: Extreme concentration

### 2. Pareto Ratio (80/20 Rule)

**Formula**: `Top 20% API Call Count / Total Call Count`

**Business Meaning**:
- **< 70%**: Good distribution
- **70% - 85%**: Moderate imbalance
- **85% - 95%**: High imbalance
- **> 95%**: Extreme imbalance

### 3. Gini Coefficient

**Formula**: Complex calculation based on API call distribution

**Business Meaning**:
- **0-30**: Good distribution
- **30-50**: Moderate concentration
- **50-70**: High concentration
- **> 70**: Extreme concentration

## 💡 Algorithm Implementation

### Data Structure

```java
public class UnevenApiUsageItem {
    private Boolean status;                    // Whether uneven API usage detected
    private Integer instanceCount;             // Number of participating instances
    private String instanceNames;              // List of participating instance names
    private Integer totalApiCount;             // Total API count (deduplicated)
    private Integer totalCallCount;            // Total call count (all instances aggregated)
    private Integer avgCallCountPerApi;        // Average call count per API
    private String hottestApi;                 // Hottest API name
    private Integer hottestApiCallCount;       // Hottest API call count
    private Double hottestApiRatio;            // Hottest API ratio
    private Double paretoRatio;                // Pareto ratio
    private Double concentrationScore;         // Concentration score (Gini coefficient)
    private List<ApiDistribution> top10Apis;   // Top 10 API distribution
    private String detectionReason;            // Detection reason
    private String recommendation;             // Optimization recommendation
}
```

### Core Algorithm

```java
public UnevenApiUsageItem detectUnevenApiUsage(String serviceName) {
    // 1. Data aggregation
    Map<String, Integer> aggregatedApiCalls = aggregateAllInstancesApiCalls(serviceName);
    
    // 2. Calculate concentration metrics
    double hottestApiRatio = calculateHottestApiRatio(aggregatedApiCalls);
    double paretoRatio = calculateParetoRatio(aggregatedApiCalls);
    double concentrationScore = calculateGiniCoefficient(aggregatedApiCalls);
    
    // 3. Multi-dimensional anomaly detection
    boolean isAnomaly = hottestApiRatio > 0.8 || hottestApiRatio > 0.6 || 
                       paretoRatio > 0.95 || concentrationScore > 70;
    
    // 4. Generate detection result
    return buildDetectionResult(aggregatedApiCalls, hottestApiRatio, 
                               paretoRatio, concentrationScore, isAnomaly);
}
```

## ✅ Advantages

### 1. **Comprehensive Analysis**
- **Multi-dimensional Detection**: Detects from multiple angles (concentration, Pareto, Gini)
- **Aggregated View**: Provides microservice-level unified analysis
- **Detailed Insights**: Rich detection result information

### 2. **Business-Oriented**
- **Practical Metrics**: Based on real API usage patterns
- **Clear Thresholds**: Business-meaningful detection criteria
- **Actionable Results**: Provides specific optimization recommendations

### 3. **High Accuracy**
- **Real Data**: Based on actual API call statistics
- **Statistical Foundation**: Uses established statistical methods (Pareto, Gini)
- **Comprehensive Coverage**: Considers all APIs with actual usage

### 4. **Scalable Design**
- **Instance Aggregation**: Handles multiple service instances
- **Flexible Thresholds**: Adjustable detection sensitivity
- **Extensible Framework**: Easy to add new detection dimensions

## ❌ Limitations

### 1. **Data Dependency**
- **Data Quality**: Depends on accurate API call statistics
- **Time Window**: Requires sufficient time window for reliable statistics
- **API Coverage**: Only detects APIs with actual call records

### 2. **Static Analysis**
- **Historical Focus**: Based on historical usage patterns
- **Dynamic Changes**: May not quickly adapt to sudden usage pattern changes
- **Business Context**: Does not consider business-specific usage requirements

### 3. **Threshold Sensitivity**
- **Fixed Thresholds**: May not suit all business scenarios
- **Context Ignorance**: Does not consider special business events
- **False Positives**: May misjudge legitimate concentrated usage

## 🎯 Applicable Scenarios

### ✅ Suitable Scenarios

1. **API Design Review**
   - New API design and architecture assessment
   - API usage pattern analysis
   - Performance optimization planning

2. **Service Architecture Optimization**
   - Microservice decomposition assessment
   - API granularity optimization
   - Service boundary adjustment

3. **Performance Monitoring**
   - Long-term API usage trend analysis
   - Performance bottleneck identification
   - Resource allocation optimization

### ❌ Unsuitable Scenarios

1. **Real-time Monitoring**
   - Immediate anomaly detection needs
   - Real-time traffic pattern changes
   - Emergency response scenarios

2. **Business-Specific Usage**
   - APIs with legitimate concentrated usage
   - Business-critical hotspot APIs
   - Special event-driven usage patterns

## 📊 Detection Examples

### Normal Scenario
```
Total APIs: 50
Total Calls: 10000
Hottest API: /api/users (15% of total calls)
Pareto Ratio: 75%
Concentration Score: 45
Result: Normal distribution
```

### Anomaly Scenario: Extreme Concentration
```
Total APIs: 50
Total Calls: 10000
Hottest API: /api/search (85% of total calls)
Pareto Ratio: 98%
Concentration Score: 85
Result: Extreme concentration anomaly
```

### Anomaly Scenario: High Imbalance
```
Total APIs: 100
Total Calls: 20000
Hottest API: /api/products (65% of total calls)
Pareto Ratio: 96%
Concentration Score: 75
Result: High imbalance anomaly
```

## 🔧 Optimization Recommendations

### 1. **API Design Optimization**
- **Split Hotspot APIs**: Decompose overly concentrated APIs
- **Enhance Cold APIs**: Improve APIs with low usage
- **Balance Distribution**: Achieve more uniform API usage

### 2. **Architecture Adjustment**
- **Service Decomposition**: Adjust service boundaries based on API usage
- **Load Balancing**: Distribute traffic more evenly
- **Caching Strategy**: Optimize hotspot API performance

### 3. **Monitoring Enhancement**
- **Usage Tracking**: Continuous monitoring of API usage patterns
- **Trend Analysis**: Regular analysis of usage trends
- **Alert Configuration**: Set up alerts for usage anomalies

## 📈 Business Impact

### 1. **Performance Benefits**
- **Reduced Bottlenecks**: Balanced API usage reduces performance bottlenecks
- **Better Resource Utilization**: More efficient resource allocation
- **Improved Scalability**: Better system scalability

### 2. **Maintenance Benefits**
- **Reduced Complexity**: More balanced API usage reduces maintenance complexity
- **Better Monitoring**: Easier to monitor and manage APIs
- **Improved Reliability**: Reduced single points of failure

### 3. **Security Benefits**
- **Risk Distribution**: Reduced security risks from hotspot concentration
- **Better Access Control**: More granular access control
- **Improved Auditability**: Better audit trail for API usage

## 📈 Summary

Uneven API Usage detection provides a data-driven approach to identify API usage imbalance issues in microservices. Through multi-dimensional analysis including concentration ratios, Pareto principles, and Gini coefficients, it can accurately detect usage distribution anomalies and provide actionable optimization recommendations.

The system's aggregated detection approach provides a holistic view of microservice API usage patterns, while the detailed metrics help identify specific optimization opportunities. This makes it an essential tool for maintaining optimal API design and usage patterns in microservice architectures.
