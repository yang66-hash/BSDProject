# Supported Plugin Types

## Static Analysis Bad Smells (9 types)

Scattered Functionality

Wrong cuts

Microservice greedy

hardcode

no-api-versioning

esb-usage

no-api-gateway

hub-like-dependency

shared-libraries (12 total)

## Runtime Bad Smells (18-6=12)

Fragile Service

uneven load distribution

inconsistent-service-response

Resource Waste

Call Rate Anomaly

uneven-api-usage

chatty-service

service-chain

High Frequency Of Slow Queries

N+1 Queries

Frequent GC

Long Time GC

Memory Jitter Of Service

Uneven Logic Processing

Falling Dominoes

unnecessary-processing

the-ramp

Cyclic Dependency

**Shared Database, Inappropriate Service Intimacy, Too Many Standards, God Component, Cyclic Hierarchy, Multipath Hierarchy**

## MBSs types supported by Static-Analysis

## Architectural Microservice Smells

| ID   | Name                           | Description                                                  | Thresholds / Metrics                                         | Possible Causes                                              |
| ---- | ------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| [1]  | Scattered Functionality        | Multiple microservices implement the same high-level functionality, caused by unclear service boundaries. | If the number of call paths between two services (calledNum) exceeds a threshold, they are considered scattered. | Lack of clear service boundary definitions, or failure to refactor services during business evolution. |
| [2]  | Wrong Cuts                     | Services are split along technical layers (presentation, business, data) instead of business functionalities. | \|entityCount - avgEntityCount\| ≥ 3 * std && (size ≠ 1) && std ≠ 0, based on entity distribution analysis. | Services not decomposed by business function, leading to improper modularization. |
| [3]  | Microservice Greedy            | A service contains unnecessary features or lacks proper domain boundaries. | ( !staticFiles.isEmpty() && staticFiles.size() ≤ staticFileThreshold && isControllerFileExists ) OR (entityCount == 0). | Blurred business domain boundaries; unnecessary decomposition of services. |
| [4]  | Hardcoded Endpoints            | Services directly hardcode endpoints (e.g., IP:PORT) for communication with other services/databases. | Regular expression matching IP:PORT patterns inside service code. | Hardcoded endpoints scattered in code reduce analyzability, scalability, and resource utilization. |
| [5]  | No API Versioning              | API endpoints lack explicit semantic versioning in URIs.     | Regex to match semantic versioning patterns in API URIs, detect missing version info. | Lack of backward compatibility considerations.               |
| [6]  | ESB Usage                      | Using an ESB introduces centralization and complexity in microservice communication. | callNum > threshold1 && calledNum > threshold2.              | Lack of understanding of microservice architectural principles and best practices. |
| [7]  | No API Gateway                 | Absence of an API Gateway, leading to poor maintainability and increased complexity as services scale. | Check service dependency/config files for API Gateway dependencies and routing rules. | Architecture not updated with system evolution; failure to introduce API Gateway. |
| [8]  | Hub-like Dependency            | A class has excessive imports and exports, acting as a dependency hub. | imports > threshold1 && exports > threshold2, with roughly balanced in/out dependencies. | Class violates single responsibility principle by handling too many concerns, increasing system complexity. |
| [9]  | Shared Libraries               | Different microservices share the same dependency library.   | Compare DependencyLists of services; if intersection is non-empty, smell is detected. | Failure to extract shared logic into a dedicated common service. |
| [10] | Sharing Persistence            | Multiple services share the same physical database (schema/instance), coupling their data models and lifecycle. | Extract DB connection info across services; if ≥2 services connect to the same DB (same URI/schema), smell detected. | Legacy monolith database reused; weak data ownership enforcement; convenience over service autonomy. |
| [11] | Inappropriate Service Intimacy | A service accesses another service's private data in addition to its own. | Parse queries/DB connections; if a service references another service's schema/tables or holds external DB credentials, smell detected. | Lack of independence and autonomy principle; improper domain separation; bypassing APIs for efficiency. |
| [12] | God Component                  | A microservice is too large and complex, containing excessive responsibilities. | Outlier detection on service size metrics: entity count, LOC, file count, API count; if above p95 threshold or composite complexity index ≥ threshold. | Wrong service cuts; over-coarse decomposition; accumulating unrelated features into a single service. |

## MBSs types supported by MBSD

