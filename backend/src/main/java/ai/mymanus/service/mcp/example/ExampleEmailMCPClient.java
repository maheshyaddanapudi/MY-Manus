package ai.mymanus.service.mcp.example;

import ai.mymanus.service.mcp.MCPClient;
import ai.mymanus.service.mcp.MCPClientRegistry;
import ai.mymanus.service.mcp.MCPToolDefinition;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Example Email MCP Client
 *
 * This is an example implementation showing how to create an MCP client.
 * In a real application, this would connect to an actual email service API.
 *
 * Enable with: mcp.enabled=true and mcp.clients.example-email.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.clients.example-email", name = "enabled", havingValue = "true")
public class ExampleEmailMCPClient implements MCPClient {

    private final MCPClientRegistry registry;

    @PostConstruct
    public void init() {
        // Register this client with the MCP registry
        registry.registerClient(this);
        log.info("Example Email MCP Client initialized and registered");
    }

    @Override
    public String getName() {
        return "example-email-client";
    }

    @Override
    public List<MCPToolDefinition> getAvailableTools() {
        return List.of(
            new MCPToolDefinition(
                "send_email",
                "Send an email message to one or more recipients",
                "send_email(to: str, subject: str, body: str, cc: str = None, bcc: str = None) -> dict",
                getName(),
                "Example email tool"
            ),
            new MCPToolDefinition(
                "read_inbox",
                "Read emails from inbox with optional filtering",
                "read_inbox(folder: str = 'INBOX', limit: int = 10, unread_only: bool = False) -> list",
                getName(),
                "Example email tool"
            ),
            new MCPToolDefinition(
                "search_emails",
                "Search emails by subject, sender, or content",
                "search_emails(query: str, folder: str = 'ALL', limit: int = 20) -> list",
                getName(),
                "Example email tool"
            ),
            new MCPToolDefinition(
                "delete_email",
                "Delete an email by message ID",
                "delete_email(message_id: str) -> dict",
                getName(),
                "Example email tool"
            )
        );
    }

    @Override
    public Map<String, Object> executeTool(String toolName, Map<String, Object> parameters) throws Exception {
        log.info("Executing email tool: {} with params: {}", toolName, parameters);

        // In a real implementation, this would connect to an actual email service
        // For this example, we'll just return simulated responses

        return switch (toolName) {
            case "send_email" -> {
                String to = (String) parameters.get("to");
                String subject = (String) parameters.get("subject");
                yield Map.of(
                    "success", true,
                    "message", "Email sent to " + to,
                    "message_id", "example-" + UUID.randomUUID()
                );
            }
            case "read_inbox" -> {
                int limit = parameters.containsKey("limit") ?
                    ((Number) parameters.get("limit")).intValue() : 10;
                yield Map.of(
                    "success", true,
                    "emails", List.of(
                        Map.of("from", "user@example.com", "subject", "Example Email 1"),
                        Map.of("from", "admin@example.com", "subject", "Example Email 2")
                    ),
                    "total", 2
                );
            }
            case "search_emails" -> {
                String query = (String) parameters.get("query");
                yield Map.of(
                    "success", true,
                    "query", query,
                    "results", List.of(
                        Map.of("subject", "Matching email", "from", "sender@example.com")
                    ),
                    "total", 1
                );
            }
            case "delete_email" -> {
                String messageId = (String) parameters.get("message_id");
                yield Map.of(
                    "success", true,
                    "message", "Deleted email " + messageId
                );
            }
            default -> throw new IllegalArgumentException("Unknown email tool: " + toolName);
        };
    }

    @Override
    public boolean isAvailable() {
        // In a real implementation, check connection to email service
        return true;
    }
}
