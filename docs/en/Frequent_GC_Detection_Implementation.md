# Four-Dimensional Frequent GC Detection Implementation

## Implementation Overview

This implementation uses a **four-dimensional comprehensive detection** approach to detect frequent GC issues in services, based on real JVM monitoring data, using `INTERNAL_METRICS` as the data source, ensuring **independent detection for each instance of each service**.

## Core Design Principles

### 1. Instance-Level Detection
- **Independence**: Each service instance independently performs GC detection analysis
- **Precision**: Judgment based on the instance's own historical GC data
- **Flexibility**: Different instances may have different GC detection results

### 2. Unified Data Source
- **Cache Priority**: Uses `CacheService` to obtain locally cached JVM data
- **Historical Analysis**: Based on historical GC data from multiple time intervals
- **Data Accuracy**: Uses `INTERNAL_METRICS` as JVM data source

## Four-Dimensional GC Detection System

This frequent GC detection system uses a **four-dimensional comprehensive detection** approach, comprehensively evaluating instance GC conditions from different angles:

### Detection Dimension Overview

| Dimension | Detection Target | Core Metrics | Threshold | Purpose |
|-----------|------------------|--------------|-----------|---------|
| **Minor GC Time Dimension** | Minor GC time anomaly | Current avg time vs historical avg | Growth >20% | Identify Young area collection anomalies |
| **Minor GC Frequency Dimension** | Minor GC frequency anomaly | Minor GC count/time | ≥10 times/minute | Identify Young area allocation pressure |
| **Major GC Time Dimension** | Major GC time anomaly | Current avg time vs historical avg | Growth >20% | Identify Old area collection anomalies |
| **Major GC Frequency Dimension** | Major GC frequency anomaly | Major GC count/time | ≥2 times/minute | Identify Old area promotion pressure |

### Detection Dimension Relationship Diagram

```
                    Frequent GC Detection
                            |
        ┌──────────────────┼──────────────────┐
        |                  |                  |
   Minor GC Detection   Minor GC          Major GC Detection
     Time Dimension    Frequency Dimension    Time Dimension
        |                  |                  |
        └──────────────────┼──────────────────┘
                            |
                     Major GC Frequency
                         Dimension
                            |
                        Comprehensive
                       Determination
                    (Any dimension anomaly)
```

## Detailed Detection Implementation

### 1. Minor GC Time Dimension Detection

**Detection Target**: Minor GC time anomaly
**Core Logic**: Compare current Minor GC average time with historical average
**Threshold**: Current time > Historical average * 1.2 (20% increase)

```java
// Minor GC time detection logic
if (currentMinorGCAvgTime > historicalMinorGCAvgTime * 1.2) {
    // Minor GC time anomaly detected
    minorGCTimeAnomaly = true;
    anomalyReasons.add("Minor GC time increased by " + 
        ((currentMinorGCAvgTime - historicalMinorGCAvgTime) / historicalMinorGCAvgTime * 100) + "%");
}
```

**Business Significance**:
- **Normal**: Young area objects can be quickly recycled
- **Anomaly**: Young area collection efficiency decreased, possible memory allocation issues

### 2. Minor GC Frequency Dimension Detection

**Detection Target**: Minor GC frequency anomaly  
**Core Logic**: Calculate Minor GC frequency per minute
**Threshold**: Minor GC count / time interval ≥ 10 times/minute

```java
// Minor GC frequency detection logic
double minorGCFrequency = minorGCCount / (timeIntervalMinutes);
if (minorGCFrequency >= 10.0) {
    // Minor GC frequency anomaly detected
    minorGCFrequencyAnomaly = true;
    anomalyReasons.add("Minor GC frequency: " + minorGCFrequency + " times/minute (threshold: 10)");
}
```

**Business Significance**:
- **Normal**: Object allocation and recycling balanced
- **Anomaly**: High object allocation rate, possible memory leaks or excessive object creation

### 3. Major GC Time Dimension Detection

**Detection Target**: Major GC time anomaly
**Core Logic**: Compare current Major GC average time with historical average  
**Threshold**: Current time > Historical average * 1.2 (20% increase)

```java
// Major GC time detection logic
if (currentMajorGCAvgTime > historicalMajorGCAvgTime * 1.2) {
    // Major GC time anomaly detected
    majorGCTimeAnomaly = true;
    anomalyReasons.add("Major GC time increased by " + 
        ((currentMajorGCAvgTime - historicalMajorGCAvgTime) / historicalMajorGCAvgTime * 100) + "%");
}
```

**Business Significance**:
- **Normal**: Old area objects can be efficiently recycled
- **Anomaly**: Old area collection efficiency decreased, possible long-lived object accumulation

### 4. Major GC Frequency Dimension Detection

**Detection Target**: Major GC frequency anomaly
**Core Logic**: Calculate Major GC frequency per minute
**Threshold**: Major GC count / time interval ≥ 2 times/minute

