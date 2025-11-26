package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import ai.mymanus.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for searching available tools by capability
 *
 * Searches across all registered tools (22 core tools) and returns
 * the most relevant tools based on the query.
 *
 * Uses keyword matching and semantic similarity to find tools that
 * match the requested capability.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SearchToolsTool implements Tool {

    private final ToolRegistry toolRegistry;

    @Override
    public String getName() {
        return "search_tools";
    }

    @Override
    public String getDescription() {
        return """
            Search for available tools by capability description.
            Use this when you need to find tools for a specific task.

            Parameters:
            - query: Natural language description of needed capability (e.g., "read files", "browser automation")
            - top_k: Number of tools to return (default: 5, max: 10)

            Returns: List of matching tools with their descriptions and signatures

            Examples:
            search_tools(query="read files", top_k=3)
            search_tools(query="browser automation")
            search_tools(query="execute python code")

            This searches across all 22 core infrastructure tools.
            """;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        int topK = params.containsKey("top_k") ?
            Math.min(((Number) params.get("top_k")).intValue(), 10) : 5;

        log.info("🔍 Searching for tools: query='{}', top_k={}", query, topK);

        // Get all available tools
        List<Tool> allTools = toolRegistry.getAllTools();

        // Score and rank tools by relevance
        List<ToolScore> scoredTools = allTools.stream()
            .map(tool -> new ToolScore(tool, calculateRelevance(tool, query)))
            .sorted(Comparator.comparingDouble(ToolScore::score).reversed())
            .limit(topK)
            .collect(Collectors.toList());

        // Build response
        List<Map<String, String>> matchingTools = scoredTools.stream()
            .map(ts -> {
                Map<String, String> toolInfo = new HashMap<>();
                toolInfo.put("name", ts.tool().getName());
                toolInfo.put("description", ts.tool().getDescription());
                toolInfo.put("signature", ts.tool().getPythonSignature());
                toolInfo.put("relevance_score", String.format("%.2f", ts.score()));
                return toolInfo;
            })
            .collect(Collectors.toList());

        log.info("✅ Found {} matching tools (top score: {})",
            matchingTools.size(),
            scoredTools.isEmpty() ? 0 : scoredTools.get(0).score());

        return Map.of(
            "success", true,
            "query", query,
            "tools_found", matchingTools.size(),
            "total_tools", allTools.size(),
            "tools", matchingTools
        );
    }

    /**
     * Calculate relevance score between query and tool
     * Uses keyword matching with term frequency weighting
     */
    private double calculateRelevance(Tool tool, String query) {
        String queryLower = query.toLowerCase();
        String toolName = tool.getName().toLowerCase();
        String toolDesc = tool.getDescription().toLowerCase();
        String toolSig = tool.getPythonSignature().toLowerCase();

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
        }

        // Normalize by query length
        if (queryTerms.length > 0) {
            score = score / Math.sqrt(queryTerms.length);
        }

        return score;
    }

    /**
     * Helper record for tool scoring
     */
    private record ToolScore(Tool tool, double score) {}
}
