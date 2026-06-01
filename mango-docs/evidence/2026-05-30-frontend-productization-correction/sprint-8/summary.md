# Sprint 8 Summary: Deployment Mode Matrix

## Scope

Sprint 8 verifies that the corrected frontend package model works in three deployment modes:

- Monolith: authorization, workflow, and template pages render locally in the shell.
- Hybrid: authorization, workflow, and template pages render through wujie micro apps.
- Mixed: authorization and template render through micro apps while workflow renders locally.

No business source code was changed in this sprint. The main deliverable is repeatable verification evidence for the deployment matrix.

## Result

The mode matrix passed.

- `monolith`: 3/3 pages passed.
- `hybrid`: 3/3 pages passed.
- `mixed`: 3/3 pages passed.
- Runtime markers matched expected module/runtime/page type for every sampled page.
- Shell topbar, sidebar, active menu, page content, buttons/tables, screenshots, console errors, page errors, and failed responses were checked.

Evidence:

- `reports/mode-matrix-report.json`
- `screenshots/monolith-rbac-menu-package.png`
- `screenshots/monolith-workflow-start.png`
- `screenshots/monolith-template-categories.png`
- `screenshots/hybrid-rbac-menu-package.png`
- `screenshots/hybrid-workflow-start.png`
- `screenshots/hybrid-template-categories.png`
- `screenshots/mixed-rbac-menu-package.png`
- `screenshots/mixed-workflow-start.png`
- `screenshots/mixed-template-categories.png`

## Verification Commands

```bash
pnpm --dir mango-ui -F mango-admin-shell build
pnpm --dir mango-ui build:micro
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://a.mango.io:5176 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:5555 pnpm --dir mango-ui -F mango-admin-shell test:e2e
node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-8/verify-mode-matrix.mjs
```

## Notes

The first matrix script attempt reported false failures for hybrid and mixed modes because it only read the shell document text. The screenshots already showed business content, and shell metrics showed wujie iframes with buttons/tables. The final script reads both shell content and wujie child document/shadow content, so the acceptance check now matches how the page is actually rendered.

The Browser plugin was unavailable in this environment, so verification used direct Playwright from `mango-admin-shell` dependencies.

## Risks

- Current verification depends on local services already running at `a.mango.io:5176`, `b.mango.io:5181`, `c.mango.io:5182`, `d.mango.io:5183`, and backend `127.0.0.1:5555`.
- The matrix is representative, not exhaustive: it samples authorization package management, workflow start process, and template categories.
- Existing untracked Sprint 4 regression evidence remains intentionally untouched and is not part of this sprint.
