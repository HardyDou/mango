# Sprint 9 Final Evidence Index

## 1. Current PR

- PR: https://github.com/HardyDou/mango/pull/39
- Branch: `worktree/frontend-productization-correction-sprint-0`

## 2. Key Usage Documents

| Document | Purpose |
| --- | --- |
| `mango-docs/designs/business-project-development-guide.md` | Business project startup and collaboration guide |
| `mango-docs/designs/mango-capability-usage-guide-for-ai.md` | Component and capability usage guide for humans and AI |
| `mango-ui/packages/mango-cli/README.md` | CLI command usage |

## 3. Sprint Evidence

| Sprint | Main Result | Evidence |
| --- | --- | --- |
| Sprint 5 | Published package consumption and feature selection verified | `sprint-5/delivery-ledger.md`, `sprint-5/nexus-consumer-report.json`, `sprint-5/nexus-runtime-e2e-report.json`, `sprint-5/feature-selection-report.json`, `sprint-5/screenshots/` |
| Sprint 6 | `mango-cli` full preset and Nexus-generated project verified | `sprint-6/delivery-ledger.md`, `sprint-6/api-report.json`, `sprint-6/layout-report.json`, `sprint-6/screenshots/`, `sprint-6/generated-full-app/` |
| Sprint 7 | Optional module selection and `mango add` verified | `sprint-7/delivery-ledger.md`, `sprint-7/summary.md` |
| Sprint 8 | Monolith, hybrid micro-frontend, and mixed deployment matrix verified | `sprint-8/delivery-ledger.md`, `sprint-8/reports/mode-matrix-report.json`, `sprint-8/screenshots/` |
| Sprint 9 | Release documentation and final evidence index | `sprint-9/final-evidence-index.md`, `sprint-9/release-readiness.md`, `sprint-9/delivery-ledger.md` |

## 4. Runtime Acceptance URLs

| Service | URL |
| --- | --- |
| Shell | `http://a.mango.io:5176` |
| RBAC micro app | `http://b.mango.io:5181` |
| Workflow micro app | `http://c.mango.io:5182` |
| Template micro app | `http://d.mango.io:5183` |
| Backend health | `http://127.0.0.1:5555/actuator/health` |

Representative pages:

- `http://a.mango.io:5176/#/system/menu-package`
- `http://a.mango.io:5176/#/workflow/start-process`
- `http://a.mango.io:5176/#/template/categories`

## 5. Final Verification Commands

```bash
pnpm --dir mango-ui -F mango-cli test
node mango-ui/packages/mango-cli/scripts/check-cli.mjs
node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-8/verify-mode-matrix.mjs
git diff --check
```

Result: all commands passed during Sprint 9 closeout. The mode matrix report returned `pass: true`.

## 6. Known Local Worktree State

The local worktree still contains untracked Sprint 4 regression evidence under:

```text
mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-4/regression-2026-05-31/
```

Those files are not part of Sprint 9 release readiness and must not be staged by broad `git add .`.
