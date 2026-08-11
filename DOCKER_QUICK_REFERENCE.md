# UMS IAM Platform - Docker Quick Reference

## Network Management

```bash
# Create shared network
docker network create ums-network

# List networks
docker network ls

# Inspect network
docker network inspect ums-network

# Remove network
docker network rm ums-network
```

---

## Container Lifecycle

### Start Services
```bash
# Start all services
docker-compose up -d

# Start specific service
docker-compose up -d authentication-service

# Start in foreground (see logs)
docker-compose up authentication-service

# Start and rebuild images
docker-compose up -d --build
```

### Stop Services
```bash
# Stop all services (data persists)
docker-compose down

# Stop all services and remove volumes (WARNING: data loss)
docker-compose down -v

# Stop specific service
docker-compose stop authentication-service

# Restart service
docker-compose restart authentication-service
```

### View Status
```bash
# List all containers
docker-compose ps

# Detailed view
docker-compose ps --services

# Watch in real-time
watch -n 1 docker-compose ps
```

---

## Logging

```bash
# View logs of specific service
docker-compose logs authentication-service

# Follow logs (live)
docker-compose logs -f authentication-service

# Last 100 lines
docker-compose logs --tail=100 authentication-service

# View all logs
docker-compose logs

# Follow all logs
docker-compose logs -f

# Since specific time
docker-compose logs --since 2024-06-23T10:00:00 authentication-service

# Save logs to file
docker-compose logs > logs.txt
```

---

## Execute Commands in Containers

```bash
# Connect to MySQL
docker-compose exec mysql mysql -u ums_user -p auth_db

# Execute bash in container
docker-compose exec authentication-service /bin/bash

# Run single command
docker-compose exec authentication-service ls -la

# Run as root
docker-compose exec -u root authentication-service apt-get update
```

---

## Health Checks

```bash
# Check service health
docker-compose exec authentication-service curl http://localhost:8086/actuator/health

# Check all services health
for service in authentication user authorization organization notification admin audit; do
  echo "=== ${service}-service ==="
  docker-compose exec ${service}-service curl http://localhost:808?/actuator/health 2>/dev/null | jq .status
done

# From host machine
curl http://localhost:8086/actuator/health | jq
curl http://localhost:8080/actuator/health | jq
```

---

## Database Operations

```bash
# Connect to MySQL
docker-compose exec mysql mysql -u root -p

# List databases
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"

# List tables in database
docker-compose exec mysql mysql -u root -p -e "USE auth_db; SHOW TABLES;"

# Run SQL query
docker-compose exec mysql mysql -u root -p -e "SELECT * FROM users;" auth_db

# Backup database
docker-compose exec mysql mysqldump -u root -p auth_db > backup.sql

# Restore database
docker-compose exec -T mysql mysql -u root -p auth_db < backup.sql

# Execute SQL file
docker-compose exec mysql mysql -u root -p < script.sql
```

---

## Monitoring & Performance

```bash
# Monitor resource usage
docker stats

# Specific container stats
docker stats ums-authentication-service

# View container processes
docker-compose top authentication-service

# Inspect container details
docker inspect ums-authentication-service

# Container memory usage
docker stats --no-stream ums-mysql | grep ums-mysql
```

---

## Image Management

```bash
# List images
docker images | grep -E "discovery|authentication|user|authorization"

# Remove image
docker rmi authentication-service:1.0

# Remove unused images
docker image prune -a

# Tag image
docker tag authentication-service:1.0 authentication-service:latest

# Build image
cd backend/authentication-service
mvn clean package -DskipTests
docker build -t authentication-service:1.0 .
```

---

## Volume Management

```bash
# List volumes
docker volume ls | grep ums

# Inspect volume
docker volume inspect ums-iam-platform_mysql_data

# Remove unused volumes
docker volume prune

# Backup volume
docker run --rm -v ums-iam-platform_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz /data

# Restore volume
docker run --rm -v ums-iam-platform_mysql_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-backup.tar.gz -C /
```

---

## Monitoring Stack

```bash
# Start monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Stop monitoring services
docker-compose -f docker-compose.monitoring.yml down

# View monitoring logs
docker-compose -f docker-compose.monitoring.yml logs prometheus

# View Grafana logs
docker-compose -f docker-compose.monitoring.yml logs grafana

# Access monitoring services
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000
# Jaeger: http://localhost:16686
# Kibana: http://localhost:5601
```

---

## Useful Aliases

Add to `~/.bashrc` or `~/.zshrc`:

