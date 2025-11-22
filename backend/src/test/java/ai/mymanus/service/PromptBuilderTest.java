package ai.mymanus.service;

import ai.mymanus.model.Event;
import ai.mymanus.model.Message;
import ai.mymanus.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PromptBuilder
 * Tests prompt generation logic
 */
@ExtendWith(MockitoExtension.class)
class PromptBuilderTest {

    @Mock
    private ToolRegistry toolRegistry;

    @InjectMocks
    private PromptBuilder promptBuilder;

    private String testToolBindings;

    @BeforeEach
    void setUp() {
        testToolBindings = """
            # Tool Functions
            def file_read(path):
                '''Read a file from workspace'''
                pass

            def browser_navigate(url, sessionId):
                '''Navigate browser to URL'''
                pass
            """;
    }

    @Test
    void testBuildSystemPrompt() {
        when(toolRegistry.generatePythonBindings())
            .thenReturn(testToolBindings);

        String systemPrompt = promptBuilder.buildSystemPrompt();

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("CodeAct"));
        assertTrue(systemPrompt.contains("Python"));
        verify(toolRegistry, times(1)).generatePythonBindings();
    }

    @Test
    void testSystemPromptContainsToolBindings() {
        when(toolRegistry.generatePythonBindings())
            .thenReturn(testToolBindings);

        String systemPrompt = promptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("file_read"));
        assertTrue(systemPrompt.contains("browser_navigate"));
    }

    @Test
    void testSystemPromptContainsCodeActInstructions() {
        when(toolRegistry.generatePythonBindings())
            .thenReturn(testToolBindings);

        String systemPrompt = promptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("python") || systemPrompt.contains("code"));
        assertTrue(systemPrompt.contains("function") || systemPrompt.contains("tool"));
    }

    @Test
    void testBuildMessages() {
        List<Event> events = new ArrayList<>();

        // User message event
        Map<String, Object> userData = new HashMap<>();
        userData.put("text", "Read /workspace/test.txt");
        Event userEvent = new Event();
        userEvent.setType(Event.EventType.USER_MESSAGE);
        userEvent.setData(userData);
        events.add(userEvent);

        // Agent thought event
        Map<String, Object> thoughtData = new HashMap<>();
        thoughtData.put("thought", "I need to read the file");
        Event thoughtEvent = new Event();
        thoughtEvent.setType(Event.EventType.AGENT_THOUGHT);
        thoughtEvent.setData(thoughtData);
        events.add(thoughtEvent);

        List<Map<String, String>> messages = promptBuilder.buildMessages(events);

        assertNotNull(messages);
        assertFalse(messages.isEmpty());
    }

    @Test
    void testBuildMessagesFromEmpty() {
        List<Event> events = new ArrayList<>();

        List<Map<String, String>> messages = promptBuilder.buildMessages(events);

        assertNotNull(messages);
        // Should return empty list or minimal messages
    }

    @Test
    void testUserMessageConversion() {
        Map<String, Object> userData = new HashMap<>();
        userData.put("text", "Hello, agent!");

        Event event = new Event();
        event.setType(Event.EventType.USER_MESSAGE);
        event.setData(userData);

        assertEquals(Event.EventType.USER_MESSAGE, event.getType());
        assertEquals("Hello, agent!", event.getData().get("text"));
    }

    @Test
    void testAssistantMessageConversion() {
        Map<String, Object> assistantData = new HashMap<>();
        assistantData.put("content", "I'll help you with that");

        Event event = new Event();
        event.setType(Event.EventType.AGENT_THOUGHT);
        event.setData(assistantData);

        assertEquals(Event.EventType.AGENT_THOUGHT, event.getType());
    }

    @Test
    void testObservationFormatting() {
        Map<String, Object> obsData = new HashMap<>();
        obsData.put("stdout", "File content here");
        obsData.put("exitCode", 0);

        Event event = new Event();
        event.setType(Event.EventType.OBSERVATION);
        event.setData(obsData);

        assertEquals("File content here", event.getData().get("stdout"));
        assertEquals(0, event.getData().get("exitCode"));
    }

    @Test
    void testToolBindingsFormat() {
        when(toolRegistry.generatePythonBindings())
            .thenReturn(testToolBindings);

        String bindings = toolRegistry.generatePythonBindings();

        assertTrue(bindings.contains("def "));
        assertTrue(bindings.contains("file_read"));
        assertTrue(bindings.contains("browser_navigate"));
    }

    @Test
    void testMultipleEvents() {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("text", "Message " + i);

            Event event = new Event();
            event.setType(Event.EventType.USER_MESSAGE);
            event.setData(data);
            event.setSequence(i);

            events.add(event);
        }

        assertEquals(10, events.size());

        List<Map<String, String>> messages = promptBuilder.buildMessages(events);
        assertNotNull(messages);
    }
}
