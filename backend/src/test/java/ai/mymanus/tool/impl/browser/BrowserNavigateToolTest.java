package ai.mymanus.tool.impl.browser;

import ai.mymanus.service.browser.BrowserExecutor;
import com.microsoft.playwright.Response;
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
 * Unit tests for BrowserNavigateTool
 */
@ExtendWith(MockitoExtension.class)
class BrowserNavigateToolTest {

    @Mock
    private BrowserExecutor browserExecutor;

    @InjectMocks
    private BrowserNavigateTool browserNavigateTool;

    @BeforeEach
    void setUp() {
        // Setup is done by Mockito annotations
    }

    @Test
    void testNavigateToUrl() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "https://example.com");
        params.put("sessionId", "session-123");

        Response mockResponse = mock(Response.class);
        when(browserExecutor.navigate(anyString(), anyString())).thenReturn(mockResponse);

        Map<String, Object> result = browserNavigateTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        verify(browserExecutor, times(1)).navigate("session-123", "https://example.com");
    }

    @Test
    void testNavigateToHttpsUrl() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "https://github.com");
        params.put("sessionId", "session-123");

        Response mockResponse = mock(Response.class);
        when(browserExecutor.navigate(anyString(), anyString())).thenReturn(mockResponse);

        Map<String, Object> result = browserNavigateTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testNavigateToHttpUrl() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "http://example.com");
        params.put("sessionId", "session-123");

        Response mockResponse = mock(Response.class);
        when(browserExecutor.navigate(anyString(), anyString())).thenReturn(mockResponse);

        Map<String, Object> result = browserNavigateTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testMissingUrl() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "session-123");

        Map<String, Object> result = browserNavigateTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testMissingSessionId() {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "https://example.com");

        assertThrows(IllegalArgumentException.class, () -> {
            browserNavigateTool.execute(params);
        });
    }

    @Test
    void testToolMetadata() {
        assertEquals("browser_navigate", browserNavigateTool.getName());
        assertNotNull(browserNavigateTool.getDescription());

        String signature = browserNavigateTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("url"));
        assertTrue(signature.contains("sessionId"));
    }
}
