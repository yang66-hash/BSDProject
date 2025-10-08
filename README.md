# SmellDoc - 微服务异味检测与分析平台

## 📋 项目概述

SmellDoc是一个基于Elastic Stack的**微服务异味检测与分析平台**，通过结合运行时遥测数据（指标、追踪、日志）与静态代码指标，实现对微服务架构中各种反模式、性能问题和架构问题的智能检测与可视化。

> **核心价值：**
> **Step 1 - 配置阶段：** 部署代理 → 收集运行时数据  
> **Step 2 - 数据收集：** 预处理与整合信号 → Elasticsearch  
> **Step 3 - 检测与可视化：** 异味检测 → Kibana仪表板

![系统架构图](docs/img/architecture.jpg)

## 🏗️ 系统架构

### 1) 配置层 (Configuration Layer)
- **Elastic APM Java Agent** (`elastic-apm-agent.jar`) - 收集运行时指标、追踪和日志
- **自定义业务收集器 (CBC)** - 收集业务层指标和自定义监控数据
- **APM Server** - 实时接收和预处理遥测数据

### 2) 数据收集层 (Data Collection Layer)
- **重整合收集器 (RIC)** - 去重并聚合运行时指标，提供标准化数据格式
- **Elasticsearch** - 存储所有处理后的监控数据和检测结果
- **Redis缓存** - 提供高性能的数据缓存和临时存储

### 3) 检测与可视化层 (Detection & Visualization Layer)
- **静态分析组件** - 从微服务源码中提取静态代码指标和架构信息
- **BSD组件** - 融合静态指标与实时运行时数据进行异味检测
- **Kibana插件** - 监控BSD状态并可视化异味发现和趋势分析

## 🔧 技术栈

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Spring Boot** | 3.2.4 | 应用框架 | 微服务架构基础框架 |
| **Java** | 17 | 开发语言 | 现代Java特性支持 |
| **MySQL** | 8.0 | 关系数据库 | 元数据和配置存储 |
| **Elasticsearch** | 8.14.3 | 搜索引擎 | 监控数据和检测结果存储 |
| **Redis** | 6.0+ | 缓存系统 | 高性能缓存和会话管理 |
| **Maven** | 3.6+ | 构建工具 | 项目构建和依赖管理 |
| **Knife4j** | 4.4.0 | API文档 | Swagger增强文档工具 |
| **JavaParser** | 3.24.2 | 代码分析 | 静态代码分析和AST解析 |
| **JGit** | 7.2.0 | 版本控制 | Git仓库操作和分析 |
| **Kubernetes Client** | 20.0.0 | 容器编排 | K8s集群管理和服务发现 |

## 🚀 核心功能

### 🔍 静态分析功能 (9种异味类型)

| 检测类型 | API端点 | 描述 | 检测目标 |
|----------|---------|------|----------|
| **硬编码端点** | `/com-inter/hardcoded-endpoints` | 检测硬编码的服务端点 | 配置缺乏灵活性，维护困难 |
| **功能分散** | `/decomposition/scattered-functionality` | 识别跨服务分散的功能 | 服务边界不清晰，重复实现 |
| **错误服务切分** | `/decomposition/wrong-cuts` | 检测不正确的服务分解 | 技术分层而非业务功能分解 |
| **贪婪服务** | `/decomposition/microservice-greedy` | 识别职责过重的服务 | 违反单一职责原则 |
| **无API版本控制** | `/api/no-api-versioning` | 检测缺失的API版本控制 | API兼容性和演进管理 |
| **ESB使用** | `/infrastructure/esb-usage` | 识别企业服务总线过度使用 | 过度集中化架构 |
| **无API网关** | `/infrastructure/no-api-gateway` | 检测缺失的API网关 | 架构设计缺陷 |
| **中心化依赖** | `/infrastructure/hub-like-dependency` | 识别依赖中心化问题 | 单点故障风险 |
| **共享库问题** | `/infrastructure/shared-libraries` | 检测有问题的共享库使用 | 依赖管理复杂性 |

### 📊 动态分析功能 (17种异味类型)

