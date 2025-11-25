package ai.mymanus.controller;

import ai.mymanus.model.Document;
import ai.mymanus.model.DocumentChunk;
import ai.mymanus.service.DocumentService;
import ai.mymanus.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for document/knowledge base management
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final RAGService ragService;

    /**
     * Upload document
     */
    @PostMapping("/{sessionId}/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file) {

        try {
            Document document = documentService.uploadDocument(sessionId, file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", document.getId());
            response.put("filename", document.getFilename());
            response.put("type", document.getType());
            response.put("chunks", document.getChunks().size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get all documents for session
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<List<Document>> getDocuments(@PathVariable String sessionId) {
        try {
            List<Document> documents = documentService.getDocuments(sessionId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting documents", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get document details
     */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<Document> getDocument(@PathVariable Long documentId) {
        return documentService.getDocument(documentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete document
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error deleting document", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Search/retrieve relevant chunks
     */
    @PostMapping("/{sessionId}/search")
    public ResponseEntity<Map<String, Object>> searchDocuments(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> request) {

        try {
            String query = (String) request.get("query");
            int topK = request.containsKey("topK") ? (int) request.get("topK") : 5;

            List<DocumentChunk> chunks = ragService.retrieveContext(sessionId, query, topK);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("chunks", chunks.stream().map(chunk -> {
                Map<String, Object> chunkData = new HashMap<>();
                chunkData.put("id", chunk.getId());
                chunkData.put("content", chunk.getContent());
                chunkData.put("documentId", chunk.getDocument().getId());
                chunkData.put("filename", chunk.getDocument().getFilename());
                chunkData.put("chunkIndex", chunk.getChunkIndex());
                return chunkData;
            }).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching documents", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get chunks for a document
     */
    @GetMapping("/document/{documentId}/chunks")
    public ResponseEntity<List<DocumentChunk>> getChunks(@PathVariable Long documentId) {
        try {
            List<DocumentChunk> chunks = documentService.getChunks(documentId);
            return ResponseEntity.ok(chunks);
        } catch (Exception e) {
            log.error("Error getting chunks", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
