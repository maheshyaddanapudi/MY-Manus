package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrowserViewTool
 */
@ExtendWith(MockitoExtension.class)
class BrowserViewToolTest {

    @Mock
    private BrowserExecutor browserExecutor;

    @InjectMocks
    private BrowserViewTool browserViewTool;

    @BeforeEach
    void setUp() {
        when(browserExecutor.captureScreenshot(anyString())).thenReturn("base64screenshot");
        when(browserExecutor.getAccessibilityTree(anyString())).thenReturn("accessibility tree");
        when(browserExecutor.getHtmlContent(anyString())).thenReturn("<html>...</html>");
        when(browserExecutor.getCurrentUrl(anyString())).thenReturn("https://example.com");
        when(browserExecutor.getCurrentTitle(anyString())).thenReturn("Example Domain");
    }

    @Test
    void testCaptureView() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "session-123");

        Map<String, Object> result = browserViewTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        assertTrue(result.containsKey("screenshot"));
        assertTrue(result.containsKey("accessibilityTree"));
        assertTrue(result.containsKey("htmlContent"));
        assertTrue(result.containsKey("url"));
        assertTrue(result.containsKey("title"));
        assertTrue(result.containsKey("timestamp"));

        verify(browserExecutor, times(1)).captureScreenshot("session-123");
        verify(browserExecutor, times(1)).getAccessibilityTree("session-123");
        verify(browserExecutor, times(1)).getHtmlContent("session-123");
    }

    @Test
    void testViewContainsAllData() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "session-123");

        Map<String, Object> result = browserViewTool.execute(params);

        assertEquals("base64screenshot", result.get("screenshot"));
        assertEquals("accessibility tree", result.get("accessibilityTree"));
        assertEquals("<html>...</html>", result.get("htmlContent"));
        assertEquals("https://example.com", result.get("url"));
        assertEquals("Example Domain", result.get("title"));
    }

    @Test
    void testMissingSessionId() throws Exception {
        Map<String, Object> params = new HashMap<>();

        Map<String, Object> result = browserViewTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("browser_view", browserViewTool.getName());
        assertNotNull(browserViewTool.getDescription());

        Map<String, String> params = browserViewTool.getParameters();
        assertTrue(params.containsKey("sessionId"));
    }
}