#### 1️⃣ 基础运行时异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 1 | `POST /dynamic/fragile-service` | Fragile Service | 脆弱服务检测 | 失败率、延迟、吞吐量 |
| 2 | `POST /dynamic/uneven-load-distribution` | Uneven Load Distribution | 负载分布不均检测 | CPU、内存、请求量、延时 |
| 3 | `POST /dynamic/inconsistent-service-response` | Inconsistent Service Response | 服务响应不一致检测 | 延迟波动、失败率变化 |

#### 2️⃣ 资源和性能异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 4 | `POST /dynamic/resource-waste` | Resource Waste | 资源浪费检测 | CPU、内存利用率 |
| 5 | `POST /dynamic/call-rate-anomaly` | Call Rate Anomaly | 调用频率异常检测 | 请求量变化、历史对比 |
| 6 | `POST /dynamic/uneven-api-usage` | Uneven API Usage | API使用不均衡检测 | API调用分布、基尼系数 |

#### 3️⃣ 服务通信异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 7 | `POST /dynamic/chatty-service` | Chatty Service | 过度通信服务检测 | 服务间调用频率 |
| 8 | `POST /dynamic/service-chain` | Service Chain | 服务链过长检测 | 调用链深度分析 |

#### 4️⃣ 数据库和查询异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 9 | `POST /dynamic/high-frequency-slow-queries` | High Frequency Of Slow Queries | 高频慢查询检测 | 慢查询比例、执行时间 |
| 10 | `POST /dynamic/n+1-queries` | N+1 Queries | N+1查询检测 | 查询数量、关联查询 |

#### 5️⃣ 内存和GC异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 11 | `POST /dynamic/frequent-gc` | Frequent GC | 频繁GC检测 | 四维度GC分析 |
| 12 | `POST /dynamic/long-time-gc` | Long Time GC | 长时间GC检测 | GC暂停时间分析 |
| 13 | `POST /dynamic/memory-jitter-of-service` | Memory Jitter Of Service | 内存抖动检测 | 内存使用波动 |

#### 6️⃣ 逻辑处理异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 14 | `POST /dynamic/uneven-logic-processing` | Uneven Logic Processing | 不均衡逻辑处理检测 | 方法调用分布 |
| 15 | `POST /dynamic/unnecessary-processing` | Unnecessary Processing | 不必要处理检测 | 重复操作、冗余计算 |

#### 7️⃣ 系统稳定性异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 16 | `POST /dynamic/falling-dominoes` | Falling Dominoes | 多米诺骨牌效应检测 | 级联故障分析 |
| 17 | `POST /dynamic/the-ramp` | The Ramp | 坡道效应检测 | 性能趋势分析 |

### 📈 数据收集与监控

- **业务指标收集** - 收集业务特定指标和KPI数据
- **内部指标监控** - 监控JVM和应用内部性能指标  
- **链路追踪收集** - 收集分布式追踪数据和调用链信息
- **API指标监控** - 监控API使用模式、性能指标和错误率
- **实时数据缓存** - 提供高性能的数据缓存和快速访问

## 📁 项目结构

