# MY Manus - Development Setup Guide

## Prerequisites

- Docker Desktop installed and running
- Java 17+
- Node.js 22.13+
- Maven 3.9+
- At least 8GB RAM available
- Anthropic API key (for Claude integration)

## Quick Start

### 1. Clone and Setup

```bash
git clone <your-repo-url>
cd MY-Manus
```

### 2. Configure Environment Variables

**Backend:**
```bash
cd backend
cp .env.example .env
# Edit .env and add your ANTHROPIC_API_KEY
```

**Frontend:**
```bash
cd frontend
cp .env.example .env
# Default values should work for local development
```

### 3. Choose Sandbox Mode

The application supports two sandbox execution modes:

#### Docker Mode (Production, Default)

This is the secure, isolated environment where agent code executes:

```bash
./scripts/build-sandbox.sh
```

This will build the `mymanus-sandbox:latest` image with:
- Ubuntu 22.04
- Python 3.11
- Node.js 22.13
- Data science libraries
- Web scraping tools
- Document processing utilities

**When to use:**
- ✅ Production deployments
- ✅ Multi-user environments
- ✅ Maximum security required
- ✅ Untrusted code execution

#### Host Mode (Development, Faster)

Executes Python code directly on your machine without Docker overhead:

```bash
# Ensure Python 3.11+ is installed
python3 --version

# Configure in backend/.env or application-dev.yml:
SANDBOX_MODE=host
```

**When to use:**
- ✅ Local development
- ✅ Faster iteration
- ✅ Debugging Python code
- ✅ Trusted development environment

**⚠️ Security Warning:** Host mode runs generated code directly on your machine. Only use in trusted development environments!

**Host Mode Configuration:**
```yaml
# application-dev.yml (already configured for dev profile)
sandbox:
  mode: host  # Switch between 'docker' or 'host'
  host:
    python-executable: python3
    workspace-dir: /tmp/manus-workspace
```

### 4. Start the Services

Using Docker Compose (recommended for full stack):

```bash
docker-compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- Backend Spring Boot API (port 8080)
- Frontend React app (port 3000)

Or run services individually:

**PostgreSQL:**
```bash
docker run -d \
  --name mymanus-db \
  -e POSTGRES_DB=mymanus \
  -e POSTGRES_USER=mymanus \
  -e POSTGRES_PASSWORD=mymanus \
  -p 5432:5432 \
  postgres:15-alpine
```

**Backend:**
```bash
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

### 5. Generate Frontend API Client

Once the backend is running, generate the TypeScript API client from OpenAPI spec:

```bash
./scripts/generate-frontend-client.sh
```

Or manually:
```bash
cd frontend
npm run generate-api
```

This creates TypeScript client code in `frontend/src/api/generated/` based on the Swagger documentation.

## Accessing the Application

- **Frontend UI**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## Development Workflow

### Backend Development

1. Make changes to Java code in `backend/src/`
2. Spring Boot DevTools will auto-reload
3. Check Swagger UI to see API documentation updates
4. Regenerate frontend client if API changes: `npm run generate-api`

### Frontend Development

1. Make changes to React components in `frontend/src/`
2. Vite will hot-reload automatically
3. Use generated API client from `src/api/generated/`

### Database Schema

The application automatically creates tables on startup using JPA/Hibernate:

- `agent_states` - Agent session state
- `messages` - Conversation history
- `tool_executions` - Tool execution logs

View schema:
```bash
docker exec -it mymanus-postgres psql -U mymanus -d mymanus -c "\dt"
```

## Testing

### Backend Tests

```bash
cd backend
./mvnw test
```

### Frontend Tests

```bash
cd frontend
npm test
```

### Integration Tests

```bash
./scripts/integration-test.sh
```

## Troubleshooting

### Backend won't start

- Check PostgreSQL is running: `docker ps | grep postgres`
- Verify database connection in backend logs
- Ensure port 8080 is not in use

### Frontend can't connect to backend

- Verify backend is running: `curl http://localhost:8080/api/health`
- Check CORS settings in `SecurityConfig.java`
- Verify `VITE_API_BASE_URL` in frontend `.env`

### Sandbox execution fails

- Check Docker socket is accessible
- Verify sandbox image exists: `docker images | grep mymanus-sandbox`
- Rebuild sandbox: `./scripts/build-sandbox.sh`

### API client generation fails

- Ensure backend is running and healthy
- Check OpenAPI spec is accessible: `curl http://localhost:8080/v3/api-docs`
- Clear node_modules and reinstall: `cd frontend && rm -rf node_modules && npm install`

## Project Structure

