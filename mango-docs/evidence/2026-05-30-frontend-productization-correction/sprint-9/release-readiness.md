# Sprint 9 Release Readiness

## 1. Release Position

This PR prepares Mango frontend productization correction for review and release readiness. It does not add new runtime capabilities in Sprint 9.

The corrected usage model is:

- Business teams use `mango-cli` to create projects from published Maven and npm packages.
- `full` preset uses all Mango admin capabilities.
- `custom` preset keeps authorization/system and adds only selected optional modules.
- Optional capabilities are integrated through npm package dependency, feature flag, page registrar, package-owned style, backend starter, and runtime config when needed.

## 2. Current Published Package Baseline

| Package | Version |
| --- | --- |
| `mango-cli` | `1.0.7` |
| `@mango/admin` | `1.0.7` |
| `@mango/admin-shell` | `1.0.4` |
| `@mango/admin-pages` | `1.0.3` |
| `@mango/workflow` | `1.0.4` |
| `@mango/workflow-business-example` | `1.0.4` |
| `@mango/file` | `1.0.4` |
| `@mango/template` | `1.0.4` |
| `@mango/notice` | `1.0.4` |
| `@mango/numgen` | `1.0.4` |
| `@mango/calendar` | `1.0.4` |

## 3. What Is Ready

- `mango-cli` can generate full and custom projects.
- `mango add` can add optional modules to CLI-generated custom projects without rewriting business-owned files.
- `@mango/admin/style.css` and `@mango/admin/style-full.css` define the style consumption contract.
- Optional package visibility is controlled by `features` and `featureRegistrars`.
- Workflow example forms are separated into `@mango/workflow-business-example`.
- Deployment mode matrix has screenshot evidence for monolith, hybrid micro-frontend, and mixed modes.

## 4. What Is Not Claimed

- This PR does not claim production gray release, remote manifest governance, or rollback orchestration.
- This PR does not replace business-specific backend design, database migration, or permission initialization work.
- Sprint 9 does not publish a new package version by itself; it documents and verifies release readiness for the current PR state.

## 5. Acceptance Checklist

- Review `mango-docs/designs/mango-capability-usage-guide-for-ai.md`.
- Review `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-9/final-evidence-index.md`.
- Open Sprint 8 screenshots and confirm monolith/hybrid/mixed pages keep Mango shell parity.
- Confirm PR #39 body includes Sprint 8 and Sprint 9 notes.
- Confirm no broad staging of old untracked Sprint 4 regression evidence.

## 6. Residual Risks

- Runtime E2E depends on local services and backend data being available.
- Full regression is representative and evidence-based; it does not exhaustively click every menu item.
- Published npm/Maven package versions must remain aligned when the final release version is cut.
