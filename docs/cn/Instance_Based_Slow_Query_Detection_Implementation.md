# 基于实例的高频慢查询检测实现说明

## 实现概述

本实现重新设计了高频慢查询检测逻辑，确保**针对每个服务的每个实例进行独立检测**，参考了`CallRateAnomalyService`的成熟模式，采用简化但有效的检测算法。

## 核心设计原则

### 1. 实例级别检测
- **独立性**: 每个服务实例独立进行检测分析
- **精确性**: 基于实例自身的历史数据进行判断
- **灵活性**: 不同实例可能有不同的检测结果

### 2. 数据来源统一
- **缓存优先**: 使用`CacheService`获取本地缓存数据
- **历史分析**: 基于多个时间间隔的历史数据
- **实时性**: 确保数据的时效性和准确性
- **数据纯净**: 仅使用SQL相关的核心指标，不涉及请求延迟等混合指标

## 四维度检测体系

本高频慢查询检测系统采用**四维度综合检测**的方式，从不同角度全面评估实例的慢查询状况：

### 检测维度概览

| 维度 | 检测目标 | 核心指标 | 阈值 | 作用 |
|------|----------|----------|------|------|
| **比例维度** | 当前慢查询占比 | 慢查询比例 | ≥20% | 识别比例异常 |
| **频率维度** | 慢查询绝对频率 | 次数/时间 | ≥10次/分钟 | 识别频率异常 |
| **趋势维度** | 历史变化趋势 | 趋势增长倍数 | >1.5倍且>10% | 识别恶化趋势 |
| **定位维度** | 具体问题SQL | SQL指纹分析 | 慢查询比例>50%且次数≥3 | 精确定位问题 |

### 检测维度关系图

```
                    高频慢查询检测
                         |
        ┌────────────────┼────────────────┐
        |                |                |
    比例维度          频率维度          趋势维度
    (当前状态)        (绝对数量)        (历史变化)
        |                |                |
        └────────────────┼────────────────┘
                         |
                    定位维度
                  (问题SQL识别)
                         |
                    综合判定
                  (任一维度异常)
```

### 维度互补性说明

1. **比例 + 频率**: 防止单一指标误判
   - 比例高但频率低：可能是查询量少导致的统计偏差
   - 比例低但频率高：可能是高并发场景下的性能问题

2. **当前 + 趋势**: 识别不同阶段的问题
   - 当前值正常但趋势恶化：提前预警潜在问题
   - 当前值异常且趋势恶化：确认严重性能问题

3. **统计 + 定位**: 从发现到解决的完整链条
   - 统计检测：发现存在慢查询问题
   - 精确定位：识别具体的问题SQL语句

### 检测触发条件

```java
// 任意一个维度检测到异常即触发告警
boolean isDetected = 比例异常 || 频率异常 || 趋势异常 || SQL定位异常;

// 具体实现
isDetected = (currentSlowQueryRatio >= 0.20) ||           // 比例维度
             (slowQueryFrequency >= 10.0) ||              // 频率维度  
             (trendResult.isTrendDeterioration()) ||      // 趋势维度
             (!problematicQueries.isEmpty());             // 定位维度
```

## 详细实现说明

### 1. 数据获取逻辑

```java
public DetectionResItem highFreSlowQueriesDetect(RequestItem requestItem) {
    // 构建服务标识
    Integer interval = intervalWindowMappingService.getValueByName(ConstantUtil.INTERVAL_OF_DYNAMIC_KEY);
    String serviceInternal = requestItem.getServiceName() + "|" + interval;
    String serviceIntervalAVG = requestItem.getServiceName();
    
    // 获取历史数据（最多4个历史时间间隔）
    Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = 
        cacheService.getResInServiceLevel(ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);
    
    // 获取平均数据（未使用，保留接口一致性）
    Map<String, SvcExternalMetricsRes> avgExternalMetricsMap = 
        cacheService.getAvgDataInServiceLevel(ResType.EXTERNAL_AVG_METRICS.name(), serviceIntervalAVG, SvcExternalMetricsRes.class);
}
```

