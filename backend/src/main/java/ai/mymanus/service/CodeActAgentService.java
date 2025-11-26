package ai.mymanus.service;

import ai.mymanus.dto.AgentEvent;
import ai.mymanus.model.Message;
import ai.mymanus.service.sandbox.ExecutionResult;
import ai.mymanus.service.sandbox.SandboxExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main CodeAct Agent Service.
 * Orchestrates the agent loop using Event Stream Architecture:
 * [UserMessage → AgentThought → AgentAction → Observation → ...]
 *
 * Key Pattern: ONE action per iteration (Manus AI design)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeActAgentService {

    private final AnthropicService anthropicService;
    private final SandboxExecutor sandboxExecutor;
    private final PromptBuilder promptBuilder;
    private final AgentStateService stateService;
    private final EventService eventService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${agent.max-iterations:20}")
    private Integer maxIterations;

    /**
     * Main agent loop: processes user query and executes CodeAct loop
     * Uses Event Stream Architecture for proper state tracking
     */
    public String processQuery(String sessionId, String userQuery) {
        log.info("═══ Starting CodeAct Agent Loop for session: {} ═══", sessionId);

        try {
            // Ensure session exists
            stateService.getOrCreateSession(sessionId);

            // Add user message to history (legacy support)
            stateService.addMessage(sessionId, Message.MessageRole.USER, userQuery);

            // Auto-generate title from first message
            stateService.autoGenerateTitle(sessionId, userQuery);

            // **Event Stream: Append USER_MESSAGE**
            eventService.appendUserMessage(sessionId, userQuery, 0);
            log.info("📨 USER_MESSAGE appended to event stream");

            // Send status update to frontend
            sendEvent(sessionId, "status", "thinking", Map.of("iteration", 0));

            // Execute agent loop with event stream
            String finalResponse = executeAgentLoop(sessionId);

            // **Event Stream: Append AGENT_RESPONSE**
            // Note: Response is built from the event stream, not returned directly
            log.info("✅ Agent loop completed successfully");

            // Add assistant response to history (legacy support)
            stateService.addMessage(sessionId, Message.MessageRole.ASSISTANT, finalResponse);

            // Send completion event
            sendEvent(sessionId, "status", "done", Map.of("message", "Task completed"));

            log.info("═══ CodeAct Agent Loop Complete ═══");
            return finalResponse;

        } catch (Exception e) {
            log.error("❌ Error processing query for session: {}", sessionId, e);
            eventService.appendError(sessionId, e.getMessage(),
                Map.of("stackTrace", e.getStackTrace()[0].toString()), 0);
            sendEvent(sessionId, "error", e.getMessage(), null);
            throw new RuntimeException("Failed to process query: " + e.getMessage(), e);
        }
    }

    /**
     * Execute the CodeAct agent loop with Event Stream Architecture
     * CRITICAL: ONE action per iteration (Manus AI pattern)
     */
    private String executeAgentLoop(String sessionId) {
        StringBuilder fullResponse = new StringBuilder();
        Map<String, Object> executionContext = stateService.getExecutionContext(sessionId);
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;
            log.info("🔄 Iteration {}/{} starting", iteration, maxIterations);

            sendEvent(sessionId, "status", "thinking",
                    Map.of("iteration", iteration, "maxIterations", maxIterations));

            // **Build context from event stream** (includes all previous events)
            String eventStreamContext = eventService.buildEventStreamContext(sessionId);
            String systemPrompt = promptBuilder.buildSystemPrompt(executionContext, false);

            // Combine system prompt with event stream history
            String fullContext = systemPrompt + "\n\n" + eventStreamContext;

            // Generate LLM response
            String llmResponse = anthropicService.generate(sessionId, fullContext, "");
            log.info("🤔 LLM Response: {}", llmResponse.substring(0, Math.min(200, llmResponse.length())) + "...");

            // **Event Stream: Append AGENT_THOUGHT**
            eventService.appendAgentThought(sessionId, llmResponse, iteration);

            fullResponse.append(llmResponse).append("\n\n");

            // Send thinking update to frontend
            sendEvent(sessionId, "thought", llmResponse, Map.of("iteration", iteration));

            // Extract code blocks from response
            List<String> codeBlocks = promptBuilder.extractCodeBlocks(llmResponse);

            if (codeBlocks.isEmpty()) {
                // No code to execute, task is complete
                log.info("✅ No code blocks found - task complete");

                // **Event Stream: Append final AGENT_RESPONSE**
                eventService.appendAgentResponse(sessionId, llmResponse, iteration);
                break;
            }

            // **CRITICAL: Execute ONLY the FIRST code block (ONE action per iteration)**
            String code = codeBlocks.get(0);
            if (codeBlocks.size() > 1) {
                log.warn("⚠️ Multiple code blocks detected ({}) but executing only FIRST one (Manus AI pattern)",
                    codeBlocks.size());
            }

            log.info("▶️ Executing action (code block)");

            // **Event Stream: Append AGENT_ACTION**
            Map<String, Object> actionData = new HashMap<>();
            actionData.put("codeLength", code.length());
            actionData.put("codePreview", code.substring(0, Math.min(100, code.length())));
            eventService.appendAgentAction(sessionId, "execute_code", code, actionData, iteration);

            // Send code event to frontend
            sendEvent(sessionId, "code", code, Map.of("iteration", iteration));

            // Execute code in sandbox
            sendEvent(sessionId, "status", "executing", Map.of("iteration", iteration));

            long startTime = System.currentTimeMillis();
            ExecutionResult result = sandboxExecutor.execute(sessionId, code, executionContext);
            long duration = System.currentTimeMillis() - startTime;

            // Update execution context with new variables
            if (result.getVariables() != null) {
                executionContext.putAll(result.getVariables());
                stateService.updateExecutionContext(sessionId, executionContext);
            }

            // Build observation
            String observation = buildObservation(result);

            // **Event Stream: Append OBSERVATION**
            Map<String, Object> observationData = new HashMap<>();
            observationData.put("stdout", result.getStdout());
            observationData.put("stderr", result.getStderr());
            observationData.put("exitCode", result.getExitCode());
            observationData.put("variables", result.getVariables());

            eventService.appendObservation(
                sessionId,
                observation,
                observationData,
                result.isSuccess(),
                result.getError(),
                duration,
                iteration
            );

            log.info("📊 OBSERVATION recorded: success={}, duration={}ms", result.isSuccess(), duration);

            // Send execution result to frontend
            Map<String, Object> resultMeta = new HashMap<>();
            resultMeta.put("success", result.isSuccess());
            resultMeta.put("executionTime", duration);
            resultMeta.put("iteration", iteration);

            sendEvent(sessionId, "output", result.getStdout(), resultMeta);

            if (!result.getStderr().isEmpty()) {
                sendEvent(sessionId, "error", result.getStderr(), resultMeta);
            }

            // If execution failed, the observation contains the error
            // LLM will see it in the event stream and can retry/fix in next iteration

            if (!result.isSuccess()) {
                log.warn("⚠️ Action failed - LLM will see error in next iteration");
            }

            // Check if we should continue
            if (isTaskComplete(llmResponse)) {
                log.info("✅ Task complete indicator found in response");
                eventService.appendAgentResponse(sessionId, llmResponse, iteration);
                break;
            }

            // Continue to next iteration (LLM will see full event stream)
            log.info("➡️ Proceeding to next iteration");
        }

        if (iteration >= maxIterations) {
            String msg = "⚠️ Reached maximum iterations (" + maxIterations + "). Task may be incomplete.";
            log.warn(msg);
            fullResponse.append("\n\n").append(msg);
            sendEvent(sessionId, "warning", msg, null);
            eventService.appendSystem(sessionId, msg, iteration);
        }

        return fullResponse.toString().trim();
    }

    /**
     * Build observation string from execution result
     */
    private String buildObservation(ExecutionResult result) {
        StringBuilder obs = new StringBuilder();

        if (result.isSuccess()) {
            obs.append("Execution successful.\n");
            if (!result.getStdout().isEmpty()) {
                obs.append("Output:\n").append(result.getStdout()).append("\n");
            }
            if (result.getVariables() != null && !result.getVariables().isEmpty()) {
                obs.append("Variables updated: ").append(result.getVariables().keySet()).append("\n");
            }
        } else {
            obs.append("Execution failed.\n");
            if (!result.getStderr().isEmpty()) {
                obs.append("Error:\n").append(result.getStderr()).append("\n");
            }
            if (result.getError() != null) {
                obs.append("Details: ").append(result.getError()).append("\n");
            }
        }

        return obs.toString();
    }

    /**
     * Determine if the task is complete based on LLM response
     */
    private boolean isTaskComplete(String response) {
        String lower = response.toLowerCase();
        return lower.contains("task complete") ||
                lower.contains("task is complete") ||
                lower.contains("finished") ||
                lower.contains("done") ||
                (lower.contains("final") && lower.contains("answer"));
    }

    /**
     * Send WebSocket event to frontend
     */
    private void sendEvent(String sessionId, String type, String content, Map<String, Object> metadata) {
        try {
            AgentEvent event = AgentEvent.builder()
                    .type(type)
                    .content(content)
                    .metadata(metadata)
                    .build();

            messagingTemplate.convertAndSend("/topic/agent/" + sessionId, event);
            log.debug("Sent {} event to session {}", type, sessionId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket event", e);
        }
    }

    /**
     * Get session status
     */
    public Map<String, Object> getSessionStatus(String sessionId) {
        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", sessionId);

        try {
            stateService.getSession(sessionId);
            List<Message> messages = stateService.getMessages(sessionId);

            status.put("exists", true);
            status.put("messageCount", messages.size());
            status.put("context", stateService.getExecutionContext(sessionId).keySet());
            status.put("metadata", stateService.getMetadata(sessionId));
        } catch (Exception e) {
            status.put("exists", false);
        }

        return status;
    }

    /**
     * Clear session state and destroy sandbox container
     * Includes event stream cleanup
     */
    public void clearSession(String sessionId) {
        log.info("🗑️ Clearing session: {}", sessionId);

        // Destroy sandbox container for this session
        sandboxExecutor.destroySessionContainer(sessionId);

        // Clear event stream
        eventService.clearEventStream(sessionId);
        log.info("📝 Event stream cleared for session: {}", sessionId);

        // Clear database state (messages, tool executions, etc.)
        stateService.deleteSession(sessionId);
        stateService.createSession(sessionId);

        log.info("✅ Session cleared successfully: {}", sessionId);
    }
}
