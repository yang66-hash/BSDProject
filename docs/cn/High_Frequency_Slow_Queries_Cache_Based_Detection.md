# 基于缓存的高频慢查询检测实现

## 修改说明

根据您的建议，我已经将数据获取方式从ES改为通过`CacheService`从本地缓存获取，参考了`UnevenApiUsageService`、`InconsistentServiceResponseService`、`CallRateAnomalyService`的实现模式。

## 数据获取方式对比

### 修改前（ES方式）
```java
// 从ES获取数据
Map<String, SvcExternalMetricsRes> windowMetrics = eSService.getExternalMetrics(indexNameForWindow, serviceName);
Map<String, SvcExternalMetricsRes> intervalMetrics = eSService.getExternalMetrics(indexNameForInterval, serviceName);
```

### 修改后（缓存方式）
```java
// 从缓存获取历史数据（参考CallRateAnomalyService）
Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = cacheService.getResInServiceLevel(
    ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);

// 获取平均数据（参考UnevenApiUsageService/InconsistentServiceResponseService）
Map<String, SvcExternalMetricsRes> avgExternalMetricsMap = cacheService.getAvgDataInServiceLevel(
    ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);
```

## 核心实现改进

### 1. 数据获取逻辑
```java
public DetectionResItem highFreSlowQueriesDetect(RequestItem requestItem) {
    // 使用CacheService获取缓存数据，参考其他服务的实现方式
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();
    
    // 获取历史数据用于统计分析（最多4个历史时间间隔）
    Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = cacheService.getResInServiceLevel(
        ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);
    
    // 获取时间窗口内的平均数据
    Map<String, SvcExternalMetricsRes> avgExternalMetricsMap = cacheService.getAvgDataInServiceLevel(
        ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);

    // 执行检测
    HighFreSlowQueriesContext context = highFreSlowQueriesDetectDetail(
        historicalExternalMetricsMap, avgExternalMetricsMap, interval);
}
```

### 2. 检测逻辑适配
```java
private HighFreSlowQueriesContext highFreSlowQueriesDetectDetail(
    Map<String, List<SvcExternalMetricsRes>> historicalMetrics, 
    Map<String, SvcExternalMetricsRes> avgMetrics, 
    Integer interval) {
    
    // 依据每一个实例的历史数据进行统计分析
    for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : historicalMetrics.entrySet()) {
        String instanceKey = entry.getKey();
        List<SvcExternalMetricsRes> instanceHistoricalData = entry.getValue();
        
        // 解析实例名（去掉时间戳后缀）
        String[] split = instanceKey.split("\\|");
        String realInstanceKey = split[0];
        
        // 获取对应的平均数据
        SvcExternalMetricsRes avgData = avgMetrics.get(realInstanceKey);
        
        // 执行检测
        HighFreSlowQueriesDetectionResult detectionResult = detectHighFrequencySlowQueries(
            instanceHistoricalData, avgData, realInstanceKey, interval);
    }
}
```

### 3. 多维度检测算法
```java
private HighFreSlowQueriesDetectionResult detectHighFrequencySlowQueries(
    List<SvcExternalMetricsRes> historicalData, 
    SvcExternalMetricsRes avgData, 
    String instanceKey, 
    Integer interval) {
    
    // 获取最新数据（最后一个数据点）
    SvcExternalMetricsRes latestData = historicalData.get(historicalData.size() - 1);
    
    // 提取指标
    int latestSlowQueryCount = latestData.getSlowQueryCount() != null ? latestData.getSlowQueryCount() : 0;
    int latestTotalQueryCount = latestData.getSqlQueryCount() != null ? latestData.getSqlQueryCount() : 0;
    int avgSlowQueryCount = (avgData != null && avgData.getSlowQueryCount() != null) ? avgData.getSlowQueryCount() : 0;
    int avgTotalQueryCount = (avgData != null && avgData.getSqlQueryCount() != null) ? avgData.getSqlQueryCount() : 0;
    
    // 计算比例
    double latestSlowQueryRatio = calculateSlowQueryRatio(latestSlowQueryCount, latestTotalQueryCount);
    double avgSlowQueryRatio = calculateSlowQueryRatio(avgSlowQueryCount, avgTotalQueryCount);
    
    // 多维度检测
    // 1. 最新时间间隔慢查询比例检测 (权重30%)
    // 2. 平均慢查询比例检测 (权重25%)
    // 3. 慢查询频率检测 (权重20%)
    // 4. SQL指纹分析 (权重15%)
    // 5. 趋势恶化检测 (权重10%)
}
```