**关键特点**:
- `serviceInternal`: 格式为`serviceName|interval`，用于获取带时间戳的历史数据
- `serviceIntervalAVG`: 仅服务名，用于获取平均数据（当前实现中未直接使用）
- 历史数据限制为4个时间间隔，避免过多数据影响性能

### 2. 实例遍历逻辑

```java
private HighFreSlowQueriesContext highFreSlowQueriesDetectDetail(
    Map<String, List<SvcExternalMetricsRes>> historicalMetrics, 
    Map<String, SvcExternalMetricsRes> avgMetrics, 
    Integer interval) {
    
    // 依据每一个实例的历史数据进行独立的慢查询检测
    for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : historicalMetrics.entrySet()) {
        String instanceKey = entry.getKey();
        List<SvcExternalMetricsRes> instanceHistoricalData = entry.getValue();
        
        // 解析实例名（去掉时间戳后缀）
        String[] split = instanceKey.split("\\|");
        String realInstanceKey = split[0];
        
        // 执行针对单个实例的高频慢查询检测
        HighFreSlowQueriesItem slowQueryItem = detectInstanceSlowQueries(
            instanceHistoricalData, realInstanceKey, interval);
    }
}
```

**关键特点**:
- **实例名解析**: 从`language-serviceName-podName|timestamp`格式中提取`language-serviceName-podName`
- **独立检测**: 每个实例使用自己的历史数据进行检测
- **数据验证**: 跳过空数据，确保检测的可靠性

### 3. 单实例检测算法

```java
private HighFreSlowQueriesItem detectInstanceSlowQueries(
    List<SvcExternalMetricsRes> historicalData, 
    String instanceKey, 
    Integer interval) {
    
    // 获取最新数据（最后一个数据点）
    SvcExternalMetricsRes latestData = historicalData.get(historicalData.size() - 1);
    
    // 多维度检测
    // 1. 当前时间间隔慢查询比例检测
    // 2. 慢查询绝对频率检测  
    // 3. 历史趋势分析
    // 4. SQL指纹分析
}
```

## 四维度检测代码实现

### 检测方法结构

```java
private HighFreSlowQueriesItem detectInstanceSlowQueries(
    List<SvcExternalMetricsRes> historicalData, 
    String instanceKey, 
    Integer interval) {
    
    // === 数据准备阶段 ===
    SvcExternalMetricsRes latestData = historicalData.get(historicalData.size() - 1);
    int currentSlowQueryCount = latestData.getSlowQueryCount() != null ? latestData.getSlowQueryCount() : 0;
    int currentTotalQueryCount = latestData.getSqlQueryCount() != null ? latestData.getSqlQueryCount() : 0;
    double currentSlowQueryRatio = calculateSlowQueryRatio(currentSlowQueryCount, currentTotalQueryCount);
    
    List<String> detectedIssues = new ArrayList<>();
    boolean isDetected = false;
    
    // === 四维度检测实现 ===
    
    // 维度1: 比例检测
    if (currentSlowQueryRatio >= 0.20) {
        detectedIssues.add("当前慢查询比例过高: " + ...);
        isDetected = true;
    }
    
    // 维度2: 频率检测  
    double slowQueryFrequency = currentSlowQueryCount / (interval / 60.0);
    if (slowQueryFrequency >= 10.0) {
        detectedIssues.add("慢查询频率过高: " + ...);
        isDetected = true;
    }
    
    // 维度3: 趋势检测
    if (historicalData.size() >= 2) {
        SlowQueryTrendResult trendResult = analyzeSlowQueryTrend(historicalData);
        if (trendResult.isTrendDeterioration()) {
            detectedIssues.add(trendResult.getTrendDescription());
            isDetected = true;
        }
    }
    
    // 维度4: SQL定位检测
    List<String> problematicQueries = analyzeSlowQueryFingerprints(latestData);
    if (!problematicQueries.isEmpty()) {
        detectedIssues.add("发现 " + problematicQueries.size() + " 个高频慢查询SQL");
        isDetected = true;
    }
    
    // === 结果封装 ===
    item.setStatus(isDetected);
    item.setDetectedIssues(detectedIssues);
    item.setProblematicQueries(problematicQueries);
    
    return item;
}
```

### 维度检测方法映射

