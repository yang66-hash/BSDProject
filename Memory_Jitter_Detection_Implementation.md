# 内存抖动检测实现说明

## 检测目标

检测服务实例是否存在内存抖动问题。内存抖动表现为堆内存使用量在短时间内频繁波动，通常伴随着频繁的GC事件，这会导致服务响应时间增加、系统吞吐量降低，影响服务稳定性。

## 检测维度

本检测系统采用**三维度综合检测**的方式，从不同角度评估内存抖动情况：

### 检测维度概览

| 维度 | 检测目标 | 核心指标 | 阈值 | 作用 |
|------|----------|----------|------|------|
| **频繁GC** | GC发生频率 | 复用FrequentGcService | 复用已有阈值 | 识别频繁GC |
| **长时间GC** | GC执行时间 | 复用LongTimeGcService | 复用已有阈值 | 识别GC效率问题 |
| **内存波动** | 内存使用情况 | 使用率和提交率波动 | >10% | 识别内存不稳定 |

### 检测维度关系图

```
                    内存抖动检测
                         |
        ┌────────────────┼────────────────┐
        |                |                |
     频繁GC           长时间GC          内存波动
  (复用检测)        (复用检测)     (使用率和提交率)
        |                |                |
        └────────────────┼────────────────┘
                         |
                    综合判定
             (GC异常 && 内存波动)
```

## 检测逻辑

1. **GC异常检测**:
```java
// 1.1 检测频繁GC
DetectionResItem frequentGCResult = frequentGcService.frequentGcDetect(requestItem);
FrequentGcItem frequentGCItem = context.getInstanceStatus().get(instanceKey);
if (frequentGCItem != null && frequentGCItem.getStatus()) {
    hasGCIssue = true;
    detectedIssues.addAll(frequentGCItem.getDetectedIssues());
}

// 1.2 检测GC时间过长
DetectionResItem longGCResult = longTimeGcService.longTimeGcDetect(requestItem);
LongTimeGcItem longGCItem = context.getInstanceStatus().get(instanceKey);
if (longGCItem != null && longGCItem.getStatus()) {
    hasGCIssue = true;
    detectedIssues.addAll(longGCItem.getDetectedIssues());
}
```

2. **内存波动检测**:
```java
// 2.1 检查堆内存使用率波动
double heapUsedRate = (double)summary.getHeapUsed() / summary.getHeapMaxed();
double heapUsedRateRange = maxHeapUsedRate - minHeapUsedRate;
if (heapUsedRateRange > HEAP_USED_RATE_THRESHOLD) {
    hasMemoryJitter = true;
}

// 2.2 检查堆内存提交率波动
double heapCommittedRate = (double)summary.getHeapCommitted() / summary.getHeapMaxed();
double heapCommittedRateRange = maxHeapCommittedRate - minHeapCommittedRate;
if (heapCommittedRateRange > HEAP_USED_RATE_THRESHOLD) {
    hasMemoryJitter = true;
}
```

## 阈值选择依据

1. **GC异常阈值**
   - 复用频繁GC检测的阈值
   - 复用长时间GC检测的阈值

2. **内存波动阈值 (10%)**
   - 堆内存使用率波动超过10%说明内存使用不稳定
   - 堆内存提交率波动超过10%说明JVM频繁调整堆大小
   - 可能原因：
     - 大对象频繁分配/回收
     - 内存泄漏导致的周期性Full GC
     - 缓存使用不当
     - JVM堆空间配置不合理

## 检测结果示例

```json
{
    "status": true,
    "heapUsedRateRange": 0.15,
    "maxHeapUsedRate": 0.85,
    "minHeapUsedRate": 0.70,
    "gcFrequency": 4,
    "avgGcTime": 150.5,
    "detectedIssues": [
        "Minor GC频率过高: 3次/分钟",
        "Major GC时间过长: 1500ms (阈值1000ms)",
        "堆内存使用率波动过大: 15.0% (最大85.0%, 最小70.0%)",
        "堆内存提交率波动过大: 12.0% (最大92.0%, 最小80.0%)"
    ],
    "detectionReason": "检测到 4 个问题: Minor GC频率过高: 3次/分钟; Major GC时间过长: 1500ms (阈值1000ms); 堆内存使用率波动过大: 15.0% (最大85.0%, 最小70.0%); 堆内存提交率波动过大: 12.0% (最大92.0%, 最小80.0%)"
}
```

## 优化建议

1. **GC问题优化**:
   - 参考频繁GC和长时间GC的优化建议
   - 调整堆内存分代大小
   - 优化对象分配模式
   - 检查内存泄漏
   - 评估GC算法选择

2. **内存波动优化**:
   - 优化缓存策略
     - 合理设置缓存大小
     - 使用软引用或弱引用
     - 实现缓存淘汰机制
   - 控制大对象分配
     - 使用对象池
     - 实现内存复用
     - 避免频繁创建大数组
   - 调整JVM参数
     - 设置合理的初始堆大小（-Xms）
     - 设置合理的最大堆大小（-Xmx）
     - 考虑禁用自动堆大小调整 