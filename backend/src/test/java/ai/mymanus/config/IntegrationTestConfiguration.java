package ai.mymanus.config;

import ai.mymanus.service.AnthropicService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test configuration for integration tests.
 * Provides mock beans for external services.
 */
@TestConfiguration
@Profile("test")
public class IntegrationTestConfiguration {

    @Bean
    @Primary
    public AnthropicService mockAnthropicService() {
        AnthropicService mock = Mockito.mock(AnthropicService.class);
        
        // Mock generate method
        when(mock.generate(anyString(), anyString(), anyString()))
            .thenReturn("I'll help you with that. Let me calculate 2 + 2.");
        
        // Mock generateStream method
        when(mock.generateStream(anyString(), anyString(), anyString()))
            .thenReturn(Flux.just("I'll help ", "you calculate ", "2 + 2.\n\n", 
                "<execute>\nresult = 2 + 2\nprint(result)\n</execute>"));
        
        // Mock generateSimple method
        when(mock.generateSimple(anyString()))
            .thenReturn("Mock response");
        
        return mock;
    }
}
