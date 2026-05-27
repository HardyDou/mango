# Mango Productization Issue #26 Sprint 4 Admin Shell

## 1. Background

Issue #26 requires a reusable frontend admin shell package so business projects can create a Mango admin app through npm dependencies instead of copying `apps/mango-admin-shell/src`.

The current shell app already contains router, layout, stores, runtime menu loading, theme handling and micro app composition. Its implementation is still app-private, so external projects cannot consume it as a stable package.

## 2. Goal

Provide `@mango/admin-shell` as a reusable frontend admin shell package and make `apps/mango-admin-shell` consume that package.

## 3. Scope

- Add package `mango-ui/packages/admin-shell`.
- Move shell runtime, layout, stores, router and bootstrap code into the package.
- Export `createMangoAdminApp(options)` as the stable app creation API.
- Keep `apps/mango-admin-shell` as a thin runnable verification app.
- Keep built-in pages, page registry and runtime composition behavior unchanged.
- Add package boundary tests that reject app-private path dependencies.
- Add Sprint 4 plan and ledger.

## 4. Out Of Scope

- No redesign of shell layout or visual style.
- No new backend API.
- No seed data, Initializr or business module template generation.
- No change to menu database schema.
- No change to micro app runtime protocol beyond package exports.

## 5. Module Boundaries

| Module | Change |
|--------|--------|
| `packages/admin-shell` | New reusable shell package |
| `apps/mango-admin-shell` | Thin app that imports and runs `@mango/admin-shell` |
| `packages/admin-pages` | Remains page registry and built-in page package |
| `mango-docs/plans` | Sprint plan and ledger |

## 6. Public API

`@mango/admin-shell` exports:

- `createMangoAdminApp(options)` for creating and mounting a shell app.
- `installShellApp(app, options)` for installing shell plugins into a Vue app.
- `createMangoAdminRouter(options)` for advanced host customization.
- Runtime/menu/store APIs that existing shell internals and tests need to import from the package.

`createMangoAdminApp` accepts:

- `mountTarget`
- `apiBaseUrl`
- `title`
- `login`
- `modules`
- `localApps`
- `runtimeConfigUrl`

## 7. Data Changes

No database or seed data changes.

## 8. Menu, Page And Permission Changes

No runtime menu or permission data changes.

The package keeps the existing page registry contract: backend menu `component` resolves through `@mango/admin-pages` page loaders.

## 9. Dependency Design

- `@mango/admin-shell` may depend on Mango frontend packages and frontend runtime libraries.
- `@mango/admin-shell` must not depend on `apps/*` paths or app-private aliases.
- Published dependencies must use installable version constraints, not `workspace:*`.
- `apps/mango-admin-shell` depends on `@mango/admin-shell` through workspace during local development.

## 10. Verification

- Delivery ledger check.
- `pnpm -F @mango/admin-shell test`.
- `pnpm -F @mango/admin-shell build`.
- `pnpm -F mango-admin-shell build`.
- `git diff --check`.

## 11. Completion Standard

- External projects can import `createMangoAdminApp` from `@mango/admin-shell`.
- Shell app uses `@mango/admin-shell` instead of private shell source implementation.
- Package boundary test passes.
- Package and shell app build successfully.
- Sprint ledger contains no incomplete item.