## 参考服务对比

### CallRateAnomalyService模式
- 使用`getResInServiceLevel()`获取历史数据
- 分析时间序列数据进行异常检测
- 适用于需要趋势分析的场景

### UnevenApiUsageService模式
- 使用`getAvgDataInServiceLevel()`获取平均数据
- 基于聚合数据进行分析
- 适用于基于统计分布的检测

### InconsistentServiceResponseService模式
- 同时使用历史数据和平均数据
- 结合时间序列分析和基线对比
- 适用于多维度综合检测

### 我们的实现
采用了**InconsistentServiceResponseService模式**，因为慢查询检测需要：
- 历史数据：分析趋势变化
- 平均数据：建立基线对比
- 多维度：综合评估异常程度

## 数据结构说明

### 历史数据结构
```json
{
    "language-serviceName-podName|1730000000": [
        {
            "slowQueryCount": 10,
            "sqlQueryCount": 100,
            "sqlSlowFingerPrintMap": {
                "SELECT * FROM users WHERE id = ?": 5,
                "SELECT * FROM orders WHERE user_id = ?": 3
            },
            "sqlFingerPrintMap": {
                "SELECT * FROM users WHERE id = ?": 20,
                "SELECT * FROM orders WHERE user_id = ?": 15,
                "INSERT INTO logs VALUES (?, ?)": 10
            }
        }
    ]
}
```

### 平均数据结构
```json
{
    "language-serviceName-podName": {
        "slowQueryCount": 8,
        "sqlQueryCount": 95,
        "sqlSlowFingerPrintMap": {
            "SELECT * FROM users WHERE id = ?": 4,
            "SELECT * FROM orders WHERE user_id = ?": 2
        },
        "sqlFingerPrintMap": {
            "SELECT * FROM users WHERE id = ?": 18,
            "SELECT * FROM orders WHERE user_id = ?": 12,
            "INSERT INTO logs VALUES (?, ?)": 8
        }
    }
}
```

## 检测维度详解

### 1. 最新时间间隔慢查询比例检测
- **数据源**: `latestData.slowQueryCount / latestData.sqlQueryCount`
- **阈值**: 20%
- **权重**: 30%

### 2. 平均慢查询比例检测
- **数据源**: `avgData.slowQueryCount / avgData.sqlQueryCount`
- **阈值**: 15%（比时间间隔稍低）
- **权重**: 25%

### 3. 慢查询频率检测
- **数据源**: `latestData.slowQueryCount / (interval/60)`
- **阈值**: 10次/分钟
- **权重**: 20%

### 4. SQL指纹分析
- **数据源**: `latestData.sqlSlowFingerPrintMap`
- **逻辑**: 识别慢查询比例>50%且执行≥3次的SQL
- **权重**: 15%

### 5. 趋势恶化检测
- **数据源**: `latestSlowQueryRatio vs avgSlowQueryRatio`
- **逻辑**: 最新比例 > 平均比例 * 1.5
- **权重**: 10%

## 优势说明

### 1. 性能优势
- **缓存访问**: 比ES查询更快
- **本地数据**: 减少网络开销
- **实时性**: 数据更新更及时

### 2. 一致性优势
- **统一接口**: 与其他检测服务保持一致
- **标准化**: 遵循既定的数据获取模式
- **可维护性**: 便于统一管理和优化

### 3. 功能优势
- **历史分析**: 支持时间序列分析
- **基线对比**: 基于平均值建立基线
- **SQL指纹**: 精确定位问题查询

这个修改确保了高频慢查询检测与其他检测服务保持一致的架构模式，同时保留了原有的多维度检测算法的先进性。 