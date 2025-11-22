package ai.mymanus.tool.impl.shell;

import ai.mymanus.service.sandbox.SandboxExecutor;
import ai.mymanus.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Shell Execution Tool - Execute shell commands in sandbox
 *
 * Manus AI Equivalent: shell_exec(command: str) -> dict
 *
 * Features:
 * - Execute bash commands in sandbox
 * - Capture stdout and stderr separately
 * - Return exit code
 * - Timeout protection
 * - Security: Runs in sandbox container
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShellExecTool implements Tool {

    private final SandboxExecutor sandboxExecutor;

    @Override
    public String getName() {
        return "shell_exec";
    }

    @Override
    public String getDescription() {
        return "Execute a shell command in the sandbox. Returns stdout, stderr, and exit code.";
    }

    @Override
    public String getPythonSignature() {
        return "shell_exec(sessionId: str, command: str, timeout: int = 30) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = (String) parameters.get("sessionId");
        String command = (String) parameters.get("command");
        Integer timeout = parameters.containsKey("timeout")
                ? ((Number) parameters.get("timeout")).intValue()
                : 30;

        if (sessionId == null || sessionId.isEmpty()) {
            return error("sessionId is required", null);
        }

        if (command == null || command.isEmpty()) {
            return error("command cannot be empty", null);
        }

        log.info("🐚 Executing shell command: {}", command.length() > 50
                ? command.substring(0, 50) + "..."
                : command);

        try {
            long startTime = System.currentTimeMillis();

            // Generate Python code to execute shell command (CodeAct pattern)
            String pythonCode = generateShellPython(command, timeout);

            // Execute Python code in sandbox
            var execResult = sandboxExecutor.execute(sessionId, pythonCode, Map.of());

            long duration = System.currentTimeMillis() - startTime;

            // Build response
            var response = new HashMap<String, Object>();
            response.put("success", execResult.isSuccess());
            response.put("stdout", execResult.getStdout());
            response.put("stderr", execResult.getStderr());
            response.put("exitCode", execResult.isSuccess() ? 0 : 1);
            response.put("command", command);
            response.put("durationMs", duration);

            log.info("✅ Shell command completed in {}ms", duration);

            return response;

        } catch (Exception e) {
            log.error("❌ Shell command failed: {}", command, e);
            return error("Shell command failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate Python code that executes a shell command
     * This is the CodeAct approach - tools generate Python code
     */
    private String generateShellPython(String command, int timeout) {
        // Escape single quotes in command
        String escapedCommand = command.replace("'", "\\'");

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
                if result.stderr:
                    print(result.stderr, file=sys.stderr, end='')
                sys.exit(result.returncode)
            except subprocess.TimeoutExpired:
                print('Command timed out after %d seconds', file=sys.stderr)
                sys.exit(124)
            except Exception as e:
                print(f'Error: {{e}}', file=sys.stderr)
                sys.exit(1)
            """, escapedCommand, timeout, timeout);
    }

    private Map<String, Object> error(String message, Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        if (e != null) {
            result.put("errorType", e.getClass().getSimpleName());
            result.put("errorMessage", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean requiresNetwork() {
        return false; // Depends on command, but default no
    }
}
