# Sandbox Architecture

## Overview

The MY-Manus sandbox implements a secure Python code execution environment using Docker containers. The architecture follows a session-based container model for efficiency and state persistence.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Backend Application                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │        CodeActAgentService (Orchestrator)             │  │
│  └────────────────────┬──────────────────────────────────┘  │
│                       │                                       │
│  ┌────────────────────▼──────────────────────────────────┐  │
│  │          PythonSandboxExecutor                        │  │
│  │  - Session container cache (Map<sessionId, containerid>│  │
│  │  - Container lifecycle management                     │  │
│  │  - Code execution and result parsing                  │  │
│  └────────────────────┬──────────────────────────────────┘  │
│                       │  Docker Java Client                  │
└───────────────────────┼──────────────────────────────────────┘
                        │
                        │  /var/run/docker.sock
                        │
        ┌───────────────▼─────────────────┐
        │       Docker Daemon             │
        └───────────────┬─────────────────┘
                        │
        ┌───────────────▼─────────────────┐
        │    Sandbox Containers           │
        │  (One per session, cached)      │
        │                                 │
        │  ┌─────────────────────────┐   │
        │  │ Session-ABC Container   │   │
        │  │ - Ubuntu 22.04          │   │
        │  │ - Python 3.11           │   │
        │  │ - Variables persist     │   │
        │  │ - Network isolated      │   │
        │  └─────────────────────────┘   │
        │                                 │
        │  ┌─────────────────────────┐   │
        │  │ Session-XYZ Container   │   │
        │  │ - Ubuntu 22.04          │   │
        │  │ - Python 3.11           │   │
        │  │ - Variables persist     │   │
        │  │ - Network isolated      │   │
        │  └─────────────────────────┘   │
        └─────────────────────────────────┘
```

## Key Components

### 1. SandboxExecutor Interface

**Location**: `/backend/src/main/java/ai/mymanus/service/sandbox/SandboxExecutor.java`

```java
public interface SandboxExecutor {
    ExecutionResult execute(String sessionId, String code,
                          Map<String, Object> previousState);
    void destroySessionContainer(String sessionId);
    void cleanupAllContainers();
    Map<String, Object> getContainerStats();
}
```

**Implementations**:
- `PythonSandboxExecutor`: Docker-based (production)
- `HostPythonExecutor`: Host-based (development)

### 2. Container Management

**Session-Based Caching**:

```java
// Container cache: sessionId -> containerId
private final Map<String, String> sessionContainers = new ConcurrentHashMap<>();

private String getOrCreateContainer(String sessionId) {
    String existingContainerId = sessionContainers.get(sessionId);

    // Reuse if exists and running
    if (existingContainerId != null && isContainerRunning(existingContainerId)) {
        return existingContainerId; // Fast path!
    }

    // Create new container
    String containerId = createContainer(sessionId);
    dockerClient.startContainerCmd(containerId).exec();

    // Cache it
    sessionContainers.put(sessionId, containerId);

    return containerId;
}
```

**Benefits**:
- **First execution**: ~500-1000ms (container creation + Python startup)
- **Subsequent executions**: ~100-200ms (reuses existing container)
- **State persistence**: Variables persist between code executions

### 3. Sandbox Image

**Base**: Ubuntu 22.04
**Python**: 3.11
**Node.js**: 22.13 (via NVM)

**Key Packages**:
- Data science: pandas, numpy, matplotlib, seaborn, plotly
- Web: requests, httpx, beautifulsoup4, playwright
- Documents: fpdf2, reportlab, openpyxl
- Cloud: boto3, openai

See `/sandbox/Dockerfile` for complete package list.

## Execution Flow

### 1. Code Preparation

```java
private String buildExecutionScript(String code, Map<String, Object> previousState) {
    StringBuilder script = new StringBuilder();

    // 1. Tool bindings (Python functions that call back to Java)
    script.append(toolRegistry.generatePythonBindings());

    // 2. Restore previous variables from JSONB
    if (previousState != null && !previousState.isEmpty()) {
        previousState.forEach((key, value) -> {
            // Deserialize and restore each variable
            script.append(key).append(" = ").append(deserialize(value)).append("\n");
        });
    }

    // 3. User code
    script.append(code);

    // 4. Capture new variables for next execution
    script.append("""
        import json
        _vars = {k: v for k, v in globals().items()
                 if not k.startswith('_')}
        print('__VARS_START__')
        print(json.dumps(_vars, default=str))
        print('__VARS_END__')
        """);

    return script.toString();
}
```

### 2. Container Execution

```java
private ExecutionResult executeInContainer(String containerId) {
    // 1. Copy code file to container
    writeCodeToContainer(containerId, fullCode);

    // 2. Execute Python
    ExecCreateCmdResponse execCmd = dockerClient.execCreateCmd(containerId)
        .withCmd("python3.11", "/home/ubuntu/workspace/code.py")
        .withAttachStdout(true)
        .withAttachStderr(true)
        .exec();

    // 3. Capture output with timeout
    dockerClient.execStartCmd(execCmd.getId())
        .exec(new ResultCallback.Adapter<Frame>() {
            public void onNext(Frame frame) {
                if (frame.getStreamType() == StreamType.STDOUT) {
                    stdout.write(frame.getPayload());
                } else {
                    stderr.write(frame.getPayload());
                }
            }
        })
        .awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);

    // 4. Parse results
    return ExecutionResult.builder()
        .success(stderrStr.isEmpty())
        .stdout(cleanOutput(stdoutStr))
        .stderr(stderrStr)
        .variables(extractVariables(stdoutStr))
        .build();
}
```

### 3. Variable Persistence

Variables are extracted from stdout and stored in PostgreSQL:

```
[Execution 1] x = 42        →  Store: {x: 42}
[Execution 2] y = x + 1     →  Restore: {x: 42}, Execute, Store: {x: 42, y: 43}
[Execution 3] print(x, y)   →  Restore: {x: 42, y: 43}, Execute
```

## Security Model

### Isolation Layers

1. **User Isolation**
   - Container runs as non-root `ubuntu` user
   - No sudo access

2. **Network Isolation**
   - `network-mode=none` (default)
   - No internet access unless explicitly enabled

3. **Filesystem Isolation**
   - Workspace directory: `/home/ubuntu/workspace`
   - File tools enforce path validation
   - No access to host filesystem

4. **Resource Limits**
   - Memory: 512MB (default)
   - CPU: 50% (default)
   - Execution timeout: 30 seconds

5. **Process Isolation**
   - Each session has own container
   - Containers cannot interact

### Configuration

```properties
# Docker settings
sandbox.docker.image=mymanus-sandbox:latest
sandbox.docker.memory-limit=536870912  # 512MB
sandbox.docker.cpu-quota=50000         # 50% CPU
sandbox.docker.timeout-seconds=30
sandbox.docker.network-mode=none       # Isolated

