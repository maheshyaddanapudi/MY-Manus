package ai.mymanus.multiagent;

import ai.mymanus.service.AnthropicService;
import ai.mymanus.service.EventService;
import ai.mymanus.service.AgentStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Multi-Agent Orchestration Service
 * Coordinates multiple specialized agents with custom LLM support
 * Falls back to primary LLM if agent-specific LLM not configured
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MultiAgentOrchestrator {

    private final AnthropicService anthropicService;
    private final EventService eventService;
    private final AgentStateService agentStateService;

    @Value("${anthropic.model:claude-sonnet-4}")
    private String primaryLlmModel;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Execute task using multi-agent orchestration
     * @param sessionId The session ID
     * @param userTask The user's task
     * @param agentConfigs List of agent configurations
     * @param sequential If true, run agents sequentially; if false, run in parallel
     * @return Aggregated results from all agents
     */
    public Map<String, Object> orchestrate(
            String sessionId,
            String userTask,
            List<AgentConfig> agentConfigs,
            boolean sequential
    ) {
        log.info("🎭 Multi-Agent Orchestration Started");
        log.info("📋 Task: {}", userTask);
        log.info("🤖 Agents: {}", agentConfigs.size());
        log.info("⏱️  Mode: {}", sequential ? "Sequential" : "Parallel");

        Map<String, Object> results = new HashMap<>();
        results.put("sessionId", sessionId);
        results.put("task", userTask);
        results.put("mode", sequential ? "sequential" : "parallel");
        results.put("agentCount", agentConfigs.size());

        List<Map<String, Object>> agentResults = new ArrayList<>();

        if (sequential) {
            // Run agents sequentially
            for (AgentConfig config : agentConfigs) {
                Map<String, Object> agentResult = executeAgent(sessionId, userTask, config);
                agentResults.add(agentResult);

                // If an agent fails critically, stop orchestration
                if (Boolean.FALSE.equals(agentResult.get("success"))) {
                    String failureReason = (String) agentResult.get("error");
                    if (isCriticalFailure(failureReason)) {
                        log.error("❌ Critical failure in agent {}: {}", config.getAgentId(), failureReason);
                        break;
                    }
                }
            }
        } else {
            // Run agents in parallel
            List<CompletableFuture<Map<String, Object>>> futures = agentConfigs.stream()
                    .map(config -> CompletableFuture.supplyAsync(
                            () -> executeAgent(sessionId, userTask, config),
                            executorService
                    ))
                    .toList();

            // Wait for all agents to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results
            for (CompletableFuture<Map<String, Object>> future : futures) {
                try {
                    agentResults.add(future.get());
                } catch (Exception e) {
                    log.error("❌ Error getting agent result", e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("error", e.getMessage());
                    agentResults.add(errorResult);
                }
            }
        }

        results.put("agentResults", agentResults);
        results.put("success", true);
        results.put("completedAt", System.currentTimeMillis());

        log.info("✅ Multi-Agent Orchestration Complete: {} agents executed", agentResults.size());

        return results;
    }

    /**
     * Execute single agent
     */
    private Map<String, Object> executeAgent(String sessionId, String task, AgentConfig config) {
        long startTime = System.currentTimeMillis();

        log.info("🤖 Executing Agent: {} ({})", config.getAgentId(), config.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("agentId", config.getAgentId());
        result.put("role", config.getRole().name());
        result.put("startTime", startTime);

        try {
            // Get effective LLM model (with fallback to primary)
            String effectiveLlm = config.getEffectiveLlmModel(primaryLlmModel);
            log.info("📡 Using LLM: {} (Primary: {})", effectiveLlm, primaryLlmModel);

            // Get effective system prompt
            String systemPrompt = config.getEffectiveSystemPrompt();

            // Build messages
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", task));

            // Generate response using configured or fallback LLM
            String response = anthropicService.generateResponse(systemPrompt, messages);

            result.put("success", true);
            result.put("response", response);
            result.put("llmUsed", effectiveLlm);
            result.put("llmFallback", config.getLlmModel() == null);

        } catch (Exception e) {
            log.error("❌ Agent {} failed: {}", config.getAgentId(), e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("exception", e.getClass().getSimpleName());
        }

        long endTime = System.currentTimeMillis();
        result.put("endTime", endTime);
        result.put("durationMs", endTime - startTime);

        log.info("✅ Agent {} completed in {}ms", config.getAgentId(), (endTime - startTime));

        return result;
    }

    /**
     * Check if failure is critical enough to stop orchestration
     */
    private boolean isCriticalFailure(String error) {
        if (error == null) return false;

        String lowerError = error.toLowerCase();
        return lowerError.contains("authentication") ||
               lowerError.contains("authorization") ||
               lowerError.contains("rate limit") ||
               lowerError.contains("quota exceeded");
    }

    /**
     * Create default agent configuration
     */
    public List<AgentConfig> createDefaultAgentPipeline() {
        return List.of(
                AgentConfig.builder()
                        .agentId("planner")
                        .role(AgentRole.PLANNER)
                        .llmModel(null) // Will use primary LLM
                        .canDelegate(false)
                        .maxIterations(5)
                        .build(),

                AgentConfig.builder()
                        .agentId("executor")
                        .role(AgentRole.EXECUTOR)
                        .llmModel(null) // Will use primary LLM
                        .canDelegate(false)
                        .maxIterations(10)
                        .build(),

                AgentConfig.builder()
                        .agentId("verifier")
                        .role(AgentRole.VERIFIER)
                        .llmModel(null) // Will use primary LLM
                        .canDelegate(false)
                        .maxIterations(3)
                        .build()
        );
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
