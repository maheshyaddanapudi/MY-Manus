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
 * Unit tests for BrowserRefreshTool
 */
@ExtendWith(MockitoExtension.class)
class BrowserRefreshToolTest {

    @Mock
    private BrowserExecutor browserExecutor;

    @InjectMocks
    private BrowserRefreshTool browserRefreshTool;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testRefreshPage() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "session-123");

        doNothing().when(browserExecutor).refresh(anyString());

        Map<String, Object> result = browserRefreshTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        verify(browserExecutor, times(1)).refresh("session-123");
    }

    @Test
    void testMissingSessionId() throws Exception {
        Map<String, Object> params = new HashMap<>();

        Map<String, Object> result = browserRefreshTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("browser_refresh", browserRefreshTool.getName());
        assertNotNull(browserRefreshTool.getDescription());

        Map<String, String> params = browserRefreshTool.getParameters();
        assertTrue(params.containsKey("sessionId"));
    }
}
