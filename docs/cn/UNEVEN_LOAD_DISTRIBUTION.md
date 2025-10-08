# 不均衡负载检测算法说明与使用示例

## 📋 概述

不均衡负载检测算法用于识别微服务实例中存在的资源利用不均衡问题。该算法主要检测以下异常模式：

**检测条件：CPU/RAM使用率高于实例平均值(1+20%)，但请求数量低于平均值(1-20%)，延时高于平均值(1+20%)**

## 🔧 算法逻辑

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

## 📊 检测条件详解

当一个实例**同时满足**以下四个条件时，被判定为不均衡负载：

1. ✅ **CPU使用率高于平均值+20%**
2. ✅ **RAM使用率高于平均值+20%**  
3. ✅ **请求数量低于平均值-20%**
4. ✅ **延时高于平均值+20%**

## 💻 代码实现示例

### 1. 调用检测服务

```java
@RestController
@RequestMapping("/dynamic")
public class DynamicController {
    
    @Autowired
    private UnevenLoadDistributionService unevenLoadDistributionService;
    
    @PostMapping("/uneven-load-distribution")
    public ResponseDTO<String> detectUnevenLoadDistribution(@RequestBody RequestItem requestItem) {
        DetectionResItem result = unevenLoadDistributionService.unevenLoadDistributionDetect(requestItem);
        // 处理检测结果
        return ResponseDTO.success("检测完成");
    }
}
```

### 2. 检测结果处理

```java
// 获取检测结果
UnevenLoadDistributionContext context = (UnevenLoadDistributionContext) result.getContext();

if (context.getStatus()) {
    System.out.println("发现不均衡负载！");
    
    // 遍历每个实例的检测结果
    Map<String, UnevenLoadDisItem> instanceStatus = context.getInstanceStatus();
    for (Map.Entry<String, UnevenLoadDisItem> entry : instanceStatus.entrySet()) {
        String instanceName = entry.getKey();
        UnevenLoadDisItem item = entry.getValue();
        
        if (item.getStatus()) {
            System.out.printf("实例 %s 存在不均衡负载:\n", instanceName);
            System.out.printf("  CPU使用率: %.2f%%\n", item.getPct());
            System.out.printf("  内存使用: %.0f MB\n", item.getMemoryUsed() / (1024.0 * 1024.0));
            System.out.printf("  平均延时: %.2f ms\n", item.getAvgLatency());
        }
    }
} else {
    System.out.println("所有实例负载均衡正常");
}
```

## 🎯 实际检测场景

### 场景1：正常负载分布

```
服务: user-service (2个实例)

窗口平均指标:
- CPU平均值: 55.0%
- 内存平均值: 556 MB  
- 请求数平均值: 1100
- 延时平均值: 110.0 ms

检测阈值:
- CPU阈值: 66.0% (55% * 1.2)
- 内存阈值: 667 MB (556MB * 1.2)
- 请求数阈值: 880 (1100 * 0.8)  
- 延时阈值: 132.0 ms (110ms * 1.2)

实例检测结果:
┌─────────────┬─────────┬──────────┬─────────┬──────────┬────────┐
│ 实例        │ CPU     │ 内存     │ 请求数  │ 延时     │ 结果   │
├─────────────┼─────────┼──────────┼─────────┼──────────┼────────┤
│ instance-1  │ 52%(✅) │ 500MB(✅)│ 1050(✅)│ 105ms(✅)│ ✅正常 │
│ instance-2  │ 58%(✅) │ 612MB(✅)│ 1150(✅)│ 115ms(✅)│ ✅正常 │
└─────────────┴─────────┴──────────┴─────────┴──────────┴────────┘
```

### 场景2：检测到不均衡负载

```
服务: order-service (3个实例)

窗口平均指标:
- CPU平均值: 45.0%
- 内存平均值: 400 MB
- 请求数平均值: 800  
- 延时平均值: 90.0 ms

检测阈值:
- CPU阈值: 54.0% (45% * 1.2)
- 内存阈值: 480 MB (400MB * 1.2)
- 请求数阈值: 640 (800 * 0.8)
- 延时阈值: 108.0 ms (90ms * 1.2)

实例检测结果:
┌─────────────┬─────────┬──────────┬─────────┬──────────┬────────────────┐
│ 实例        │ CPU     │ 内存     │ 请求数  │ 延时     │ 结果           │
├─────────────┼─────────┼──────────┼─────────┼──────────┼────────────────┤
│ instance-1  │ 40%(✅) │ 350MB(✅)│ 850(✅) │ 85ms(✅) │ ✅正常         │
│ instance-2  │ 42%(✅) │ 380MB(✅)│ 820(✅) │ 88ms(✅) │ ✅正常         │
│ instance-3  │ 75%(⚠️) │ 600MB(⚠️)│ 500(⚠️) │ 130ms(⚠️)│ 🚨不均衡负载   │
└─────────────┴─────────┴──────────┴─────────┴──────────┴────────────────┘

⚠️ 警告: instance-3 存在不均衡负载
- CPU高于阈值: 75% > 54%
- 内存高于阈值: 600MB > 480MB  
- 请求数低于阈值: 500 < 640
- 延时高于阈值: 130ms > 108ms
```

## 🔍 算法特点

### 优势
1. **相对比较**: 基于同服务实例间的相对比较，避免绝对阈值的局限性
2. **多维度检测**: 综合CPU、内存、请求量、延时四个维度
3. **适应性强**: 自动适应不同服务的负载特征
4. **误报率低**: 需要同时满足四个条件，减少误判

### 适用场景
- **负载均衡异常**: 某些实例获得过少请求但资源消耗过高
- **性能瓶颈识别**: 识别处理效率低下的实例
- **资源浪费检测**: 发现资源利用率与工作负载不匹配的情况

## ⚙️ 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| coefficient | 0.2 | 容差系数(20%) |
| 最小实例数 | 2 | 至少需要2个实例才进行检测 |

## 📤 输出结果

### UnevenLoadDisItem 字段
- `status`: 是否存在不均衡负载
- `pct`: 当前CPU使用率
- `memoryUsed`: 当前内存使用量  
- `avgLatency`: 当前平均延时