package ai.mymanus.tool.impl.communication;

import ai.mymanus.tool.Tool;
import ai.mymanus.service.AgentStateService;
import ai.mymanus.model.AgentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool to request input from the user mid-execution
 * Pauses agent execution until user responds
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageAskUserTool implements Tool {

    private final AgentStateService agentStateService;

    @Override
    public String getName() {
        return "message_ask_user";
    }

    @Override
    public String getDescription() {
        return "Ask the user a question and wait for their response. Use this when you need additional information or clarification from the user to complete the task.";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("question", "string - The question to ask the user");
        params.put("sessionId", "string - The current session ID");
        return params;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        try {
            String question = (String) parameters.get("question");
            String sessionId = (String) parameters.get("sessionId");

            if (question == null || question.trim().isEmpty()) {
                return error("Question cannot be empty", null);
            }

            if (sessionId == null) {
                return error("Session ID is required", null);
            }

            log.info("❓ Asking user: {}", question);

            // Update agent state to WAITING_INPUT
            AgentState state = agentStateService.getOrCreateState(sessionId);
            state.setStatus(AgentState.Status.WAITING_INPUT);
            state.setCurrentTask("Waiting for user response to: " + question);

            Map<String, Object> context = state.getExecutionContext();
            if (context == null) {
                context = new HashMap<>();
            }
            context.put("pending_question", question);
            context.put("waiting_since", System.currentTimeMillis());

            state.setExecutionContext(context);
            agentStateService.updateState(state);

            var result = success("Question sent to user, waiting for response");
            result.put("question", question);
            result.put("status", "waiting_for_user_input");
            result.put("message", "Agent paused. Waiting for user response.");

            return result;

        } catch (Exception e) {
            log.error("❌ Error asking user", e);
            return error("Failed to ask user: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> success(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", message);
        return result;
    }

    private Map<String, Object> error(String message, Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        if (e != null) {
            result.put("exception", e.getClass().getSimpleName());
        }
        return result;
    }
}
