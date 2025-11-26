package ai.mymanus.tool.impl;

import ai.mymanus.service.mcp.MCPClientRegistry;
import ai.mymanus.service.mcp.MCPToolDefinition;
import ai.mymanus.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for searching external MCP (Model Context Protocol) tools
 *
 * Architecture:
 * - Core 22 infrastructure tools: ALWAYS pre-registered with LLM (always in context)
 * - MCP tools: Registered in application but NOT in LLM context
 * - SearchToolsTool: Discovers MCP tools on-demand via intelligent local search
 *
 * How it works:
 * 1. MCP clients are registered in MCPClientRegistry at application startup
 * 2. Their tools are NOT sent to LLM (would bloat context)
 * 3. When agent needs additional capability, it calls search_tools(query)
 * 4. This tool queries all registered MCP clients for their tool definitions
 * 5. Performs intelligent keyword/semantic matching locally
 * 6. Returns top-k most relevant tools
 * 7. Agent can then use the discovered tools
 *
 * Example:
 * search_tools(query="send email", top_k=3)
 * # Searches all MCP clients (email, calendar, database, etc.)
 * # Returns: send_email, read_inbox, search_emails from email MCP client
 *
 * Benefits:
 * - Zero context cost for unused MCP tools
 * - Infinite tool scalability
 * - Natural language tool discovery
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchToolsTool implements Tool {

    private final MCPClientRegistry mcpClientRegistry;

    @Override
    public String getName() {
        return "search_tools";
    }

    @Override
    public String getDescription() {
        return """
            Search for additional tools from external MCP (Model Context Protocol) clients.
            Use this when you need capabilities beyond the core infrastructure tools.

            The 22 core infrastructure tools (file ops, browser, shell, etc.) are always available
            and do NOT need to be searched. This tool discovers EXTERNAL tools from MCP clients.

            Parameters:
            - query: Natural language description of needed capability (e.g., "send email", "book flight")
            - top_k: Number of tools to return (default: 5, max: 10)

            Returns: List of available MCP tools with their descriptions and usage

            Examples:
            search_tools(query="send email", top_k=3)
            # Returns: send_email, read_inbox, search_emails from Email MCP Client

            search_tools(query="flight booking API")
            # Returns: search_flights, book_flight, cancel_booking from Travel MCP Client

            search_tools(query="database operations")
            # Returns: query_postgres, run_migration, backup_db from Database MCP Client

            Note: Returns empty list if no MCP clients are registered.
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

        // Check if MCP is enabled
        if (!mcpClientRegistry.isEnabled()) {
            log.info("MCP is disabled - returning empty list");
            return buildEmptyResponse(query, "MCP is disabled. Set mcp.enabled=true in application.yml");
        }

        // Get all available tools from all registered MCP clients
        List<MCPToolDefinition> allMcpTools = mcpClientRegistry.getAllAvailableTools();

        if (allMcpTools.isEmpty()) {
            log.info("No MCP tools available - no clients registered or clients have no tools");
            return buildEmptyResponse(query, "No MCP clients registered or no tools available");
        }

        log.debug("Searching across {} MCP tools from {} clients",
            allMcpTools.size(), mcpClientRegistry.getClientCount());

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
                toolInfo.put("name", st.tool.getName());
                toolInfo.put("description", st.tool.getDescription());
                toolInfo.put("signature", st.tool.getSignature());
                toolInfo.put("source_client", st.tool.getSourceClient());
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
            "mcp_clients_available", mcpClientRegistry.getClientCount(),
            "tools", matchingTools
        );
    }

    /**
     * Build empty response when no tools are available
     */
    private Map<String, Object> buildEmptyResponse(String query, String message) {
        return Map.of(
            "success", true,
            "query", query,
            "tools_found", 0,
            "total_mcp_tools", 0,
            "mcp_clients_available", mcpClientRegistry.getClientCount(),
            "tools", Collections.emptyList(),
            "message", message
        );
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
    private double calculateRelevance(MCPToolDefinition tool, String query) {
        String queryLower = query.toLowerCase();
        String toolName = tool.getName().toLowerCase();
        String toolDesc = tool.getDescription().toLowerCase();
        String toolSig = tool.getSignature().toLowerCase();

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
     * Helper record for tool scoring
     */
    private record ScoredTool(MCPToolDefinition tool, double score) {}
}
