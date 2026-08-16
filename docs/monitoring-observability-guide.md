# Monitoring & Observability Setup Guide

## Overview

The UMS IAM Platform uses a comprehensive monitoring and observability stack to ensure system reliability, performance visibility, and quick incident response.

## Stack Components

### 1. **Prometheus** (Metrics Collection)
- **Port**: 9090
- **Purpose**: Time-series database for collecting metrics from all services
- **Config**: `observability/prometheus.yml`
- **Access**: http://localhost:9090

### 2. **Grafana** (Visualization)
- **Port**: 3000
- **Default Credentials**: admin / admin
- **Purpose**: Visualizes metrics and creates dashboards
- **Access**: http://localhost:3000

### 3. **Jaeger** (Distributed Tracing)
- **Port**: 16686 (UI)
- **Purpose**: Traces requests across microservices
- **Access**: http://localhost:16686

### 4. **Elasticsearch** (Log Storage)
- **Port**: 9200
- **Purpose**: Stores and indexes logs for searching

### 5. **Kibana** (Log Visualization)
- **Port**: 5601
- **Purpose**: Visualizes logs and creates dashboards
- **Access**: http://localhost:5601

### 6. **Logstash** (Log Processing)
- **Purpose**: Processes and enriches logs before storing in Elasticsearch
- **Config**: `observability/logstash.conf`

## Starting the Monitoring Stack

```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

## Key Metrics to Monitor

### Application Metrics
- `http_requests_total` - Total HTTP requests
- `http_request_duration_seconds` - Request latency
- `jvm_memory_used_bytes` - JVM memory usage
- `process_cpu_usage` - CPU usage
- `db_connection_pool_active` - Active database connections

### Business Metrics
- `authentication_login_attempts_total` - Login attempts
- `authentication_login_success_total` - Successful logins
- `user_registration_total` - New user registrations
- `role_assignment_total` - Role assignments

## Health Checks

Each service exposes health endpoints:
- `GET /actuator/health` - Basic health status
- `GET /actuator/health/liveness` - Liveness probe
- `GET /actuator/health/readiness` - Readiness probe

### Example Health Check Response
```json
{
  "status": "UP",
  "components": {
    "authServiceHealth": {
      "status": "UP",
      "details": {
        "database": "connected",
        "cache": "connected",
        "service": "operational"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1000000000,
        "free": 500000000,
        "threshold": 10000000
      }
    }
  }
}
```

## Setting Up Prometheus Alerts

Example alert rule in `observability/prometheus-rules.yml`:

```yaml
groups:
  - name: ums_iam_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"

      - alert: HighLatency
        expr: histogram_quantile(0.95, http_request_duration_seconds_bucket) > 1
        for: 5m
        annotations:
          summary: "High API latency detected"

      - alert: LowDiskSpace
        expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.1
        for: 5m
        annotations:
          summary: "Low disk space"
```

## Grafana Dashboard Setup

### Importing Dashboards

1. Go to Grafana (http://localhost:3000)
2. Click "+ Create" → "Import"
3. Import community dashboards:
   - **JVM Micrometer**: 4701
   - **Spring Boot Application**: 11955
   - **Jaeger**: 13207

### Creating Custom Dashboards

Sample queries:

```promql
# Request Rate
rate(http_requests_total[5m])

# Error Rate
rate(http_requests_total{status=~"5.."}[5m])

# P95 Latency
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Memory Usage
jvm_memory_used_bytes / jvm_memory_max_bytes
```

## Distributed Tracing Setup

### Configuring Jaeger in Services

Add to `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests
  otlp:
    tracing:
      endpoint: http://jaeger:4317
      compression: gzip
```

### Viewing Traces

1. Go to Jaeger UI (http://localhost:16686)
2. Select service from dropdown
3. View trace details and latency breakdowns

## Centralized Logging

### Sending Logs to ELK Stack

Add to `logback-spring.xml`:

```xml
<appender name="logstash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>localhost:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"service_name":"auth-service"}</customFields>
    </encoder>
</appender>

<root level="INFO">
    <appender-ref ref="logstash" />
</root>
```

### Querying Logs in Kibana

1. Go to Kibana (http://localhost:5601)
2. Create index pattern: `ums-iam-*`
3. Use Kibana Query Language (KQL) to search:
   ```
   service_name: "authentication-service" AND level: "ERROR"
   ```

## Performance Tuning

### Prometheus Retention
```yaml
# In docker-compose.monitoring.yml
command:
  - '--storage.tsdb.retention.time=30d'
  - '--storage.tsdb.retention.size=50GB'
```

### Elasticsearch Optimization
```yaml
environment:
  - indices.memory.index_buffer_size=40%
  - thread_pool.search.queue_size=1000
```

## Alerting Strategy

### Alert Channels

1. **Email**: Configure AlertManager with SMTP
2. **Slack**: Add webhook to AlertManager
3. **PagerDuty**: Integrate for on-call escalation

### Critical Alerts

- Service down (health check failing)
- Error rate > 5%
- Response time P95 > 2s
- Memory usage > 85%
- Disk space < 10%
- Database connection pool exhausted

## Troubleshooting

### Services not appearing in Prometheus

1. Check service health: `curl http://localhost:8086/actuator/health`
2. Verify metrics endpoint: `curl http://localhost:8086/actuator/prometheus`
3. Check prometheus.yml configuration

### No logs in Kibana

1. Verify Logstash is running: `docker logs logstash`
2. Check logs are being sent to port 5000
3. Verify Elasticsearch is running

### Traces not appearing in Jaeger

1. Verify OTEL environment variables are set
2. Check Jaeger collector: `curl http://localhost:14268/api/traces`
3. Verify sampling probability > 0

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
