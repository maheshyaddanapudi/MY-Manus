import json
import sys
import uuid
import traceback

# Tool execution bridge (RPC to Java)
def _execute_tool(tool_name, params):
    try:
        # Generate unique request ID
        request_id = str(uuid.uuid4())
        
        # Build request
        request = {
            'id': request_id,
            'tool': tool_name,
            'params': params
        }
        
        # Send request to Java via stdout
        request_json = json.dumps(request)
        print(f'__TOOL_REQUEST__{request_json}__END__', flush=True)
        
        # Read response from Java via stdin
        response_line = sys.stdin.readline().strip()
        
        # Parse response
        if '__TOOL_RESPONSE__' in response_line:
            response_json = response_line.split('__TOOL_RESPONSE__')[1].split('__END__')[0]
            response = json.loads(response_json)
            
            # Check for errors
            if response.get('error'):
                raise Exception(f"Tool error: {response['error']}")
            
            # Return result
            return response['result']
        else:
            raise Exception('Invalid tool response format')
    except Exception as e:
        print(f'ERROR: Tool execution failed: {str(e)}', file=sys.stderr)
        traceback.print_exc()
        return {'success': False, 'error': str(e)}

# Tool functions available in this environment

def search_tools(query: str, top_k: int = 5) -> str:
    '''Search for additional tools from external MCP (Model Context Protocol) servers.

Use this when you need capabilities beyond the built-in tools. For example:
- Email operations: search_tools(query="send email")
- Calendar operations: search_tools(query="schedule meeting")
- Database operations: search_tools(query="query database")

Returns a list of matching tools with their names, descriptions, and parameter schemas.
After finding a tool, use mcp_call() to execute it.

Example workflow:
    # 1. Search for tools
    tools = search_tools(query="send email", top_k=3)
    print(tools)  # Shows available email tools

    # 2. Execute the tool (use mcp_call)
    result = mcp_call(tool_name="send_email", to="john@example.com", ...)
'''
    return _execute_tool('search_tools', locals())

def visualize_data(chart_type: str, data_description: str) -> dict:
    '''Create data visualizations. Supported types: line, bar, scatter, histogram, heatmap. Returns guidance on which Python visualization libraries to use. Agent should write Python code using matplotlib, seaborn, or plotly.'''
    return _execute_tool('visualize_data', locals())

def browser_press_key(sessionId: str, key: str) -> dict:
    '''Press a keyboard key. Supports: Enter, Escape, Tab, Backspace, ArrowDown, ArrowUp, etc.'''
    return _execute_tool('browser_press_key', locals())

def file_list(path: str = '/workspace', maxDepth: int = 3, includeHidden: bool = False) -> dict:
    '''List files and directories in tree structure. Returns file paths, sizes, and types.'''
    return _execute_tool('file_list', locals())

def message_notify_user(message: str, level: str = 'info') -> dict:
    '''Send a notification message to the user. Use this to inform the user of progress, warnings, or status updates during task execution.'''
    return _execute_tool('message_notify_user', locals())

def message_ask_user(question: str, sessionId: str) -> dict:
    '''Ask the user a question and wait for their response. Use this when you need additional information or clarification from the user to complete the task.'''
    return _execute_tool('message_ask_user', locals())

def browser_view(sessionId: str) -> dict:
    '''Get current browser view including screenshot (base64) and accessibility tree. Essential for agent to understand page state.'''
    return _execute_tool('browser_view', locals())

def browser_scroll_down(sessionId: str, amount: int = 500) -> dict:
    '''Scroll the page downward. Default scroll amount is 500 pixels.'''
    return _execute_tool('browser_scroll_down', locals())

def file_find_content(pattern: str, path: str = '.', regex: bool = False) -> list[dict]:
    '''Search for text content within files in the workspace. Supports regex patterns. Returns file paths, line numbers, and matching text. Default path is '.' (current workspace).'''
    return _execute_tool('file_find_content', locals())

def browser_input(sessionId: str, selector: str, text: str) -> dict:
    '''Type text into an input field using a CSS selector.'''
    return _execute_tool('browser_input', locals())

def todo(action: str, content: str = None) -> dict:
    '''Read or write the todo.md file. Use 'read' action to view todos, 'write' action to update them. Helps track multi-step tasks across iterations.'''
    return _execute_tool('todo', locals())

def search_web(query: str, max_results: int = 5) -> dict:
    '''Search the web for information using multiple search providers.

Parameters:
- query (required): Search query
- max_results (optional): Number of results to return (default: 5, max: 10)

Examples:
search_web(query="latest AI news", max_results=5)
search_web(query="Python async programming tutorial")

This tool automatically falls back to browser-based search if no API key is configured,
ensuring it always works.
'''
    return _execute_tool('search_web', locals())

def print_message(message: str):
    '''Print a message to the output'''
    return _execute_tool('print_message', locals())

def mcp_call(tool_name: str, **kwargs) -> dict:
    '''Execute an MCP (Model Context Protocol) tool from an external server.

Use this after discovering tools with search_tools(). This tool acts as a bridge
to execute remote MCP tools and return their results.

Parameters:
- tool_name: The exact name of the MCP tool to execute (from search_tools results)
- **kwargs: All other parameters are passed to the MCP tool as arguments

Example workflow:
    # 1. First, discover available tools
    tools = search_tools(query="send email")
    print(tools)  # Shows: send_email tool is available

    # 2. Then, execute the tool
    result = mcp_call(
        tool_name="send_email",
        to="john@example.com",
        subject="Meeting Tomorrow",
        body="Let's meet at 10am"
    )
    print(result)  # Shows: Email sent successfully

Returns: The result from the MCP tool execution (usually a dict with success/error info)
'''
    return _execute_tool('mcp_call', locals())

