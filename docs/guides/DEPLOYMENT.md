# Deployment Guide

## Quick Start (Docker Compose)

### Prerequisites

- Docker & Docker Compose
- Anthropic API Key

### Local Development

```bash
# Clone repository
git clone <repository-url>
cd MY-Manus

# Set API key
export ANTHROPIC_API_KEY=your-key-here

# Build sandbox image
cd sandbox
docker build -t mymanus-sandbox:latest .
cd ..

# Start all services
docker-compose up

# Access application
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# PostgreSQL: localhost:5432
```

## Docker Compose Configuration

**Location**: `/docker-compose.yml`

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: mymanus-postgres
    environment:
      POSTGRES_DB: mymanus
      POSTGRES_USER: mymanus
      POSTGRES_PASSWORD: mymanus
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U mymanus"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Backend Spring Boot Application
  backend:
    build: ./backend
    container_name: mymanus-backend
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: mymanus
      DB_USER: mymanus
      DB_PASSWORD: mymanus
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      AUTH_ENABLED: false
      DOCKER_HOST: unix:///var/run/docker.sock
    ports:
      - "8080:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./backend:/app
    depends_on:
      postgres:
        condition: service_healthy
    command: ./mvnw spring-boot:run

  # Frontend React Application
  frontend:
    build: ./frontend
    container_name: mymanus-frontend
    environment:
      VITE_API_BASE_URL: http://localhost:8080
      VITE_WS_URL: ws://localhost:8080/ws
    ports:
      - "3000:3000"
    volumes:
      - ./frontend:/app
      - /app/node_modules
    depends_on:
      - backend
    command: npm run dev -- --host

volumes:
  postgres_data:

networks:
  mymanus-network:
    driver: bridge
```

**Key Configuration**:
- PostgreSQL 15 with health checks
- Backend with Docker socket mounted for sandbox
- Frontend with hot reload
- Shared network for service communication

## Backend Configuration

**Location**: `/backend/src/main/resources/application.properties`

```properties
# Application
spring.application.name=my-manus
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mymanus
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Spring AI (Anthropic Claude)
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-3-5-sonnet-20241022
spring.ai.anthropic.chat.options.temperature=0.7
spring.ai.anthropic.chat.options.max-tokens=4096

# Agent Configuration
agent.max-iterations=20
agent.execution-timeout=30000

# Sandbox
sandbox.mode=docker
sandbox.workspace-path=/workspace
sandbox.network-enabled=true

# RAG
rag.top-k=5

# WebSocket
spring.websocket.allowed-origins=*

# Actuator/Metrics (Prometheus)
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true
management.metrics.tags.application=${spring.application.name}

# Logging
logging.level.root=INFO
logging.level.ai.mymanus=DEBUG
logging.level.org.springframework.ai=DEBUG
```

## Environment Variables

### Required

- `ANTHROPIC_API_KEY`: Anthropic API key for Claude

### Optional

- `DB_HOST`: PostgreSQL host (default: localhost)
- `DB_PORT`: PostgreSQL port (default: 5432)
- `DB_NAME`: Database name (default: mymanus)
- `DB_USER`: Database user (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
- `AUTH_ENABLED`: Enable authentication (default: false)

## Building for Production

### Backend JAR

```bash
cd backend
./mvnw clean package -DskipTests
# Output: backend/target/my-manus-0.0.1-SNAPSHOT.jar
```

### Frontend Build

```bash
cd frontend
npm install
npm run build
# Output: frontend/dist/
```

### Docker Images

```bash
# Backend
cd backend
docker build -t mymanus-backend:latest .

# Frontend
cd frontend
docker build -t mymanus-frontend:latest .

