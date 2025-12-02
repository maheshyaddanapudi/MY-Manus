package ai.mymanus.service.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock sandbox executor for integration tests.
 * Activated when sandbox.mode=mock
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sandbox.mode", havingValue = "mock")
public class MockSandboxExecutor implements SandboxExecutor {

    @Override
    public ExecutionResult execute(String sessionId, String code, Map<String, Object> context) {
        log.info("MockSandboxExecutor: Executing code for session: {}", sessionId);
        log.debug("Code: {}", code);
        
        // Simulate successful execution
        Map<String, Object> newVariables = new HashMap<>(context);
        
        // Parse simple calculations from code
        if (code.contains("2 + 2") || code.contains("2+2")) {
            newVariables.put("result", 4);
            return ExecutionResult.builder()
                .stdout("4\n")
                .stderr("")
                .exitCode(0)
                .success(true)
                .variables(newVariables)
                .executionTimeMs(10L)
                .build();
        }
        
        // Default mock response
        return ExecutionResult.builder()
            .stdout("Mock execution successful\n")
            .stderr("")
            .exitCode(0)
            .success(true)
            .variables(newVariables)
            .executionTimeMs(10L)
            .build();
    }

    @Override
    public void destroySessionContainer(String sessionId) {
        log.info("MockSandboxExecutor: Cleanup for session: {}", sessionId);
    }

    @Override
    public void cleanupAllContainers() {
        log.info("MockSandboxExecutor: Cleanup all sessions");
    }

    @Override
    public Map<String, Object> getContainerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeContainers", 0);
        stats.put("totalExecutions", 0);
        return stats;
    }
}
