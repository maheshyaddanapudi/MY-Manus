package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Web Search Tool - Performs web searches using multiple strategies
 *
 * Strategies (in order of priority):
 * 1. SerpAPI (if API key configured) - Most reliable
 * 2. Google Custom Search API (if API key configured) - Good quality
 * 3. Browser Automation Guidance - Instructs agent to use browser tools to search ANY website
 *
 * The browser fallback is intelligent: instead of hardcoded scraping, it guides
 * the agent to use browser_navigate, browser_view, etc. to search on ANY website
 * (Google, Bing, DuckDuckGo, or any other search engine the agent chooses).
 */
@Slf4j
@Component
public class WebSearchTool implements Tool {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${search.api.serp-api-key:}")
    private String serpApiKey;

    @Value("${search.api.google-api-key:}")
    private String googleApiKey;

    @Value("${search.api.google-cx:}")
    private String googleCx;

    @Override
    public String getName() {
        return "search_web";
    }

    @Override
    public String getDescription() {
        return """
            Search the web for information using multiple search providers.

            Parameters:
            - query (required): Search query
            - max_results (optional): Number of results to return (default: 5, max: 10)

            Examples:
            search_web(query="latest AI news", max_results=5)
            search_web(query="Python async programming tutorial")

            This tool automatically falls back to browser-based search if no API key is configured,
            ensuring it always works.
            """;
    }

    @Override
    public String getPythonSignature() {
        return "search_web(query: str, max_results: int = 5) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        Integer maxResults = parameters.containsKey("max_results")
            ? ((Number) parameters.get("max_results")).intValue()
            : 5;

        // Cap at 10 results
        maxResults = Math.min(maxResults, 10);

        log.info("🔍 Web search: query='{}', max_results={}", query, maxResults);

        try {
            // Try SerpAPI first (most reliable)
            if (serpApiKey != null && !serpApiKey.isEmpty() && !serpApiKey.equals("your-serp-api-key-here")) {
                log.info("Using SerpAPI for search");
                return searchWithSerpAPI(query, maxResults);
            }
            // Try Google Custom Search API
            else if (googleApiKey != null && !googleApiKey.isEmpty() && googleCx != null && !googleCx.isEmpty()) {
                log.info("Using Google Custom Search API");
                return searchWithGoogleAPI(query, maxResults);
            }
            // Fallback to browser automation guidance
            else {
                log.info("No API key configured - providing browser automation guidance");
                return provideBrowserGuidance(query, maxResults);
            }

        } catch (Exception e) {
            log.error("❌ Search failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("query", query);
            errorResponse.put("results", Collections.emptyList());
            return errorResponse;
        }
    }

    /**
     * Search using SerpAPI (most reliable, costs money)
     */
    private Map<String, Object> searchWithSerpAPI(String query, int maxResults) {
        try {
            String url = String.format(
                "https://serpapi.com/search.json?q=%s&num=%d&api_key=%s",
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                maxResults,
                serpApiKey
            );

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> organicResults =
                    (List<Map<String, Object>>) response.getBody().get("organic_results");

                if (organicResults != null) {
                    List<Map<String, String>> results = new ArrayList<>();
                    for (Map<String, Object> result : organicResults) {
                        Map<String, String> searchResult = new HashMap<>();
                        searchResult.put("title", (String) result.get("title"));
                        searchResult.put("url", (String) result.get("link"));
                        searchResult.put("snippet", (String) result.get("snippet"));
                        results.add(searchResult);

                        if (results.size() >= maxResults) break;
                    }

                    Map<String, Object> successResponse = new HashMap<>();
                    successResponse.put("success", true);
                    successResponse.put("query", query);
                    successResponse.put("method", "serpapi");
                    successResponse.put("results", results);
                    return successResponse;
                }
            }
        } catch (Exception e) {
            log.warn("SerpAPI search failed, falling back to browser guidance: {}", e.getMessage());
        }

        // Fallback to browser guidance
        return provideBrowserGuidance(query, maxResults);
    }

    /**
     * Search using Google Custom Search API
     */
    private Map<String, Object> searchWithGoogleAPI(String query, int maxResults) {
        try {
            String url = String.format(
                "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=%d",
                googleApiKey,
                googleCx,
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                Math.min(maxResults, 10) // Google API max is 10
            );

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items =
                    (List<Map<String, Object>>) response.getBody().get("items");

                if (items != null) {
                    List<Map<String, String>> results = new ArrayList<>();
                    for (Map<String, Object> item : items) {
                        Map<String, String> searchResult = new HashMap<>();
                        searchResult.put("title", (String) item.get("title"));
                        searchResult.put("url", (String) item.get("link"));
                        searchResult.put("snippet", (String) item.get("snippet"));
                        results.add(searchResult);

                        if (results.size() >= maxResults) break;
                    }

                    Map<String, Object> successResponse = new HashMap<>();
                    successResponse.put("success", true);
                    successResponse.put("query", query);
                    successResponse.put("method", "google-custom-search");
                    successResponse.put("results", results);
                    return successResponse;
                }
            }
        } catch (Exception e) {
            log.warn("Google API search failed, falling back to browser guidance: {}", e.getMessage());
        }

        // Fallback to browser guidance
        return provideBrowserGuidance(query, maxResults);
    }

    /**
     * Provide browser automation guidance for web search
     *
     * Instead of hardcoded scraping, this guides the agent to use browser tools
     * to search ANY website (Google, Bing, DuckDuckGo, or any other search engine).
     *
     * The agent can choose which search engine to use and navigate there using
     * browser_navigate, browser_view, browser_click, etc.
     */
    private Map<String, Object> provideBrowserGuidance(String query, int maxResults) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("method", "browser-guidance");
            response.put("message", "No search API key configured. Use browser tools to search any website.");

            // Provide multiple search engine options
            List<Map<String, String>> searchEngineOptions = new ArrayList<>();
            searchEngineOptions.add(Map.of(
                "engine", "Google",
                "url", "https://www.google.com/search?q=" + encodedQuery,
                "description", "Most popular search engine with comprehensive results"
            ));
            searchEngineOptions.add(Map.of(
                "engine", "DuckDuckGo",
                "url", "https://duckduckgo.com/?q=" + encodedQuery,
                "description", "Privacy-focused search engine"
            ));
            searchEngineOptions.add(Map.of(
                "engine", "Bing",
                "url", "https://www.bing.com/search?q=" + encodedQuery,
                "description", "Microsoft's search engine"
            ));
            searchEngineOptions.add(Map.of(
                "engine", "Brave Search",
                "url", "https://search.brave.com/search?q=" + encodedQuery,
                "description", "Independent, privacy-focused search"
            ));

            response.put("search_engine_options", searchEngineOptions);

            // Provide recommended browser tool sequence
            List<String> toolSequence = new ArrayList<>();
            toolSequence.add("1. Choose a search engine from the options above (or use any other you prefer)");
            toolSequence.add("2. Use browser_navigate(url) to navigate to the search engine URL");
            toolSequence.add("3. Use browser_view() to see the search results page");
            toolSequence.add("4. Use browser_click() or browser_extract() to interact with specific results if needed");
            toolSequence.add("5. Extract the information you need from the visible content");

            response.put("browser_tool_sequence", toolSequence);
            response.put("max_results_requested", maxResults);

            log.info("✅ Provided browser guidance for search: {}", query);
            return response;

        } catch (Exception e) {
            log.error("Failed to provide browser guidance: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to provide browser guidance: " + e.getMessage());
            errorResponse.put("query", query);
            return errorResponse;
        }
    }

    @Override
    public boolean requiresNetwork() {
        return true;
    }
}
