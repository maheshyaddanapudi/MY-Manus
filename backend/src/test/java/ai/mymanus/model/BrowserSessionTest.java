package ai.mymanus.model;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrowserSession model
 * Tests browser session lifecycle and cleanup
 */
class BrowserSessionTest {

    @Mock
    private Browser browser;

    @Mock
    private BrowserContext context;

    @Mock
    private Page page;

    private ai.mymanus.service.browser.BrowserSession browserSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBrowserSessionCreation() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-123", browser, context, page
        );

        assertNotNull(browserSession);
        assertEquals("session-123", browserSession.getSessionId());
        assertEquals(browser, browserSession.getBrowser());
        assertEquals(context, browserSession.getContext());
        assertEquals(page, browserSession.getPage());
    }

    @Test
    void testGetSessionId() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "test-session", browser, context, page
        );

        assertEquals("test-session", browserSession.getSessionId());
    }

    @Test
    void testGetBrowser() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", browser, context, page
        );

        assertNotNull(browserSession.getBrowser());
        assertEquals(browser, browserSession.getBrowser());
    }

    @Test
    void testGetContext() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", browser, context, page
        );

        assertNotNull(browserSession.getContext());
        assertEquals(context, browserSession.getContext());
    }

    @Test
    void testGetPage() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", browser, context, page
        );

        assertNotNull(browserSession.getPage());
        assertEquals(page, browserSession.getPage());
    }

    @Test
    void testClose() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", browser, context, page
        );

        browserSession.close();

        verify(context, times(1)).close();
        verify(browser, times(1)).close();
    }

    @Test
    void testCloseWithNullContext() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", browser, null, page
        );

        // Should not throw exception
        browserSession.close();

        verify(browser, times(1)).close();
    }

    @Test
    void testCloseWithNullBrowser() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", null, context, page
        );

        // Should not throw exception
        browserSession.close();

        verify(context, times(1)).close();
    }

    @Test
    void testCloseIdempotent() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", browser, context, page
        );

        // Close multiple times should not cause issues
        browserSession.close();
        browserSession.close();
        browserSession.close();

        // Verify close was called (may be called multiple times)
        verify(context, atLeastOnce()).close();
        verify(browser, atLeastOnce()).close();
    }

    @Test
    void testSessionIsolation() {
        ai.mymanus.service.browser.BrowserSession session1 =
            new ai.mymanus.service.browser.BrowserSession(
                "session-1", mock(Browser.class), mock(BrowserContext.class), mock(Page.class)
            );

        ai.mymanus.service.browser.BrowserSession session2 =
            new ai.mymanus.service.browser.BrowserSession(
                "session-2", mock(Browser.class), mock(BrowserContext.class), mock(Page.class)
            );

        assertNotEquals(session1.getSessionId(), session2.getSessionId());
        assertNotEquals(session1.getBrowser(), session2.getBrowser());
        assertNotEquals(session1.getContext(), session2.getContext());
        assertNotEquals(session1.getPage(), session2.getPage());
    }

    @Test
    void testGetCreatedAt() {
        browserSession = new ai.mymanus.service.browser.BrowserSession(
            "session-1", browser, context, page
        );

        long createdAt = browserSession.getCreatedAt();
        assertTrue(createdAt > 0);
        assertTrue(createdAt <= System.currentTimeMillis());
    }

    @Test
    void testCreatedAtOrdering() throws InterruptedException {
        ai.mymanus.service.browser.BrowserSession session1 =
            new ai.mymanus.service.browser.BrowserSession(
                "session-1", mock(Browser.class), mock(BrowserContext.class), mock(Page.class)
            );

        Thread.sleep(10); // Small delay

        ai.mymanus.service.browser.BrowserSession session2 =
            new ai.mymanus.service.browser.BrowserSession(
                "session-2", mock(Browser.class), mock(BrowserContext.class), mock(Page.class)
            );

        assertTrue(session2.getCreatedAt() >= session1.getCreatedAt());
    }
}
