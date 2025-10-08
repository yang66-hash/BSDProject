# SmellDoc - 微服务异味检测与分析平台

## 📋 项目概述

SmellDoc是一个基于Elastic Stack的**微服务异味检测与分析插件**，通过结合运行时数据（指标、追踪、日志）与静态代码指标数据，实现对微服务架构中各种异味、性能问题和架构问题的检测与可视化。

> **核心价值：**
> **Step 1 - 配置阶段：** 部署agent代理 → 收集运行时数据  
> **Step 2 - 数据收集：** 预处理与整合指标数据 → 发送至Elasticsearch  
> **Step 3 - 检测与可视化：** 异味检测 → 展示于Kibana

![系统架构图](docs/img/architecture.jpg)
1. Re-integration Collector (RIC)  -  代码位置：`service/datacollector/`
2. MBS Detection (BSD) -  代码位置：`controller/`
3. Custom-Business-Collector (CBC) -  位置：[apm-springcloud-business-plugin](https://github.com/yang66-hash/apm-springcloud-business-plugin.git) 
4. Kibana MBS Detection Plugin -  位置：[kibana-mbs-detection-plugin](https://github.com/yang66-hash/kibana_bad_smell_detection_plugin.git)
5. Microservice System for Demonstrate  -  位置：[PropertyManagementCloud](https://github.com/yang66-hash/PropertyManagementCloud.git)
6. ELastic APM -  位置：[elastic apm](https://github.com/elastic/apm.git)
6. 各类文档见`docs`

## 🏗️ 系统架构

### 1) 配置层 (Configuration Layer)
- **Elastic APM Java Agent** (`elastic-apm-agent.jar`) - 收集运行时指标、链路信息
- **自定义业务收集器 (CBC)** - 收集业务层指标和自定义监控数据
- **APM Server** - 实时接收、处理并将数据存储到ES

### 2) 数据收集层 (Data Collection Layer)
- **重整合收集器 (RIC)** - 去重并聚合运行时指标，提供标准化数据格式
- **Elasticsearch** - 存储所有处理后的监控数据和检测结果

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
| **Maven** | 3.6+ | 构建工具 | 项目构建和依赖管理 |
| **Knife4j** | 4.4.0 | API文档 | Swagger增强文档工具 |
| **JavaParser** | 3.24.2 | 代码分析 | 静态代码分析和AST解析 |
| **JGit** | 7.2.0 | 版本控制 | Git仓库操作和分析 |
| **Kubernetes Client** | 20.0.0 | 容器编排 | K8s集群管理和服务发现 |

## 🚀 核心功能

### 🔍 静态分析功能 (12种异味类型)

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
| **共享持久化** | `/infrastructure/sharing-persistence` | 检测多个服务共享同一数据库 | 数据模型耦合，服务生命周期绑定 |
| **不恰当服务亲密** | `/infrastructure/inappropriate-service-intimacy` | 检测服务访问其他服务私有数据 | 缺乏独立性和自治原则 |
| **上帝组件** | `/infrastructure/god-component` | 检测过大且复杂的微服务 | 服务职责过重，违反单一职责原则 |

### 📊 动态分析功能 (12种异味类型)

#### 1️⃣ 基础运行时异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 1 | `POST /dynamic/fragile-service` | Fragile Service | 脆弱服务检测 | 失败率、延迟、吞吐量 |
| 2 | `POST /dynamic/uneven-load-distribution` | Uneven Load Distribution | 负载分布不均检测 | CPU、内存、请求量、延时 |
| 3 | `POST /dynamic/inconsistent-service-response` | Inconsistent Service Response | 服务响应不一致检测 | 延迟波动、失败率变化 |

#### 2️⃣ 资源和性能异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|----|---------|----------|------|----------|
| 4  | `POST /dynamic/call-rate-anomaly` | Call Rate Anomaly | 调用频率异常检测 | 请求量变化、历史对比 |
| 5  | `POST /dynamic/uneven-api-usage` | Uneven API Usage | API使用不均衡检测 | API调用分布、基尼系数 |


#### 3️⃣ 数据库和查询异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 6 | `POST /dynamic/high-frequency-slow-queries` | High Frequency Of Slow Queries | 高频慢查询检测 | 慢查询比例、执行时间 |

#### 4️⃣ 内存和GC异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 7 | `POST /dynamic/frequent-gc` | Frequent GC | 频繁GC检测 | 四维度GC分析 |
| 8 | `POST /dynamic/long-time-gc` | Long Time GC | 长时间GC检测 | GC暂停时间分析 |
| 9 | `POST /dynamic/memory-jitter-of-service` | Memory Jitter Of Service | 内存抖动检测 | 内存使用波动 |

#### 5️⃣ 逻辑处理异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 10 | `POST /dynamic/uneven-logic-processing` | Uneven Logic Processing | 不均衡逻辑处理检测 | 方法调用分布 |

#### 6️⃣ 系统稳定性异味检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 11 | `POST /dynamic/the-ramp` | The Ramp | 坡道效应检测 | 性能趋势分析 |

#### 7️⃣ 循环依赖检测
| 序号 | API端点 | 异味类型 | 描述 | 检测维度 |
|------|---------|----------|------|----------|
| 12 | `POST /dynamic/cyclic-dependency` | Cyclic Dependency | 循环依赖检测 | 调用链循环分析 |

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
│   │   │   ├── context/                        # 检测结果类
│   │   │   ├── Enum/                          
│   │   │   ├── item/                          
│   │   │   └── utils/                         
│   │   ├── constant/                          # 常量定义
│   │   ├── expection/                         # 异常处理
│   │   ├── indexmapping/                      # ES索引映射
│   │   ├── model/                             # 数据模型
│   │   ├── pojo/                              # 数据传输对象
│   │   └── utils/                             # 通用工具
│   └── pom.xml                                
├── BSDComponent/                              # 主组件模块
│   ├── src/main/java/com/yang/apm/springplugin/
│   │   ├── BSDComponentApplication.java      
│   │   ├── config/                            # 配置类
│   │   │   ├── AsyncConfig.java               # 异步配置
│   │   │   ├── CacheConfig.java               # 缓存配置
│   │   │   └── WebConfig.java                 # Web配置
│   │   ├── controller/                        # REST API控制器
│   │   │   ├── datacollector/                 # 数据收集端点
│   │   │   ├── dynamicanalysis/               # 动态分析端点
│   │   │   ├── staticanalysis/                # 静态分析端点
│   │   │   └── HealthCheckController.java     # 健康检查
│   │   ├── factory/                           # 工厂类
│   │   ├── listener/                          # 事件监听器
│   │   ├── manager/                           # 资源管理器
│   │   ├── mapper/                            # 数据访问层
│   │   ├── monitor/                           # 监控组件
│   │   ├── pojo/                              # 数据对象
│   │   ├── services/                          # 业务逻辑服务
│   │   │   ├── datacollector/                 # 数据收集组件
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
│   │   ├── UnevenApiUsageDetection.md         # API使用不均检测
│   │   └── ...                                # 其他技术文档
│   ├── datademo/                              # 数据示例
│   │   ├── BusinessMetricsDemo.json           # 业务指标示例
│   │   ├── SvcMetricsResDemo.json             # 服务指标示例
│   │   └── cycleDependencyDemo.json           # 循环依赖示例
│   └── img/                                   
│       └── architecture.jpg                   # 架构图
├── pom.xml                                    # 根项目配置
└── README.md                                  # 项目说明文档
```

## 🚀 快速开始

### 前置条件

- **Java 17** 或更高版本
- **Maven 3.6** 或更高版本
- **MySQL 8.0** - 关系数据库
- **Elasticsearch 8.14.3** - 搜索引擎

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/yang66-hash/BSDProject.git
   cd BSDProject
   ```

2. **配置数据库**
   导入数据库脚本 `docs/db/data_collector_db.sql`到本地mysql数据库

3. **更新配置文件**
   编辑 `BSDComponent/src/main/resources/application-dev.yml`:
   ```yaml
   server:
     port: 32000
   
   spring:
     datasource:
       username: your_username
       password: your_password
       url: jdbc:mysql://localhost:3306/data_collector_db
     elasticsearch:
       username: elastic
       password: your_elasticsearch_password
       uris: "http://localhost:9200"
   
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

应用将在默认端口32000上启动。或者也可以通过`java -jar`命令启动jar包。

### API文档访问

- **Swagger UI**: http://localhost:32000/doc.html

## 📖 使用示例

### 静态分析示例

需要配置`mbst.repository`参数，用于访问Git仓库。

#### 检测硬编码端点
```bash
curl -X POST http://localhost:32000/com-inter/hardcoded-endpoints \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "user-service"}'
```

#### 检测功能分散
```bash
curl -X POST http://localhost:32000/decomposition/scattered-functionality \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "order-service"}'
```

### 动态分析示例
需要存在已经配置好APM Java Agent的微服务系统才可以使用。
#### 检测负载分布不均
```bash
curl -X POST http://localhost:32000/dynamic/uneven-load-distribution \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "payment-service"}'
```

#### 检测调用率异常
```bash
curl -X POST http://localhost:32000/dynamic/call-rate-anomaly \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "user-service"}'
```

#### 检测频繁GC
```bash
curl -X POST http://localhost:32000/dynamic/frequent-gc \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "order-service"}'
```


## 📈 系统特性

### 1. 全面覆盖
- **24种异味类型**: 涵盖微服务架构中所有主要的静态和运行时异味类型
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
| **静态分析** | 12 | ✅ 完成 | 代码结构、架构设计检测 |
| **基础运行时** | 3 | ✅ 完成 | 服务健康、负载均衡检测 |
| **资源性能** | 2 | ✅ 完成 | 资源利用、性能异常检测 |
| **数据库查询** | 1 | ✅ 完成 | SQL性能、查询优化检测 |
| **内存GC** | 3 | ✅ 完成 | JVM性能、内存管理检测 |
| **逻辑处理** | 1 | ✅ 完成 | 业务逻辑分布检测 |
| **系统稳定性** | 1 | ✅ 完成 | 故障传播、性能趋势检测 |
| **循环依赖** | 1 | ✅ 完成 | 调用链循环检测 |
| **总计** | **24** | **✅ 100%完成** | **全方位异味检测** |

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

- 📧 邮箱: [2019023491@qq.com]
- 📱 钉钉群: [群号]
- 🐛 问题反馈: [GitHub Issues](https://github.com/yang66-hash/BSDProject.git)
- 📚 文档: [项目文档](docs/)

## 📝 更新日志

### 版本 1.0.0 (2025-10-01)
- ✨ 初始版本发布
- 🔍 **静态分析功能** (12种异味类型)
  - 硬编码端点检测
  - 功能分散检测
  - 错误服务切分检测
  - 贪婪服务检测
  - 无API版本控制检测
  - ESB使用检测
  - 无API网关检测
  - 中心化依赖检测
  - 共享库问题检测
  - 共享持久化检测
  - 不恰当服务亲密检测
  - 上帝组件检测
- 📊 **动态分析功能** (12种异味类型)
  - 脆弱服务检测
  - 负载分布不均检测
  - 服务响应不一致检测
  - 调用率异常检测
  - API使用不均检测
  - 高频慢查询检测
  - 频繁GC检测
  - 长时间GC检测
  - 内存抖动检测
  - 不均衡逻辑处理检测
  - 坡道效应检测
  - 循环依赖检测
- 📈 **数据收集和监控**
  - 业务指标收集
  - 内部指标监控
  - 链路追踪收集
- 🔌 **REST API端点**
  - 24个检测接口
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