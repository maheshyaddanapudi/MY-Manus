package ai.mymanus.service.sandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private boolean success;
    private String stdout;
    private String stderr;
    private Integer exitCode;
    private Map<String, Object> variables;  // Python variables state
    private long executionTimeMs;
    private String error;
}
