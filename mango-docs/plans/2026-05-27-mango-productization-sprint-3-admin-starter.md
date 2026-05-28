# Mango Productization Issue #26 Sprint 3 Admin Starter

## 1. Background

Issue #26 requires a backend `mango-admin-starter` so business projects can consume Mango admin backend capabilities through one Maven dependency instead of copying app assembly dependencies.

The current monolith app directly lists many infra and platform starters. That works inside the Mango repository, but it is not a productized entry point for external business projects.

## 2. Goal

Provide `io.mango:mango-admin-starter` as a reusable backend admin starter aggregation package.

## 3. Scope

- Add a Maven module named `mango-admin-starter`.
- Aggregate the backend admin runtime starter dependencies already used by the monolith app.
- Keep the module as an assembly package only: no domain implementation, no controller, no database migration.
- Add a dependency boundary test that rejects direct `*-core` dependencies and `server.*` resources.
- Update the monolith app as an assembly example that consumes `mango-admin-starter`.
- Document dependency boundary and usage.

## 4. Out Of Scope

- No frontend `@mango/admin-shell` package.
- No Mango Initializr implementation.
- No seed data package.
- No new menu, role or permission data.
- No microservice topology rewrite.
- No change to file preview engine port behavior.

## 5. Module Boundaries

| Module | Change |
|--------|--------|
| `mango-admin-starter` | New reusable backend admin aggregation starter |
| `mango-app/monolith/mango-monolith-app` | Example app consumes the aggregation starter |
| `mango-docs/plans` | Sprint plan and ledger |

## 6. API Changes

No HTTP API change.

## 7. Data Changes

No schema or seed data change.

## 8. Dependency Design

`mango-admin-starter` directly depends only on public assembly packages:

- Mango infra starters.
- Mango platform starters.
- Third-party Spring Boot developer/runtime dependencies that are already part of the admin app assembly.

It must not directly depend on platform or infra `*-core` modules. Core implementation remains hidden behind each owning starter.

## 9. Configuration Design

The starter does not ship `application.yml`, `application.properties` or any `server.*` default. Host applications keep full ownership of ports, datasource, context path and deployment configuration.

## 10. Verification

- Delivery ledger check.
- `mvn -pl :mango-admin-starter,:mango-monolith-app -am test`.
- `git diff --check`.

## 11. Completion Standard

- `io.mango:mango-admin-starter` exists and compiles.
- Monolith app consumes `mango-admin-starter` as the backend admin assembly example.
- Boundary test passes and guards direct `*-core` dependencies and `server.*` resources.
- Sprint ledger contains no incomplete item.
