---
name: secure-mdm-dev
description: Develop a requested change in the Secure MDM repository through analysis, implementation, focused skill review, and verification. Use when the user explicitly invokes $secure-mdm-dev or requests this workflow.
---

# Secure MDM development workflow

Run this workflow only when explicitly requested. It coordinates work in the active Codex session; it does not schedule jobs or install hooks. The user supplies the feature, bug, or refactor after `$secure-mdm-dev`. If no task is supplied, ask for the intended change before editing files.

## Understand the change

Locate the repository containing `fe/package.json` and `be/pom.xml`. Read applicable repository instructions, current scripts, and the relevant README. Inspect the working diff and identify existing user edits before making changes.

Trace the affected flow and state the intended behavior, scope, and acceptance criteria briefly. Ask for missing requirements only when they affect correctness. Use `brainstorming` for feature/design work when available, respecting its approval stages and the user's existing decisions. Do not restart approved design discussions. For a bug, reproduce or identify its cause before patching.

## Implement and select relevant guidance

Use `ponytail` to keep the change focused on the request and existing project patterns. Read the appropriate installed skills rather than loading all skills for every task:

| Work | Guidance |
| --- | --- |
| React implementation or performance | `vercel-react-best-practices`; apply React rules compatible with the installed version. The frontend uses Vite, so Next.js-specific rules apply only if that stack actually changes. |
| Component API or composition changes | `vercel-composition-patterns` |
| UI design or visual changes | `design-taste-frontend`; preserve existing MUI components, theme, and branding unless redesign is requested |
| UI accessibility and usability review | `web-design-guidelines` |
| Correctness review of the task's diff | `code-review-expert` |
| Complexity review when abstractions or dependencies change | `ponytail-review` |

Use Context7 for library API documentation, matching installed versions when possible. Do not send secrets or private source code as documentation queries. If a skill or tool is unavailable, report the limitation and continue with the relevant available checks; do not silently install it.

The backend uses Java 21, Spring Boot, WebFlux, R2DBC, and Liquibase. Read `be/README.md` for its current contracts. Preserve reactive behavior and the documented exception responses. Make schema changes with new Liquibase changesets rather than editing applied migrations.

## Review, verify, and fix

Review the changes attributable to this task, including interactions with existing code. Fix findings within the authorized scope. Report unrelated pre-existing problems separately rather than broadening the patch.

Select checks based on affected behavior, using the repository's current commands:

- Frontend changes: from `fe`, run `yarn lint` and `yarn build` when Yarn is the active package manager (currently declared in package.json). If this checkout uses npm, the equivalents are `npm run lint` and `npm run build`; preserve the existing lockfiles. Neither command is a runtime behavior test.
- UI flow changes: use Playwright MCP against the running local app to exercise the changed interactions and relevant error states; inspect console/network errors and responsive layout when affected. Record the URL and scenario actually checked. MCP browser checks do not imply a saved automated test suite. Do not assume `@playwright/test` is installed.
- Backend changes: from `be`, run a focused Maven test while iterating, then `./mvnw verify` for the affected backend delivery. The full suite uses Testcontainers and requires Docker. For advice-only checks, `./mvnw -Dtest=GlobalExceptionHandlerTests verify` is available, but does not replace the full suite. Report missing Docker or other prerequisites as incomplete verification, not a passing result.
- Add a regression test for nontrivial changed behavior when the existing test setup supports it. Documentation-only changes need link/command consistency checks, not application builds.

After fixing an issue, rerun the checks relevant to that fix. Stop optional testing once acceptance criteria and required checks pass. If a check repeatedly fails for an external prerequisite, or continuing would require a product decision or expanding scope, report the specific blocker instead of disabling the check or claiming completion.

Strix scans are a separate, explicitly requested security workflow. Check its current prerequisites when requested; do not upload source or start scans as a default development step. Commit, push, deploy, and changes to shared services require their own user authorization.

## Report the outcome

Reply in the user's language with the changed behavior, important files, actual review and test results, and any remaining blocker. Distinguish passed, failed, and not-run checks. Include only commands and evidence observed in this run.
