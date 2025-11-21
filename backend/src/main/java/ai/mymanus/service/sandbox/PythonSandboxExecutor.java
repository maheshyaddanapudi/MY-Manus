package ai.mymanus.service.sandbox;

import ai.mymanus.tool.Tool;
import ai.mymanus.tool.ToolRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Executes Python code in isolated Docker containers.
 * Implements the CodeAct architecture with state persistence.
 */
@Slf4j
@Service
public class PythonSandboxExecutor {

    private final DockerClient dockerClient;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    @Value("${docker.sandbox.image:mymanus-sandbox:latest}")
    private String sandboxImage;

    @Value("${docker.sandbox.memory-limit:536870912}")
    private Long memoryLimit;

    @Value("${docker.sandbox.cpu-quota:50000}")
    private Long cpuQuota;

    @Value("${docker.sandbox.timeout-seconds:30}")
    private Integer timeoutSeconds;

    @Value("${docker.sandbox.network-mode:none}")
    private String networkMode;

    public PythonSandboxExecutor(DockerClient dockerClient,
                                  ToolRegistry toolRegistry,
                                  ObjectMapper objectMapper) {
        this.dockerClient = dockerClient;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute Python code with state persistence
     */
    public ExecutionResult execute(String code, Map<String, Object> previousState) {
        long startTime = System.currentTimeMillis();
        String containerId = null;

        try {
            // Create container with resource limits
            containerId = createContainer();
            log.info("Created container: {}", containerId);

            // Start container
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Started container: {}", containerId);

            // Prepare Python code with state restoration and tool bindings
            String fullCode = buildExecutionScript(code, previousState);

            // Write code to container
            writeCodeToContainer(containerId, fullCode);

            // Execute Python code
            ExecutionResult result = executeInContainer(containerId);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            log.error("Execution error", e);
            return ExecutionResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } finally {
            // Cleanup container
            if (containerId != null) {
                cleanup(containerId);
            }
        }
    }

    private String createContainer() {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memoryLimit)
                .withCpuQuota(cpuQuota)
                .withNetworkMode(networkMode)
                .withAutoRemove(false);

        CreateContainerResponse container = dockerClient.createContainerCmd(sandboxImage)
                .withHostConfig(hostConfig)
                .withUser("ubuntu")
                .withWorkingDir("/home/ubuntu/workspace")
                .withCmd("/bin/bash", "-c", "sleep infinity")
                .exec();

        return container.getId();
    }

    private String buildExecutionScript(String userCode, Map<String, Object> previousState) {
        StringBuilder script = new StringBuilder();

        // Add imports
        script.append("import json\n");
        script.append("import sys\n");
        script.append("import traceback\n\n");

        // Tool execution function
        script.append("# Tool execution bridge\n");
        script.append("def _execute_tool(tool_name, params):\n");
        script.append("    # In real implementation, this calls back to Java\n");
        script.append("    print(f'TOOL_CALL:{tool_name}:{json.dumps(params)}')\n");
        script.append("    return {'status': 'pending'}\n\n");

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

    private void writeCodeToContainer(String containerId, String code) throws Exception {
        // Create a temporary file with the code
        File tempFile = File.createTempFile("code", ".py");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(code);
        }

        // Copy file to container
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(tempFile.getAbsolutePath())
                .withRemotePath("/home/ubuntu/workspace/code.py")
                .exec();

        tempFile.delete();
    }

    private ExecutionResult executeInContainer(String containerId) throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient
                .execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("python3.11", "/home/ubuntu/workspace/code.py")
                .exec();

        boolean completed = dockerClient
                .execStartCmd(execCreateCmdResponse.getId())
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
                            log.error("Error capturing output", e);
                        }
                    }
                })
                .awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            return ExecutionResult.builder()
                    .success(false)
                    .error("Execution timeout after " + timeoutSeconds + " seconds")
                    .build();
        }

        String stdoutStr = stdout.toString(StandardCharsets.UTF_8);
        String stderrStr = stderr.toString(StandardCharsets.UTF_8);

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

    private void cleanup(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(5)
                    .exec();
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
            log.info("Cleaned up container: {}", containerId);
        } catch (Exception e) {
            log.error("Error cleaning up container: {}", containerId, e);
        }
    }
}
