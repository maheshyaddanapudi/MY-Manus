package ai.mymanus.service.mcp;

import java.util.List;
import java.util.Map;

/**
 * Interface for MCP (Model Context Protocol) clients
 *
 * Each MCP client represents a connection to an external tool provider
 * (email server, calendar API, database, etc.)
 *
 * MCP clients are registered in the application but their tools are NOT
 * automatically sent to the LLM context. Tools are discovered on-demand
 * via the search_tools() function.
 */
public interface MCPClient {

    /**
     * Get the name of this MCP client
     * Example: "email-client", "calendar-client", "database-client"
     */
    String getName();

    /**
     * Get all tools provided by this MCP client
     *
     * This is called by SearchToolsTool to discover available tools.
     * The tools are NOT automatically registered with the LLM.
     */
    List<MCPToolDefinition> getAvailableTools();

    /**
     * Execute a tool provided by this MCP client
     *
     * Called when the agent decides to use a discovered tool.
     *
     * @param toolName The name of the tool to execute
     * @param parameters The parameters to pass to the tool
     * @return The result of the tool execution
     */
    Map<String, Object> executeTool(String toolName, Map<String, Object> parameters) throws Exception;

    /**
     * Check if this MCP client is currently available/healthy
     */
    boolean isAvailable();
}
