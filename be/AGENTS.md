# Backend (Java 21, Spring Boot 4, WebFlux, R2DBC/MySQL, Liquibase)

Base package `com.anhtran.securemdm`. Business modules live under `src/` (so
`com.anhtran.securemdm.src.device`, directory `.../securemdm/src/device`); the shared packages sit
next to it, which keeps "module" and "shared" visibly apart.

## Modules (`securemdm/src/<module>`)

Each module root has the same layers (create only the ones a module needs):

| Layer                                                      | Contents                                                                                                                                                                                                         |
| ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `controller`                                               | Thin `@RestController`s: bind/validate input, take `@AuthenticationPrincipal Actor`, call one service. No SQL, no business rules, no repositories.                                                               |
| `service` / `service.impl`                                 | Interface + `*ServiceImpl`. Authorization (`actor.requireAdmin()/requireOperator()` inside `Mono.defer`), transactions (`.as(transactions::transactional)`), audit.                                              |
| `service.rule`                                             | Pure static domain rules (`CommandStateMachine`, `ComplianceEvaluator`); unit-tested in `LogicTests`.                                                                                                            |
| `repository` / `repository.impl` / `repository.projection` | `XxxRepository extends ReactiveCrudRepository<Entity,String>, XxxRepositoryCustom`; hand-written SQL (row locks, upserts, joins, paging) in `XxxRepositoryCustomImpl extends SqlSupport`; read models for joins. |
| `entity` (+ `entity.enums`)                                | `@Table` classes mirroring `db/scripts/sql/*.sql`; implement `Persistable`, set `newEntity(true)` to insert.                                                                                                     |
| `dto.request` / `dto.response` / `dto.model`               | Request records with Bean Validation; response classes (Lombok) for ModelMapper; value objects.                                                                                                                  |
| `mapper`                                                   | `XxxMapper` wraps ModelMapper (STRICT); converts `*Json` columns explicitly.                                                                                                                                     |
| `config`                                                   | Module-owned wiring only (e.g. `command.config.SchedulingConfig`, `identity.config.DemoDataInitializer`).                                                                                                        |

| Module      | Owns                                                                                       |
| ----------- | ------------------------------------------------------------------------------------------ |
| `identity`  | organizations, users, sessions, refresh tokens; `/auth/*`; implements `ActorAuthenticator` |
| `device`    | devices, enrollment tokens, device certificates and proof replay ids; heartbeat; revocation |
| `policy`    | policies, immutable versions, assignments, reports, compliance                             |
| `command`   | signed commands, attempts, leases, FIFO queue, sweeper; cancels commands on revocation     |
| `audit`     | append-only audit log                                                                      |
| `dashboard` | read-only overview across `device` and `command`                                           |

Agent-facing `/device-agent/*` endpoints live in each module's own `*AgentController`
(`device`: heartbeat, `policy`: policy and report, `command`: claim and result).

## Shared packages (`securemdm/config`, `security`, `exception`, `utils`, `common`; they never import `src`)

`config` (`MdmProperties`, clock, ModelMapper, transactions, OpenAPI), `security` (filter chain,
`Actor`, `Role`, JWT, command signer, keys, Device CA, Device Proof checks, `ActorAuthenticator`), `exception`
(`CommonException`, `ErrorCode`, `GlobalExceptionHandler`), `utils` (`AppConstants`, `PageUtils`,
`JsonUtils`, `IdUtils`), `common` (`PageResponse`, `SqlSupport`).

## Module dependencies

`audit` <- `device` <- `identity`, `policy`, `command` <- `dashboard` (and `command` -> `identity`).
`ArchitectureTests` fails on cycles, on shared code importing a module, and on controllers using
repositories. To make a module react to another without a cycle, define a small listener
interface in the module that owns the event (see `device.service.DeviceRevocationListener`,
implemented by `command.service.impl.CommandRevocationListener`).

## Context7 library ids (use with `query-docs`; Boot 4.1 / Framework 7 / Java 21)

Spring Boot `/websites/spring_io_spring-boot_reference_htmlsingle` (or `/websites/spring_io_spring-boot`),
Spring Framework `/websites/spring_io_spring-framework_reference`, Spring Security
`/spring-projects/spring-security`, Spring Data R2DBC `/spring-projects/spring-data-relational`,
Reactor `/reactor/reactor-core`, Liquibase `/liquibase/liquibase-docs`, Testcontainers
`/websites/java_testcontainers`. Anything else: `resolve-library-id` first. If an answer looks like
Boot 2/3 (e.g. `javax.*`, `WebSecurityConfigurerAdapter`), re-query with the version in `pom.xml`.

## Conventions

- Every query is scoped by `organizationId` from the `Actor`; never trust ids from the body.
- Lock the device row before changing device, policy-assignment or command state.
- Pagination: `@RequestParam` defaults from `AppConstants`; `PageUtils.offset` validates (size 1..100);
  services return `Mono<PageResponse<T>>` from `Mono.zip(list, count)`. Only whitelisted filters.
- Throw `CommonException` factories (`notFound()`, `conflict(msg)`, ...); keep messages stable.
- Time comes from the injected `Clock` (tests use a mutable clock); ids from `IdUtils.newId()`.
- Google Java Style: `./mvnw spotless:apply`; Checkstyle (`google_checks.xml`) fails the build.
- Tests: `WorkflowTests` (real HTTP + MySQL Testcontainers) is the safety net; add a scenario for
  behavior changes. Pure rules go to `LogicTests`. No Mockito-only tests for SQL behavior.
