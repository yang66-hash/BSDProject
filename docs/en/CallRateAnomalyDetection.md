# Call Rate Anomaly Detection - Call Rate Anomaly Detection

## 📋 Bad Smell Definition

**Call Rate Anomaly** refers to the phenomenon where the API call count of a service instance deviates significantly from historical normal levels in a short period of time. When normal call frequency is disturbed, this may indicate potential issues such as request surges, service attacks, or other atypical scenarios.

## 🎯 Detection Approach

### Business-Oriented Simplified Detection Method

Adopts a **business-oriented** detection strategy, avoiding complex statistical assumptions, and establishing simple and effective determination rules based on actual business experience.

### Core Algorithm

```java
Detection Steps:
1. Obtain request count data for the 4 time intervals before the current moment
2. Calculate the average request count of the first 3 time intervals as the historical baseline
3. Compare the latest time interval's request count with the historical average
4. Apply business rules for anomaly determination
```

## 🔍 Detection Rules

### Main Determination Conditions

| Condition | Threshold | Description |
|-----------|-----------|-------------|
| **Surge Multiplier** | ≥ 1.8x | Current request count reaches 1.8 times or more of historical average |
| **Growth Rate** | ≥ 100% | Request count doubling growth directly determined as anomaly |

### Surge Level Classification

| Level | Multiplier Range | Description | Business Meaning |
|-------|------------------|-------------|------------------|
| **NORMAL** | < 1.5x | Normal range | Normal business fluctuation |
| **MILD** | 1.5x - 1.8x | Mild surge | Requires attention but not reaching anomaly threshold |
| **MODERATE** | 1.8x - 2.0x | Moderate surge | Reaches anomaly threshold, requires investigation |
| **HIGH** | 2.0x - 3.0x | High surge | Obvious anomaly, requires immediate handling |
| **EXTREME** | ≥ 3.0x | Extreme surge | Severe anomaly, possibly attack or failure |

## 💡 Algorithm Implementation

### Data Structure

```java
public class CallRateAnomalyItem {
    private Boolean status;                    // Whether anomaly is detected
    private Integer currentRequestCount;       // Current request count
    private Double historicalAvgRequestCount;  // Historical average request count
    private Double growthMultiplier;          // Growth multiplier
    private Double growthPercentage;          // Growth percentage
    private String surgeLevel;                // Surge level
    private String historicalTrend;           // Historical trend
    private String detectionReason;           // Detection reason
    private Integer detectionInterval;        // Detection interval
}
```

### Core Algorithm

```java
private BusinessOrientedDetectionResult performBusinessOrientedDetection(List<Integer> requestCounts) {
    // 1. Data preprocessing
    int currentCount = requestCounts.get(requestCounts.size() - 1);
    List<Integer> historicalCounts = requestCounts.subList(0, requestCounts.size() - 1);
    
    // 2. Calculate historical average
    double historicalAvg = historicalCounts.stream()
        .mapToInt(Integer::intValue)
        .average()
        .orElse(0.0);
    
    // 3. Calculate growth metrics
    double growthMultiplier = currentCount / historicalAvg;
    double growthPercentage = (currentCount - historicalAvg) / historicalAvg * 100;
    
    // 4. Apply business rules
    boolean isAnomaly = growthMultiplier >= 1.8 || growthPercentage >= 100.0;
    
    // 5. Determine surge level and reason
    return buildDetectionResult(currentCount, historicalAvg, growthMultiplier, growthPercentage, isAnomaly);
}
```

## ✅ Advantages

### 1. **Simple and Intuitive**
- **Simple Algorithm**: Avoids complex statistical calculations, easy to understand and implement
- **Clear Rules**: 1.8x and 100% thresholds have clear business meanings
- **Interpretable Results**: Detection results are easy to explain to business personnel

### 2. **Business-Oriented**
- **High Practicality**: Rules established based on actual business experience
- **Reasonable Thresholds**: 1.8x surge is significantly anomalous in most business scenarios
- **Clear Classification**: Multi-level surge severity facilitates priority handling

### 3. **Computationally Efficient**
- **Excellent Performance**: Only requires simple arithmetic operations, minimal computational overhead
- **Good Real-time Performance**: Suitable for real-time monitoring and alerting scenarios
- **Low Resource Consumption**: Very low system resource requirements

