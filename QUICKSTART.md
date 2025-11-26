# MY Manus - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites
- Docker Desktop (running)
- Java 17+
- Node.js 22+
- PostgreSQL 15+ (or use Docker Compose)
- Anthropic API key

### Step 1: Clone & Configure

```bash
git clone <your-repo-url>
cd MY-Manus

# Backend environment
cd backend
cp .env.example .env
# Edit .env and add your ANTHROPIC_API_KEY

# Frontend environment (already configured)
cd ../frontend
# Default values in .env should work
```

### Step 2: Choose Sandbox Mode

**🐳 Docker Mode (Production-like, Recommended for first run)**
```bash
cd ..
./scripts/build-sandbox.sh
```

This builds the Docker sandbox with:
- Ubuntu 22.04
- Python 3.11 + data science libraries
- Node.js 22.13
- FFmpeg, Poppler, GraphViz

**⏱️ Takes ~10-15 minutes** (one-time build)

**💻 Host Mode (Development, Faster)**

For faster development without Docker overhead:
```bash
# Ensure Python 3.11+ is installed
python3 --version

# Set environment variable
export SANDBOX_MODE=host

# Or add to backend/.env:
# SANDBOX_MODE=host
```

**Benefits of Host Mode:**
- ⚡ Much faster execution (no container overhead)
- 🐛 Easier to debug
- 🔧 No Docker build required

**⚠️ Security Warning:** Host mode runs code directly on your machine. Only use in trusted development environments!

### Step 3: Start Services

#### Option A: Docker Compose (Recommended)

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Backend API (port 8080)
- Frontend UI (port 3000)

#### Option B: Run Individually

**Terminal 1 - PostgreSQL:**
```bash
docker run -d \
  --name mymanus-db \
  -e POSTGRES_DB=mymanus \
  -e POSTGRES_USER=mymanus \
  -e POSTGRES_PASSWORD=mymanus \
  -p 5432:5432 \
  postgres:15-alpine
```

**Terminal 2 - Backend:**
```bash
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

**Terminal 3 - Frontend:**
```bash
cd frontend
npm install
npm run dev
```

### Step 4: Access the Application

🌐 **Frontend**: http://localhost:3000
📡 **Backend API**: http://localhost:8080
📚 **Swagger UI**: http://localhost:8080/swagger-ui.html
🔍 **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### Step 5: Test the Agent

1. Open http://localhost:3000
2. Type a task: *"Create a simple data visualization showing random numbers"*
3. Watch the agent:
   - 💭 Think and plan
   - 📝 Write Python code (visible in Editor panel)
   - 💻 Execute code (output in Terminal panel)
   - ✅ Show results

## 🎯 Example Tasks to Try

### Data Analysis
```
Analyze this data and create a visualization:
Sales: [100, 150, 200, 180, 220]
Months: ['Jan', 'Feb', 'Mar', 'Apr', 'May']
```

### Web Search
```
Search for the latest news about AI and summarize the top 3 results
```

### File Operations
```
Create a CSV file with 10 rows of random user data (name, email, age)
```

### Math & Science
```
Calculate the first 20 Fibonacci numbers and plot them
```

## 🔍 Understanding the UI

### Three-Panel Layout

```
┌─────────────────────┬─────────────────────┐
│                     │   Terminal / Editor │
│    Chat Panel       │                     │
│                     │    (Tabbed View)    │
│  • Your messages    │                     │
│  • Agent thoughts   │  • Code execution   │
│  • Responses        │  • Generated code   │
│                     │  • Output/errors    │
└─────────────────────┴─────────────────────┘
```

### Status Indicators

- 🔵 **Thinking** - Agent is generating code
- 🟡 **Executing** - Running Python code in sandbox
- 🟢 **Done** - Task completed
- 🔴 **Error** - Something went wrong
- ⚪ **Idle** - Waiting for input

### Real-time Updates

All events are streamed via WebSocket:
- Agent thoughts appear in chat
- Generated code appears in Editor panel
- Execution output appears in Terminal panel

## 🛠️ Development Workflow

### Making Changes

**Backend Changes:**
```bash
cd backend
# Edit Java files
# Spring Boot DevTools auto-reloads
```

**Frontend Changes:**
```bash
cd frontend
# Edit TypeScript/React files
# Vite hot-reloads automatically
```

### Viewing Logs

**Docker Compose:**
```bash
docker-compose logs -f backend
docker-compose logs -f frontend
```

**Individual Services:**
```bash
# Backend logs in terminal
# Frontend logs in browser console
```

### Database Access

```bash
docker exec -it mymanus-postgres psql -U mymanus -d mymanus

