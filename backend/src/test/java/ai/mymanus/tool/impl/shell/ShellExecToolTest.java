package ai.mymanus.tool.impl.shell;

import ai.mymanus.service.sandbox.SandboxExecutor;
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
 * Unit tests for ShellExecTool
 * Tests shell command execution via CodeAct pattern
 */
@ExtendWith(MockitoExtension.class)
class ShellExecToolTest {

    @Mock
    private SandboxExecutor sandboxExecutor;

    @InjectMocks
    private ShellExecTool shellExecTool;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testToolMetadata() {
        assertEquals("shell_exec", shellExecTool.getName());
        assertNotNull(shellExecTool.getDescription());

        String signature = shellExecTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("command"));
    }

    @Test
    void testToolRequiresNoNetwork() {
        assertFalse(shellExecTool.requiresNetwork());
    }
}