| 维度 | 实现方法 | 输入参数 | 输出结果 |
|------|----------|----------|----------|
| **比例维度** | 直接计算判断 | `currentSlowQueryRatio` | `boolean` + 问题描述 |
| **频率维度** | 直接计算判断 | `slowQueryFrequency` | `boolean` + 问题描述 |
| **趋势维度** | `analyzeSlowQueryTrend()` | `historicalData` | `SlowQueryTrendResult` |
| **定位维度** | `analyzeSlowQueryFingerprints()` | `latestData` | `List<String> problematicQueries` |

## 具体检测逻辑详解

### 1. 慢查询比例检测

**目的**: 检测当前时间间隔内慢查询占总查询的比例是否超过阈值

```java
// 计算公式
double currentSlowQueryRatio = calculateSlowQueryRatio(currentSlowQueryCount, currentTotalQueryCount);

private double calculateSlowQueryRatio(int slowQueryCount, int totalQueryCount) {
    if (totalQueryCount <= 0) {
        return 0.0;
    }
    return (double) slowQueryCount / totalQueryCount;
}

// 检测逻辑
if (currentSlowQueryRatio >= 0.20) { // 20%阈值
    detectedIssues.add(String.format("当前慢查询比例过高: %.1f%%", currentSlowQueryRatio * 100));
    isDetected = true;
}
```

**检测逻辑**:
- **数据来源**: `latestData.getSlowQueryCount()` 和 `latestData.getSqlQueryCount()`
- **计算公式**: `慢查询次数 ÷ 总查询次数`
- **阈值**: 20%（与原始需求"SlowQueryRate >= 20%"一致）
- **触发条件**: 比例 ≥ 20%
- **业务含义**: 当前时间间隔内，每5次查询中就有1次是慢查询时触发告警

### 2. 慢查询频率检测

**目的**: 检测慢查询的绝对频率，防止虽然比例不高但频率过于密集的情况

```java
// 计算公式：将时间间隔转换为分钟，然后计算每分钟慢查询次数
double slowQueryFrequency = currentSlowQueryCount / (interval / 60.0);

// 检测逻辑
if (slowQueryFrequency >= 10.0) { // 10次/分钟阈值
    detectedIssues.add(String.format("慢查询频率过高: %.1f次/分钟", slowQueryFrequency));
    isDetected = true;
}
```

**检测逻辑**:
- **数据来源**: `latestData.getSlowQueryCount()` 和配置的时间间隔
- **计算公式**: `慢查询次数 ÷ (时间间隔秒数 ÷ 60)`
- **阈值**: 10次/分钟
- **触发条件**: 频率 ≥ 10次/分钟
- **业务含义**: 即使慢查询比例不高，但如果每分钟出现10次以上慢查询仍然异常
- **场景示例**: 
  - 300秒间隔内50次慢查询 → 10次/分钟 → 触发告警
  - 即使总查询数很高(如1000次)，慢查询比例只有5%，但频率过高仍需关注

### 3. 历史趋势分析

**目的**: 检测慢查询比例是否出现恶化趋势，即使当前值未达到绝对阈值

```java
private SlowQueryTrendResult analyzeSlowQueryTrend(List<SvcExternalMetricsRes> historicalData) {
    if (historicalData.size() < 2) {
        return new SlowQueryTrendResult(false, "历史数据不足");
    }
    
    // 计算历史慢查询比例序列
    List<Double> slowQueryRatios = new ArrayList<>();
    for (SvcExternalMetricsRes data : historicalData) {
        int slowCount = data.getSlowQueryCount() != null ? data.getSlowQueryCount() : 0;
        int totalCount = data.getSqlQueryCount() != null ? data.getSqlQueryCount() : 0;
        double ratio = calculateSlowQueryRatio(slowCount, totalCount);
        slowQueryRatios.add(ratio);
    }
    
    // 趋势判断：最新值与历史平均值对比
    double currentRatio = slowQueryRatios.get(slowQueryRatios.size() - 1);
    double historicalAvg = slowQueryRatios.subList(0, slowQueryRatios.size() - 1)
        .stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    
    // 双重条件判断趋势恶化
    boolean isTrendDeterioration = currentRatio > historicalAvg * 1.5 && currentRatio > 0.10;
    
    return new SlowQueryTrendResult(isTrendDeterioration, description);
}
```