# View tables
\dt

# View messages
SELECT * FROM messages ORDER BY timestamp DESC LIMIT 10;

# View agent states
SELECT session_id, created_at FROM agent_states;
```

## 🔧 Troubleshooting

### Backend Won't Start

**Problem:** Port 8080 already in use
```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9
```

**Problem:** Database connection failed
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Restart PostgreSQL
docker restart mymanus-postgres
```

### Frontend Can't Connect

**Problem:** CORS errors
- Check `SecurityConfig.java` has CORS enabled
- Verify `VITE_API_BASE_URL` in frontend/.env

**Problem:** WebSocket connection failed
- Check backend is running
- Verify WebSocket endpoint: `ws://localhost:8080/ws`

### Agent Not Responding

**Problem:** No Anthropic API key
```bash
# Check backend/.env has valid key
echo $ANTHROPIC_API_KEY
```

**Problem:** Sandbox execution failed
```bash
# Rebuild sandbox image
./scripts/build-sandbox.sh

# Check Docker is running
docker info
```

## 📊 Monitoring

### Check System Health

```bash
# Backend health
curl http://localhost:8080/api/health

# Create session
curl -X POST http://localhost:8080/api/agent/session

# Send message
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test","message":"Hello"}'
```

### View Swagger API

Open http://localhost:8080/swagger-ui.html to:
- Test all API endpoints
- View request/response formats
- Understand the API structure

## 🚀 Next Steps

1. **Explore Tools**: Check `backend/src/main/java/ai/mymanus/tool/impl/`
2. **Add Custom Tools**: Follow `docs/TOOLS_GUIDE.md`
3. **Customize Prompts**: Edit `PromptBuilder.java`
4. **Deploy to Production**: See `docs/DEPLOYMENT.md`

## 💡 Tips

- **Long Tasks**: Agent has max 20 iterations (configurable)
- **Code Persistence**: Variables persist between code blocks
- **Error Recovery**: Agent can see errors and retry
- **Tool Usage**: Agent automatically uses available tools
- **Session Management**: Sessions persist in database

## 📚 Comprehensive Documentation

### Getting Started
- **[Setup Guide](SETUP.md)** - Detailed installation and configuration
- **[Development Guide](docs/guides/DEVELOPMENT_GUIDE.md)** - Complete developer onboarding (prerequisites → first contribution)
- **[Project Overview](docs/PROJECT_OVERVIEW.md)** - Project need, inspiration, vision, and design philosophy

### Architecture & Design
- **[System Architecture](docs/architecture/ARCHITECTURE.md)** - Complete system architecture with 10+ diagrams
- **[Database Schema](docs/architecture/DATABASE_SCHEMA.md)** - Full ERD and schema documentation for all 9 tables
- **[Event Stream Guide](docs/architecture/EVENT_STREAM_GUIDE.md)** - Deep dive into event stream architecture (7 event types)
- **[Frontend Architecture](docs/architecture/FRONTEND_ARCHITECTURE.md)** - React components, state management, WebSocket integration

### Advanced Features
- **[Multi-Turn Conversations](docs/guides/MULTI_TURN_SCENARIOS.md)** - TASK/QUERY/ADJUSTMENT classification with real examples
- **[Observability](docs/guides/OBSERVABILITY.md)** - Prometheus/Grafana setup with metrics and dashboards
- **[API Reference](docs/guides/API_REFERENCE.md)** - Complete REST and WebSocket API documentation (40+ endpoints)

### Implementation Guides
- **[Agent Guide](docs/guides/AGENT_GUIDE.md)** - CodeAct implementation details
- **[UI Guide](docs/guides/UI_GUIDE.md)** - Frontend development patterns
- **[Sandbox Guide](docs/guides/SANDBOX_GUIDE.md)** - Docker environment setup
- **[Tools Guide](docs/guides/TOOLS_GUIDE.md)** - Adding new tools
- **[Deployment Guide](docs/guides/DEPLOYMENT.md)** - Production deployment

### Project Status
- **[Implementation Status](IMPLEMENTATION_STATUS.md)** - Current implementation status
- **[Final Summary](docs/project/FINAL_SUMMARY.md)** - Complete differential analysis

## 🆘 Get Help

- **Documentation**: See comprehensive docs above
- **Issues**: Create a GitHub issue
- **Quick Reference**: See [Development Guide - Quick Reference](docs/guides/DEVELOPMENT_GUIDE.md#quick-reference)

---

**🎉 You're all set!** Start chatting with your AI agent and watch it solve problems by writing code!
