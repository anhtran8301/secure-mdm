# Secure MDM: agent guide

Multi-tenant mobile device management demo: Spring WebFlux backend (`be/`), React admin console
(`fe/`), TypeScript device simulator (`simulator/`). All device actions are simulated.
Reply to the user in their language (usually Vietnamese).

- Domain vocabulary: [GLOSSARY.md](GLOSSARY.md). Use these terms in code, tests and issues.
- Decisions: [docs/adr/](docs/adr/). Contracts and transactions: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
- Layer and module rules load on demand from [be/AGENTS.md](be/AGENTS.md) and [fe/AGENTS.md](fe/AGENTS.md).

## Commands

| Area               | Command (run from that directory)                                                                                                                               |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend format     | `./mvnw spotless:apply`                                                                                                                                         |
| Backend full check | `./mvnw verify` (Checkstyle, Spotless check, Testcontainers MySQL, JaCoCo 80/70; needs Docker)                                                                  |
| Backend one test   | `./mvnw -Dtest=WorkflowTests verify`                                                                                                                            |
| Frontend           | `corepack yarn lint`, `corepack yarn typecheck`, `corepack yarn test`, `corepack yarn build`                                                                    |
| E2E                | root: `node scripts/keys.mjs .local/e2e && docker compose -f compose.e2e.yaml --env-file .local/e2e/demo.env up -d --wait`; then `fe`: `corepack yarn test:e2e` |
| Simulator          | `corepack yarn typecheck && corepack yarn test`                                                                                                                 |

Yarn 1.22.22 via Corepack everywhere (`yarn` may not be on PATH; use `corepack yarn`). Never use npm
install. A missing prerequisite (Docker, browsers) is "not verified", never "passed".

## Rules

- Keep API contracts stable: prefix `/api/v1`, errors `{code,message,status}`, pages
  `{items,total,page,size}`. The simulator and E2E depend on them.
- Security invariants are tested; do not weaken them: tenant-scoped queries (foreign tenant = 404),
  refresh rotation and reuse revocation, CSRF on cookie endpoints, device row locks, FIFO leases,
  idempotency keys, separate access/command signing keys, audit in the same transaction.
- Schema changes are new Liquibase changesets; never edit an applied one.
- Never read or print `.local/`, `.env*` or key JSON files; never log secrets or tokens.
- `cross-review` (Claude <-> Codex) is opt-in: run it only when the user invokes it. For a large or
  risky change you may ask once whether to use it; never for small changes.
- Commits follow Conventional Commits with scopes `be|fe|sim|e2e|ci|docs|ai|deps|repo`
  (commitlint + Husky enforce it). Commit, push, deploy or run Strix scans only when asked.

## Docs lookup: Context7 (always on, not only when asked)

Before writing or changing code that uses a library, framework, language feature or CLI, and when
answering questions about them, look the API up with Context7 instead of relying on memory:
`resolve-library-id` (skip it when `be/AGENTS.md` or `fe/AGENTS.md` already lists the id), then
`query-docs` with **one concept per call**. This stack is newer than most training data (Spring Boot 4
/ Framework 7, Java 21, React 19, MUI 7, Zod 4, TanStack Query 5), so "I remember the syntax" is not a
reason to skip. Match the version in `be/pom.xml` or `fe/package.json`; prefer version-specific ids.
Queries leave the machine: ask about the public API, never paste secrets, tokens, keys or our private
source. If Context7 is unreachable, say so and fall back to the official docs, then continue.
The server is declared in `.mcp.json`; the `context7-mcp` skill carries the call procedure.

## Workflow

For a feature or non-trivial change use `/secure-mdm-dev` (Codex: `$secure-mdm-dev`). It chains the
skills below; use them on their own when only that situation applies. Pick by situation, load only what
the task needs, and never ask the user which skill to use.

| Situation                                                                                                | Skill (project skills live in `.agents/skills/`, pinned by `skills-lock.json`)                                                                                                                                           |
| -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Always, for any code or library question                                                                 | Context7 (above), `ponytail` (smallest change, reuse before adding)                                                                                                                                                      |
| Requirements are vague, a design is being decided                                                        | `grill-with-docs`; record terms in `GLOSSARY.md`, trade-offs in `docs/adr/`                                                                                                                                              |
| Agreed scope needs writing down and splitting                                                            | `to-spec`, then `to-tickets`                                                                                                                                                                                             |
| User asks for a second-agent review of a plan or diff (or confirms your offer for a large, risky change) | `cross-review`: Claude and Codex review each other's plan (before building) and diff (after) until both agree, max 3 rounds, then the user decides                                                                       |
| Implementing behavior                                                                                    | `implement` with `tdd`; `codebase-design` when shaping a module interface                                                                                                                                                |
| Building or restyling UI (screens, dashboards, forms)                                                    | `design-taste-frontend` (app-screen rules, keep the MUI theme), `vercel-react-best-practices`, `vercel-composition-patterns`                                                                                             |
| A change is finished                                                                                     | `code-review` (standards + spec), `code-review-expert` (SOLID, security, bugs), `ponytail-review` (over-engineering)                                                                                                     |
| UI change is finished                                                                                    | `web-design-guidelines` (a11y, UX), then verify in the browser with `playwright-cli`                                                                                                                                     |
| Something is broken, failing or slow                                                                     | `diagnosing-bugs` (reproduce first)                                                                                                                                                                                      |
| Structure feels wrong, modules leak                                                                      | `improve-codebase-architecture`                                                                                                                                                                                          |
| Whole-repo health check                                                                                  | `ponytail-audit` (report only, changes nothing)                                                                                                                                                                          |
| Security scan, or fixing scan findings                                                                   | `find-security-vulnerabilities-in-code`, `fix-security-vulnerabilities-with-strix`: **only when the user asks**. Strix sends code to an LLM provider and needs Docker and an API key; keep `strix` out of the allow list |

Browser checks use `corepack yarn playwright-cli ...` from the repo root (installed Chrome, no extra
download). Not installed, by decision (enable only when the user asks): `claude-mem`
(`npx claude-mem install`), `headroom` (needs Python 3.10+), `task-observer`
(`npx skills add rebelytics/one-skill-to-rule-them-all -s task-observer`; `retro` already covers
session retrospectives).

## Agent skills

### Issue tracker

GitHub Issues on `anhtran8301/secure-mdm` via `gh`. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: root `GLOSSARY.md` and `docs/adr/`. See `docs/agents/domain.md`.
