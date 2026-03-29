---
name: create-pr
description: |
  Create a GitHub pull request with auto-populated title and description.
  Use when user says: "create PR", "open pull request", "submit PR",
  "ship it", "open a PR", "make a pull request", "send PR"
allowed-tools:
  - Bash
  - Read
  - Grep
---

# Create Pull Request

Create a GitHub PR with a well-structured title and description.

## Prerequisites

The `gh` CLI must be installed and authenticated. Check:
```bash
gh --version && gh auth status
```

If `gh` is not installed, inform the user and provide the manual alternative:
1. Push the branch: `git push -u origin {branch-name}`
2. Open the PR URL printed by git push
3. Copy the generated description below into the PR body

## Steps

1. **Verify state:**
   ```bash
   git status                          # ensure working tree is clean
   git log --oneline main...HEAD       # commits to include
   git diff --stat main...HEAD         # files changed summary
   ```

2. **Ensure branch is pushed:**
   ```bash
   git push -u origin $(git branch --show-current)
   ```

3. **Generate PR title** — derive from commits:
   - Single commit: use the commit message
   - Multiple commits: summarize the theme (under 70 characters)
   - Use conventional commit prefix if commits follow the pattern

4. **Generate PR body** — compile from commit history and diff:

   ```markdown
   ## Summary
   - [Bullet point for each logical change]

   ## Changes
   - [List of files changed, grouped by module]

   ## Testing
   - [ ] Backend compiles: `cd backend && mvn compile`
   - [ ] Backend tests pass: `cd backend && mvn test`
   - [ ] Frontend type checks: `cd frontend && npx tsc -b`
   - [ ] Frontend tests pass: `cd frontend && npx vitest run`
   - [ ] No new lint errors: `cd frontend && npm run lint`
   ```

5. **Create PR:**
   ```bash
   gh pr create --title "{title}" --body "{body}" --base main
   ```

6. **Report** the PR URL to the user.

## Notes
- Target branch is `main` (the remote default)
- No PR template exists in this repo — the generated body above IS the template
- No CI/CD runs on PRs — the testing checklist is for manual verification
