package ai.mymanus.service;

import lombok.Builder;
import lombok.Data;

/**
 * Individual task from todo.md
 */
@Data
@Builder
public class TodoTask {
    private int taskNumber;
    private String description;
    private boolean completed;
    private TaskStatus status;
    private String notes;
}
