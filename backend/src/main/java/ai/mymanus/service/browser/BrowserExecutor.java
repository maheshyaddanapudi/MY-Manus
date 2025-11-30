package ai.mymanus.service.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
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

        log.info("🌳 Extracting DOM structure...");
        log.warn("⚠️ Accessibility tree API unavailable in Playwright 1.40.0 (requires 1.47+)");

        try {
            // Temporary workaround: Extract simplified DOM structure
            // TODO: Upgrade Playwright to 1.47+ to use proper ariaSnapshot() API
            String jsCode = "() => { " +
                "const getStructure = (el, depth = 0) => { " +
                "  if (depth > 5) return null; " +
                "  const text = el.innerText || ''; " +
                "  return { " +
                "    tag: el.tagName, " +
                "    role: el.getAttribute('role'), " +
                "    text: text.substring(0, 100), " +
                "    children: Array.from(el.children).slice(0, 10).map(c => getStructure(c, depth + 1)).filter(Boolean) " +
                "  }; " +
                "}; " +
                "return JSON.stringify(getStructure(document.body)); " +
            "}";
            
            String domStructure = page.evaluate(jsCode).toString();

            log.info("✅ DOM structure extracted (limited alternative to accessibility tree)");
            return domStructure;

        } catch (Exception e) {
            log.error("❌ Failed to extract DOM structure: {}", e.getMessage());
            return "{\"error\": \"DOM structure unavailable\"}";
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
