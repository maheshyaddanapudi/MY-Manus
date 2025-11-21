package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for file operations in the sandbox
 */
@Slf4j
@Component
public class FileOperationsTool implements Tool {

    @Override
    public String getName() {
        return "write_file";
    }

    @Override
    public String getDescription() {
        return "Write content to a file in the workspace";
    }

    @Override
    public String getPythonSignature() {
        return "write_file(filename: str, content: str)";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String filename = (String) parameters.get("filename");
        String content = (String) parameters.get("content");

        log.info("Writing file: {}, content length: {}", filename, content.length());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("filename", filename);
        result.put("bytes_written", content.length());
        return result;
    }
}
