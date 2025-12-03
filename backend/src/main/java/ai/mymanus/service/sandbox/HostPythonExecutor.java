package ai.mymanus.service.sandbox;

import ai.mymanus.tool.ToolRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Executes Python code directly on the host machine (no Docker).
 * Useful for development to avoid Docker overhead.
 *
 * WARNING: This is NOT secure for production use!
 * Only enable in trusted development environments.
 *
 * This implementation is active when sandbox.mode=host.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sandbox.mode", havingValue = "host")
public class HostPythonExecutor implements SandboxExecutor {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final ToolRpcHandler rpcHandler;

    @Value("${sandbox.host.python-executable:python3}")
    private String pythonExecutable;

    @Value("${sandbox.host.workspace-dir:/tmp/manus-workspace}")
    private String workspaceDir;

    @Value("${sandbox.host.timeout-seconds:30}")
    private Integer timeoutSeconds;

    public HostPythonExecutor(ToolRegistry toolRegistry, ObjectMapper objectMapper, ToolRpcHandler rpcHandler) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.rpcHandler = rpcHandler;
    }

    /**
     * Execute Python code directly on the host machine.
     *
     * @param sessionId Session ID (used for workspace isolation)
     * @param code Python code to execute
     * @param previousState Previous execution context
     * @return Execution result
     */
    @Override
    public ExecutionResult execute(String sessionId, String code, Map<String, Object> previousState) {
        long startTime = System.currentTimeMillis();

        try {
            // Create session-specific workspace
            Path sessionWorkspace = createSessionWorkspace(sessionId);
            log.debug("Using workspace: {}", sessionWorkspace);

            // Build execution script
            String fullCode = buildExecutionScript(sessionId, code, previousState);

            // Write code to file
            Path codeFile = sessionWorkspace.resolve("code.py");
            Files.writeString(codeFile, fullCode);

            // Execute Python process
            ExecutionResult result = executeProcess(sessionWorkspace, codeFile);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            log.info("Executed code in session {} (host mode) in {}ms", sessionId, result.getExecutionTimeMs());
            return result;

        } catch (Exception e) {
            log.error("Host execution error in session {}", sessionId, e);
            return ExecutionResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Create or get workspace directory for session
     */
    private Path createSessionWorkspace(String sessionId) throws Exception {
        Path workspace = Paths.get(workspaceDir, sessionId);
        if (!Files.exists(workspace)) {
            Files.createDirectories(workspace);
            log.info("Created workspace for session {}: {}", sessionId, workspace);
        }
        return workspace;
    }

    /**
     * Build Python script with state restoration and tool bindings
     */
    private String buildExecutionScript(String sessionId, String userCode, Map<String, Object> previousState) {
        StringBuilder script = new StringBuilder();

        // Add imports
        script.append("import json\n");
        script.append("import sys\n");
        script.append("import os\n");
        script.append("import uuid\n");
        script.append("import traceback\n\n");
        
        // Inject sessionId as a global variable
        script.append("# Session context (automatically injected)\n");
        script.append(String.format("SESSION_ID = '%s'\n\n", sessionId));
        
        // Debug: Print current working directory
        script.append("# Debug: Current working directory\n");
        script.append("print(f'[DEBUG] CWD: {os.getcwd()}')\n\n");

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

    /**
     * Execute Python process and capture output with RPC tool execution support
     */
    private ExecutionResult executeProcess(Path workspace, Path codeFile) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                codeFile.toString()
        );

        // Set working directory
        processBuilder.directory(workspace.toFile());

        // Redirect error stream
        processBuilder.redirectErrorStream(false);

        // Start process
        Process process = processBuilder.start();

        // Capture stdout
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        // Get stdin writer for sending tool responses
        java.io.PrintWriter stdinWriter = new java.io.PrintWriter(process.getOutputStream(), true);

        // Read stdout with tool request handling
        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Check for tool request
                    if (line.contains("__TOOL_REQUEST__")) {
                        handleToolRequest(line, stdinWriter);
                    } else {
                        stdout.append(line).append("\n");
                    }
                }
            } catch (Exception e) {
                log.error("Error reading stdout", e);
            }
        });

        // Read stderr
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            } catch (Exception e) {
                log.error("Error reading stderr", e);
            }
        });

        stdoutThread.start();
        stderrThread.start();

        // Wait for process with timeout
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            return ExecutionResult.builder()
                    .success(false)
                    .error("Execution timeout after " + timeoutSeconds + " seconds")
                    .build();
        }

        // Wait for output threads
        stdoutThread.join(1000);
        stderrThread.join(1000);

        String stdoutStr = stdout.toString();
        String stderrStr = stderr.toString();

        // Parse state from output
        Map<String, Object> variables = parseStateFromOutput(stdoutStr);

        // Remove state marker from stdout
        stdoutStr = stdoutStr.replaceAll("STATE:.*\n?", "").trim();

        return ExecutionResult.builder()
                .success(stderrStr.isEmpty() || !stderrStr.contains("ERROR:"))
                .stdout(stdoutStr)
                .stderr(stderrStr)
                .variables(variables)
                .exitCode(process.exitValue())
                .build();
    }

    /**
     * Handle tool execution request from Python (delegates to shared RPC handler)
     */
    private void handleToolRequest(String line, java.io.PrintWriter stdinWriter) {
        try {
            // Delegate to shared RPC handler
            String response = rpcHandler.handleToolRequest(line);
            
            // Send response to Python
            stdinWriter.println(response);
            stdinWriter.flush();
            
        } catch (Exception e) {
            log.error("❌ Error handling tool request", e);
        }
    }

    /**
     * Parse Python state from output
     */
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
     * Clean up workspace for a session
     */
    @Override
    public void destroySessionContainer(String sessionId) {
        try {
            Path workspace = Paths.get(workspaceDir, sessionId);
            if (Files.exists(workspace)) {
                deleteDirectory(workspace.toFile());
                log.info("Cleaned up workspace for session {}", sessionId);
            }
        } catch (Exception e) {
            log.warn("Error cleaning up workspace for session {}", sessionId, e);
        }
    }

    /**
     * Clean up all session workspaces on application shutdown
     */
    @Override
    @PreDestroy
    public void cleanupAllContainers() {
        try {
            Path workspace = Paths.get(workspaceDir);
            if (Files.exists(workspace)) {
                long sessionCount = Files.list(workspace)
                        .filter(Files::isDirectory)
                        .count();

                log.info("Cleaning up {} session workspaces on shutdown", sessionCount);

                Files.list(workspace)
                        .filter(Files::isDirectory)
                        .forEach(sessionPath -> {
                            try {
                                deleteDirectory(sessionPath.toFile());
                                log.info("Cleaned up workspace: {}", sessionPath.getFileName());
                            } catch (Exception e) {
                                log.warn("Error cleaning up workspace: {}", sessionPath.getFileName(), e);
                            }
                        });
            }
        } catch (Exception e) {
            log.error("Error during cleanup of all workspaces", e);
        }
    }

    /**
     * Recursively delete directory
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Get statistics about host execution
     */
    @Override
    public Map<String, Object> getContainerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("mode", "host");
        stats.put("pythonExecutable", pythonExecutable);
        stats.put("workspaceDir", workspaceDir);
        stats.put("timeoutSeconds", timeoutSeconds);

        // Count active session workspaces
        try {
            Path workspace = Paths.get(workspaceDir);
            if (Files.exists(workspace)) {
                long sessionCount = Files.list(workspace)
                        .filter(Files::isDirectory)
                        .count();
                stats.put("activeSessions", sessionCount);
            } else {
                stats.put("activeSessions", 0);
            }
        } catch (Exception e) {
            stats.put("activeSessions", "unknown");
        }

        return stats;
    }
}
