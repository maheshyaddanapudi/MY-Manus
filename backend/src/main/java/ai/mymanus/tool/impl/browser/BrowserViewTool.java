package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserViewTool extends BrowserTool {

    public BrowserViewTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_view";
    }

    @Override
    public String getDescription() {
        return "Get current browser view including screenshot (base64) and accessibility tree. " +
               "Essential for agent to understand page state.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_view(sessionId: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);

        log.info("📸 Capturing browser view...");

        try {
            String screenshot = browserExecutor.captureScreenshot(sessionId);
            String accessibilityTree = browserExecutor.getAccessibilityTree(sessionId);

            var result = success("Browser view captured");
            result.put("screenshot", screenshot);
            result.put("accessibilityTree", accessibilityTree);
            result.put("url", browserExecutor.getCurrentUrl(sessionId));
            result.put("title", browserExecutor.getCurrentTitle(sessionId));

            return result;

        } catch (Exception e) {
            log.error("❌ Failed to capture browser view", e);
            return error("Failed to capture view: " + e.getMessage(), e);
        }
    }
}
