# Development Guide

Complete guide for developers to set up, understand, and contribute to MY-Manus.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Code Style & Conventions](#code-style--conventions)
- [Adding Features](#adding-features)
- [Debugging](#debugging)
- [Contributing](#contributing)
- [First Contribution Ideas](#first-contribution-ideas)

---

## Prerequisites

### Required Tools

**Backend Development:**
- **Java 21** - OpenJDK 21 or later
- **Maven 3.9+** - Build tool (or use wrapper: `./mvnw`)
- **Docker 24+** - For sandbox containers
- **PostgreSQL 15+** - Primary database

**Frontend Development:**
- **Node.js 22.13** - JavaScript runtime
- **pnpm 9.15+** - Package manager (faster than npm)

**General:**
- **Git** - Version control
- **IDE** - IntelliJ IDEA (recommended) or VS Code
- **Postman/Thunder Client** - API testing (optional)

### Installation

#### macOS
```bash
# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install prerequisites
brew install openjdk@21 maven node@22 docker postgresql@15
brew install pnpm

# Link Java
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

#### Ubuntu/Debian
```bash
# Java 21
sudo apt update
sudo apt install openjdk-21-jdk maven

# Node.js 22
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs

# pnpm
npm install -g pnpm

# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER  # Run Docker without sudo

# PostgreSQL
sudo apt install postgresql-15 postgresql-contrib
```

#### Windows
```powershell
# Install via Chocolatey
choco install openjdk21 maven nodejs-lts docker-desktop postgresql15
npm install -g pnpm
```

### Verify Installation

```bash
java -version      # Should show 21.x.x
mvn -version       # Should show 3.9+
node -version      # Should show v22.13+
pnpm -version      # Should show 9.15+
docker -version    # Should show 24.x.x
psql --version     # Should show 15.x
```

---

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/maheshyaddanapudi/MY-Manus.git
cd MY-Manus
```

### 2. Database Setup

**Option A: Local PostgreSQL**
```bash
# Create database and user
psql postgres -c "CREATE DATABASE mymanus;"
psql postgres -c "CREATE USER manus WITH PASSWORD 'manus123';"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE mymanus TO manus;"
```

**Option B: Docker PostgreSQL**
```bash
docker run -d \
  --name mymanus-postgres \
  -e POSTGRES_DB=mymanus \
  -e POSTGRES_USER=manus \
  -e POSTGRES_PASSWORD=manus123 \
  -p 5432:5432 \
  postgres:15-alpine
```

### 3. Backend Setup

```bash
cd backend

# Copy environment template
cp src/main/resources/application.properties.template \
   src/main/resources/application.properties

# Edit application.properties (optional - defaults work for local dev)
# Set your Anthropic API key
echo "anthropic.api-key=sk-ant-xxx" >> src/main/resources/application.properties

# Build and run
./mvnw clean install
./mvnw spring-boot:run
```

Backend will start on http://localhost:8080

**Verify backend:**
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

### 4. Frontend Setup

```bash
cd frontend

# Install dependencies
pnpm install

# Start development server
pnpm dev
```

Frontend will start on http://localhost:5173

### 5. Test the Application

1. Open browser: http://localhost:5173
2. Type a message: "Write Python code to calculate fibonacci(10)"
3. Watch the agent:
   - Generate Python code
   - Execute in sandbox
   - Return result

---

## Project Structure

```
MY-Manus/
├── backend/                      # Spring Boot backend
│   ├── src/main/java/ai/mymanus/
│   │   ├── config/              # Configuration classes
│   │   │   ├── AnthropicConfig.java
│   │   │   ├── WebSocketConfig.java
│   │   │   └── ToolRegistryConfig.java
│   │   ├── controller/          # REST endpoints
│   │   │   ├── AgentController.java      # /api/agent/*
│   │   │   ├── SessionController.java    # /api/sessions/*
│   │   │   └── NotificationController.java
│   │   ├── model/               # JPA entities
│   │   │   ├── AgentState.java          # Session state
│   │   │   ├── Event.java               # Event stream
│   │   │   ├── Message.java             # Chat messages
│   │   │   ├── Document.java            # RAG documents
│   │   │   └── Notification.java        # Notifications
│   │   ├── repository/          # Spring Data JPA
│   │   │   ├── AgentStateRepository.java
│   │   │   ├── EventRepository.java
│   │   │   └── MessageRepository.java
│   │   ├── service/             # Business logic
│   │   │   ├── CodeActAgentService.java  # Agent loop
│   │   │   ├── AnthropicService.java     # Claude API
│   │   │   ├── SandboxService.java       # Docker execution
│   │   │   ├── TodoMdWatcher.java        # Plan tracking
│   │   │   ├── MessageClassifier.java    # Multi-turn
│   │   │   └── NotificationService.java  # Notifications
│   │   ├── tool/                # Tool implementations
│   │   │   ├── ToolRegistry.java        # Tool manager
│   │   │   ├── PythonExecutor.java      # Code execution
│   │   │   ├── WebBrowser.java          # Browser automation
│   │   │   └── [24 total tools]
│   │   └── dto/                 # Request/Response objects
│   └── src/main/resources/
│       ├── application.properties       # Configuration
│       └── db/migration/                # Flyway migrations
├── frontend/                    # React frontend
│   ├── src/
│   │   ├── components/          # React components
│   │   │   ├── Chat/           # Chat interface
│   │   │   ├── Panels/         # Browser/Terminal/Editor
│   │   │   ├── Plan/           # Plan visualization
│   │   │   └── Notifications/  # Toast notifications
│   │   ├── stores/             # Zustand state
│   │   │   ├── chatStore.ts    # Chat state
│   │   │   ├── panelStore.ts   # Panel state
│   │   │   └── planStore.ts    # Plan state
│   │   ├── services/           # API clients
│   │   │   ├── api.ts          # REST API
│   │   │   └── websocket.ts    # WebSocket
│   │   └── types/              # TypeScript types
│   └── package.json
├── sandbox/                     # Docker sandbox
│   ├── Dockerfile              # Ubuntu 22.04 base
│   └── requirements.txt        # Python packages
└── docs/                        # Documentation
    ├── PROJECT_OVERVIEW.md
    ├── architecture/
    │   ├── ARCHITECTURE.md
    │   ├── DATABASE_SCHEMA.md
    │   ├── EVENT_STREAM_GUIDE.md
    │   └── FRONTEND_ARCHITECTURE.md
    └── guides/
        ├── API_REFERENCE.md
        ├── DEVELOPMENT_GUIDE.md (this file)
        ├── OBSERVABILITY.md
        └── MULTI_TURN_SCENARIOS.md
```

### Key Directories Explained

**Backend:**
- **service/** - Core business logic, agent loop lives here
- **tool/** - All 22 tools (file operations, browser automation, etc.)
- **model/** - Database entities with JPA annotations
- **controller/** - REST API endpoints exposed to frontend

**Frontend:**
- **components/** - Reusable UI components (Chat, Panels, Plan)
- **stores/** - Zustand state management (global state)
- **services/** - API clients (REST + WebSocket)

**Sandbox:**
- **Dockerfile** - Ubuntu 22.04 with Python 3.11, Node.js 22, FFmpeg

---

## Development Workflow

### Daily Development Flow

```bash
# 1. Pull latest changes
git pull origin main

# 2. Create feature branch
git checkout -b feature/your-feature-name

# 3. Start backend (Terminal 1)
cd backend
./mvnw spring-boot:run

# 4. Start frontend (Terminal 2)
cd frontend
pnpm dev

# 5. Make changes...
# - Edit code
# - Test in browser
# - Check logs

# 6. Run tests
cd backend && ./mvnw test
cd frontend && pnpm test

# 7. Commit changes
git add .
git commit -m "feat: Add your feature description"

# 8. Push and create PR
git push origin feature/your-feature-name
# Then create Pull Request on GitHub
```

### Hot Reload

**Backend (Spring Boot DevTools):**
- Changes to Java files auto-reload (if DevTools enabled)
- Changes to `application.properties` require restart

**Frontend (Vite HMR):**
- Changes to `.tsx` files hot-reload instantly
- Changes to `index.html` require refresh

### Environment Variables

**Backend** (`backend/src/main/resources/application.properties`):
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mymanus
spring.datasource.username=manus
spring.datasource.password=manus123

# Anthropic API
anthropic.api-key=sk-ant-xxx
anthropic.model=claude-3-5-sonnet-20241022

# Docker Sandbox
sandbox.image=mymanus-sandbox:latest
sandbox.memory-limit=512m
sandbox.cpu-quota=50000

# Development Mode (disable auth)
auth.enabled=false
```

**Frontend** (`frontend/.env.local`):
```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

---

## Testing

### Backend Tests

**Run all tests:**
```bash
cd backend
./mvnw test
```

**Run specific test class:**
```bash
./mvnw test -Dtest=CodeActAgentServiceTest
```

**Run with coverage:**
```bash
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html
```

**Test Structure:**
```
backend/src/test/java/ai/mymanus/
├── service/
│   ├── CodeActAgentServiceTest.java
│   ├── SandboxServiceTest.java
│   └── MessageClassifierTest.java
├── tool/
│   ├── PythonExecutorTest.java
│   └── WebBrowserTest.java
└── integration/
    └── AgentIntegrationTest.java
```

**Example Unit Test:**
```java
@SpringBootTest
class CodeActAgentServiceTest {

    @Autowired
    private CodeActAgentService agentService;

    @Test
    void testProcessQuery_SimpleCalculation() {
        String sessionId = UUID.randomUUID().toString();
        String query = "Calculate 2 + 2";

        String response = agentService.processQuery(sessionId, query);

        assertThat(response).contains("4");
    }

    @Test
    void testExtractCodeBlocks() {
        String llmResponse = """
            I'll calculate that for you.
            ```python
            result = 2 + 2
            print(result)
            ```
            The answer is 4.
            """;

        List<String> blocks = agentService.extractCodeBlocks(llmResponse);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0)).contains("result = 2 + 2");
    }
}
```

### Frontend Tests

**Run all tests:**
```bash
cd frontend
pnpm test
```

**Run with UI:**
```bash
pnpm test:ui
```

**Run with coverage:**
```bash
pnpm test:coverage
```

**Test Structure:**
```
frontend/src/
├── components/
│   ├── Chat/__tests__/
│   │   └── ChatInput.test.tsx
│   ├── Panels/__tests__/
│   │   └── BrowserPanel.test.tsx
│   └── Plan/__tests__/
│       └── PlanVisualization.test.tsx
└── stores/__tests__/
    └── chatStore.test.ts
```

**Example Component Test:**
```tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { ChatInput } from '../ChatInput';

describe('ChatInput', () => {
  it('should send message on submit', () => {
    const onSend = vi.fn();
    render(<ChatInput onSend={onSend} />);

    const input = screen.getByPlaceholderText('Type a message...');
    fireEvent.change(input, { target: { value: 'Hello' } });

    const button = screen.getByRole('button', { name: /send/i });
    fireEvent.click(button);

    expect(onSend).toHaveBeenCalledWith('Hello');
  });
});
```

### Integration Testing

**Test full agent loop:**
```bash
cd backend
./mvnw test -Dtest=AgentIntegrationTest
```

**Manual integration test:**
```bash
# 1. Start backend
cd backend && ./mvnw spring-boot:run

# 2. Send test request
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-123",
    "message": "Write Python code to calculate fibonacci(10)"
  }'

# 3. Check event stream
curl http://localhost:8080/api/agent/test-123/events
```

---

## Code Style & Conventions

### Java (Backend)

**Naming:**
- **Classes**: PascalCase (`CodeActAgentService`)
- **Methods**: camelCase (`processQuery()`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_ITERATIONS`)
- **Packages**: lowercase (`ai.mymanus.service`)

**Code Style:**
```java
// ✅ Good
@Service
@Slf4j
public class CodeActAgentService {

    private final AnthropicService anthropicService;
    private final SandboxService sandboxService;

    public String processQuery(String sessionId, String userMessage) {
        log.info("Processing query for session: {}", sessionId);

        // Build context
        String context = buildContext(sessionId);

        // Call LLM
        String response = anthropicService.sendMessage(context);

        return response;
    }
}

// ❌ Bad
public class codeActAgentService {  // Wrong case
    public String ProcessQuery(String session_id, String user_message) {  // Wrong case
        System.out.println("Processing...");  // Use log.info instead
        String resp = anthropicService.sendMessage(null);  // No context
        return resp;
    }
}
```

**Annotations:**
- Use Lombok: `@Data`, `@Builder`, `@Slf4j`
- Use Spring: `@Service`, `@RestController`, `@Autowired`
- Document APIs: `@Operation`, `@ApiResponse`

**Error Handling:**
```java
// ✅ Good
try {
    String result = sandboxService.execute(code);
    return result;
} catch (SandboxException e) {
    log.error("Sandbox execution failed: {}", e.getMessage(), e);
    throw new AgentException("Code execution failed", e);
}

// ❌ Bad
try {
    String result = sandboxService.execute(code);
    return result;
} catch (Exception e) {
    e.printStackTrace();  // Don't use printStackTrace
    return null;  // Don't swallow exceptions
}
```

### TypeScript (Frontend)

**Naming:**
- **Components**: PascalCase (`ChatInput.tsx`)
- **Functions**: camelCase (`sendMessage()`)
- **Types**: PascalCase (`ChatMessage`)
- **Constants**: UPPER_SNAKE_CASE (`API_BASE_URL`)

**Code Style:**
```tsx
// ✅ Good
interface ChatInputProps {
  onSend: (message: string) => void;
  disabled?: boolean;
}

export const ChatInput: React.FC<ChatInputProps> = ({ onSend, disabled = false }) => {
  const [message, setMessage] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (message.trim()) {
      onSend(message);
      setMessage('');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        disabled={disabled}
      />
      <button type="submit">Send</button>
    </form>
  );
};

// ❌ Bad
export default function chatInput(props: any) {  // Use named export, typed props
  const [msg, setMsg] = useState('');  // Use descriptive names

  return (
    <div onClick={() => props.onSend(msg)}>  // Use form submit
      <input onChange={(e) => setMsg(e.target.value)} />
    </div>
  );
}
```

**Zustand Store Pattern:**
```typescript
// ✅ Good
interface ChatStore {
  messages: ChatMessage[];
  addMessage: (message: ChatMessage) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatStore>((set) => ({
  messages: [],
  addMessage: (message) => set((state) => ({
    messages: [...state.messages, message]
  })),
  clearMessages: () => set({ messages: [] }),
}));

// ❌ Bad
export const useChatStore = create((set: any) => ({
  msgs: [],  // Use full name 'messages'
  add: (msg: any) => set({ msgs: [...useChatStore.getState().msgs, msg] }),  // Don't access state directly
}));
```

### Git Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Format: <type>(<scope>): <description>

# Types:
feat:     New feature
fix:      Bug fix
docs:     Documentation only
style:    Code style (formatting, semicolons, etc.)
refactor: Code refactor (no functional change)
perf:     Performance improvement
test:     Add/update tests
chore:    Build, dependencies, tooling

# Examples:
git commit -m "feat(agent): Add multi-turn conversation support"
git commit -m "fix(sandbox): Handle container timeout errors"
git commit -m "docs(api): Add WebSocket endpoint documentation"
git commit -m "refactor(tools): Extract common tool validation logic"
```

---

## Adding Features

### Adding a New Tool

**Example: Add a `FileReader` tool**

**1. Create Tool Class** (`backend/src/main/java/ai/mymanus/tool/FileReader.java`):

```java
package ai.mymanus.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileReader implements Tool {

    @Override
    public String getName() {
        return "read_file";
    }

    @Override
    public String getDescription() {
        return """
            Read contents of a file from the sandbox filesystem.
            Usage: read_file(filepath)
            Example: read_file('/workspace/data.txt')
            """;
    }

    @Override
    public String execute(String... args) {
        if (args.length == 0) {
            return "Error: filepath required";
        }

        String filepath = args[0];
        log.info("Reading file: {}", filepath);

        try {
            // Implementation here
            String content = readFromSandbox(filepath);
            return content;
        } catch (Exception e) {
            log.error("Failed to read file: {}", filepath, e);
            return "Error: " + e.getMessage();
        }
    }

    private String readFromSandbox(String filepath) {
        // Actual implementation
        return "file contents";
    }
}
```

**2. Register Tool** (in `ToolRegistryConfig.java`):

```java
@Configuration
public class ToolRegistryConfig {

    @Bean
    public ToolRegistry toolRegistry(FileReader fileReader, /* other tools */) {
        ToolRegistry registry = new ToolRegistry();
        registry.registerTool(fileReader);
        // ... register other tools
        return registry;
    }
}
```

**3. Add Python Binding** (in sandbox):

```python
# In sandbox preload script
def read_file(filepath):
    """Read file from sandbox filesystem"""
    import subprocess
    result = subprocess.run(['cat', filepath], capture_output=True, text=True)
    return result.stdout
```

**4. Add Test** (`FileReaderTest.java`):

```java
@SpringBootTest
class FileReaderTest {

    @Autowired
    private FileReader fileReader;

    @Test
    void testReadFile() {
        String result = fileReader.execute("/workspace/test.txt");
        assertThat(result).isNotEmpty();
    }
}
```

**5. Update Documentation** (`docs/guides/TOOLS_GUIDE.md`).

### Adding a New REST Endpoint

**Example: Add session export endpoint**

**1. Add Controller Method** (`AgentController.java`):

```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @GetMapping("/session/{sessionId}/export")
    @Operation(summary = "Export session data", description = "Export complete session as JSON")
    public ResponseEntity<SessionExport> exportSession(@PathVariable String sessionId) {
        SessionExport export = agentService.exportSession(sessionId);
        return ResponseEntity.ok(export);
    }
}
```

**2. Add Service Method** (`CodeActAgentService.java`):

```java
public SessionExport exportSession(String sessionId) {
    AgentState state = getAgentState(sessionId);
    List<Event> events = eventRepository.findByAgentStateId(state.getId());
    List<Message> messages = messageRepository.findBySessionId(sessionId);

    return SessionExport.builder()
        .sessionId(sessionId)
        .state(state)
        .events(events)
        .messages(messages)
        .build();
}
```

**3. Add DTO** (`dto/SessionExport.java`):

```java
@Data
@Builder
public class SessionExport {
    private String sessionId;
    private AgentState state;
    private List<Event> events;
    private List<Message> messages;
}
```

**4. Update API Service** (frontend `services/api.ts`):

```typescript
export const exportSession = async (sessionId: string): Promise<SessionExport> => {
  const response = await axios.get(`/api/agent/session/${sessionId}/export`);
  return response.data;
};
```

**5. Update Documentation** (`docs/guides/API_REFERENCE.md`).

### Adding a New UI Panel

**Example: Add a "Variables" panel**

**1. Create Component** (`frontend/src/components/Panels/VariablesPanel.tsx`):

```tsx
import React from 'react';
import { useChatStore } from '../../stores/chatStore';

export const VariablesPanel: React.FC = () => {
  const variables = useChatStore((state) => state.variables);

  return (
    <div className="h-full bg-gray-900 text-white p-4">
      <h3 className="text-lg font-bold mb-4">Python Variables</h3>
      <div className="space-y-2">
        {Object.entries(variables).map(([name, value]) => (
          <div key={name} className="border-b border-gray-700 pb-2">
            <span className="text-blue-400">{name}</span>
            <span className="text-gray-400"> = </span>
            <span className="text-green-400">{JSON.stringify(value)}</span>
          </div>
        ))}
      </div>
    </div>
  );
};
```

**2. Add to Panel Store** (`stores/panelStore.ts`):

```typescript
interface PanelStore {
  activePanels: string[];
  togglePanel: (panel: string) => void;
}

export const usePanelStore = create<PanelStore>((set) => ({
  activePanels: ['browser', 'terminal', 'variables'],  // Add 'variables'
  togglePanel: (panel) => set((state) => ({
    activePanels: state.activePanels.includes(panel)
      ? state.activePanels.filter(p => p !== panel)
      : [...state.activePanels, panel]
  })),
}));
```

**3. Integrate in Layout** (`components/Layout.tsx`):

```tsx
import { VariablesPanel } from './Panels/VariablesPanel';

export const Layout: React.FC = () => {
  const activePanels = usePanelStore((state) => state.activePanels);

  return (
    <div className="grid grid-cols-3 gap-4">
      {activePanels.includes('browser') && <BrowserPanel />}
      {activePanels.includes('terminal') && <TerminalPanel />}
      {activePanels.includes('variables') && <VariablesPanel />}
    </div>
  );
};
```

---

## Debugging

### Backend Debugging

**1. Enable Debug Logs** (`application.properties`):

```properties
# Enable debug for specific packages
logging.level.ai.mymanus=DEBUG
logging.level.org.springframework.web=DEBUG

# Log all SQL queries
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**2. IntelliJ IDEA Debugging:**

- Set breakpoints in `CodeActAgentService.processQuery()`
- Right-click project → Debug 'MyManusApplication'
- Step through agent loop iteration by iteration

**3. Remote Debugging:**

```bash
# Start with debug port
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# In IntelliJ: Run → Edit Configurations → + Remote JVM Debug → Port 5005
```

**4. Common Issues:**

**Issue: Agent loop hangs**
```bash
# Check logs for LLM errors
tail -f backend/logs/spring.log | grep "anthropic"

# Check if sandbox is running
docker ps | grep mymanus-sandbox
```

**Issue: Database connection errors**
```bash
# Verify PostgreSQL is running
psql -U manus -d mymanus -c "SELECT 1;"

# Check connection in application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mymanus
```

**Issue: Code execution fails**
```bash
# Test sandbox manually
docker run -it --rm mymanus-sandbox:latest python3 -c "print('test')"

# Check sandbox logs
docker logs <container-id>
```

### Frontend Debugging

**1. Browser DevTools:**

- Open DevTools (F12)
- **Console**: View console.log, errors
- **Network**: Monitor API calls, WebSocket connections
- **React DevTools**: Inspect component state

**2. Redux DevTools (Zustand):**

```typescript
// Install zustand devtools
import { devtools } from 'zustand/middleware';

export const useChatStore = create<ChatStore>()(
  devtools(
    (set) => ({
      messages: [],
      addMessage: (message) => set((state) => ({
        messages: [...state.messages, message]
      })),
    }),
    { name: 'ChatStore' }
  )
);
```

**3. Debug WebSocket:**

```typescript
// Add logging to websocket.ts
const client = Stomp.over(socket);
client.debug = (msg) => console.log('[WS]', msg);  // Enable debug logs

client.subscribe('/topic/agent/session-123', (message) => {
  console.log('[WS] Received:', JSON.parse(message.body));
});
```

**4. Common Issues:**

**Issue: WebSocket disconnects**
```javascript
// Check connection status
console.log('WebSocket connected:', client.connected);

// Add reconnect logic
client.onStompError = (frame) => {
  console.error('WebSocket error:', frame);
  setTimeout(() => client.connect(), 5000);  // Reconnect after 5s
};
```

**Issue: State not updating**
```typescript
// Verify store is being called
const addMessage = useChatStore((state) => state.addMessage);
console.log('Adding message:', message);
addMessage(message);
console.log('Current messages:', useChatStore.getState().messages);
```

---

## Contributing

### Contribution Workflow

1. **Fork & Clone**
   ```bash
   git clone https://github.com/YOUR-USERNAME/MY-Manus.git
   cd MY-Manus
   git remote add upstream https://github.com/maheshyaddanapudi/MY-Manus.git
   ```

2. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make Changes**
   - Write code
   - Add tests
   - Update documentation

4. **Test Locally**
   ```bash
   # Backend tests
   cd backend && ./mvnw test

   # Frontend tests
   cd frontend && pnpm test

   # Manual testing
   ./mvnw spring-boot:run  # Terminal 1
   pnpm dev                # Terminal 2
   ```

5. **Commit Changes**
   ```bash
   git add .
   git commit -m "feat(scope): Description of changes"
   ```

6. **Push & Create PR**
   ```bash
   git push origin feature/your-feature-name
   # Create Pull Request on GitHub
   ```

7. **Code Review**
   - Address review comments
   - Push updates to same branch
   - PR automatically updates

8. **Merge**
   - Once approved, maintainer merges PR
   - Delete feature branch

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings generated
```

### Code Review Guidelines

**As Reviewer:**
- Be constructive and respectful
- Suggest improvements, don't demand
- Approve if changes are good enough (don't nitpick)

**As Author:**
- Respond to all comments
- Be open to feedback
- Don't take criticism personally

---

## First Contribution Ideas

Looking for your first contribution? Try these beginner-friendly tasks:

### 🟢 Easy (Good First Issue)

1. **Add Tool Description**
   - File: `docs/guides/TOOLS_GUIDE.md`
   - Task: Document an existing tool with examples
   - Skills: Markdown, reading code

2. **Improve Error Messages**
   - File: `backend/src/main/java/ai/mymanus/service/SandboxService.java`
   - Task: Make error messages more helpful
   - Skills: Java, error handling

3. **Add Loading Indicators**
   - File: `frontend/src/components/Chat/ChatInput.tsx`
   - Task: Show spinner while agent is thinking
   - Skills: React, TypeScript

4. **Add Unit Test**
   - File: `backend/src/test/java/ai/mymanus/tool/PythonExecutorTest.java`
   - Task: Add test case for edge case
   - Skills: JUnit, Java

### 🟡 Medium

5. **Add Session Export**
   - Files: `AgentController.java`, `api.ts`, `SessionList.tsx`
   - Task: Add button to export session as JSON
   - Skills: Spring Boot, React, REST API

6. **Add Keyboard Shortcuts**
   - File: `frontend/src/components/Chat/ChatInput.tsx`
   - Task: Add Ctrl+Enter to send, Ctrl+K to clear
   - Skills: React, event handling

7. **Add Prometheus Metric**
   - File: `backend/src/main/java/ai/mymanus/service/CodeActAgentService.java`
   - Task: Track average agent loop duration
   - Skills: Micrometer, Prometheus

8. **Add Dark Mode**
   - Files: `frontend/src/App.tsx`, `tailwind.config.js`
   - Task: Add dark mode toggle
   - Skills: Tailwind CSS, React

### 🔴 Advanced

9. **Add RAG Document Upload**
   - Files: Multiple (controller, service, repository, frontend)
   - Task: Implement PDF upload and chunking
   - Skills: Spring Boot, React, file handling, embeddings

10. **Add Agent Loop Visualization**
    - Files: `frontend/src/components/AgentLoop/`
    - Task: Visualize agent loop with flow diagram
    - Skills: React, D3.js or similar, WebSocket

11. **Add Multi-Agent Support**
    - Files: `CodeActAgentService.java`, database schema
    - Task: Support multiple agents working together
    - Skills: Java, concurrency, system design

---

## Getting Help

### Documentation
- **Project Overview**: `docs/PROJECT_OVERVIEW.md`
- **Architecture**: `docs/architecture/ARCHITECTURE.md`
- **API Reference**: `docs/guides/API_REFERENCE.md`
- **Event Stream**: `docs/architecture/EVENT_STREAM_GUIDE.md`

### Community
- **GitHub Issues**: https://github.com/maheshyaddanapudi/MY-Manus/issues
- **Discussions**: https://github.com/maheshyaddanapudi/MY-Manus/discussions

### Asking Good Questions

**❌ Bad Question:**
> "It doesn't work, help!"

**✅ Good Question:**
> "I'm trying to add a new tool but getting `NullPointerException` in `ToolRegistry.registerTool()` (line 45). I've added the `@Component` annotation and autowired dependencies. Here's my code: [paste code]. What am I missing?"

Include:
- What you're trying to achieve
- What you've tried
- Error messages (full stack trace)
- Relevant code snippets
- Environment (Java version, OS, etc.)

---

## Quick Reference

### Common Commands

```bash
# Backend
./mvnw clean install          # Build
./mvnw spring-boot:run        # Run
./mvnw test                   # Test
./mvnw spring-boot:run -Ddebug  # Debug mode

# Frontend
pnpm install                  # Install deps
pnpm dev                      # Run dev server
pnpm build                    # Production build
pnpm test                     # Run tests
pnpm lint                     # Lint code

# Docker
docker build -t mymanus-sandbox sandbox/
docker run --rm mymanus-sandbox python3 --version
docker ps                     # List running containers
docker logs <container-id>    # View logs

# Database
psql -U manus -d mymanus      # Connect to DB
\dt                           # List tables
\d agent_states               # Describe table
SELECT * FROM events LIMIT 10;  # Query
```

### Project URLs

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

### Key Files

- **Backend Config**: `backend/src/main/resources/application.properties`
- **Frontend Config**: `frontend/.env.local`
- **Database Schema**: `backend/src/main/resources/db/migration/`
- **Sandbox**: `sandbox/Dockerfile`

---

## Next Steps

Now that you're set up, here's what to do next:

1. **Read Core Docs**
   - `docs/PROJECT_OVERVIEW.md` - Understand the vision
   - `docs/architecture/ARCHITECTURE.md` - System architecture
   - `docs/architecture/EVENT_STREAM_GUIDE.md` - Core pattern

2. **Run Example Queries**
   - "Calculate fibonacci(10)"
   - "Fetch data from https://api.github.com/users/octocat"
   - "Create a bar chart of sales data"

3. **Explore Codebase**
   - Read `CodeActAgentService.java` (agent loop)
   - Read `PythonExecutor.java` (code execution)
   - Read `ChatInput.tsx` (frontend)

4. **Make Your First Change**
   - Pick a "Good First Issue" from above
   - Follow contribution workflow
   - Submit your first PR!

5. **Join Community**
   - Watch repository for updates
   - Participate in discussions
   - Help others getting started

---

**Welcome to MY-Manus development! Happy coding! 🚀**
