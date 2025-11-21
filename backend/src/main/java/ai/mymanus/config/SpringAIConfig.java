package ai.mymanus.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI configuration for ChatClient and chat memory.
 */
@Configuration
public class SpringAIConfig {

    /**
     * Create ChatClient with Anthropic model
     */
    @Bean
    public ChatClient chatClient(AnthropicChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    /**
     * Create JDBC-based chat memory store
     */
    @Bean
    public JdbcChatMemoryStore chatMemoryStore(JdbcTemplate jdbcTemplate) {
        return new JdbcChatMemoryStore(jdbcTemplate);
    }

    /**
     * Create ChatMemory with JDBC store
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryStore chatMemoryStore) {
        return ChatMemory.of(chatMemoryStore);
    }
}
