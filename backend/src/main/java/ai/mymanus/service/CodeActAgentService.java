package ai.mymanus.service;

import ai.mymanus.dto.AgentEvent;
import ai.mymanus.model.Message;
import ai.mymanus.service.sandbox.ExecutionResult;
import ai.mymanus.service.sandbox.PythonSandboxExecutor;
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
 * Orchestrates the agent loop: LLM generation → Code execution → Observation → Repeat
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeActAgentService {

    private final AnthropicService anthropicService;
    private final PythonSandboxExecutor sandboxExecutor;
    private final PromptBuilder promptBuilder;
    private final AgentStateService stateService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${agent.max-iterations:20}")
    private Integer maxIterations;

    /**
     * Main agent loop: processes user query and executes CodeAct loop
     */
    public String processQuery(String sessionId, String userQuery) {
        log.info("Processing query for session: {}", sessionId);

        try {
            // Ensure session exists
            stateService.getOrCreateSession(sessionId);

            // Add user message to history
            stateService.addMessage(sessionId, Message.MessageRole.USER, userQuery);

            // Send status update
            sendEvent(sessionId, "status", "thinking", Map.of("iteration", 0));

            // Execute agent loop
            String finalResponse = executeAgentLoop(sessionId, userQuery);

            // Add assistant response to history
            stateService.addMessage(sessionId, Message.MessageRole.ASSISTANT, finalResponse);

            // Send completion event
            sendEvent(sessionId, "status", "done", Map.of("message", "Task completed"));

            return finalResponse;

        } catch (Exception e) {
            log.error("Error processing query", e);
            sendEvent(sessionId, "error", e.getMessage(), null);
            throw new RuntimeException("Failed to process query: " + e.getMessage(), e);
        }
    }

    /**
     * Execute the CodeAct agent loop with iterations
     */
    private String executeAgentLoop(String sessionId, String userQuery) {
        StringBuilder fullResponse = new StringBuilder();
        Map<String, Object> executionContext = stateService.getExecutionContext(sessionId);
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;
            log.info("Agent iteration {}/{}", iteration, maxIterations);

            sendEvent(sessionId, "status", "thinking",
                    Map.of("iteration", iteration, "maxIterations", maxIterations));

            // Build system prompt with current context
            String systemPrompt = promptBuilder.buildSystemPrompt(executionContext, false);

            // Generate LLM response (ChatClient uses conversation history from JDBC memory)
            String llmResponse = anthropicService.generate(sessionId, systemPrompt, userQuery);
            fullResponse.append(llmResponse).append("\n\n");

            log.info("LLM Response (iteration {}): {}", iteration, llmResponse.substring(0, Math.min(200, llmResponse.length())));

            // Send thinking update
            sendEvent(sessionId, "thought", llmResponse, Map.of("iteration", iteration));

            // Extract code blocks from response
            List<String> codeBlocks = promptBuilder.extractCodeBlocks(llmResponse);

            if (codeBlocks.isEmpty()) {
                // No code to execute, task is complete
                log.info("No code blocks found, task complete");
                break;
            }

            // Execute each code block
            for (int i = 0; i < codeBlocks.size(); i++) {
                String code = codeBlocks.get(i);
                log.info("Executing code block {}/{}", i + 1, codeBlocks.size());

                // Send code event
                sendEvent(sessionId, "code", code,
                        Map.of("iteration", iteration, "block", i + 1, "total", codeBlocks.size()));

                // Execute code in sandbox
                sendEvent(sessionId, "status", "executing",
                        Map.of("iteration", iteration, "block", i + 1));

                ExecutionResult result = sandboxExecutor.execute(code, executionContext);

                // Update execution context with new variables
                if (result.getVariables() != null) {
                    executionContext.putAll(result.getVariables());
                    stateService.updateExecutionContext(sessionId, executionContext);
                }

                // Send execution result
                Map<String, Object> resultMeta = new HashMap<>();
                resultMeta.put("success", result.isSuccess());
                resultMeta.put("executionTime", result.getExecutionTimeMs());
                resultMeta.put("iteration", iteration);

                sendEvent(sessionId, "output", result.getStdout(), resultMeta);

                if (!result.getStderr().isEmpty()) {
                    sendEvent(sessionId, "error", result.getStderr(), resultMeta);
                }

                // Build observation for next iteration
                String observation = promptBuilder.buildObservation(
                        result.getStdout(),
                        result.getStderr(),
                        result.isSuccess()
                );

                // If execution failed, provide feedback and continue
                if (!result.isSuccess()) {
                    userQuery = observation;
                    break;  // Try again with error feedback
                }
            }

            // Check if we should continue or if task is complete
            if (isTaskComplete(llmResponse)) {
                log.info("Task determined to be complete");
                break;
            }

            // Prepare for next iteration with observation
            userQuery = "Please continue with the next step.";
        }

        if (iteration >= maxIterations) {
            String msg = "Reached maximum iterations (" + maxIterations + "). Task may be incomplete.";
            fullResponse.append("\n\n").append(msg);
            sendEvent(sessionId, "warning", msg, null);
        }

        return fullResponse.toString().trim();
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
     * Clear session state
     */
    public void clearSession(String sessionId) {
        log.info("Clearing session: {}", sessionId);
        stateService.deleteSession(sessionId);
        stateService.createSession(sessionId);
    }
}
