---
name: debug-issue
description: |
  Systematic debugging workflow for any type of issue.
  Use when user says: "debug this", "why is this failing", "help me fix",
  "this is broken", "error", "bug", "not working", "investigate this",
  "trace this issue", "find the bug"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Debug Issue

Systematic debugging following reproduce → isolate → trace → fix → verify.

## Step 1: Classify the Issue

Determine the issue type and apply the right first action:

**Build failure:**
- Backend: `cd backend && mvn compile 2>&1 | tail -30`
- Frontend: `cd frontend && npx tsc -b 2>&1` or `npm run build 2>&1`

**Test failure:**
- Backend: `cd backend && mvn test -Dtest={FailingTest} 2>&1 | tail -50`
- Frontend: `cd frontend && npx vitest run {test-file} 2>&1`

**Runtime error:**
- Check the error message and stack trace
- For backend: look in `CodeActAgentService.processQuery()` as the main execution path
- For frontend: check browser console and `agentStore.handleAgentEvent()` for WebSocket issues

**Logic bug:**
- Identify the affected code path
- Read the relevant service/component

## Step 2: Isolate

- If backend: identify which layer (controller, service, sandbox, tool) by reading the stack trace
- If frontend: identify which component/store/service by checking the error source
- Known bug hotspots in this repo:
  - `JsonMapConverter` — JSON/JSONB serialization issues
  - `HostPythonExecutor` — state persistence, Python variable capture
  - `agentStore.handleAgentEvent()` — chunk buffering, duplicate messages
  - `MessageItem.tsx` — rendering edge cases

## Step 3: Trace Root Cause

- Read the relevant source code
- Check git history for recent changes: `git log --oneline -10 -- {file}`
- Search for similar past fixes: `git log --all --oneline --grep="fix" -- {file}`

## Step 4: Fix

- Apply the **minimal change** that fixes the bug
- Do NOT refactor surrounding code
- Do NOT add unrelated improvements

## Step 5: Verify

- Run the specific test that reproduces the bug
- Run the full test suite for the affected module
- If no test exists, write a regression test first

## Step 6: Document

- Summarize: what was the bug, what caused it, what fixed it
- Suggest if a rule update is needed to prevent recurrence