**检测逻辑**:
- **数据来源**: 历史数据列表中的所有数据点（最多4个时间间隔）
- **计算步骤**:
  1. 计算每个历史时间点的慢查询比例
  2. 计算除当前值外的历史平均比例
  3. 对比当前值与历史平均值
- **触发条件**（同时满足）:
  - **条件1**: 当前比例 > 历史平均 × 1.5（增长50%以上）
  - **条件2**: 当前比例 > 10%（避免低基数误判）
- **业务含义**: 检测慢查询比例的突然恶化，提前预警
- **场景示例**:
  - 历史3个间隔平均比例: 6% → 当前比例: 12% → 触发（12% > 6% × 1.5 且 12% > 10%）
  - 历史3个间隔平均比例: 2% → 当前比例: 8% → 不触发（8% < 10%）

### 4. SQL指纹分析

**目的**: 识别具体的问题SQL语句，通过分析SQL指纹定位高频慢查询的根本原因

```java
private List<String> analyzeSlowQueryFingerprints(SvcExternalMetricsRes data) {
    List<String> problematicQueries = new ArrayList<>();
    
    // 数据验证
    if (data.getSqlSlowFingerPrintMap() == null || data.getSqlFingerPrintMap() == null) {
        return problematicQueries;
    }
    
    Map<String, Integer> slowFingerprintMap = data.getSqlSlowFingerPrintMap();
    Map<String, Integer> totalFingerprintMap = data.getSqlFingerPrintMap();
    
    // 遍历所有慢查询SQL指纹
    for (Map.Entry<String, Integer> entry : slowFingerprintMap.entrySet()) {
        String sqlFingerprint = entry.getKey();
        int slowCount = entry.getValue();
        int totalCount = totalFingerprintMap.getOrDefault(sqlFingerprint, slowCount);
        
        // 双重条件判断问题SQL
        if (slowCount >= 3 && totalCount > 0) {
            double slowRatio = (double) slowCount / totalCount;
            if (slowRatio > 0.50) {
                problematicQueries.add(String.format("%s (慢查询: %d/%d, %.1f%%)", 
                    sqlFingerprint, slowCount, totalCount, slowRatio * 100));
            }
        }
    }
    
    return problematicQueries;
}
```

**检测逻辑**:
- **数据来源**: 
  - `sqlSlowFingerPrintMap`: 慢查询SQL指纹及其出现次数
  - `sqlFingerPrintMap`: 所有SQL指纹及其总执行次数
- **SQL指纹格式**: 使用DBModel的statement字段原始值作为指纹
- **触发条件**（同时满足）:
  - **条件1**: 慢查询次数 ≥ 3次（避免偶发情况，确保统计意义）
  - **条件2**: 该SQL的慢查询比例 > 50%（该SQL大概率存在性能问题）
- **计算公式**: `单个SQL慢查询比例 = 该SQL慢查询次数 ÷ 该SQL总执行次数`
- **输出格式**: `SQL语句 (慢查询: X/Y, Z%)`
- **业务含义**: 精确定位哪些具体的SQL语句导致了慢查询问题
- **场景示例**:
     - `SELECT * FROM users WHERE status = ?`: 执行12次，其中8次慢查询 → 66.7% → 识别为问题SQL
   - `SELECT * FROM orders WHERE id = ?`: 执行100次，其中2次慢查询 → 2% → 不视为问题SQL

### 5. 综合判定逻辑

**目的**: 整合四个维度的检测结果，进行最终的异常判定

```java
// 综合判定：任意一个维度检测到问题即判定为异常
boolean isDetected = false;
List<String> detectedIssues = new ArrayList<>();

// 维度1: 慢查询比例检测
if (currentSlowQueryRatio >= 0.20) {
    detectedIssues.add("当前慢查询比例过高: " + ...);
    isDetected = true;
}

// 维度2: 慢查询频率检测
if (slowQueryFrequency >= 10.0) {
    detectedIssues.add("慢查询频率过高: " + ...);
    isDetected = true;
}

// 维度3: 历史趋势分析
if (trendResult.isTrendDeterioration()) {
    detectedIssues.add(trendResult.getTrendDescription());
    isDetected = true;
}

// 维度4: SQL指纹分析
if (!problematicQueries.isEmpty()) {
    detectedIssues.add("发现 " + problematicQueries.size() + " 个高频慢查询SQL");
    isDetected = true;
}

// 设置最终结果
item.setStatus(isDetected);
item.setDetectedIssues(detectedIssues);
```

