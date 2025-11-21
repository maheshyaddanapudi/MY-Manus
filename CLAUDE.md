# Manus AI Clone - CodeAct Platform

## Mission
Build a Manus AI clone where agents write and execute Python code to solve tasks. Provide transparent visualization through Browser, Terminal, and Editor panels.

## Core Concept
- **CodeAct**: Agents generate Python code instead of calling APIs
- **Sandbox**: Ubuntu 22.04 container matching Manus environment
- **UI**: Three synchronized panels showing real-time execution
- **PostgreSQL**: Single database for all persistence
- **Auth**: Optional (disabled for development)

## Project Setup
```
manus-clone/
├── backend/      # Spring Boot
├── frontend/     # React + TypeScript
├── sandbox/      # Docker environment
└── docs/         # Guides
```

## Commands for Claude Code

### START
Initialize the project structure:
1. Spring Boot with WebSocket, JPA, Docker dependencies
2. React with TypeScript, Tailwind, Zustand
3. PostgreSQL database setup
4. Basic Docker sandbox

### AGENT
Build the CodeAct loop:
1. Parse Python code from LLM responses
2. Execute in Docker sandbox
3. Capture output as observations
4. Feed back to agent
5. Persist state in PostgreSQL

### UI
Create the interface:
1. Three-panel layout (Browser/Terminal/Editor)
2. Chat with streaming responses
3. WebSocket for real-time updates
4. Tool execution visualization

### SANDBOX
Setup execution environment:
1. Ubuntu 22.04 base
2. Python 3.11 + essential packages
3. Node.js 22.13 + pnpm
4. FFmpeg, Poppler utilities
5. Browser automation support

### TOOLS [name]
Add a new tool:
1. Create tool class implementing Tool interface
2. Register in ToolRegistry
3. Add Python bindings
4. Update UI visualization

### TEST
Verify everything works:
1. Backend unit tests
2. Frontend component tests
3. Agent loop integration test
4. Sandbox security test

## Development Phases

### Week 1-2: Core
- Basic agent loop
- Python code execution
- Simple chat UI
- PostgreSQL setup

### Week 3-4: Enhanced
- Multi-tool support  
- State persistence
- Terminal/Editor UI
- Error recovery

### Week 5-6: Production
- Full sandbox environment
- Browser automation
- Session replay
- Multi-agent support

## Key Design Patterns

### Agent Loop
```
User Query → LLM generates code → Execute in sandbox → 
Capture output → Feed to LLM → Repeat until done
```

### State Management
- Messages in PostgreSQL
- Execution context as JSONB
- Python variables persist between blocks
- Tool executions logged

### Security
- All code runs in Docker
- Resource limits enforced
- Network isolated
- Input sanitization

### Authentication
- Development: `auth.enabled=false`
- Production: `auth.enabled=true` with JWT

## Implementation Notes

### What We Build
- Custom agent loop (not Manus API)
- Our tool system
- Custom utilities to replace manus-* tools
- Direct Anthropic API integration

### What We Match
- Ubuntu 22.04 environment
- Python/Node versions
- Three-panel UI pattern
- Real-time visualization

### Key Libraries
- Spring Boot + Spring AI
- React + TypeScript  
- Docker Java client
- PostgreSQL with JSONB
- WebSocket + SSE
- Monaco Editor + xterm.js

## Quick Start
1. Type `START` - Initialize project
2. Type `AGENT` - Build core loop
3. Type `UI` - Create interface
4. Type `SANDBOX` - Setup environment
5. Type `TEST` - Verify it works

## References
- docs/AGENT_GUIDE.md - Agent implementation
- docs/UI_GUIDE.md - Frontend patterns
- docs/SANDBOX_GUIDE.md - Environment setup
- docs/TOOLS_GUIDE.md - Tool development
- docs/DEPLOYMENT.md - Production setup