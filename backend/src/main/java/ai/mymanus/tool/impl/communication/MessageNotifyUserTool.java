package ai.mymanus.tool.impl.communication;

import ai.mymanus.tool.Tool;
import ai.mymanus.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool to send notifications to the user
 * Used when agent wants to inform user of progress or status
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageNotifyUserTool implements Tool {

    private final EventService eventService;

    @Override
    public String getName() {
        return "message_notify_user";
    }

    @Override
    public String getDescription() {
        return "Send a notification message to the user. Use this to inform the user of progress, warnings, or status updates during task execution.";
    }

    @Override
    public String getPythonSignature() {
        return "message_notify_user(sessionId: str, message: str, level: str = 'info') -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        try {
            String sessionId = (String) parameters.get("sessionId");
            String message = (String) parameters.get("message");
            String level = (String) parameters.getOrDefault("level", "info");

            if (sessionId == null || sessionId.trim().isEmpty()) {
                return error("sessionId parameter required", null);
            }

            if (message == null || message.trim().isEmpty()) {
                return error("Message cannot be empty", null);
            }

            // Create notification event
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("message", message);
            notificationData.put("level", level);
            notificationData.put("timestamp", System.currentTimeMillis());
            notificationData.put("type", "user_notification");

            log.info("📬 Notifying user ({}) in session {}: {}", level, sessionId, message);

            var result = success("Notification sent to user");
            result.put("message", message);
            result.put("level", level);

            return result;

        } catch (Exception e) {
            log.error("❌ Error sending notification", e);
            return error("Failed to send notification: " + e.getMessage(), e);
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
