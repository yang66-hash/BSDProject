# The Ramp Detection Implementation

## Detection Objective

Detect whether service instances have The Ramp problem. The Ramp refers to the phenomenon where request processing time increases sharply as the dataset grows, manifested as the same API experiencing continuously increasing processing time across consecutive time windows. This phenomenon usually indicates gradual system performance degradation and may lead to decreased user experience.

## Detection Dimensions

This detection system uses a **continuous time window analysis** approach to evaluate the growth trend of request processing time:

### Detection Dimension Overview

| Dimension | Detection Target | Core Metrics | Threshold | Purpose |
|-----------|------------------|--------------|-----------|---------|
| **Continuous Growth** | Persistence | Consecutive growth intervals | ≥3 intervals | Identify continuous growth trends |
| **Growth Magnitude** | Significance | Single interval growth rate | >20% | Identify significant growth |
| **Overall Growth** | Severity | Cumulative growth rate | >50% | Assess overall impact |

### Detection Dimension Relationship Diagram

```
                The Ramp Detection
                         |
        ┌────────────────┼────────────────┐
        |                |                |
    Continuity      Significance      Severity
  Determination   Determination    Determination
   (≥3 intervals)  (>20%/interval) (Total growth >50%)
        |                |                |
        └────────────────┼────────────────┘
                         |
                    Comprehensive
                   Determination
            (Continuity && Severity = true)
```

## Detection Logic

1. **Extract Response Time Data**:
```java
// Get average response time for each time window
List<Double> avgLatencies = metrics.stream()
    .map(SvcExternalMetricsRes::getAvgLatency)
    .collect(Collectors.toList());
```

2. **Calculate Growth Trends**:
```java
// Calculate growth rate for each interval
for (int i = 1; i < avgLatencies.size(); i++) {
    double current = avgLatencies.get(i);
    double previous = avgLatencies.get(i - 1);
    double growthRate = (current - previous) / previous;
    
    if (growthRate > GROWTH_THRESHOLD) {
        consecutiveGrowth++;
        totalGrowthRate += growthRate;
    } else {
        consecutiveGrowth = 0;
        totalGrowthRate = 0;
    }
}
```

3. **Determination Rules**:
```java
// Determine if The Ramp problem exists
boolean hasConsecutiveGrowth = consecutiveGrowth >= MIN_CONSECUTIVE_INTERVALS - 1;
boolean hasSignificantGrowth = totalGrowthRate > SIGNIFICANT_GROWTH_THRESHOLD;

boolean isDetected = hasConsecutiveGrowth && hasSignificantGrowth;
```

## Threshold Selection Rationale

1. **Consecutive Growth Intervals (≥3)**
   - At least 3 consecutive growth intervals required
   - Filters out temporary fluctuations
   - Ensures persistence of growth trend

2. **Single Interval Growth Rate (>20%)**
   - Growth exceeding 20% per interval considered significant
   - Filters out normal load fluctuations
   - Identifies abnormal performance degradation

3. **Overall Growth Rate (>50%)**
   - Cumulative growth exceeding 50% considered severe
   - Indicates significant system performance decline
   - Requires immediate attention and handling

## Detection Result Example

```json
{
    "status": true,
    "consecutiveGrowthCount": 3,
    "totalGrowthRate": 0.75,
    "latencyTrend": [
        "Interval 1→2: 100.00ms → 125.00ms (growth 25.0%)",
        "Interval 2→3: 125.00ms → 156.25ms (growth 25.0%)",
        "Interval 3→4: 156.25ms → 195.31ms (growth 25.0%)"
    ],
    "detectionReason": "The Ramp problem detected: 3 consecutive intervals with response time growth exceeding 20%, total growth rate 75.0%"
}
```

## Optimization Recommendations

1. **Data Access Optimization**:
   - Optimize database queries
   - Implement data pagination
   - Add appropriate indexes

2. **Caching Strategy Optimization**:
   - Implement multi-level caching
   - Optimize cache update strategies
   - Set reasonable cache capacity

3. **Query Optimization**:
   - Optimize SQL statements
   - Reduce database connection count
   - Implement query result caching

4. **Monitoring Optimization**:
   - Real-time response time monitoring
   - Set performance baselines
   - Configure alert thresholds