```java
// Major GC frequency detection logic
double majorGCFrequency = majorGCCount / (timeIntervalMinutes);
if (majorGCFrequency >= 2.0) {
    // Major GC frequency anomaly detected
    majorGCFrequencyAnomaly = true;
    anomalyReasons.add("Major GC frequency: " + majorGCFrequency + " times/minute (threshold: 2)");
}
```

**Business Significance**:
- **Normal**: Old area object promotion rate moderate
- **Anomaly**: High object promotion rate, possible memory leaks or inappropriate object lifecycle

## Comprehensive Detection Logic

### Final Determination Rule

```java
// Comprehensive detection result
boolean hasFrequentGC = minorGCTimeAnomaly || minorGCFrequencyAnomaly || 
                       majorGCTimeAnomaly || majorGCFrequencyAnomaly;

if (hasFrequentGC) {
    // Frequent GC detected
    result.setStatus(true);
    result.setDetectionReasons(anomalyReasons);
    result.setSeverityLevel(calculateSeverityLevel());
} else {
    // No frequent GC detected
    result.setStatus(false);
}
```

### Severity Level Calculation

| Anomaly Count | Severity Level | Description |
|---------------|----------------|-------------|
| 1 | LOW | Single dimension anomaly, requires attention |
| 2 | MEDIUM | Multiple dimension anomalies, needs investigation |
| 3-4 | HIGH | Comprehensive anomalies, requires immediate handling |

## Data Structure

### Detection Result Item

```java
public class FrequentGCItem {
    private Boolean status;                    // Whether frequent GC is detected
    private String severityLevel;             // Severity level (LOW/MEDIUM/HIGH)
    private List<String> anomalyReasons;      // Detailed anomaly reasons
    private Double minorGCAvgTime;            // Minor GC average time
    private Double majorGCAvgTime;            // Major GC average time
    private Integer minorGCCount;             // Minor GC count
    private Integer majorGCCount;             // Major GC count
    private Double minorGCFrequency;          // Minor GC frequency
    private Double majorGCFrequency;          // Major GC frequency
    private String detectionInterval;         // Detection time interval
}
```

## Advantages of Four-Dimensional Detection

### 1. **Comprehensive Coverage**
- **Multi-angle Analysis**: Detects GC issues from time and frequency dimensions
- **Complete Coverage**: Covers both Young and Old area GC anomalies
- **Holistic View**: Provides comprehensive GC health assessment

### 2. **High Precision**
- **Historical Comparison**: Based on historical baseline for accurate detection
- **Instance-Level**: Independent detection for each instance
- **Quantified Thresholds**: Clear and measurable detection criteria

### 3. **Practical Business Value**
- **Performance Impact**: Directly relates to application performance
- **Actionable Insights**: Provides specific optimization directions
- **Early Warning**: Early detection of potential memory issues

### 4. **Flexible Configuration**
- **Adjustable Thresholds**: Can adjust detection sensitivity according to business needs
- **Severity Classification**: Different severity levels for different handling priorities
- **Detailed Reporting**: Rich anomaly reason information

## Usage Examples

### Detection Request

```bash
curl -X POST http://localhost:8080/dynamic/frequent-gc \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "user-service",
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

### Detection Response

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": true,
    "severityLevel": "MEDIUM",
    "anomalyReasons": [
      "Minor GC frequency: 12.5 times/minute (threshold: 10)",
      "Major GC time increased by 25%"
    ],
    "minorGCAvgTime": 15.2,
    "majorGCAvgTime": 120.5,
    "minorGCCount": 125,
    "majorGCCount": 8,
    "minorGCFrequency": 12.5,
    "majorGCFrequency": 0.8,
    "detectionInterval": "10 minutes"
  }
}
```

## Optimization Recommendations

### 1. **Threshold Tuning**
- Adjust detection thresholds based on actual business scenarios
- Consider different thresholds for different service types
- Regular review and optimization of threshold settings

### 2. **Historical Data Management**
- Maintain sufficient historical data for accurate baseline calculation
- Consider seasonal and business cycle factors
- Regular data cleaning and archiving

### 3. **Integration with Monitoring**
- Integrate with existing monitoring and alerting systems
- Set up automated responses for different severity levels
- Regular analysis and reporting of detection results

### 4. **Performance Optimization**
- Optimize data query and calculation performance
- Consider caching strategies for frequently accessed data
- Monitor detection system's own resource consumption

## Summary

The four-dimensional frequent GC detection system provides a comprehensive and practical solution for identifying GC-related performance issues in microservices. Through multi-dimensional analysis and historical comparison, it can accurately detect various GC anomalies and provide actionable optimization insights.

The system's instance-level detection approach ensures precise identification of problematic instances, while the severity classification helps prioritize handling efforts. This makes it an essential tool for maintaining optimal performance in microservice architectures.
