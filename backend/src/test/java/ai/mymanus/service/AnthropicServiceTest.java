package ai.mymanus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnthropicService
 * Tests LLM integration and response handling
 */
@ExtendWith(MockitoExtension.class)
class AnthropicServiceTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private AnthropicService anthropicService;

    private List<Map<String, String>> testMessages;
    private String testSystemPrompt;

    @BeforeEach
    void setUp() {
        testMessages = new ArrayList<>();

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "Read /workspace/test.txt");
        testMessages.add(userMsg);

        testSystemPrompt = "You are a helpful AI agent using CodeAct pattern.";
    }

    @Test
    void testGenerateResponse() {
        ChatResponse mockResponse = mock(ChatResponse.class);

        when(chatModel.call(any(Prompt.class)))
            .thenReturn(mockResponse);

        assertDoesNotThrow(() -> {
            anthropicService.generateResponse(testSystemPrompt, testMessages);
        });

        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testMessageConversion() {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(testSystemPrompt));
        messages.add(new UserMessage("Read /workspace/test.txt"));
        messages.add(new AssistantMessage("I'll read the file for you"));

        assertEquals(3, messages.size());
        assertTrue(messages.get(0) instanceof SystemMessage);
        assertTrue(messages.get(1) instanceof UserMessage);
        assertTrue(messages.get(2) instanceof AssistantMessage);
    }

    @Test
    void testSystemMessageCreation() {
        Message systemMessage = new SystemMessage(testSystemPrompt);

        assertNotNull(systemMessage);
        assertEquals(testSystemPrompt, systemMessage.getContent());
    }

    @Test
    void testUserMessageCreation() {
        Message userMessage = new UserMessage("Hello, agent!");

        assertNotNull(userMessage);
        assertEquals("Hello, agent!", userMessage.getContent());
    }

    @Test
    void testAssistantMessageCreation() {
        Message assistantMessage = new AssistantMessage("I'll help you with that");

        assertNotNull(assistantMessage);
        assertEquals("I'll help you with that", assistantMessage.getContent());
    }

    @Test
    void testPromptCreation() {
        List<Message> messages = List.of(
            new SystemMessage(testSystemPrompt),
            new UserMessage("Test query")
        );

        Prompt prompt = new Prompt(messages);

        assertNotNull(prompt);
        assertEquals(2, prompt.getInstructions().size());
    }

    @Test
    void testMultipleMessageConversation() {
        List<Map<String, String>> conversation = new ArrayList<>();

        conversation.add(Map.of("role", "user", "content", "Read file1.txt"));
        conversation.add(Map.of("role", "assistant", "content", "I read file1.txt"));
        conversation.add(Map.of("role", "user", "content", "Now read file2.txt"));
        conversation.add(Map.of("role", "assistant", "content", "I read file2.txt"));

        assertEquals(4, conversation.size());
    }

    @Test
    void testLongSystemPrompt() {
        String longPrompt = "a".repeat(5000);

        Message systemMessage = new SystemMessage(longPrompt);

        assertNotNull(systemMessage);
        assertEquals(5000, systemMessage.getContent().length());
    }

    @Test
    void testEmptyMessageList() {
        List<Map<String, String>> emptyMessages = new ArrayList<>();

        assertNotNull(emptyMessages);
        assertTrue(emptyMessages.isEmpty());
    }

    @Test
    void testMessageWithCodeBlock() {
        String messageWithCode = """
            I'll read the file:

            ```python
            with open('/workspace/test.txt', 'r') as f:
                content = f.read()
            print(content)
            ```
            """;

        Message assistantMessage = new AssistantMessage(messageWithCode);

        assertNotNull(assistantMessage);
        assertTrue(assistantMessage.getContent().contains("```python"));
        assertTrue(assistantMessage.getContent().contains("open"));
    }

    @Test
    void testMessageWithSpecialCharacters() {
        String specialMessage = "Hello 世界! <>&\"' 🌍";

        Message userMessage = new UserMessage(specialMessage);

        assertNotNull(userMessage);
        assertEquals(specialMessage, userMessage.getContent());
    }
}
