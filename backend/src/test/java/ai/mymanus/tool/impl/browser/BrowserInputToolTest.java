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
 * Unit tests for BrowserInputTool
 */
@ExtendWith(MockitoExtension.class)
class BrowserInputToolTest {

    @Mock
    private BrowserExecutor browserExecutor;

    @InjectMocks
    private BrowserInputTool browserInputTool;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testInputText() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", "#username");
        params.put("text", "testuser");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).type(anyString(), anyString(), anyString());

        Map<String, Object> result = browserInputTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        verify(browserExecutor, times(1)).type("session-123", "#username", "testuser");
    }

    @Test
    void testInputPassword() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", "#password");
        params.put("text", "secret123");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).type(anyString(), anyString(), anyString());

        Map<String, Object> result = browserInputTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testInputEmptyString() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", "#search");
        params.put("text", "");
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).type(anyString(), anyString(), anyString());

        Map<String, Object> result = browserInputTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testMissingSelector() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("text", "test");
        params.put("sessionId", "session-123");

        Map<String, Object> result = browserInputTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testMissingText() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", "#input");
        params.put("sessionId", "session-123");
        // Missing text parameter

        // Tool accepts empty text, so this should succeed
        Map<String, Object> result = browserInputTool.execute(params);
        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("browser_input", browserInputTool.getName());
        assertNotNull(browserInputTool.getDescription());

        String signature = browserInputTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("selector"));
        assertTrue(signature.contains("text"));
        assertTrue(signature.contains("sessionId"));
    }
}
