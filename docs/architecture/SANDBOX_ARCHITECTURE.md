# Sandbox Architecture - Deep Dive

## 🏗️ Current Architecture

### Sandbox Image Location
```
sandbox/Dockerfile  ← Separate from backend/frontend
```
✅ **Correct** - Sandbox is a completely separate Docker image

### How Backend Communicates with Sandbox

#### Scenario 1: Backend on Host Machine
```
Host Machine
├── Backend (Java process)
│   └── Docker Java Client
│       └── Connects to: unix:///var/run/docker.sock
├── Docker Daemon
│   └── Manages sandbox containers
└── Sandbox Containers (ephemeral)
```

**Communication Flow:**
1. Backend uses Docker Java Client library
2. Connects to Docker daemon via Unix socket
3. Creates/manages containers programmatically

**Configuration:**
```java
DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
    .withDockerHost("unix:///var/run/docker.sock")  // Default on Linux/Mac
    .build();
```

#### Scenario 2: Backend in Container (docker-compose)
```
Docker Network
├── Backend Container
│   ├── Backend (Java process)
│   ├── Docker Java Client
│   └── Mounted: /var/run/docker.sock  ← KEY!
├── PostgreSQL Container
└── Sandbox Containers (created by backend)
```

**Our docker-compose.yml:**
```yaml
backend:
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock  # ✅ Already configured!
```

**How it works:**
- Backend container has Docker socket mounted
- Can create "sibling" containers (not child containers)
- This is **Docker-out-of-Docker (DooD)** pattern
- Sandbox containers run alongside backend, not inside it

---

## ⚠️ Current Implementation Issue

### Problem: Ephemeral Containers (One Per Execution)

**Current Code in `PythonSandboxExecutor.java`:**
```java
public ExecutionResult execute(String code, Map<String, Object> previousState) {
    // 1. Create new container
    String containerId = createContainer();

    // 2. Start container
    dockerClient.startContainerCmd(containerId).exec();

    // 3. Execute code
    ExecutionResult result = executeInContainer(containerId);

    // 4. Destroy container immediately!
    cleanup(containerId);  // ← Problem!

    return result;
}
```

**What Happens:**
```
User sends message → Agent generates 5 code blocks
→ Container created (2s)
→ Code executed (1s)
→ Container destroyed (1s)
→ Container created (2s)   ← Repeat 5 times!
→ Code executed (1s)
→ Container destroyed (1s)
...
```

**Total time:** ~20 seconds just for container overhead!

**Pros:**
- ✅ Maximum security (isolation between executions)
- ✅ No state leakage
- ✅ Clean slate every time

**Cons:**
- ❌ **VERY SLOW** (2-5 seconds per execution)
- ❌ Wasteful (creating/destroying containers)
- ❌ Variables don't truly persist (we serialize/deserialize)

---

## ✅ Recommended Architecture: Session-Based Containers

### Option A: One Container Per Session (RECOMMENDED)

```
Session 1 (user-123)
└── Container manus-sandbox-session-123
    └── Stays alive during entire conversation
    └── Destroyed when session cleared

Session 2 (user-456)
└── Container manus-sandbox-session-456
    └── Independent from session 1
```

**Benefits:**
- ✅ Fast (container created once, reused)
- ✅ True variable persistence (Python process stays alive)
- ✅ Better resource utilization
- ✅ Still isolated per user

**Implementation Changes Needed:**

```java
// Add container cache
private final Map<String, String> sessionContainers = new ConcurrentHashMap<>();

public ExecutionResult execute(String sessionId, String code, Map<String, Object> previousState) {
    // Get or create container for this session
    String containerId = getOrCreateContainer(sessionId);

    // Execute in existing container
    ExecutionResult result = executeInContainer(containerId);

    // Keep container alive!
    return result;
}

private String getOrCreateContainer(String sessionId) {
    String existingId = sessionContainers.get(sessionId);

    if (existingId != null && isRunning(existingId)) {
        return existingId;  // Reuse!
    }

    // Create new container for this session
    String newId = createContainer(sessionId);
    sessionContainers.put(sessionId, newId);
    return newId;
}

// Clean up when session is cleared
public void destroySessionContainer(String sessionId) {
    String containerId = sessionContainers.remove(sessionId);
    if (containerId != null) {
        cleanup(containerId);
    }
}
```

**When to destroy:**
- User clears session (`DELETE /api/agent/session/{id}`)
- Application shutdown
- Container crashes
- Idle timeout (optional: 30 minutes)

