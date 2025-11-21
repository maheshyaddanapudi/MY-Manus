package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for web search (placeholder implementation)
 * In production, integrate with real search API
 */
@Slf4j
@Component
public class WebSearchTool implements Tool {

    @Override
    public String getName() {
        return "search_web";
    }

    @Override
    public String getDescription() {
        return "Search the web for information";
    }

    @Override
    public String getPythonSignature() {
        return "search_web(query: str, max_results: int = 5)";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        Integer maxResults = parameters.containsKey("max_results")
            ? ((Number) parameters.get("max_results")).intValue()
            : 5;

        log.info("Web search: query='{}', max_results={}", query, maxResults);

        // Placeholder results
        List<Map<String, String>> results = new ArrayList<>();
        for (int i = 0; i < Math.min(maxResults, 3); i++) {
            Map<String, String> result = new HashMap<>();
            result.put("title", "Result " + (i + 1) + " for: " + query);
            result.put("url", "https://example.com/result-" + (i + 1));
            result.put("snippet", "This is a placeholder search result for: " + query);
            results.add(result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("query", query);
        response.put("results", results);
        return response;
    }

    @Override
    public boolean requiresNetwork() {
        return true;
    }
}