def browser_refresh(sessionId: str) -> dict:
    '''Refresh the current page.'''
    return _execute_tool('browser_refresh', locals())

def file_read(path: str) -> str:
    '''Read the entire contents of a file. Returns the file content as a string.'''
    return _execute_tool('file_read', locals())

def file_replace_string(path: str, old: str, new: str) -> dict:
    '''Find and replace text in a file. Returns the number of replacements made. The original file is only modified if replacements are found.'''
    return _execute_tool('file_replace_string', locals())

def shell_exec(sessionId: str, command: str, timeout: int = 30) -> dict:
    '''Execute a shell command in the sandbox. Returns stdout, stderr, and exit code.'''
    return _execute_tool('shell_exec', locals())

def browser_navigate(sessionId: str, url: str) -> dict:
    '''Navigate the browser to a specific URL. Returns success status and current URL.'''
    return _execute_tool('browser_navigate', locals())

def file_write(path: str, content: str) -> None:
    '''Write content to a file. Creates the file if it doesn't exist, overwrites if it does. Parent directories are created automatically.'''
    return _execute_tool('file_write', locals())

def file_find_by_name(pattern: str, path: str = '.', regex: bool = False) -> list[str]:
    '''Find files by name pattern within the workspace. Supports glob patterns (*.txt, test*.py) and regex. Returns list of relative file paths. Default path is '.' (current workspace).'''
    return _execute_tool('file_find_by_name', locals())

def browser_click(sessionId: str, selector: str) -> dict:
    '''Click an element on the page using a CSS selector.'''
    return _execute_tool('browser_click', locals())

def browser_scroll_up(sessionId: str, amount: int = 500) -> dict:
    '''Scroll the page upward. Default scroll amount is 500 pixels.'''
    return _execute_tool('browser_scroll_up', locals())


# Restore previous state
browser_press_key = json.loads('"<function browser_press_key at 0x7f0d9c4f45e0>"')
file_list = json.loads('"<function file_list at 0x7f0d9c4f4680>"')
message_notify_user = json.loads('"<function message_notify_user at 0x7f0d9c4f4720>"')
uuid = json.loads('"<module \'uuid\' from \'/usr/lib/python3.11/uuid.py\'>"')
file_find_content = json.loads('"<function file_find_content at 0x7f0d9c4f49a0>"')
result = json.loads('100')
search_web = json.loads('"<function search_web at 0x7f0d9c4f4b80>"')
mcp_call = json.loads('"<function mcp_call at 0x7f0d9c4f4cc0>"')
browser_refresh = json.loads('"<function browser_refresh at 0x7f0d9c4f4d60>"')
file_content_success = json.loads('"All fixes applied successfully!"')
file_write = json.loads('"<function file_write at 0x7f0d9c4f5080>"')
browser_click = json.loads('"<function browser_click at 0x7f0d9c4f51c0>"')
browser_scroll_up = json.loads('"<function browser_scroll_up at 0x7f0d9c4f5260>"')
search_tools = json.loads('"<function search_tools at 0x7f0d9c6ccae0>"')
visualize_data = json.loads('"<function visualize_data at 0x7f0d9c4efd80>"')
message_ask_user = json.loads('"<function message_ask_user at 0x7f0d9c4f47c0>"')
browser_view = json.loads('"<function browser_view at 0x7f0d9c4f4860>"')
browser_scroll_down = json.loads('"<function browser_scroll_down at 0x7f0d9c4f4900>"')
browser_input = json.loads('"<function browser_input at 0x7f0d9c4f4a40>"')
todo = json.loads('"<function todo at 0x7f0d9c4f4ae0>"')
print_message = json.loads('"<function print_message at 0x7f0d9c4f4c20>"')
file_read = json.loads('"<function file_read at 0x7f0d9c4f4e00>"')
file_replace_string = json.loads('"<function file_replace_string at 0x7f0d9c4f4ea0>"')
shell_exec = json.loads('"<function shell_exec at 0x7f0d9c4f4f40>"')
browser_navigate = json.loads('"<function browser_navigate at 0x7f0d9c4f4fe0>"')
file_find_by_name = json.loads('"<function file_find_by_name at 0x7f0d9c4f5120>"')

# User code
try:
    # Use standard Python file operations to create the files
    import os
    
    # Ensure the workspace directory exists
    workspace_dir = "/home/ubuntu/workspace"
    os.makedirs(workspace_dir, exist_ok=True)
    
    # Create success.txt
    success_file_path = os.path.join(workspace_dir, "success.txt")
    with open(success_file_path, "w") as f:
        f.write("All fixes applied successfully!")
    print("✅ success.txt created")
    
    # Create result.txt with the calculation result
    result_file_path = os.path.join(workspace_dir, "result.txt")
    with open(result_file_path, "w") as f:
        f.write(str(result))
    print("✅ result.txt created")
    
    # Verify the files were created by reading them
    with open(success_file_path, "r") as f:
        success_content = f.read()
    print(f"success.txt contains: '{success_content}'")
    
    with open(result_file_path, "r") as f:
        result_content = f.read()
    print(f"result.txt contains: '{result_content}'")

except Exception as e:
    print(f'ERROR: {str(e)}', file=sys.stderr)
    traceback.print_exc()

# Capture state
_state = {k: v for k, v in globals().items() if not k.startswith('_') and k not in ['json', 'sys', 'traceback']}
print(f'STATE:{json.dumps(_state, default=str)}')
