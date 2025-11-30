package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for searching and discovering MCP tools from external servers.
 * Performs semantic search over available MCP tools and returns top-k matches.
 * 
 * This tool enables lazy loading of MCP tools - instead of exposing all MCP tools
 * to the LLM upfront, the LLM can search for relevant tools on-demand.
 */
@Slf4j
@Component
public class SearchToolsTool implements Tool {

    @Autowired(required = false)
    private Map<String, McpSyncClient> syncClients;

    @Autowired(required = false)
    private Map<String, McpAsyncClient> asyncClients;

    @Override
    public String getName() {
        return "search_tools";
    }

    @Override
    public String getDescription() {
        return """
            Search for additional tools from external MCP (Model Context Protocol) servers.
            
            Use this when you need capabilities beyond the built-in tools. For example:
            - Email operations: search_tools(query="send email")
            - Calendar operations: search_tools(query="schedule meeting")
            - Database operations: search_tools(query="query database")
            
            Returns a list of matching tools with their names, descriptions, and parameter schemas.
            After finding a tool, use mcp_call() to execute it.
            
            Example workflow:
                # 1. Search for tools
                tools = search_tools(query="send email", top_k=3)
                print(tools)  # Shows available email tools
                
                # 2. Execute the tool (use mcp_call)
                result = mcp_call(tool_name="send_email", to="john@example.com", ...)
            """;
    }

    @Override
    public String getPythonSignature() {
        return "search_tools(query: str, top_k: int = 5) -> str";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String query = (String) parameters.get("query");
        Integer topK = parameters.containsKey("top_k") ? 
                ((Number) parameters.get("top_k")).intValue() : 5;

        if (query == null || query.isBlank()) {
            return Map.of(
                "success", false,
                "error", "Query parameter is required"
            );
        }

        log.info("🔍 Searching MCP tools for query: '{}' (top_k={})", query, topK);

        // Collect all available MCP tools
        List<ToolInfo> allTools = new ArrayList<>();

        // Collect from sync clients
        if (syncClients != null && !syncClients.isEmpty()) {
            log.debug("Collecting tools from {} sync MCP clients", syncClients.size());
            for (Map.Entry<String, McpSyncClient> entry : syncClients.entrySet()) {
                String serverName = entry.getKey();
                McpSyncClient client = entry.getValue();
                
                try {
                    var toolsResult = client.listTools();
                    if (toolsResult != null && toolsResult.tools() != null) {
                        for (var tool : toolsResult.tools()) {
                            allTools.add(new ToolInfo(
                                serverName,
                                tool.name(),
                                tool.description() != null ? tool.description() : "",
                                tool.inputSchema() != null ? tool.inputSchema().toString() : "{}"
                            ));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error listing tools from sync client '{}': {}", serverName, e.getMessage());
                }
            }
        }

        // Collect from async clients
        if (asyncClients != null && !asyncClients.isEmpty()) {
            log.debug("Collecting tools from {} async MCP clients", asyncClients.size());
            for (Map.Entry<String, McpAsyncClient> entry : asyncClients.entrySet()) {
                String serverName = entry.getKey();
                McpAsyncClient client = entry.getValue();
                
                try {
                    var toolsResult = client.listTools().block();
                    if (toolsResult != null && toolsResult.tools() != null) {
                        for (var tool : toolsResult.tools()) {
                            allTools.add(new ToolInfo(
                                serverName,
                                tool.name(),
                                tool.description() != null ? tool.description() : "",
                                tool.inputSchema() != null ? tool.inputSchema().toString() : "{}"
                            ));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error listing tools from async client '{}': {}", serverName, e.getMessage());
                }
            }
        }

        if (allTools.isEmpty()) {
            log.info("ℹ️ No MCP tools available");
            return Map.of(
                "success", true,
                "result", "No MCP tools are currently available. Make sure MCP servers are configured in application.yml."
            );
        }

        log.info("📋 Found {} total MCP tools", allTools.size());

        // Perform semantic search (simple keyword matching for now)
        List<ToolInfo> matchingTools = performSemanticSearch(allTools, query, topK);

        // Format results
        String formattedResults = formatResults(matchingTools, query);

        log.info("✅ Returning {} matching tools", matchingTools.size());

        return Map.of(
            "success", true,
            "result", formattedResults,
            "tool_count", matchingTools.size()
        );
    }

    /**
     * Perform semantic search over tools (simple keyword-based for now)
     */
    private List<ToolInfo> performSemanticSearch(List<ToolInfo> tools, String query, int topK) {
        String queryLower = query.toLowerCase();
        
        return tools.stream()
                .map(tool -> {
                    // Calculate relevance score
                    int score = 0;
                    String nameLower = tool.name.toLowerCase();
                    String descLower = tool.description.toLowerCase();
                    
                    // Exact name match gets highest score
                    if (nameLower.equals(queryLower)) {
                        score += 100;
                    }
                    
                    // Name contains query
                    if (nameLower.contains(queryLower)) {
                        score += 50;
                    }
                    
                    // Description contains query
                    if (descLower.contains(queryLower)) {
                        score += 25;
                    }
                    
                    // Word-level matching
                    String[] queryWords = queryLower.split("\\s+");
                    for (String word : queryWords) {
                        if (nameLower.contains(word)) {
                            score += 10;
                        }
                        if (descLower.contains(word)) {
                            score += 5;
                        }
                    }
                    
                    return new ScoredTool(tool, score);
                })
                .filter(scored -> scored.score > 0)  // Only include tools with some relevance
                .sorted((a, b) -> Integer.compare(b.score, a.score))  // Sort by score descending
                .limit(topK)
                .map(scored -> scored.tool)
                .collect(Collectors.toList());
    }

    /**
     * Format search results as human-readable text
     */
    private String formatResults(List<ToolInfo> tools, String query) {
        if (tools.isEmpty()) {
            return String.format("No MCP tools found matching query: '%s'", query);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d MCP tool(s) matching '%s':\n\n", tools.size(), query));

        for (int i = 0; i < tools.size(); i++) {
            ToolInfo tool = tools.get(i);
            sb.append(String.format("%d. %s (from %s)\n", i + 1, tool.name, tool.serverName));
            sb.append(String.format("   Description: %s\n", tool.description));
            sb.append(String.format("   Parameters: %s\n", tool.inputSchema));
            if (i < tools.size() - 1) {
                sb.append("\n");
            }
        }

        sb.append("\nTo use a tool, call: mcp_call(tool_name=\"<name>\", <parameters>)");

        return sb.toString();
    }

    @Override
    public boolean requiresNetwork() {
        return true;  // MCP tools require network access
    }

    /**
     * Tool information
     */
    private static class ToolInfo {
        final String serverName;
        final String name;
        final String description;
        final String inputSchema;

        ToolInfo(String serverName, String name, String description, String inputSchema) {
            this.serverName = serverName;
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }
    }

    /**
     * Tool with relevance score
     */
    private static class ScoredTool {
        final ToolInfo tool;
        final int score;

        ScoredTool(ToolInfo tool, int score) {
            this.tool = tool;
            this.score = score;
        }
    }
}
