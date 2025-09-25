# Uneven API Usage Detection - API使用不均检测

## 📋 异味定义

**Uneven API Usage（API使用不均,针对微服务下所有实例作统一整合检测,汇总所有API调用次数）** 是指在微服务实例中，外部客户端或服务内部对于某些API的依赖过于集中，而其他API则沉寂不受欢迎的现象。这种不均匀的使用模式可能会导致性能瓶颈、安全风险和维护困难。

### 异味特征
- **热点集中**：少数API承担了大部分的调用量
- **冷门API**：大量API几乎不被使用或使用频率极低
- **分布失衡**：API调用分布严重偏离均匀分布
- **资源浪费**：冷门API占用系统资源但价值有限

## 🎯 检测思路

### 数据驱动的检测方法

基于**真实的API调用数据**进行检测，使用时间窗口内的`instanceAPICallNumMap`统计信息，通过多维度分析识别API使用分布的不均衡模式。

### 核心检测维度

```java
检测维度：
1. 极端集中分析 - 检测单个API是否占用绝大部分调用（>80%）
2. 高度集中分析 - 检测单个API是否过度集中（>60%）
3. 帕累托异常检测 - 检测前20%API是否占用过多调用（>95%）
4. 集中度评分 - 基于基尼系数的分布集中度评估（>70分）
```

## 🔍 检测算法

### 数据来源
- **数据源**：AVGExternalMetrics中的`instanceAPICallNumMap`
- **时间范围**：整个时间窗口内的聚合数据
- **检测粒度**：针对整个微服务进行聚合检测（将所有实例的API调用数据合并）
- **重要说明**：只统计有调用记录的API，调用次数为0的API不会出现在统计中

### 核心算法流程

```java
1. 数据聚合与预处理
   └── 聚合所有实例的instanceAPICallNumMap
   └── 将相同API在不同实例上的调用次数累加
   └── 统计微服务级别的API总数、总调用次数、平均调用次数
   └── 识别最热门API及其调用信息

2. 集中度特征计算
   └── 最热门API占比 = 最热API调用次数 / 总调用次数
   └── 帕累托比例 = 前20%API调用次数 / 总调用次数
   └── 集中度评分 = 基于基尼系数的0-100分评分
   └── API分布详情 = 前10名API的详细分布

3. 智能检测规则
   └── 极端集中检测（>80%）
   └── 高度集中检测（>60%）
   └── 帕累托异常检测（>95%）
   └── 集中度评分检测（>70分）

4. 综合判定
   └── 强条件：极端集中 OR 帕累托严重违反 → 异常
   └── 一般条件：满足任意2个检测条件 → 异常
```

## 📊 检测规则详解

### 主要检测条件

| 检测维度 | 阈值 | 说明 | 业务含义 |
|----------|------|------|----------|
| **极端集中** | >80% | 单个API占总调用量比例 | 严重的热点集中问题 |
| **高度集中** | >60% | 单个API占总调用量比例 | 明显的集中度问题 |
| **帕累托异常** | >95% | 前20%API占用调用量比例 | 严重违反80/20法则 |
| **高集中度评分** | >70分 | 基于基尼系数的集中度评分 | 整体分布不均衡 |

### 检测逻辑

```java
// 智能判定规则
强条件 = 极端集中(>80%) OR 帕累托严重违反(>95%)
一般条件 = 满足的条件数量 >= 2

boolean isUnevenUsage = 强条件 OR 一般条件;

// 前置条件
if (API总数 < 3) {
    return false; // API数量太少，无法检测不均衡
}
```

## 💡 算法实现

### 数据结构

```java
public class UnevenApiUsageItem {
    private Boolean status;                    // 是否检测到不均衡
    private Integer instanceCount;             // 参与统计的实例数量
    private String instanceNames;              // 参与统计的实例名称列表（逗号分隔）
    private Integer totalApiCount;             // 总API数量（去重后）
    private Integer totalCallCount;            // 总调用次数（所有实例聚合）
    private String hottestApiName;             // 最热门API名称
    private Integer hottestApiCallCount;       // 最热门API调用次数
    private Double hottestApiRatio;            // 最热门API占比
    private Double top20PercentApiRatio;       // 前20%API的调用占比
    private Double avgCallsPerApi;             // 平均每个API的调用次数
    private Double concentrationScore;         // 集中度评分（0-100分）
    private String triggeredRules;             // 触发的检测规则
}
```

### 核心算法

```java
// 1. 数据聚合
private Map<String, Integer> aggregateApiCallData(Map<String, SvcExternalMetricsRes> avgExternalMetrics) {
    Map<String, Integer> aggregatedMap = new HashMap<>();
    
    for (SvcExternalMetricsRes metrics : avgExternalMetrics.values()) {
        for (Map.Entry<String, Integer> apiEntry : metrics.getInstanceAPICallNumMap().entrySet()) {
            String apiName = apiEntry.getKey();
            Integer callCount = apiEntry.getValue();
            aggregatedMap.merge(apiName, callCount, Integer::sum); // 累加调用次数
        }
    }
    return aggregatedMap;
}

// 2. 微服务级别分析
private UnevenApiUsageItem analyzeApiUsageDistribution(Map<String, Integer> aggregatedApiCallMap) {
    // 基于聚合后的数据进行分析
    int totalApiCount = aggregatedApiCallMap.size();
    int totalCallCount = aggregatedApiCallMap.values().stream().mapToInt(Integer::intValue).sum();
    
    // 找出最热门API
    Map.Entry<String, Integer> hottestApi = aggregatedApiCallMap.entrySet().stream()
        .max(Map.Entry.comparingByValue()).orElse(null);
    
    // 计算帕累托分布和集中度评分
    double top20PercentRatio = calculateTop20PercentRatio(aggregatedApiCallMap, totalCallCount);
    double concentrationScore = calculateConcentrationScore(aggregatedApiCallMap, totalCallCount);
    
    // 应用检测规则
    ApiUsageAnalysisResult result = detectUnevenApiUsage(item);
    
    return item;
}
```

