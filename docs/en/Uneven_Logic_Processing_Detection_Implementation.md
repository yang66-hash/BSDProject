# Uneven Logic Processing Detection Implementation

## Detection Objective

Detect whether service instances have uneven business logic processing problems. If within a certain time window, when processing external requests, there are significant differences in the number of method calls, it may lead to high load on certain service methods while other methods may be idle. This unevenness may cause unreasonable resource usage, performance bottlenecks, and system instability.

## Detection Dimensions

This detection system uses a **multi-dimensional analysis** approach to evaluate the uniformity of business logic processing:

### Detection Dimension Overview

| Dimension | Detection Target | Core Metrics | Threshold | Purpose |
|-----------|------------------|--------------|-----------|---------|
| **Extreme Concentration** | Single method ratio | Hottest method call ratio | >80% | Identify severe hotspot methods |
| **High Concentration** | Single method ratio | Hottest method call ratio | >60% | Identify obvious concentration issues |
| **Pareto Anomaly** | Method distribution | Top 20% method call ratio | >95% | Identify severe distribution imbalance |
| **Concentration Score** | Overall distribution | Gini coefficient score | >70 points | Assess overall uniformity |

### Detection Dimension Relationship Diagram

```
                Uneven Logic Processing Detection
                                |
        ┌──────────────────────┼──────────────────────┐
        |                      |                      |
    Strong Condition      General Condition      Distribution Analysis
   Detection (Any         Detection (Meet         (Auxiliary Judgment)
    Satisfied)            Two Conditions)
        |                      |                      |
   ┌────┴────┐           ┌────┴────┐           ┌────┴────┐
   |Extreme  |           |High     |           |Concentration|
   |Concentration|       |Concentration|       |Score        |
   |Pareto    |           |Pareto   |           |Method       |
   |Anomaly   |           |Ratio    |           |Distribution |
   └─────────┘           └─────────┘           └─────────────┘
        |                      |                      |
        └──────────────────────┼──────────────────────┘
                                |
                        Comprehensive
                       Determination
```

## Detection Logic

1. **Data Aggregation**:
```java
// Aggregate method call data from all instances
for (BusinessMetricsRes metrics : instanceData) {
    for (Map.Entry<String, Long> methodEntry : metrics.getBusinessCallMap().entrySet()) {
        String methodName = methodEntry.getKey();
        Long callCount = methodEntry.getValue();
        aggregatedMap.merge(methodName, callCount, Long::sum);
    }
}
```

2. **Key Metrics Calculation**:
```java
// Hottest method ratio
double hottestMethodRatio = (double) hottestMethod.getValue() / totalCalls;

// Pareto metrics
int top20MethodCount = Math.max(1, (int) Math.ceil(totalMethods * 0.2));
double top20Ratio = (double) top20Calls / totalCalls;

// Concentration score (based on Gini coefficient)
double concentrationScore = calculateConcentrationScore(sortedMethods, totalCalls);
```

3. **Detection Rule Determination**:
```java
// Strong condition: Extreme concentration OR Pareto anomaly
boolean strongCondition = hottestMethodRatio > 0.8 || top20Ratio > 0.95;

// General condition: Meet any two conditions
boolean weakCondition = triggeredRules.size() >= 2;

boolean isDetected = strongCondition || weakCondition;
```

## Threshold Selection Rationale

1. **Extreme Concentration (80%)**
   - Single method occupying more than 80% of call volume
   - Indicates severe hotspot method problem
   - Requires immediate attention and optimization

2. **High Concentration (60%)**
   - Single method occupying more than 60% of call volume
   - Indicates obvious method call imbalance
   - Suggests method design optimization

3. **Pareto Anomaly (95%)**
   - Top 20% methods occupying more than 95% of call volume
   - Severely violates 80/20 rule
   - Requires re-evaluation of business process design

4. **Concentration Score (70 points)**
   - 0-100 score based on Gini coefficient
   - Above 70 points indicates severe distribution imbalance
   - Requires attention to overall architecture design

## Detection Result Example

```json
{
    "status": true,
    "maxCallCount": 2500,
    "minCallCount": 30,
    "avgCallCount": 600.0,
    "callCountRange": 0.988,
    "highLoadMethods": [
        "UserService#processOrder: 2500 calls (83.3%)",
        "UserService#createUser: 240 calls (8.0%)",
        "UserService#updateUser: 120 calls (4.0%)",
        "UserService#queryUser: 90 calls (3.0%)",
        "UserService#deleteUser: 50 calls (1.7%)"
    ],
    "detectionReason": "Uneven logic processing problem detected [Participating instances: 3] [Total methods: 5] [Total calls: 3000] [Triggered rules: Extreme concentration (83.3%); Pareto anomaly (96.0%); High concentration score (78.5)]"
}
```

## Optimization Recommendations

1. **Business Process Optimization**:
   - Review business processes, identify hotspot methods
   - Consider business process refactoring
   - Implement load distribution

2. **Method Design Optimization**:
   - Split large methods into smaller ones
   - Extract common logic
   - Implement method-level caching

3. **Architecture Optimization**:
   - Introduce message queues for peak shaving
   - Implement method-level rate limiting
   - Consider service splitting

4. **Monitoring Optimization**:
   - Monitor high-load methods separately
   - Set method-level alerts
   - Regularly evaluate method distribution
