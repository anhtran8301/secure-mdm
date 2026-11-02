# Hybrid R2DBC repositories: Spring Data plus hand-written SQL

Simple reads and inserts use Spring Data `ReactiveCrudRepository` with `@Table` entities. Row locks
(`SELECT ... FOR UPDATE`), MySQL upserts, joined read models and paged lists stay hand-written in
`XxxRepositoryCustomImpl` (via `SqlSupport` and `DatabaseClient`), because the concurrency guarantees
(enrollment one-use, FIFO claims, lease reconciliation) depend on exact SQL that derived queries
and `@Query` cannot express or make obvious. Do not "simplify" these into derived queries.
