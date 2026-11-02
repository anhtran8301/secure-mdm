---
status: superseded by ADR-0004 (package layout only)
---

# Layered backend packages with service and repository interfaces

The backend moved from feature packages with raw SQL in services to technical layers (`controller`,
`service`/`service.impl`, `repository`/`repository.impl`, `entity`, `dto`, `mapper`, `security`,
`config`, `exception`, `utils`), matching the team's other Spring projects so that code is placed
predictably. Feature cohesion is traded for a familiar convention; domain rules stay pure in
`service.rule`.
