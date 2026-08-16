# UMS IAM Platform - Complete Startup Guide

## Prerequisites Check

```bash
# Verify Docker is installed
docker --version
docker-compose --version

# Verify Git is installed
git --version

# Verify you have at least 8GB RAM available for Docker
```

---

## Step 1: Environment Setup

### 1.1 Clone Repository (if not already done)
```bash
git clone <your-repo-url>
cd ums-iam-platform
```

### 1.2 Create .env File from Template
```bash
cp .env.example .env
```

### 1.3 Edit .env File with Your Values
```bash
# Change critical values:
# - MYSQL_ROOT_PASSWORD (change from default)
# - MYSQL_PASSWORD (change from default)
# - JWT_PRIVATE_KEY_PATH / JWT_PUBLIC_KEY_PATH
# - JWT_KEY_ID
# - MAIL credentials (for notification service)
# - GRAFANA_ADMIN_PASSWORD

# Generate a fresh local RSA keypair outside Git:
mkdir -p secrets/jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/jwt/private_key.pem
openssl rsa -pubout -in secrets/jwt/private_key.pem -out secrets/jwt/public_key.pem
```

### 1.4 Verify Config Repo Structure
```bash
# Check that config files exist
ls -la backend/config-repo/*-docker.yml

# Ensure local Docker JWT key material exists
ls -la secrets/jwt/
```

---

## Step 2: Build Docker Images

### 2.1 Build All Service Images
```bash
# Navigate to each service and build
cd backend/discovery-service
mvn clean package -DskipTests
docker build -t discovery-service:1.0 .

cd ../config-service
mvn clean package -DskipTests
docker build -t config-service:1.0 .

cd ../authentication-service
mvn clean package -DskipTests
docker build -t authentication-service:1.0 .

cd ../user-service
mvn clean package -DskipTests
docker build -t user-service:1.0 .

cd ../authorization-service
mvn clean package -DskipTests
docker build -t authorization-service:1.0 .

cd ../organization-service
mvn clean package -DskipTests
docker build -t organization-service:1.0 .

cd ../notification-service
mvn clean package -DskipTests
docker build -t notification-service:1.0 .

cd ../admin-service
mvn clean package -DskipTests
docker build -t admin-service:1.0 .

cd ../audit-service
mvn clean package -DskipTests
docker build -t audit-service:1.0 .

cd ../api-gateway
mvn clean package -DskipTests
docker build -t api-gateway:1.0 .

cd ../.. # Back to root
```

### 2.2 Verify Images Built Successfully
```bash
docker images | grep -E "discovery|config|authentication|user|authorization|organization|notification|admin|audit|api-gateway"
```

---

## Step 3: Create Shared Network

```bash
# Create the shared network for all containers
docker network create ums-network

# Verify network created
docker network ls | grep ums-network
```

---

## Step 4: Start Main Application Stack

### 4.1 Start Infrastructure Services First
```bash
# Start only infrastructure (MySQL, RabbitMQ, Redis, Eureka, Config)
docker-compose up -d mysql rabbitmq redis discovery-service config-service

# Wait 60 seconds for services to be healthy
sleep 60

# Check health status
docker-compose ps
docker-compose logs config-service | tail -20
```

### 4.2 Verify Config Server is Running
```bash
# Test config server endpoint
curl http://localhost:8888/actuator/health

# Expected response:
# {"status":"UP", ...}
```

### 4.3 Start All Application Services
```bash
# Start remaining services
docker-compose up -d

# Wait for all services to start
sleep 120

# Verify all services are running
docker-compose ps
```

### 4.4 Check Service Health
```bash
# Check each service health endpoint
for port in 8080 8086 8081 8082 8087 8085 8089 8088; do
  echo "Checking port $port..."
  curl -s http://localhost:$port/actuator/health | jq .
done
```

### 4.5 Verify Eureka Service Registration
```bash
# Open browser or curl
curl http://localhost:8761/eureka/apps

# All services should be registered (UP status)
```

---

## Step 5: Start Monitoring Stack (Optional)

### 5.1 Create Monitoring Network Configuration
```bash
# Create Docker Compose override for monitoring to use shared network
# This is already configured in docker-compose.monitoring.yml
```

### 5.2 Start Monitoring Services
```bash
# Start monitoring stack
docker-compose -f docker-compose.monitoring.yml up -d

# Wait for services to start
sleep 60

# Verify all monitoring services running
docker-compose -f docker-compose.monitoring.yml ps
```

### 5.3 Access Monitoring Dashboards
```bash
# Once all services are running, access:
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3000 (admin/admin)
# - Jaeger: http://localhost:16686
# - Kibana: http://localhost:5601
# - Elasticsearch: http://localhost:9200
```

---

## Step 6: Start Frontend (Optional)

### 6.1 Install Dependencies
```bash
cd frontend/admin-portal
npm install
```

### 6.2 Start Development Server
```bash
npm run dev

# Frontend should start on http://localhost:5173
```

### 6.3 Build for Production
```bash
npm run build

# Output will be in dist/ directory
```

---

## Verification Checklist

### ✓ Application Services
- [ ] API Gateway running: `curl http://localhost:8080/actuator/health`
- [ ] Auth Service running: `curl http://localhost:8086/actuator/health`
- [ ] User Service running: `curl http://localhost:8081/actuator/health`
- [ ] All 8 services registered in Eureka
- [ ] Can access Swagger UI: `http://localhost:8086/swagger-ui.html`

