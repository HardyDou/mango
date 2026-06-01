# Sprint 1 Salvage Matrix

Status: `PENDING_USER_ACCEPTANCE`

## Matrix

| Area | Failed Branch Input | Decision | Reason | Next Sprint |
|---|---|---|---|---:|
| Admin shell layout | `mango-ui/packages/admin-shell/src/layout/*` | DROP direct code | It is a divergent copy from the original app shell. | 2 |
| Shell app API | `createMangoAdminApp` concept | REWORK | API direction is useful, but implementation must use one shell owner. | 2 |
| `@mango/admin` facade | `mango-ui/packages/admin/*` | KEEP concept | Facade entry matches dependency-based business development. | 4 |
| `@mango/admin/style.css` | package style export | KEEP concept | Consumers need one stable style entry. | 4 |
| Menu merge runtime | `menuHost.ts`, `menuMerge.ts` | REWORK | Useful mechanics, but full mode cannot silently add capability/business menus. | 3 |
| Dev menu injection | `createDevRouteMenus()` | REWORK | Dev-only tools must be explicit and separately reported. | 3 |
| Fallback menus | capability/business fallback | REWORK | Diagnostics are useful; fallback cannot count as full-mode success. | 3 |
| Capability manifests | `@mango/admin-pages` capability metadata | REWORK | Need correct semantics: pages/resources, not hidden menu authority. | 5 |
| Split API packages | `@mango/*-api` | KEEP concept | API packages can serve admin and non-admin UI. | 5 |
| Split admin packages | `@mango/*-admin` | KEEP concept | Admin packages bind pages to admin-shell. | 5 |
| Legacy `@mango/file`, `@mango/rbac` changes | mixed package edits | REWORK | Need staged compatibility and package contract checks. | 5 |
| Package contract checker | `check-package-contracts.mjs` | REWORK | Useful guard, must align with PMO and actual package boundary. | 4 |
| create-mango-app full preset | generated full admin app | REWORK | Must wait for shell, menu and style contracts. | 6 |
| create-mango-app custom preset | generated custom app | REWORK | Must be explicit customization after full preset passes. | 7 |
| Starter business menu | `starterMenus.ts` | DROP direct default | Business menus cannot appear by default in full baseline. | 7 |
| Mode matrix scripts | local/micro/mixed checks | KEEP concept | Needed later, but only after basics are stable. | 8 |
| Remote/registry/release work | release checks and remote assumptions | DROP | Outside current correction boundary. | none |
| Old screenshots/reports | failed branch evidence dirs | DROP as proof | Produced from divergent branch and cannot verify correction branch. | none |
| Old plans | failed branch plan docs | DROP as current scope | Current source is the correction plan and PMO. | none |
| Root ignore/agent churn | `.ignore`, `AGENTS.md` changes | DROP unless independently needed | Not required for Sprint 2 implementation. | none |

## Direct Cherry-pick Policy

No file from the failed branch is approved for direct cherry-pick by this matrix.

Allowed use:

- Read implementation ideas.
- Reuse test intent after rewriting against the correction branch.
- Reuse naming direction when it matches the correction plan.

Disallowed use:

- Carrying over a copied shell.
- Treating old screenshots as proof.
- Accepting menu drift as success.
- Expanding into remote registry, release platform, gray release or cache governance.
