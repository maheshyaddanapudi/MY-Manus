# Sandbox Environment Guide

## Overview

The Manus AI clone uses Docker-based sandboxing for secure Python code execution. The sandbox environment is based on Ubuntu 22.04 and includes Python 3.11, Node.js 22.13, and essential data science packages.

## Architecture

### Two Execution Modes

**1. Docker Mode (Production)**
- **Location**: `/backend/src/main/java/ai/mymanus/service/sandbox/PythonSandboxExecutor.java`
- Executes code in isolated Docker containers
- One container per session (cached and reused)
- Resource limits enforced
- Network isolated by default

**2. Host Mode (Development/Testing)**
- **Location**: `/backend/src/main/java/ai/mymanus/service/sandbox/HostPythonExecutor.java`
- Executes code directly on host machine
- Faster for development
- No resource limits
- Enabled via `sandbox.mode=host`

## Dockerfile

**Location**: `/sandbox/Dockerfile`

```dockerfile
FROM ubuntu:22.04

# System packages
RUN apt-get update && apt-get install -y \
    build-essential gcc g++ make \
    git curl wget vim nano \
    jq tar gzip zip unzip \
    libssl3 libcurl4 libxml2 libxmlsec1-dev \
    libjpeg-turbo8 libpng16-16 libfreetype6 \
    ffmpeg poppler-utils graphviz \
    python3.11 python3.11-dev python3.11-venv python3-pip \
    openjdk-11-jre-headless \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m -s /bin/bash ubuntu && \
    mkdir -p /home/ubuntu/workspace && \
    chown -R ubuntu:ubuntu /home/ubuntu

USER ubuntu
WORKDIR /home/ubuntu

# Python packages
RUN python3.11 -m pip install --user --upgrade pip setuptools wheel && \
    python3.11 -m pip install --user \
    pandas==2.2.3 numpy==2.1.3 \
    matplotlib==3.10.0 seaborn==0.13.2 plotly==5.24.1 \
    fastapi==0.119.1 flask==3.1.0 uvicorn==0.32.1 \
    requests==2.32.3 httpx==0.28.1 \
    fpdf2==2.8.4 reportlab==4.2.5 pypdf==5.1.0 openpyxl==3.1.5 \
    beautifulsoup4==4.12.3 lxml==5.3.0 playwright==1.48.0 \
    boto3==1.35.82 openai==1.57.4 \
    pillow==11.0.0 python-dotenv==1.0.1

# Install NVM and Node.js 22.13
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash && \
    export NVM_DIR="$HOME/.nvm" && \
    nvm install 22.13.0 && \
    nvm use 22.13.0 && \
    npm install -g pnpm@10.8.0 yarn@1.22.22 && \
    python3.11 -m playwright install chromium

ENV PATH=/home/ubuntu/.local/bin:/home/ubuntu/.nvm/versions/node/v22.13.0/bin:$PATH
ENV PYTHONPATH=/home/ubuntu/workspace
ENV PYTHONUNBUFFERED=1
WORKDIR /home/ubuntu/workspace
```

## Build and Run

### Build Image

```bash
cd sandbox
docker build -t mymanus-sandbox:latest .
```

### Test Container

```bash
docker run -it --rm mymanus-sandbox:latest python3.11 -c "import pandas; print(pandas.__version__)"
# Should output: 2.2.3
```

## Container Lifecycle

### Session-Based Containers

**Key Pattern**: One container per session, cached and reused for efficiency.

```java
@Service
public class PythonSandboxExecutor implements SandboxExecutor {

    // Container cache: sessionId -> containerId
    private final Map<String, String> sessionContainers = new ConcurrentHashMap<>();

    @Override
    public ExecutionResult execute(String sessionId, String code,
                                    Map<String, Object> previousState) {
        // Get or create container for this session (cached!)
        String containerId = getOrCreateContainer(sessionId);

        // Execute code in container
        ExecutionResult result = executeInContainer(containerId);

        // Container stays alive for reuse!
        return result;
    }

    private String getOrCreateContainer(String sessionId) {
        String existingContainerId = sessionContainers.get(sessionId);

        // Check if we have a cached container that's still running
        if (existingContainerId != null && isContainerRunning(existingContainerId)) {
            return existingContainerId; // Reuse!
        }

        // Create new container
        String containerId = createContainer(sessionId);
        dockerClient.startContainerCmd(containerId).exec();

        // Cache it
        sessionContainers.put(sessionId, containerId);

        return containerId;
    }
}
```

**Benefits**:
- No cold start after first execution
- Variables persist between executions
- Faster execution (no container creation overhead)

### Container Creation

```java
private String createContainer(String sessionId) {
    CreateContainerResponse container = dockerClient.createContainerCmd(sandboxImage)
        .withName("manus-session-" + sessionId)
        .withHostConfig(new HostConfig()
            .withMemory(memoryLimit) // 512MB
            .withCpuQuota(cpuQuota) // 50% CPU
            .withNetworkMode(networkMode)) // "none" = isolated
        .withWorkingDir("/home/ubuntu/workspace")
        .withUser("ubuntu")
        .withTty(true) // Keep alive
        .withStdinOpen(true)
        .exec();

    return container.getId();
}
```

