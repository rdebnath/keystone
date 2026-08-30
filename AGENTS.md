# AGENTS.md

Instructions for AI coding agents working in this repository. This file is intentionally
short: it points to the authoritative documents rather than duplicating them.

## Project

**Keystone** — an enterprise-grade Spring Boot 4 application with a Flutter client
(web, iOS, Android).

## Read first

1. **Backend coding guidelines** — `docs/CODING_GUIDELINES_BACKEND.md`
   Authoritative for all backend code. Summary: Java 25 (pinned), Spring Boot 4 (Spring
   Framework 7, Jakarta EE 11), JPA/Hibernate 7, PostgreSQL, Liquibase migrations, STOMP
   over RabbitMQ for pub/sub, nginx as the API gateway, functional-first programming style,
   one method call per line on fluent chains, and Maven and Gradle both maintained.

2. **Frontend coding guidelines** — `docs/CODING_GUIDELINES_FRONTEND.md`
   Authoritative for all Flutter client code. Summary: Dart 3, Riverpod (codegen), freezed +
   json_serializable, dio for REST, stomp_dart_client for STOMP/RabbitMQ, go_router,
   flutter_appauth (OAuth2 PKCE), functional and immutable style.

3. **Feature delivery skill** — `.common/skills/feature-delivery/SKILL.md`
   Use when implementing a feature or a significant bug. Produces a phased delivery plan
   written to `docs/feature/<short-name>/` and executes it phase by phase after
   confirmation. See `.common/skills/feature-delivery/examples.md` for a worked example.