```
BSDProject/
├── commons/                                    # 公共模块
│   ├── src/main/java/com/yang/apm/springplugin/
│   │   ├── base/                               # 基础类库
│   │   │   ├── context/                        # 检测上下文类
│   │   │   ├── Enum/                          # 枚举定义
│   │   │   ├── item/                          # 数据项类
│   │   │   └── utils/                         # 工具类
│   │   ├── constant/                          # 常量定义
│   │   ├── expection/                         # 异常处理
│   │   ├── indexmapping/                      # ES索引映射
│   │   ├── model/                             # 数据模型
│   │   ├── pojo/                              # 数据传输对象
│   │   └── utils/                             # 通用工具
│   └── pom.xml                                # 公共模块配置
├── BSDComponent/                              # 主组件模块
│   ├── src/main/java/com/yang/apm/springplugin/
│   │   ├── BSDComponentApplication.java       # 主应用类
│   │   ├── config/                            # 配置类
│   │   │   ├── AsyncConfig.java               # 异步配置
│   │   │   ├── CacheConfig.java               # 缓存配置
│   │   │   └── WebConfig.java                 # Web配置
│   │   ├── controller/                        # REST API控制器
│   │   │   ├── datacollector/                 # 数据收集端点
│   │   │   ├── dynamicanalysis/               # 动态分析端点
│   │   │   ├── staticanalysis/                # 静态分析端点
│   │   │   └── HealthCheckController.java     # 健康检查
│   │   ├── factory/                           # 工厂模式实现
│   │   ├── listener/                          # 事件监听器
│   │   ├── manager/                           # 资源管理器
│   │   ├── mapper/                            # 数据访问层
│   │   ├── monitor/                           # 监控组件
│   │   ├── pojo/                              # 数据对象
│   │   ├── services/                          # 业务逻辑服务
│   │   │   ├── datacollector/                 # 数据收集服务
│   │   │   ├── dynamicdetect/                 # 动态检测服务
│   │   │   ├── staticdetect/                  # 静态检测服务
│   │   │   └── db/                            # 数据库服务
│   │   └── utils/                             # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml                    # 主配置文件
│   │   ├── application-dev.yml                # 开发环境配置
│   │   ├── application-pro.yml                # 生产环境配置
│   │   └── logback-spring.xml                 # 日志配置
│   └── pom.xml                                # 主组件配置
├── docs/                                      # 文档目录
│   ├── cn/                                    # 中文文档
│   │   ├── FINAL_COMPLETE_API_LIST.md         # 完整API列表
│   │   ├── CallRateAnomalyDetection.md        # 调用率异常检测
│   │   ├── Frequent_GC_Detection_Implementation.md # 频繁GC检测
│   │   ├── UnevenApiUsageDetection.md         # API使用不均检测
│   │   └── ...                                # 其他技术文档
│   ├── datademo/                              # 数据示例
│   │   ├── BusinessMetricsDemo.json           # 业务指标示例
│   │   ├── SvcMetricsResDemo.json             # 服务指标示例
│   │   └── cycleDependencyDemo.json           # 循环依赖示例
│   └── img/                                   # 图片资源
│       └── architecture.jpg                   # 架构图
├── pom.xml                                    # 根项目配置
└── README.md                                  # 项目说明文档
```

## 🚀 快速开始

### 前置条件

- **Java 17** 或更高版本
- **Maven 3.6** 或更高版本
- **MySQL 8.0** - 关系数据库
- **Elasticsearch 8.x** - 搜索引擎
- **Redis 6.0** 或更高版本 - 缓存系统

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone <repository-url>
   cd BSDProject
   ```

2. **配置数据库**
   ```sql
   CREATE DATABASE data_collector_db;
   ```

3. **更新配置文件**
   编辑 `BSDComponent/src/main/resources/application-dev.yml`:
   ```yaml
   server:
     port: 8090
   
   spring:
     datasource:
       username: your_username
       password: your_password
       url: jdbc:mysql://localhost:3306/data_collector_db
     elasticsearch:
       username: elastic
       password: your_elasticsearch_password
       uris: "http://localhost:9200"
     data:
       redis:
         host: 127.0.0.1
         port: 6379
   
   mbst:
     repository:
       username: your_git_username
       password: your_git_password
       local-repository: "/path/to/local/repo"
       remote-repositories: "repo1,repo2"
   ```

4. **构建项目**
   ```bash
   mvn clean install
   ```

5. **运行应用**
   ```bash
   cd BSDComponent
   mvn spring-boot:run
   ```

应用将在默认端口8090上启动。

### API文档访问

- **Swagger UI**: http://localhost:8090/doc.html
- **OpenAPI JSON**: http://localhost:8090/v3/api-docs

## 📖 使用示例

### 静态分析示例

#### 检测硬编码端点
```bash
curl -X POST http://localhost:8090/com-inter/hardcoded-endpoints \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "user-service"}'
```

#### 检测功能分散
```bash
curl -X POST http://localhost:8090/decomposition/scattered-functionality \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "order-service"}'
```

### 动态分析示例

#### 检测负载分布不均
```bash
curl -X POST http://localhost:8090/dynamic/uneven-load-distribution \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "payment-service"}'
```

#### 检测调用率异常
```bash
curl -X POST http://localhost:8090/dynamic/call-rate-anomaly \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "user-service"}'
```

#### 检测频繁GC
```bash
curl -X POST http://localhost:8090/dynamic/frequent-gc \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "order-service"}'
```

### 数据收集示例

#### 收集业务指标
```bash
curl -X POST http://localhost:8090/collector/business-metrics \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "user-service",
    "metrics": {
      "userRegistrations": 150,
      "activeUsers": 1200,
      "loginAttempts": 500
    }
  }'
