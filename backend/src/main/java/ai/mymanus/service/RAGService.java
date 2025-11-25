package ai.mymanus.service;

import ai.mymanus.model.DocumentChunk;
import ai.mymanus.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval Augmented Generation) Service
 * Handles embeddings and semantic search for knowledge base
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RAGService {

    private final DocumentChunkRepository chunkRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${rag.top-k:5}")
    private int topK;

    /**
     * Generate embedding for text
     * In production, use Anthropic's embedding endpoint or OpenAI embeddings
     * For now, we'll use a simple hash-based mock embedding
     */
    public double[] generateEmbedding(String text) {
        // TODO: Replace with actual embedding API call
        // For now, use simple hash-based embedding (not semantic, just for demo)
        return mockEmbedding(text);
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

        log.info("✅ Retrieved {} relevant chunks", results.size());

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
     * Mock embedding generation (replace with real API)
     * Creates a simple 384-dimensional vector based on text hash
     */
    private double[] mockEmbedding(String text) {
        Random random = new Random(text.hashCode());
        double[] embedding = new double[384]; // Common embedding dimension

        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = random.nextGaussian();
        }

        // Normalize
        double norm = 0.0;
        for (double v : embedding) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }

        return embedding;
    }

    /**
     * Helper record for chunk scoring
     */
    private record ChunkScore(DocumentChunk chunk, double score) {}
}
