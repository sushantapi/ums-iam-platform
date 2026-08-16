# UMS IAM Platform - Quick Start Guide

## Prerequisites

- Docker & Docker Compose
- Node.js 18+ (for frontend)
- Java 21
- Maven 3.9+

## Quick Setup (5 minutes)

### 1. Setup Environment
```bash
# Clone environment template
cp .env.example .env

# Edit with your settings
# At minimum, change:
# - MYSQL_ROOT_PASSWORD
# - JWT_PRIVATE_KEY_PATH / JWT_PUBLIC_KEY_PATH
# - JWT_KEY_ID
```

### 2. Start Backend Services
```bash
docker-compose up -d

# Wait for services to start (~30 seconds)
docker-compose ps
```

### 3. Start Frontend
```bash
cd frontend/admin-portal

npm install
npm run dev
```

Access frontend at: http://localhost:5173

### 4. (Optional) Start Monitoring
```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

## Key URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Admin Portal | http://localhost:5173 | - |
| API Gateway | http://localhost:8080 | - |
| Swagger UI | http://localhost:8086/swagger-ui.html | - |
| Grafana | http://localhost:3000 | admin / admin |
| Jaeger UI | http://localhost:16686 | - |
| Kibana | http://localhost:5601 | - |

## Common Tasks

### View Logs
```bash
docker-compose logs -f authentication-service
docker-compose logs -f api-gateway
```

### Restart Service
```bash
docker-compose restart authentication-service
```

### Stop Everything
```bash
docker-compose down
docker-compose -f docker-compose.monitoring.yml down
```

### Rebuild Services
```bash
docker-compose down
docker system prune -f
docker-compose up -d --build
```

## Development Workflow

### Backend Changes
1. Edit Java files in `backend/*/src`
2. Changes auto-compile with spring-boot-devtools
3. Service restarts automatically

### Frontend Changes
1. Edit files in `frontend/admin-portal/src`
2. Vite hot-reloads automatically
3. No page refresh needed

### Database Changes
1. Create migration in `backend/*/src/main/resources/db/migration`
2. Flyway auto-migrates on startup

## Testing

### Run Integration Tests
```bash
cd backend/authentication-service
mvn test
```

### Build Frontend
```bash
cd frontend/admin-portal
npm run build
```

## Troubleshooting

### Services won't start
```bash
# Check logs
docker-compose logs

# Reset volumes
docker-compose down -v
docker-compose up -d
```

### Can't connect to database
```bash
# Check MySQL is running
docker-compose ps mysql

# Check credentials in .env
echo $MYSQL_PASSWORD
```

### Frontend can't reach API
```bash
# Check .env.development has correct API URL
cat frontend/admin-portal/.env.development

# Verify API gateway is running
curl http://localhost:8080/actuator/health
```

### No metrics in Grafana
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Verify service metrics endpoint
curl http://localhost:8086/actuator/prometheus
```

## Next Steps

- Read [`REFACTORING_SUMMARY.md`](./REFACTORING_SUMMARY.md) for detailed changes
- Check [`docs/monitoring-observability-guide.md`](./docs/monitoring-observability-guide.md) for monitoring setup
- Review [`docs/asyncapi-events.yml`](./docs/asyncapi-events.yml) for event specifications
- See [`backend/authentication-service/src/test`] for test examples

## Support

For issues or questions:
1. Check logs: `docker-compose logs`
2. Review monitoring dashboards
3. Check Jaeger traces for request flow
4. Review Kibana logs for errors
