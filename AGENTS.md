# AGENTS.md

Instructions for AI coding agents working in this repository. This file is intentionally
short: it points to the authoritative documents rather than duplicating them.

## Project

**Keystone** — a platform (`platform/keystone-*` libraries: Guice · Javalin · jOOQ · Liquibase ·
Supabase Realtime) on which applications are built (`apps/*`), each a holder with a Java backend
(`server/`) and a Flutter frontend (`frontend/` — web, iOS, Android). See `docs/ARCHITECTURE.md`.

## Read first

1. **Architecture** — `docs/ARCHITECTURE.md`
   System-level design: components, data flows, and deployment topology.

2. **Backend coding guidelines** — `docs/CODING_GUIDELINES_BACKEND.md`
   Authoritative for all Java backend code.

3. **Frontend coding guidelines** — `docs/CODING_GUIDELINES_FRONTEND.md`
   Authoritative for all Flutter client code.

4. **UX guidelines** — `docs/UX_GUIDELINES.md`
   Authoritative for what a screen must do for the user. §1 covers every list screen (server-side
   search, filtering, sorting and paging) and names the shared widgets that enforce it; read it
   together with the coding guidelines before building or changing a screen.

5. **Feature delivery skill** — `.agents/skills/feature-delivery/SKILL.md`
   Use when implementing a feature or a significant bug. Produces a phased delivery plan
   written to `docs/delivery/<feature-or-ticket>/` for platform-level changes or to
   `apps/<app>/docs/delivery/<feature-or-ticket>/` for app-specific changes, and executes
   it phase by phase after confirmation. See
   `.agents/skills/feature-delivery/examples.md` for a worked example.
