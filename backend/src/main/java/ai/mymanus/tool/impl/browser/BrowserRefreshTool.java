package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserRefreshTool extends BrowserTool {

    public BrowserRefreshTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_refresh";
    }

    @Override
    public String getDescription() {
        return "Refresh the current page.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_refresh(sessionId: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);

        log.info("🔄 Refreshing page...");

        try {
            browserExecutor.refresh(sessionId);

            var result = success("Page refreshed successfully");
            result.put("url", browserExecutor.getCurrentUrl(sessionId));
            result.put("title", browserExecutor.getCurrentTitle(sessionId));

            return result;

        } catch (Exception e) {
            log.error("❌ Refresh failed", e);
            return error("Refresh failed: " + e.getMessage(), e);
        }
    }
}
