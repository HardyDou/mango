# Mango Frontend Productization Correction Plan

## 1. Goal

Restore the Mango frontend productization work to the correct target:

- A business project can depend on Mango npm materials and start with the complete original Mango Admin experience.
- The default admin experience is the full Mango Admin shell, menus, top bar, notice bell, tags view, theme/settings drawer, route behavior, styles and built-in capabilities.
- Custom projects can configure, add or remove capabilities after the full default baseline is proven.
- Monolith, micro frontend and mixed deployment modes remain supported, but baseline stability and original Mango parity come first.

## 2. Scope

- Rebuild the admin shell productization from `main`, not by continuing to patch the divergent sample result.
- Preserve useful package work only after review: API package split, admin capability packages, package checks, create-mango-app scaffolding and E2E scripts.
- Replace the copied/divergent admin shell with a single-source shell model.
- Stabilize menu, style, route, permission and package contracts.
- Require automatic verification and user manual acceptance after every Sprint.

## 3. Out Of Scope

The following are explicitly out of scope for this correction plan:

- Gray release.
- A/B traffic strategy.
- Remote registry delivery.
- CDN cache governance.
- Remote module version rollback.
- Release platform.
- Monitoring and alerting platform.
- Performance专项优化.
- Redesigning Mango Admin visuals.
- Writing a new shell that only looks like Mango.
- Fixing visual issues by adding unrelated page-level CSS.

## 4. Design Inputs

- `main` branch is the source of the current accepted Mango baseline.
- `worktree/frontend-productization-plan` is only a historical input for salvage analysis.
- Historical productization plans under `mango-docs/plans` are reference material, not completion proof.
- PMO rules under `mango-pmo` are the only long-term rules source.

## 5. Non-Negotiable Design Decisions

### 5.1 Single Source Admin Shell

There must not be two long-term implementations of the Mango Admin layout.

Final acceptable state:

- The original Mango Admin shell source is extracted into a package, or the package is otherwise made the single implementation owner.
- `apps/mango-admin` consumes the same package used by generated business projects.
- `apps/mango-admin` and `packages/admin-shell` do not keep separate copies of layout, nav bar, tags view, theme/settings, user menu, notice bell, menu loader or route host logic.
- Any migration step that temporarily duplicates files must end in the same Sprint and must be removed before Sprint acceptance.

### 5.2 Default Means Full Mango

The default generated admin app must behave as full Mango Admin:

- Same first-screen layout as baseline.
- Same top bar tools, including notice bell.
- Same menu source and menu set when connected to the same backend and user.
- Same style chain and theme behavior.
- Same login page style and auth flow.
- Same built-in capability pages for the selected full preset.

### 5.3 Menu Contract

Backend menu data is the source of truth for Mango Admin menus.

- Capability packages provide page loaders, route metadata and resource declarations.
- Capability packages do not silently invent top-level menus in the default full mode.
- Business menus are additive only when explicitly configured.
- Fallback menus can be used for local diagnostics only and must be reported as fallback, not accepted as full Mango verification.

### 5.4 Style Contract

Published admin materials must expose a stable style entry:

- `@mango/admin/style.css` aggregates required shell, common, Element Plus, auth and built-in capability styles.
- Consumers must not need to know internal package style files to render the default admin correctly.
- Missing styles are fixed by package exports/import chain, not by writing a new visual skin.

### 5.5 Verification Contract

Every Sprint must stop for acceptance.

Before moving to the next Sprint:

- Regression tests must pass.
- New feature tests must pass.
- Real backend must be used for menu, auth, data and permission validation unless the Sprint is explicitly only package/static verification.
- E2E screenshots must be saved.
- Screenshot review must compare UI layout, style, colors and required Mango elements against the baseline.
- Every first-level menu must be sampled.
- For each sampled first-level menu, 1 to 3 child pages or child functions must be sampled.
- Data/API results must be checked against real requests where applicable.
- User manual acceptance must be recorded in the Sprint ledger before proceeding.

## 6. Sprint Plan

### Sprint 0: Containment And Baseline Lock

Goal:

- Stop relying on the failed branch as proof.
- Establish the clean baseline and evidence that future Sprints must match.

Scope:

- Create a new correction worktree from `main`.
- Keep the failed worktree read-only as salvage input.
- Capture baseline screenshots from current Mango Admin with real backend.
- Record baseline menu tree, top bar elements, login page, settings drawer, user dropdown, tags view and representative pages.

Automatic verification:

- `git status --short --branch`.
- `pnpm install`.
- `pnpm -F mango-admin build`.
- Real backend startup check.
- Baseline E2E with screenshots and layout report.

