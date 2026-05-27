# Mango Productization Issue #26 Sprint 2 Resource Manifest

## 1. Background

Issue #26 requires business modules to have a standard menu, permission and resource synchronization extension. Existing Mango authorization already supports API resource scanning and app module menu runtime config synchronization, but it does not provide a package-consumable manifest contract for business module menus and button permissions.

## 2. Goal

Provide a Mango-managed resource manifest path so business modules can ship menu and permission assets in a jar and let the host synchronize them through authorization services.

## 3. Scope

- Add a backend manifest contract under authorization API.
- Support menu tree and button permission entries in one manifest.
- Add a local authorization core service to upsert manifest menus into `authorization_menu`.
- Add a starter auto runner that loads manifests from classpath resources and invokes authorization API.
- Add remote Feign support so business apps can register manifests to the authorization service.
- Document the manifest format in authorization README.

## 4. Out Of Scope

- No new database table or DDL migration.
- No role assignment automation.
- No deletion of old menus that are not present in the manifest.
- No Mango Initializr implementation.
- No `@mango/admin-shell` productization.
- No full initialization seed package.

## 5. Module Boundaries

| Module | Change |
|--------|--------|
| `mango-authorization-api` | Manifest command and API contract |
| `mango-authorization-core` | Manifest to menu upsert service |
| `mango-authorization-starter` | HTTP controller endpoint |
| `mango-authorization-starter-remote` | Feign client for remote apps |
| `mango-authorization-resource-sync-starter` | Classpath manifest loader |
| `mango-docs/plans` | Sprint plan and ledger |

## 6. API Changes

- Add `AppModuleApi.registerResourceManifest(AppModuleResourceManifestCommand command)`.
- Add HTTP endpoint `POST /authorization/app-modules/resource-manifests/register`.

## 7. Data Changes

- No schema change.
- Manifest entries write existing `authorization_menu` rows by `appCode + moduleCode + menuCode`.
- Runtime page type is stored through existing `frontend_menu_runtime_config`.

## 8. Permission Model

- Directory and page entries are regular menu rows.
- Button permission entries are `menuType=3` rows.
- Runtime authorization continues to read permissions from button menu `menuCode`, preserving existing `SubjectAuthorityServiceImpl` behavior.
- Page-level `permissions` field can carry comma-separated permission codes for frontend display and route metadata.

## 9. Manifest Resource Convention

Default classpath locations:

- `META-INF/mango/resource-manifest.json`
- `META-INF/mango/resource-manifests/*.json`

Each manifest declares `appCode`, `moduleCode`, `moduleName` and a tree of menu nodes. A menu node can include nested `children` and `permissions`; each permission is materialized as a button menu under the node.

## 10. Verification

- Unit tests for manifest registration service.
- Unit tests for classpath manifest loader.
- Maven tests for affected authorization modules.
- Delivery ledger checks for this Sprint and overall Issue #26 plan.

## 11. Completion Standard

- Manifest command, API, local/remote endpoints and runner compile.
- Tests pass for affected modules.
- Ledger contains no incomplete item.
- Remaining larger Issue #26 items stay tracked in the overall plan.