### Container Cleanup

```java
@Override
public void destroySessionContainer(String sessionId) {
    String containerId = sessionContainers.remove(sessionId);
    if (containerId != null) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
            log.info("Destroyed container for session {}", sessionId);
        } catch (Exception e) {
            log.warn("Failed to destroy container {}", containerId, e);
        }
    }
}

@PreDestroy
public void cleanupAllContainers() {
    sessionContainers.values().forEach(containerId -> {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.warn("Cleanup failed for container {}", containerId, e);
        }
    });
}
```

## Code Execution Flow

### 1. Prepare Execution Script

```java
private String buildExecutionScript(String code, Map<String, Object> previousState) {
    StringBuilder script = new StringBuilder();

    // 1. Tool bindings
    script.append(toolRegistry.generatePythonBindings());
    script.append("\n\n");

    // 2. Restore previous variables
    if (previousState != null && !previousState.isEmpty()) {
        script.append("# Restore previous state\n");
        script.append("import pickle, base64\n");
        previousState.forEach((key, value) -> {
            String serialized = serializeToBase64(value);
            script.append(key).append(" = pickle.loads(base64.b64decode('")
                  .append(serialized).append("'))\n");
        });
        script.append("\n");
    }

    // 3. User code
    script.append("# User code\n");
    script.append(code);
    script.append("\n\n");

    // 4. Capture new variables
    script.append("""
        # Capture variables for next execution
        import json
        _vars = {k: v for k, v in globals().items()
                 if not k.startswith('_') and k not in ['json', 'pickle', 'base64']}
        # Serialize and print
        print('__VARS_START__')
        print(json.dumps(_vars, default=str))
        print('__VARS_END__')
        """);

    return script.toString();
}
```

### 2. Write Code to Container

```java
private void writeCodeToContainer(String containerId, String code) throws Exception {
    File tempFile = File.createTempFile("code-", ".py");
    try (FileWriter writer = new FileWriter(tempFile)) {
        writer.write(code);
    }

    // Copy file into container
    dockerClient.copyArchiveToContainerCmd(containerId)
        .withHostResource(tempFile.getAbsolutePath())
        .withRemotePath("/home/ubuntu/workspace/code.py")
        .exec();

    tempFile.delete();
}
```

### 3. Execute in Container

```java
private ExecutionResult executeInContainer(String containerId) throws Exception {
    // Create exec command
    ExecCreateCmdResponse execCmd = dockerClient.execCreateCmd(containerId)
        .withCmd("python3.11", "/home/ubuntu/workspace/code.py")
        .withAttachStdout(true)
        .withAttachStderr(true)
        .exec();

    // Execute and capture output
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    dockerClient.execStartCmd(execCmd.getId())
        .exec(new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
                try {
                    if (frame.getStreamType() == StreamType.STDOUT) {
                        stdout.write(frame.getPayload());
                    } else if (frame.getStreamType() == StreamType.STDERR) {
                        stderr.write(frame.getPayload());
                    }
                } catch (Exception e) {
                    log.error("Error processing frame", e);
                }
            }
        })
        .awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);

    // Parse output
    String stdoutStr = stdout.toString(StandardCharsets.UTF_8);
    String stderrStr = stderr.toString(StandardCharsets.UTF_8);

    // Extract variables from stdout
    Map<String, Object> variables = extractVariables(stdoutStr);

    return ExecutionResult.builder()
        .success(stderrStr.isEmpty())
        .stdout(cleanOutput(stdoutStr))
        .stderr(stderrStr)
        .variables(variables)
        .build();
}
```

### 4. Parse Results

```java
private Map<String, Object> extractVariables(String stdout) {
    int startIdx = stdout.indexOf("__VARS_START__");
    int endIdx = stdout.indexOf("__VARS_END__");

    if (startIdx == -1 || endIdx == -1) {
        return Map.of();
    }

    String varsJson = stdout.substring(
        startIdx + "__VARS_START__".length(),
        endIdx
    ).trim();

    try {
        return objectMapper.readValue(varsJson,
            new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
        log.warn("Failed to parse variables", e);
        return Map.of();
    }
}
```

## Configuration

**Location**: `/backend/src/main/resources/application.properties`

```properties
# Sandbox mode: docker (production) or host (development)
sandbox.mode=docker

# Docker settings
sandbox.docker.image=mymanus-sandbox:latest
sandbox.docker.memory-limit=536870912  # 512MB
sandbox.docker.cpu-quota=50000         # 50% CPU
sandbox.docker.timeout-seconds=30
sandbox.docker.network-mode=none       # Isolated (no internet)

# Workspace directory (for file tools)
sandbox.workspace=/tmp/manus-workspace
```

## Security

