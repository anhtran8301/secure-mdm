# Backend organised as business modules, each with its own layers

Supersedes the package layout of [ADR 0001](0001-layered-backend.md). Technical layers across the
whole app made a change to one feature touch every layer package, so the backend is now split into
modules (`identity`, `device`, `policy`, `command`, `audit`, `dashboard`) grouped under `securemdm/src`, each with its own
`controller`, `service`, `repository`, `entity`, `dto` and `mapper`; only `config`, `security`,
`exception`, `utils` and `common` are shared and sit beside `src`, so the two are easy to tell apart. Service interfaces, hybrid repositories and ModelMapper
from ADR 0001 and 0002 are unchanged.

Modules may depend on each other only without cycles, and shared code must not depend on modules.
Where a module needs to react to another (revoking a device cancels its commands), the owner
publishes a listener interface and the other module implements it. `ArchitectureTests` enforces this.
