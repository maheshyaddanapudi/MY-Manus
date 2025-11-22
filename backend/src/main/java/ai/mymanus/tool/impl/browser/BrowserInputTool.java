package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserInputTool extends BrowserTool {

    public BrowserInputTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_input";
    }

    @Override
    public String getDescription() {
        return "Type text into an input field using a CSS selector.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_input(sessionId: str, selector: str, text: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);
        String selector = (String) parameters.get("selector");
        String text = (String) parameters.get("text");

        if (selector == null || selector.isEmpty()) {
            return error("Selector parameter is required", null);
        }

        if (text == null) {
            text = "";
        }

        log.info("⌨️ Typing into element: {}", selector);

        try {
            browserExecutor.type(sessionId, selector, text);

            var result = success("Input successful");
            result.put("selector", selector);
            result.put("textLength", text.length());

            return result;

        } catch (Exception e) {
            log.error("❌ Input failed: {}", selector, e);
            return error("Input failed: " + e.getMessage(), e);
        }
    }
}
