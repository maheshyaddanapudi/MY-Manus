package ai.mymanus.service.sandbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PythonSandboxExecutor
 * Tests Python code execution in sandbox
 */
@ExtendWith(MockitoExtension.class)
class PythonSandboxExecutorTest {

    private PythonSandboxExecutor sandboxExecutor;

    @BeforeEach
    void setUp() {
        // Initialize executor (implementation depends on sandbox mode)
        sandboxExecutor = new PythonSandboxExecutor();
    }

    @Test
    void testSimplePrintExecution() {
        String code = "print('Hello, World!')";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertNotNull(result);
        assertTrue(result.getStdout().contains("Hello, World!"));
        assertEquals(0, result.getExitCode());
    }

    @Test
    void testVariablePersistence() {
        Map<String, Object> context = new HashMap<>();

        // First execution: set variable
        String code1 = "x = 42";
        ExecutionResult result1 = sandboxExecutor.execute(code1, context);
        assertEquals(0, result1.getExitCode());

        // Second execution: use variable
        String code2 = "print(x)";
        ExecutionResult result2 = sandboxExecutor.execute(code2, context);
        assertTrue(result2.getStdout().contains("42"));
    }

    @Test
    void testErrorHandling() {
        String code = "1 / 0  # Division by zero";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertNotNull(result);
        assertNotEquals(0, result.getExitCode());
        assertTrue(result.getStderr().contains("ZeroDivisionError") ||
                   result.getStderr().contains("division"));
    }

    @Test
    void testSyntaxError() {
        String code = "print('missing parenthesis'";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertNotNull(result);
        assertNotEquals(0, result.getExitCode());
        assertTrue(result.getStderr().contains("SyntaxError") ||
                   result.getStderr().contains("syntax"));
    }

    @Test
    void testMultilineCode() {
        String code = """
            def greet(name):
                return f"Hello, {name}!"

            message = greet("Alice")
            print(message)
            """;
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("Hello, Alice!"));
    }

    @Test
    void testStdout() {
        String code = "print('Line 1'); print('Line 2')";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertNotNull(result.getStdout());
        assertTrue(result.getStdout().contains("Line 1"));
        assertTrue(result.getStdout().contains("Line 2"));
    }

    @Test
    void testStderr() {
        String code = "import sys; print('Error message', file=sys.stderr)";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertNotNull(result.getStderr());
        assertTrue(result.getStderr().contains("Error message"));
    }

    @Test
    void testExitCodeSuccess() {
        String code = "print('Success')";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertEquals(0, result.getExitCode());
    }

    @Test
    void testExitCodeFailure() {
        String code = "raise Exception('Test exception')";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertNotEquals(0, result.getExitCode());
    }

    @Test
    void testContextPreservation() {
        Map<String, Object> context = new HashMap<>();

        // Execute multiple statements
        sandboxExecutor.execute("a = 1", context);
        sandboxExecutor.execute("b = 2", context);
        sandboxExecutor.execute("c = a + b", context);
        ExecutionResult result = sandboxExecutor.execute("print(c)", context);

        assertTrue(result.getStdout().contains("3"));
    }

    @Test
    void testFileOperations() {
        String code = """
            with open('/workspace/test.txt', 'w') as f:
                f.write('Test content')
            """;
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertEquals(0, result.getExitCode());
    }

    @Test
    void testImportStatements() {
        String code = "import os; print(os.path.exists('/workspace'))";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("True") || result.getStdout().contains("False"));
    }

    @Test
    void testLoopExecution() {
        String code = """
            for i in range(5):
                print(i)
            """;
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("0"));
        assertTrue(result.getStdout().contains("4"));
    }

    @Test
    void testListComprehension() {
        String code = "result = [x*2 for x in range(5)]; print(result)";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("[0, 2, 4, 6, 8]"));
    }

    @Test
    void testDictionaryOperations() {
        String code = """
            data = {'name': 'Alice', 'age': 30}
            print(data['name'])
            """;
        Map<String, Object> context = new HashMap<>();

        ExecutionResult result = sandboxExecutor.execute(code, context);

        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("Alice"));
    }
}
