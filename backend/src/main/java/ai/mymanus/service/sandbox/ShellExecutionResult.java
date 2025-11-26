package ai.mymanus.service.sandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a shell command execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShellExecutionResult {
    private String stdout;
    private String stderr;
    private int exitCode;
    private long durationMs;
}