```
MY-Manus/
├── backend/               # Spring Boot application
│   ├── src/
│   │   └── main/
│   │       ├── java/ai/mymanus/
│   │       │   ├── config/      # Configuration classes
│   │       │   ├── controller/  # REST controllers
│   │       │   ├── dto/         # Data transfer objects
│   │       │   ├── model/       # JPA entities
│   │       │   ├── repository/  # Database repositories
│   │       │   └── service/     # Business logic
│   │       └── resources/
│   │           └── application.yml
│   └── pom.xml
├── frontend/              # React application
│   ├── src/
│   │   ├── api/          # Generated API client
│   │   ├── components/   # React components
│   │   ├── stores/       # Zustand state management
│   │   ├── hooks/        # Custom React hooks
│   │   └── types/        # TypeScript types
│   └── package.json
├── sandbox/              # Docker execution environment
│   └── Dockerfile
├── scripts/              # Utility scripts
│   ├── build-sandbox.sh
│   └── generate-frontend-client.sh
├── docs/                 # Documentation
└── docker-compose.yml    # Multi-container setup
```

## Next Steps

1. **Implement Core Services**: CodeActAgentService, PythonSandboxExecutor
2. **Build UI Components**: Chat panel, Terminal view, Editor view
3. **Add Tools**: Web search, file operations, data analysis
4. **Test Agent Loop**: End-to-end agent execution
5. **Deploy**: Production deployment with authentication

## 📚 Comprehensive Documentation

### Getting Started
- **[Quick Start](QUICKSTART.md)** - Get started in 5 minutes
- **[Development Guide](docs/guides/DEVELOPMENT_GUIDE.md)** - Complete developer onboarding (prerequisites → first contribution)
- **[Project Overview](docs/PROJECT_OVERVIEW.md)** - Project need, inspiration, vision, and design philosophy

### Architecture & Design
- **[System Architecture](docs/architecture/ARCHITECTURE.md)** - Complete system architecture with 10+ diagrams
  - Component architecture, data flow, deployment models
  - Security architecture (5 layers)
  - Performance optimizations
  - MY-Manus vs Manus AI comparison
- **[Database Schema](docs/architecture/DATABASE_SCHEMA.md)** - Full ERD and schema documentation for all 12 tables
  - Complete ERD diagrams
  - JSONB usage patterns
  - Indexes and query examples
- **[Event Stream Guide](docs/architecture/EVENT_STREAM_GUIDE.md)** - Deep dive into event stream architecture
  - All 7 event types documented with examples
  - Implementation patterns and best practices
  - Debugging with events
- **[Frontend Architecture](docs/architecture/FRONTEND_ARCHITECTURE.md)** - React components, state management, WebSocket integration
  - Three-panel UI architecture
  - Zustand store patterns
  - Real-time event streaming

### Advanced Features
- **[Multi-Turn Conversations](docs/guides/MULTI_TURN_SCENARIOS.md)** - TASK/QUERY/ADJUSTMENT classification
  - Real-world examples with flow diagrams
  - LLM-based classification logic
  - Edge cases and best practices
- **[Observability](docs/guides/OBSERVABILITY.md)** - Prometheus/Grafana setup
  - Complete metrics documentation (JVM, HTTP, DB, Spring AI, custom)
  - PromQL query examples
  - Example Grafana dashboard configurations
  - Alerting rules and troubleshooting
- **[API Reference](docs/guides/API_REFERENCE.md)** - Complete REST and WebSocket API documentation
  - All 40+ REST endpoints documented
  - All 6 WebSocket topics
  - Request/response examples
  - Error handling and rate limiting

### Implementation Guides
- **[Agent Guide](docs/guides/AGENT_GUIDE.md)** - CodeAct implementation details
- **[UI Guide](docs/guides/UI_GUIDE.md)** - Frontend development patterns
- **[Sandbox Guide](docs/guides/SANDBOX_GUIDE.md)** - Docker environment setup
- **[Tools Guide](docs/guides/TOOLS_GUIDE.md)** - Adding new tools
- **[Deployment Guide](docs/guides/DEPLOYMENT.md)** - Production deployment

### Project Status
- **[Implementation Status](IMPLEMENTATION_STATUS.md)** - Current implementation status
- **[Final Summary](docs/project/FINAL_SUMMARY.md)** - Complete differential analysis

### For Claude Code
- **[CLAUDE.md](CLAUDE.md)** - Instructions for Claude Code development

## 🆘 Support

- **Documentation**: See comprehensive docs above
- **GitHub Issues**: [Report bugs](https://github.com/yourusername/my-manus/issues)
- **Quick Reference**: See [Development Guide - Quick Reference](docs/guides/DEVELOPMENT_GUIDE.md#quick-reference)
