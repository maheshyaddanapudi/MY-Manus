package ai.mymanus.service.mcp.example;

import ai.mymanus.service.mcp.BaseMCPClient;
import ai.mymanus.service.mcp.MCPClientRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Example Email MCP Client
 *
 * This client connects to an EXTERNAL email MCP server and queries it for available tools.
 * Tools are NOT hardcoded - they are retrieved from the server dynamically.
 *
 * The MCP server would typically run separately and expose:
 * - GET /tools/list - Returns available tools
 * - POST /tools/execute - Executes a tool
 * - GET /health - Health check
 *
 * Configuration:
 * mcp:
 *   enabled: true
 *   clients:
 *     example-email:
 *       enabled: true
 *       server-url: http://email-mcp-server:8080
 *
 * Enable with: mcp.enabled=true and mcp.clients.example-email.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.clients.example-email", name = "enabled", havingValue = "true")
public class ExampleEmailMCPClient extends BaseMCPClient {

    private final MCPClientRegistry registry;

    @Value("${mcp.clients.example-email.server-url}")
    private String serverUrl;

    @PostConstruct
    public void init() {
        // Register this client with the MCP registry
        registry.registerClient(this);
        log.info("Example Email MCP Client initialized and registered (server: {})", serverUrl);
    }

    @Override
    protected String getServerUrl() {
        return serverUrl;
    }

    @Override
    protected String getClientName() {
        return "example-email-client";
    }
}
