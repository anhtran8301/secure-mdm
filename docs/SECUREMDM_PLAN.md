# SecureMDM implementation plan

Approved scope: an interview portfolio with a multi-tenant backend, React admin
console, persistent TypeScript device simulator, signed command queue, policy
reconciliation, audit, CI and reproducible local demo. All device actions are
simulated. FCM, mTLS, cloud and real Android/iOS agents are deferred.

## Milestones

1. M0: real MySQL Testcontainers tests, frontend Vitest/RTL/Playwright and CI.
2. M1: reactive Spring Security, tenant-bound JWT sessions, rotating refresh
   cookies, CSRF, immediate logout and three roles per organization.
3. M2: atomic one-time enrollment, expiring device credentials, heartbeat,
   revocation and a persistent standalone TypeScript simulator.
4. M3: signed ES256 commands, atomic FIFO claims, leases, retry and deduplication.
5. M4: immutable policy versions, revision-bound assignments/reports, compliance
   and real dashboard/audit data.
6. M5: full E2E, Docker Compose, security documentation and interview demo.

## Contracts and defaults

- Java 21, Spring Boot/WebFlux, R2DBC, MySQL 8.4, Liquibase; modular monolith.
- React/TypeScript/Vite/MUI, TanStack Query, same-origin fetch; Yarn 1.22.22.
- Node 24 TypeScript CLI with `enroll`, `run`, `status`, local atomic state and
  single-process locking. Trusted command verification keys are provisioned
  separately. Private keys and simulator credentials stay outside git.
- One organization per user; login takes organizationSlug/email/password.
  ORG_ADMIN can enroll/revoke; OPERATOR manages policies/commands; VIEWER reads.
- Access JWT: 10 minutes, memory only; refresh: random hashed token, HttpOnly
  SameSite=Strict cookie, absolute 7 days. Rotate atomically; reuse revokes the
  family. Check active session/user on every admin request. Cookie auth uses CSRF.
- Enrollment: random hashed one-use token, 15 minutes. Device credential:
  random hashed secret, 30 days. Consume enrollment/create device/audit atomically.
- Heartbeat every 15 seconds; offline after 90 seconds of server-observed silence.
- Commands: REPORT_STATUS, SYNC_DEVICE, SHOW_MESSAGE, LOCK_DEVICE_SIMULATED.
  Idempotency-Key binds request contents; mismatch is 409. Claim via POST with a
  device row lock, FIFO, one active lease, 30 seconds, three attempts, TTL 10 min.
  Reconcile every 5 seconds. Old attempt results are rejected; identical accepted
  results are idempotent. Expiry before delivery is EXPIRED; uncertain execution
  after delivery is UNKNOWN. Revoke cancels undispatched commands and marks
  in-flight commands UNKNOWN. Delivery is at-least-once, not exactly-once.
- JWS ES256 binds issuer, tenant, device, command ID/type/payload/iat/exp; strict
  algorithm/type allowlists and separate admin/command keys. Pre-distribute new
  public keys before rotating signing keys; retain old verification keys until
  old commands are no longer valid.
- Policy versions are immutable; one current assignment/device; assignment
  revision increases on assign/unassign. Stale reports cannot replace current
  state. Rules: requireScreenLock/minAndroidApiLevel/blockedPackages. Compliance:
  PENDING/COMPLIANT/NON_COMPLIANT/UNKNOWN; stale devices become UNKNOWN.
- Audit important mutations transactionally; never log secrets. Append-only via
  application APIs does not imply protection against database administrators.
- API prefix /api/v1; UUID strings; UTC; tenant-qualified resource relationships;
  foreign-tenant resources return 404. Preserve existing error contracts.
  Lists default to 20, cap at 100, and permit only explicit sort/filter fields.

## Verification and delivery

Backend: JUnit/WebTestClient/Testcontainers; test tenant/RBAC, token races,
refresh reuse, revocation, leases, duplicate/stale results, policy revisions and
transaction rollback. Critical business logic coverage: 80% lines/70% branches.
Simulator: persistence/dedup, Java–TypeScript signature interoperability,
tampering, wrong identity/algorithm/key, expiry and key rotation.
Frontend: Vitest/RTL and checked-in Playwright tests for auth/errors/status and
login → enroll → heartbeat → policy → command → result → audit, including loss
of results, restart, tenant isolation and revoke.
CI runs verify/lint/build/tests/E2E without cloud credentials. Docker demo binds
loopback. README, architecture/threat model and 5–7 minute demo must be accurate.
Each milestone uses secure-mdm-dev with relevant review skills; no automatic
commit/push/deploy/Strix. Preserve user changes and report actual verification.
