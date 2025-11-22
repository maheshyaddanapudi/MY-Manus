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
 * Unit tests for BrowserClickTool
 */
@ExtendWith(MockitoExtension.class)
class BrowserClickToolTest {

    @Mock
    private BrowserExecutor browserExecutor;

    @InjectMocks
    private BrowserClickTool browserClickTool;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testClickElement() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", "#submit-button");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).click(anyString(), anyString());

        Map<String, Object> result = browserClickTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        verify(browserExecutor, times(1)).click("session-123", "#submit-button");
    }

    @Test
    void testClickByClass() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", ".btn-primary");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).click(anyString(), anyString());

        Map<String, Object> result = browserClickTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testClickByXPath() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", "//button[@id='submit']");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).click(anyString(), anyString());

        Map<String, Object> result = browserClickTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testMissingSelector() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "session-123");

        Map<String, Object> result = browserClickTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("browser_click", browserClickTool.getName());
        assertNotNull(browserClickTool.getDescription());

        Map<String, String> params = browserClickTool.getParameters();
        assertTrue(params.containsKey("selector"));
        assertTrue(params.containsKey("sessionId"));
    }
}