### 4. **Robust**
- **Low Data Requirements**: Minimum of 2 data points needed for detection
- **Noise Resistant**: Based on averages, has some resistance to individual outliers
- **Boundary Handling**: Complete handling of boundary cases like zero values, missing values

## ❌ Disadvantages

### 1. **Statistical Limitations**
- **Lack of Theoretical Foundation**: No rigorous statistical theoretical support
- **False Positive Risk**: May misjudge normal business peaks as anomalies
- **Fixed Thresholds**: 1.8x threshold may not apply to all business scenarios

### 2. **Time Feature Ignorance**
- **Periodic Blind Spots**: Cannot identify periodic request patterns (such as daily cycles)
- **Trend Ignorance**: Does not consider long-term growth trends, may misjudge normal growth
- **Seasonality**: Cannot handle seasonal business fluctuations

### 3. **Context Deficiency**
- **Single Dimension**: Only considers request count, ignores other related metrics
- **Business Context**: Does not consider special business events (such as promotional activities)
- **Service Dependencies**: Does not consider upstream and downstream service impacts

### 4. **Precision Limitations**
- **Coarse Granularity**: Based on fixed time intervals, may miss anomalies within short periods
- **Delayed Detection**: Must wait for complete time interval to end before detection
- **Limited History**: Only uses 3 historical data points, limited historical information

## 🎯 Applicable Scenarios

### ✅ Suitable Scenarios

1. **Real-time Monitoring Systems**
   - Scenarios requiring rapid response alerts
   - Environments with high computational performance requirements
   - Simple and effective anomaly detection needs

2. **Stable Business Systems**
   - Services with relatively stable request patterns
   - Businesses without obvious periodic fluctuations
   - Scenarios requiring rapid identification of sudden traffic

3. **Primary Monitoring Needs**
   - Systems just starting to implement monitoring
   - Need for simple and understandable detection methods
   - Scenarios where detection precision requirements are not extremely high

### ❌ Unsuitable Scenarios

1. **Complex Business Patterns**
   - Businesses with obvious daily cycles, weekly cycles
   - Obvious seasonal business fluctuations
   - Systems with frequently changing business patterns

2. **High Precision Requirements**
   - Critical systems requiring extremely low false positive rates
   - Strict detection precision requirements
   - Scenarios requiring consideration of multi-dimensional metrics

3. **Dynamic Environments**
   - Microservices frequently scaling up and down
   - Frequently adjusted business logic
   - Systems with unstable traffic patterns

## 🔧 Optimization Suggestions

### Short-term Improvements

1. **Dynamic Thresholds**: Dynamically adjust 1.8x threshold based on historical data
2. **Time Window Optimization**: Support configurable historical data window sizes
3. **Business Tags**: Combine business tags for classified detection

### Long-term Evolution

1. **Multi-dimensional Fusion**: Combine other metrics like latency, error rates
2. **Machine Learning**: Introduce simple machine learning methods to improve precision
3. **Context Awareness**: Consider impacts of business events and system changes

## 📊 Detection Examples

### Normal Scenario
```
Historical request count: [100, 95, 105] → Average: 100
Current request count: 120
Growth multiplier: 1.2 < 1.8 ✗
Growth rate: 20% < 100% ✗
Result: Normal fluctuation
```

### Anomaly Scenario 1: Surge Detection
```
Historical request count: [100, 95, 105] → Average: 100  
Current request count: 180
Growth multiplier: 1.8 ≥ 1.8 ✓
Result: Moderate surge anomaly
```

### Anomaly Scenario 2: Doubling Detection
```
Historical request count: [50, 55, 45] → Average: 50
Current request count: 120
Growth rate: 140% ≥ 100% ✓
Result: Doubling growth anomaly
```

## 📈 Summary

The business-oriented detection method for Call Rate Anomaly is a **simple and practical** anomaly detection solution. It identifies significant request count anomalies through clear business rules (1.8x surge, 100% growth), with advantages of **computational efficiency, easy understanding, and good real-time performance**.

Although there are limitations in statistical rigor and complex scenario adaptability, for most **real-time monitoring** and **primary anomaly detection** needs, this is a very suitable solution.

It is recommended to adjust threshold parameters according to specific business scenarios when using, and combine with other monitoring means to form a complete anomaly detection system.
