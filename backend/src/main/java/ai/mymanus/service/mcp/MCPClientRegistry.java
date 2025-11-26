package ai.mymanus.service.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for MCP (Model Context Protocol) clients
 *
 * Architecture:
 * - MCP clients are registered in the application at startup
 * - Their tools are NOT sent to the LLM context (would bloat context)
 * - SearchToolsTool queries this registry to discover tools on-demand
 * - When agent needs a tool, it's dynamically made available
 *
 * This enables infinite tool scalability without context cost.
 */
@Slf4j
@Service
public class MCPClientRegistry {

    private final Map<String, MCPClient> clients = new ConcurrentHashMap<>();

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    /**
     * Register an MCP client
     *
     * This is called during application startup by any MCP client beans.
     * The client's tools are discovered but NOT sent to the LLM context.
     */
    public void registerClient(MCPClient client) {
        if (!mcpEnabled) {
            log.info("MCP is disabled - skipping client registration: {}", client.getName());
            return;
        }

        clients.put(client.getName(), client);
        log.info("Registered MCP client: {} ({} tools available)",
            client.getName(),
            client.getAvailableTools().size());
    }

    /**
     * Get all registered MCP clients
     */
    public Collection<MCPClient> getAllClients() {
        return clients.values();
    }

    /**
     * Get a specific MCP client by name
     */
    public Optional<MCPClient> getClient(String name) {
        return Optional.ofNullable(clients.get(name));
    }

    /**
     * Get all available tools from all registered MCP clients
     *
     * This is called by SearchToolsTool to discover tools.
     * Returns a flat list of all tools from all clients.
     */
    public List<MCPToolDefinition> getAllAvailableTools() {
        List<MCPToolDefinition> allTools = new ArrayList<>();

        for (MCPClient client : clients.values()) {
            if (client.isAvailable()) {
                try {
                    allTools.addAll(client.getAvailableTools());
                } catch (Exception e) {
                    log.warn("Failed to get tools from MCP client {}: {}",
                        client.getName(), e.getMessage());
                }
            }
        }

        return allTools;
    }

    /**
     * Execute a tool from any registered MCP client
     *
     * @param toolName The name of the tool (e.g., "send_email")
     * @param parameters The parameters to pass to the tool
     * @return The result of the tool execution
     */
    public Map<String, Object> executeTool(String toolName, Map<String, Object> parameters) throws Exception {
        // Find which client provides this tool
        for (MCPClient client : clients.values()) {
            List<MCPToolDefinition> tools = client.getAvailableTools();
            boolean hasToolExecuted = tools.stream()
                .anyMatch(tool -> tool.getName().equals(toolName));

            if (hasToolExecuted) {
                return client.executeTool(toolName, parameters);
            }
        }

        throw new IllegalArgumentException("No MCP client provides tool: " + toolName);
    }

    /**
     * Check if MCP is enabled
     */
    public boolean isEnabled() {
        return mcpEnabled;
    }

    /**
     * Get count of registered clients
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Get total count of available tools across all clients
     */
    public int getTotalToolCount() {
        return clients.values().stream()
            .filter(MCPClient::isAvailable)
            .mapToInt(client -> client.getAvailableTools().size())
            .sum();
    }
}
