package ai.mymanus.service.browser;

import com.microsoft.playwright.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a browser session for a specific agent session.
 * Manages Playwright Browser, BrowserContext, and Page instances.
 *
 * Lifecycle:
 * - One BrowserSession per agent session
 * - Browser context maintains cookies, storage, and session data
 * - Page is reused across navigation actions
 * - Cleanup on session end
 */
@Slf4j
@Getter
public class BrowserSession {

    private final String sessionId;
    private final Browser browser;
    private final BrowserContext context;
    private Page page;
    private final long createdAt;

    public BrowserSession(String sessionId, Browser browser, BrowserContext context) {
        this.sessionId = sessionId;
        this.browser = browser;
        this.context = context;
        this.createdAt = System.currentTimeMillis();
        this.page = context.newPage();
        log.info("🌐 Browser session created for: {}", sessionId);
    }

    /**
     * Get the current page, creating if needed
     */
    public synchronized Page getPage() {
        if (page == null || page.isClosed()) {
            page = context.newPage();
            log.info("📄 Created new page for session: {}", sessionId);
        }
        return page;
    }

    /**
     * Close the browser session and cleanup resources
     */
    public synchronized void close() {
        try {
            if (page != null && !page.isClosed()) {
                page.close();
                log.info("📄 Closed page for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error closing page: {}", e.getMessage());
        }

        try {
            if (context != null) {
                context.close();
                log.info("🔒 Closed browser context for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error closing context: {}", e.getMessage());
        }

        try {
            if (browser != null) {
                browser.close();
                log.info("🌐 Closed browser for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error closing browser: {}", e.getMessage());
        }

        log.info("✅ Browser session cleaned up: {}", sessionId);
    }

    /**
     * Get session age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    /**
     * Check if browser is still active
     */
    public boolean isActive() {
        return browser != null && browser.isConnected();
    }
}