---

### Option B: Container Pool (ADVANCED)

```
Container Pool (size: 5)
├── Container 1 (available)
├── Container 2 (in use by session-123)
├── Container 3 (in use by session-456)
├── Container 4 (available)
└── Container 5 (available)
```

**Benefits:**
- ✅ Fastest (pre-warmed containers)
- ✅ Good for multi-user scenarios

**Cons:**
- ❌ More complex
- ❌ Less secure (must clean state between users)
- ❌ Resource overhead (idle containers)

**Not recommended for MVP**

---

### Option C: Ephemeral (CURRENT - NOT RECOMMENDED)

**Keep for:**
- Maximum paranoia security requirements
- Compliance needs
- Testing/debugging

**Current behavior:**
- Creates/destroys per execution
- Slow but safest

---

---

## 🖥️ Option D: Host Mode (NEW - For Development)

**When:** Local development only

**How it works:**
```
Backend (Java)
    │
    ↓ ProcessBuilder
    │
Python Process (Direct Execution)
    │
    ↓ Writes to
    │
/tmp/manus-workspace/{sessionId}/
```

**Benefits:**
- ⚡ **Fastest** - No Docker overhead
- 🐛 **Easy debugging** - Direct Python process
- 🔧 **No Docker required** - Simpler setup
- 📁 **File access** - Easy to inspect workspace

**Implementation:**
- Uses Java `ProcessBuilder` to execute Python directly
- Creates workspace directories per session
- Captures stdout/stderr via process streams
- Same state serialization as Docker mode

**Configuration:**
```yaml
sandbox:
  mode: host  # Set to 'host' for development
  host:
    python-executable: python3
    workspace-dir: /tmp/manus-workspace
    timeout-seconds: 30
```

**Security Considerations:**
- ⚠️ **NOT SECURE** - Code runs directly on your machine
- 🚫 No resource limits
- 🚫 No network isolation
- 🚫 No filesystem isolation
- ✅ Only use in trusted development environments

**Performance Comparison:**
```
Host Mode:
Create workspace (instant)
Execute code #1 (0.5s)
Execute code #2 (0.5s)
Execute code #3 (0.5s)
Execute code #4 (0.5s)
Execute code #5 (0.5s)
Total: ~2.5 seconds

Docker Session-Based:
Create container (2s)
Execute code #1 (1s)
Execute code #2 (1s)
Execute code #3 (1s)
Execute code #4 (1s)
Execute code #5 (1s)
Total: ~7 seconds

Improvement: 3x faster than Docker!
```

---

## 🔧 How Docker Communication Works

### Docker Socket Communication

**Unix Socket (Linux/Mac):**
```
Backend Process
    │
    ↓ Docker Java Client
    │
    ↓ HTTP over Unix Socket
    │
Docker Daemon (unix:///var/run/docker.sock)
    │
    ↓ Creates/Manages
    │
Sandbox Containers
```

**Windows (Named Pipe):**
```
Backend Process
    │
    ↓ Docker Java Client
    │
    ↓ HTTP over Named Pipe
    │
Docker Daemon (npipe:////./pipe/docker_engine)
    │
    ↓ Creates/Manages
    │
Sandbox Containers
```

### Docker API Calls Made

1. **Create Container:**
```java
dockerClient.createContainerCmd(sandboxImage)
    .withName("manus-sandbox-session-123")
    .withHostConfig(hostConfig)
    .exec();
```

2. **Start Container:**
```java
dockerClient.startContainerCmd(containerId).exec();
```

3. **Execute Command:**
```java
dockerClient.execCreateCmd(containerId)
    .withCmd("python3.11", "/workspace/code.py")
    .exec();
```

4. **Copy Files:**
```java
dockerClient.copyArchiveToContainerCmd(containerId)
    .withHostResource("/tmp/code.py")
    .withRemotePath("/workspace/code.py")
    .exec();
```

5. **Stop/Remove:**
```java
dockerClient.stopContainerCmd(containerId).exec();
dockerClient.removeContainerCmd(containerId).exec();
```

---

## 🔒 Security Considerations

### Per-Execution Containers (Current)
```
Security: ⭐⭐⭐⭐⭐ (Maximum)
Speed:    ⭐ (Very slow)
```
- No state leakage possible
- Complete isolation
- Clean environment every time

### Per-Session Containers (Recommended)
```
Security: ⭐⭐⭐⭐ (Very good)
Speed:    ⭐⭐⭐⭐ (Fast)
```
- Isolated per user
- State persists within session
- Must clean up on session end

