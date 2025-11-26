package ai.mymanus.service;

import ai.mymanus.model.Document;
import ai.mymanus.model.DocumentChunk;
import ai.mymanus.repository.DocumentRepository;
import ai.mymanus.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Document management service for RAG/Knowledge Base
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final RAGService ragService;

    private static final int CHUNK_SIZE = 1000; // characters per chunk
    private static final int CHUNK_OVERLAP = 200; // overlap between chunks

    /**
     * Upload and process document
     */
    @Transactional
    public Document uploadDocument(String sessionId, MultipartFile file) throws IOException {
        log.info("📄 Uploading document {} for session {}", file.getOriginalFilename(), sessionId);

        // Read file content
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Determine document type
        String filename = file.getOriginalFilename();
        String type = determineDocumentType(filename);

        // Create document entity
        Document document = Document.builder()
                .sessionId(sessionId)
                .filename(filename)
                .type(type)
                .content(content)
                .fileSize(file.getSize())
                .indexed(false)
                .metadata(new HashMap<>())
                .build();

        // Save document
        document = documentRepository.save(document);

        // Chunk and index document
        chunkAndIndexDocument(document);

        log.info("✅ Document uploaded and indexed: {} ({} chunks)", filename, document.getChunks().size());

        return document;
    }

    /**
     * Chunk document and generate embeddings
     */
    @Transactional
    public void chunkAndIndexDocument(Document document) {
        log.info("🔍 Chunking and indexing document {}", document.getFilename());

        String content = document.getContent();
        List<DocumentChunk> chunks = new ArrayList<>();

        int startPos = 0;
        int chunkIndex = 0;

        while (startPos < content.length()) {
            int endPos = Math.min(startPos + CHUNK_SIZE, content.length());

            // Extract chunk content
            String chunkContent = content.substring(startPos, endPos);

            // Generate embedding
            double[] embedding = ragService.generateEmbedding(chunkContent);

            // Create chunk entity
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunkContent)
                    .chunkIndex(chunkIndex)
                    .startPosition(startPos)
                    .endPosition(endPos)
                    .embedding(embedding)
                    .metadata(new HashMap<>())
                    .build();

            chunks.add(chunk);

            // Move to next chunk with overlap
            startPos += (CHUNK_SIZE - CHUNK_OVERLAP);
            chunkIndex++;
        }

        // Save chunks
        chunkRepository.saveAll(chunks);

        // Update document
        document.setChunks(chunks);
        document.setIndexed(true);
        documentRepository.save(document);

        log.info("✅ Document indexed with {} chunks", chunks.size());
    }

    /**
     * Get all documents for session
     */
    public List<Document> getDocuments(String sessionId) {
        return documentRepository.findBySessionIdOrderByUploadedAtDesc(sessionId);
    }

    /**
     * Get document by ID
     */
    public Optional<Document> getDocument(Long documentId) {
        return documentRepository.findById(documentId);
    }

    /**
     * Delete document
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        documentRepository.deleteById(documentId);
        log.info("🗑️ Deleted document {}", documentId);
    }

    /**
     * Get chunks for document
     */
    public List<DocumentChunk> getChunks(Long documentId) {
        return chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
    }

    /**
     * Determine document type from filename
     */
    private String determineDocumentType(String filename) {
        if (filename == null) return "unknown";

        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".txt")) return "text";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".js") || lower.endsWith(".ts")) return "javascript";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".html")) return "html";

        return "text";
    }
}