# Workspace
sandbox.workspace-path=/workspace
```

## Performance Characteristics

### Container Lifecycle

| Stage | Duration | Notes |
|-------|----------|-------|
| Create container | 200-300ms | First execution only |
| Start container | 100-200ms | First execution only |
| Python startup | 200-400ms | First execution only |
| **Total first execution** | **500-1000ms** | One-time cost per session |
| Execute code (cached) | 50-150ms | Subsequent executions |
| **Total cached execution** | **100-200ms** | Normal operation |

### Memory Usage

| Component | Per Container | Notes |
|-----------|---------------|-------|
| Base OS + Python | ~100MB | Ubuntu + Python 3.11 |
| Python packages | ~200MB | pandas, numpy, etc. |
| Runtime variables | Varies | User code data |
| **Total** | **~300-500MB** | Per active session |

### Scaling Considerations

- **Horizontal**: Multiple backend instances, each with own Docker daemon
- **Vertical**: Increase container resource limits
- **Cleanup**: Auto-cleanup idle containers (can be implemented)

## Tool Integration

Tools generate Python code that executes in the sandbox:

```java
// Example: ShellExecTool
private String generateShellPython(String command, int timeout) {
    return String.format("""
        import subprocess
        import sys

        try:
            result = subprocess.run(
                '%s',
                shell=True,
                capture_output=True,
                text=True,
                timeout=%d
            )
            print(result.stdout, end='')
            sys.exit(result.returncode)
        except subprocess.TimeoutExpired:
            print('Command timed out', file=sys.stderr)
            sys.exit(124)
        """, command, timeout);
}
```

The generated Python code is executed in the sandbox with full isolation.

## Monitoring

### Container Stats API

```java
@Override
public Map<String, Object> getContainerStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("activeSessions", sessionContainers.size());

    sessionContainers.forEach((sessionId, containerId) -> {
        InspectContainerResponse info =
            dockerClient.inspectContainerCmd(containerId).exec();
        stats.put(sessionId, Map.of(
            "containerId", containerId,
            "running", info.getState().getRunning(),
            "created", info.getCreated()
        ));
    });

    return stats;
}
```

### Health Checks

Containers include health checks for monitoring:

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s \
  CMD python3.11 -c "print('healthy')" || exit 1
```

## Future Enhancements

1. **Container Pooling**: Pre-warmed container pool for faster cold starts
2. **Resource Monitoring**: Per-container resource usage tracking
3. **Auto-Cleanup**: Remove idle containers after timeout
4. **Shared Cache**: Shared Python package cache across containers
5. **GPU Support**: NVIDIA Docker for ML workloads
6. **Multi-Language**: Support for Node.js, Java, etc.

## Best Practices

1. **Always cache containers** per session for performance
2. **Set resource limits** to prevent resource exhaustion
3. **Use network isolation** unless tools require internet
4. **Monitor container count** to prevent runaway growth
5. **Clean up on session end** to free resources
6. **Log all executions** for debugging and auditing
7. **Test security** regularly with penetration testing

## Comparison with Alternatives

| Approach | Pros | Cons | Used By |
|----------|------|------|---------|
| **Docker (Our Choice)** | Strong isolation, resource limits, production-ready | Overhead, requires Docker daemon | Manus AI, Code Interpreter |
| **Direct Host** | Fast, simple | No isolation, security risk | Development only |
| **Virtualization** | Strong isolation | Heavy overhead, slow | Enterprise solutions |
| **WebAssembly** | Fast, lightweight | Limited ecosystem | Emerging |

Our implementation balances security, performance, and practicality using Docker with session-based caching.
