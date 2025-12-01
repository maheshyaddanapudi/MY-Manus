package ai.mymanus.config;

import ai.mymanus.service.sandbox.SandboxExecutor;
import ai.mymanus.tool.ToolRegistry;
import ai.mymanus.tool.impl.shell.ShellExecTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration for tools that require special initialization.
 * 
 * ShellExecTool is registered manually here to break the circular dependency:
 * - PythonSandboxExecutor needs ToolRegistry
 * - ToolRegistry needs all Tools (including ShellExecTool)
 * - ShellExecTool needs SandboxExecutor
 * 
 * By removing @Component from ShellExecTool and registering it here,
 * we ensure proper initialization order:
 * 1. ToolRegistry is created with 24 auto-discovered tools
 * 2. PythonSandboxExecutor is created with the registry
 * 3. ShellExecTool is created with the executor
 * 4. ShellExecTool is manually registered into the registry
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ToolConfiguration {

    private final ApplicationContext applicationContext;

    /**
     * Create ShellExecTool bean.
     * Note: We don't register it here to avoid circular dependency.
     * Registration happens in onApplicationEvent() after all beans are created.
     */
    @Bean
    public ShellExecTool shellExecTool(@Lazy SandboxExecutor sandboxExecutor) {
        log.info("Creating ShellExecTool bean");
        return new ShellExecTool(sandboxExecutor);
    }

    /**
     * Register ShellExecTool into ToolRegistry after application context is fully initialized.
     * This breaks the circular dependency by deferring registration until after bean creation.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void registerShellExecTool() {
        log.info("Registering ShellExecTool into ToolRegistry");
        
        ToolRegistry toolRegistry = applicationContext.getBean(ToolRegistry.class);
        ShellExecTool shellExecTool = applicationContext.getBean(ShellExecTool.class);
        
        toolRegistry.registerTool(shellExecTool);
        
        log.info("✅ ShellExecTool registered successfully");
    }
}
