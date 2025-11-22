package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserScrollDownTool extends BrowserTool {

    private static final int DEFAULT_SCROLL_AMOUNT = 500;

    public BrowserScrollDownTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_scroll_down";
    }

    @Override
    public String getDescription() {
        return "Scroll the page downward. Default scroll amount is 500 pixels.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_scroll_down(sessionId: str, amount: int = 500) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);
        int amount = parameters.containsKey("amount")
                ? ((Number) parameters.get("amount")).intValue()
                : DEFAULT_SCROLL_AMOUNT;

        log.info("📜 Scrolling down by {} pixels", amount);

        try {
            browserExecutor.scroll(sessionId, amount); // Positive for downward

            var result = success("Scroll down successful");
            result.put("scrollAmount", amount);

            return result;

        } catch (Exception e) {
            log.error("❌ Scroll down failed", e);
            return error("Scroll down failed: " + e.getMessage(), e);
        }
    }
}