Manual acceptance:

- User confirms the baseline screenshot is the expected original Mango Admin.
- User confirms the captured menu tree matches the original Mango expectation.
- User confirms top bar includes expected tools such as notice bell.
- User confirms login page, home page, settings drawer and user dropdown match Mango.

Exit criteria:

- Baseline evidence directory exists.
- Baseline report lists screenshot absolute paths.
- User acceptance recorded.

### Sprint 1: Salvage Audit And Revert Strategy

Goal:

- Decide exactly which failed-branch changes are salvageable and which must be discarded.

Scope:

- Compare `worktree/frontend-productization-plan` against `main`.
- Classify changes into `KEEP`, `REWORK`, `DROP`.
- Produce a cherry-pick or reimplementation map.
- No product code changes except documentation and ledger updates.

Automatic verification:

- `git diff --name-status main...worktree/frontend-productization-plan`.
- Layout diff report between original app and failed admin-shell.
- Menu source report.
- Style import/export report.

Manual acceptance:

- User reviews the salvage matrix.
- User confirms no divergent shell copy will be carried forward.
- User confirms the next Sprint implementation scope.

Exit criteria:

- Salvage matrix is committed to the plan or a linked report.
- User acceptance recorded.

### Sprint 2: Single-Source Admin Shell Extraction

Goal:

- Make the admin shell package use the original Mango Admin shell as the single source.

Scope:

- Extract original shell layout/runtime from `apps/mango-admin` into `packages/admin-shell`, or move ownership to the package and update `apps/mango-admin` to consume it.
- Preserve nav bar, notice bell, tags view, user dropdown, settings drawer, layout modes and theme behavior.
- Remove duplicate long-term shell implementation.

Automatic verification:

- Package boundary test: `packages/admin-shell` must not import from `apps/*`.
- Duplication check: no separate long-term layout copies in app and package.
- `pnpm -F @mango/admin-shell test`.
- `pnpm -F @mango/admin-shell build`.
- `pnpm -F mango-admin build`.
- E2E full shell screenshot comparison against Sprint 0 baseline.

Manual acceptance:

- User compares screenshots for login, home, top bar, left menu, tags, settings drawer and user dropdown.
- User confirms notice bell is present and visually consistent.
- User confirms the main framework is not a new copied shell.

Exit criteria:

- Original Mango Admin and packaged shell render equivalent framework screenshots.
- User acceptance recorded.

### Sprint 3: Stable Menu And Route Contract

Goal:

- Make menu behavior stable and backend-first.

Scope:

- Define and implement the menu contract for full/default and custom modes.
- Full/default mode uses backend menu as source of truth.
- Capability packages provide component/page resolution and resource metadata.
- Business/custom menus are explicit configuration only.
- Fallback menu usage is detectable and fails full-mode verification.

Automatic verification:

- Unit tests for menu merge and fallback diagnostics.
- Real backend menu API capture.
- E2E verifies UI menu equals backend menu for full mode.
- Menu sampling screenshots: every first-level menu, 1 to 3 child pages/functions per first-level menu.

Manual acceptance:

- User reviews backend menu capture and page menu screenshot comparison.
- User confirms no unexpected generated/dev/business menus appear in full mode.
- User approves custom-mode behavior only after full-mode passes.

Exit criteria:

- Full-mode menu equals backend menu under the same user.
- User acceptance recorded.

### Sprint 4: Style And Package Export Contract

Goal:

- Make npm consumers render Mango correctly through documented style/package entries.

Scope:

- Stabilize `@mango/admin`, `@mango/admin-shell`, auth, common and capability package exports.
- Add `@mango/admin/style.css` aggregation.
- Ensure login page, auth pages, shell and capability pages load required styles without consumer internal imports.
- Remove accidental reliance on source aliases or transitive dependencies.

Automatic verification:

- Package export contract checks.
- Tarball or local packed package consumption test.
- `pnpm -F @mango/admin build`.
- Generated consumer app build and typecheck.
- E2E screenshots for login, home, representative built-in pages and empty/error states.

Manual acceptance:

- User confirms login page style matches baseline.
- User confirms no new hand-written visual skin was introduced.
- User confirms consumer app renders through package imports only.

Exit criteria:

- Style entry is sufficient for default admin rendering.
- User acceptance recorded.

### Sprint 5: Built-In Capability Package Integration

Goal:

- Make built-in Mango capabilities consumable as admin-bound packages without breaking full default Mango.

Scope:

