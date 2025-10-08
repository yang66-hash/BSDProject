# Instance-Based High Frequency Slow Queries Detection Implementation

## Implementation Overview

This implementation redesigns the high frequency slow queries detection logic to ensure **independent detection for each instance of each service**, referencing the mature pattern of `CallRateAnomalyService` and adopting a simplified but effective detection algorithm.

## Core Design Principles

### 1. Instance-Level Detection
- **Independence**: Each service instance independently performs detection analysis
- **Precision**: Judgment based on the instance's own historical data
- **Flexibility**: Different instances may have different detection results

### 2. Unified Data Source
- **Cache Priority**: Uses `CacheService` to obtain locally cached data
- **Historical Analysis**: Based on historical data from multiple time intervals
- **Real-time Performance**: Ensures data timeliness and accuracy
- **Data Purity**: Uses only SQL-related core metrics, not involving mixed metrics like request latency

## Four-Dimensional Detection System

This high frequency slow queries detection system uses a **four-dimensional comprehensive detection** approach to comprehensively evaluate instance slow query conditions from different angles:

### Detection Dimension Overview

| Dimension | Detection Target | Core Metrics | Threshold | Purpose |
|-----------|------------------|--------------|-----------|---------|
| **Ratio Dimension** | Current slow query ratio | Slow query ratio | ≥20% | Identify ratio anomalies |
| **Frequency Dimension** | Slow query absolute frequency | Count/time | ≥10 times/minute | Identify frequency anomalies |
| **Trend Dimension** | Historical change trend | Trend growth multiplier | >1.5x and >10% | Identify deterioration trends |
| **Location Dimension** | Specific problematic SQL | SQL fingerprint analysis | Slow query ratio >50% and count ≥3 | Precise problem location |

### Detection Dimension Relationship Diagram

```
                    High Frequency Slow Queries Detection
                                |
        ┌──────────────────────┼──────────────────────┐
        |                      |                      |
    Ratio Dimension        Frequency Dimension     Trend Dimension
   (Current State)        (Absolute Count)        (Historical Change)
        |                      |                      |
        └──────────────────────┼──────────────────────┘
                                |
                        Location Dimension
                      (Problem SQL Identification)
                                |
                        Comprehensive
                       Determination
                    (Any dimension anomaly)
```

## Detailed Detection Implementation

### 1. Ratio Dimension Detection

**Detection Target**: Current slow query ratio anomaly
**Core Logic**: Compare current slow query ratio with threshold
**Threshold**: Current ratio ≥ 20%

```java
// Ratio dimension detection logic
double currentSlowQueryRatio = (double) slowQueryCount / totalQueryCount;
if (currentSlowQueryRatio >= SLOW_QUERY_RATIO_THRESHOLD) {
    // Ratio anomaly detected
    ratioAnomaly = true;
    anomalyReasons.add("Slow query ratio: " + String.format("%.1f%%", currentSlowQueryRatio * 100) + 
                      " (threshold: 20%)");
}
```

**Business Significance**:
- **Normal**: Most queries execute efficiently
- **Anomaly**: High proportion of slow queries, possible performance issues

### 2. Frequency Dimension Detection

**Detection Target**: Slow query absolute frequency anomaly
**Core Logic**: Calculate slow query frequency per minute
**Threshold**: Slow query count / time interval ≥ 10 times/minute

```java
// Frequency dimension detection logic
double slowQueryFrequency = slowQueryCount / (timeIntervalMinutes);
if (slowQueryFrequency >= SLOW_QUERY_FREQUENCY_THRESHOLD) {
    // Frequency anomaly detected
    frequencyAnomaly = true;
    anomalyReasons.add("Slow query frequency: " + String.format("%.1f", slowQueryFrequency) + 
                      " times/minute (threshold: 10)");
}
```

**Business Significance**:
- **Normal**: Occasional slow queries within acceptable range
- **Anomaly**: Frequent slow queries, possible systematic performance issues

### 3. Trend Dimension Detection

**Detection Target**: Historical change trend anomaly
**Core Logic**: Compare current slow query ratio with historical average
**Threshold**: Current ratio > Historical average * 1.5 AND growth > 10%

```java
// Trend dimension detection logic
double historicalAvgSlowQueryRatio = calculateHistoricalAverage(historicalRatios);
double growthMultiplier = currentSlowQueryRatio / historicalAvgSlowQueryRatio;
double growthRate = (currentSlowQueryRatio - historicalAvgSlowQueryRatio) / historicalAvgSlowQueryRatio;

if (growthMultiplier > TREND_GROWTH_MULTIPLIER && growthRate > TREND_GROWTH_RATE) {
    // Trend anomaly detected
    trendAnomaly = true;
    anomalyReasons.add("Slow query trend deterioration: " + String.format("%.1f%%", growthRate * 100) + 
                      " increase from historical average");
}
```

**Business Significance**:
- **Normal**: Slow query performance stable or improving
- **Anomaly**: Slow query performance deteriorating, needs attention

### 4. Location Dimension Detection

**Detection Target**: Specific problematic SQL identification
**Core Logic**: Analyze SQL fingerprints to identify problematic queries
**Threshold**: Slow query ratio > 50% AND count ≥ 3

```java
// Location dimension detection logic
Map<String, Integer> sqlFingerprintCount = analyzeSqlFingerprints(sqlQueries);
for (Map.Entry<String, Integer> entry : sqlFingerprintCount.entrySet()) {
    String sqlFingerprint = entry.getKey();
    Integer count = entry.getValue();
    double sqlSlowQueryRatio = (double) count / totalQueryCount;
    
    if (sqlSlowQueryRatio > LOCATION_RATIO_THRESHOLD && count >= LOCATION_COUNT_THRESHOLD) {
        // Problematic SQL detected
        problematicSqls.add(sqlFingerprint + " (" + count + " times, " + 
                           String.format("%.1f%%", sqlSlowQueryRatio * 100) + ")");
        locationAnomaly = true;
    }
}
```

