package ai.mymanus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
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
    private ChatModel chatModel;

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
    }

    @Test
    void testGenerate() {
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("I'll read the file for you");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generate(testConversationId, testSystemPrompt, testUserMessage);

        assertNotNull(response);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testGenerateWithEmptyMessage() {
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generate(testConversationId, testSystemPrompt, "");

        assertNotNull(response);
    }

    @Test
    void testGenerateWithLongMessage() {
        String longMessage = "a".repeat(1000);
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Response to long message");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generate(testConversationId, testSystemPrompt, longMessage);

        assertNotNull(response);
    }

    @Test
    void testGenerateStream() {
        Flux<ChatResponse> mockFlux = Flux.empty();
        
        when(chatModel.stream(any(Prompt.class))).thenReturn(mockFlux);

        Flux<String> responseStream = anthropicService.generateStream(
            testConversationId, 
            testSystemPrompt, 
            testUserMessage
        );

        assertNotNull(responseStream);
        verify(chatModel, times(1)).stream(any(Prompt.class));
    }

    @Test
    void testGenerateStreamWithMultipleChunks() {
        ChatResponse response1 = mock(ChatResponse.class);
        ChatResponse response2 = mock(ChatResponse.class);
        Generation gen1 = mock(Generation.class);
        Generation gen2 = mock(Generation.class);
        
        when(gen1.getOutput()).thenReturn(new AssistantMessage("Hello "));
        when(gen2.getOutput()).thenReturn(new AssistantMessage("World"));
        when(response1.getResults()).thenReturn(List.of(gen1));
        when(response2.getResults()).thenReturn(List.of(gen2));

        Flux<ChatResponse> mockFlux = Flux.just(response1, response2);
        when(chatModel.stream(any(Prompt.class))).thenReturn(mockFlux);

        Flux<String> responseStream = anthropicService.generateStream(
            testConversationId,
            testSystemPrompt,
            testUserMessage
        );

        assertNotNull(responseStream);
    }

    @Test
    void testGenerateWithTemplate() {
        Map<String, Object> params = new HashMap<>();
        params.put("filename", "test.txt");
        params.put("action", "read");

        String template = "Please {action} the file {filename}";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("I'll read test.txt");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generateWithTemplate(testConversationId, template, params);

        assertNotNull(response);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testGenerateWithEmptyTemplate() {
        Map<String, Object> params = new HashMap<>();
        String template = "";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Response");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generateWithTemplate(testConversationId, template, params);

        assertNotNull(response);
    }

    @Test
    void testGenerateSimple() {
        String simplePrompt = "What is 2+2?";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("4");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generateSimple(simplePrompt);

        assertNotNull(response);
        assertEquals("4", response);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testGenerateSimpleWithComplexPrompt() {
        String complexPrompt = "Explain quantum computing in simple terms";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Quantum computing uses quantum mechanics...");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generateSimple(complexPrompt);

        assertNotNull(response);
        assertTrue(response.contains("Quantum"));
    }

    @Test
    void testGenerateWithDifferentConversationIds() {
        String conversationId1 = "conv-1";
        String conversationId2 = "conv-2";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Response");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response1 = anthropicService.generate(conversationId1, testSystemPrompt, testUserMessage);
        String response2 = anthropicService.generate(conversationId2, testSystemPrompt, testUserMessage);

        assertNotNull(response1);
        assertNotNull(response2);
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void testGenerateWithDifferentSystemPrompts() {
        String systemPrompt1 = "You are a helpful assistant";
        String systemPrompt2 = "You are a code expert";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Response");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response1 = anthropicService.generate(testConversationId, systemPrompt1, testUserMessage);
        String response2 = anthropicService.generate(testConversationId, systemPrompt2, testUserMessage);

        assertNotNull(response1);
        assertNotNull(response2);
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void testServiceDependencies() {
        assertNotNull(anthropicService);
        // Verify service can be instantiated with dependencies
    }

    @Test
    void testChatModelInteraction() {
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Test response");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        anthropicService.generate(testConversationId, testSystemPrompt, testUserMessage);

        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testMultipleGenerateCalls() {
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Response");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        anthropicService.generate(testConversationId, testSystemPrompt, "Message 1");
        anthropicService.generate(testConversationId, testSystemPrompt, "Message 2");
        anthropicService.generate(testConversationId, testSystemPrompt, "Message 3");

        verify(chatModel, times(3)).call(any(Prompt.class));
    }

    @Test
    void testGenerateWithSpecialCharacters() {
        String specialMessage = "Test with special chars: @#$%^&*()";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Handled special chars");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generate(testConversationId, testSystemPrompt, specialMessage);

        assertNotNull(response);
    }

    @Test
    void testGenerateWithUnicodeCharacters() {
        String unicodeMessage = "Test with unicode: 你好世界 🌍";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockMessage = new AssistantMessage("Handled unicode");

        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String response = anthropicService.generate(testConversationId, testSystemPrompt, unicodeMessage);

        assertNotNull(response);
    }
}
