package ai.mymanus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.StreamResponseSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import java.util.function.Consumer;

import java.util.HashMap;
import java.util.Map;

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
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec responseSpec;

    @InjectMocks
    private AnthropicService anthropicService;

    private String testConversationId;
    private String testSystemPrompt;
    private String testUserMessage;

    @BeforeEach
    void setUp() {
        testConversationId = "test-conversation-123";
        testSystemPrompt = "You are a helpful AI agent using CodeAct pattern.";
        testUserMessage = "Read /workspace/test.txt";

        // Setup fluent API chain - lenient because not all tests use all methods
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.system(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(responseSpec);
    }

    @Test
    void testGenerate() {
        when(responseSpec.content()).thenReturn("I'll read the file for you");

        String response = anthropicService.generate(testConversationId, testSystemPrompt, testUserMessage);

        assertNotNull(response);
        assertEquals("I'll read the file for you", response);
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void testGenerateWithEmptyMessage() {
        when(responseSpec.content()).thenReturn("Please provide a message");

        String response = anthropicService.generate(testConversationId, testSystemPrompt, "");

        assertNotNull(response);
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void testGenerateWithLongMessage() {
        String longMessage = "a".repeat(1000);
        when(responseSpec.content()).thenReturn("Processed long message");

        String response = anthropicService.generate(testConversationId, testSystemPrompt, longMessage);

        assertNotNull(response);
        assertEquals("Processed long message", response);
    }

    @Test
    void testGenerateWithSpecialCharacters() {
        String specialMessage = "Test with special chars: @#$%^&*()";
        when(responseSpec.content()).thenReturn("Handled special characters");

        String response = anthropicService.generate(testConversationId, testSystemPrompt, specialMessage);

        assertNotNull(response);
        assertEquals("Handled special characters", response);
    }

    @Test
    void testGenerateWithUnicodeCharacters() {
        String unicodeMessage = "Test with unicode: 你好世界 🌍";
        when(responseSpec.content()).thenReturn("Handled unicode");

        String response = anthropicService.generate(testConversationId, testSystemPrompt, unicodeMessage);

        assertNotNull(response);
        assertEquals("Handled unicode", response);
    }

    @Test
    void testGenerateWithDifferentConversationIds() {
        when(responseSpec.content()).thenReturn("Response 1");
        String response1 = anthropicService.generate("conv-1", testSystemPrompt, testUserMessage);

        when(responseSpec.content()).thenReturn("Response 2");
        String response2 = anthropicService.generate("conv-2", testSystemPrompt, testUserMessage);

        assertNotNull(response1);
        assertNotNull(response2);
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void testGenerateWithDifferentSystemPrompts() {
        when(responseSpec.content()).thenReturn("Response with prompt 1");
        String response1 = anthropicService.generate(testConversationId, "Prompt 1", testUserMessage);

        when(responseSpec.content()).thenReturn("Response with prompt 2");
        String response2 = anthropicService.generate(testConversationId, "Prompt 2", testUserMessage);

        assertNotNull(response1);
        assertNotNull(response2);
    }

    @Test
    void testMultipleGenerateCalls() {
        when(responseSpec.content())
            .thenReturn("Response 1")
            .thenReturn("Response 2")
            .thenReturn("Response 3");

        String response1 = anthropicService.generate(testConversationId, testSystemPrompt, "Message 1");
        String response2 = anthropicService.generate(testConversationId, testSystemPrompt, "Message 2");
        String response3 = anthropicService.generate(testConversationId, testSystemPrompt, "Message 3");

        assertEquals("Response 1", response1);
        assertEquals("Response 2", response2);
        assertEquals("Response 3", response3);
        verify(chatClient, times(3)).prompt();
    }

    @Test
    void testGenerateSimple() {
        when(responseSpec.content()).thenReturn("Simple response");

        String response = anthropicService.generateSimple(testUserMessage);

        assertNotNull(response);
        assertEquals("Simple response", response);
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void testGenerateSimpleWithComplexPrompt() {
        String complexPrompt = "Analyze this code:\n```python\ndef hello():\n    print('world')\n```";
        when(responseSpec.content()).thenReturn("Code analysis result");

        String response = anthropicService.generateSimple(complexPrompt);

        assertNotNull(response);
        assertEquals("Code analysis result", response);
    }

    @Test
    void testGenerateWithTemplate() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "John");
        params.put("action", "read file");

        when(responseSpec.content()).thenReturn("Template response");

        String response = anthropicService.generateWithTemplate(
            testConversationId,
            "User {name} wants to {action}",
            params
        );

        assertNotNull(response);
        assertEquals("Template response", response);
    }

    @Test
    void testGenerateWithEmptyTemplate() {
        Map<String, Object> params = new HashMap<>();

        // Empty template should throw RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            anthropicService.generateWithTemplate(
                testConversationId,
                "",
                params
            );
        });

        assertTrue(exception.getMessage().contains("Failed to generate response"));
    }

    @Test
    void testGenerateStream() {
        // Mock the complete stream chain
        StreamResponseSpec streamSpec = mock(StreamResponseSpec.class);
        Flux<String> mockFlux = Flux.just("chunk1", "chunk2", "chunk3");
        
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(mockFlux);

        Flux<String> result = anthropicService.generateStream(testConversationId, testSystemPrompt, testUserMessage);

        assertNotNull(result);
        assertEquals(3, result.collectList().block().size());
    }

    @Test
    void testGenerateStreamWithMultipleChunks() {
        StreamResponseSpec streamSpec = mock(StreamResponseSpec.class);
        Flux<String> mockFlux = Flux.just("Hello", " ", "World", "!");
        
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(mockFlux);

        Flux<String> result = anthropicService.generateStream(testConversationId, testSystemPrompt, "Say hello");

        assertNotNull(result);
        assertEquals(4, result.collectList().block().size());
    }

    @Test
    void testChatModelInteraction() {
        when(responseSpec.content()).thenReturn("Model interaction successful");

        String response = anthropicService.generate(testConversationId, testSystemPrompt, testUserMessage);

        assertNotNull(response);
        verify(requestSpec).system(testSystemPrompt);
        verify(requestSpec).user(testUserMessage);
        verify(requestSpec).call();
    }
}
