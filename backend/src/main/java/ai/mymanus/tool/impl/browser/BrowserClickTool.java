package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserClickTool extends BrowserTool {

    public BrowserClickTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_click";
    }

    @Override
    public String getDescription() {
        return "Click an element on the page using a CSS selector.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_click(sessionId: str, selector: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);
        String selector = (String) parameters.get("selector");

        if (selector == null || selector.isEmpty()) {
            return error("Selector parameter is required", null);
        }

        log.info("🖱️ Clicking element: {}", selector);

        try {
            browserExecutor.click(sessionId, selector);

            var result = success("Click successful");
            result.put("selector", selector);

            return result;

        } catch (Exception e) {
            log.error("❌ Click failed: {}", selector, e);
            return error("Click failed: " + e.getMessage(), e);
        }
    }
}