**判定逻辑**:
- **判定原则**: 只要任意一个维度检测到异常，即判定该实例存在高频慢查询问题
- **优先级**: 四个维度平等重要，无优先级区分
- **输出信息**: 记录所有检测到的问题，提供完整的诊断信息
- **实例独立**: 每个服务实例独立判定，互不影响

**检测结果汇总逻辑**:
```java
// 服务级别汇总
for (每个实例的检测结果) {
    if (实例检测结果.getStatus()) {
        服务整体异常标志 = true;
        记录异常实例信息;
    }
}
```

## 完整检测流程

```
1. 数据获取阶段
   ├── 获取服务所有实例的历史数据 (最多4个时间间隔)
   └── 获取服务所有实例的平均数据 (当前未使用)

2. 实例遍历阶段
   ├── 解析实例标识 (去掉时间戳后缀)
   ├── 验证历史数据完整性
   └── 获取最新数据点

3. 单实例四维度检测阶段
   ├── 数据准备
   │   ├── 提取基础指标 (slowQueryCount, sqlQueryCount)
   │   ├── 计算慢查询比例 (currentSlowQueryRatio)
   │   └── 初始化检测结果容器 (detectedIssues, isDetected)
   │
   ├── 维度1: 比例检测 (即时判断)
   │   ├── 输入: currentSlowQueryRatio
   │   ├── 阈值: ≥20%
   │   ├── 输出: boolean + "当前慢查询比例过高: X.X%"
   │   └── 作用: 识别当前时刻的比例异常
   │
   ├── 维度2: 频率检测 (即时计算)
   │   ├── 输入: currentSlowQueryCount, interval
   │   ├── 计算: slowQueryFrequency = count / (interval/60)
   │   ├── 阈值: ≥10次/分钟
   │   ├── 输出: boolean + "慢查询频率过高: X.X次/分钟"
   │   └── 作用: 识别高频率慢查询情况
   │
   ├── 维度3: 趋势检测 (历史分析)
   │   ├── 输入: historicalData (≥2个数据点)
   │   ├── 方法: analyzeSlowQueryTrend()
   │   ├── 算法: 当前比例 vs 历史平均比例
   │   ├── 阈值: >1.5倍增长 且 >10%
   │   ├── 输出: SlowQueryTrendResult + "慢查询比例趋势恶化"
   │   └── 作用: 识别性能恶化趋势，提前预警
   │
   └── 维度4: SQL定位检测 (指纹分析)
       ├── 输入: latestData (sqlSlowFingerPrintMap, sqlFingerPrintMap)
       ├── 方法: analyzeSlowQueryFingerprints()
       ├── 算法: 遍历慢查询指纹，计算每个SQL的慢查询比例
       ├── 阈值: 慢查询次数≥3 且 比例>50%
       ├── 输出: List<String> + "发现 X 个高频慢查询SQL"
       └── 作用: 精确定位具体的问题SQL语句

4. 综合判定阶段
   ├── 逻辑: 任意维度异常 → 实例异常 (OR逻辑)
   ├── 汇总: 收集所有维度的检测问题
   ├── 封装: 设置检测状态、问题列表、SQL列表
   └── 说明: 生成详细的检测原因说明

5. 服务级汇总阶段
   ├── 统计: 计算异常实例数量
   ├── 判定: 服务整体状态 (有异常实例即为异常)
   ├── 记录: 保存每个实例的详细检测结果
   └── 返回: 完整的检测上下文 (HighFreSlowQueriesContext)
```

### 四维度执行特点

