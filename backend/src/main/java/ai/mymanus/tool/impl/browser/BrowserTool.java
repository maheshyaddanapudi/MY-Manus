package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import ai.mymanus.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for browser automation tools.
 *
 * Manus AI Integration:
 * - Uses Playwright for browser automation
 * - Provides screenshot + accessibility tree for agent observation
 * - Session-based browser instances
 * - Chromium in headless mode
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BrowserTool implements Tool {

    protected final BrowserExecutor browserExecutor;

    /**
     * Get session ID from parameters (required for all browser tools)
     */
    protected String getSessionId(Map<String, Object> parameters) {
        String sessionId = (String) parameters.get("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("sessionId is required for browser operations");
        }
        return sessionId;
    }

    /**
     * Create a successful result map
     */
    protected Map<String, Object> success(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    /**
     * Create an error result map
     */
    protected Map<String, Object> error(String message, Exception e) {
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
        return true; // Browser operations require network access
    }
}
