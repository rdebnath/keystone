---
name: feature-delivery
description: Plans and delivers a feature or a significant bug fix in phases. Use when the user asks to implement, build, add, create, develop, deliver, scaffold, wire up, enhance, or extend a feature or capability; work on a ticket, story, epic, or sprint item (e.g. "implement ticket ABC-123", "work on JIRA-456", "fix TICKET-789", "deliver PROJ-42"); add a new endpoint, API, service, module, screen, or UI; or fix a significant, critical, or production bug. Produces a detailed delivery plan first, waits for confirmation, then executes phase by phase (discovery, database/Liquibase, domain and application services, server-side API and STOMP/RabbitMQ messaging, frontend/UI (Flutter), security and observability, testing, delivery). Skips planning for small, low-risk changes.
---

# Feature Delivery

Plan a feature or significant bug fix, get confirmation, then deliver it phase by phase.

## When to use this skill

Apply when the user requests any of the following (triggers, not exhaustive):

**Feature / capability work**
- "implement …", "build …", "add …", "create …", "develop …", "deliver …", "scaffold …", "wire up …"
- "add support for …", "enhance …", "extend …", "roll out …", "introduce …"
- "new endpoint", "new API", "new service", "new module", "new capability", "new screen", "new flow", "new UI", "frontend change", "Flutter screen"

**Ticket / story / epic references**
- "implement ticket ABC-123", "work on ticket …", "deliver ticket …", "fix ticket …", "close ticket …"
- Any ticket id pattern: "JIRA-456", "TICKET-789", "ABC-123", "PROJ-42", "DEV-101"
- "this story", "this epic", "this user story", "this sprint item", "this requirement", "this change request"

**Significant bug fixes**
- "fix this bug", "fix the bug", "significant bug", "critical bug", "production bug", "P0 bug", "P1 bug", "regression"

**General**
- "feature request", "enhancement request", "implement the following requirements", "build out the following"

Also apply for any change that spans more than one layer of the codebase, even if the user
doesn't use these exact words. For small, low-risk changes, apply the skill but skip the
plan (see Step 1).

## Step 0 — Read the project guidelines

Before planning, read `docs/CODING_GUIDELINES_BACKEND.md` and follow it throughout: Java 25,
Spring Boot 4, functional-first style, JPA/Hibernate, Liquibase migrations, STOMP over
RabbitMQ for pub/sub, nginx as the API gateway, and one method call per line on fluent
chains.

## Step 1 — Size the change

Decide whether a plan is required.

**Skip the plan** (and say you are skipping it, with a one-line reason) when the change is
small and low-risk, for example:

- A single-file, single-area fix or tweak
- Adding one field to a DTO or one method to a service
- A config, dependency, or formatting change

**Produce a plan** when the change:

- Introduces a feature or new capability
- Touches multiple layers (web, domain, persistence, messaging)
- Changes the database schema or an API/message contract
- Affects security, performance, or observability
- Requires a migration or a deployment note

When in doubt, produce a plan.

## Step 2 — Produce the delivery plan

Present a plan with these phases in this order; skip any that do not apply. For each phase
list the concrete changes, artifacts, dependencies, and how it will be verified.

### Phases

1. **Discovery & design** — confirm requirements, acceptance criteria, and contracts:
   API shape (request/response DTOs), STOMP destinations and payloads, DB entities, and UI
   flows. Surface open questions before building.
2. **Database changes** — Liquibase changelog(s), JPA entities/repositories, indexes,
   migrations, and any backfill. Never `ddl-auto=update`.
3. **Domain & application services** — pure domain logic, value objects, ports
   (interfaces), use-case services, validation, and error types.
4. **Server-side API & messaging** — REST controllers, request/response mapping, STOMP
   publishers/listeners over RabbitMQ, and nginx routing notes.
5. **Frontend / UI (Flutter)** — screens and widgets, state management (Riverpod), REST
   integration (`dio`), STOMP subscriptions (`stomp_dart_client`), and immutable models
   (`freezed`/`json_serializable`).
6. **Security & observability** — authorization (`@PreAuthorize`), input validation,
   structured logging, metrics, trace/tenant propagation.
7. **Testing** — unit tests (domain + Flutter unit/widget), slice tests (`@WebMvcTest`,
   `@DataJpaTest`), integration tests (`@SpringBootTest` + Testcontainers + broker), and
   E2E where needed.
8. **Delivery** — changelog/release notes, migration/deployment steps, doc updates.

### Plan format

For each phase include:

- **Scope** — what gets built or changed
- **Artifacts** — files, classes, DB objects to create or modify
- **Dependencies** — phases or external systems it depends on
- **Verification** — how we will know it is done (tests, manual checks, etc.)

### Plan files

Write the plan and phase artifacts to disk under `docs/feature/<short-name>/`, where
`<short-name>` is a short slug for the feature or the ticket number (e.g.
`order-cancellation` or `ABC-123`). Create:

- `plan.md` — the overall delivery plan: sizing decision, phase list, open questions, and
  confirmation state.
- One file per phase, numbered and slugified, e.g.:
  - `01-discovery-and-design.md`
  - `02-database-changes.md`
  - `03-domain-and-application-services.md`
  - `04-server-side-api-and-messaging.md`
  - `05-frontend-ui-flutter.md`
  - `06-security-and-observability.md`
  - `07-testing.md`
  - `08-delivery.md`

Each phase file starts with the **Scope / Artifacts / Dependencies / Verification** block
and is updated with progress and results during execution (see Step 4). Skip a phase file
only if the phase does not apply; do not renumber the remaining files.

## Step 3 — Wait for confirmation

Present the plan and **stop**. Do not write code until the user confirms. Allow them to
reorder, drop, or add phases.

## Step 4 — Execute phase by phase

After confirmation, deliver **one phase at a time**:

1. Implement that phase following `docs/CODING_GUIDELINES_BACKEND.md`.
2. Run its verification (tests, build) and report results.
3. Update that phase's `.md` file in `docs/feature/<short-name>/` with what was done,
   verification results, and any decisions or follow-ups.
4. Summarize what changed and what is next.
5. Pause at the phase boundary for review before starting the next phase (unless the user
   said to proceed through all phases without stopping).

## Guardrails

- Never write code before plan confirmation when a plan applies.
- Stay within the current phase; do not pull forward work from later phases.
- Flag scope changes or new risks immediately and update the plan.
- Keep the change minimal and aligned with existing conventions — no speculative features.

## Example

See [examples.md](examples.md) for a worked example plan.
