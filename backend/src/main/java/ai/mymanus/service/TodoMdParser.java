package ai.mymanus.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for todo.md files
 * Extracts structured plan information from markdown
 */
@Service
@Slf4j
public class TodoMdParser {

    private static final Pattern TASK_PATTERN = Pattern.compile("^\\s*-\\s*\\[([ xX])\\]\\s*(.+?)(?:\\s*\\((.+?)\\))?$");

    /**
     * Parse todo.md content into structured format
     */
    public TodoMdStructure parse(String todoMdContent) {
        if (todoMdContent == null || todoMdContent.trim().isEmpty()) {
            return TodoMdStructure.builder()
                .title("No Plan")
                .tasks(new ArrayList<>())
                .sections(new HashMap<>())
                .lastUpdated(LocalDateTime.now())
                .build();
        }

        List<TodoTask> tasks = new ArrayList<>();
        String title = "";
        Map<String, String> sections = new LinkedHashMap<>();
        String currentSection = "main";
        StringBuilder sectionContent = new StringBuilder();

        String[] lines = todoMdContent.split("\n");

        for (String line : lines) {
            // Extract title (first # heading)
            if (line.trim().startsWith("# ") && title.isEmpty()) {
                title = line.trim().substring(2).trim();
                continue;
            }

            // Extract sections (## headings)
            if (line.trim().startsWith("## ")) {
                // Save previous section
                if (sectionContent.length() > 0) {
                    sections.put(currentSection, sectionContent.toString().trim());
                }
                currentSection = line.trim().substring(3).trim().toLowerCase();
                sectionContent = new StringBuilder();
                continue;
            }

            // Parse task lines (within Tasks section or at root)
            if (line.matches("^\\s*-\\s*\\[([ xX])\\].*")) {
                TodoTask task = parseTaskLine(line, tasks.size() + 1);
                if (task != null) {
                    tasks.add(task);
                }
                continue;
            }

            // Accumulate section content
            if (!line.trim().isEmpty() || sectionContent.length() > 0) {
                sectionContent.append(line).append("\n");
            }
        }

        // Save last section
        if (sectionContent.length() > 0) {
            sections.put(currentSection, sectionContent.toString().trim());
        }

        // Infer in-progress task from Progress section
        inferInProgressTask(tasks, sections);

        return TodoMdStructure.builder()
            .title(title.isEmpty() ? "Plan" : title)
            .tasks(tasks)
            .sections(sections)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    /**
     * Parse a single task line
     */
    private TodoTask parseTaskLine(String line, int taskNumber) {
        Matcher matcher = TASK_PATTERN.matcher(line);

        if (matcher.find()) {
            boolean completed = !matcher.group(1).equals(" ");
            String description = matcher.group(2).trim();
            String notes = matcher.group(3) != null ? matcher.group(3).trim() : null;

            return TodoTask.builder()
                .taskNumber(taskNumber)
                .description(description)
                .completed(completed)
                .status(completed ? TaskStatus.COMPLETED : TaskStatus.PENDING)
                .notes(notes)
                .build();
        }

        return null;
    }

    /**
     * Infer which task is in progress from Progress section
     */
    private void inferInProgressTask(List<TodoTask> tasks, Map<String, String> sections) {
        String progressSection = sections.get("progress");
        if (progressSection == null || progressSection.isEmpty()) {
            return;
        }

        // Look for patterns like "Currently working on: Book hotel"
        Pattern workingOnPattern = Pattern.compile("(?i)(?:currently working on|working on|started|executing):\\s*(.+?)(?:\n|$)");
        Matcher matcher = workingOnPattern.matcher(progressSection);

        if (matcher.find()) {
            String workingOnText = matcher.group(1).trim().toLowerCase();

            // Find matching task
            for (TodoTask task : tasks) {
                String taskDesc = task.getDescription().toLowerCase();
                if (!task.isCompleted() && (
                    taskDesc.contains(workingOnText) ||
                    workingOnText.contains(taskDesc) ||
                    similarity(taskDesc, workingOnText) > 0.5
                )) {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    log.debug("Inferred task {} is IN_PROGRESS: {}", task.getTaskNumber(), task.getDescription());
                    break;
                }
            }
        }
    }

    /**
     * Simple string similarity calculation
     */
    private double similarity(String s1, String s2) {
        String longer = s1.length() > s2.length() ? s1 : s2;
        String shorter = s1.length() > s2.length() ? s2 : s1;

        if (longer.length() == 0) return 1.0;

        int editDistance = computeEditDistance(longer, shorter);
        return (longer.length() - editDistance) / (double) longer.length();
    }

    /**
     * Compute Levenshtein distance
     */
    private int computeEditDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
}

/**
 * Structured representation of todo.md
 */
@Data
@Builder
class TodoMdStructure {
    private String title;
    private List<TodoTask> tasks;
    private Map<String, String> sections;  // e.g., "progress", "notes"
    private LocalDateTime lastUpdated;
}

/**
 * Individual task from todo.md
 */
@Data
@Builder
class TodoTask {
    private int taskNumber;
    private String description;
    private boolean completed;
    private TaskStatus status;
    private String notes;
}

/**
 * Task status enum
 */
enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
