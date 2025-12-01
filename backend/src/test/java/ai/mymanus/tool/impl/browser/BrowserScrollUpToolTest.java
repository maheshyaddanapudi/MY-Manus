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
 * Unit tests for BrowserScrollUpTool
 */
@ExtendWith(MockitoExtension.class)
class BrowserScrollUpToolTest {

    @Mock
    private BrowserExecutor browserExecutor;

    @InjectMocks
    private BrowserScrollUpTool browserScrollUpTool;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testScrollUp() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).scroll(anyString(), anyInt());

        Map<String, Object> result = browserScrollUpTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        verify(browserExecutor, times(1)).scroll(eq("session-123"), eq(-500));
    }

    @Test
    void testMissingSessionId() throws Exception {
        Map<String, Object> params = new HashMap<>();

        Map<String, Object> result = browserScrollUpTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("browser_scroll_up", browserScrollUpTool.getName());
        assertNotNull(browserScrollUpTool.getDescription());

        String signature = browserScrollUpTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("sessionId"));
    }
}
