package ai.mymanus.service;

import ai.mymanus.model.DocumentChunk;
import ai.mymanus.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval Augmented Generation) Service
 * Handles embeddings and semantic search for knowledge base
 *
 * Embedding Strategies (in priority order):
 * 1. Voyage AI (voyage-2) - Excellent quality, cost-effective
 * 2. OpenAI (text-embedding-3-small) - High quality, widely available
 * 3. TF-IDF Fallback - Works without API key, keyword-based matching
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RAGService {

    private final DocumentChunkRepository chunkRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rag.embedding.provider:auto}")
    private String embeddingProvider; // auto, voyageai, openai, tfidf

    @Value("${rag.embedding.voyageai-api-key:}")
    private String voyageApiKey;

    @Value("${rag.embedding.openai-api-key:}")
    private String openaiApiKey;

    @Value("${rag.embedding.dimension:1024}")
    private int embeddingDimension; // voyage-2: 1024, openai-small: 1536

    @Value("${rag.top-k:5}")
    private int topK;

    /**
     * Generate embedding for text using configured provider
     */
    public double[] generateEmbedding(String text) {
        try {
            // Auto-select provider based on available API keys
            if ("auto".equals(embeddingProvider)) {
                if (voyageApiKey != null && !voyageApiKey.isEmpty() && !voyageApiKey.startsWith("your-")) {
                    log.debug("Using Voyage AI embeddings");
                    return generateVoyageEmbedding(text);
                } else if (openaiApiKey != null && !openaiApiKey.isEmpty() && !openaiApiKey.startsWith("your-")) {
                    log.debug("Using OpenAI embeddings");
                    return generateOpenAIEmbedding(text);
                } else {
                    log.debug("No embedding API key configured - using TF-IDF fallback");
                    return generateTFIDFEmbedding(text);
                }
            }

            // Use explicitly configured provider
            return switch (embeddingProvider.toLowerCase()) {
                case "voyageai" -> generateVoyageEmbedding(text);
                case "openai" -> generateOpenAIEmbedding(text);
                case "tfidf" -> generateTFIDFEmbedding(text);
                default -> {
                    log.warn("Unknown embedding provider: {}, falling back to TF-IDF", embeddingProvider);
                    yield generateTFIDFEmbedding(text);
                }
            };

        } catch (Exception e) {
            log.error("Embedding generation failed, falling back to TF-IDF: {}", e.getMessage());
            return generateTFIDFEmbedding(text);
        }
    }

    /**
     * Generate embedding using Voyage AI (voyage-2 model)
     * High quality, 1024 dimensions, cost-effective
     */
    private double[] generateVoyageEmbedding(String text) {
        try {
            String url = "https://api.voyageai.com/v1/embeddings";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", List.of(text));
            requestBody.put("model", "voyage-2");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(voyageApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data =
                    (List<Map<String, Object>>) response.getBody().get("data");

                if (data != null && !data.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");

                    if (embedding != null) {
                        double[] result = new double[embedding.size()];
                        for (int i = 0; i < embedding.size(); i++) {
                            result[i] = embedding.get(i);
                        }
                        log.debug("✅ Generated Voyage AI embedding: {} dimensions", result.length);
                        return result;
                    }
                }
            }

            throw new RuntimeException("Invalid response from Voyage AI");

        } catch (Exception e) {
            log.error("Voyage AI embedding failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Generate embedding using OpenAI (text-embedding-3-small)
     * High quality, 1536 dimensions
     */
    private double[] generateOpenAIEmbedding(String text) {
        try {
            String url = "https://api.openai.com/v1/embeddings";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", text);
            requestBody.put("model", "text-embedding-3-small");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data =
                    (List<Map<String, Object>>) response.getBody().get("data");

                if (data != null && !data.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");

                    if (embedding != null) {
                        double[] result = new double[embedding.size()];
                        for (int i = 0; i < embedding.size(); i++) {
                            result[i] = embedding.get(i);
                        }
                        log.debug("✅ Generated OpenAI embedding: {} dimensions", result.length);
                        return result;
                    }
                }
            }

            throw new RuntimeException("Invalid response from OpenAI");

        } catch (Exception e) {
            log.error("OpenAI embedding failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Generate TF-IDF based embedding (fallback when no API key)
     * This provides better semantic matching than random hash
     * Uses a simple bag-of-words approach with term frequency weighting
     */
    private double[] generateTFIDFEmbedding(String text) {
        // Tokenize and clean text
        String[] words = text.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", " ")
            .split("\\s+");

        // Build vocabulary and count frequencies
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String word : words) {
            if (word.length() > 2) { // Filter out very short words
                termFrequency.merge(word, 1, Integer::sum);
            }
        }

        // Create fixed-size embedding (384 dimensions)
        double[] embedding = new double[384];

        // Use consistent hashing to map terms to dimensions
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();

            // Hash term to get dimension index
            int hash = Math.abs(term.hashCode());
            int dim1 = hash % 384;
            int dim2 = (hash / 384) % 384;
            int dim3 = (hash / (384 * 384)) % 384;

            // Distribute frequency across multiple dimensions for better representation
            double weight = Math.log(1 + frequency); // Log scaling
            embedding[dim1] += weight;
            embedding[dim2] += weight * 0.5;
            embedding[dim3] += weight * 0.25;
        }

        // Normalize to unit vector
        double norm = 0.0;
        for (double v : embedding) {
            norm += v * v;
        }

        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }

        log.debug("✅ Generated TF-IDF embedding: {} dimensions, {} unique terms",
            embedding.length, termFrequency.size());

        return embedding;
    }

    /**
     * Retrieve relevant context for a query
     */
    public List<DocumentChunk> retrieveContext(String sessionId, String query, int topK) {
        log.info("🔍 Retrieving context for query: {}", query);

        // Generate query embedding
        double[] queryEmbedding = generateEmbedding(query);

        // Get all chunks for session
        List<DocumentChunk> allChunks = chunkRepository.findBySessionId(sessionId);

        if (allChunks.isEmpty()) {
            log.info("No documents found for session {}", sessionId);
            return Collections.emptyList();
        }

        // Calculate similarity scores
        List<ChunkScore> scores = allChunks.stream()
                .map(chunk -> new ChunkScore(
                        chunk,
                        cosineSimilarity(queryEmbedding, chunk.getEmbedding())
                ))
                .sorted(Comparator.comparingDouble(ChunkScore::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        // Return top-k chunks
        List<DocumentChunk> results = scores.stream()
                .map(ChunkScore::chunk)
                .collect(Collectors.toList());

        log.info("✅ Retrieved {} relevant chunks (scores: {}-{})",
            results.size(),
            scores.isEmpty() ? 0 : scores.get(0).score(),
            scores.isEmpty() ? 0 : scores.get(scores.size() - 1).score());

        return results;
    }

    /**
     * Retrieve context with default topK
     */
    public List<DocumentChunk> retrieveContext(String sessionId, String query) {
        return retrieveContext(sessionId, query, topK);
    }

    /**
     * Build context string for agent prompt
     */
    public String buildContextString(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("# Relevant Knowledge Base Context\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            context.append(String.format("## Context %d (from %s)\n\n",
                    i + 1, chunk.getDocument().getFilename()));
            context.append(chunk.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Helper record for chunk scoring
     */
    private record ChunkScore(DocumentChunk chunk, double score) {}
}
