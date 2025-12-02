package ai.mymanus.controller;

import ai.mymanus.config.TestSecurityConfig;
import ai.mymanus.service.sandbox.SandboxExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SandboxController
 * Tests sandbox monitoring and management API
 */
@WebMvcTest(SandboxController.class)
@Import(TestSecurityConfig.class)
class SandboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SandboxExecutor sandboxExecutor;

    private String testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = "test-session-123";
    }

    @Test
    void testGetStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalContainers", 3);
        stats.put("activeSessions", List.of("session1", "session2", "session3"));

        when(sandboxExecutor.getContainerStats())
            .thenReturn(stats);

        mockMvc.perform(get("/api/sandbox/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalContainers").value(3))
            .andExpect(jsonPath("$.activeSessions").isArray());

        verify(sandboxExecutor, times(1)).getContainerStats();
    }

    @Test
    void testGetStatsError() throws Exception {
        when(sandboxExecutor.getContainerStats())
            .thenThrow(new RuntimeException("Docker daemon not running"));

        mockMvc.perform(get("/api/sandbox/stats"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Docker daemon not running"));

        verify(sandboxExecutor, times(1)).getContainerStats();
    }

    @Test
    void testCleanupContainer() throws Exception {
        doNothing().when(sandboxExecutor).destroySessionContainer(testSessionId);

        mockMvc.perform(delete("/api/sandbox/cleanup/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Container destroyed successfully"))
            .andExpect(jsonPath("$.sessionId").value(testSessionId));

        verify(sandboxExecutor, times(1)).destroySessionContainer(testSessionId);
    }

    @Test
    void testCleanupContainerError() throws Exception {
        doThrow(new RuntimeException("Container not found"))
            .when(sandboxExecutor).destroySessionContainer(testSessionId);

        mockMvc.perform(delete("/api/sandbox/cleanup/" + testSessionId))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Container not found"))
            .andExpect(jsonPath("$.sessionId").value(testSessionId));

        verify(sandboxExecutor, times(1)).destroySessionContainer(testSessionId);
    }

    @Test
    void testCleanupAllContainers() throws Exception {
        Map<String, Object> statsBefore = new HashMap<>();
        statsBefore.put("totalContainers", 5);
        statsBefore.put("activeSessions", List.of("s1", "s2", "s3", "s4", "s5"));

        when(sandboxExecutor.getContainerStats())
            .thenReturn(statsBefore);
        doNothing().when(sandboxExecutor).cleanupAllContainers();

        mockMvc.perform(post("/api/sandbox/cleanup/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("All sandbox environments destroyed"))
            .andExpect(jsonPath("$.environmentsDestroyed").value(5));

        verify(sandboxExecutor, times(1)).getContainerStats();
        verify(sandboxExecutor, times(1)).cleanupAllContainers();
    }

    @Test
    void testCleanupAllContainersHostMode() throws Exception {
        // In host mode, stats don't have totalContainers, use activeSessions instead
        Map<String, Object> statsBefore = new HashMap<>();
        statsBefore.put("activeSessions", 3);
        statsBefore.put("workspaceDir", "/tmp/manus-workspace");

        when(sandboxExecutor.getContainerStats())
            .thenReturn(statsBefore);
        doNothing().when(sandboxExecutor).cleanupAllContainers();

        mockMvc.perform(post("/api/sandbox/cleanup/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("All sandbox environments destroyed"))
            .andExpect(jsonPath("$.environmentsDestroyed").value(3));

        verify(sandboxExecutor, times(1)).getContainerStats();
        verify(sandboxExecutor, times(1)).cleanupAllContainers();
    }

    @Test
    void testCleanupAllContainersError() throws Exception {
        when(sandboxExecutor.getContainerStats())
            .thenThrow(new RuntimeException("Failed to get stats"));

        mockMvc.perform(post("/api/sandbox/cleanup/all"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Failed to get stats"));

        verify(sandboxExecutor, times(1)).getContainerStats();
        verify(sandboxExecutor, never()).cleanupAllContainers();
    }

    @Test
    void testCleanupAllContainersNoActive() throws Exception {
        Map<String, Object> statsBefore = new HashMap<>();
        statsBefore.put("totalContainers", 0);
        statsBefore.put("activeSessions", List.of());

        when(sandboxExecutor.getContainerStats())
            .thenReturn(statsBefore);
        doNothing().when(sandboxExecutor).cleanupAllContainers();

        mockMvc.perform(post("/api/sandbox/cleanup/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("All sandbox environments destroyed"))
            .andExpect(jsonPath("$.environmentsDestroyed").value(0));

        verify(sandboxExecutor, times(1)).getContainerStats();
        verify(sandboxExecutor, times(1)).cleanupAllContainers();
    }

    @Test
    void testGetStatsDockerMode() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalContainers", 2);
        stats.put("activeSessions", List.of("session1", "session2"));
        stats.put("mode", "docker");

        when(sandboxExecutor.getContainerStats())
            .thenReturn(stats);

        mockMvc.perform(get("/api/sandbox/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalContainers").value(2))
            .andExpect(jsonPath("$.mode").value("docker"));

        verify(sandboxExecutor, times(1)).getContainerStats();
    }

    @Test
    void testGetStatsHostMode() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", 1);
        stats.put("workspaceDir", "/tmp/manus-workspace");
        stats.put("pythonPath", "/usr/bin/python3");
        stats.put("mode", "host");

        when(sandboxExecutor.getContainerStats())
            .thenReturn(stats);

        mockMvc.perform(get("/api/sandbox/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeSessions").value(1))
            .andExpect(jsonPath("$.mode").value("host"));

        verify(sandboxExecutor, times(1)).getContainerStats();
    }

    @Test
    void testCleanupContainerSuccess() throws Exception {
        doNothing().when(sandboxExecutor).destroySessionContainer(anyString());

        mockMvc.perform(delete("/api/sandbox/cleanup/session-abc-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.sessionId").value("session-abc-123"));

        verify(sandboxExecutor, times(1)).destroySessionContainer("session-abc-123");
    }
}
