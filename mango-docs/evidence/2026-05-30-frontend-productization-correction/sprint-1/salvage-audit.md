# Sprint 1 Salvage Audit And Reimplementation Strategy

Status: `PENDING_USER_ACCEPTANCE`

## 1. Scope

Sprint 1 only audits the failed frontend productization branch.

- Current correction branch: `worktree/frontend-productization-correction-sprint-0`
- Failed input branch: `worktree/frontend-productization-plan`
- Baseline branch: `main`
- Product code changed in Sprint 1: none
- Verification type: diff audit, shell duplication audit, menu source audit, style/export audit

Sprint 1 does not implement package extraction, menu fixes, shell extraction, starter generation, deployment modes or visual changes.

## 2. Diff Scale

Evidence files:

- `diff-name-status.txt`
- `diff-stat.txt`
- `diff-category-summary.json`

Summary:

| Category | Count |
|---|---:|
| Total changed files | 570 |
| Admin shell package | 17 |
| Admin facade package | 9 |
| Split API/admin packages | 89 |
| Legacy packages touched | 46 |
| Starter/templates | 77 |
| Scripts/checks | 10 |
| Old evidence | 156 |
| Old plans | 45 |
| PMO/root config | 8 |
| Apps | 6 |
| Other | 107 |

Conclusion: the failed branch mixes shell extraction, package split, starter template, mode matrix, verification scripts, old evidence, old plans and root governance changes. It cannot be cherry-picked as a whole.

## 3. Critical Findings

### 3.1 Divergent Shell Copy

Evidence:

- `current-shell-layout-diff.txt`

The current codebase has separate layout implementations under:

- `mango-ui/apps/mango-admin/src/layout`
- `mango-ui/packages/admin-shell/src/layout`

The diff report shows differences across layout entry, aside, header, main, nav bars, tags view, settings drawer, user menu and route view files.

Decision:

- The duplicated shell implementation is not acceptable as the final model.
- Sprint 2 must establish a single source of truth for the Mango Admin shell.
- Any temporary migration copy must be removed within the same Sprint before acceptance.

### 3.2 Menu Drift Source

Evidence:

- `menu-source-report.txt`
- Sprint 0 `menu-report.json`
- Sprint 0 `layout-report.json`

Sprint 0 backend menu API returned 4 first-level menus:

- `系统管理`
- `审批中心`
- `平台能力`
- `通知中心`

Browser top navigation also displayed `开发中心`.

The source audit found `mango-ui/packages/admin-shell/src/runtime/menuHost.ts` appends `createDevRouteMenus()` to loaded menus in dev mode. The original app has a separate `mango-ui/apps/mango-admin/src/config/menuLoader.ts` path with a comment that `开发中心` is dev-only and does not enter test/production menus.

Decision:

- `开发中心` must not be silently accepted as a default full-mode backend menu.
- Sprint 3 must define menu modes clearly: backend-first full mode, explicit custom/business mode, and separately reported dev diagnostics.
- Capability packages may provide page resolution and resource metadata, but must not silently invent top-level menus in full/default mode.

### 3.3 Style Export Is Incomplete In Current Baseline

Evidence:

- `style-export-report.txt`

Current baseline has `@mango/admin-shell` and internal style imports, but no stable `@mango/admin/style.css` facade package in this correction branch. The failed branch added the concept of an admin facade package and style export.

Decision:

- Keep the concept of `@mango/admin/style.css`.
- Reimplement it after Sprint 2 and Sprint 3, so the style export reflects the single-source shell and stable menu/runtime contract.
- Do not fix missing styles by writing a new skin or page-level CSS patches.

### 3.4 Failed Branch Evidence Cannot Prove Success

The failed branch contains many old screenshots and reports under dated evidence directories, but those reports were produced against a divergent shell and moving menu behavior.

Decision:

- Old evidence is not proof for this correction plan.
- It can be used only to identify useful test script ideas and failure modes.
- New Sprint evidence must be generated from the correction branch after each implemented change.

## 4. Salvage Decisions

The failed branch contains useful product ideas, but most are not safe as direct code.

Keep as concept:

