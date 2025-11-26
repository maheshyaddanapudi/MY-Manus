package ai.mymanus.service.browser;

import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Browser Executor - Manages Playwright browser instances for agent sessions.
 *
 * Architecture:
 * - One BrowserSession per agent session
 * - Chromium browser in headless mode
 * - Session isolation via browser contexts
 * - Automatic cleanup on session end
 *
 * Features:
 * - Screenshot capture
 * - Accessibility tree extraction
 * - Cookie and storage management
 * - Viewport configuration
 */
@Slf4j
@Service
public class BrowserExecutor {

    private Playwright playwright;
    private final Map<String, BrowserSession> sessions = new ConcurrentHashMap<>();

    // Browser configuration
    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 720;
    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds

    @PostConstruct
    public void initialize() {
        try {
            log.info("🎭 Initializing Playwright...");
            playwright = Playwright.create();
            log.info("✅ Playwright initialized successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize Playwright: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Playwright", e);
        }
    }

    /**
     * Get or create browser session for given sessionId
     */
    public synchronized BrowserSession getOrCreateSession(String sessionId) {
        BrowserSession session = sessions.get(sessionId);

        if (session == null || !session.isActive()) {
            log.info("🌐 Creating new browser session for: {}", sessionId);
            session = createSession(sessionId);
            sessions.put(sessionId, session);
        }

        return session;
    }

    /**
     * Create a new browser session
     */
    private BrowserSession createSession(String sessionId) {
        try {
            // Launch browser in headless mode
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setTimeout(DEFAULT_TIMEOUT));

            // Create browser context with viewport
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/120.0.0.0 Safari/537.36"));

            // Set default timeout
            context.setDefaultTimeout(DEFAULT_TIMEOUT);

            BrowserSession session = new BrowserSession(sessionId, browser, context);
            log.info("✅ Browser session created successfully for: {}", sessionId);

            return session;

        } catch (Exception e) {
            log.error("❌ Failed to create browser session for {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to create browser session", e);
        }
    }

    /**
     * Navigate to URL
     */
    public Response navigate(String sessionId, String url) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("🔗 Navigating to: {}", url);
        Response response = page.navigate(url);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.info("✅ Navigation complete: {}", url);

        return response;
    }

    /**
     * Capture screenshot as base64
     */
    public String captureScreenshot(String sessionId) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("📸 Capturing screenshot...");
        byte[] screenshot = page.screenshot();
        String base64 = Base64.getEncoder().encodeToString(screenshot);
        log.info("✅ Screenshot captured ({} bytes)", screenshot.length);

        return base64;
    }

    /**
     * Get accessibility tree snapshot
     */
    public String getAccessibilityTree(String sessionId) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("🌳 Extracting accessibility tree...");

        try {
            // Get accessibility snapshot
            var snapshot = page.accessibility().snapshot();

            // Convert to simplified tree structure
            StringBuilder tree = new StringBuilder();
            buildAccessibilityTree(snapshot, tree, 0);

            log.info("✅ Accessibility tree extracted");
            return tree.toString();

        } catch (Exception e) {
            log.warn("⚠️ Failed to extract accessibility tree: {}", e.getMessage());
            return "Accessibility tree unavailable";
        }
    }

    /**
     * Get HTML content of current page
     */
    public String getHtmlContent(String sessionId) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("📄 Extracting HTML content...");

        try {
            String html = page.content();
            log.info("✅ HTML content extracted ({} bytes)", html.length());
            return html;

        } catch (Exception e) {
            log.warn("⚠️ Failed to extract HTML content: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Build accessibility tree string representation
     */
    private void buildAccessibilityTree(AccessibilityNode node, StringBuilder tree, int depth) {
        if (node == null) return;

        String indent = "  ".repeat(depth);
        String role = node.role() != null ? node.role() : "unknown";
        String name = node.name() != null ? node.name() : "";

        tree.append(indent).append(role);
        if (!name.isEmpty()) {
            tree.append(": ").append(name);
        }
        tree.append("\n");

        // Recursively build children
        if (node.children() != null) {
            for (AccessibilityNode child : node.children()) {
                buildAccessibilityTree(child, tree, depth + 1);
            }
        }
    }

    /**
     * Click element by selector
     */
    public void click(String sessionId, String selector) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("🖱️ Clicking element: {}", selector);
        page.click(selector);
        page.waitForTimeout(500); // Short wait for UI updates
        log.info("✅ Click complete: {}", selector);
    }

    /**
     * Type text into input field
     */
    public void type(String sessionId, String selector, String text) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("⌨️ Typing into element: {} (text length: {})", selector, text.length());
        page.fill(selector, text);
        log.info("✅ Type complete: {}", selector);
    }

    /**
     * Scroll page
     */
    public void scroll(String sessionId, int deltaY) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("📜 Scrolling by: {} pixels", deltaY);
        page.evaluate(String.format("window.scrollBy(0, %d)", deltaY));
        page.waitForTimeout(300); // Wait for scroll animation
        log.info("✅ Scroll complete");
    }

    /**
     * Press keyboard key
     */
    public void pressKey(String sessionId, String key) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("⌨️ Pressing key: {}", key);
        page.keyboard().press(key);
        log.info("✅ Key press complete: {}", key);
    }

    /**
     * Refresh current page
     */
    public void refresh(String sessionId) {
        BrowserSession session = getOrCreateSession(sessionId);
        Page page = session.getPage();

        log.info("🔄 Refreshing page...");
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        log.info("✅ Page refreshed");
    }

    /**
     * Get current page URL
     */
    public String getCurrentUrl(String sessionId) {
        BrowserSession session = sessions.get(sessionId);
        if (session == null || !session.isActive()) {
            return null;
        }
        return session.getPage().url();
    }

    /**
     * Get current page title
     */
    public String getCurrentTitle(String sessionId) {
        BrowserSession session = sessions.get(sessionId);
        if (session == null || !session.isActive()) {
            return null;
        }
        return session.getPage().title();
    }

    /**
     * Close browser session
     */
    public void closeSession(String sessionId) {
        BrowserSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
            log.info("🗑️ Browser session closed: {}", sessionId);
        }
    }

    /**
     * Cleanup all sessions
     */
    @PreDestroy
    public void cleanup() {
        log.info("🧹 Cleaning up all browser sessions...");

        sessions.values().forEach(BrowserSession::close);
        sessions.clear();

        if (playwright != null) {
            playwright.close();
            log.info("✅ Playwright closed");
        }

        log.info("✅ Browser executor cleanup complete");
    }

    /**
     * Get session count
     */
    public int getSessionCount() {
        return sessions.size();
    }
}
