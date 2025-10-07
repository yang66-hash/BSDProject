# BSDComponent

## 概述

BSDComponent 是一个全面的微服务分析和监控平台，专为检测分布式系统中的反模式、性能问题和架构问题而设计。该系统提供静态代码分析和动态运行时监控功能，以确保微服务架构和性能的最优化。

## 系统架构

系统架构图如下所示：

![系统架构图](architecture.jpg)

## 核心功能

### 🔍 静态分析
- **ESB使用检测**: 识别企业服务总线使用模式
- **硬编码端点**: 检测硬编码的服务端点
- **功能分散**: 识别跨服务分散的功能
- **标准过多**: 检测过度标准化问题
- **中心服务检测**: 识别类似中心的服务模式
- **循环依赖**: 检测服务间的循环依赖
- **贪婪服务检测**: 识别职责过重的服务
- **无网关服务**: 检测缺失的API网关模式
- **共享库问题**: 识别有问题的共享库使用
- **无版本API**: 检测没有适当版本控制的API
- **错误服务边界**: 识别不正确的服务分解

### 📊 动态分析
- **服务链检测**: 识别过长的服务调用链
- **调用率异常**: 检测异常的服务调用模式
- **资源浪费检测**: 识别资源利用效率低下
- **聊天服务检测**: 检测过度通信的服务
- **脆弱服务检测**: 识别容易失败的服务
- **频繁GC检测**: 检测频繁垃圾回收问题
- **高频慢查询**: 识别数据库查询性能瓶颈
- **多米诺效应**: 检测级联故障模式
- **服务响应不一致**: 检测响应时间不一致
- **长时间GC**: 检测长时间运行的垃圾回收
- **内存抖动**: 检测内存使用波动
- **N+1查询**: 检测低效的数据库查询模式
- **斜坡检测**: 识别渐进式性能下降
- **API使用不均**: 检测API端点使用不均衡
- **负载分布不均**: 检测负载均衡问题
- **逻辑处理不均**: 检测处理时间不平衡
- **不必要处理**: 识别冗余处理操作

### 📈 数据收集与监控
- **业务指标收集**: 收集业务特定指标
- **内部指标监控**: 监控JVM和应用指标
- **链路追踪收集**: 收集分布式追踪数据
- **API指标**: 监控API使用和性能
- **循环依赖检测**: 实时循环依赖监控

## 技术栈

- **框架**: Spring Boot 3.3.0
- **Java版本**: 17
- **数据库**: MySQL 8.0
- **搜索引擎**: Elasticsearch 8.x
- **缓存**: Redis
- **构建工具**: Maven
- **文档**: Knife4j (Swagger)
- **代码分析**: JavaParser
- **版本控制**: JGit
- **容器编排**: Kubernetes Client

## 项目结构

```
BSDComponent/
├── src/main/java/com/yang/apm/springplugin/
│   ├── BSDComponentApplication.java          # 主应用类
│   ├── config/                               # 配置类
│   ├── controller/                           # REST API控制器
│   │   ├── datacollector/                    # 数据收集端点
│   │   ├── dynamicanalysis/                  # 动态分析端点
│   │   └── staticanalysis/                   # 静态分析端点
│   ├── factory/                              # 工厂模式实现
│   ├── listener/                             # 事件监听器
│   ├── manager/                              # 资源管理器
│   ├── mapper/                               # 数据访问层
│   ├── monitor/                              # 监控组件
│   ├── pojo/                                 # 普通Java对象
│   ├── services/                             # 业务逻辑服务
│   │   ├── datacollector/                    # 数据收集服务
│   │   ├── dynamicdetect/                    # 动态检测服务
│   │   ├── staticdetect/                     # 静态检测服务
│   │   └── db/                               # 数据库服务
│   └── utils/                                # 工具类
├── src/main/resources/
│   ├── application.yml                       # 主配置文件
│   ├── application-dev.yml                   # 开发环境配置
│   ├── application-pro.yml                   # 生产环境配置
│   └── logback-spring.xml                    # 日志配置
└── docs/                                     # 文档
    ├── UnevenLoadDistribution算法说明.md      # 负载不均分布算法说明
    └── 不均衡负载检测使用示例.md              # 使用示例
```

## 快速开始

### 前置条件

