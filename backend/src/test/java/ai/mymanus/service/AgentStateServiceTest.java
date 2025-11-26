package ai.mymanus.service;

import ai.mymanus.model.AgentState;
import ai.mymanus.repository.AgentStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentStateService
 * Tests state persistence and retrieval
 */
@ExtendWith(MockitoExtension.class)
class AgentStateServiceTest {

    @Mock
    private AgentStateRepository agentStateRepository;

    @InjectMocks
    private AgentStateService agentStateService;

    private AgentState testState;
    private String testSessionId = "test-session-123";

    @BeforeEach
    void setUp() {
        testState = new AgentState();
        testState.setSessionId(testSessionId);
        testState.setIteration(1);
        testState.setStatus(AgentState.Status.IDLE);
        testState.setPythonVariables(new HashMap<>());
        testState.setExecutionContext(new HashMap<>());
    }

    @Test
    void testGetOrCreateState_WhenStateExists() {
        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(testState));

        AgentState result = agentStateService.getOrCreateState(testSessionId);

        assertNotNull(result);
        assertEquals(testSessionId, result.getSessionId());
        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
        verify(agentStateRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateState_WhenStateDoesNotExist() {
        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.empty());
        when(agentStateRepository.save(any(AgentState.class)))
            .thenReturn(testState);

        AgentState result = agentStateService.getOrCreateState(testSessionId);

        assertNotNull(result);
        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
        verify(agentStateRepository, times(1)).save(any(AgentState.class));
    }

    @Test
    void testUpdateState() {
        when(agentStateRepository.save(testState))
            .thenReturn(testState);

        AgentState result = agentStateService.updateState(testState);

        assertNotNull(result);
        assertEquals(testState.getSessionId(), result.getSessionId());
        verify(agentStateRepository, times(1)).save(testState);
    }

    @Test
    void testUpdateIteration() {
        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(testState));
        when(agentStateRepository.save(any(AgentState.class)))
            .thenReturn(testState);

        agentStateService.updateIteration(testSessionId, 5);

        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
        verify(agentStateRepository, times(1)).save(any(AgentState.class));
    }

    @Test
    void testUpdateStatus() {
        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(testState));
        when(agentStateRepository.save(any(AgentState.class)))
            .thenReturn(testState);

        agentStateService.updateStatus(testSessionId, AgentState.Status.RUNNING);

        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
        verify(agentStateRepository, times(1)).save(any(AgentState.class));
    }

    @Test
    void testUpdatePythonVariables() {
        Map<String, Object> newVariables = new HashMap<>();
        newVariables.put("counter", 10);
        newVariables.put("message", "Hello");

        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(testState));
        when(agentStateRepository.save(any(AgentState.class)))
            .thenReturn(testState);

        agentStateService.updatePythonVariables(testSessionId, newVariables);

        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
        verify(agentStateRepository, times(1)).save(any(AgentState.class));
    }

    @Test
    void testUpdateExecutionContext() {
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("current_file", "/workspace/test.py");

        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(testState));
        when(agentStateRepository.save(any(AgentState.class)))
            .thenReturn(testState);

        agentStateService.updateExecutionContext(testSessionId, newContext);

        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
        verify(agentStateRepository, times(1)).save(any(AgentState.class));
    }

    @Test
    void testGetState() {
        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(testState));

        Optional<AgentState> result = agentStateService.getState(testSessionId);

        assertTrue(result.isPresent());
        assertEquals(testSessionId, result.get().getSessionId());
        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
    }

    @Test
    void testGetState_WhenNotFound() {
        when(agentStateRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.empty());

        Optional<AgentState> result = agentStateService.getState(testSessionId);

        assertFalse(result.isPresent());
        verify(agentStateRepository, times(1)).findBySessionId(testSessionId);
    }

    @Test
    void testDeleteState() {
        doNothing().when(agentStateRepository).deleteBySessionId(testSessionId);

        agentStateService.deleteState(testSessionId);

        verify(agentStateRepository, times(1)).deleteBySessionId(testSessionId);
    }
}
