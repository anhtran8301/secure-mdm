---
name: cross-review
description: Two-agent review loop between Claude and Codex until both agree. Use after a plan or design is drafted (before building) and after code is written (before reporting done), when /secure-mdm-dev or the user asks for cross-review, "Codex review", or a second opinion.
disable-model-invocation: true
---

# Cross-review: Claude <-> Codex

One agent drafts, the other reviews, both revise until they agree. Neither agent's opinion wins by
default; every claim is checked against the code, `GLOSSARY.md` and `docs/adr/`.

Two gates use the same loop:

| Gate     | Artifact reviewed                 | Reviewer also checks                                           |
| -------- | --------------------------------- | -------------------------------------------------------------- |
| **Plan** | the plan or design, before coding | scope, edge cases, ADR conflicts, security invariants, tests   |
| **Code** | the diff, after the build         | spec match, bugs, security, over-engineering, missing tests    |

## When to run it

Opt-in and costly (tokens, and code leaves the machine). Run it only when the user invokes it
(`/cross-review`, `$cross-review`) or confirms an offer. Never for a typo, copy, style, docs or any
small change. Without an invocation, offer it with `AskUserQuestion` only when a change is large or
risky (cross-module, schema, auth, signing, tenancy, leases or locks, API contract, architecture);
if the user declines or does not answer, skip it and say so in the report. The user may ask for
just one gate (Plan or Code).

## Roles

- **Author**: whoever produced the artifact. Revises it after each review.
- **Reviewer**: the other agent. Read-only: it never edits files.
- Claude normally authors the plan and the code. When started from Codex (`$cross-review`), Codex
  authors and Claude reviews. The roles swap with the starting agent; the loop is the same.

## Loop

1. **Package** the artifact in `.scratch/cross-review/<topic>/` (gitignored): `plan.md` or
   `diff.patch` (`git diff <base>`), plus `context.md` with the request, acceptance criteria and
   decisions already made. Never include `.local/`, `.env*`, key files or tokens.
2. **Ask the reviewer** with the template in [reviewer-prompt.md](reviewer-prompt.md). Claude calls
   Codex like this (read-only, output to a file):

   ```bash
   codex exec --sandbox read-only -m <model> -c model_reasoning_effort=<effort> \
     -o .scratch/cross-review/<topic>/review-1.md \
     "$(cat .agents/skills/cross-review/reviewer-prompt.md) Gate: <plan|code>. Topic dir: .scratch/cross-review/<topic>"
   ```

   Pick the tier by size and risk, to save tokens (both model names were checked with the CLI):

   | Change                                                                                  | Model            | `model_reasoning_effort` |
   | --------------------------------------------------------------------------------------- | ---------------- | ------------------------ |
   | Small or routine: one module, UI polish, docs, tests, refactor with no behaviour change | `gpt-6-sol`      | `low`                    |
   | Medium: new endpoint or screen, several files in one layer                              | `gpt-6-sol`      | `medium`                 |
   | Large or complex: cross-module, schema/Liquibase, concurrency (locks, leases, FIFO), auth, signing, tenancy, API contract, architecture decisions | `gpt-6-astra` | `medium` |

   `medium` is the ceiling: never use `high` or above (the user's decision, to save tokens). Rounds
   2 and 3 drop one effort step (they only judge the delta). A `blocking` finding on a
   lower tier, or doubt about the verdict, escalates the next round to `gpt-6-astra` at `medium`. Send the
   reviewer only what it needs: the diff or plan plus `context.md`, not whole files it can read
   itself. Run `codex` from the repo root so it loads `AGENTS.md`.

   When Codex is the author, Claude reviews through a Claude
   subagent (`Agent`, read-only) with the same template.
3. **Triage every finding** in `response-<n>.md`: `accept` (apply it), `reject` (give evidence from
   code, tests or an ADR) or `ask-user` (a product decision, not a technical one). Verify a finding
   before accepting it; reviewers are wrong sometimes, and so is the author.
4. **Revise** the artifact, then send round `n+1` with only the delta and the response file.
5. **Stop** when the reviewer's last message says `VERDICT: AGREE` and no `blocking` item is open,
   or after **3 rounds**. Cap reached with open items: stop and show the user both positions
   (finding, evidence, each side's recommendation) and let the user decide. Never loop silently.

## Finding format (reviewer output)

```
VERDICT: AGREE | REVISE
- [blocking|major|minor|nit] <file or plan section> - <problem> - <evidence> - <suggested fix>
```

`blocking` = wrong behaviour, broken invariant (tenancy, locks, leases, idempotency, signing, audit
in the same transaction), contract break, or missing acceptance criterion. Only `blocking` and
`major` keep the loop going; apply `minor`/`nit` at the author's discretion.

## Guardrails

- Both agents follow `AGENTS.md`: Context7 lookups for library APIs, `ponytail` (smallest change),
  never weaken a security invariant, never edit an applied Liquibase changeset.
- Plan gate reviewer skills: `grill-with-docs` (terms, ADRs), `codebase-design`.
  Code gate reviewer skills: `code-review` (standards + spec), `code-review-expert`
  (SOLID, security, bugs), `ponytail-review` (over-engineering).
- Consensus is about the best idea, not about speed: do not concede a point you can disprove, and do
  not hold one you cannot defend.
- The artifact leaves the machine when sent to Codex. If the change is sensitive, tell the user
  before the first round.
- If the Codex CLI is missing or fails, say so, run the review with a fresh Claude subagent
  instead, and report that the second opinion came from the same vendor. Never claim a Codex
  review that did not happen.
- Report: rounds used, findings accepted / rejected / escalated, and the final verdict.
