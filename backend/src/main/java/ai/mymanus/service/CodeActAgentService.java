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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final PythonValidationService validationService;

    @Value("${agent.max-iterations:20}")
    private Integer maxIterations;

    @Value("${sandbox.host.workspace-dir}")
    private String workspaceDir;

    // Track sessions that should be stopped
    private final Set<String> stopRequests = ConcurrentHashMap.newKeySet();

    // Track currently running sessions
    private final Set<String> runningSessions = ConcurrentHashMap.newKeySet();

    /**
     * Request to stop the agent loop for a session.
     * The loop will stop at the next iteration boundary.
     *
     * @param sessionId Session to stop
     * @return true if session was running and stop was requested
     */
    public boolean stopAgent(String sessionId) {
        if (runningSessions.contains(sessionId)) {
            stopRequests.add(sessionId);
            log.info("🛑 Stop requested for session: {}", sessionId);
            sendEvent(sessionId, "status", "stopping", Map.of("message", "Stop requested"));
            return true;
        }
        log.warn("⚠️ Stop requested but session not running: {}", sessionId);
        return false;
    }

    /**
     * Check if a session is currently running
     */
    public boolean isRunning(String sessionId) {
        return runningSessions.contains(sessionId);
    }

    /**
     * Main agent loop: processes user query and executes CodeAct loop
     * Uses Event Stream Architecture for proper state tracking
     */
    public String processQuery(String sessionId, String userQuery) {
        log.info("═══ Starting CodeAct Agent Loop for session: {} ═══", sessionId);

        // Mark session as running
        runningSessions.add(sessionId);

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
            String finalResponse = executeAgentLoop(sessionId, userQuery);

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
        } finally {
            // Clean up running state
            runningSessions.remove(sessionId);
            stopRequests.remove(sessionId);
            log.debug("🧹 Cleaned up session state for: {}", sessionId);
        }
    }

    /**
     * Execute the CodeAct agent loop with Event Stream Architecture
     * CRITICAL: ONE action per iteration (Manus AI pattern)
     */
    private String executeAgentLoop(String sessionId, String userQuery) {
        StringBuilder fullResponse = new StringBuilder();
        Map<String, Object> executionContext = stateService.getExecutionContext(sessionId);
        int iteration = 0;

        while (iteration < maxIterations) {
            // Check for stop request at the beginning of each iteration
            if (stopRequests.contains(sessionId)) {
                log.info("🛑 Stop request detected - stopping agent loop at iteration {}", iteration);
                String stopMsg = "Agent stopped by user request at iteration " + iteration;
                eventService.appendSystem(sessionId, stopMsg, iteration);
                sendEvent(sessionId, "status", "stopped", Map.of("message", stopMsg, "iteration", iteration));
                fullResponse.append("\n\n").append(stopMsg);
                break;
            }

            iteration++;
            log.info("🔄 Iteration {}/{} starting", iteration, maxIterations);

            sendEvent(sessionId, "status", "thinking",
                    Map.of("iteration", iteration, "maxIterations", maxIterations));

            // **Build system prompt** (instructions only, no history)
            String systemPrompt = promptBuilder.buildSystemPrompt(executionContext, false);

            // **Build user message** based on iteration
            String userMessage;
            if (iteration == 1) {
                // First iteration: pass user query directly
                userMessage = userQuery;
            } else {
                // Subsequent iterations: pass observation from previous iteration
                String lastObservation = eventService.getLastObservation(sessionId);
                userMessage = "The code executed with result: " + lastObservation;
            }

            // Capture iteration in final variable for lambda
            final int currentIteration = iteration;

            // Generate LLM response with streaming
            StringBuilder llmResponseBuilder = new StringBuilder();

            log.info("🌊 Starting streaming LLM response...");

            // Stream response and send chunks to frontend in real-time
            anthropicService.generateStream(sessionId, systemPrompt, userMessage)
                .doOnNext(chunk -> {
                    llmResponseBuilder.append(chunk);
                    
                    // Send chunk to frontend for real-time display
                    sendEvent(sessionId, "thought_chunk", chunk, Map.of("iteration", currentIteration));
                })
                .blockLast(); // Wait for stream to complete

            String llmResponse = llmResponseBuilder.toString();

            log.info("🤔 LLM Response complete: {} chars", llmResponse.length());

            // **Event Stream: Append AGENT_THOUGHT**
            eventService.appendAgentThought(sessionId, llmResponse, iteration);

            fullResponse.append(llmResponse).append("\n\n");

            // Send final complete thought to frontend
            sendEvent(sessionId, "thought", llmResponse, Map.of("iteration", iteration, "complete", true));

            // Extract code blocks from response
            List<String> codeBlocks = promptBuilder.extractCodeBlocks(llmResponse);

            if (codeBlocks.isEmpty()) {
                // No code to execute, task is complete
                log.info("✅ No code blocks found - task complete");

                // **Optional: Validate todos if they exist**
                validateTodoCompletion(sessionId);

                // **Event Stream: Append final AGENT_RESPONSE**
                eventService.appendAgentResponse(sessionId, llmResponse, iteration);
                
                // Generate final summary using LLM
                log.info("📝 Generating final summary...");
                String summaryPrompt = "Based on the conversation above, provide a brief summary (2-3 sentences) of what was accomplished. " +
                    "Focus on the final result and what the user can now do. Be concise and friendly.";
                
                try {
                    final int finalIteration = iteration;
                    StringBuilder summaryBuilder = new StringBuilder();
                    anthropicService.generateStream(sessionId, systemPrompt, summaryPrompt)
                        .doOnNext(chunk -> {
                            summaryBuilder.append(chunk);
                            // Send summary chunks as regular message (not thought)
                            sendEvent(sessionId, "message_chunk", chunk, Map.of("iteration", finalIteration, "final", true));
                        })
                        .blockLast();
                    
                    String summary = summaryBuilder.toString();
                    // Send complete summary as regular message
                    sendEvent(sessionId, "message", summary, Map.of("iteration", finalIteration, "complete", true, "final", true));
                    log.info("✅ Final summary sent: {} chars", summary.length());
                } catch (Exception e) {
                    log.error("❌ Failed to generate summary: {}", e.getMessage());
                    // Fallback - send the thought as final message
                    sendEvent(sessionId, "message", llmResponse, Map.of("iteration", iteration, "complete", true, "final", true));
                }
                
                break;
            }

            // **CRITICAL: Execute ONLY the FIRST code block (ONE action per iteration)**
            String code = codeBlocks.get(0);
            if (codeBlocks.size() > 1) {
                log.warn("⚠️ Multiple code blocks detected ({}) but executing only FIRST one (Manus AI pattern)",
                    codeBlocks.size());
            }

            log.info("▶️ Executing action (code block)");

            // **Validate code before execution**
            log.info("🔍 Validating code syntax and safety...");

            PythonValidationService.ValidationResult syntaxCheck = validationService.validateSyntax(code);
            if (!syntaxCheck.isValid()) {
                log.error("❌ Syntax validation failed: {}", syntaxCheck.error());

                // Append validation error as OBSERVATION so LLM can fix it
                String errorObs = "Code validation failed (syntax error):\n" + syntaxCheck.error() +
                    "\n\nPlease fix the syntax errors and try again.";

                eventService.appendObservation(
                    sessionId,
                    errorObs,
                    Map.of("validationError", syntaxCheck.error(), "codeRejected", true),
                    false,
                    syntaxCheck.error(),
                    0L,
                    iteration
                );

                sendEvent(sessionId, "error", "Syntax validation failed: " + syntaxCheck.error(),
                    Map.of("iteration", iteration, "validation", true));

                // Continue to next iteration so LLM can fix the code
                continue;
            }

            PythonValidationService.ValidationResult safetyCheck = validationService.checkSafety(code);
            if (!safetyCheck.isValid()) {
                log.error("❌ Safety validation failed: {}", safetyCheck.error());

                // Append safety error as OBSERVATION
                String errorObs = "Code rejected due to safety concerns:\n" + safetyCheck.error() +
                    "\n\nThis operation is not allowed for security reasons.";

                eventService.appendObservation(
                    sessionId,
                    errorObs,
                    Map.of("safetyError", safetyCheck.error(), "codeRejected", true),
                    false,
                    safetyCheck.error(),
                    0L,
                    iteration
                );

                sendEvent(sessionId, "error", "Safety check failed: " + safetyCheck.error(),
                    Map.of("iteration", iteration, "safety", true));

                // Continue to next iteration so LLM can use safer approach
                continue;
            }

            // Log warnings but allow execution
            if (safetyCheck.warning() != null) {
                log.warn("⚠️ Code safety warnings: {}", safetyCheck.warning());
                sendEvent(sessionId, "warning", "Code contains risky operations: " + safetyCheck.warning(),
                    Map.of("iteration", iteration));
            }

            log.info("✅ Code validation passed");

            // **Event Stream: Append AGENT_ACTION**
            Map<String, Object> actionData = new HashMap<>();
            actionData.put("codeLength", code.length());
            actionData.put("codePreview", code.substring(0, Math.min(100, code.length())));
            actionData.put("validated", true);
            if (safetyCheck.warning() != null) {
                actionData.put("warnings", safetyCheck.warning());
            }
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

            if (result.getStderr() != null && !result.getStderr().isEmpty()) {
                sendEvent(sessionId, "error", result.getStderr(), resultMeta);
            }

            // If execution failed, the observation contains the error
            // LLM will see it in the event stream and can retry/fix in next iteration

            if (!result.isSuccess()) {
                log.warn("⚠️ Action failed - LLM will see error in next iteration");
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
            if (result.getStdout() != null && !result.getStdout().isEmpty()) {
                obs.append("Output:\n").append(result.getStdout()).append("\n");
            }
            if (result.getVariables() != null && !result.getVariables().isEmpty()) {
                obs.append("Variables updated: ").append(result.getVariables().keySet()).append("\n");
            }
        } else {
            obs.append("Execution failed.\n");
            if (result.getStderr() != null && !result.getStderr().isEmpty()) {
                obs.append("Error:\n").append(result.getStderr()).append("\n");
            }
            if (result.getError() != null) {
                obs.append("Details: ").append(result.getError()).append("\n");
            }
        }

        return obs.toString();
    }

    /**
     * Validate todo.md completion as a safety check
     * Logs warnings if todos exist but are not all checked
     */
    private void validateTodoCompletion(String sessionId) {
        try {
            Path todoPath = Paths.get(workspaceDir, sessionId, "todo.md");
            if (Files.exists(todoPath)) {
                String content = Files.readString(todoPath);
                boolean hasUnchecked = content.contains("- [ ]");
                
                if (hasUnchecked) {
                    log.warn("⚠️ Task stopped but todo.md has unchecked items!");
                    log.warn("Todo content:\n{}", content);
                } else {
                    log.info("✅ All todo items are checked");
                }
            }
        } catch (Exception e) {
            log.debug("Could not validate todo.md: {}", e.getMessage());
        }
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
