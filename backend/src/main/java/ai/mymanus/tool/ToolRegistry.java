package ai.mymanus.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for all available tools.
 * Tools are automatically registered when they are Spring components.
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        toolList.forEach(this::registerTool);
    }

    /**
     * Register a tool into the registry.
     * Can be called during initialization or later for special cases.
     */
    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
        log.info("Registered tool: {} - {}", tool.getName(), tool.getDescription());
    }

    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<Tool> getAllTools() {
        return tools.values();
    }

    public String getToolDescriptions() {
        return tools.values().stream()
                .map(tool -> String.format("%s: %s\nSignature: %s",
                        tool.getName(),
                        tool.getDescription(),
                        tool.getPythonSignature()))
                .collect(Collectors.joining("\n\n"));
    }

    public String generatePythonBindings() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tool functions available in this environment\n\n");

        for (Tool tool : tools.values()) {
            sb.append(String.format("def %s:\n", tool.getPythonSignature()));
            sb.append(String.format("    '''%s'''\n", tool.getDescription()));
            sb.append(String.format("    return _execute_tool('%s', locals())\n\n", tool.getName()));
        }

        return sb.toString();
    }
}
