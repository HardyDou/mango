# Sprint 9 Delivery Ledger

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S9-001 | correction plan Sprint 9 | Update package usage docs | Add ForAI capability usage guide and update business project guide to use `mango-cli` as current entry | `mango-docs/designs/mango-capability-usage-guide-for-ai.md`; `mango-docs/designs/business-project-development-guide.md` | Document review | DONE | Rules remain in `mango-pmo`; guide stays in `mango-docs` |
| S9-002 | correction plan Sprint 9 | Update verification index | Add final evidence index linking Sprint 5-9 reports and screenshots | `final-evidence-index.md` | Path review | DONE | Includes runtime URLs and verification commands |
| S9-003 | correction plan Sprint 9 | Release readiness notes | Add final release readiness, claimed scope, non-claimed scope and residual risks | `release-readiness.md` | Document review | DONE | Avoids overstating production release capabilities |
| S9-004 | correction plan Sprint 9 | Final regression | Record passed CLI checks, mode matrix and diff check | `summary.md`; `sprint-8/reports/mode-matrix-report.json` | `pnpm --dir mango-ui -F mango-cli test`; `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`; `node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-8/verify-mode-matrix.mjs`; `git diff --check` | DONE | Sprint 8 report was rerun for final regression; pure timestamp-only report change is not part of Sprint 9 scope |
