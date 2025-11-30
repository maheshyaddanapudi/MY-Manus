package ai.mymanus.service;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Structured representation of todo.md
 */
@Data
@Builder
public class TodoMdStructure {
    private String title;
    private List<TodoTask> tasks;
    private Map<String, String> sections;  // e.g., "progress", "notes"
    private LocalDateTime lastUpdated;
}
