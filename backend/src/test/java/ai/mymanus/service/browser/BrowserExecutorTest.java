package ai.mymanus.service.browser;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrowserExecutor
 * Tests browser automation and session management
 */
@ExtendWith(MockitoExtension.class)
class BrowserExecutorTest {

    @Mock
    private Playwright playwright;

    @Mock
    private Browser browser;

    @Mock
    private BrowserContext context;

    @Mock
    private Page page;

    private BrowserExecutor browserExecutor;
    private String testSessionId = "test-session-123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(playwright.chromium()).thenReturn(mock(BrowserType.class));
        when(playwright.chromium().launch(any())).thenReturn(browser);
        when(browser.newContext(any())).thenReturn(context);
        when(context.newPage()).thenReturn(page);

        browserExecutor = new BrowserExecutor(playwright);
    }

    @Test
    void testGetOrCreateSession_NewSession() {
        BrowserSession session = browserExecutor.getOrCreateSession(testSessionId);

        assertNotNull(session);
        assertEquals(testSessionId, session.getSessionId());
    }

    @Test
    void testGetOrCreateSession_ExistingSession() {
        BrowserSession session1 = browserExecutor.getOrCreateSession(testSessionId);
        BrowserSession session2 = browserExecutor.getOrCreateSession(testSessionId);

        assertNotNull(session1);
        assertNotNull(session2);
        assertEquals(session1, session2);
    }

    @Test
    void testNavigate() {
        String url = "https://example.com";

        when(page.url()).thenReturn(url);

        browserExecutor.navigate(testSessionId, url);

        verify(page, times(1)).navigate(url);
    }

    @Test
    void testCaptureScreenshot() {
        byte[] mockScreenshot = "screenshot-data".getBytes();

        when(page.screenshot(any(Page.ScreenshotOptions.class)))
            .thenReturn(mockScreenshot);

        String screenshot = browserExecutor.captureScreenshot(testSessionId);

        assertNotNull(screenshot);
        verify(page, times(1)).screenshot(any(Page.ScreenshotOptions.class));
    }

    @Test
    void testGetAccessibilityTree() {
        when(page.accessibility()).thenReturn(mock(Accessibility.class));
        when(page.accessibility().snapshot(any())).thenReturn(null);

        String tree = browserExecutor.getAccessibilityTree(testSessionId);

        assertNotNull(tree);
        verify(page, atLeastOnce()).accessibility();
    }

    @Test
    void testGetHtmlContent() {
        String mockHtml = "<html><body>Test</body></html>";

        when(page.content()).thenReturn(mockHtml);

        String html = browserExecutor.getHtmlContent(testSessionId);

        assertEquals(mockHtml, html);
        verify(page, times(1)).content();
    }

    @Test
    void testGetCurrentUrl() {
        String url = "https://example.com/page";

        when(page.url()).thenReturn(url);

        String currentUrl = browserExecutor.getCurrentUrl(testSessionId);

        assertEquals(url, currentUrl);
        verify(page, times(1)).url();
    }

    @Test
    void testGetCurrentTitle() {
        String title = "Example Domain";

        when(page.title()).thenReturn(title);

        String currentTitle = browserExecutor.getCurrentTitle(testSessionId);

        assertEquals(title, currentTitle);
        verify(page, times(1)).title();
    }

    @Test
    void testClick() {
        String selector = "#submit-button";

        when(page.locator(selector)).thenReturn(mock(Locator.class));

        browserExecutor.click(testSessionId, selector);

        verify(page, times(1)).locator(selector);
    }

    @Test
    void testInput() {
        String selector = "#username";
        String text = "testuser";

        when(page.locator(selector)).thenReturn(mock(Locator.class));

        browserExecutor.input(testSessionId, selector, text);

        verify(page, times(1)).locator(selector);
    }

    @Test
    void testScrollUp() {
        browserExecutor.scrollUp(testSessionId);

        verify(page, times(1)).evaluate(contains("scrollBy"));
    }

    @Test
    void testScrollDown() {
        browserExecutor.scrollDown(testSessionId);

        verify(page, times(1)).evaluate(contains("scrollBy"));
    }

    @Test
    void testPressKey() {
        String key = "Enter";

        when(page.keyboard()).thenReturn(mock(Keyboard.class));

        browserExecutor.pressKey(testSessionId, key);

        verify(page, times(1)).keyboard();
    }

    @Test
    void testRefresh() {
        browserExecutor.refresh(testSessionId);

        verify(page, times(1)).reload();
    }

    @Test
    void testCloseSession() {
        // Create session first
        BrowserSession session = browserExecutor.getOrCreateSession(testSessionId);

        // Close it
        browserExecutor.closeSession(testSessionId);

        // Attempting to close should not throw
        assertDoesNotThrow(() -> {
            browserExecutor.closeSession(testSessionId);
        });
    }

    @Test
    void testMultipleSessions() {
        String session1Id = "session-1";
        String session2Id = "session-2";

        BrowserSession session1 = browserExecutor.getOrCreateSession(session1Id);
        BrowserSession session2 = browserExecutor.getOrCreateSession(session2Id);

        assertNotNull(session1);
        assertNotNull(session2);
        assertNotEquals(session1, session2);
        assertEquals(session1Id, session1.getSessionId());
        assertEquals(session2Id, session2.getSessionId());
    }

    @Test
    void testErrorHandling() {
        when(page.navigate(anyString()))
            .thenThrow(new PlaywrightException("Navigation failed"));

        assertThrows(PlaywrightException.class, () -> {
            browserExecutor.navigate(testSessionId, "https://invalid-url");
        });
    }
}
