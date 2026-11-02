---
name: secure-mdm-dev
description: Take a Secure MDM feature, bug or refactor from idea to verified change - align, spec, slice, build test-first, review and verify with this repo's checks. Use when the user invokes /secure-mdm-dev or $secure-mdm-dev.
disable-model-invocation: true
---

# Secure MDM development flow

The user supplies the change after the command. If none is given, ask for it before touching files.
Reply in the user's language. Read `AGENTS.md`, then `be/AGENTS.md` and/or `fe/AGENTS.md` for the
layers you will touch. Check `git status` and treat existing uncommitted edits as the user's.

Two things apply at every step, without being asked: look up any library, framework or language API
with Context7 before relying on memory (`AGENTS.md` explains how and lists the ids), and apply
`ponytail` (smallest change, reuse before adding). Pick other skills by the situation table in
`AGENTS.md`; never ask the user which skill to use.

Scale the flow to the change: a typo or one-line fix goes straight to **Build** and **Verify**.
A bug starts with **Diagnose**. Anything that changes behavior, contracts or data goes through all steps.

## 1. Align - `grill-with-docs`

Interview the user until scope, acceptance criteria and edge cases are settled, using
`GLOSSARY.md` terms. Check claims against the code (tenancy, locks, revisions, leases). Record
new terms in the glossary and real trade-offs as ADRs in `docs/adr/`. Do not reopen decisions
already recorded in an ADR unless the user asks; flag conflicts explicitly.

## 2. Spec and slice - `to-spec`, then `to-tickets`

Publish the agreed spec and tracer-bullet tickets to GitHub Issues (`docs/agents/issue-tracker.md`).
Skip publishing when the user wants to stay local; keep the same structure in the conversation.

## 3. Build - `implement` with `tdd`

One vertical slice at a time, red-green-refactor at these seams:

- Backend behavior: a scenario in `WorkflowTests` (real HTTP + MySQL); pure rules in `LogicTests`.
- Frontend: Vitest for logic in `src/lib` or schemas; Playwright `e2e/workflow.spec.ts` for flows.
- Simulator: its Vitest suite; keep Java-TypeScript signature interop green.

Follow the module rules (backend: a module under `securemdm/src/<module>` goes controller -> service
interface -> repository, DTO <-> entity via mapper, no cycles between modules; frontend: TanStack
Query key factories, RHF + Zod). Schema changes are new Liquibase changesets. Reuse what exists
(es-toolkit, Day.js, Minimal components) before adding code or a dependency.
For React work consult `vercel-react-best-practices`; for component APIs `vercel-composition-patterns`;
for new or restyled screens `design-taste-frontend` (app-screen rules, keep the MUI theme).

## Diagnose - `diagnosing-bugs`

Reproduce first (failing test or script), find the cause, then fix through **Build**.

## 4. Review - `code-review`

Review the diff since the starting point on Standards (AGENTS.md files, ADRs) and Spec (the
ticket). Add `code-review-expert` for SOLID, security and bug findings and `ponytail-review` to
catch over-engineering. For UI changes also run `web-design-guidelines`. Fix findings within scope;
report unrelated pre-existing problems separately.

## 5. Verify

Run only what the change affects, from the right directory:

- Backend: `./mvnw spotless:apply`, then `./mvnw verify` (Checkstyle, Spotless check, Testcontainers, JaCoCo 80/70). Needs Docker.
- Frontend: `corepack yarn lint`, `corepack yarn typecheck`, `corepack yarn test`, `corepack yarn build`.
- Flows across services: the E2E commands in `AGENTS.md`; for UI behavior also exercise the running
  app with `playwright-cli` (`corepack yarn playwright-cli ...` from the repo root) and check
  console/network errors. Record the URL and scenario checked.
- Simulator: `corepack yarn typecheck && corepack yarn test`.

Re-run the checks a fix touches. If a prerequisite is missing or a check keeps failing for an
external reason, stop and report the blocker; never disable a check or claim it passed.

## Report

State the changed behavior, key files, and each check as passed / failed / not run, with only
evidence observed in this session. Commit (Conventional Commits), push, deploy and Strix scans
need explicit user authorization.