### ✓ Data Layer
- [ ] MySQL running: `docker exec ums-mysql mysql -u root -p -e "SHOW DATABASES;"`
- [ ] All 7 databases created (auth_db, user_db, etc.)
- [ ] RabbitMQ running: `curl http://localhost:15672/api/whoami -u guest:guest`
- [ ] Redis running: `docker exec ums-redis redis-cli PING`

### ✓ Monitoring (if started)
- [ ] Prometheus scraping metrics: `http://localhost:9090/targets`
- [ ] Grafana loaded: `http://localhost:3000`
- [ ] Jaeger receiving traces: `http://localhost:16686`
- [ ] Elasticsearch healthy: `curl http://localhost:9200/_cluster/health`
- [ ] Kibana loaded: `http://localhost:5601`

---

## Quick Commands Reference

```bash
# View all running containers
docker-compose ps

# View logs of specific service
docker-compose logs -f authentication-service

# View logs of all services
docker-compose logs -f

# Stop all services
docker-compose down

# Stop with volume cleanup (WARNING: deletes data)
docker-compose down -v

# Restart a service
docker-compose restart authentication-service

# Execute command in running container
docker-compose exec mysql mysql -u root -p

# Build and start from scratch
docker-compose down
docker system prune -f
docker-compose up -d

# Scale a service (example)
docker-compose up -d --scale user-service=3
```

---

## Troubleshooting

### Issue: Services won't start
```bash
# Check logs
docker-compose logs authentication-service

# Check if ports are in use
netstat -tulpn | grep -E "8080|8086|8081|8082|3306|5672|6379"

# Kill process using port (example)
lsof -i :8086
kill -9 <PID>
```

### Issue: Can't connect to MySQL
```bash
# Verify MySQL is healthy
docker-compose ps mysql
docker-compose logs mysql

# Test connection
docker-compose exec mysql mysql -h localhost -u ums_user -p -e "USE auth_db; SHOW TABLES;"
```

### Issue: Services can't find config server
```bash
# Verify config-service is healthy
curl http://localhost:8888/actuator/health

# Check config-service logs
docker-compose logs config-service

# Verify config files exist
ls backend/config-repo/*-docker.yml
```

### Issue: Services registered as DOWN in Eureka
```bash
# Check individual service health
curl http://localhost:8086/actuator/health

# Check service logs for errors
docker-compose logs authentication-service

# Verify database connectivity
docker-compose logs authentication-service | grep -i "datasource\|connection"
```

### Issue: No metrics in Prometheus
```bash
# Verify Prometheus can reach services
curl http://localhost:9090/api/v1/targets

# Check if services expose metrics
curl http://localhost:8086/actuator/prometheus

# Verify Prometheus config
cat observability/prometheus.yml
```

### Issue: Frontend can't reach API
```bash
# Verify API gateway is running
curl http://localhost:8080/actuator/health

# Check frontend environment
cat frontend/admin-portal/.env.development

# Check CORS configuration
curl -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET" http://localhost:8080/

# Check browser console for errors
# Press F12 in browser -> Console tab
```

---

## Complete Flow Example

```bash
# 1. Setup environment
cp .env.example .env
# Edit .env with your values

# 2. Build images
cd backend/discovery-service && mvn clean package -DskipTests && docker build -t discovery-service:1.0 .
# ... (repeat for all services)

# 3. Create network
docker network create ums-network

# 4. Start main stack
docker-compose up -d

# 5. Wait and verify
sleep 120
docker-compose ps
curl http://localhost:8761/eureka/apps

# 6. Start monitoring (optional)
docker-compose -f docker-compose.monitoring.yml up -d

# 7. Start frontend (optional)
cd frontend/admin-portal
npm install
npm run dev

# 8. Access services
# API Gateway: http://localhost:8080
# Swagger: http://localhost:8086/swagger-ui.html
# Grafana: http://localhost:3000
# Jaeger: http://localhost:16686
# Frontend: http://localhost:5173
```

---

## Production Deployment Notes

### Before Production
1. Change all default passwords in .env
2. Generate new JWT secrets
3. Configure TLS/HTTPS certificates
4. Setup external database (not containerized)
5. Setup external message broker (RabbitMQ cluster)
6. Configure log aggregation endpoint
7. Setup proper secret management (Vault, AWS Secrets Manager)
8. Enable authentication on monitoring services
9. Configure backup strategy
10. Run security scanning: `mvn clean verify`

### Kubernetes Deployment
```bash
# Generate Kubernetes manifests (optional)
# Services are containerized and can be deployed to K8s

# Example service definition:
apiVersion: apps/v1
kind: Deployment
metadata:
  name: authentication-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: authentication-service
        image: authentication-service:1.0
        ports:
        - containerPort: 8086
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: CONFIG_SERVER_URL
          value: "http://config-service:8888"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8086
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8086
          initialDelaySeconds: 30
          periodSeconds: 5
```

---

## Support & Documentation

- **API Documentation**: http://localhost:8086/swagger-ui.html
- **Architecture Guide**: See `docs/architecture/`
- **Monitoring Guide**: See `docs/monitoring-observability-guide.md`
- **Event Documentation**: See `docs/asyncapi-events.yml`
- **Refactoring Summary**: See `REFACTORING_SUMMARY.md`

---

## Success Indicators

✅ All 8 services running and healthy
✅ All 7 databases created in MySQL
✅ Eureka showing all services as UP
✅ Can access Swagger UI and test endpoints
✅ Metrics visible in Prometheus dashboard
✅ No critical errors in logs
✅ Frontend loading (if started)

---

**Congratulations! Your UMS IAM Platform is ready for development/testing!**