| 维度 | 执行时机 | 数据依赖 | 计算复杂度 | 检测灵敏度 |
|------|----------|----------|------------|------------|
| **比例维度** | 立即执行 | 当前数据点 | O(1) | 高 - 直接反映当前状态 |
| **频率维度** | 立即执行 | 当前数据点 | O(1) | 中 - 考虑时间因素 |
| **趋势维度** | 条件执行 | 历史数据序列 | O(n) | 低 - 平滑历史波动 |
| **定位维度** | 立即执行 | SQL指纹映射 | O(m) | 高 - 针对具体SQL |

**说明**:
- `n`: 历史数据点数量 (≤4)
- `m`: 慢查询SQL指纹数量
- 执行顺序: 按维度1→2→3→4的顺序执行，但任一维度检测到异常都会继续执行后续维度

## 检测阈值配置说明

| 检测维度 | 阈值参数 | 值 | 业务含义 | 调整建议 |
|----------|----------|-----|----------|----------|
| 慢查询比例 | `SLOW_QUERY_RATIO_THRESHOLD` | 20% | 慢查询占总查询的比例上限 | 根据业务容忍度调整，一般10%-30% |
| 慢查询频率 | `SLOW_QUERY_FREQUENCY_THRESHOLD` | 10次/分钟 | 慢查询的绝对频率上限 | 根据系统吞吐量调整，高并发系统可适当提高 |
| 趋势恶化倍数 | `TREND_DETERIORATION_MULTIPLIER` | 1.5倍 | 当前值超过历史平均的倍数 | 1.2-2.0倍之间，值越小越敏感 |
| 趋势恶化最低比例 | `TREND_DETERIORATION_MIN_RATIO` | 10% | 避免低基数误判的最低比例 | 建议5%-15%，防止噪声数据 |
| SQL问题次数 | `SQL_PROBLEM_MIN_COUNT` | 3次 | SQL被判定为问题的最小慢查询次数 | 根据监控周期调整，一般2-5次 |
| SQL问题比例 | `SQL_PROBLEM_MIN_RATIO` | 50% | SQL被判定为问题的最小慢查询比例 | 根据业务要求调整，一般30%-70% |

## 数据指标说明

### 核心SQL指标
本检测实现严格基于以下SQL相关的核心指标：

| 指标名称 | 数据来源 | 说明 | 用途 |
|----------|----------|------|------|
| `slowQueryCount` | `SvcExternalMetricsRes.getSlowQueryCount()` | 慢查询次数 | 计算比例和频率 |
| `sqlQueryCount` | `SvcExternalMetricsRes.getSqlQueryCount()` | 总SQL查询次数 | 计算慢查询比例 |
| `sqlSlowFingerPrintMap` | `SvcExternalMetricsRes.getSqlSlowFingerPrintMap()` | 慢查询SQL指纹映射 | 识别具体问题SQL |
| `sqlFingerPrintMap` | `SvcExternalMetricsRes.getSqlFingerPrintMap()` | 全部SQL指纹映射 | 计算单SQL慢查询比例 |

### 排除的指标
以下指标虽然存在于数据源中，但**不用于**慢查询检测：

| 排除指标 | 原因 | 说明 |
|----------|------|------|
| `avgLatency` | 非SQL执行时间 | 这是整个请求的平均延迟，包含网络、业务逻辑等，不能代表SQL执行时间 |
| `requestCount` | 与SQL无关 | 请求数量与SQL查询数量不等价，一个请求可能包含多个SQL |
| `failPercent` | 非慢查询指标 | 请求失败率与SQL执行速度无直接关系 |
| `dbConnectionUsage` | 无法准确计算 | 缺乏数据库连接池的直接监控数据 |

### 慢查询定义
- **慢查询标准**: 执行时间超过1秒的SQL查询（在数据收集层面已确定）
- **检测目标**: 识别慢查询频发的实例，而非单次慢查询事件
- **业务影响**: 高频慢查询会显著影响用户体验和系统性能

## 数据结构说明

### 历史数据结构
```json
{
    "java-user-service-pod1|1730000000": [
        {
            "slowQueryCount": 15,
            "sqlQueryCount": 100,
            "sqlSlowFingerPrintMap": {
                "SELECT * FROM users WHERE status = ?": 8,
                "SELECT * FROM orders WHERE user_id = ? AND date > ?": 5,
                "UPDATE users SET last_login = ? WHERE id = ?": 2
            },
            "sqlFingerPrintMap": {
                "SELECT * FROM users WHERE status = ?": 12,
                "SELECT * FROM orders WHERE user_id = ? AND date > ?": 25,
                "UPDATE users SET last_login = ? WHERE id = ?": 30,
                "SELECT * FROM products WHERE category = ?": 33
            }
        },
        // ... 更多历史数据点
    ]
}
```