# Sandbox
cd sandbox
docker build -t mymanus-sandbox:latest .
```

## Production Deployment

### Using Docker Compose (Simple)

```bash
# Create production compose file
cat > docker-compose.prod.yml <<EOF
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: \${DB_NAME:-mymanus}
      POSTGRES_USER: \${DB_USER:-mymanus}
      POSTGRES_PASSWORD: \${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  backend:
    image: mymanus-backend:latest
    environment:
      DB_HOST: postgres
      ANTHROPIC_API_KEY: \${ANTHROPIC_API_KEY}
      AUTH_ENABLED: true
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    restart: unless-stopped

  frontend:
    image: mymanus-frontend:latest
    environment:
      VITE_API_BASE_URL: https://api.yourdomain.com
      VITE_WS_URL: wss://api.yourdomain.com/ws
    ports:
      - "3000:80"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres_data:
EOF

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

### Using Kubernetes (Advanced)

See `/k8s/` directory for Kubernetes manifests (if implemented).

Example deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mymanus-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mymanus-backend
  template:
    metadata:
      labels:
        app: mymanus-backend
    spec:
      containers:
      - name: backend
        image: mymanus-backend:latest
        env:
        - name: DB_HOST
          value: postgres-service
        - name: ANTHROPIC_API_KEY
          valueFrom:
            secretKeyRef:
              name: mymanus-secrets
              key: anthropic-api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
```

## Database Management

### Migrations

Schema updates are managed by Hibernate DDL auto-update:

```properties
spring.jpa.hibernate.ddl-auto=update
```

For production, consider using Flyway:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Backups

```bash
# Backup database
docker exec mymanus-postgres pg_dump -U mymanus mymanus > backup.sql

# Restore database
docker exec -i mymanus-postgres psql -U mymanus mymanus < backup.sql
```

## Monitoring

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Response:
# {"status":"UP","components":{"db":{"status":"UP"},...}}
```

### Metrics (Prometheus)

```bash
# Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Scrape configuration for Prometheus:
scrape_configs:
  - job_name: 'mymanus'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
```

### Logging

Logs are output to console in JSON format. Configure log aggregation:

```properties
# application.properties
logging.pattern.console=%d{ISO8601} %-5level [%thread] %logger{36} - %msg%n
```

For production, use centralized logging (ELK, Loki, etc.).

## Security

### HTTPS/TLS

Use a reverse proxy (Nginx, Traefik) for TLS termination:

```nginx
server {
    listen 443 ssl;
    server_name yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### Authentication

Enable authentication in production:

```properties
auth.enabled=true
```

Configure JWT settings and user management.

### Secrets Management

Never commit secrets. Use environment variables or secret management tools:

```bash
# .env file (add to .gitignore)
ANTHROPIC_API_KEY=sk-ant-...
DB_PASSWORD=strong-password
JWT_SECRET=random-secret
```

## Scaling

### Horizontal Scaling

- **Backend**: Scale pods/containers (stateless)
- **Frontend**: CDN + multiple instances
- **Database**: Read replicas, connection pooling
- **Sandbox**: Docker-in-Docker with resource limits

### Resource Planning

**Minimum (Single Instance)**:
- Backend: 512MB RAM, 1 CPU
- Frontend: 256MB RAM, 0.5 CPU
- PostgreSQL: 1GB RAM, 1 CPU
- Sandbox containers: 512MB RAM each

**Production (3 replicas)**:
- Backend: 2GB RAM, 2 CPU per instance
- PostgreSQL: 4GB RAM, 2 CPU
- Total: ~10GB RAM, 8+ CPU

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose logs backend
docker-compose logs postgres

# Common issues:
# 1. API key not set
# 2. Database not ready
# 3. Port already in use
```

### Database Connection Failed

```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check connectivity
docker exec mymanus-backend psql -h postgres -U mymanus -d mymanus

# Solution: Ensure postgres service is healthy before backend starts
```

### Sandbox Execution Fails

```bash
# Check Docker socket access
docker exec mymanus-backend docker ps

# Verify sandbox image exists
docker images | grep mymanus-sandbox

# Solution: Mount Docker socket correctly
```

## Maintenance

### Updates

```bash
# Pull latest code
git pull

# Rebuild images
docker-compose build

# Restart with zero downtime (if multiple instances)
docker-compose up -d --no-deps --build backend

# Or restart all
docker-compose down
docker-compose up -d
```

### Cleanup

```bash
# Remove stopped containers
docker-compose down

# Remove volumes (WARNING: deletes data)
docker-compose down -v

# Clean up old sandbox containers
docker container prune -f

# Clean up old images
docker image prune -a -f
```

## Best Practices

1. **Always use secrets management** - never hardcode API keys
2. **Enable health checks** - for container orchestration
3. **Use connection pooling** - for database efficiency
4. **Monitor resource usage** - prevent container exhaustion
5. **Set up alerts** - for production issues
6. **Regular backups** - of PostgreSQL database
7. **Log aggregation** - for debugging
8. **HTTPS in production** - always use TLS
9. **Resource limits** - prevent runaway processes
10. **Version control** - tag Docker images properly

## Support

For issues or questions:
- Check logs: `docker-compose logs -f`
- Review health endpoint: `/actuator/health`
- Monitor metrics: `/actuator/prometheus`
- Check documentation in `/docs`