- API/admin package split direction, such as `@mango/file-api` and `@mango/file-admin`.
- `@mango/admin` facade entry with `createMangoAdmin`.
- Aggregated `@mango/admin/style.css` entry.
- Capability manifest and page registry idea.
- Package contract checker idea.
- E2E script ideas for screenshots, full/custom preset checks and mode matrix.
- create-mango-app preset concept.

Rework before implementation:

- `admin-shell` runtime and layout.
- Menu merge and fallback behavior.
- Preset resolution defaults.
- Starter template menu behavior.
- Capability manifest semantics.
- Package contract checks.
- Deployment mode checks.

Drop from correction implementation:

- Divergent copied shell layout.
- Any menu injection accepted as full-mode behavior.
- Any mock/fallback menu accepted as full-mode success.
- Old evidence as proof.
- Old plans as current scope.
- Release, remote registry, rollback, cache governance, gray release or platform work.
- Root/PMO churn not independently required by this correction plan.

## 5. Reimplementation Map

| Target | Sprint | Source Input | Required Rule |
|---|---:|---|---|
| Single-source shell | 2 | Original `apps/mango-admin` shell plus failed branch API idea | One shell owner; `apps/mango-admin` consumes the same package as generated apps |
| Menu contract | 3 | Existing backend menu API, original `menuLoader`, failed `menuHost` lessons | Full mode follows backend menu; custom menus explicit; fallback reported |
| Style/package contract | 4 | Failed `@mango/admin/style.css` idea | Consumers import stable package exports only |
| Capability package split | 5 | Failed split package direction | `*-admin` binds admin shell; `*-api` remains usable outside admin UI |
| create-mango-app full preset | 6 | Failed starter template direction | Generated full project starts as usable Mango Admin through dependencies |
| create-mango-app custom preset | 7 | Failed custom preset tests | Customization is explicit and tested against full baseline |
| Deployment matrix | 8 | Failed mode matrix scripts | Only local/micro/mixed basics; no remote governance scope |

## 6. Sprint 2 Entry Conditions

Sprint 2 may start only after user acceptance of this report.

Sprint 2 must start from these decisions:

- Do not cherry-pick the failed branch as a whole.
- Do not carry forward a copied or visually approximate shell.
- Do not hide menu drift with manual menu edits.
- Implement the shell as a single source of truth first.
- E2E screenshots and visual inspection are mandatory before Sprint 2 acceptance.

## 7. Verification

Commands executed:

```bash
node mango-pmo/tools/pmo-preflight.mjs --role tech-lead --phase design --task "Sprint 1 salvage audit for failed frontend productization branch: compare against main, classify changes KEEP REWORK DROP, produce reimplementation map without product code changes" --paths "mango-docs/evidence,mango-docs/plans,mango-ui,create-mango-app,package.json,pnpm-workspace.yaml"
git status --short --branch
git diff --name-status main...worktree/frontend-productization-plan
git diff --stat main...worktree/frontend-productization-plan
diff -qr mango-ui/apps/mango-admin/src/layout mango-ui/packages/admin-shell/src/layout
rg -n "开发中心|createDevRouteMenus|capabilityMenus|businessMenus|fallbackMenus|mergeShellMenus|/authorization/menus/user|menuLoader|config/menu" mango-ui/packages/admin-shell/src mango-ui/apps/mango-admin/src
rg -n "style\\.css|index\\.css|index\\.scss|theme/index.scss|exports|createMangoAdmin|createMangoAdminApp|ShellView" mango-ui/packages/admin-shell mango-ui/apps/mango-admin mango-ui/packages/*/package.json
```

Result:

- Diff evidence generated.
- Shell duplication evidence generated.
- Menu source evidence generated.
- Style/export evidence generated.
- No product code changed.

## 8. Risks

- Sprint 2 is the highest-risk correction step because it touches the shell owner boundary.
- Current baseline still has the known dev-menu drift finding from Sprint 0.
- Current baseline does not yet provide the final `@mango/admin` facade package.
- Sprint 1 does not prove runtime behavior; runtime proof resumes in Sprint 2 after implementation.
