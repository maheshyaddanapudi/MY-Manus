package ai.mymanus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating Python code before execution
 *
 * Provides syntax validation and safety checks to prevent execution
 * of obviously malformed or dangerous code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonValidationService {

    /**
     * Validate Python code syntax using Python's ast.parse
     *
     * @param code Python code to validate
     * @return ValidationResult with success/error details
     */
    public ValidationResult validateSyntax(String code) {
        if (code == null || code.trim().isEmpty()) {
            return new ValidationResult(false, "Code is empty", null);
        }

        // Use Python's ast module for syntax validation
        // Pass code via stdin to avoid escaping issues
        String validationScript = """
            import ast
            import sys

            # Read code from stdin
            code = sys.stdin.read()

            try:
                ast.parse(code)
                print("VALID")
            except SyntaxError as e:
                print(f"SYNTAX_ERROR: {e.msg} at line {e.lineno}")
            except Exception as e:
                print(f"ERROR: {str(e)}")
            """;

        try {
            // Execute validation script in a safe Python environment
            Process process = new ProcessBuilder(
                "python3", "-c", validationScript
            ).redirectErrorStream(true).start();
            
            // Send code to Python via stdin
            process.getOutputStream().write(code.getBytes());
            process.getOutputStream().close();

            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();

            if (output.startsWith("VALID")) {
                log.debug("Python syntax validation passed");
                return new ValidationResult(true, null, null);
            } else {
                String errorMsg = output.replace("SYNTAX_ERROR: ", "").replace("ERROR: ", "");
                log.warn("Python syntax validation failed: {}", errorMsg);
                return new ValidationResult(false, errorMsg, null);
            }

        } catch (Exception e) {
            log.error("Failed to validate Python syntax: {}", e.getMessage());
            // If validation fails, allow execution but warn
            return new ValidationResult(true, null, "Could not validate syntax: " + e.getMessage());
        }
    }

    /**
     * Check for common safety issues and dangerous patterns
     *
     * @param code Python code to check
     * @return ValidationResult with safety assessment
     */
    public ValidationResult checkSafety(String code) {
        List<String> warnings = new ArrayList<>();

        // Check for dangerous file system operations
        if (Pattern.compile("rm\\s+-rf").matcher(code).find()) {
            return new ValidationResult(false, "Dangerous command 'rm -rf' detected", null);
        }

        if (code.contains("shutil.rmtree") && !code.contains("#")) {
            warnings.add("Recursive directory deletion detected (shutil.rmtree)");
        }

        // Check for suspicious system commands
        if (code.contains("__import__('os').system") || code.contains("__import__(\"os\").system")) {
            return new ValidationResult(false, "Suspicious obfuscated system command detected", null);
        }

        // Check for eval/exec (can be dangerous but sometimes necessary)
        if (code.contains("eval(") || code.contains("exec(")) {
            warnings.add("Dynamic code execution detected (eval/exec)");
        }

        // Check for network operations (informational only)
        if (code.matches(".*\\b(requests|urllib|socket|http\\.client)\\b.*")) {
            log.info("Code contains network operations");
        }

        // Check for file operations (informational only)
        if (code.contains("open(") && code.contains("'w'")) {
            log.info("Code contains file write operations");
        }

        String warningMsg = warnings.isEmpty() ? null : String.join("; ", warnings);
        return new ValidationResult(true, null, warningMsg);
    }

    /**
     * Perform complete validation (syntax + safety)
     *
     * @param code Python code to validate
     * @return Combined validation result
     */
    public ValidationResult validate(String code) {
        // First check syntax
        ValidationResult syntaxResult = validateSyntax(code);
        if (!syntaxResult.isValid()) {
            return syntaxResult;
        }

        // Then check safety
        ValidationResult safetyResult = checkSafety(code);
        if (!safetyResult.isValid()) {
            return safetyResult;
        }

        // Combine warnings
        String combinedWarnings = null;
        if (syntaxResult.warning() != null && safetyResult.warning() != null) {
            combinedWarnings = syntaxResult.warning() + "; " + safetyResult.warning();
        } else if (syntaxResult.warning() != null) {
            combinedWarnings = syntaxResult.warning();
        } else if (safetyResult.warning() != null) {
            combinedWarnings = safetyResult.warning();
        }

        return new ValidationResult(true, null, combinedWarnings);
    }

    /**
     * Validation result
     *
     * @param isValid Whether the code passed validation
     * @param error Error message if validation failed
     * @param warning Warning message (code is valid but potentially risky)
     */
    public record ValidationResult(boolean isValid, String error, String warning) {
        public String getErrorMessage() {
            return error != null ? error : "";
        }

        public String getWarningMessage() {
            return warning != null ? warning : "";
        }

        public boolean hasWarning() {
            return warning != null && !warning.isEmpty();
        }
    }
}
