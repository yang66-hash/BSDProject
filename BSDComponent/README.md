# BSDComponent

## Overview

BSDComponent is a comprehensive microservices analysis and monitoring platform designed for detecting anti-patterns, performance issues, and architectural problems in distributed systems. The system provides both static code analysis and dynamic runtime monitoring capabilities to ensure optimal microservices architecture and performance.

## Architecture

The system architecture is illustrated in the following diagram:

![System Architecture](architecture.jpg)

## Key Features

### 🔍 Static Analysis
- **ESB Usage Detection**: Identifies Enterprise Service Bus usage patterns
- **Hard-coded Endpoints**: Detects hard-coded service endpoints
- **Scattered Functionality**: Identifies functionality scattered across services
- **Too Many Standards**: Detects excessive standardization issues
- **Hub Service Detection**: Identifies hub-like service patterns
- **Cyclic Dependencies**: Detects circular dependencies between services
- **Greedy Service Detection**: Identifies services with excessive responsibilities
- **No Gateway Service**: Detects missing API gateway patterns
- **Shared Library Issues**: Identifies problematic shared library usage
- **Unversioned APIs**: Detects APIs without proper versioning
- **Wrong Service Boundaries**: Identifies incorrect service decomposition

### 📊 Dynamic Analysis
- **Service Chain Detection**: Identifies overly long service call chains
- **Call Rate Anomaly**: Detects abnormal service call patterns
- **Resource Waste Detection**: Identifies resource utilization inefficiencies
- **Chatty Service Detection**: Detects services with excessive communication
- **Fragile Service Detection**: Identifies services prone to failures
- **Frequent GC Detection**: Detects frequent garbage collection issues
- **High Frequency Slow Queries**: Identifies performance bottlenecks in database queries
- **Falling Dominoes**: Detects cascading failure patterns
- **Inconsistent Service Response**: Detects response time inconsistencies
- **Long Time GC**: Detects long-running garbage collection
- **Memory Jitter**: Detects memory usage fluctuations
- **N+1 Queries**: Detects inefficient database query patterns
- **The Ramp Detection**: Identifies gradual performance degradation
- **Uneven API Usage**: Detects uneven API endpoint utilization
- **Uneven Load Distribution**: Detects load balancing issues
- **Uneven Logic Processing**: Detects processing time imbalances
- **Unnecessary Processing**: Identifies redundant processing operations

### 📈 Data Collection & Monitoring
- **Business Metrics Collection**: Collects business-specific metrics
- **Internal Metrics Monitoring**: Monitors JVM and application metrics
- **Trace Collection**: Collects distributed tracing data
- **API Metrics**: Monitors API usage and performance
- **Circular Dependency Detection**: Real-time circular dependency monitoring

## Technology Stack

- **Framework**: Spring Boot 3.3.0
- **Java Version**: 17
- **Database**: MySQL 8.0
- **Search Engine**: Elasticsearch 8.x
- **Cache**: Redis
- **Build Tool**: Maven
- **Documentation**: Knife4j (Swagger)
- **Code Analysis**: JavaParser
- **Version Control**: JGit
- **Container Orchestration**: Kubernetes Client

## Project Structure

```
BSDComponent/
├── src/main/java/com/yang/apm/springplugin/
│   ├── BSDComponentApplication.java          # Main application class
│   ├── config/                               # Configuration classes
│   ├── controller/                           # REST API controllers
│   │   ├── datacollector/                    # Data collection endpoints
│   │   ├── dynamicanalysis/                  # Dynamic analysis endpoints
│   │   └── staticanalysis/                   # Static analysis endpoints
│   ├── factory/                              # Factory pattern implementations
│   ├── listener/                             # Event listeners
│   ├── manager/                              # Resource managers
│   ├── mapper/                               # Data access layer
│   ├── monitor/                              # Monitoring components
│   ├── pojo/                                 # Plain Old Java Objects
│   ├── services/                             # Business logic services
│   │   ├── datacollector/                    # Data collection services
│   │   ├── dynamicdetect/                    # Dynamic detection services
│   │   ├── staticdetect/                     # Static detection services
│   │   └── db/                               # Database services
│   └── utils/                                # Utility classes
├── src/main/resources/
│   ├── application.yml                       # Main configuration
│   ├── application-dev.yml                   # Development configuration
│   ├── application-pro.yml                   # Production configuration
│   └── logback-spring.xml                    # Logging configuration
└── docs/                                     # Documentation
    ├── UnevenLoadDistribution算法说明.md      # Uneven load distribution algorithm
    └── 不均衡负载检测使用示例.md              # Usage examples
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0
- Elasticsearch 8.x
- Redis 6.0 or higher

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd BSDProject/BSDComponent
   ```

2. **Configure the database**
   ```sql
   CREATE DATABASE data_collector_db;
   ```

3. **Update configuration**
   Edit `src/main/resources/application-dev.yml`:
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

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on port 8090 by default.

### API Documentation

Once the application is running, you can access the API documentation at:
- **Swagger UI**: http://localhost:8090/doc.html
- **OpenAPI JSON**: http://localhost:8090/v3/api-docs

## Usage Examples

### Static Analysis

#### Detect Hard-coded Endpoints
```bash
curl -X POST http://localhost:8090/static/hard-code \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "user-service"}'
```

#### Detect ESB Usage
```bash
curl -X POST http://localhost:8090/static/esb \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "order-service"}'
```

### Dynamic Analysis

#### Detect Uneven Load Distribution
```bash
curl -X POST http://localhost:8090/dynamic/uneven-load-distribution \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "payment-service"}'
```

#### Detect Service Chain Issues
```bash
curl -X POST http://localhost:8090/dynamic/service-chain \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "inventory-service"}'
```

### Data Collection

#### Collect Business Metrics
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

## Configuration

### Application Properties

Key configuration options in `application-dev.yml`:

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

### Detection Parameters

The system supports various detection parameters that can be configured:

- **Uneven Load Distribution**: 20% tolerance coefficient
- **Service Chain**: Configurable chain length thresholds
- **Call Rate Anomaly**: Statistical deviation thresholds
- **Resource Waste**: CPU and memory utilization thresholds

## Monitoring and Alerting

### Health Check

The application provides health check endpoints:

```bash
curl http://localhost:8090/health
```

### Logging

Logging is configured using Logback with different levels:
- **INFO**: General application information
- **WARN**: Warning messages for detected issues
- **DEBUG**: Detailed debugging information

### Metrics Collection

The system automatically collects:
- JVM metrics (CPU, memory, GC)
- Application metrics (request count, response time)
- Business metrics (custom application metrics)
- Distributed tracing data

## Development

### Adding New Detection Services

1. Create a new service class implementing `IDetectConvert`
2. Add the detection logic in the service
3. Create a corresponding controller endpoint
4. Update the documentation

### Testing

Run tests using Maven:
```bash
mvn test
```

### Building for Production

```bash
mvn clean package -Pprod
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation in the `docs/` folder

## Changelog

### Version 1.0.0
- Initial release
- Static analysis capabilities
- Dynamic analysis capabilities
- Data collection and monitoring
- REST API endpoints
- Comprehensive documentation
