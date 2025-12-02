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
        when(toolRegistry.getToolDescriptions())
            .thenReturn(testToolBindings);

        String systemPrompt = promptBuilder.buildSystemPrompt(new HashMap<>(), false);

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("Python"));
        assertTrue(systemPrompt.contains("execute"));
        verify(toolRegistry, times(1)).getToolDescriptions();
    }

    @Test
    void testSystemPromptContainsToolBindings() {
        when(toolRegistry.getToolDescriptions())
            .thenReturn(testToolBindings);

        String systemPrompt = promptBuilder.buildSystemPrompt(new HashMap<>(), false);

        assertTrue(systemPrompt.contains("file_read"));
        assertTrue(systemPrompt.contains("browser_navigate"));
    }

    @Test
    void testSystemPromptContainsCodeActInstructions() {
        lenient().when(toolRegistry.getToolDescriptions())
            .thenReturn(testToolBindings);

        String systemPrompt = promptBuilder.buildSystemPrompt(new HashMap<>(), false);

        assertTrue(systemPrompt.contains("Python") || systemPrompt.contains("code"));
        assertTrue(systemPrompt.contains("function") || systemPrompt.contains("tool"));
    }

    @Test
    void testBuildMessages() {
        List<Message> messages = new ArrayList<>();

        // User message
        Message userMessage = new Message();
        userMessage.setRole(Message.MessageRole.USER);
        userMessage.setContent("Read /workspace/test.txt");
        messages.add(userMessage);

        // Assistant message
        Message assistantMessage = new Message();
        assistantMessage.setRole(Message.MessageRole.ASSISTANT);
        assistantMessage.setContent("I need to read the file");
        messages.add(assistantMessage);

        String history = promptBuilder.buildConversationHistory(messages);

        assertNotNull(history);
        assertTrue(history.contains("Read /workspace/test.txt"));
    }

    @Test
    void testBuildMessagesFromEmpty() {
        List<Message> messages = new ArrayList<>();

        String history = promptBuilder.buildConversationHistory(messages);

        assertNotNull(history);
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
    void testMultipleMessages() {
        List<Message> messages = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Message message = new Message();
            message.setRole(i % 2 == 0 ? Message.MessageRole.USER : Message.MessageRole.ASSISTANT);
            message.setContent("Message " + i);
            messages.add(message);
        }

        assertEquals(10, messages.size());

        String history = promptBuilder.buildConversationHistory(messages);
        assertNotNull(history);
        assertTrue(history.contains("Message 0"));
        assertTrue(history.contains("Message 9"));
    }
}