```

## 🔧 核心算法详解

### 1. 不均衡负载检测算法

该算法用于识别微服务实例中存在的资源利用不均衡问题。

#### 检测条件
当一个实例**同时满足**以下四个条件时，被判定为不均衡负载：

1. ✅ **CPU使用率高于平均值+20%**
2. ✅ **RAM使用率高于平均值+20%**  
3. ✅ **请求数量低于平均值-20%**
4. ✅ **延时高于平均值+20%**

#### 算法特点
- **相对比较**: 基于同服务实例间的相对比较，避免绝对阈值的局限性
- **多维度检测**: 综合CPU、内存、请求量、延时四个维度
- **适应性强**: 自动适应不同服务的负载特征
- **误报率低**: 需要同时满足四个条件，减少误判

### 2. 调用率异常检测算法

基于业务导向的简化检测方法，避免复杂的统计学假设。

#### 检测规则

| 条件 | 阈值 | 说明 |
|------|------|------|
| **激增倍数** | ≥ 1.8倍 | 当前请求量达到历史平均值的1.8倍以上 |
| **增长率** | ≥ 100% | 请求量翻倍增长直接判定为异常 |

#### 激增程度分级

| 等级 | 倍数范围 | 描述 | 业务含义 |
|------|----------|------|----------|
| **NORMAL** | < 1.5x | 正常范围 | 正常的业务波动 |
| **MILD** | 1.5x - 1.8x | 轻度激增 | 需要关注但未达异常阈值 |
| **MODERATE** | 1.8x - 2.0x | 中度激增 | 达到异常阈值，需要调查 |
| **HIGH** | 2.0x - 3.0x | 高度激增 | 明显异常，需要立即处理 |
| **EXTREME** | ≥ 3.0x | 极端激增 | 严重异常，可能是攻击或故障 |

### 3. 频繁GC检测算法

采用四维度综合检测方式，从不同角度全面评估实例的GC状况。

#### 四维度检测体系

| 维度 | 检测目标 | 核心指标 | 阈值 | 作用 |
|------|----------|----------|------|------|
| **Minor GC时间维度** | Minor GC时间异常 | 当前平均时间 vs 历史平均 | 增长>20% | 识别Young区回收异常 |
| **Minor GC频率维度** | Minor GC频率异常 | Minor GC次数/时间 | ≥10次/分钟 | 识别Young区分配压力 |
| **Major GC时间维度** | Major GC时间异常 | 当前平均时间 vs 历史平均 | 增长>20% | 识别Old区回收异常 |
| **Major GC频率维度** | Major GC频率异常 | Major GC次数/时间 | ≥2次/分钟 | 识别Old区晋升压力 |

### 4. API使用不均检测算法

基于真实的API调用数据进行检测，使用多维度分析识别API使用分布的不均衡模式。

#### 核心检测维度

```java
检测维度：
1. 极端集中分析 - 检测单个API是否占用绝大部分调用（>80%）
2. 高度集中分析 - 检测单个API是否过度集中（>60%）
3. 帕累托异常检测 - 检测前20%API是否占用过多调用（>95%）
4. 集中度评分 - 基于基尼系数的分布集中度评估（>70分）
```

#### 智能判定逻辑

```java
// 智能判定规则
强条件 = 极端集中(>80%) OR 帕累托严重违反(>95%)
一般条件 = 满足的条件数量 >= 2

