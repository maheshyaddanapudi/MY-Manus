package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for searching external MCP (Model Context Protocol) tools
 *
 * This tool uses Spring AI's built-in MCP client support to discover tools from
 * external MCP servers configured in application.yml.
 *
 * Architecture:
 * - Core 22 infrastructure tools: ALWAYS pre-registered with LLM (always in context)
 * - MCP tools: Registered in Spring AI's MCP registry, NOT sent to LLM context
 * - SearchToolsTool: Discovers MCP tools on-demand via intelligent local search
 *
 * Configuration (application.yml):
 * spring:
 *   ai:
 *     mcp:
 *       enabled: true
 *       servers:
 *         email:
 *           url: http://email-mcp-server:8080
 *         calendar:
 *           url: http://calendar-mcp-server:8080
 *
 * How it works:
 * 1. Spring AI auto-discovers and registers MCP servers at startup
 * 2. When agent calls search_tools(query), we query Spring AI's MCP registry
 * 3. Perform intelligent keyword matching locally
 * 4. Return top-k most relevant tools
 * 5. When agent uses a tool, Spring AI handles execution seamlessly
 *
 * Example:
 * search_tools(query="send email", top_k=3)
 * # Returns: send_email, read_inbox, search_emails from email MCP server
 *
 * Benefits:
 * - Zero context cost for unused MCP tools
 * - Leverage Spring AI's robust MCP implementation
 * - No custom MCP client code needed
 * - Configuration-driven server registration
 */
@Slf4j
@Component
public class SearchToolsTool implements Tool {

    @Autowired(required = false)
    private List<McpClient> mcpClients;

    @Override
    public String getName() {
        return "search_tools";
    }

    @Override
    public String getDescription() {
        return """
            Search for additional tools from external MCP (Model Context Protocol) servers.
            Use this when you need capabilities beyond the core infrastructure tools.

            The 22 core infrastructure tools (file ops, browser, shell, etc.) are always available
            and do NOT need to be searched. This tool discovers EXTERNAL tools from MCP servers.

            Parameters:
            - query: Natural language description of needed capability (e.g., "send email", "book flight")
            - top_k: Number of tools to return (default: 5, max: 10)

            Returns: List of available MCP tools with their descriptions and signatures

            Examples:
            search_tools(query="send email", top_k=3)
            # Returns: send_email, read_inbox, search_emails from Email MCP Server

            search_tools(query="flight booking")
            # Returns: search_flights, book_flight, cancel_booking from Travel MCP Server

            search_tools(query="database query")
            # Returns: query_postgres, run_migration, backup_db from Database MCP Server

            Note: Returns empty list if no MCP servers are configured.
            Core infrastructure tools are always available without searching.
            """;
    }

    @Override
    public String getPythonSignature() {
        return "search_tools(query: str, top_k: int = 5) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        int topK = params.containsKey("top_k") ?
            Math.min(((Number) params.get("top_k")).intValue(), 10) : 5;

        log.info("🔍 Searching MCP tools: query='{}', top_k={}", query, topK);

        // Check if Spring AI MCP clients are available
        if (mcpClients == null || mcpClients.isEmpty()) {
            log.info("No MCP clients configured - returning empty list");
            return buildEmptyResponse(query,
                "No MCP servers configured. Configure MCP servers in application.yml under spring.ai.mcp.servers");
        }

        // Collect all tools from all MCP clients
        List<McpToolInfo> allMcpTools = new ArrayList<>();
        for (McpClient client : mcpClients) {
            try {
                // Query the MCP server for its tools
                List<McpSchema.Tool> tools = client.listTools();
                log.debug("Found {} tools from MCP client: {}", tools.size(), client.getName());

                // Convert to our internal format for scoring
                for (McpSchema.Tool tool : tools) {
                    allMcpTools.add(new McpToolInfo(
                        tool.name(),
                        tool.description() != null ? tool.description() : "",
                        tool.inputSchema() != null ? formatSchema(tool.inputSchema()) : "",
                        client.getName()
                    ));
                }
            } catch (Exception e) {
                log.warn("Failed to get tools from MCP client {}: {}", client.getName(), e.getMessage());
            }
        }

        if (allMcpTools.isEmpty()) {
            log.info("No MCP tools available from {} clients", mcpClients.size());
            return buildEmptyResponse(query, "No tools available from MCP servers");
        }

        log.debug("Searching across {} MCP tools from {} clients", allMcpTools.size(), mcpClients.size());

        // Score and rank tools by relevance to query
        List<ScoredTool> scoredTools = allMcpTools.stream()
            .map(tool -> new ScoredTool(tool, calculateRelevance(tool, query)))
            .filter(st -> st.score > 0) // Only include tools with positive relevance
            .sorted(Comparator.comparingDouble(ScoredTool::score).reversed())
            .limit(topK)
            .toList();

        // Build response
        List<Map<String, String>> matchingTools = scoredTools.stream()
            .map(st -> {
                Map<String, String> toolInfo = new HashMap<>();
                toolInfo.put("name", st.tool.name);
                toolInfo.put("description", st.tool.description);
                toolInfo.put("signature", st.tool.signature);
                toolInfo.put("mcp_server", st.tool.mcpServer);
                toolInfo.put("relevance_score", String.format("%.2f", st.score));
                return toolInfo;
            })
            .collect(Collectors.toList());

        log.info("✅ Found {} matching MCP tools (top score: {})",
            matchingTools.size(),
            scoredTools.isEmpty() ? 0 : String.format("%.2f", scoredTools.get(0).score));

        return Map.of(
            "success", true,
            "query", query,
            "tools_found", matchingTools.size(),
            "total_mcp_tools", allMcpTools.size(),
            "mcp_servers_available", mcpClients.size(),
            "tools", matchingTools
        );
    }