- Java 17 或更高版本
- Maven 3.6 或更高版本
- MySQL 8.0
- Elasticsearch 8.x
- Redis 6.0 或更高版本

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone <repository-url>
   cd BSDProject/BSDComponent
   ```

2. **配置数据库**
   ```sql
   CREATE DATABASE data_collector_db;
   ```

3. **更新配置**
   编辑 `src/main/resources/application-dev.yml`:
   ```yaml
   spring:
     datasource:
       username: your_username
       password: your_password
       url: jdbc:mysql://localhost:3306/data_collector_db
     elasticsearch:
       username: elastic
       password: your_elasticsearch_password
       uris: "http://localhost:9200"
   ```

4. **构建项目**
   ```bash
   mvn clean install
   ```

5. **运行应用**
   ```bash
   mvn spring-boot:run
   ```

应用将在默认端口8090上启动。

### API文档

应用启动后，可以通过以下地址访问API文档：
- **Swagger UI**: http://localhost:8090/doc.html
- **OpenAPI JSON**: http://localhost:8090/v3/api-docs

## 使用示例

### 静态分析

#### 检测硬编码端点
```bash
curl -X POST http://localhost:8090/static/hard-code \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "user-service"}'
```

#### 检测ESB使用
```bash
curl -X POST http://localhost:8090/static/esb \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "order-service"}'
```

### 动态分析

#### 检测负载分布不均
```bash
curl -X POST http://localhost:8090/dynamic/uneven-load-distribution \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "payment-service"}'
```

#### 检测服务链问题
```bash
curl -X POST http://localhost:8090/dynamic/service-chain \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "inventory-service"}'
```

### 数据收集

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

## 配置说明

### 应用属性

`application-dev.yml` 中的关键配置选项：

```yaml
server:
  port: 8090

spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://127.0.0.1:3306/data_collector_db
  elasticsearch:
    username: elastic
    password: your_password
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

### 检测参数

系统支持多种可配置的检测参数：

- **负载分布不均**: 20%容差系数
- **服务链**: 可配置的链长度阈值
- **调用率异常**: 统计偏差阈值
- **资源浪费**: CPU和内存利用率阈值

## 监控和告警

### 健康检查

应用提供健康检查端点：

```bash
curl http://localhost:8090/health
```

### 日志记录

使用Logback配置日志，支持不同级别：
- **INFO**: 一般应用信息
- **WARN**: 检测到问题时的警告消息
- **DEBUG**: 详细的调试信息

### 指标收集

系统自动收集：
- JVM指标（CPU、内存、GC）
- 应用指标（请求数、响应时间）
- 业务指标（自定义应用指标）
- 分布式追踪数据

## 开发指南

### 添加新的检测服务

1. 创建实现 `IDetectConvert` 的新服务类
2. 在服务中添加检测逻辑
3. 创建相应的控制器端点
4. 更新文档

### 测试

使用Maven运行测试：
```bash
mvn test
```

### 生产环境构建

```bash
mvn clean package -Pprod
```

## 贡献指南

1. Fork 仓库
2. 创建功能分支
3. 进行更改
4. 为新功能添加测试
5. 提交拉取请求

## 许可证

本项目采用 MIT 许可证。

## 支持

如需支持和问题解答：
- 在仓库中创建问题
- 联系开发团队
- 查看 `docs/` 文件夹中的文档

## 更新日志

### 版本 1.0.0
- 初始版本
- 静态分析功能
- 动态分析功能
- 数据收集和监控
- REST API端点
- 全面的文档

## 算法详解

### 不均衡负载检测算法

该算法用于识别微服务实例中存在的资源利用不均衡问题。详细说明请参考：
- [不均衡负载分布算法说明](docs/UnevenLoadDistribution算法说明.md)
- [不均衡负载检测使用示例](docs/不均衡负载检测使用示例.md)

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

## 常见问题

### Q: 如何调整检测阈值？
A: 可以通过修改配置文件中的 `coefficient` 参数来调整容差系数，默认为0.2（20%）。

### Q: 系统支持哪些数据库？
A: 目前主要支持MySQL 8.0，同时使用Elasticsearch进行数据存储和检索。

### Q: 如何添加自定义检测规则？
A: 实现 `IDetectConvert` 接口，创建新的服务类，并在控制器中添加相应的端点。

### Q: 系统的性能如何？
A: 系统采用异步处理和缓存机制，支持高并发访问。具体性能指标取决于部署环境和数据量。

### Q: 如何集成到现有系统？
A: 系统提供REST API接口，可以轻松集成到现有的监控和告警系统中。
