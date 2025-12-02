# MY-Manus Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.8+
- Anthropic API key

### 1. Clone Repository
```bash
git clone https://github.com/maheshyaddanapudi/MY-Manus.git
cd MY-Manus
```

### 2. Start Backend
```bash
cd backend

# Set API key
export ANTHROPIC_API_KEY=your-api-key-here

# Build and run
mvn clean install
java -jar target/manus-backend-1.0.0.jar --spring.profiles.active=dev
```

Backend runs on: **http://localhost:8080**

### 3. Start Frontend
```bash
cd frontend

# Install and run
npm install
npm run dev
```

Frontend runs on: **http://localhost:5173**

### 4. Test It Works

**Check Backend Health:**
```bash
curl http://localhost:8080/actuator/health
```

**Create Session:**
```bash
curl -X POST "http://localhost:8080/api/agent/session?userId=test"
```

**Open Browser:**
- Navigate to http://localhost:5173
- Status should show "Idle" (green) = Connected ✅
- Type a message: "Calculate 2 + 2"
- Watch the agent think and execute code!

---

## 🧪 Run Tests

### Backend Tests (299 tests)
```bash
cd backend
mvn test
```

Expected: `Tests run: 299, Failures: 0, Errors: 0, Skipped: 0`

### Frontend Tests (154 tests)
```bash
cd frontend
npm test
```

Expected: `Test Files 21 passed (21), Tests 154 passed (154)`

### All Tests
```bash
# From root directory
cd backend && mvn test && cd ../frontend && npm test
```

Expected: **453/453 tests passing (100%)**

---

## 🐳 Docker Deployment

### Quick Start with Docker Compose
```bash
# Set API key
export ANTHROPIC_API_KEY=your-api-key-here

# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f

# Stop services
docker-compose down
```

Access application at: **http://localhost**

---

## 📊 Application Status

### Current Status: ✅ FULLY WORKING

- ✅ **Backend:** 299/299 tests passing
- ✅ **Frontend:** 154/154 tests passing
- ✅ **WebSocket:** Connected
- ✅ **Database:** H2 (dev) / PostgreSQL (prod)
- ✅ **Session Management:** Working
- ✅ **Agent Loop:** Functional

### Key Features
- 🤖 AI-powered CodeAct agent (Claude)
- 🐍 Python code execution
- 🌐 Browser automation
- 💬 Real-time chat interface
- 📝 Code editor with syntax highlighting
- 📊 Event stream visualization
- 🔧 20+ built-in tools

---

## 🔧 Troubleshooting

### WebSocket Not Connecting
```bash
# Check backend is running
curl http://localhost:8080/actuator/health

# Check session creation
curl -X POST http://localhost:8080/api/agent/session

# Restart backend
pkill -f manus-backend
java -jar target/manus-backend-1.0.0.jar --spring.profiles.active=dev
```

### Tests Failing
```bash
# Clean rebuild
cd backend
mvn clean install
mvn test

cd ../frontend
rm -rf node_modules
npm install
npm test
```

### Database Issues
```bash
# Reset H2 database
rm -rf backend/data/

# Restart backend
java -jar target/manus-backend-1.0.0.jar --spring.profiles.active=dev
```

---

## 📚 Documentation

- **Full Report:** [TASK_COMPLETION_REPORT.md](TASK_COMPLETION_REPORT.md)
- **Architecture:** See report section "Application Architecture"
- **API Docs:** http://localhost:8080/swagger-ui.html (if enabled)
- **GitHub:** https://github.com/maheshyaddanapudi/MY-Manus

---

## 🎯 Next Steps

1. **Add Your API Key**
   ```bash
   export ANTHROPIC_API_KEY=your-key-here
   ```

2. **Try Sample Tasks**
   - "Calculate the factorial of 10"
   - "Create a bar chart of sales data"
   - "Search for Python tutorials"
   - "Analyze this CSV file"

3. **Explore Tools**
   - Check Terminal tab for code execution
   - View Event Stream for agent reasoning
   - Use Code Editor for complex scripts
   - Monitor Browser tab for web tasks

4. **Deploy to Production**
   - Use PostgreSQL instead of H2
   - Set up Docker containers
   - Configure environment variables
   - Enable monitoring and logging

---

## 💡 Tips

- **First Run:** Application creates H2 database in `backend/data/`
- **Session IDs:** Each chat creates a new session (persisted)
- **WebSocket:** Auto-reconnects if connection drops
- **Logs:** Check `backend/backend.log` for debugging
- **API Key:** Required for AI functionality (tests use mocks)

---

## 🆘 Support

**Issues?** Check:
1. Backend logs: `tail -f backend/backend.log`
2. Frontend console: Browser DevTools (F12)
3. Health endpoint: `curl http://localhost:8080/actuator/health`
4. Test results: `mvn test` and `npm test`

**Still stuck?** See [TASK_COMPLETION_REPORT.md](TASK_COMPLETION_REPORT.md) for detailed troubleshooting.

---

**Status:** ✅ All systems operational  
**Tests:** 453/453 passing (100%)  
**Ready:** Production deployment