### 集中度评分算法（基于基尼系数）

```java
private double calculateConcentrationScore(Map<String, Integer> apiCallNumMap, int totalCallCount) {
    List<Integer> sortedCalls = apiCallNumMap.values().stream().sorted().toList();
    int n = sortedCalls.size();
    double giniSum = 0.0;
    
    for (int i = 0; i < n; i++) {
        giniSum += (2 * (i + 1) - n - 1) * sortedCalls.get(i);
    }
    
    double giniCoeff = giniSum / (n * totalCallCount);
    return Math.abs(giniCoeff) * 100; // 转换为0-100分
}
```

## ✅ 优点

### 1. **真实数据驱动**
- **准确反映实际情况**：只统计真正被调用的API，无0值干扰
- **业务意义明确**：最热门API名称、调用次数等信息直观有用
- **分布详情完整**：提供前10名API的详细调用分布

### 2. **科学的检测方法**
- **基尼系数应用**：使用经济学中的基尼系数衡量分布不均程度
- **帕累托原则检验**：科学的80/20法则检验
- **智能判定逻辑**：强条件+一般条件的组合判定

### 3. **丰富的检测信息**
- **最热API识别**：直接指出问题API的名称和占比
- **触发规则记录**：明确记录哪些规则被触发
- **分布详情展示**：提供完整的API调用分布信息
- **集中度量化**：0-100分的集中度评分

### 4. **智能化的阈值设计**
- **分层检测**：极端集中（80%）vs 高度集中（60%）
- **强弱条件**：重要条件单独触发，一般条件组合触发
- **业务可理解**：所有阈值都有明确的业务含义

## ❌ 缺点

### 1. **统计盲点**
- **零调用API忽略**：无法识别完全未被使用的API
- **冷门API缺失**：可能存在但调用极少的API被忽略
- **API覆盖率盲点**：无法评估API的整体覆盖情况

### 2. **基尼系数局限性**
- **小样本偏差**：API数量较少时基尼系数可能不准确
- **计算复杂性**：相比简单统计方法计算稍复杂
- **理解门槛**：需要一定的统计学背景才能理解

## 📊 检测示例

### 正常API使用分布（微服务级别聚合）
```
微服务API调用分布聚合统计: 
- 参与统计实例数: 3个
- 实例名称列表: user-service-001, user-service-002, user-service-003
- API调用分布: 
  * UserController#getUser: 540次 (30%) [实例1:180 + 实例2:180 + 实例3:180]
  * UserController#updateUser: 450次 (25%) [实例1:150 + 实例2:150 + 实例3:150]
  * UserController#listUsers: 360次 (20%) [实例1:120 + 实例2:120 + 实例3:120]
  * UserController#createUser: 270次 (15%) [实例1:90 + 实例2:90 + 实例3:90]
  * UserController#deleteUser: 180次 (10%) [实例1:60 + 实例2:60 + 实例3:60]

分析结果:
- 参与统计实例数: 3个
- 总API数量: 5个（去重后）
- 总调用次数: 1800次（所有实例聚合）
- 最热API占比: 30% < 80% ✗ (极端集中)
- 最热API占比: 30% < 60% ✗ (高度集中)
- 帕累托比例: 55% < 95% ✗ (前20%API占比)
- 集中度评分: 45分 < 70分 ✗
满足条件数: 0/4 → 微服务API分布相对均衡
```

### 异常API使用分布（微服务级别聚合）
```
微服务API调用分布聚合统计:
- 参与统计实例数: 3个
- 实例名称列表: user-service-001, user-service-002, user-service-003
- API调用分布:
  * UserController#getUser: 2550次 (85%) [实例1:850 + 实例2:850 + 实例3:850]
  * UserController#updateUser: 240次 (8%) [实例1:80 + 实例2:80 + 实例3:80]
  * UserController#listUsers: 120次 (4%) [实例1:40 + 实例2:40 + 实例3:40]
  * UserController#createUser: 60次 (2%) [实例1:20 + 实例2:20 + 实例3:20]
  * UserController#deleteUser: 30次 (1%) [实例1:10 + 实例2:10 + 实例3:10]

分析结果:
- 参与统计实例数: 3个
- 总API数量: 5个（去重后）
- 总调用次数: 3000次（所有实例聚合）
- 最热门API: UserController#getUser (2550次调用)
- 最热API占比: 85% > 80% ✓ (极端集中)
- 帕累托比例: 93% < 95% ✗ (前20%API占比)
- 集中度评分: 78分 > 70分 ✓
强条件满足: 极端集中 → 微服务API使用严重不均
触发规则: 极端集中(85.0%); 高集中度评分(78.0)
```

## 📈 总结

优化后的API使用不均检测是一个**微服务级别聚合、真实数据驱动、科学方法支撑**的智能检测方案。通过将所有实例的API调用数据聚合到微服务层面，提供了更全面的API使用分布视图，专注于真正有业务价值的指标：最热门API识别、帕累托分析、基尼系数评分等。

该方案具有**信息全面、检测准确、业务导向**的优点，能够从微服务整体角度准确识别API使用的集中度问题，并提供详细的聚合分析信息帮助业务决策。对于**微服务架构的API治理、整体性能优化、架构重构决策**等实际业务需求来说，是一个更加实用、全面和精准的解决方案。 