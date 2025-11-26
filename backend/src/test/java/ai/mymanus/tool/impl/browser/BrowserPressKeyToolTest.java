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
 * Unit tests for BrowserPressKeyTool
 */
@ExtendWith(MockitoExtension.class)
class BrowserPressKeyToolTest {

    @Mock
    private BrowserExecutor browserExecutor;

    @InjectMocks
    private BrowserPressKeyTool browserPressKeyTool;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testPressEnterKey() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "Enter");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).pressKey(anyString(), anyString());

        Map<String, Object> result = browserPressKeyTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        verify(browserExecutor, times(1)).pressKey("session-123", "Enter");
    }

    @Test
    void testPressEscapeKey() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "Escape");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).pressKey(anyString(), anyString());

        Map<String, Object> result = browserPressKeyTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testPressTabKey() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "Tab");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).pressKey(anyString(), anyString());

        Map<String, Object> result = browserPressKeyTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testMissingKey() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "session-123");

        Map<String, Object> result = browserPressKeyTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("browser_press_key", browserPressKeyTool.getName());
        assertNotNull(browserPressKeyTool.getDescription());

        Map<String, String> params = browserPressKeyTool.getParameters();
        assertTrue(params.containsKey("key"));
        assertTrue(params.containsKey("sessionId"));
    }
}
