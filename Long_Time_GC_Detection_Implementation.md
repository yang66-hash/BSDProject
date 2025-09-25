# GC时间过长检测实现说明

## 检测目标

检测服务实例在进行垃圾回收时是否出现暂停时间过长的问题。过长的GC暂停时间会导致服务响应延迟，在高并发场景下可能造成请求失败率上升。

## 检测策略

本检测系统根据历史数据量采用不同的检测策略：

### 1. 数据量少于5个时：固定阈值检测

| 维度 | 检测目标 | 核心指标 | 阈值 | 作用 |
|------|----------|----------|------|------|
| **Minor GC时间** | Young区GC暂停时间 | 最长Minor GC时间 | >200ms | 识别Young区GC问题 |
| **Major GC时间** | Old区GC暂停时间 | 最长Major GC时间 | >1000ms | 识别Full GC问题 |

### 2. 数据量大于等于5个时：动态阈值检测

1. 收集历史GC时间（除最新数据点外）
2. 剔除超过固定阈值的极端值
3. 计算正常GC的平均时间
4. 使用 平均时间 * (1 + 20%) 作为动态阈值
5. 检查最新GC时间是否超过动态阈值

## 检测维度关系图

```
                    GC时间过长检测
                         |
        ┌────────────────┼────────────────┐  
        |                                 |
   Minor GC时间                     Major GC时间
    (Young区)                         (Old区)
        |                                 |
        └────────────────┼────────────────┘
                         |
                    策略选择
                         |
        ┌────────────────┼────────────────┐
        |                                 |
    固定阈值                         动态阈值
  (数据量<5)                      (数据量>=5)
```

## GC数据计算方式

1. **单次GC时间计算**:
```java
// 计算单次GC的平均时间
long avgGCTime = latestGC.getGcTime() / latestGC.getGcCount();

// 根据GC类型分类
if (gcType.contains("Scavenge") || gcType.contains("ParNew") || gcType.contains("G1 Young")) {
    minorGCTimes.add(avgGCTime);
} else {
    majorGCTimes.add(avgGCTime);
}
```

2. **动态阈值计算**:
```java
// 计算正常GC的平均时间（剔除超过固定阈值的极端值）
double avgMinorGCTime = historicalMinorGCTimes.stream()
    .filter(time -> time <= MINOR_GC_TIME_THRESHOLD)
    .mapToLong(Long::longValue)
    .average()
    .orElse(MINOR_GC_TIME_THRESHOLD);

// 计算动态阈值
double minorGCThreshold = avgMinorGCTime * (1 + GC_TIME_GROWTH_RATIO);
```

## 阈值选择依据

1. **固定阈值**
   - Minor GC时间阈值 (200ms)
     - Young区GC通常应该在几十到100ms内完成
     - 超过200ms说明Young区存在严重问题
   - Major GC时间阈值 (1000ms)
     - Old区GC由于需要处理整个堆，暂停时间会更长
     - 超过1s的暂停时间对大多数在线服务来说都是不可接受的

2. **动态阈值 (历史平均 + 20%)**
   - 基于实际运行数据动态调整
   - 剔除异常值后计算平均值
   - 允许20%的增长容忍度
   - 更好地适应不同服务的GC特征

## 检测结果示例

1. **使用固定阈值时**:
```json
{
    "status": true,
    "maxMinorGcTime": 250,
    "maxMajorGcTime": 1500,
    "detectedIssues": [
        "Minor GC时间过长: 250ms (阈值200ms)",
        "Major GC时间过长: 1500ms (阈值1000ms)"
    ],
    "detectionReason": "检测到 2 个问题: Minor GC时间过长: 250ms (阈值200ms); Major GC时间过长: 1500ms (阈值1000ms)"
}
```

2. **使用动态阈值时**:
```json
{
    "status": true,
    "maxMinorGcTime": 180,
    "maxMajorGcTime": 900,
    "detectedIssues": [
        "Minor GC时间异常增长: 180ms (历史平均120.5ms，动态阈值144.6ms)",
        "Major GC时间异常增长: 900ms (历史平均600.0ms，动态阈值720.0ms)"
    ],
    "detectionReason": "检测到 2 个问题: Minor GC时间异常增长: 180ms (历史平均120.5ms，动态阈值144.6ms); Major GC时间异常增长: 900ms (历史平均600.0ms，动态阈值720.0ms)"
}
```

## 优化建议

1. **Minor GC时间过长**:
   - 调整Young区大小（-Xmn）
   - 检查对象分配模式
   - 减少跨代引用
   - 考虑使用并行GC

2. **Major GC时间过长**:
   - 调整Old区大小（-Xmx和-Xms）
   - 检查是否存在内存泄漏
   - 优化对象生命周期
   - 考虑使用G1或ZGC 