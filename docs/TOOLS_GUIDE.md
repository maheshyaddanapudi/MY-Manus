# Tools Development Guide

## Tool Architecture

Tools are Python functions available to the agent in the sandbox environment.

## Core Interface

Every tool must implement:
- Name (unique identifier)
- Description (for agent context)
- Python signature
- Documentation with examples
- Execution logic
- Sandbox requirements

## Tool Categories

### Research Tools
- **web_search**: Search the internet
- **wikipedia**: Query Wikipedia
- **arxiv_search**: Academic papers

### File System Tools
- **read_file**: Read file contents
- **write_file**: Create/update files
- **list_files**: Directory listing
- **delete_file**: Remove files

### Data Tools
- **query_database**: SQL queries (read-only)
- **parse_csv**: CSV processing
- **analyze_data**: Statistical analysis

### Network Tools
- **http_request**: API calls
- **download_file**: Fetch from URL
- **parse_html**: Extract from web pages

### Media Tools
- **render_diagram**: Create diagrams
- **convert_image**: Image manipulation
- **generate_pdf**: PDF creation
- **extract_text**: OCR/parsing

## Implementation Pattern

### Spring Component Structure
```
@Component
@ToolMetadata(
    name = "tool_name",
    category = "category",
    requiresSandbox = true
)
public class CustomTool implements Tool
```

### Python Bridge
Tools appear as Python functions:
```python
def tool_name(param1, param2="default"):
    """Documentation here"""
    return _execute_tool('tool_name', locals())
```

## Tool Registry

### Registration Flow
1. Scan for @Component beans
2. Extract @ToolMetadata
3. Generate Python signatures
4. Create execution mappings
5. Build documentation

### Dynamic Loading
- Hot reload in development
- Cache in production
- Version management
- Deprecation handling

## Parameter Handling

### Input Validation
- Type checking
- Range validation
- Format verification
- Injection prevention

### Serialization
- JSON-compatible types only
- Handle nested structures
- Convert special types
- Preserve precision

## Security Considerations

### Sandboxing Levels
1. **No Sandbox**: Internal calculations
2. **Restricted**: File system access
3. **Full Sandbox**: Network/execution

### Input Sanitization
- SQL injection prevention
- Path traversal blocking
- Command injection defense
- XXE attack prevention

### Rate Limiting
Per-tool limits:
- Requests per minute
- Data size limits
- Execution time
- Resource consumption

## Example Implementations

### Web Search Tool
Features:
- Query sanitization
- Result limiting
- Snippet extraction
- URL validation

### File Operations
Constraints:
- Workspace isolation
- Size limits (10MB)
- Type restrictions
- Permission checks

### Database Query
Safety:
- Read-only access
- Query validation
- Result pagination
- Timeout enforcement

### HTTP Request
Controls:
- URL whitelist
- Method restrictions
- Header validation
- Response size limits

## Custom Utility Tools

Replacing Manus utilities:

### render_diagram
Supports:
- Mermaid syntax
- D2 diagrams
- GraphViz DOT
- PlantUML

Implementation:
1. Parse diagram syntax
2. Detect format type
3. Call appropriate renderer
4. Return PNG/SVG

### markdown_to_pdf
Pipeline:
1. Parse Markdown
2. Generate HTML
3. Apply CSS styling
4. Convert via WeasyPrint
5. Return PDF bytes

### file_upload
Process:
1. Validate file type
2. Generate unique ID
3. Store in S3/local
4. Create shareable URL
5. Set expiration

## Tool Documentation

### Required Sections
- Brief description
- Parameter details
- Return value format
- Usage examples
- Error cases
- Performance notes

### Example Format
```
Search the web for information.

Args:
    query: Search terms
    max_results: Limit (1-10)
    
Returns:
    List of results with:
    - title: Page title
    - url: Page URL  
    - snippet: Preview text
    
Example:
    results = web_search("CodeAct paper")
    for r in results:
        print(r['title'])
```

## Error Handling

### Graceful Failures
- Return error in result
- Include helpful message
- Suggest alternatives
- Log for debugging

### Recovery Strategies
- Retry with backoff
- Fallback options
- Cached responses
- Default values

## Testing Tools

### Unit Tests
- Input validation
- Output format
- Error conditions
- Edge cases

### Integration Tests
- Full execution path
- Sandbox interaction
- State management
- Performance limits

### Security Tests
- Injection attempts
- Resource exhaustion
- Permission bypass
- Data leakage

## Performance Optimization

### Caching Strategy
- Cache tool metadata
- Store common results
- Precompute expensive ops
- Invalidate intelligently

### Async Execution
- Non-blocking I/O
- Parallel processing
- Queue management
- Result aggregation

## Monitoring

### Metrics
- Execution count
- Success rate
- Average duration
- Error frequency
- Resource usage

### Logging
- Input parameters
- Execution time
- Result size
- Error details
- User context

## Tool Versioning

### Compatibility
- Backward compatibility
- Deprecation warnings
- Migration guides
- Feature flags

### Updates
- Add parameters carefully
- Preserve signatures
- Document changes
- Test thoroughly

## Best Practices

1. **Keep tools focused** on single purpose
2. **Validate everything** before execution
3. **Document thoroughly** with examples
4. **Handle errors** gracefully
5. **Log appropriately** for debugging
6. **Test security** rigorously
7. **Monitor usage** patterns
8. **Optimize hot paths** only

## Common Pitfalls

- Over-complex tools
- Poor error messages
- Missing validation
- Inadequate documentation
- Performance bottlenecks
- Security vulnerabilities
- State corruption
- Memory leaks

## Tool Development Workflow

1. Define tool purpose
2. Design Python interface
3. Implement Java backend
4. Add validation logic
5. Create documentation
6. Write comprehensive tests
7. Security review
8. Performance testing
9. Deploy to registry
10. Monitor usage

## Future Enhancements

- Tool composition
- Dynamic parameters
- Streaming results
- Batch operations
- Tool marketplace
- User-defined tools
- ML-enhanced tools
- Tool recommendations