package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple tool for printing messages (for testing)
 */
@Component
public class PrintTool implements Tool {

    @Override
    public String getName() {
        return "print_message";
    }

    @Override
    public String getDescription() {
        return "Print a message to the output";
    }

    @Override
    public String getPythonSignature() {
        return "print_message(message: str)";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String message = (String) parameters.get("message");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("output", message);
        return result;
    }
}
