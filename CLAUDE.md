@AGENTS.md

## Claude Code

- Project skills live in `.agents/skills/` and are symlinked into `.claude/skills/`
  (`npx skills update` refreshes them; `skills-lock.json` pins versions).
- `.claude/settings.json` formats edited TypeScript with Prettier after each edit and runs Spotless
  on changed Java when a turn ends. Still run the check commands above before reporting done.
- Context7 is a project MCP server (`.mcp.json`). Approve it once when Claude Code asks, and choose
  "always allow" for its two read-only tools so lookups do not interrupt work. The rule for using it
  lives in `AGENTS.md`.
