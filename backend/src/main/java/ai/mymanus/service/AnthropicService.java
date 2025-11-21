package ai.mymanus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Service for interacting with Anthropic Claude via Spring AI ChatClient.
 * Uses JDBC chat memory for conversation history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnthropicService {

    private final ChatClient chatClient;

    /**
     * Generate a response using ChatClient with conversation ID
     */
    public String generate(String conversationId, String systemPrompt, String userMessage) {
        try {
            log.info("Generating response for conversation: {}", conversationId);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .advisors(advisor -> advisor.param("conversationId", conversationId))
                    .call()
                    .content();

            log.info("Generated response: {} chars", response.length());
            return response;

        } catch (Exception e) {
            log.error("Error calling Anthropic API", e);
            throw new RuntimeException("Failed to generate response from Claude: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a streaming response
     */
    public Flux<String> generateStream(String conversationId, String systemPrompt, String userMessage) {
        try {
            log.info("Starting stream for conversation: {}", conversationId);

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .advisors(advisor -> advisor.param("conversationId", conversationId))
                    .stream()
                    .content()
                    .doOnNext(chunk -> log.debug("Received chunk: {} chars", chunk.length()))
                    .doOnError(error -> log.error("Streaming error", error))
                    .doOnComplete(() -> log.info("Streaming complete for conversation: {}", conversationId));

        } catch (Exception e) {
            log.error("Error initiating stream", e);
            return Flux.error(new RuntimeException("Failed to stream response: " + e.getMessage(), e));
        }
    }

    /**
     * Generate with template and parameters
     */
    public String generateWithTemplate(String conversationId, String template, Map<String, Object> params) {
        try {
            PromptTemplate promptTemplate = new PromptTemplate(template);

            return chatClient.prompt(promptTemplate.create(params))
                    .advisors(advisor -> advisor.param("conversationId", conversationId))
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating with template", e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    /**
     * Simple generation for testing (no conversation history)
     */
    public String generateSimple(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error in simple generation", e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }
}