**Business Significance**:
- **Normal**: No specific SQL causing performance issues
- **Anomaly**: Specific SQL queries causing performance problems, needs optimization

## Comprehensive Detection Logic

### Final Determination Rule

```java
// Comprehensive detection result
boolean hasSlowQueryIssue = ratioAnomaly || frequencyAnomaly || trendAnomaly || locationAnomaly;

if (hasSlowQueryIssue) {
    // High frequency slow queries detected
    result.setStatus(true);
    result.setDetectionReasons(anomalyReasons);
    result.setSeverityLevel(calculateSeverityLevel());
    result.setProblematicSqls(problematicSqls);
} else {
    // No slow query issues detected
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
public class HighFreSlowQueriesItem {
    private Boolean status;                    // Whether high frequency slow queries detected
    private String severityLevel;             // Severity level (LOW/MEDIUM/HIGH)
    private List<String> detectionReasons;     // Detailed detection reasons
    private Double slowQueryRatio;            // Current slow query ratio
    private Integer slowQueryCount;           // Slow query count
    private Integer totalQueryCount;          // Total query count
    private Double slowQueryFrequency;        // Slow query frequency per minute
    private Double historicalAvgRatio;        // Historical average slow query ratio
    private Double trendGrowthRate;           // Trend growth rate
    private List<String> problematicSqls;     // Problematic SQL fingerprints
    private String detectionInterval;         // Detection time interval
}
```

## Advantages of Four-Dimensional Detection

### 1. **Comprehensive Coverage**
- **Multi-angle Analysis**: Detects slow query issues from ratio, frequency, trend, and location dimensions
- **Complete Assessment**: Provides comprehensive slow query health evaluation
- **Holistic View**: Considers both current state and historical trends

### 2. **High Precision**
- **Instance-Level**: Independent detection for each instance
- **Historical Comparison**: Based on historical baseline for accurate detection
- **Quantified Thresholds**: Clear and measurable detection criteria

### 3. **Practical Business Value**
- **Performance Impact**: Directly relates to database performance
- **Actionable Insights**: Provides specific optimization directions
- **Early Warning**: Early detection of potential database performance issues

### 4. **Flexible Configuration**
- **Adjustable Thresholds**: Can adjust detection sensitivity according to business needs
- **Severity Classification**: Different severity levels for different handling priorities
- **Detailed Reporting**: Rich anomaly reason and SQL location information

## Usage Examples

### Detection Request

```bash
curl -X POST http://localhost:8080/dynamic/high-frequency-slow-queries \
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
    "detectionReasons": [
      "Slow query ratio: 25.5% (threshold: 20%)",
      "Slow query frequency: 12.3 times/minute (threshold: 10)"
    ],
    "slowQueryRatio": 0.255,
    "slowQueryCount": 128,
    "totalQueryCount": 502,
    "slowQueryFrequency": 12.3,
    "historicalAvgRatio": 0.15,
    "trendGrowthRate": 0.7,
    "problematicSqls": [
      "SELECT * FROM users WHERE name LIKE '%search%' (45 times, 8.9%)",
      "SELECT * FROM orders WHERE status = 'pending' ORDER BY created_at (32 times, 6.4%)"
    ],
    "detectionInterval": "10 minutes"
  }
}
```

## Optimization Recommendations

### 1. **SQL Optimization**
- **Query Optimization**: Optimize slow SQL queries identified by the system
- **Index Optimization**: Add appropriate indexes for frequently queried columns
- **Query Rewriting**: Rewrite complex queries to improve performance

### 2. **Database Configuration**
- **Connection Pool**: Optimize database connection pool settings
- **Query Cache**: Enable and optimize query caching
- **Buffer Settings**: Adjust database buffer settings

### 3. **Application Optimization**
- **Data Access Patterns**: Optimize data access patterns in application code
- **Batch Operations**: Use batch operations instead of individual queries
- **Connection Management**: Optimize database connection management

### 4. **Monitoring Enhancement**
- **Real-time Monitoring**: Implement real-time slow query monitoring
- **Alert Configuration**: Set up alerts for slow query anomalies
- **Performance Baselines**: Establish performance baselines for different scenarios

## Business Impact

### 1. **Performance Benefits**
- **Faster Queries**: Optimized queries improve database performance
- **Better Throughput**: Reduced slow queries improve overall system throughput
- **Resource Efficiency**: More efficient database resource utilization

### 2. **User Experience**
- **Faster Response**: Reduced database query time improves response times
- **Better Reliability**: More stable database performance
- **Consistent Performance**: More predictable system behavior

### 3. **System Reliability**
- **Reduced Failures**: Lower risk of database-related failures
- **Better Scalability**: Improved ability to handle increased load
- **Predictable Behavior**: More consistent database performance

## Summary

The instance-based high frequency slow queries detection system provides a comprehensive and practical solution for identifying database performance issues in microservices. Through four-dimensional analysis including ratio, frequency, trend, and location, it can accurately detect various slow query anomalies and provide actionable optimization insights.

The system's instance-level detection approach ensures precise identification of problematic instances, while the severity classification helps prioritize handling efforts. The SQL fingerprint analysis provides specific optimization targets, making it an essential tool for maintaining optimal database performance in microservice architectures.