### 检测结果结构
```json
{
    "status": true,
    "slowQueryCount": 15,
    "slowQueryFrequency": 18.0,
    "slowQueryRatio": 0.15,
    "totalQueryCount": 100,
                    "detectedIssues": [
                    "当前慢查询比例过高: 22.0%",
                    "慢查询频率过高: 12.5次/分钟", 
                    "慢查询比例趋势恶化: 当前22.0% vs 历史平均8.5%",
                    "发现 2 个高频慢查询SQL"
                ],
    "problematicQueries": [
        "SELECT * FROM users WHERE status = ? (慢查询: 8/12, 66.7%)",
        "SELECT * FROM orders WHERE user_id = ? AND date > ? (慢查询: 5/25, 20.0%)"
    ],
    "detectionReason": "检测到 2 个问题: 慢查询比例趋势恶化: 当前15.0% vs 历史平均8.5%; 发现 2 个高频慢查询SQL"
}
```

**字段说明**:
- `status`: 该实例是否检测到高频慢查询异常
- `slowQueryCount`: 慢查询次数（来源于最新数据点）
- `slowQueryFrequency`: 慢查询频率（次/分钟）
- `slowQueryRatio`: 慢查询占比（0-1之间的小数）
- `totalQueryCount`: 总SQL查询次数
- `detectedIssues`: 检测到的问题列表（四个维度的检测结果）
- `problematicQueries`: 具体的问题SQL语句列表
- `detectionReason`: 检测原因的详细说明

**注意**: 不包含SQL查询执行时间相关字段，因为当前监控数据中的`avgLatency`是整个请求的平均延迟时间，而非单纯的SQL查询执行时间。

## 检测流程图

```
开始检测
    ↓
获取服务的所有实例历史数据
    ↓
遍历每个实例
    ↓
解析实例标识
    ↓
验证历史数据完整性
    ↓
获取最新数据点
    ↓
计算基础指标
    ↓
多维度检测
    ├── 慢查询比例检测 (阈值: 20%)
    ├── 慢查询频率检测 (阈值: 10次/分钟)
    ├── 历史趋势分析 (增长: >1.5倍)
    └── SQL指纹分析 (慢查询比例>50% & 次数≥3)
    ↓
综合判定结果
    ↓
记录检测详情
    ↓
下一个实例
    ↓
汇总所有实例结果
    ↓
返回检测上下文
```

## 与其他服务的对比

### CallRateAnomalyService模式（参考）
```java
// 数据获取
Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = 
    cacheService.getResInServiceLevel(ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);

// 实例遍历
for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : historicalMetrics.entrySet()) {
    String instanceKey = entry.getKey();
    List<SvcExternalMetricsRes> instanceHistoricalData = entry.getValue();
    
    // 解析实例名
    String[] split = instanceKey.split("\\|");
    String realInstanceKey = split[0];
    
    // 单实例检测
    CallRateAnomalyItem item = detectCallRateSurge(instanceHistoricalData, interval);
}
```

### 我们的实现（一致性）
```java
// 数据获取（完全一致）
Map<String, List<SvcExternalMetricsRes>> historicalExternalMetricsMap = 
    cacheService.getResInServiceLevel(ResType.EXTERNAL_METRICS.name(), serviceInternal, 4, SvcExternalMetricsRes.class);

// 实例遍历（完全一致）
for (Map.Entry<String, List<SvcExternalMetricsRes>> entry : historicalMetrics.entrySet()) {
    String instanceKey = entry.getKey();
    List<SvcExternalMetricsRes> instanceHistoricalData = entry.getValue();
    
    // 解析实例名（完全一致）
    String[] split = instanceKey.split("\\|");
    String realInstanceKey = split[0];
    
    // 单实例检测（针对慢查询特化）
    HighFreSlowQueriesItem item = detectInstanceSlowQueries(instanceHistoricalData, realInstanceKey, interval);
}
```