boolean isUnevenUsage = 强条件 OR 一般条件;
```

## ⚙️ 配置说明

### 检测参数配置

| 检测类型 | 参数 | 默认值 | 说明 | 调整建议 |
|----------|------|--------|------|----------|
| **负载分布不均** | coefficient | 0.2 | 容差系数(20%) | 根据业务特性调整 |
| **服务链** | chainLengthThreshold | 5 | 链长度阈值 | 根据架构复杂度调整 |
| **调用率异常** | growthMultiplier | 1.8 | 激增倍数阈值 | 根据业务波动特性调整 |
| **频繁GC** | gcFrequencyThreshold | 10 | GC频率阈值(次/分钟) | 根据JVM配置调整 |
| **API使用不均** | extremeConcentrationThreshold | 0.8 | 极端集中阈值 | 根据API数量调整 |

### 监控和告警配置

```yaml
# 告警规则建议
alert_rules:
  - name: "不均衡负载检测"
    condition: "uneven_load_instances > 0"
    duration: "10m"  # 连续10分钟异常才告警
    severity: "warning"
    message: "发现 {{ uneven_load_instances }} 个不均衡负载实例"
  
  - name: "调用率异常检测"
    condition: "call_rate_anomaly_detected"
    duration: "5m"
    severity: "warning"
    message: "检测到调用率异常激增"
  
  - name: "频繁GC检测"
    condition: "frequent_gc_detected"
    duration: "15m"
    severity: "critical"
    message: "检测到频繁GC问题"
```

## 📊 监控和告警

### 健康检查

```bash
# 应用健康状态
curl http://localhost:8090/health

# 检测服务状态
curl http://localhost:8090/health/detection-services
```

### 日志级别

- **INFO**: 一般应用信息和检测结果
- **WARN**: 检测到问题时的警告消息
- **DEBUG**: 详细的调试信息和检测过程
- **ERROR**: 系统错误和异常情况

### 指标收集

系统自动收集以下指标：

- **JVM指标**: CPU使用率、内存使用量、GC统计
- **应用指标**: 请求数量、响应时间、错误率
- **业务指标**: 自定义业务相关指标
- **系统指标**: 分布式追踪数据、服务依赖关系

## 🛠️ 开发指南

### 添加新的检测服务

1. **创建检测服务类**
   ```java
   @Service
   public class NewDetectionService implements IDetectConvert {
       @Override
       public DetectionResItem detect(RequestItem requestItem) {
           // 实现检测逻辑
           return result;
       }
   }
   ```

2. **创建控制器端点**
   ```java
   @RestController
   @RequestMapping("/dynamic")
   public class DynamicAnalysisController {
       @Autowired
       private NewDetectionService newDetectionService;
       
       @PostMapping("/new-detection")
       public ResponseDTO<String> detect(@RequestBody RequestItem requestItem) {
           DetectionResItem result = newDetectionService.detect(requestItem);
           return ResponseDTO.success("检测完成");
       }
   }
   ```

3. **添加配置参数**
   ```yaml
   detection:
     new-detection:
       threshold: 0.5
       enabled: true
   ```

### 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=DetectionServiceTest

# 生成测试覆盖率报告
mvn test jacoco:report
```

### 生产环境构建

```bash
# 生产环境打包
mvn clean package -Pprod

# Docker镜像构建
docker build -t smelldoc:latest .

# Kubernetes部署
kubectl apply -f k8s/
```

## 📈 系统特性

### 1. 全面覆盖
- **26种异味类型**: 涵盖微服务架构中所有主要的静态和运行时异味类型
- **多维度分析**: 从基础性能到复杂系统级异味全面覆盖
- **智能检测**: 基于多维度指标的智能检测算法

### 2. 统一架构
- **标准化框架**: 所有检测服务采用相同的架构模式
- **统一数据流**: ES数据获取 → 实例级检测 → 结果聚合 → 缓存存储
- **模块化设计**: 高内聚低耦合的模块化架构

### 3. 高性能设计
- **异步处理**: 采用异步处理机制，支持高并发访问
- **缓存优化**: 多级缓存策略，提升数据访问性能
- **批量处理**: 批量数据写入和检测，提高系统吞吐量