- Review and integrate packages such as system, rbac, file, workflow and notice admin packages.
- Keep API packages separate for non-admin use.
- Admin packages bind to admin-shell conventions.
- Required grouped capabilities, such as system management modules, are handled as explicit dependency groups.

Automatic verification:

- Package build/typecheck for each touched `*-api` and `*-admin` package.
- Capability registration tests.
- Real backend E2E for representative pages.
- Menu/function sampling screenshots for each first-level capability group.
- Data/API checks for list, search, create/edit where applicable.

Manual acceptance:

- User reviews sampled pages and confirms layout follows Mango conventions.
- User confirms data appears from real backend, not mock or static arrays.
- User confirms package naming and dependency grouping are understandable.

Exit criteria:

- Built-in capability package integration passes real backend E2E.
- User acceptance recorded.

### Sprint 6: create-mango-app Full Preset

Goal:

- Generated business project starts as usable full Mango Admin by depending on Mango materials.

Scope:

- Make `create-mango-app` generate a full-preset admin app that depends on `@mango/admin`.
- Generated app should not copy Mango shell internals.
- Generated app should be able to add business pages/modules without rewriting the framework.

Automatic verification:

- CLI generation test.
- Fresh install from package artifacts.
- Typecheck and build.
- Real backend E2E for generated full preset.
- Screenshot comparison against Sprint 0 baseline.

Manual acceptance:

- User runs or reviews the generated app experience URL.
- User confirms it starts as full Mango Admin.
- User confirms original menus/framework/topbar/styles are present.

Exit criteria:

- Generated full preset is accepted as full Mango baseline.
- User acceptance recorded.

### Sprint 7: Custom Preset And Business Extension

Goal:

- Allow business projects to depend on Mango and customize safely after full baseline is proven.

Scope:

- Add documented custom preset configuration.
- Add one real business sample page/module using the starter pattern.
- Ensure custom additions are additive and do not mutate full-mode Mango menus unexpectedly.

Automatic verification:

- Custom preset build/typecheck.
- E2E for full preset and custom preset in the same Sprint.
- Screenshot comparison proving custom mode differs only where configured.
- Data/API check for the sample business page.

Manual acceptance:

- User confirms full preset is still unchanged.
- User confirms custom preset changes are intentional and configurable.
- User confirms business page uses Mango shell and style correctly.

Exit criteria:

- Full and custom modes both pass.
- User acceptance recorded.

### Sprint 8: Deployment Mode Matrix

Goal:

- Verify the corrected package model works in monolith, micro frontend and mixed deployment modes without changing the baseline UI contract.

Scope:

- Reuse the existing runtime mode direction only after full/default and custom modes are stable.
- Verify local, micro and mixed modes with the same shell/menu/style contract.
- No remote registry, gray release or platform features.

Automatic verification:

- Mode matrix build/typecheck.
- Local mode E2E screenshots.
- Micro mode E2E screenshots.
- Mixed mode E2E screenshots.
- Menu/API/style consistency reports across modes.

Manual acceptance:

- User compares screenshots across modes.
- User confirms all modes keep Mango framework parity.
- User confirms no scope expansion occurred.

Exit criteria:

- Mode matrix passes without changing visual or menu contract.
- User acceptance recorded.

### Sprint 9: Release Readiness And Documentation

Goal:

- Prepare the corrected implementation for PR/release without overstating capabilities.

Scope:

- Update migration notes.
- Update package usage docs.
- Update verification index with all screenshot/report paths.
- Run final full regression.
- Create PR only after user accepts all prior Sprint evidence.

Automatic verification:

- `pnpm lint` where available.
- `pnpm build`.
- `pnpm test`.
- Full E2E regression.
- Delivery ledger verification.
- `git diff --check`.

Manual acceptance:

- User reviews final evidence index.
- User confirms no unaccepted Sprint remains.
- User approves PR creation.

Exit criteria:

- PR is created with evidence and risk notes.
- User acceptance recorded.

## 7. Required Evidence Structure

Each Sprint must save evidence under:

```text
mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-<n>/
```

Required files:

- `summary.md`
- `commands.log`
- `screenshots/*.png`
- `layout-report.json`
- `menu-report.json` when menus are involved
- `api-report.json` when APIs/data are involved
- `manual-acceptance.md`

## 8. Risk Controls

- Do not continue development on the failed worktree as the correction branch.
- Do not mark old Sprint evidence as accepted unless it is rerun under this plan.
- Do not accept a page only because it opens.
- Do not accept a generated menu as full Mango menu unless backend menu equality is proven.
- Do not accept a visual fix unless screenshot comparison passes.
- Do not move to the next Sprint until the current Sprint has user manual acceptance.

