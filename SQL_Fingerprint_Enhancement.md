# SQL指纹数据收集功能实现说明

## 功能概述

在原有的 `sqlQueryCount` 和 `slowQueryCount` 统计基础上，新增了SQL指纹数据收集功能，通过 `sqlFingerPrintMap` 和 `sqlSlowFingerPrintMap` 字段记录更详细的SQL查询模式信息。

## 主要改进

### 1. 数据结构扩展

在 `SvcExternalMetricsRes` 中新增两个字段：
```java
@Schema(description = "key为当前时间窗口下当前实例的数据库查询的指纹，value为对应数据库查询次数")
private Map<String, Integer> sqlFingerPrintMap;

@Schema(description = "key为当前时间窗口下当前实例的数据库查询的指纹，value为对应慢查询的次数")
private Map<String, Integer> sqlSlowFingerPrintMap;
```

### 2. 指纹生成逻辑

#### SQL指纹生成策略
1. **直接使用statement值**: 使用DBModel中的statement字段的原始值作为指纹key
2. **备用策略**: 如果没有SQL语句，则使用 `数据库类型.子类型.实例名` 格式

#### 指纹生成实现
```java
private static String generateSqlFingerprint(Span span) {
    if (span.getDb() == null) {
        return "UNKNOWN_SQL";
    }
    
    // 直接使用statement字段的值作为指纹key
    String statement = span.getDb().getStatement();
    if (statement != null && !statement.trim().isEmpty()) {
        return statement.trim();
    }
    
    // 备用策略：数据库类型.子类型.实例名
    return buildFallbackFingerprint(span);
}
```

#### 指纹示例
| DBModel中的statement字段 | 生成的指纹key |
|-------------------------|---------------|
| `SELECT * FROM users WHERE id = ?` | `SELECT * FROM users WHERE id = ?` |
| `INSERT INTO orders (id, user_id) VALUES (?, ?)` | `INSERT INTO orders (id, user_id) VALUES (?, ?)` |
| `UPDATE products SET price = ? WHERE id = ?` | `UPDATE products SET price = ? WHERE id = ?` |
| `DELETE FROM logs WHERE created_at < ?` | `DELETE FROM logs WHERE created_at < ?` |

### 3. 数据收集流程

#### 在 `dealWithSQL` 方法中的处理逻辑：
```java
subSqlSpan.forEach(sqlSpan -> {
    Span span = sqlSpan.getSpan();
    
    // 1. 生成SQL指纹
    String sqlFingerprint = generateSqlFingerprint(span);
    
    // 2. 处理单次查询
    if (span.getComposite() == null) {
        // 判断是否为慢查询
        if (isDurationExceedsThreshold(span)) {
            sqlSlowQueryNumMap.put(uniqueNote, count + 1);
            updateSqlFingerprint(sqlSlowFingerPrintMap, uniqueNote, sqlFingerprint, 1);
        }
        sqlQueryNumMap.put(uniqueNote, count + 1);
        updateSqlFingerprint(sqlFingerPrintMap, uniqueNote, sqlFingerprint, 1);
    } 
    // 3. 处理批量查询（composite情况）
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

## 数据结构示例

### 输出数据示例
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

### 数据分析价值
1. **查询模式识别**: 识别最频繁的查询类型
2. **慢查询热点**: 发现哪些表或查询类型最容易出现慢查询
3. **性能优化指导**: 为索引优化和查询重写提供数据支持
4. **容量规划**: 基于查询模式进行数据库容量规划

## 技术实现细节

### 1. 指纹更新机制
```java
private static void updateSqlFingerprint(Map<String, Map<String, Integer>> fingerprintMap, 
                                       String uniqueNote, 
                                       String fingerprint, 
                                       Integer count) {
    fingerprintMap.computeIfAbsent(uniqueNote, k -> new HashMap<>())
                 .merge(fingerprint, count, Integer::sum);
}
```

### 2. 指纹key特点
- **完整性**: 保留完整的SQL语句，包含参数占位符
- **精确性**: 能够区分不同的SQL模式和查询条件
- **可读性**: 直接使用原始SQL，便于理解和分析
- **一致性**: 与DBModel中的statement字段保持完全一致

### 3. 容错处理
- SQL解析失败时使用备用指纹生成策略
- 空值处理：确保不会因为缺失数据导致异常
- 异常捕获：SQL解析过程中的异常不会影响整体统计

## 使用场景

### 1. 性能监控
- 识别慢查询热点表
- 监控特定查询类型的性能趋势
- 发现新增的查询模式

### 2. 容量规划
- 分析查询负载分布
- 预测数据库压力增长点
- 优化数据库分区策略

### 3. 开发优化
- 指导索引创建策略
- 识别需要优化的SQL模式
- 监控代码变更对数据库的影响

## 配置说明

### 慢查询阈值配置
慢查询判断基于 `ConstantUtil.SQL_EXEC_TIME_BOUND` 常量，可根据业务需求调整：
```java
// 示例：设置慢查询阈值为1秒（1,000,000微秒）
public static final Integer SQL_EXEC_TIME_BOUND = 1000000;
```

### 指纹生成配置
使用statement原始值作为指纹的优势：
- **无需额外配置**: 直接使用DBModel中的现有字段
- **保持数据完整性**: 不丢失任何SQL语句信息
- **便于调试**: 可以直接看到具体的SQL语句
- **精确匹配**: 能够精确识别相同的SQL查询模式

这个实现为您的高频慢查询检测提供了最直接和精确的数据支持，使得检测算法能够基于具体的SQL语句进行精确的分析和优化建议。 