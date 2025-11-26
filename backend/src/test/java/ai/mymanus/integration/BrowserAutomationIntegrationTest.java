package ai.mymanus.integration;

import ai.mymanus.service.browser.BrowserExecutor;
import ai.mymanus.service.browser.BrowserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for browser automation
 */
@SpringBootTest
@ActiveProfiles("test")
class BrowserAutomationIntegrationTest {

    @Autowired
    private BrowserExecutor browserExecutor;

    private String testSessionId = "browser-integration-test";

    @AfterEach
    void cleanup() {
        browserExecutor.closeSession(testSessionId);
    }

    @Test
    void testBrowserSessionCreation() {
        BrowserSession session = browserExecutor.getOrCreateSession(testSessionId);
        assertNotNull(session);
        assertEquals(testSessionId, session.getSessionId());
    }

    @Test
    void testBrowserNavigation() {
        assertDoesNotThrow(() -> {
            browserExecutor.navigate(testSessionId, "https://example.com");
            String url = browserExecutor.getCurrentUrl(testSessionId);
            assertTrue(url.contains("example.com"));
        });
    }

    @Test
    void testBrowserScreenshot() {
        browserExecutor.navigate(testSessionId, "https://example.com");
        String screenshot = browserExecutor.captureScreenshot(testSessionId);
        assertNotNull(screenshot);
        assertFalse(screenshot.isEmpty());
    }
}
