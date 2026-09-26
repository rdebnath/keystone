# Phase 8 — Delivery

**Scope** — Documentation and deployment notes.

**Artifacts**
- `README.md` (modify) — note the read-replica capability in the `keystone-data` row.
- `docs/ARCHITECTURE.md` (modify) — mention read/write split in the data section (§ around the `keystone-data` description).
- `docs/CODING_GUIDELINES_BACKEND.md` (modify) — §7 persistence: document `Database.read()/write()/transaction()/readFromPrimary(...)` and the rule that reads go through `read()` (replica) and writes through `write()` (primary), with `readFromPrimary(...)` for read-your-writes.
- `apps/inventory/server/src/main/resources/config/application.yaml` comments — document `database.read.*` (yaml only; blank = read/write on the primary).
- CHANGELOG/release note — new `DataAccess` facade; `DataModule` overloaded constructor; read target configured via `database.read.*` in yaml (blank = primary).

**Dependencies** — All prior phases.

**Verification** — Docs render; `mvn test` passes; reviewers confirm the guideline matches the implemented API.
