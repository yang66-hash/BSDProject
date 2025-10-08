# 不均匀业务逻辑处理检测实现说明

## 检测目标

检测服务实例是否存在不均匀的业务逻辑处理问题。如果在某个时间窗口内，处理外部请求时，所调用的方法次数存在显著差异，可能导致某些服务方法的高负载，而其他方法可能则处于闲置状态。这种不均匀性可能引发资源的不合理使用、性能瓶颈以及系统的不稳定性。

## 检测维度

本检测系统采用**多维度分析**的方式，评估业务逻辑处理的均匀性：

### 检测维度概览

| 维度 | 检测目标 | 核心指标 | 阈值 | 作用 |
|------|----------|----------|------|------|
| **极端集中** | 单个方法占比 | 最热门方法调用占比 | >80% | 识别严重的热点方法 |
| **高度集中** | 单个方法占比 | 最热门方法调用占比 | >60% | 识别明显的集中问题 |
| **帕累托异常** | 方法分布 | 前20%方法调用占比 | >95% | 识别严重的分布不均 |
| **集中度评分** | 整体分布 | 基尼系数评分 | >70分 | 评估整体均匀性 |

### 检测维度关系图

```
                不均匀业务逻辑检测
                         |
        ┌────────────────┼────────────────┐
        |                |                |
    强条件检测      一般条件检测      分布分析
   (任一满足)      (满足两项)      (辅助判断)
        |                |                |
   ┌────┴────┐     ┌────┴────┐     ┌────┴────┐
   |极端集中 |     |高度集中 |     |集中度评分|
   |帕累托异常|     |帕累托比例|     |方法分布 |
   └─────────┘     └─────────┘     └─────────┘
        |                |                |
        └────────────────┼────────────────┘
                         |
                    综合判定
```

## 检测逻辑

1. **数据聚合**:
```java
// 聚合所有实例的方法调用数据
for (BusinessMetricsRes metrics : instanceData) {
    for (Map.Entry<String, Long> methodEntry : metrics.getBusinessCallMap().entrySet()) {
        String methodName = methodEntry.getKey();
        Long callCount = methodEntry.getValue();
        aggregatedMap.merge(methodName, callCount, Long::sum);
    }
}
```

2. **关键指标计算**:
```java
// 最热门方法占比
double hottestMethodRatio = (double) hottestMethod.getValue() / totalCalls;

// 帕累托指标
int top20MethodCount = Math.max(1, (int) Math.ceil(totalMethods * 0.2));
double top20Ratio = (double) top20Calls / totalCalls;

// 集中度评分（基于基尼系数）
double concentrationScore = calculateConcentrationScore(sortedMethods, totalCalls);
```

3. **检测规则判定**:
```java
// 强条件：极端集中 或 帕累托异常
boolean strongCondition = hottestMethodRatio > 0.8 || top20Ratio > 0.95;

// 一般条件：满足任意两个条件
boolean weakCondition = triggeredRules.size() >= 2;

boolean isDetected = strongCondition || weakCondition;
```

## 阈值选择依据

1. **极端集中 (80%)**
   - 单个方法占用超过80%的调用量
   - 表明存在严重的热点方法问题
   - 需要立即关注和优化

2. **高度集中 (60%)**
   - 单个方法占用超过60%的调用量
   - 表明存在明显的方法调用不均衡
   - 建议进行方法设计优化

3. **帕累托异常 (95%)**
   - 前20%的方法占用超过95%的调用量
   - 严重违反80/20法则
   - 需要重新评估业务流程设计

4. **集中度评分 (70分)**
   - 基于基尼系数的0-100分评分
   - 超过70分表示分布严重不均
   - 需要关注整体架构设计

## 检测结果示例

```json
{
    "status": true,
    "maxCallCount": 2500,
    "minCallCount": 30,
    "avgCallCount": 600.0,
    "callCountRange": 0.988,
    "highLoadMethods": [
        "UserService#processOrder: 2500次 (83.3%)",
        "UserService#createUser: 240次 (8.0%)",
        "UserService#updateUser: 120次 (4.0%)",
        "UserService#queryUser: 90次 (3.0%)",
        "UserService#deleteUser: 50次 (1.7%)"
    ],
    "detectionReason": "检测到不均匀业务逻辑处理问题 [参与统计实例数:3] [总方法数:5] [总调用量:3000] [触发规则:极端集中(83.3%); 帕累托异常(96.0%); 高集中度评分(78.5)]"
}
```

## 优化建议

1. **业务流程优化**:
   - 梳理业务流程，识别热点方法
   - 考虑业务流程重构
   - 实现负载分散

2. **方法设计优化**:
   - 拆分大方法为小方法
   - 提取公共逻辑
   - 实现方法级缓存

3. **架构优化**:
   - 引入消息队列削峰
   - 实现方法级限流
   - 考虑服务拆分

4. **监控优化**:
   - 对高负载方法单独监控
   - 设置方法级告警
   - 定期评估方法分布 