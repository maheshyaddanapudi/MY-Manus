#!/bin/bash
# ============================================================================
# analyze_claude_session.sh — Claude Code Session Report Generator
# ============================================================================
# Reads Claude Code session JSONL files from ~/.claude/projects/ and produces
# a comprehensive sequential report of all agent activity.
#
# Usage:
#   ./analyze_claude_session.sh                          # most recent session
#   ./analyze_claude_session.sh <project-path>           # most recent session in project
#   ./analyze_claude_session.sh <project-path> <session>  # specific session
#   ./analyze_claude_session.sh --list                   # list all projects
#   ./analyze_claude_session.sh <project-path> --list    # list sessions in project
#
# Arguments:
#   project-path   The actual project path (e.g. /Users/me/myproject),
#                  the encoded directory name under ~/.claude/projects/,
#                  or a substring match. If omitted, the most recent session
#                  across all projects is used.
#   session        Specific session UUID (full or partial). If omitted,
#                  the most recent session in the project is used.
#   --list         List available projects or sessions instead of analyzing.
#
# Requirements:
#   - macOS (or Linux) with python3
#   - No other external dependencies
#
# Output:
#   - Colored report to stdout (when connected to a terminal)
#   - Clean Markdown report saved to ./claude_session_report_TIMESTAMP.md
# ============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Color helpers (terminal only)
# ---------------------------------------------------------------------------
if [ -t 1 ]; then
    BOLD='\033[1m'
    DIM='\033[2m'
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[0;33m'
    BLUE='\033[0;34m'
    CYAN='\033[0;36m'
    RESET='\033[0m'
else
    BOLD='' DIM='' RED='' GREEN='' YELLOW='' BLUE='' CYAN='' RESET=''
fi

info()  { printf "${BLUE}[INFO]${RESET}  %s\n" "$*" >&2; }
warn()  { printf "${YELLOW}[WARN]${RESET}  %s\n" "$*" >&2; }
error() { printf "${RED}[ERROR]${RESET} %s\n" "$*" >&2; }
die()   { error "$@"; exit 1; }

# ---------------------------------------------------------------------------
# Locate the Claude data directory
# ---------------------------------------------------------------------------
CLAUDE_DIR="${HOME}/.claude"
PROJECTS_DIR="${CLAUDE_DIR}/projects"

[ -d "$CLAUDE_DIR" ]  || die "Claude Code data directory not found: ${CLAUDE_DIR}"
[ -d "$PROJECTS_DIR" ] || die "No projects directory found: ${PROJECTS_DIR}"

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
PROJECT_DIR_ARG=""
SESSION_ID_ARG=""
LIST_MODE=""
POSITIONAL_ARGS=()

for arg in "$@"; do
    if [ "$arg" = "--list" ] || [ "$arg" = "-l" ]; then
        LIST_MODE="--list"
    elif [ "$arg" = "--help" ] || [ "$arg" = "-h" ]; then
        echo "Usage: $0 [project-path] [session-id] [--list]"
        echo ""
        echo "Analyze Claude Code session JSONL files and produce a report."
        echo ""
        echo "Arguments:"
        echo "  project-path   Project path, encoded dir name, or substring"
        echo "  session-id     Session UUID (full or partial)"
        echo "  --list, -l     List available projects or sessions"
        echo "  --help, -h     Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0                              # analyze most recent session"
        echo "  $0 /Users/me/myproject          # most recent session in project"
        echo "  $0 myproject                    # substring match on project name"
        echo "  $0 myproject a1b2c3d4           # specific session (partial UUID)"
        echo "  $0 --list                       # list all projects"
        echo "  $0 myproject --list             # list sessions in project"
        exit 0
    else
        POSITIONAL_ARGS+=("$arg")
    fi
done

PROJECT_DIR_ARG="${POSITIONAL_ARGS[0]:-}"
SESSION_ID_ARG="${POSITIONAL_ARGS[1]:-}"

# ---------------------------------------------------------------------------
# Embedded Python script — does all the heavy lifting
# ---------------------------------------------------------------------------
PYTHON_SCRIPT=$(mktemp "${TMPDIR:-/tmp}/claude_report_XXXXXX.py")
trap 'rm -f "$PYTHON_SCRIPT"' EXIT

cat > "$PYTHON_SCRIPT" << 'PYTHON_EOF'
#!/usr/bin/env python3
"""
Claude Code Session Analyzer — Python core.

Reads JSONL session files, extracts structured data, and emits a Markdown report
to stdout. A second copy without ANSI escapes is written to a file.
"""

import json
import os
import sys
import re
import glob
import datetime
from pathlib import Path
from collections import defaultdict, OrderedDict

# ── Configuration ──────────────────────────────────────────────────────────

CLAUDE_DIR = os.path.expanduser("~/.claude")
PROJECTS_DIR = os.path.join(CLAUDE_DIR, "projects")

# Maximum characters for various truncation contexts
MAX_THOUGHT = 600
MAX_TOOL_DESC = 200
MAX_BASH_CMD = 250
MAX_USER_MSG = 400
MAX_FINAL_MSG = 2000

# ── ANSI color helpers ─────────────────────────────────────────────────────

IS_TTY = os.isatty(sys.stdout.fileno()) if hasattr(sys.stdout, 'fileno') else False

def _c(code, text):
    if IS_TTY:
        return f"\033[{code}m{text}\033[0m"
    return text

def bold(t):    return _c("1", t)
def dim(t):     return _c("2", t)
def red(t):     return _c("31", t)
def green(t):   return _c("32", t)
def yellow(t):  return _c("33", t)
def blue(t):    return _c("34", t)
def magenta(t): return _c("35", t)
def cyan(t):    return _c("36", t)

# ── Utility ────────────────────────────────────────────────────────────────

def safe_json_loads(line):
    """Parse a single JSON line, returning None on failure."""
    line = line.strip()
    if not line:
        return None
    try:
        return json.loads(line)
    except (json.JSONDecodeError, ValueError):
        return None

def ts_to_str(ts):
    """Convert an ISO-8601 timestamp or epoch-ms to readable string."""
    if ts is None:
        return "unknown"
    if isinstance(ts, (int, float)):
        try:
            return datetime.datetime.fromtimestamp(ts / 1000).strftime("%Y-%m-%d %H:%M:%S")
        except Exception:
            return str(ts)
    if isinstance(ts, str):
        try:
            ts_clean = ts.replace("Z", "+00:00")
            dt = datetime.datetime.fromisoformat(ts_clean)
            return dt.strftime("%Y-%m-%d %H:%M:%S")
        except Exception:
            return ts
    return str(ts)

def truncate(text, maxlen=200):
    """Truncate text for display, collapsing whitespace."""
    if not text:
        return ""
    text = str(text)
    text = re.sub(r'\s+', ' ', text).strip()
    if len(text) <= maxlen:
        return text
    return text[:maxlen] + "…"

def decode_project_path(encoded):
    """Decode a project directory name back to a path.
    e.g. '-Users-sam-Projects-myapp' -> '/Users/sam/Projects/myapp'
    Note: This is inherently ambiguous since hyphens in directory names
    are indistinguishable from path separators. The cwd from session
    data is preferred when available.
    """
    if encoded.startswith("-"):
        return "/" + encoded[1:].replace("-", "/")
    return encoded.replace("-", "/")

def file_mod_time(path):
    """Return file modification time as epoch float, or 0."""
    try:
        return os.path.getmtime(path)
    except OSError:
        return 0

def file_size_str(path):
    """Return human-readable file size."""
    try:
        size = os.path.getsize(path)
        if size < 1024:
            return f"{size} B"
        elif size < 1024 * 1024:
            return f"{size / 1024:.1f} KB"
        else:
            return f"{size / (1024 * 1024):.1f} MB"
    except OSError:
        return "?"

# ── JSONL streaming parser ─────────────────────────────────────────────────

def iter_jsonl(filepath):
    """Yield parsed JSON objects from a JSONL file, line by line."""
    try:
        with open(filepath, "r", encoding="utf-8", errors="replace") as fh:
            for lineno, line in enumerate(fh, 1):
                obj = safe_json_loads(line)
                if obj is not None:
                    yield obj
    except (IOError, OSError) as e:
        print(f"  [warning] Could not read {filepath}: {e}", file=sys.stderr)

# ── Resolve which project / session to analyze ─────────────────────────────

def find_all_project_dirs():
    """Return list of (encoded_name, full_path) for all project dirs."""
    results = []
    if not os.path.isdir(PROJECTS_DIR):
        return results
    for entry in sorted(os.listdir(PROJECTS_DIR)):
        full = os.path.join(PROJECTS_DIR, entry)
        if os.path.isdir(full):
            results.append((entry, full))
    return results

def find_session_files(project_full_path):
    """Return dict: { session_id_or_agent: filepath } for all JSONL files."""
    results = {}
    for f in glob.glob(os.path.join(project_full_path, "*.jsonl")):
        basename = os.path.basename(f)
        name = basename.rsplit(".", 1)[0]
        results[name] = f
    return results

def find_most_recent_session(project_full_path):
    """Find the most recently modified main session JSONL (UUID-named, not agent-*)."""
    best_file = None
    best_mtime = 0
    for f in glob.glob(os.path.join(project_full_path, "*.jsonl")):
        basename = os.path.basename(f)
        if basename.startswith("agent-"):
            continue
        mtime = file_mod_time(f)
        if mtime > best_mtime:
            best_mtime = mtime
            best_file = f
    return best_file

def list_projects():
    """Print all available projects and exit."""
    all_projects = find_all_project_dirs()
    if not all_projects:
        print("No project directories found in " + PROJECTS_DIR)
        return
    print(f"\nFound {len(all_projects)} project(s) in {PROJECTS_DIR}:\n")
    print(f"{'Encoded Name':<50} {'Decoded Path':<50} {'Sessions':<10}")
    print(f"{'-'*50} {'-'*50} {'-'*10}")
    for enc, full in all_projects:
        decoded = decode_project_path(enc)
        sessions = find_session_files(full)
        main_count = sum(1 for n in sessions if not n.startswith("agent-"))
        print(f"{enc:<50} {decoded:<50} {main_count:<10}")
    print()

def list_sessions(project_full_path, enc_name):
    """Print all sessions in a project and exit."""
    sessions = find_session_files(project_full_path)
    decoded = decode_project_path(enc_name)
    main_sessions = {n: p for n, p in sessions.items() if not n.startswith("agent-")}
    agent_sessions = {n: p for n, p in sessions.items() if n.startswith("agent-")}

    print(f"\nProject: {decoded}")
    print(f"Directory: {project_full_path}\n")

    if main_sessions:
        print(f"Main Sessions ({len(main_sessions)}):")
        print(f"  {'Session ID':<45} {'Size':<12} {'Modified':<20}")
        print(f"  {'-'*45} {'-'*12} {'-'*20}")
        for name in sorted(main_sessions, key=lambda n: file_mod_time(main_sessions[n]), reverse=True):
            path = main_sessions[name]
            mtime = datetime.datetime.fromtimestamp(file_mod_time(path)).strftime("%Y-%m-%d %H:%M:%S")
            print(f"  {name:<45} {file_size_str(path):<12} {mtime:<20}")
    else:
        print("No main sessions found.")

    if agent_sessions:
        print(f"\nSub-Agent Files ({len(agent_sessions)}):")
        for name in sorted(agent_sessions, key=lambda n: file_mod_time(agent_sessions[n]), reverse=True):
            path = agent_sessions[name]
            mtime = datetime.datetime.fromtimestamp(file_mod_time(path)).strftime("%Y-%m-%d %H:%M:%S")
            print(f"  {name:<45} {file_size_str(path):<12} {mtime:<20}")
    print()

def resolve_target(project_arg, session_arg):
    """
    Resolve the target project directory and session file(s).
    Returns (project_encoded_name, project_full_path, main_session_file, all_jsonl_files_dict).
    """
    all_projects = find_all_project_dirs()
    if not all_projects:
        print("ERROR: No project directories found in " + PROJECTS_DIR, file=sys.stderr)
        sys.exit(1)

    target_project = None

    if project_arg:
        # Try exact match on encoded name
        for enc, full in all_projects:
            if enc == project_arg:
                target_project = (enc, full)
                break
        if not target_project:
            # Try matching the decoded path
            for enc, full in all_projects:
                decoded = decode_project_path(enc)
                if decoded == project_arg or decoded.rstrip("/") == project_arg.rstrip("/"):
                    target_project = (enc, full)
                    break
        if not target_project:
            # Try partial / substring match (case-insensitive)
            project_arg_lower = project_arg.lower()
            candidates = []
            for enc, full in all_projects:
                if project_arg_lower in enc.lower() or project_arg_lower in decode_project_path(enc).lower():
                    candidates.append((enc, full))
            if len(candidates) == 1:
                target_project = candidates[0]
            elif len(candidates) > 1:
                print(f"ERROR: Ambiguous project match for '{project_arg}'. Matches:", file=sys.stderr)
                for enc, _ in candidates:
                    print(f"  {enc}  ->  {decode_project_path(enc)}", file=sys.stderr)
                sys.exit(1)
        if not target_project:
            print(f"ERROR: Could not find project matching '{project_arg}'", file=sys.stderr)
            print("Available projects:", file=sys.stderr)
            for enc, _ in sorted(all_projects):
                print(f"  {enc}  ->  {decode_project_path(enc)}", file=sys.stderr)
            sys.exit(1)
    else:
        # Find the project with the most recently modified JSONL
        best_project = None
        best_mtime = 0
        for enc, full in all_projects:
            for f in glob.glob(os.path.join(full, "*.jsonl")):
                mtime = file_mod_time(f)
                if mtime > best_mtime:
                    best_mtime = mtime
                    best_project = (enc, full)
        if not best_project:
            print("ERROR: No JSONL files found in any project directory.", file=sys.stderr)
            sys.exit(1)
        target_project = best_project

    enc_name, proj_path = target_project
    all_files = find_session_files(proj_path)

    if session_arg:
        main_file = all_files.get(session_arg)
        if not main_file:
            # Try partial match
            matches = [(n, p) for n, p in all_files.items() if session_arg in n and not n.startswith("agent-")]
            if len(matches) == 1:
                main_file = matches[0][1]
            elif len(matches) > 1:
                print(f"ERROR: Ambiguous session match for '{session_arg}':", file=sys.stderr)
                for n, _ in matches:
                    print(f"  {n}", file=sys.stderr)
                sys.exit(1)
        if not main_file:
            print(f"ERROR: Session '{session_arg}' not found in {proj_path}", file=sys.stderr)
            sys.exit(1)
    else:
        main_file = find_most_recent_session(proj_path)
        if not main_file:
            print(f"ERROR: No main session files found in {proj_path}", file=sys.stderr)
            sys.exit(1)

    return enc_name, proj_path, main_file, all_files

# ── Data extraction ────────────────────────────────────────────────────────

class SessionData:
    """Holds extracted data from a single JSONL file."""

    def __init__(self, filepath, label="main"):
        self.filepath = filepath
        self.label = label
        self.messages_count = 0
        self.total_cost = 0.0
        self.total_input_tokens = 0
        self.total_output_tokens = 0
        self.total_cache_creation = 0
        self.total_cache_read = 0
        self.session_id = None
        self.start_time = None
        self.end_time = None
        self.project_path = None
        self.git_branch = None
        self.version = None
        self.slug = None
        self.agent_id = None
        self.model = None

        # Extracted items
        self.files_read = defaultdict(int)
        self.files_written = []
        self.files_edited = []
        self.bash_commands = []
        self.glob_searches = []
        self.grep_searches = []
        self.todo_writes = []
        self.task_invocations = []
        self.git_commits = []
        self.pr_created = False
        self.claude_md_read = False
        self.workflow_config_read = False
        self.agent_files_read = []
        self.rules_files_read = []
        self.skills_invoked = []
        self.timeline = []

    def parse(self):
        """Stream-parse the JSONL file and extract all relevant data."""
        turn_number = 0
        for obj in iter_jsonl(self.filepath):
            msg_type = obj.get("type", "")
            timestamp = obj.get("timestamp")
            self.messages_count += 1

            if msg_type in ("user", "human"):
                self._parse_user(obj, timestamp)
                turn_number += 1
                # Build timeline entry
                message = obj.get("message", {})
                content = message.get("content", "")
                user_text = self._extract_user_text(content)
                self.timeline.append({
                    "timestamp": timestamp,
                    "turn": turn_number,
                    "role": "user",
                    "text": user_text,
                    "tools": [],
                })

            elif msg_type == "assistant":
                self._parse_assistant(obj, timestamp, turn_number)

            elif msg_type == "summary":
                self._parse_summary(obj, timestamp, turn_number)

    def _parse_user(self, obj, timestamp):
        """Extract metadata from a user message."""
        if self.session_id is None:
            self.session_id = obj.get("sessionId")
        if self.project_path is None:
            self.project_path = obj.get("cwd")
        if self.git_branch is None:
            self.git_branch = obj.get("gitBranch")
        if self.version is None:
            self.version = obj.get("version")
        if self.start_time is None:
            self.start_time = timestamp
        self.end_time = timestamp

    def _extract_user_text(self, content):
        """Extract displayable text from user message content."""
        if isinstance(content, str):
            return truncate(content, MAX_USER_MSG)
        elif isinstance(content, list):
            parts = []
            for block in content:
                if isinstance(block, dict):
                    if block.get("type") == "tool_result":
                        result_content = block.get("content", "")
                        if isinstance(result_content, list):
                            # Sometimes content is a list of text blocks
                            texts = []
                            for rc in result_content:
                                if isinstance(rc, dict) and rc.get("type") == "text":
                                    texts.append(rc.get("text", ""))
                            result_content = " ".join(texts)
                        is_err = block.get("is_error", False)
                        prefix = "error" if is_err else "result"
                        parts.append(f"[tool_{prefix}]")
                    elif block.get("type") == "text":
                        parts.append(truncate(block.get("text", ""), 150))
                elif isinstance(block, str):
                    parts.append(truncate(block, 150))
            return " | ".join(parts) if parts else "(tool results)"
        return truncate(str(content), MAX_USER_MSG)

    def _parse_assistant(self, obj, timestamp, turn_number):
        """Parse an assistant message."""
        self.end_time = timestamp
        if self.slug is None:
            self.slug = obj.get("slug")
        if self.agent_id is None:
            self.agent_id = obj.get("agentId")
        if self.model is None:
            self.model = obj.get("model")

        # Token / cost accounting
        self.total_cost += obj.get("costUSD", 0) or 0
        self.total_input_tokens += obj.get("inputTokens", 0) or 0
        self.total_output_tokens += obj.get("outputTokens", 0) or 0
        self.total_cache_creation += obj.get("cacheCreationInputTokens", 0) or 0
        self.total_cache_read += obj.get("cacheReadInputTokens", 0) or 0

        message = obj.get("message", {})
        content = message.get("content", [])
        if isinstance(content, str):
            content = [{"type": "text", "text": content}]

        thoughts = []
        tools_used = []

        if isinstance(content, list):
            for block in content:
                if not isinstance(block, dict):
                    continue
                btype = block.get("type", "")

                if btype == "text":
                    text = block.get("text", "")
                    if text.strip():
                        thoughts.append(truncate(text, MAX_THOUGHT))

                elif btype == "thinking":
                    thinking = block.get("thinking", "")
                    if thinking.strip():
                        thoughts.append("[thinking] " + truncate(thinking, MAX_THOUGHT))

                elif btype == "tool_use":
                    tool_name = block.get("name", "unknown")
                    tool_input = block.get("input", {})
                    tool_id = block.get("id", "")
                    self._process_tool_use(tool_name, tool_input, tool_id, timestamp)
                    tool_desc = self._describe_tool(tool_name, tool_input)
                    tools_used.append({"name": tool_name, "description": tool_desc, "id": tool_id})

        # Also check toolUseMessages array (alternate format)
        for tum in obj.get("toolUseMessages", []):
            if isinstance(tum, dict):
                tool_name = tum.get("name", "")
                tool_input = tum.get("input", {})
                tool_id = tum.get("id", "")
                if tool_name:
                    self._process_tool_use(tool_name, tool_input, tool_id, timestamp)
                    tool_desc = self._describe_tool(tool_name, tool_input)
                    tools_used.append({"name": tool_name, "description": tool_desc, "id": tool_id})

        self.timeline.append({
            "timestamp": timestamp,
            "turn": turn_number,
            "role": "assistant",
            "thoughts": thoughts,
            "tools": tools_used,
        })

    def _parse_summary(self, obj, timestamp, turn_number):
        """Parse a summary message."""
        summary_text = ""
        message = obj.get("message", {})
        content = message.get("content", "")
        if isinstance(content, str):
            summary_text = content
        elif isinstance(content, list):
            for block in content:
                if isinstance(block, dict) and block.get("type") == "text":
                    summary_text += block.get("text", "")
        if summary_text:
            self.timeline.append({
                "timestamp": timestamp,
                "turn": turn_number,
                "role": "summary",
                "text": truncate(summary_text, 1500),
                "tools": [],
            })

    def _process_tool_use(self, name, inp, tool_id, timestamp):
        """Process a tool_use block and update tracking lists."""
        if not isinstance(inp, dict):
            inp = {}

        if name == "Read":
            filepath = inp.get("file_path") or inp.get("filePath") or inp.get("path", "")
            if filepath:
                self.files_read[filepath] += 1
                if "CLAUDE.md" in filepath:
                    self.claude_md_read = True
                if "workflow-config.yml" in filepath or "workflow-config.yaml" in filepath:
                    self.workflow_config_read = True
                if ".claude/agents/" in filepath and filepath.endswith(".md"):
                    self.agent_files_read.append(filepath)
                if ".claude/rules/" in filepath and filepath.endswith(".md"):
                    self.rules_files_read.append(filepath)

        elif name == "Write":
            filepath = inp.get("file_path") or inp.get("filePath") or inp.get("path", "")
            if filepath:
                self.files_written.append(filepath)

        elif name in ("Edit", "MultiEdit"):
            filepath = inp.get("file_path") or inp.get("filePath") or inp.get("path", "")
            if filepath:
                self.files_edited.append(filepath)

        elif name == "Bash":
            command = inp.get("command", "")
            if command:
                self.bash_commands.append(command)
                if "git commit" in command:
                    self.git_commits.append(command)
                if "gh pr create" in command:
                    self.pr_created = True

        elif name == "Glob":
            pattern = inp.get("pattern", "") or inp.get("glob", "")
            if pattern:
                self.glob_searches.append(pattern)

        elif name == "Grep":
            pattern = inp.get("pattern", "") or inp.get("regex", "")
            if pattern:
                self.grep_searches.append(pattern)

        elif name == "TodoWrite":
            self.todo_writes.append(inp)

        elif name == "Task":
            desc = inp.get("description", "")
            command = inp.get("command", inp.get("prompt", ""))
            agent_name = inp.get("agent", "")
            self.task_invocations.append({
                "timestamp": timestamp,
                "description": desc,
                "command": truncate(str(command), 500),
                "agent": agent_name,
                "tool_id": tool_id,
            })

    def _describe_tool(self, name, inp):
        """Return a brief human-readable description of a tool call."""
        if not isinstance(inp, dict):
            return ""
        if name == "Read":
            return inp.get("file_path") or inp.get("filePath") or inp.get("path", "")
        elif name == "Write":
            fp = inp.get("file_path") or inp.get("filePath") or inp.get("path", "")
            return f"write → {fp}"
        elif name in ("Edit", "MultiEdit"):
            fp = inp.get("file_path") or inp.get("filePath") or inp.get("path", "")
            return f"edit → {fp}"
        elif name == "Bash":
            cmd = inp.get("command", "")
            return truncate(cmd, MAX_BASH_CMD)
        elif name == "Glob":
            return inp.get("pattern", "") or inp.get("glob", "")
        elif name == "Grep":
            pat = inp.get("pattern", "") or inp.get("regex", "")
            path = inp.get("path", "")
            if path:
                return f"`{pat}` in {path}"
            return pat
        elif name == "Task":
            desc = inp.get("description", "")
            return f"sub-agent: {truncate(desc, 150)}"
        elif name in ("TodoRead", "TodoWrite"):
            return name
        elif name == "WebSearch":
            return f"search: {inp.get('query', '')}"
        elif name == "WebFetch":
            return f"fetch: {inp.get('url', '')}"
        else:
            # Generic: show first key-value pair
            for k, v in inp.items():
                return f"{k}={truncate(str(v), 100)}"
            return ""

# ── Report generation ──────────────────────────────────────────────────────

class ReportGenerator:
    """Generates the final Markdown report from parsed session data."""

    def __init__(self, enc_name, proj_path, main_session, all_files):
        self.enc_name = enc_name
        self.proj_path = proj_path
        self.main_session_file = main_session
        self.all_files = all_files
        self.main_data = None
        self.agent_data = []
        self.lines = []       # clean markdown
        self.color_lines = [] # with ANSI

    def run(self):
        """Parse all files and generate the report."""
        # Parse main session
        self.main_data = SessionData(self.main_session_file, label="main")
        self.main_data.parse()

        # Find and parse agent files, sorted by modification time
        agent_files = []
        for name, path in self.all_files.items():
            if name.startswith("agent-"):
                agent_files.append((file_mod_time(path), name, path))
        agent_files.sort(key=lambda x: x[0])

        for _, name, path in agent_files:
            ad = SessionData(path, label=name)
            ad.parse()
            self.agent_data.append(ad)

        # Build report sections
        self._section_header()
        self._section_overview()
        self._section_agent_discovery()
        self._section_timeline()
        self._section_sub_agent_timelines()
        self._section_files_touched()
        self._section_rules_config()
        self._section_final_outcome()

        return "\n".join(self.lines), "\n".join(self.color_lines)

    # ── Output helpers ─────────────────────────────────────────────────────

    def _add(self, clean_line, color_line=None):
        self.lines.append(clean_line)
        self.color_lines.append(color_line if color_line is not None else clean_line)

    def _blank(self):
        self._add("")

    def _tool_icon(self, name):
        icons = {
            "Read": "📖", "Write": "✏️", "Edit": "🔧", "MultiEdit": "🔧",
            "Bash": "💻", "Glob": "🔍", "Grep": "🔍", "Task": "🤖",
            "TodoRead": "📋", "TodoWrite": "📋", "WebSearch": "🌐",
            "WebFetch": "🌐",
        }
        return icons.get(name, "⚙️")

    # ── Report sections ────────────────────────────────────────────────────

    def _section_header(self):
        title = "Claude Code Session Analysis Report"
        now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self._add(f"# {title}", bold(f"# {title}"))
        self._blank()
        self._add(f"*Generated: {now}*", dim(f"Generated: {now}"))
        self._blank()
        self._add("---")
        self._blank()

    def _section_overview(self):
        d = self.main_data
        self._add("## 1. Session Overview", bold(cyan("## 1. Session Overview")))
        self._blank()

        session_id = d.session_id or os.path.basename(d.filepath).replace(".jsonl", "")
        project = d.project_path or decode_project_path(self.enc_name)

        # Aggregate totals across main + all agents
        all_sessions = [d] + self.agent_data
        total_cost = sum(s.total_cost for s in all_sessions)
        total_input = sum(s.total_input_tokens for s in all_sessions)
        total_output = sum(s.total_output_tokens for s in all_sessions)
        total_cache_create = sum(s.total_cache_creation for s in all_sessions)
        total_cache_read = sum(s.total_cache_read for s in all_sessions)
        total_jsonl = len(all_sessions)
        total_user_turns = sum(len([e for e in s.timeline if e["role"] == "user"]) for s in all_sessions)
        total_assistant = sum(len([e for e in s.timeline if e["role"] == "assistant"]) for s in all_sessions)
        total_tool_calls = sum(
            sum(len(e.get("tools", [])) for e in s.timeline if e["role"] == "assistant")
            for s in all_sessions
        )

        # Duration
        duration_str = "N/A"
        if d.start_time and d.end_time:
            try:
                start = d.start_time
                end = d.end_time
                if isinstance(start, str) and isinstance(end, str):
                    s_dt = datetime.datetime.fromisoformat(start.replace("Z", "+00:00"))
                    e_dt = datetime.datetime.fromisoformat(end.replace("Z", "+00:00"))
                    delta = e_dt - s_dt
                    minutes = int(delta.total_seconds() // 60)
                    seconds = int(delta.total_seconds() % 60)
                    if minutes > 60:
                        hours = minutes // 60
                        minutes = minutes % 60
                        duration_str = f"{hours}h {minutes}m {seconds}s"
                    else:
                        duration_str = f"{minutes}m {seconds}s"
            except Exception:
                pass

        rows = [
            ("Session ID", f"`{session_id}`"),
            ("Project Path", f"`{project}`"),
            ("Git Branch", f"`{d.git_branch or 'N/A'}`"),
            ("Claude Code Version", f"`{d.version or 'N/A'}`"),
            ("Model", f"`{d.model or 'N/A'}`"),
            ("Session Slug", f"`{d.slug or 'N/A'}`"),
            ("Start Time", ts_to_str(d.start_time)),
            ("End Time", ts_to_str(d.end_time)),
            ("Duration", duration_str),
            ("JSONL Files", f"{total_jsonl} (1 main + {len(self.agent_data)} sub-agent{'s' if len(self.agent_data) != 1 else ''})"),
            ("Main Session File Size", file_size_str(self.main_session_file)),
            ("User Turns (total)", str(total_user_turns)),
            ("Assistant Responses (total)", str(total_assistant)),
            ("Total Tool Calls", str(total_tool_calls)),
            ("Input Tokens", f"{total_input:,}"),
            ("Output Tokens", f"{total_output:,}"),
            ("Cache Creation Tokens", f"{total_cache_create:,}"),
            ("Cache Read Tokens", f"{total_cache_read:,}"),
            ("Total Cost (USD)", f"${total_cost:.4f}"),
        ]

        self._add("| Metric | Value |")
        self._add("|--------|-------|")
        for label, val in rows:
            self._add(f"| {label} | {val} |")
        self._blank()

    def _section_agent_discovery(self):
        self._add("## 2. Agent Discovery", bold(cyan("## 2. Agent Discovery")))
        self._blank()

        if not self.agent_data and not self.main_data.task_invocations:
            self._add("No sub-agents were spawned in this session.")
            self._blank()
            return

        self._add(f"**Main session file**: `{os.path.basename(self.main_data.filepath)}`")
        self._blank()

        if self.agent_data:
            self._add("### Sub-Agent Files")
            self._blank()
            self._add("| # | Agent File | Agent ID | Slug | User Turns | Tool Calls | Cost |")
            self._add("|---|------------|----------|------|------------|------------|------|")
            for i, ad in enumerate(self.agent_data, 1):
                fname = os.path.basename(ad.filepath)
                aid = ad.agent_id or fname.replace("agent-", "").replace(".jsonl", "")
                slug = ad.slug or "—"
                turns = len([e for e in ad.timeline if e["role"] == "user"])
                tool_calls = sum(len(e.get("tools", [])) for e in ad.timeline if e["role"] == "assistant")
                cost = f"${ad.total_cost:.4f}"
                self._add(f"| {i} | `{fname}` | `{aid}` | {slug} | {turns} | {tool_calls} | {cost} |")
            self._blank()

        if self.main_data.task_invocations:
            self._add("### Task (Sub-Agent) Invocations from Main Session")
            self._blank()
            for i, task in enumerate(self.main_data.task_invocations, 1):
                self._add(f"**Task #{i}** — {ts_to_str(task['timestamp'])}")
                if task.get("agent"):
                    self._add(f"- **Agent**: `{task['agent']}`")
                if task.get("description"):
                    self._add(f"- **Description**: {task['description']}")
                if task.get("command"):
                    self._add(f"- **Prompt**: {task['command']}")
                self._blank()

        # Custom agents
        all_agent_files = set()
        for ad in [self.main_data] + self.agent_data:
            all_agent_files.update(ad.agent_files_read)
        if all_agent_files:
            self._add("### Custom Agents Referenced")
            self._blank()
            for af in sorted(all_agent_files):
                self._add(f"- `{af}`")
            self._blank()

    def _section_timeline(self):
        self._add("## 3. Sequential Timeline (Main Session)", bold(cyan("## 3. Sequential Timeline (Main Session)")))
        self._blank()

        if not self.main_data.timeline:
            self._add("_(No messages found in the main session.)_")
            self._blank()
            return

        assistant_num = 0
        for entry in self.main_data.timeline:
            ts = ts_to_str(entry.get("timestamp"))
            role = entry["role"]

            if role == "user":
                text = entry.get("text", "")
                header = f"### Turn {entry['turn']} — User ({ts})"
                self._add(header, bold(green(header)))
                self._blank()
                # Only show user text if it's a real prompt (not just tool results)
                if text and not text.startswith("[tool_"):
                    self._add(f"> {text}")
                    self._blank()

            elif role == "assistant":
                assistant_num += 1
                header = f"### Turn {entry['turn']} — Assistant #{assistant_num} ({ts})"
                self._add(header, bold(magenta(header)))
                self._blank()

                thoughts = entry.get("thoughts", [])
                if thoughts:
                    self._add("**Reasoning / Thoughts:**")
                    self._blank()
                    for t in thoughts:
                        # Escape any markdown blockquote characters inside the text
                        self._add(f"> {t}")
                        self._blank()

                tools = entry.get("tools", [])
                if tools:
                    self._add("**Tool Calls:**")
                    self._blank()
                    for tc in tools:
                        name = tc.get("name", "?")
                        desc = tc.get("description", "")
                        icon = self._tool_icon(name)
                        self._add(f"- {icon} **{name}**: {desc}")
                    self._blank()

                if not thoughts and not tools:
                    self._add("_(empty response)_")
                    self._blank()

            elif role == "summary":
                header = f"### Session Summary ({ts})"
                self._add(header, bold(yellow(header)))
                self._blank()
                self._add(f"> {entry.get('text', '')}")
                self._blank()

        self._add("---")
        self._blank()

    def _section_sub_agent_timelines(self):
        if not self.agent_data:
            return

        self._add("## 3b. Sub-Agent Timelines", bold(cyan("## 3b. Sub-Agent Timelines")))
        self._blank()

        for ad in self.agent_data:
            fname = os.path.basename(ad.filepath)
            aid = ad.agent_id or fname.replace("agent-", "").replace(".jsonl", "")
            slug_str = f" ({ad.slug})" if ad.slug else ""
            header = f"### Sub-Agent: `{fname}`{slug_str}"
            self._add(header, bold(yellow(header)))
            self._blank()

            if not ad.timeline:
                self._add("_(No messages found)_")
                self._blank()
                continue

            assistant_num = 0
            for entry in ad.timeline:
                ts = ts_to_str(entry.get("timestamp"))
                role = entry["role"]

                if role == "user":
                    text = entry.get("text", "")
                    if text and not text.startswith("[tool_"):
                        self._add(f"**User ({ts})**:")
                        self._add(f"> {text}")
                        self._blank()

                elif role == "assistant":
                    assistant_num += 1
                    self._add(f"**Assistant #{assistant_num} ({ts})**:")
                    self._blank()

                    thoughts = entry.get("thoughts", [])
                    if thoughts:
                        for t in thoughts:
                            self._add(f"> {t}")
                            self._blank()

                    tools = entry.get("tools", [])
                    if tools:
                        for tc in tools:
                            name = tc.get("name", "?")
                            desc = tc.get("description", "")
                            icon = self._tool_icon(name)
                            self._add(f"  - {icon} **{name}**: {desc}")
                        self._blank()

            self._add("---")
            self._blank()

    def _section_files_touched(self):
        self._add("## 4. Files Touched Summary", bold(cyan("## 4. Files Touched Summary")))
        self._blank()

        all_sessions = [self.main_data] + self.agent_data

        # Aggregate
        all_reads = defaultdict(int)
        all_writes = []
        all_edits = []
        all_bash = []
        all_globs = []
        all_greps = []

        for sd in all_sessions:
            for fp, count in sd.files_read.items():
                all_reads[fp] += count
            all_writes.extend(sd.files_written)
            all_edits.extend(sd.files_edited)
            all_bash.extend(sd.bash_commands)
            all_globs.extend(sd.glob_searches)
            all_greps.extend(sd.grep_searches)

        # Files Read
        self._add("### Files Read")
        self._blank()
        if all_reads:
            self._add("| File | Read Count |")
            self._add("|------|------------|")
            for fp, count in sorted(all_reads.items(), key=lambda x: (-x[1], x[0])):
                self._add(f"| `{fp}` | {count} |")
            self._blank()
            self._add(f"**Total unique files read: {len(all_reads)}**")
            self._blank()
        else:
            self._add("_(No files read)_")
            self._blank()

        # Files Written
        self._add("### Files Written / Created")
        self._blank()
        if all_writes:
            unique_writes = list(OrderedDict.fromkeys(all_writes))
            for fp in unique_writes:
                self._add(f"- `{fp}`")
            self._blank()
            self._add(f"**Total files written: {len(unique_writes)}**")
            self._blank()
        else:
            self._add("_(No files written)_")
            self._blank()

        # Files Edited
        self._add("### Files Edited / Modified")
        self._blank()
        if all_edits:
            edit_counts = defaultdict(int)
            for fp in all_edits:
                edit_counts[fp] += 1
            self._add("| File | Edit Count |")
            self._add("|------|------------|")
            for fp, count in sorted(edit_counts.items(), key=lambda x: (-x[1], x[0])):
                self._add(f"| `{fp}` | {count} |")
            self._blank()
        else:
            self._add("_(No files edited)_")
            self._blank()

        # Bash Commands
        self._add("### Bash Commands Executed")
        self._blank()
        if all_bash:
            for i, cmd in enumerate(all_bash, 1):
                self._add(f"{i}. `{truncate(cmd, MAX_BASH_CMD)}`")
            self._blank()
            self._add(f"**Total bash commands: {len(all_bash)}**")
            self._blank()
        else:
            self._add("_(No bash commands)_")
            self._blank()

        # Glob / Grep
        if all_globs or all_greps:
            self._add("### File Searches")
            self._blank()
            if all_globs:
                self._add("**Glob patterns:**")
                for g in all_globs:
                    self._add(f"- `{g}`")
                self._blank()
            if all_greps:
                self._add("**Grep patterns:**")
                for g in all_greps:
                    self._add(f"- `{g}`")
                self._blank()

    def _section_rules_config(self):
        self._add("## 5. Rules / Skills / Config Referenced", bold(cyan("## 5. Rules / Skills / Config Referenced")))
        self._blank()

        all_sessions = [self.main_data] + self.agent_data

        claude_md = any(sd.claude_md_read for sd in all_sessions)
        wf = any(sd.workflow_config_read for sd in all_sessions)

        all_agent_files = set()
        all_rules = set()
        all_skills = set()
        all_todos = []
        for sd in all_sessions:
            all_agent_files.update(sd.agent_files_read)
            all_rules.update(sd.rules_files_read)
            all_skills.update(sd.skills_invoked)
            all_todos.extend(sd.todo_writes)

        items = [
            ("CLAUDE.md read", "✅ Yes" if claude_md else "❌ No"),
            ("workflow-config.yml read", "✅ Yes" if wf else "❌ No"),
            ("Custom agent files (.claude/agents/*.md)",
             ", ".join(f"`{f}`" for f in sorted(all_agent_files)) if all_agent_files else "None"),
            ("Rules files (.claude/rules/*.md)",
             ", ".join(f"`{f}`" for f in sorted(all_rules)) if all_rules else "None"),
            ("Skills invoked", ", ".join(sorted(all_skills)) if all_skills else "None"),
            ("TodoWrite entries", str(len(all_todos)) if all_todos else "0"),
        ]

        self._add("| Item | Status |")
        self._add("|------|--------|")
        for label, val in items:
            self._add(f"| {label} | {val} |")
        self._blank()

        if all_todos:
            self._add("### Todo Entries Written")
            self._blank()
            for i, todo in enumerate(all_todos, 1):
                if isinstance(todo, dict):
                    # Try to extract individual todo items
                    todo_items = todo.get("todos", [])
                    if todo_items and isinstance(todo_items, list):
                        self._add(f"**TodoWrite #{i}:**")
                        for item in todo_items:
                            if isinstance(item, dict):
                                status = item.get("status", "?")
                                content = item.get("content", "?")
                                status_icon = {"completed": "✅", "in_progress": "🔄", "pending": "⬜"}.get(status, "❓")
                                self._add(f"  - {status_icon} {content} ({status})")
                        self._blank()
                    else:
                        self._add(f"{i}. {truncate(json.dumps(todo), 300)}")
                else:
                    self._add(f"{i}. {truncate(str(todo), 300)}")
            self._blank()

    def _section_final_outcome(self):
        self._add("## 6. Final Outcome", bold(cyan("## 6. Final Outcome")))
        self._blank()

        # Find last assistant message with actual content
        last_assistant = None
        for entry in reversed(self.main_data.timeline):
            if entry["role"] == "assistant" and (entry.get("thoughts") or entry.get("tools")):
                last_assistant = entry
                break

        if last_assistant:
            self._add("### Last Assistant Message")
            self._blank()
            thoughts = last_assistant.get("thoughts", [])
            if thoughts:
                for t in thoughts:
                    # Use longer truncation for the final message
                    self._add(f"> {truncate(t, MAX_FINAL_MSG)}")
                    self._blank()
            tools = last_assistant.get("tools", [])
            if tools:
                self._add("**Final tool calls:**")
                self._blank()
                for tc in tools:
                    icon = self._tool_icon(tc["name"])
                    self._add(f"- {icon} **{tc['name']}**: {tc['description']}")
                self._blank()
        else:
            self._add("_(No assistant messages found)_")
            self._blank()

        # Git commits
        all_commits = []
        for sd in [self.main_data] + self.agent_data:
            all_commits.extend(sd.git_commits)

        self._add("### Git Activity")
        self._blank()
        if all_commits:
            self._add(f"**{len(all_commits)} git commit(s) detected:**")
            self._blank()
            for cmd in all_commits:
                self._add(f"- `{truncate(cmd, MAX_BASH_CMD)}`")
            self._blank()
        else:
            self._add("No git commits detected in this session.")
            self._blank()

        pr_created = any(sd.pr_created for sd in [self.main_data] + self.agent_data)
        if pr_created:
            self._add("**Pull Request**: ✅ A PR creation was detected.")
        else:
            self._add("**Pull Request**: No PR creation detected.")
        self._blank()

        # Push activity
        pushes = []
        for sd in [self.main_data] + self.agent_data:
            for cmd in sd.bash_commands:
                if "git push" in cmd:
                    pushes.append(cmd)
        if pushes:
            self._add(f"**Git Push**: {len(pushes)} push(es) detected.")
            self._blank()

        self._add("---")
        self._add("*End of report.*")

# ── Main ───────────────────────────────────────────────────────────────────

def main():
    project_arg = sys.argv[1] if len(sys.argv) > 1 else ""
    session_arg = sys.argv[2] if len(sys.argv) > 2 else ""
    list_mode = sys.argv[3] if len(sys.argv) > 3 else ""

    # Handle sentinel values from shell wrapper
    if project_arg == "__NONE__":
        project_arg = ""
    if session_arg == "__NONE__":
        session_arg = ""
    if list_mode == "__NONE__":
        list_mode = ""

    # Handle --list mode
    if list_mode == "--list" or project_arg == "--list" or session_arg == "--list":
        if project_arg and project_arg != "--list":
            # List sessions in a specific project
            all_projects = find_all_project_dirs()
            target = None
            for enc, full in all_projects:
                if project_arg in enc or project_arg in decode_project_path(enc):
                    target = (enc, full)
                    break
            if target:
                list_sessions(target[1], target[0])
            else:
                print(f"Project not found: {project_arg}")
                list_projects()
        else:
            list_projects()
        return

    enc_name, proj_path, main_file, all_files = resolve_target(project_arg, session_arg)

    decoded_path = decode_project_path(enc_name)
    print(f"╔══════════════════════════════════════════════════════════╗", file=sys.stderr)
    print(f"║  Claude Code Session Analyzer                          ║", file=sys.stderr)
    print(f"╚══════════════════════════════════════════════════════════╝", file=sys.stderr)
    print(f"  Project:       {decoded_path}", file=sys.stderr)
    print(f"  Directory:     {proj_path}", file=sys.stderr)
    print(f"  Main session:  {os.path.basename(main_file)} ({file_size_str(main_file)})", file=sys.stderr)
    agent_count = sum(1 for n in all_files if n.startswith("agent-"))
    print(f"  Sub-agents:    {agent_count}", file=sys.stderr)
    print(f"  Analyzing...", file=sys.stderr)
    print(file=sys.stderr)

    gen = ReportGenerator(enc_name, proj_path, main_file, all_files)
    clean_report, color_report = gen.run()

    # Write clean markdown to file
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    outfile = f"claude_session_report_{timestamp}.md"
    with open(outfile, "w", encoding="utf-8") as f:
        f.write(clean_report)
        f.write("\n")
    print(f"  ✓ Report saved to: {os.path.abspath(outfile)}", file=sys.stderr)
    print(file=sys.stderr)

    # Print colored version to stdout
    print(color_report)

if __name__ == "__main__":
    main()
PYTHON_EOF

# ---------------------------------------------------------------------------
# Run the Python script with the user's arguments
# ---------------------------------------------------------------------------
PROJECT_ARG="${PROJECT_DIR_ARG:-__NONE__}"
SESSION_ARG="${SESSION_ID_ARG:-__NONE__}"
LIST_ARG="${LIST_MODE:-__NONE__}"

python3 "$PYTHON_SCRIPT" "$PROJECT_ARG" "$SESSION_ARG" "$LIST_ARG"
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    error "Analysis failed (exit code $EXIT_CODE)"
    exit $EXIT_CODE
fi