### Isolation Layers

1. **User Isolation**: Runs as non-root `ubuntu` user
2. **Network Isolation**: `network-mode=none` blocks internet access
3. **Resource Limits**: Memory and CPU quotas enforced
4. **Filesystem Isolation**: Workspace-only access for file operations
5. **Timeout Protection**: 30-second max execution time

### File System Security

File tools use workspace sandboxing:

```java
protected Path validateAndResolvePath(String filePath) {
    Path workspaceRoot = Paths.get(WORKSPACE_ROOT).toRealPath();
    Path resolvedPath = workspaceRoot.resolve(filePath).normalize();

    // Security check: path must stay within workspace
    if (!resolvedPath.startsWith(workspaceRoot)) {
        throw new SecurityException("Access denied: Path escapes workspace");
    }

    return resolvedPath;
}
```

### Network Control

Enable network for specific tools:

```java
@Override
public boolean requiresNetwork() {
    return true; // Tool needs network access
}
```

Configure network mode:
```properties
sandbox.docker.network-mode=bridge  # Enable network
sandbox.docker.network-mode=none    # Disable network (default)
```

## Monitoring

### Container Stats

```java
@Override
public Map<String, Object> getContainerStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("activeSessions", sessionContainers.size());
    stats.put("containerIds", sessionContainers.values());

    sessionContainers.forEach((sessionId, containerId) -> {
        try {
            InspectContainerResponse info =
                dockerClient.inspectContainerCmd(containerId).exec();
            stats.put(sessionId, Map.of(
                "containerId", containerId,
                "running", info.getState().getRunning(),
                "created", info.getCreated()
            ));
        } catch (Exception e) {
            log.warn("Failed to get stats for {}", sessionId);
        }
    });

    return stats;
}
```

### Logging

Structured logging throughout execution:

```java
log.info("Creating new container for session {}", sessionId);
log.debug("Using container {} for session {}", containerId, sessionId);
log.info("Executed code in session {} in {}ms", sessionId, duration);
log.warn("Failed to destroy container {}", containerId, e);
```

## Testing

### Integration Test

```java
@SpringBootTest
class PythonSandboxExecutorTest {

    @Autowired
    private SandboxExecutor sandboxExecutor;

    @Test
    void testBasicExecution() {
        String code = "result = 2 + 2\nprint(result)";

        ExecutionResult result = sandboxExecutor.execute(
            "test-session",
            code,
            Map.of()
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("4"));
        assertEquals(4, result.getVariables().get("result"));
    }

    @Test
    void testStatePersistence() {
        String sessionId = "test-session-2";

        // First execution
        ExecutionResult result1 = sandboxExecutor.execute(
            sessionId,
            "x = 42",
            Map.of()
        );
        assertEquals(42, result1.getVariables().get("x"));

        // Second execution (reuses container and state)
        ExecutionResult result2 = sandboxExecutor.execute(
            sessionId,
            "y = x + 1\nprint(y)",
            result1.getVariables()
        );
        assertTrue(result2.getStdout().contains("43"));
        assertEquals(43, result2.getVariables().get("y"));
    }

    @Test
    void testTimeout() {
        String code = "import time\ntime.sleep(60)"; // Exceeds 30s timeout

        ExecutionResult result = sandboxExecutor.execute(
            "test-session-3",
            code,
            Map.of()
        );

        assertFalse(result.isSuccess());
        // Should timeout and return error
    }
}
```

## Troubleshooting

### Container Not Starting

**Problem**: Container creation fails
**Solution**:
```bash
# Check Docker is running
docker ps

# Verify image exists
docker images | grep mymanus-sandbox

# Rebuild if needed
cd sandbox && docker build -t mymanus-sandbox:latest .
```

### Permission Errors

**Problem**: File operations fail with permission denied
**Solution**: Ensure container runs as `ubuntu` user, not root

### Memory/CPU Limits

**Problem**: Code fails with resource errors
**Solution**: Adjust limits in application.properties
```properties
sandbox.docker.memory-limit=1073741824  # 1GB
sandbox.docker.cpu-quota=100000         # 100% CPU
```

### Network Issues

**Problem**: Code needs internet access
**Solution**: Change network mode
```properties
sandbox.docker.network-mode=bridge
```

## Best Practices

1. **Always use Docker mode in production** for security
2. **Test with host mode** for faster development
3. **Clean up containers** when sessions end
4. **Monitor container count** to prevent resource exhaustion
5. **Set appropriate resource limits** based on expected workload
6. **Use workspace sandboxing** for all file operations
7. **Log execution times** for performance monitoring
8. **Handle timeouts gracefully** in application code

## Performance

### Container Caching

- First execution: ~500-1000ms (container creation)
- Subsequent executions: ~100-200ms (reuses container)

### Optimization Tips

1. Reuse containers per session (already implemented)
2. Pre-warm containers for common operations
3. Limit variable state size
4. Use efficient serialization
5. Set reasonable timeout values