```bash
# UMS IAM Platform shortcuts
alias ums-up='docker-compose up -d'
alias ums-down='docker-compose down'
alias ums-logs='docker-compose logs -f'
alias ums-ps='docker-compose ps'
alias ums-restart='docker-compose restart'
alias ums-clean='docker-compose down -v && docker system prune -f'
alias ums-monitor-up='docker-compose -f docker-compose.monitoring.yml up -d'
alias ums-monitor-down='docker-compose -f docker-compose.monitoring.yml down'
alias ums-mysql='docker-compose exec mysql mysql -u root -p'
alias ums-health='curl http://localhost:8761/eureka/apps | xmllint --format -'

# Load aliases
source ~/.bashrc
```

---

## Common Scenarios

### Scenario: Service Failed to Start
```bash
# 1. Check logs
docker-compose logs authentication-service | tail -50

# 2. Verify dependencies
docker-compose ps

# 3. Check configuration
docker-compose exec authentication-service env | grep MYSQL

# 4. Check health endpoint
curl http://localhost:8086/actuator/health

# 5. Restart service
docker-compose restart authentication-service
```

### Scenario: Database Connection Failed
```bash
# 1. Verify MySQL is running
docker-compose exec mysql mysqladmin ping

# 2. Check credentials in .env
grep MYSQL .env

# 3. Test connection
docker-compose exec mysql mysql -u ums_user -p -h mysql auth_db -e "SELECT 1"

# 4. Check MySQL logs
docker-compose logs mysql

# 5. Verify database exists
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
```

### Scenario: Can't Connect to API
```bash
# 1. Verify API Gateway running
curl http://localhost:8080/actuator/health

# 2. Check service registration
curl http://localhost:8761/eureka/apps

# 3. Check API Gateway logs
docker-compose logs api-gateway

# 4. Check authentication service
curl http://localhost:8086/actuator/health

# 5. Check service discovery
docker-compose logs discovery-service
```

### Scenario: No Metrics in Prometheus
```bash
# 1. Verify service metrics endpoint
curl http://localhost:8086/actuator/prometheus | head -20

# 2. Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq

# 3. Check Prometheus config
cat observability/prometheus.yml | grep -A5 "scrape_configs"

# 4. Check Prometheus logs
docker-compose -f docker-compose.monitoring.yml logs prometheus

# 5. Restart Prometheus
docker-compose -f docker-compose.monitoring.yml restart prometheus
```

---

## Cleanup Operations

```bash
# Remove all stopped containers
docker container prune -f

# Remove all dangling images
docker image prune -f

# Remove all unused images
docker image prune -a -f

# Remove all unused volumes
docker volume prune -f

# Complete cleanup (WARNING: removes everything)
docker system prune -a -f --volumes

# Stop and remove everything related to UMS IAM
docker-compose down -v
docker-compose -f docker-compose.monitoring.yml down -v
docker network rm ums-network
docker system prune -a -f
```

---

## Performance Tuning

```bash
# Increase Docker memory limit
# Edit ~/.docker/daemon.json:
{
  "memory": "4g",
  "memswap": "4g",
  "cpus": "2.0"
}

# Restart Docker daemon
sudo systemctl restart docker

# Check resource limits
docker stats --no-stream

# Limit specific container memory
docker-compose exec authentication-service docker update --memory 1g ums-authentication-service
```

---

## Debugging

```bash
# Get container IP
docker-compose exec authentication-service hostname -I

# Network inspection
docker network inspect ums-network | jq '.Containers'

# DNS resolution
docker-compose exec authentication-service nslookup mysql
docker-compose exec authentication-service nslookup config-service

# Ping other services
docker-compose exec authentication-service ping -c 3 mysql

# Test port connectivity
docker-compose exec authentication-service nc -zv config-service 8888

# View environment variables
docker-compose exec authentication-service env | sort
```

---

## Useful Commands Cheatsheet

| Command | Purpose |
|---------|---------|
| `docker-compose up -d` | Start all services |
| `docker-compose ps` | List all services |
| `docker-compose logs -f service` | View service logs |
| `docker-compose exec service bash` | Connect to service |
| `docker-compose restart service` | Restart service |
| `docker-compose down` | Stop all services |
| `docker network ls` | List networks |
| `docker volume ls` | List volumes |
| `docker stats` | View resource usage |
| `docker system prune` | Clean up unused resources |

---

## Help & Documentation

```bash
# Get help for docker-compose
docker-compose --help

# Get help for specific command
docker-compose up --help

# View full docker-compose.yml
cat docker-compose.yml

# View environment file
cat .env

# View config server files
ls -la backend/config-repo/*-docker.yml

# View MySQL init script
cat docker/mysql/init.sql

# View monitoring config
cat observability/prometheus.yml
cat observability/logstash.conf
```

---

This quick reference covers 80% of common Docker operations for the UMS IAM Platform. For more information, see STARTUP_GUIDE.md or official Docker documentation.