## 优势特点

### 1. 架构一致性
- **标准模式**: 完全遵循`CallRateAnomalyService`的成熟模式
- **代码风格**: 保持与现有代码库的一致性
- **维护性**: 便于团队理解和维护

### 2. 检测精度
- **实例独立**: 每个实例基于自身数据进行判断
- **多维度**: 4个维度综合检测，减少误报
- **历史趋势**: 基于时间序列数据分析趋势

### 3. 性能优化
- **缓存优先**: 使用本地缓存，响应速度快
- **数据限制**: 最多4个历史时间间隔，控制内存使用
- **简化算法**: 避免复杂计算，提高检测效率

### 4. 实用性
- **阈值合理**: 基于实际业务经验设定阈值
- **输出详细**: 提供具体的问题SQL和检测原因
- **可扩展**: 易于添加新的检测维度

## 配置参数

| 参数 | 值 | 说明 |
|------|----|----|
| 慢查询比例阈值 | 20% | 触发检测的慢查询占总查询的比例 |
| 慢查询频率阈值 | 10次/分钟 | 触发检测的慢查询绝对频率 |
| 趋势恶化倍数 | 1.5倍 | 当前比例超过历史平均的倍数 |
| 趋势恶化最低比例 | 10% | 避免低基数情况下的误判 |
| SQL问题判定次数 | 3次 | SQL被判定为问题的最小慢查询次数 |
| SQL问题判定比例 | 50% | SQL被判定为问题的最小慢查询比例 |
| 历史数据窗口 | 4个间隔 | 用于趋势分析的历史数据点数量 |

## 使用示例

### API调用
```bash
POST /dynamic-analysis/high-frequency-of-slow-queries
Content-Type: application/json

{
    "serviceName": "user-service"
}
```

### 响应示例
```json
{
    "code": 200,
    "message": "success",
    "data": "Detect command reached.",
    "detectionResult": {
        "status": true,
        "instanceStatus": {
            "java-user-service-pod1": {
                "status": true,
                "slowQueryCount": 15,
                "slowQueryFrequency": 18.0,
                "slowQueryRatio": 0.15,
                "totalQueryCount": 100,
                "detectedIssues": [
                    "慢查询比例趋势恶化: 当前15.0% vs 历史平均8.5%",
                    "发现 2 个高频慢查询SQL"
                ],
                "problematicQueries": [
                    "SELECT * FROM users WHERE status = ? (慢查询: 8/12, 66.7%)"
                ],
                "detectionReason": "检测到 4 个问题: 当前慢查询比例过高: 22.0%; 慢查询频率过高: 12.5次/分钟; 慢查询比例趋势恶化: 当前22.0% vs 历史平均8.5%; 发现 2 个高频慢查询SQL"
            },
            "java-user-service-pod2": {
                "status": false,
                "detectionReason": "未检测到高频慢查询异常"
            }
        },
        "metricsInterval": 300
    }
}
```

## 四维度检测优势总结

### 1. 检测全面性
- **多角度覆盖**: 从比例、频率、趋势、定位四个维度全面检测
- **相互补充**: 各维度相互验证，提高检测准确性
- **场景适应**: 适应不同的慢查询表现模式

### 2. 检测精确性
- **量化标准**: 每个维度都有明确的数值阈值
- **科学算法**: 基于统计学和时间序列分析的检测逻辑
- **误报控制**: 通过多维度验证减少单一指标的误判

### 3. 实用价值
- **问题发现**: 及时识别慢查询异常
- **原因定位**: 通过SQL指纹精确定位问题语句
- **趋势预警**: 提前发现性能恶化趋势
- **运维指导**: 提供详细的检测原因和问题SQL列表

### 4. 技术特点
- **实例独立**: 每个服务实例基于自身数据独立检测
- **架构一致**: 完全遵循现有检测服务的标准模式
- **性能高效**: 优化的算法复杂度，支持实时检测
- **可配置**: 所有阈值参数可根据业务需求调整

这个基于四维度的实现确保了针对每个服务实例的独立、精确、全面的高频慢查询检测，同时保持了与现有系统架构的一致性，提供了可靠、高效的检测能力。 