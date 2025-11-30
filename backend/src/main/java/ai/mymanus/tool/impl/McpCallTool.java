package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for executing MCP (Model Context Protocol) tools from external servers.
 * 
 * This tool acts as a bridge between the CodeAct Python execution environment
 * and Spring AI's MCP client infrastructure. It allows the LLM to execute
 * MCP tools discovered via search_tools().
 * 
 * Workflow:
 * 1. LLM calls search_tools(query="send email") to discover available tools
 * 2. LLM calls mcp_call(tool_name="send_email", to="john@example.com", ...)
 * 3. This tool finds the MCP client, executes the tool, and returns the result
 */
@Slf4j
@Component
public class McpCallTool implements Tool {

    @Autowired(required = false)
    private Map<String, McpSyncClient> syncClients;

    @Autowired(required = false)
    private Map<String, McpAsyncClient> asyncClients;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "mcp_call";
    }

    @Override
    public String getDescription() {
        return """
            Execute an MCP (Model Context Protocol) tool from an external server.
            
            Use this after discovering tools with search_tools(). This tool acts as a bridge
            to execute remote MCP tools and return their results.
            
            Parameters:
            - tool_name: The exact name of the MCP tool to execute (from search_tools results)
            - **kwargs: All other parameters are passed to the MCP tool as arguments
            
            Example workflow:
                # 1. First, discover available tools
                tools = search_tools(query="send email")
                print(tools)  # Shows: send_email tool is available
                
                # 2. Then, execute the tool
                result = mcp_call(
                    tool_name="send_email",
                    to="john@example.com",
                    subject="Meeting Tomorrow",
                    body="Let's meet at 10am"
                )
                print(result)  # Shows: Email sent successfully
            
            Returns: The result from the MCP tool execution (usually a dict with success/error info)
            """;
    }

    @Override
    public String getPythonSignature() {
        return "mcp_call(tool_name: str, **kwargs) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        // Extract tool name
        String toolName = (String) parameters.get("tool_name");
        
        if (toolName == null || toolName.isBlank()) {
            return Map.of(
                "success", false,
                "error", "tool_name parameter is required"
            );
        }

        // Extract tool arguments (everything except tool_name)
        Map<String, Object> toolArgs = new HashMap<>(parameters);
        toolArgs.remove("tool_name");

        log.info("🔧 Executing MCP tool: {} with args: {}", toolName, toolArgs);

        // Try to find and execute the tool from sync clients first
        if (syncClients != null && !syncClients.isEmpty()) {
            for (Map.Entry<String, McpSyncClient> entry : syncClients.entrySet()) {
                String serverName = entry.getKey();
                McpSyncClient client = entry.getValue();
                
                try {
                    // Check if this client has the tool
                    var toolsResult = client.listTools();
                    boolean hasTool = toolsResult.tools().stream()
                            .anyMatch(t -> t.name().equals(toolName));
                    
                    if (hasTool) {
                        log.info("📡 Found tool '{}' on sync MCP server: {}", toolName, serverName);
                        
                        // Create CallToolRequest with tool name and arguments map
                        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, toolArgs);
                        
                        // Execute the tool
                        var result = client.callTool(request);
                        
                        log.info("✅ MCP tool '{}' executed successfully", toolName);
                        
                        // Parse and return the result
                        return Map.of(
                            "success", true,
                            "result", result.content() != null ? result.content() : "Tool executed successfully",
                            "mcp_server", serverName,
                            "tool_name", toolName
                        );
                    }
                } catch (Exception e) {
                    log.warn("Error checking/executing tool '{}' on sync server '{}': {}", 
                            toolName, serverName, e.getMessage());
                }
            }
        }

        // Try async clients if not found in sync clients
        if (asyncClients != null && !asyncClients.isEmpty()) {
            for (Map.Entry<String, McpAsyncClient> entry : asyncClients.entrySet()) {
                String serverName = entry.getKey();
                McpAsyncClient client = entry.getValue();
                
                try {
                    // Check if this client has the tool
                    var toolsResult = client.listTools().block();
                    boolean hasTool = toolsResult.tools().stream()
                            .anyMatch(t -> t.name().equals(toolName));
                    
                    if (hasTool) {
                        log.info("📡 Found tool '{}' on async MCP server: {}", toolName, serverName);
                        
                        // Create CallToolRequest with tool name and arguments map
                        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, toolArgs);
                        
                        // Execute the tool
                        var result = client.callTool(request).block();
                        
                        log.info("✅ MCP tool '{}' executed successfully", toolName);
                        
                        // Parse and return the result
                        return Map.of(
                            "success", true,
                            "result", result.content() != null ? result.content() : "Tool executed successfully",
                            "mcp_server", serverName,
                            "tool_name", toolName
                        );
                    }
                } catch (Exception e) {
                    log.warn("Error checking/executing tool '{}' on async server '{}': {}", 
                            toolName, serverName, e.getMessage());
                }
            }
        }

        // Tool not found on any MCP server
        log.error("❌ MCP tool '{}' not found on any configured server", toolName);
        
        int totalServers = (syncClients != null ? syncClients.size() : 0) + 
                          (asyncClients != null ? asyncClients.size() : 0);
        
        return Map.of(
            "success", false,
            "error", String.format("MCP tool '%s' not found on any of the %d configured MCP servers. " +
                    "Use search_tools() to discover available tools.", toolName, totalServers),
            "tool_name", toolName,
            "servers_checked", totalServers
        );
    }

    @Override
    public boolean requiresNetwork() {
        return true;  // MCP tools require network access
    }
}
