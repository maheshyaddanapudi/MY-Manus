# Observability & Monitoring Guide

**Version:** 1.0
**Date:** November 2025
**Stack:** Micrometer + Prometheus + Grafana

---

## Table of Contents

1. [Overview](#overview)
2. [Metrics Stack](#metrics-stack)
3. [Available Metrics](#available-metrics)
4. [Prometheus Setup](#prometheus-setup)
5. [Grafana Dashboards](#grafana-dashboards)
6. [Custom Metrics](#custom-metrics)
7. [Alerts](#alerts)
8. [Troubleshooting](#troubleshooting)

---

## Overview

MY-Manus provides **comprehensive observability** through:

- ✅ **Spring Boot Actuator**: Health checks, metrics endpoints
- ✅ **Micrometer**: Metrics collection library
- ✅ **Prometheus**: Time-series metrics database
- ✅ **Grafana**: Visualization and dashboards
- ✅ **Custom Business Metrics**: Agent-specific metrics

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│             MY-Manus Backend (Spring Boot)                   │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Micrometer Metrics Registry                       │    │
│  │  • JVM metrics (memory, threads, GC)               │    │
│  │  • HTTP metrics (requests, latency, errors)        │    │
│  │  • Database metrics (connections, queries)         │    │
│  │  • Custom business metrics                         │    │
│  └────────────────────────────────────────────────────┘    │
│                           ↓                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Spring Boot Actuator                              │    │
│  │  GET /actuator/prometheus                          │    │
│  │  (Exposes metrics in Prometheus format)            │    │
│  └────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
                           ↓ HTTP Scrape (every 15s)
┌──────────────────────────────────────────────────────────────┐
│                    Prometheus Server                         │
│  • Scrapes /actuator/prometheus endpoint                    │
│  • Stores time-series data                                  │
│  • Evaluates alerting rules                                 │
│  • Provides PromQL query interface                          │
└──────────────────────────────────────────────────────────────┘
                           ↓ PromQL Queries
┌──────────────────────────────────────────────────────────────┐
│                      Grafana                                 │
│  • Visualizes metrics in dashboards                          │
│  • Sends alerts via email/Slack/PagerDuty                   │
│  • Provides query builder                                   │
│  • Historical data analysis                                  │
└──────────────────────────────────────────────────────────────┘
```

---

## Metrics Stack

### Spring Boot Actuator

**Configuration** (`application.properties`):

```properties
# Actuator Endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true

# Metrics tags (for filtering)
management.metrics.tags.application=my-manus
management.metrics.tags.environment=${ENV:development}

# Health indicators
management.health.db.enabled=true
management.health.diskspace.enabled=true
```

**Available Actuator Endpoints:**

| Endpoint | Purpose | Example |
|----------|---------|---------|
| `/actuator/health` | Health check (liveness/readiness) | `{"status": "UP"}` |
| `/actuator/info` | Application info | `{"app": "my-manus", "version": "1.0"}` |
| `/actuator/metrics` | List all metrics | `{"names": ["jvm.memory.used", ...]}` |
| `/actuator/metrics/{name}` | Get specific metric | `/actuator/metrics/jvm.memory.used` |
| `/actuator/prometheus` | **Prometheus format** | Scrapable metrics |

### Micrometer

**Dependency** (`pom.xml`):

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Metric Types:**

1. **Counter**: Monotonically increasing (e.g., total requests)
2. **Gauge**: Current value that can go up/down (e.g., active sessions)
3. **Timer**: Duration and count of events (e.g., API latency)
4. **DistributionSummary**: Distribution of values (e.g., response sizes)

---

## Available Metrics

### 1. JVM Metrics (Built-in)

**Memory:**
```
jvm.memory.used{area="heap"}         # Heap memory used (bytes)
jvm.memory.max{area="heap"}          # Max heap size
jvm.memory.committed{area="heap"}    # Committed heap
jvm.gc.pause                         # GC pause duration
jvm.gc.memory.allocated              # Memory allocated
```

**Threads:**
```
jvm.threads.live                     # Current thread count
jvm.threads.daemon                   # Daemon thread count
jvm.threads.peak                     # Peak thread count
jvm.threads.states{state="runnable"} # Threads by state
```

**CPU:**
```
process.cpu.usage                    # Process CPU usage (0-1)
system.cpu.usage                     # System CPU usage (0-1)
system.cpu.count                     # Number of CPUs
```

---

### 2. HTTP Metrics (Built-in)

```
http.server.requests.count{uri="/api/agent/chat"}     # Request count
http.server.requests.seconds{uri="/api/agent/chat"}   # Request duration
http.server.requests{status="200"}                    # Successful requests
http.server.requests{status="500"}                    # Server errors
```

**Example Query:**
```promql
# Request rate (requests per second)
rate(http_server_requests_seconds_count{uri="/api/agent/chat"}[1m])

# 95th percentile latency
histogram_quantile(0.95, http_server_requests_seconds_bucket{uri="/api/agent/chat"})
```

---

### 3. Database Metrics (Built-in)

**HikariCP Connection Pool:**
```
hikaricp.connections.active             # Active connections
hikaricp.connections.idle               # Idle connections
hikaricp.connections.pending            # Waiting connections
hikaricp.connections.max                # Max pool size
hikaricp.connections.timeout            # Connection timeouts
```

**JDBC:**
```
jdbc.connections.active                 # Active JDBC connections
```

---

### 4. Spring AI Metrics (Auto-registered)

```
spring.ai.chat.client.call.duration     # LLM API call duration
spring.ai.chat.client.call.count        # Total LLM calls
spring.ai.chat.client.tokens.input      # Input tokens used
spring.ai.chat.client.tokens.output     # Output tokens generated
```

**Example:**
```promql
# Total tokens used (cost tracking)
sum(spring_ai_chat_client_tokens_input + spring_ai_chat_client_tokens_output)

# Average LLM latency
avg(spring_ai_chat_client_call_duration_seconds)
```

---

### 5. Custom Business Metrics

#### Notifications

**Implementation** (`NotificationService.java`):

```java
@Service
public class NotificationService {
    private final MeterRegistry meterRegistry;
    private Counter notificationsSent;
    private Counter notificationsRead;

    @PostConstruct
    public void initMetrics() {
        notificationsSent = Counter.builder("notifications.sent")
            .description("Total notifications sent")
            .tag("type", "all")
            .register(meterRegistry);

        notificationsRead = Counter.builder("notifications.read")
            .description("Total notifications marked as read")
            .register(meterRegistry);
    }

    public Notification sendNotification(Notification notification) {
        // ... send logic ...

        notificationsSent.increment();

        Counter.builder("notifications.sent.by.type")
            .tag("type", notification.getType().toString())
            .register(meterRegistry)
            .increment();

        return notification;
    }
}
```

**Metrics:**
```
notifications.sent.total                               # All notifications
notifications.sent.by.type{type="TASK_COMPLETED"}     # By type
notifications.sent.by.type{type="TASK_FAILED"}
notifications.sent.by.type{type="AGENT_WAITING"}
notifications.read.total                               # Read notifications
```

---

#### Multi-Turn Message Classification

**Implementation** (`MessageClassifier.java`):

```java
@Service
public class MessageClassifier {
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void initMetrics() {
        Counter.builder("messages.classified.task")
            .description("Messages classified as TASK")
            .register(meterRegistry);

        Counter.builder("messages.classified.query")
            .description("Messages classified as QUERY")
            .register(meterRegistry);

        Counter.builder("messages.classified.adjustment")
            .description("Messages classified as ADJUSTMENT")
            .register(meterRegistry);
    }

    public MessageType classify(String sessionId, String message) {
        MessageType type = doClassification(message);

        Counter.builder("messages.classified." + type.name().toLowerCase())
            .register(meterRegistry)
            .increment();

        return type;
    }
}
```

**Metrics:**
```
messages.classified.task.total           # New task requests
messages.classified.query.total          # Quick questions
messages.classified.adjustment.total     # Plan adjustments
```

---

#### Plan Visualization

**Implementation** (`TodoMdWatcher.java`):

```java
@Service
public class TodoMdWatcher {
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void initMetrics() {
        Counter.builder("plan.updates")
            .description("Plan file updates detected")
            .register(meterRegistry);
    }

    private void handleFileChange(Path path) {
        // ... parse todo.md ...

        Counter.builder("plan.updates")
            .register(meterRegistry)
            .increment();
    }
}
```

**Metrics:**
```
plan.updates.total                      # Total plan updates
```

---

### 6. Agent Execution Metrics (Recommended Custom Metrics)

**Add to `CodeActAgentService.java`:**

```java
@Service
public class CodeActAgentService {
    private final MeterRegistry meterRegistry;
    private Counter agentTasksCompleted;
    private Counter agentTasksFailed;
    private Timer agentExecutionTime;

    @PostConstruct
    public void initMetrics() {
        agentTasksCompleted = Counter.builder("agent.tasks.completed")
            .description("Total tasks completed successfully")
            .register(meterRegistry);

        agentTasksFailed = Counter.builder("agent.tasks.failed")
            .description("Total tasks that failed")
            .register(meterRegistry);

        agentExecutionTime = Timer.builder("agent.execution.time")
            .description("Time to complete agent tasks")
            .register(meterRegistry);

        Gauge.builder("agent.active.sessions", sessionCache::size)
            .description("Currently active agent sessions")
            .register(meterRegistry);
    }

    public String processQuery(String sessionId, String userQuery) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            String result = executeAgentLoop(sessionId);
            agentTasksCompleted.increment();
            return result;
        } catch (Exception e) {
            agentTasksFailed.increment();
            throw e;
        } finally {
            sample.stop(agentExecutionTime);
        }
    }
}
```

**Metrics:**
```
agent.tasks.completed.total             # Successful tasks
agent.tasks.failed.total                # Failed tasks
agent.execution.time.seconds            # Task duration
agent.active.sessions                   # Active sessions (gauge)
```

---

## Prometheus Setup

### Installation

**Docker Compose** (`docker-compose.yml`):

```yaml
version: '3.8'

services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'

volumes:
  prometheus-data:
```

### Configuration

**Create `prometheus/prometheus.yml`:**

```yaml
global:
  scrape_interval: 15s      # Scrape every 15 seconds
  evaluation_interval: 15s  # Evaluate rules every 15 seconds

# Scrape configs
scrape_configs:
  - job_name: 'my-manus-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
        labels:
          application: 'my-manus'
          environment: 'production'

# Alerting rules (optional)
rule_files:
  - 'alerts.yml'
```

### Start Prometheus

```bash
docker-compose up -d prometheus

# Access Prometheus UI
open http://localhost:9090
```

### Test Prometheus

**Query examples in Prometheus UI:**

```promql
# All MY-Manus metrics
{application="my-manus"}

# JVM heap usage
jvm_memory_used_bytes{area="heap"}

# HTTP request rate
rate(http_server_requests_seconds_count[1m])

# Notification rate
rate(notifications_sent_total[5m])
```

---

## Grafana Dashboards

### Installation

**Add to `docker-compose.yml`:**

```yaml
grafana:
  image: grafana/grafana:latest
  ports:
    - "3001:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_USERS_ALLOW_SIGN_UP=false
  volumes:
    - grafana-data:/var/lib/grafana
    - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
    - ./grafana/datasources:/etc/grafana/provisioning/datasources

volumes:
  grafana-data:
```

### Configure Prometheus Data Source

**Create `grafana/datasources/prometheus.yml`:**

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

### Start Grafana

```bash
docker-compose up -d grafana

# Access Grafana
open http://localhost:3001

# Login: admin / admin
```

---

### Example Dashboard 1: System Overview

**Purpose:** Monitor overall system health

**Panels:**

1. **JVM Heap Usage**
   ```promql
   jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
   ```
   - Type: Gauge
   - Unit: Percent (%)
   - Thresholds: Warn: 75%, Critical: 90%

2. **HTTP Request Rate**
   ```promql
   sum(rate(http_server_requests_seconds_count[1m])) by (uri)
   ```
   - Type: Graph (time series)
   - Unit: req/sec
   - Legend: By URI

3. **HTTP Error Rate**
   ```promql
   sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))
   ```
   - Type: Stat
   - Unit: req/sec
   - Color: Red if > 0

4. **Active Database Connections**
   ```promql
   hikaricp_connections_active
   ```
   - Type: Gauge
   - Max: hikaricp_connections_max

5. **CPU Usage**
   ```promql
   process_cpu_usage * 100
   ```
   - Type: Graph
   - Unit: Percent (%)

---

### Example Dashboard 2: Agent Performance

**Purpose:** Monitor agent execution metrics

**Panels:**

1. **Tasks Completed vs Failed**
   ```promql
   sum(increase(agent_tasks_completed_total[1h]))
   sum(increase(agent_tasks_failed_total[1h]))
   ```
   - Type: Stat + Bar gauge
   - Time range: Last hour

2. **Task Success Rate**
   ```promql
   (
     sum(rate(agent_tasks_completed_total[5m]))
     /
     (sum(rate(agent_tasks_completed_total[5m])) + sum(rate(agent_tasks_failed_total[5m])))
   ) * 100
   ```
   - Type: Gauge
   - Unit: Percent (%)
   - Target: 95%

3. **Agent Execution Time (p95)**
   ```promql
   histogram_quantile(0.95, agent_execution_time_seconds_bucket)
   ```
   - Type: Graph
   - Unit: Seconds
   - Description: 95% of tasks complete within this time

4. **Active Sessions**
   ```promql
   agent_active_sessions
   ```
   - Type: Stat
   - Sparkline: Show trend

5. **LLM API Latency**
   ```promql
   histogram_quantile(0.50, spring_ai_chat_client_call_duration_seconds_bucket)
   histogram_quantile(0.95, spring_ai_chat_client_call_duration_seconds_bucket)
   histogram_quantile(0.99, spring_ai_chat_client_call_duration_seconds_bucket)
   ```
   - Type: Graph
   - Legend: p50, p95, p99
   - Unit: Seconds

---

### Example Dashboard 3: Business Metrics

**Purpose:** Monitor application-specific features

**Panels:**

1. **Notifications by Type**
   ```promql
   sum(increase(notifications_sent_by_type_total[1h])) by (type)
   ```
   - Type: Pie chart
   - Legend: Notification type

2. **Notification Read Rate**
   ```promql
   (
     sum(rate(notifications_read_total[5m]))
     /
     sum(rate(notifications_sent_total[5m]))
   ) * 100
   ```
   - Type: Gauge
   - Unit: Percent (%)
   - Target: >60%

3. **Message Classification Distribution**
   ```promql
   sum(increase(messages_classified_task_total[1h]))
   sum(increase(messages_classified_query_total[1h]))
   sum(increase(messages_classified_adjustment_total[1h]))
   ```
   - Type: Bar chart
   - Description: TASK vs QUERY vs ADJUSTMENT

4. **Plan Updates Over Time**
   ```promql
   rate(plan_updates_total[5m])
   ```
   - Type: Graph
   - Description: How often agents update their plans

---

### Dashboard JSON Export

**Example Panel JSON** (JVM Heap Usage):

```json
{
  "id": 1,
  "title": "JVM Heap Usage",
  "type": "gauge",
  "targets": [
    {
      "expr": "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"} * 100",
      "legendFormat": "Heap Usage"
    }
  ],
  "options": {
    "unit": "percent",
    "min": 0,
    "max": 100,
    "thresholds": {
      "mode": "absolute",
      "steps": [
        {"value": 0, "color": "green"},
        {"value": 75, "color": "yellow"},
        {"value": 90, "color": "red"}
      ]
    }
  }
}
```

---

## Custom Metrics

### How to Add Custom Metrics

**Step 1: Inject MeterRegistry**

```java
@Service
public class MyService {
    private final MeterRegistry meterRegistry;

    public MyService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
}
```

**Step 2: Create Metrics**

```java
@PostConstruct
public void initMetrics() {
    // Counter
    Counter.builder("my.custom.counter")
        .description("Description of what this counts")
        .tag("environment", "prod")
        .register(meterRegistry);

    // Gauge
    Gauge.builder("my.custom.gauge", this::getCurrentValue)
        .description("Current value of something")
        .register(meterRegistry);

    // Timer
    Timer.builder("my.custom.timer")
        .description("Duration of operations")
        .register(meterRegistry);
}
```

**Step 3: Use Metrics**

```java
public void doSomething() {
    // Increment counter
    meterRegistry.counter("my.custom.counter").increment();

    // Time an operation
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
        performOperation();
    } finally {
        sample.stop(meterRegistry.timer("my.custom.timer"));
    }
}
```

---

## Alerts

### Alerting Rules

**Create `prometheus/alerts.yml`:**

```yaml
groups:
  - name: my-manus-alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP error rate"
          description: "Error rate is {{ $value }} req/sec"

      # High heap usage
      - alert: HighHeapUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap usage is high"
          description: "Heap usage is {{ $value | humanizePercentage }}"

      # Task failure rate
      - alert: HighTaskFailureRate
        expr: |
          (
            rate(agent_tasks_failed_total[5m])
            /
            (rate(agent_tasks_completed_total[5m]) + rate(agent_tasks_failed_total[5m]))
          ) > 0.10
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High agent task failure rate"
          description: "Failure rate is {{ $value | humanizePercentage }}"

      # Database connection pool exhaustion
      - alert: ConnectionPoolExhausted
        expr: hikaricp_connections_pending > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool exhausted"
          description: "{{ $value }} connections waiting"
```

### Alertmanager Setup

**Add to `docker-compose.yml`:**

```yaml
alertmanager:
  image: prom/alertmanager:latest
  ports:
    - "9093:9093"
  volumes:
    - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
```

**Configure `alertmanager/alertmanager.yml`:**

```yaml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@my-manus.com'
  smtp_auth_username: 'your-email@gmail.com'
  smtp_auth_password: 'your-password'

route:
  receiver: 'email'
  group_by: ['alertname']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: 'email'
    email_configs:
      - to: 'team@my-manus.com'
        headers:
          Subject: '[MY-Manus] {{ .GroupLabels.alertname }}'
```

---

## Troubleshooting

### 1. Metrics Not Showing in Prometheus

**Problem:** Prometheus can't scrape backend

**Solutions:**

```bash
# Check Actuator endpoint is accessible
curl http://localhost:8080/actuator/prometheus

# Check Prometheus targets page
open http://localhost:9090/targets

# Verify prometheus.yml config
docker exec prometheus cat /etc/prometheus/prometheus.yml

# Check backend logs
docker logs my-manus-backend
```

### 2. High Memory Usage

**Problem:** JVM heap growing indefinitely

**Diagnosis:**

```promql
# Heap usage trend
jvm_memory_used_bytes{area="heap"}

# GC activity
rate(jvm_gc_pause_seconds_count[5m])

# Check for memory leaks
increase(jvm_memory_used_bytes{area="heap"}[1h])
```

**Solutions:**
- Increase heap size: `-Xmx2g`
- Tune GC: `-XX:+UseG1GC`
- Profile with VisualVM
- Check for memory leaks in code

### 3. Slow Queries

**Problem:** High database latency

**Diagnosis:**

```promql
# Connection pool usage
hikaricp_connections_active / hikaricp_connections_max

# Pending connections
hikaricp_connections_pending
```

**Solutions:**
- Increase pool size
- Add database indexes
- Optimize queries
- Enable query logging

---

## Summary

MY-Manus provides **production-grade observability** with:

✅ **Automatic JVM Metrics**: Memory, threads, GC
✅ **HTTP Metrics**: Request rate, latency, errors
✅ **Database Metrics**: Connection pools, query performance
✅ **Spring AI Metrics**: LLM calls, token usage
✅ **Custom Business Metrics**: Notifications, multi-turn, plans
✅ **Prometheus Integration**: Time-series storage
✅ **Grafana Dashboards**: Visual monitoring
✅ **Alerting**: Proactive issue detection

**Key Endpoints:**
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`
- Prometheus UI: `http://localhost:9090`
- Grafana: `http://localhost:3001`

---

## Next Steps

**Set Up Monitoring:**
```bash
cd MY-Manus
docker-compose up -d prometheus grafana
```

**Access Dashboards:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)

**Explore More:**
- [Architecture Guide →](../architecture/ARCHITECTURE.md)
- [Deployment Guide →](DEPLOYMENT.md)
- [API Reference →](API_REFERENCE.md)

---

**Document Version:** 1.0
**Last Updated:** November 2025
**Next:** [Multi-Turn Scenarios →](MULTI_TURN_SCENARIOS.md)
