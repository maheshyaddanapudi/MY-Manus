package ai.mymanus.service.sandbox;

import java.util.Map;

/**
 * Interface for sandbox code execution.
 * Implementations can use Docker containers or direct host execution.
 */
public interface SandboxExecutor {

    /**
     * Execute Python code in a sandbox environment.
     *
     * @param sessionId Session/conversation ID
     * @param code Python code to execute
     * @param previousState Previous execution context (variables)
     * @return Execution result with stdout, stderr, and new state
     */
    ExecutionResult execute(String sessionId, String code, Map<String, Object> previousState);

    /**
     * Clean up sandbox environment for a specific session.
     * Called when user clears session or session expires.
     *
     * @param sessionId Session ID to clean up
     */
    void destroySessionContainer(String sessionId);

    /**
     * Clean up all sandbox environments.
     * Called on application shutdown or manual cleanup.
     */
    void cleanupAllContainers();

    /**
     * Get statistics about active sandbox environments.
     *
     * @return Statistics map with environment-specific information
     */
    Map<String, Object> getContainerStats();
}
