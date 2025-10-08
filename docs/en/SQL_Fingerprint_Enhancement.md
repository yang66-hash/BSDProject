# SQL Fingerprint Data Collection Function Implementation

## Function Overview

Based on the original `sqlQueryCount` and `slowQueryCount` statistics, a new SQL fingerprint data collection function has been added. Through the `sqlFingerPrintMap` and `sqlSlowFingerPrintMap` fields, more detailed SQL query pattern information is recorded.

## Major Improvements

### 1. Data Structure Extension

Two new fields added to `SvcExternalMetricsRes`:
```java
@Schema(description = "Key is the fingerprint of database queries for the current instance in the current time window, value is the corresponding database query count")
private Map<String, Integer> sqlFingerPrintMap;

@Schema(description = "Key is the fingerprint of database queries for the current instance in the current time window, value is the corresponding slow query count")
private Map<String, Integer> sqlSlowFingerPrintMap;
```

### 2. Fingerprint Generation Logic

#### SQL Fingerprint Generation Strategy
1. **Direct use of statement value**: Use the original value of the statement field in DBModel as the fingerprint key
2. **Fallback strategy**: If no SQL statement is available, use the format `database_type.sub_type.instance_name`

#### Fingerprint Generation Implementation
```java
private static String generateSqlFingerprint(Span span) {
    if (span.getDb() == null) {
        return "UNKNOWN_SQL";
    }
    
    // Directly use the statement field value as fingerprint key
    String statement = span.getDb().getStatement();
    if (statement != null && !statement.trim().isEmpty()) {
        return statement.trim();
    }
    
    // Fallback strategy: database_type.sub_type.instance_name
    return buildFallbackFingerprint(span);
}
```

#### Fingerprint Examples
| DBModel statement field | Generated fingerprint key |
|------------------------|---------------------------|
| `SELECT * FROM users WHERE id = ?` | `SELECT * FROM users WHERE id = ?` |
| `INSERT INTO orders (id, user_id) VALUES (?, ?)` | `INSERT INTO orders (id, user_id) VALUES (?, ?)` |
| `UPDATE products SET price = ? WHERE id = ?` | `UPDATE products SET price = ? WHERE id = ?` |
| `DELETE FROM logs WHERE created_at < ?` | `DELETE FROM logs WHERE created_at < ?` |

### 3. Data Collection Process

#### Processing logic in the `dealWithSQL` method:
```java
subSqlSpan.forEach(sqlSpan -> {
    Span span = sqlSpan.getSpan();
    
    // 1. Generate SQL fingerprint
    String sqlFingerprint = generateSqlFingerprint(span);
    
    // 2. Process single query
    if (span.getComposite() == null) {
        // Determine if it's a slow query
        if (isDurationExceedsThreshold(span)) {
            sqlSlowQueryNumMap.put(uniqueNote, count + 1);
            updateSqlFingerprint(sqlSlowFingerPrintMap, uniqueNote, sqlFingerprint, 1);
        }
        sqlQueryNumMap.put(uniqueNote, count + 1);
        updateSqlFingerprint(sqlFingerPrintMap, uniqueNote, sqlFingerprint, 1);
    } 
    // 3. Process batch queries (composite case)
    else {
        Integer count = span.getComposite().getCount();
        if (isAverageTimeExceedsThreshold(span, count)) {
            sqlSlowQueryNumMap.put(uniqueNote, existingCount + count);
            updateSqlFingerprint(sqlSlowFingerPrintMap, uniqueNote, sqlFingerprint, count);
        }
        sqlQueryNumMap.put(uniqueNote, existingCount + count);
        updateSqlFingerprint(sqlFingerPrintMap, uniqueNote, sqlFingerprint, count);
    }
});
```

## Data Structure Examples

### Output Data Example
```json
{
    "serviceName": "user-service",
    "podName": "user-service-pod-1",
    "sqlQueryCount": 150,
    "slowQueryCount": 12,
    "sqlFingerPrintMap": {
        "SELECT * FROM users WHERE id = ?": 45,
        "SELECT * FROM orders WHERE user_id = ?": 32,
        "INSERT INTO users (name, email) VALUES (?, ?)": 28,
        "UPDATE users SET last_login = ? WHERE id = ?": 25,
        "DELETE FROM logs WHERE created_at < ?": 20
    },
    "sqlSlowFingerPrintMap": {
        "SELECT * FROM orders WHERE user_id = ?": 8,
        "SELECT * FROM users WHERE id = ?": 4
    }
}
```

### Data Analysis Value
1. **Query Pattern Recognition**: Identify the most frequent query types
2. **Slow Query Hotspots**: Discover which tables or query types are most prone to slow queries
3. **Performance Optimization Guidance**: Provide data support for index optimization and query rewriting
4. **Capacity Planning**: Perform database capacity planning based on query patterns

## Technical Implementation Details

### 1. Fingerprint Update Mechanism
```java
private static void updateSqlFingerprint(Map<String, Map<String, Integer>> fingerprintMap, 
                                       String uniqueNote, 
                                       String fingerprint, 
                                       Integer count) {
    fingerprintMap.computeIfAbsent(uniqueNote, k -> new HashMap<>())
                 .merge(fingerprint, count, Integer::sum);
}
```

### 2. Fingerprint Key Characteristics
- **Completeness**: Preserves complete SQL statements including parameter placeholders
- **Precision**: Can distinguish different SQL patterns and query conditions
- **Readability**: Uses original SQL directly for easy understanding and analysis
- **Consistency**: Maintains complete consistency with the statement field in DBModel

### 3. Error Handling
- Uses fallback fingerprint generation strategy when SQL parsing fails
- Null value handling: Ensures exceptions won't occur due to missing data
- Exception catching: Exceptions during SQL parsing won't affect overall statistics

## Use Cases

### 1. Performance Monitoring
- Identify slow query hotspot tables
- Monitor performance trends of specific query types
- Discover new query patterns

### 2. Capacity Planning
- Analyze query load distribution
- Predict database pressure growth points
- Optimize database partitioning strategies

### 3. Development Optimization
- Guide index creation strategies
- Identify SQL patterns that need optimization
- Monitor the impact of code changes on databases

## Configuration Instructions

### Slow Query Threshold Configuration
Slow query determination is based on the `ConstantUtil.SQL_EXEC_TIME_BOUND` constant, which can be adjusted according to business needs:
```java
// Example: Set slow query threshold to 1 second (1,000,000 microseconds)
public static final Integer SQL_EXEC_TIME_BOUND = 1000000;
```

### Fingerprint Generation Configuration
Advantages of using original statement values as fingerprints:
- **No additional configuration needed**: Directly uses existing fields in DBModel
- **Maintains data integrity**: No loss of SQL statement information
- **Easy debugging**: Can directly see specific SQL statements
- **Precise matching**: Can precisely identify identical SQL query patterns

This implementation provides the most direct and precise data support for your high frequency slow query detection, enabling the detection algorithm to perform precise analysis and optimization recommendations based on specific SQL statements.