### Container Pool
```
Security: ⭐⭐⭐ (Good, with careful cleanup)
Speed:    ⭐⭐⭐⭐⭐ (Fastest)
```
- Must sanitize between users
- Risk of state leakage if not cleaned properly
- Best for trusted environments

---

## 📊 Performance Comparison

### Scenario: User asks agent to create 5 visualizations

**Current (Ephemeral):**
```
Create container (2s)
Execute code #1 (1s)
Destroy container (1s)
Create container (2s)   ← Overhead!
Execute code #2 (1s)
Destroy container (1s)
...
Total: ~20 seconds
```

**Session-Based (Recommended):**
```
Create container (2s)   ← Once!
Execute code #1 (1s)
Execute code #2 (1s)
Execute code #3 (1s)
Execute code #4 (1s)
Execute code #5 (1s)
Total: ~7 seconds
```

**Improvement: 3x faster!**

---

## 🎯 Recommendation

### For Production: Use Session-Based Containers (Implemented ✅)

**Why:**
1. **Much faster** - 3-5x improvement over ephemeral
2. **True Python state** - Variables actually persist
3. **Still secure** - Isolated per user
4. **Scalable** - Works with many concurrent users
5. **Simple** - Cache containers by session ID

**Status:** ✅ Fully implemented
- `SandboxExecutor` interface created
- `PythonSandboxExecutor` implements session-based caching
- Conditional Spring Bean based on `sandbox.mode=docker`

### For Development: Use Host Mode (Implemented ✅)

**Why:**
1. **Fastest** - No Docker overhead
2. **Easy debugging** - Direct Python process
3. **Simple setup** - No Docker build required
4. **File inspection** - Easy to view workspace

**Status:** ✅ Fully implemented
- `HostPythonExecutor` implements direct execution
- Conditional Spring Bean based on `sandbox.mode=host`
- Automatically enabled in `application-dev.yml`

**⚠️ Security:** Only use host mode in trusted development environments!

### Migration Guide

**Switch to Docker Mode (Production):**
```yaml
# application.yml
sandbox:
  mode: docker  # Default
```

**Switch to Host Mode (Development):**
```yaml
# application-dev.yml or .env
sandbox:
  mode: host
SANDBOX_MODE=host
```

Both modes implement the same `SandboxExecutor` interface, so the application code doesn't need to change!

---

## 📝 Implementation Status

### Session-Based Containers: ✅ COMPLETE

- [x] Created `SandboxExecutor` interface
- [x] Updated `PythonSandboxExecutor` class
  - [x] Added `sessionContainers` cache
  - [x] Changed `execute()` signature to include `sessionId`
  - [x] Implemented `getOrCreateContainer(sessionId)`
  - [x] Added `destroySessionContainer(sessionId)`
  - [x] Added `@PreDestroy` cleanup
  - [x] Added `@ConditionalOnProperty` for mode switching

- [x] Created `HostPythonExecutor` class
  - [x] Implemented direct Python execution
  - [x] Session-based workspace directories
  - [x] Same interface as Docker mode
  - [x] Added `@ConditionalOnProperty` for mode switching

- [x] Updated `CodeActAgentService`
  - [x] Use `SandboxExecutor` interface
  - [x] Pass `sessionId` to sandbox executor
  - [x] Call `destroySessionContainer()` on session clear

- [x] Updated `SandboxController`
  - [x] Use `SandboxExecutor` interface
  - [x] Handle both modes in stats endpoint
  - [x] Handle both modes in cleanup endpoints

- [x] Added monitoring endpoints
  - [x] GET `/api/sandbox/stats` - Show active environments
  - [x] DELETE `/api/sandbox/cleanup/{sessionId}` - Cleanup specific
  - [x] POST `/api/sandbox/cleanup/all` - Force cleanup all

- [x] Updated configuration
  - [x] Added `sandbox.mode` property
  - [x] Docker mode configuration
  - [x] Host mode configuration
  - [x] Dev profile defaults to host mode

- [x] Updated documentation
  - [x] QUICKSTART.md - Added sandbox mode selection
  - [x] SETUP.md - Documented both modes
  - [x] SANDBOX_ARCHITECTURE.md - Comprehensive guide

---

## 🎉 Status: COMPLETE

Both Docker mode (session-based) and Host mode are fully implemented and ready to use!

**Choose your mode:**
- Production: `sandbox.mode=docker` (default)
- Development: `sandbox.mode=host` (faster, less secure)
