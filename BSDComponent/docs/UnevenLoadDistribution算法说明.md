# 不均衡负载检测算法说明

## 概述

不均衡负载检测算法用于识别微服务实例中存在的资源利用不均衡问题。该算法主要检测以下异常模式：

**检测条件：CPU/RAM使用率高于实例平均值(1+20%)，但请求数量低于平均值(1-20%)，延时高于平均值(1+20%)**

## 算法逻辑

### 1. 数据收集

算法需要以下两类指标数据：

#### 内部指标 (SvcMetricsRes)
- **CPU使用率**: `JVMSummaryRes.cpuProPCT` - 进程CPU使用百分比
- **内存使用量**: `JVMSummaryRes.heapUsed` - JVM堆内存使用量(字节)

#### 外部指标 (SvcExternalMetricsRes)  
- **请求数量**: `requestCount` - 时间间隔内的请求总数
- **平均延时**: `avgLatency` - 所有请求的平均响应时间(毫秒)

### 2. 平均值计算

算法计算所有实例的平均指标：

```java
// CPU平均值
avgCpuPct = allInstances.stream()
    .mapToDouble(JVMSummaryRes::getCpuProPCT)
    .average()

// 内存平均值  
avgMemoryUsed = allInstances.stream()
    .mapToLong(JVMSummaryRes::getHeapUsed)
    .average()

// 请求数平均值
avgRequestCount = allInstances.stream()
    .mapToInt(SvcExternalMetricsRes::getRequestCount)
    .average()

// 加权平均延时
avgLatency = sum(latency * requestCount) / totalRequests
```

### 3. 阈值计算

基于平均值计算检测阈值（20%容差）：

```java
double coefficient = 0.2;

// 高资源使用阈值
cpuThreshold = avgCpuPct * (1 + coefficient)      // +20%
memoryThreshold = avgMemoryUsed * (1 + coefficient) // +20%

// 低请求数阈值  
requestThreshold = avgRequestCount * (1 - coefficient) // -20%

// 高延时阈值
latencyThreshold = avgLatency * (1 + coefficient)     // +20%
```

### 4. 实例检测

对每个实例进行四个条件的检测：

```java
boolean isUnevenLoad = 
    (currentCpuPct > cpuThreshold) &&           // CPU高
    (currentMemoryUsed > memoryThreshold) &&   // 内存高  
    (currentRequestCount < requestThreshold) && // 请求低
    (currentLatency > latencyThreshold);       // 延时高
```

## 检测示例

### 场景1：正常实例
```
平均值: CPU=55%, 内存=556MB, 请求=1100个, 延时=110ms
阈值:   CPU=66%, 内存=667MB, 请求=880个,  延时=132ms

实例A: CPU=45%, 内存=400MB, 请求=1100个, 延时=95ms
结果:   正常 (所有指标都在合理范围内)
```

### 场景2：不均衡负载实例
```
平均值: CPU=55%, 内存=556MB, 请求=1100个, 延时=110ms  
阈值:   CPU=66%, 内存=667MB, 请求=880个,  延时=132ms

实例B: CPU=80%, 内存=800MB, 请求=800个, 延时=150ms
结果:   不均衡负载 ✓
- CPU高: 80% > 66% ✓
- 内存高: 800MB > 667MB ✓  
- 请求低: 800 < 880 ✓
- 延时高: 150ms > 132ms ✓
```

## 算法特点

### 优势
1. **相对比较**: 基于同服务实例间的相对比较，避免绝对阈值的局限性
2. **多维度检测**: 综合CPU、内存、请求量、延时四个维度
3. **适应性强**: 自动适应不同服务的负载特征
4. **误报率低**: 需要同时满足四个条件，减少误判

### 适用场景
- **负载均衡异常**: 某些实例获得过少请求但资源消耗过高
- **性能瓶颈识别**: 识别处理效率低下的实例
- **资源浪费检测**: 发现资源利用率与工作负载不匹配的情况

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| coefficient | 0.2 | 容差系数(20%) |
| 最小实例数 | 2 | 至少需要2个实例才进行检测 |

## 输出结果

### UnevenLoadDisItem 字段
- `status`: 是否存在不均衡负载
- `pct`: 当前CPU使用率
- `memoryUsed`: 当前内存使用量  
- `avgLatency`: 当前平均延时

### 日志输出
- **INFO**: 实例平均指标信息
- **WARN**: 检测到不均衡负载的实例详情
- **DEBUG**: 各条件的判断结果

## 使用建议

1. **监控频率**: 建议每5-10分钟检测一次
2. **阈值调整**: 可根据业务特性调整coefficient参数
3. **结合其他指标**: 建议结合网络I/O、磁盘I/O等指标综合判断
4. **告警策略**: 连续多次检测为不均衡负载时才触发告警 