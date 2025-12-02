package ai.mymanus.service.sandbox;

import ai.mymanus.tool.ToolRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Executes Python code in session-based Docker containers.
 * Implements the CodeAct architecture with state persistence.
 *
 * IMPORTANT: Uses one container per session for efficiency.
 * Containers are cached and reused across multiple code executions.
 *
 * This implementation is active when sandbox.mode=docker (default).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sandbox.mode", havingValue = "docker", matchIfMissing = true)
public class PythonSandboxExecutor implements SandboxExecutor {

    private final DockerClient dockerClient;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final ToolRpcHandler rpcHandler;

    // Container cache: sessionId -> containerId
    private final Map<String, String> sessionContainers = new ConcurrentHashMap<>();

    @Value("${sandbox.docker.image:mymanus-sandbox:latest}")
    private String sandboxImage;

    @Value("${sandbox.docker.memory-limit:536870912}")
    private Long memoryLimit;

    @Value("${sandbox.docker.cpu-quota:50000}")
    private Long cpuQuota;

    @Value("${sandbox.docker.timeout-seconds:30}")
    private Integer timeoutSeconds;

    @Value("${sandbox.docker.network-mode:none}")
    private String networkMode;

    public PythonSandboxExecutor(DockerClient dockerClient,
                                  ToolRegistry toolRegistry,
                                  ObjectMapper objectMapper,
                                  ToolRpcHandler rpcHandler) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient cannot be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.rpcHandler = Objects.requireNonNull(rpcHandler, "rpcHandler cannot be null");
    }

    /**
     * Execute Python code in a session-specific sandbox container.
     * Reuses existing container if available for better performance.
     *
     * @param sessionId Session/conversation ID
     * @param code Python code to execute
     * @param previousState Previous execution context (variables)
     * @return Execution result with stdout, stderr, and new state
     */
    @Override
    public ExecutionResult execute(String sessionId, String code, Map<String, Object> previousState) {
        long startTime = System.currentTimeMillis();
        String containerId = null;

        try {
            // Get or create container for this session (cached!)
            containerId = getOrCreateContainer(sessionId);
            log.debug("Using container {} for session {}", containerId.substring(0, 12), sessionId);

            // Prepare Python code with state restoration and tool bindings
            String fullCode = buildExecutionScript(code, previousState);

            // Write code to container (session-specific workspace)
            writeCodeToContainer(containerId, sessionId, fullCode);

            // Execute Python code with RPC support
            ExecutionResult result = executeInContainer(containerId, sessionId);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            log.info("Executed code in session {} in {}ms", sessionId, result.getExecutionTimeMs());
            return result;

        } catch (Exception e) {
            log.error("Execution error in session {}", sessionId, e);

            // If container failed, remove it from cache so it gets recreated
            if (containerId != null) {
                sessionContainers.remove(sessionId);
                cleanup(containerId);
            }

            return ExecutionResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
        // Note: No finally block - container stays alive for reuse!
    }

    /**
     * Get existing container for session or create a new one.
     * Containers are cached and reused for efficiency.
     */
    private String getOrCreateContainer(String sessionId) {
        String existingContainerId = sessionContainers.get(sessionId);

        // Check if we have a cached container that's still running
        if (existingContainerId != null && isContainerRunning(existingContainerId)) {
            log.debug("Reusing existing container {} for session {}",
                    existingContainerId.substring(0, 12), sessionId);
            return existingContainerId;
        }

        // Create new container
        log.info("Creating new container for session {}", sessionId);
        String containerId = createContainer(sessionId);

        // Start container
        dockerClient.startContainerCmd(containerId).exec();
        log.info("Started container {} for session {}", containerId.substring(0, 12), sessionId);

        // Cache it
        sessionContainers.put(sessionId, containerId);

        return containerId;
    }

    /**
     * Check if container is still running
     */
    private boolean isContainerRunning(String containerId) {
        try {
            var container = dockerClient.inspectContainerCmd(containerId).exec();
            Boolean running = container.getState().getRunning();
            return running != null && running;
        } catch (Exception e) {
            log.debug("Container {} is not running: {}", containerId.substring(0, 12), e.getMessage());
            return false;
        }
    }

    /**
     * Create a new sandbox container for a session
     */
    private String createContainer(String sessionId) {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memoryLimit)
                .withCpuQuota(cpuQuota)
                .withNetworkMode(networkMode)
                .withAutoRemove(false);

        // Use FULL session ID in container name for easy identification and uniqueness
        String containerName = "manus-sandbox-" + sessionId;

        CreateContainerResponse container = dockerClient.createContainerCmd(sandboxImage)
                .withName(containerName)
                .withHostConfig(hostConfig)
                .withUser("ubuntu")
                .withWorkingDir("/home/ubuntu/workspace/" + sessionId)  // Session-specific workspace
                .withCmd("/bin/bash", "-c", "sleep infinity")  // Keep container alive
                .exec();

        return container.getId();
    }

    private String buildExecutionScript(String userCode, Map<String, Object> previousState) {
        StringBuilder script = new StringBuilder();

        // Add imports
        script.append("import json\n");
        script.append("import sys\n");
        script.append("import uuid\n");
        script.append("import traceback\n\n");

        // Tool execution function (RPC bridge to Java)
        script.append("# Tool execution bridge (RPC to Java)\n");
        script.append("def _execute_tool(tool_name, params):\n");
        script.append("    try:\n");
        script.append("        # Generate unique request ID\n");
        script.append("        request_id = str(uuid.uuid4())\n");
        script.append("        \n");
        script.append("        # Build request\n");
        script.append("        request = {\n");
        script.append("            'id': request_id,\n");
        script.append("            'tool': tool_name,\n");
        script.append("            'params': params\n");
        script.append("        }\n");
        script.append("        \n");
        script.append("        # Send request to Java via stdout\n");
        script.append("        request_json = json.dumps(request)\n");
        script.append("        print(f'__TOOL_REQUEST__{request_json}__END__', flush=True)\n");
        script.append("        \n");
        script.append("        # Read response from Java via stdin\n");
        script.append("        response_line = sys.stdin.readline().strip()\n");
        script.append("        \n");
        script.append("        # Parse response\n");
        script.append("        if '__TOOL_RESPONSE__' in response_line:\n");
        script.append("            response_json = response_line.split('__TOOL_RESPONSE__')[1].split('__END__')[0]\n");
        script.append("            response = json.loads(response_json)\n");
        script.append("            \n");
        script.append("            # Check for errors\n");
        script.append("            if response.get('error'):\n");
        script.append("                raise Exception(f\"Tool error: {response['error']}\")\n");
        script.append("            \n");
        script.append("            # Return result\n");
        script.append("            return response['result']\n");
        script.append("        else:\n");
        script.append("            raise Exception('Invalid tool response format')\n");
        script.append("    except Exception as e:\n");
        script.append("        print(f'ERROR: Tool execution failed: {str(e)}', file=sys.stderr)\n");
        script.append("        traceback.print_exc()\n");
        script.append("        return {'success': False, 'error': str(e)}\n\n");

        // Add tool function definitions
        script.append(toolRegistry.generatePythonBindings());
        script.append("\n");

        // Restore previous state
        if (previousState != null && !previousState.isEmpty()) {
            script.append("# Restore previous state\n");
            for (Map.Entry<String, Object> entry : previousState.entrySet()) {
                try {
                    String value = objectMapper.writeValueAsString(entry.getValue());
                    script.append(String.format("%s = json.loads('%s')\n",
                            entry.getKey(),
                            value.replace("'", "\\'")));
                } catch (Exception e) {
                    log.warn("Could not serialize variable: {}", entry.getKey());
                }
            }
            script.append("\n");
        }

        // User code wrapped in try-except
        script.append("# User code\n");
        script.append("try:\n");
        for (String line : userCode.split("\n")) {
            script.append("    ").append(line).append("\n");
        }
        script.append("\nexcept Exception as e:\n");
        script.append("    print(f'ERROR: {str(e)}', file=sys.stderr)\n");
        script.append("    traceback.print_exc()\n\n");

        // Capture final state
        script.append("# Capture state\n");
        script.append("_state = {k: v for k, v in globals().items() ");
        script.append("if not k.startswith('_') and k not in ['json', 'sys', 'traceback']}\n");
        script.append("print(f'STATE:{json.dumps(_state, default=str)}')\n");

        return script.toString();
    }

    private void writeCodeToContainer(String containerId, String sessionId, String code) throws Exception {
        // Create a temporary file with the code
        File tempFile = File.createTempFile("code", ".py");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(code);
        }

        // Copy file to session-specific workspace in container
        String remotePath = "/home/ubuntu/workspace/" + sessionId + "/code.py";
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(tempFile.getAbsolutePath())
                .withRemotePath(remotePath)
                .exec();

        tempFile.delete();
    }

    private ExecutionResult executeInContainer(String containerId, String sessionId) throws Exception {
        // Use shell-based docker exec for RPC support (stdin/stdout access)
        String codePath = "/home/ubuntu/workspace/" + sessionId + "/code.py";
        String workspaceDir = "/home/ubuntu/workspace/" + sessionId;
        
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", "-i",
                "-w", workspaceDir,
                containerId,
                "python3.11", codePath
        );

        Process process = pb.start();
        
        // Set up RPC communication
        PrintWriter stdinWriter = new PrintWriter(process.getOutputStream(), true);
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();
        
        // Read stdout in separate thread with RPC handling
        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("__TOOL_REQUEST__")) {
                        // Handle tool request via RPC handler
                        String response = rpcHandler.handleToolRequest(line);
                        stdinWriter.println(response);
                        stdinWriter.flush();
                    } else {
                        stdoutBuilder.append(line).append("\n");
                    }
                }
            } catch (Exception e) {
                log.error("Error reading stdout", e);
            }
        });
        stdoutThread.start();
        
        // Read stderr in separate thread
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrBuilder.append(line).append("\n");
                }
            } catch (Exception e) {
                log.error("Error reading stderr", e);
            }
        });
        stderrThread.start();
        
        // Wait for process to complete with timeout
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!completed) {
            process.destroyForcibly();
            return ExecutionResult.builder()
                    .success(false)
                    .error("Execution timeout after " + timeoutSeconds + " seconds")
                    .build();
        }
        
        // Wait for threads to finish reading
        stdoutThread.join(1000);
        stderrThread.join(1000);
        
        String stdoutStr = stdoutBuilder.toString();
        String stderrStr = stderrBuilder.toString();

        // Parse state from output
        Map<String, Object> variables = parseStateFromOutput(stdoutStr);

        // Remove state marker from stdout
        stdoutStr = stdoutStr.replaceAll("STATE:.*", "").trim();

        return ExecutionResult.builder()
                .success(stderrStr.isEmpty() || !stderrStr.contains("ERROR:"))
                .stdout(stdoutStr)
                .stderr(stderrStr)
                .variables(variables)
                .exitCode(0)
                .build();
    }

    private Map<String, Object> parseStateFromOutput(String output) {
        try {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.startsWith("STATE:")) {
                    String jsonState = line.substring(6);
                    return objectMapper.readValue(jsonState, new TypeReference<Map<String, Object>>() {});
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse state from output", e);
        }
        return new HashMap<>();
    }

    /**
     * Cleanup a specific container
     */
    private void cleanup(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(5)
                    .exec();
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
            log.info("Cleaned up container: {}", containerId.substring(0, 12));
        } catch (Exception e) {
            log.warn("Error cleaning up container {}: {}",
                    containerId.substring(0, 12), e.getMessage());
        }
    }

    /**
     * Destroy container for a specific session.
     * Called when user clears session or session expires.
     */
    @Override
    public void destroySessionContainer(String sessionId) {
        String containerId = sessionContainers.remove(sessionId);
        if (containerId != null) {
            log.info("Destroying container for session {}", sessionId);
            cleanup(containerId);
        } else {
            log.debug("No container found for session {}", sessionId);
        }
    }

    /**
     * Clean up all containers on application shutdown
     */
    @Override
    @PreDestroy
    public void cleanupAllContainers() {
        log.info("Cleaning up {} session containers on shutdown", sessionContainers.size());
        sessionContainers.forEach((sessionId, containerId) -> {
            log.info("Cleaning up container {} for session {}",
                    containerId.substring(0, 12), sessionId);
            cleanup(containerId);
        });
        sessionContainers.clear();
    }

    /**
     * Get statistics about active containers for monitoring
     */
    @Override
    public Map<String, Object> getContainerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalContainers", sessionContainers.size());
        stats.put("sessions", new ArrayList<>(sessionContainers.keySet()));

        // Get detailed container info
        List<Map<String, String>> containers = new ArrayList<>();
        sessionContainers.forEach((sessionId, containerId) -> {
            Map<String, String> containerInfo = new HashMap<>();
            containerInfo.put("sessionId", sessionId);
            containerInfo.put("containerId", containerId.substring(0, 12));
            containerInfo.put("running", String.valueOf(isContainerRunning(containerId)));
            containers.add(containerInfo);
        });
        stats.put("containers", containers);

        return stats;
    }
}
