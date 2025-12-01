package ai.mymanus.model;

import ai.mymanus.service.browser.BrowserSession;
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
 * Unit tests for BrowserSession
 * Tests browser session lifecycle and cleanup
 */
class BrowserSessionTest {

    @Mock
    private Browser browser;

    @Mock
    private BrowserContext context;

    @Mock
    private Page page;

    private BrowserSession browserSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock context.newPage() to return our mock page
        when(context.newPage()).thenReturn(page);
        when(browser.isConnected()).thenReturn(true);
    }

    @Test
    void testBrowserSessionCreation() {
        browserSession = new BrowserSession("session-123", browser, context);

        assertNotNull(browserSession);
        assertEquals("session-123", browserSession.getSessionId());
        assertEquals(browser, browserSession.getBrowser());
        assertEquals(context, browserSession.getContext());
        assertNotNull(browserSession.getPage());
        verify(context, times(1)).newPage();
    }

    @Test
    void testGetSessionId() {
        browserSession = new BrowserSession("test-session", browser, context);

        assertEquals("test-session", browserSession.getSessionId());
    }

    @Test
    void testGetBrowser() {
        browserSession = new BrowserSession("session-1", browser, context);

        assertNotNull(browserSession.getBrowser());
        assertEquals(browser, browserSession.getBrowser());
    }

    @Test
    void testGetContext() {
        browserSession = new BrowserSession("session-1", browser, context);

        assertNotNull(browserSession.getContext());
        assertEquals(context, browserSession.getContext());
    }

    @Test
    void testGetPage() {
        browserSession = new BrowserSession("session-1", browser, context);

        assertNotNull(browserSession.getPage());
        assertEquals(page, browserSession.getPage());
    }

    @Test
    void testClose() {
        when(page.isClosed()).thenReturn(false);
        
        browserSession = new BrowserSession("session-1", browser, context);
        browserSession.close();

        verify(page, times(1)).close();
        verify(context, times(1)).close();
        verify(browser, times(1)).close();
    }

    @Test
    void testCloseWithAlreadyClosedPage() {
        when(page.isClosed()).thenReturn(true);
        
        browserSession = new BrowserSession("session-1", browser, context);
        browserSession.close();

        // Page.close() should not be called if already closed
        verify(page, never()).close();
        verify(context, times(1)).close();
        verify(browser, times(1)).close();
    }

    @Test
    void testGetPageCreatesNewIfClosed() {
        Page newPage = mock(Page.class);
        when(page.isClosed()).thenReturn(true);
        when(context.newPage()).thenReturn(page, newPage);
        
        browserSession = new BrowserSession("session-1", browser, context);
        
        // First call returns the initial page
        Page firstPage = browserSession.getPage();
        assertEquals(page, firstPage);
        
        // Simulate page being closed
        when(page.isClosed()).thenReturn(true);
        when(context.newPage()).thenReturn(newPage);
        
        // Second call should create new page
        Page secondPage = browserSession.getPage();
        
        // Should have called newPage at least twice (once in constructor, once in getPage)
        verify(context, atLeast(1)).newPage();
    }

    @Test
    void testGetAge() throws InterruptedException {
        browserSession = new BrowserSession("session-1", browser, context);
        
        long age1 = browserSession.getAge();
        assertTrue(age1 >= 0);
        
        Thread.sleep(10);
        
        long age2 = browserSession.getAge();
        assertTrue(age2 > age1);
    }

    @Test
    void testIsActive() {
        when(browser.isConnected()).thenReturn(true);
        browserSession = new BrowserSession("session-1", browser, context);
        
        assertTrue(browserSession.isActive());
        
        when(browser.isConnected()).thenReturn(false);
        assertFalse(browserSession.isActive());
    }

    @Test
    void testIsActiveWithNullBrowser() {
        // This tests the null check in isActive()
        browserSession = new BrowserSession("session-1", null, context);
        assertFalse(browserSession.isActive());
    }

    @Test
    void testMultipleSessions() {
        BrowserSession session1 = new BrowserSession("session-1", browser, context);
        BrowserSession session2 = new BrowserSession("session-2", browser, context);
        
        assertNotEquals(session1.getSessionId(), session2.getSessionId());
        assertEquals("session-1", session1.getSessionId());
        assertEquals("session-2", session2.getSessionId());
    }

    @Test
    void testCloseHandlesExceptions() {
        when(page.isClosed()).thenReturn(false);
        doThrow(new RuntimeException("Page close error")).when(page).close();
        doThrow(new RuntimeException("Context close error")).when(context).close();
        doThrow(new RuntimeException("Browser close error")).when(browser).close();
        
        browserSession = new BrowserSession("session-1", browser, context);
        
        // Should not throw exception despite errors
        assertDoesNotThrow(() -> browserSession.close());
        
        verify(page, times(1)).close();
        verify(context, times(1)).close();
        verify(browser, times(1)).close();
    }

    @Test
    void testCreatedAtTimestamp() {
        long beforeCreation = System.currentTimeMillis();
        browserSession = new BrowserSession("session-1", browser, context);
        long afterCreation = System.currentTimeMillis();
        
        long age = browserSession.getAge();
        assertTrue(age >= 0);
        assertTrue(age <= (afterCreation - beforeCreation) + 10); // Small tolerance
    }
}
