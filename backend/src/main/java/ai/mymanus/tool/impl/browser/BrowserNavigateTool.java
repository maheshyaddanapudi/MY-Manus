package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserNavigateTool extends BrowserTool {

    public BrowserNavigateTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_navigate";
    }

    @Override
    public String getDescription() {
        return "Navigate the browser to a specific URL. Returns success status and current URL.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_navigate(sessionId: str, url: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);
        String url = (String) parameters.get("url");

        if (url == null || url.isEmpty()) {
            return error("URL parameter is required", null);
        }

        log.info("🔗 Navigating to: {}", url);

        try {
            browserExecutor.navigate(sessionId, url);

            var result = success("Navigation successful");
            result.put("url", browserExecutor.getCurrentUrl(sessionId));
            result.put("title", browserExecutor.getCurrentTitle(sessionId));

            return result;

        } catch (Exception e) {
            log.error("❌ Navigation failed: {}", url, e);
            return error("Navigation failed: " + e.getMessage(), e);
        }
    }
}
