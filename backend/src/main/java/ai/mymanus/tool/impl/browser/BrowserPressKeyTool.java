package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserPressKeyTool extends BrowserTool {

    public BrowserPressKeyTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_press_key";
    }

    @Override
    public String getDescription() {
        return "Press a keyboard key. Supports: Enter, Escape, Tab, Backspace, ArrowDown, ArrowUp, etc.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_press_key(sessionId: str, key: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);
        String key = (String) parameters.get("key");

        if (key == null || key.isEmpty()) {
            return error("Key parameter is required", null);
        }

        log.info("⌨️ Pressing key: {}", key);

        try {
            browserExecutor.pressKey(sessionId, key);

            var result = success("Key press successful");
            result.put("key", key);

            return result;

        } catch (Exception e) {
            log.error("❌ Key press failed: {}", key, e);
            return error("Key press failed: " + e.getMessage(), e);
        }
    }
}