### 4. 完整生命周期
- **实时监控**: 实时数据收集和异常检测
- **历史分析**: 支持历史数据对比分析
- **结果存储**: 检测结果自动存储到系统缓存和ES
- **流程追踪**: 完整的检测流程追踪和审计

## 🎯 检测能力矩阵

| 检测类别 | 异味数量 | 状态 | 主要功能 |
|----------|----------|------|----------|
| **静态分析** | 9 | ✅ 完成 | 代码结构、架构设计检测 |
| **基础运行时** | 3 | ✅ 完成 | 服务健康、负载均衡检测 |
| **资源性能** | 3 | ✅ 完成 | 资源利用、性能异常检测 |
| **服务通信** | 2 | ✅ 完成 | 服务间调用模式检测 |
| **数据库查询** | 2 | ✅ 完成 | SQL性能、查询优化检测 |
| **内存GC** | 3 | ✅ 完成 | JVM性能、内存管理检测 |
| **逻辑处理** | 2 | ✅ 完成 | 业务逻辑分布检测 |
| **系统稳定性** | 2 | ✅ 完成 | 故障传播、性能趋势检测 |
| **总计** | **26** | **✅ 100%完成** | **全方位异味检测** |

## 🤝 贡献指南

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循Java编码规范
- 添加适当的注释和文档
- 编写单元测试
- 确保代码通过所有检查

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🆘 支持与帮助

### 常见问题

**Q: 如何调整检测阈值？**
A: 可以通过修改配置文件中的相关参数来调整检测阈值，如 `coefficient`、`threshold` 等。

**Q: 系统支持哪些数据库？**
A: 目前主要支持MySQL 8.0作为关系数据库，同时使用Elasticsearch进行数据存储和检索。

**Q: 如何添加自定义检测规则？**
A: 实现 `IDetectConvert` 接口，创建新的服务类，并在控制器中添加相应的端点。

**Q: 系统的性能如何？**
A: 系统采用异步处理和缓存机制，支持高并发访问。具体性能指标取决于部署环境和数据量。

**Q: 如何集成到现有系统？**
A: 系统提供REST API接口，可以轻松集成到现有的监控和告警系统中。

### 获取帮助

- 📧 邮箱: [your-email@example.com]
- 📱 钉钉群: [群号]
- 🐛 问题反馈: [GitHub Issues](https://github.com/your-repo/issues)
- 📚 文档: [项目文档](docs/)

## 📝 更新日志

### 版本 1.0.0 (2024-01-15)
- ✨ 初始版本发布
- 🔍 **静态分析功能** (9种异味类型)
  - 硬编码端点检测
  - 功能分散检测
  - 错误服务切分检测
  - 贪婪服务检测
  - 无API版本控制检测
  - ESB使用检测
  - 无API网关检测
  - 中心化依赖检测
  - 共享库问题检测
- 📊 **动态分析功能** (17种异味类型)
  - 脆弱服务检测
  - 负载分布不均检测
  - 服务响应不一致检测
  - 资源浪费检测
  - 调用率异常检测
  - API使用不均检测
  - 过度通信服务检测
  - 服务链过长检测
  - 高频慢查询检测
  - N+1查询检测
  - 频繁GC检测
  - 长时间GC检测
  - 内存抖动检测
  - 不均衡逻辑处理检测
  - 不必要处理检测
  - 多米诺骨牌效应检测
  - 坡道效应检测
- 📈 **数据收集和监控**
  - 业务指标收集
  - 内部指标监控
  - 链路追踪收集
  - 实时数据缓存
- 🔌 **REST API端点**
  - 26个检测接口
  - 统一请求响应格式
  - 完整的API文档
- 📖 **全面的文档和示例**
  - 详细的算法说明
  - 完整的使用示例
  - 配置和部署指南

### 版本 1.1.0 (计划中)
- 🚀 性能优化
- 🔧 更多检测算法
- 📊 增强的可视化功能
- 🐳 Docker支持
- ☸️ Kubernetes集成

---

**SmellDoc** - 让微服务架构更加健康、高效、可维护！

> **核心理念**: 通过智能检测和分析，帮助开发团队识别和解决微服务架构中的各种异味问题，提升系统的可维护性、性能和稳定性。