    /**
     * Build empty response when no tools are available
     */
    private Map<String, Object> buildEmptyResponse(String query, String message) {
        int serverCount = mcpClients != null ? mcpClients.size() : 0;
        return Map.of(
            "success", true,
            "query", query,
            "tools_found", 0,
            "total_mcp_tools", 0,
            "mcp_servers_available", serverCount,
            "tools", Collections.emptyList(),
            "message", message
        );
    }

    /**
     * Format MCP input schema into a function signature
     */
    private String formatSchema(McpSchema.ToolInputSchema schema) {
        try {
            // Extract parameters from schema
            Map<String, Object> properties = schema.properties();
            List<String> required = schema.required() != null ? schema.required() : List.of();

            if (properties == null || properties.isEmpty()) {
                return "() -> dict";
            }

            // Build parameter list
            List<String> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String paramName = entry.getKey();
                boolean isRequired = required.contains(paramName);

                if (isRequired) {
                    params.add(paramName + ": str");
                } else {
                    params.add(paramName + ": str = None");
                }
            }

            return "(" + String.join(", ", params) + ") -> dict";
        } catch (Exception e) {
            return "(...) -> dict";
        }
    }

    /**
     * Calculate relevance score between query and MCP tool
     *
     * Uses intelligent keyword matching with term frequency weighting:
     * - Name match: 10x weight (highest priority)
     * - Signature match: 5x weight (high priority)
     * - Description match: 2x weight (medium priority)
     * - Phrase match bonus: 15x for exact multi-word phrase matches
     * - Normalized by query length for fair comparison
     */
    private double calculateRelevance(McpToolInfo tool, String query) {
        String queryLower = query.toLowerCase();
        String toolName = tool.name.toLowerCase();
        String toolDesc = tool.description.toLowerCase();
        String toolSig = tool.signature.toLowerCase();

        double score = 0.0;

        // Extract query terms
        String[] queryTerms = queryLower
            .replaceAll("[^a-z0-9\\s]", " ")
            .split("\\s+");

        // Score based on term matches
        for (String term : queryTerms) {
            if (term.length() < 3) continue; // Skip very short words

            // Exact name match (highest weight)
            if (toolName.contains(term)) {
                score += 10.0;
            }

            // Signature match (high weight)
            if (toolSig.contains(term)) {
                score += 5.0;
            }

            // Description match (medium weight)
            if (toolDesc.contains(term)) {
                score += 2.0;
            }
        }

        // Bonus for multiple term matches (phrase matching)
        if (queryTerms.length > 1) {
            String queryPhrase = String.join(" ", queryTerms);
            if (toolDesc.contains(queryPhrase)) {
                score += 15.0; // Strong bonus for exact phrase match
            }
            if (toolName.contains(queryPhrase)) {
                score += 20.0; // Even stronger bonus for name phrase match
            }
        }

        // Normalize by query length for fair comparison
        if (queryTerms.length > 0) {
            score = score / Math.sqrt(queryTerms.length);
        }

        return score;
    }

    /**
     * Internal representation of an MCP tool for scoring
     */
    private record McpToolInfo(String name, String description, String signature, String mcpServer) {}

    /**
     * Helper record for tool scoring
     */
    private record ScoredTool(McpToolInfo tool, double score) {}
}