| ID      | Name                           | Description                                                  | Symptoms / Indicators                                        | Thresholds / Metrics                                         | Possible Causes                                              |
| ------- | ------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| [1]     | Fragile Service                | Significant increase in failure rate, sharp rise in latency, and drop in throughput. | Failure rate exceeds threshold (FTRTH), latency above system average, throughput below average. | FTRTH (5%), ExecutimeTH (> avgSystem +avgSystem*20%), ThroughputTH (< avgSystem -avgSystem*50%). | Long response time from external dependencies, DB/API bottlenecks, insufficient resources. |
| [2]     | Uneven Load Distribution       | Uneven CPU or RAM usage across instances, with throughput lower than average. | CPU and memory utilization imbalance, throughput below system average. | CPUTH, RAMTH (> avgSystem +avgSystem*10%), ThroughputTH (same as above). | (1) Poor interface design causing heavy computation; (2) Improper container resource allocation. |
| [3]     | Inconsistent Service Response  | High fluctuations in latency and failure rate for the same service, affecting UX. | Latency variance (e.g., PT95 > 1000 ms), failure rate inconsistent across time windows. | 95%Executime (1000 ms), FTRTH (>5%), coefficient of variation (CV > 0.3). | (1) Async logic or resource contention; (2) Locking and synchronization bottlenecks. |
| [4]     | ~~Resource Waste~~             | Low latency and normal throughput, but CPU/RAM usage inefficient, raising cost. | CPU or RAM utilization significantly below system average.   | CPUTH, RAMTH (< avgSystem -avgSystem*10%).                   | (1) Overprovisioned resources; (2) Too many service instances. |
| [5]     | Call Rate Anomaly (Horizontal) | API call frequency in a time window deviates significantly from historical mean. | Abnormal surge or drop in call frequency.                    | 3-SIGMA rule: \|CallOffset - avgCallNum\| ≥ 3 * StandardCallNum. | Traffic spikes, DDoS, logic bugs, or external changes.       |
| [6]     | Uneven API Usage (Vertical)    | Over-dependence on certain APIs while others are rarely used. | Hotspot APIs disproportionately exceed expected usage ratio. | APICallNum > avgSystem + avgSystem * APICallTH (50%).        | (1) Business demand shifts; (2) API design flaws; (3) Scenario-specific imbalance. |
| [7]     | ~~Chatty Service~~             | ~~Excessive calls to one service within a single request chain.~~ | ~~Request chain overuses a specific service compared to average.~~ | ~~SVCCall > avgSVCCall + avgSVCCall * SVCCallTH (50%).~~     | ~~Poor service design, lack of aggregation, too many fine-grained requests.~~ |
| ~~[8]~~ | ~~Service Chain~~              | Requests depend on too many chained services, lowering reliability. | Request chain length exceeds average expected number of services. | SVCNum > avgSVCNum + avgSVCNum * DiffSVCNumTH (20%).         | Poor service boundary design, lack of aggregation logic.     |
| [9]     | Frequent Slow Queries          | Service executes frequent slow SQL queries, degrading UX.    | Proportion of slow queries in a time window increases sharply. | SlowQueryRate ≥ SlowQueryTH (20%, threshold = 1s).           | (1) Poor table design; (2) Missing indexes; (3) Rapid data growth. |
| [10]    | ~~N+1 Queries~~                | One query triggers N additional queries, overloading DB and lowering performance. | Total queries per request far exceed expectation.            | Rule-based detection.                                        | (1) Poor data access design; (2) Not using joins effectively. |
| [11]    | Frequent GC                    | Excessive garbage collection pauses degrade performance.     | High GCCount compared to historical baseline.                | GCCount > avgGCCount + avgGCCount * GCTH; GCTimeTH (20%).    | (1) Memory leaks; (2) Improper heap sizing; (3) Excessive object churn. |
| [12]    | Long GC Time                   | Garbage collection pauses exceed expected duration, harming responsiveness. | GC pause time significantly above historical/system average. | GCTime > avgGCTime + avgGCTime * GCTimeTH (20%).             | (1) Improper heap config; (2) Wrong GC algorithm; (3) Heavy allocation patterns. |
| [13]    | Memory Jitter                  | Frequent short-term memory usage spikes causing instability. | Frequent GC events, heap utilization fluctuates beyond threshold. | GCCount ↑, GCTime ↑, HeapUsedRateMax - HeapUsedRateMin > HeapUsedRateTH (10%). | Memory leaks, poor heap config, improper allocation, GC policy issues. |
| [14]    | Uneven Logic Processing        | ServiceImpl methods show significant imbalance in invocation counts. | Certain methods' call counts far exceed others in same service. | MethodCallCount > avgSystem + avgSystem * MethodCallCountTH (50%). | (1) Hotspot methods overloaded; (2) Imbalanced request distribution. |
| [15]    | ~~Falling Dominoes~~           | Failure in one service cascades to dependent services.       | Dependent services all show similar performance degradation. | (1) No circuit breaker, multiple services show high latency/failure; (2) Error-work ratio monitoring. | No fallback mechanisms, tight coupling, synchronous dependency chains. |
| [16]    | ~~Unnecessary Processing~~     | Service executes redundant or unnecessary operations.        | (1) Repeated DB queries without data change; (2) Duplicate API calls within request. | Rule-based detection.                                        | Poor service/database interaction design.                    |
| [17]    | The Ramp                       | Processing time increases sharply as dataset grows.          | Latency per API call grows with dataset size.                | Continuous time window analysis shows increasing trend.      | Poor algorithmic scalability, unoptimized queries or data structures. |
| [18]    | Cyclic Dependency              | Occurs when a complete request chain in a microservice system contains circular calls. | Frequent timeouts or unusually long request chains. Changes in one service trigger cascading modifications in other cyclic services. | Next traceId contains in TraceIds.                           | Messy dependencies; overly complex business logic; poor interface design leading to circular service calls. |
