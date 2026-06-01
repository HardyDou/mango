# Sprint 3 Manual Acceptance

Status: `ACCEPTED`

## Acceptance Scope

User must review Sprint 3 evidence and confirm:

- Full/default mode visible backend menus match the real backend menu API for the same user.
- `开发中心` follows the agreed rule: configurable, visible by default in dev/test, hidden in prod.
- Shell-only menus are explicit and do not silently replace backend menus.
- Fallback menu usage is detectable and not accepted as full-mode verification.
- Sampled pages render inside the original Mango shell without `404`.

## Evidence To Review

- `delivery-ledger.md`
- `menu-contract-report.json`
- `dev-center-visibility-report.json`
- `menu-sampling-report.json`
- `dev-center-pages-report.json`
- `screenshots/sprint-3-layout-contact-sheet.png`
- `screenshots/*.png`

## Automated Verification Summary

- Backend health and login used real local backend `http://127.0.0.1:18800`.
- Menu contract E2E passed: backend top menus are `系统管理`, `审批中心`, `平台能力`, `通知中心`; shell-only menus are limited to `首页` and configurable `开发中心`.
- Menu sampling E2E passed: each backend first-level menu sampled 3 child pages, with screenshots saved.
- Development center E2E passed for `文件上传`, `组织架构选择器`, `省市区选择器`, `AI 对话`, `实时通信`, `验证码`.
- Runtime composition E2E passed for hybrid, monolith, broken remote, missing remote, invalid runtime mode and unauthorized event scenarios.
- Screenshot review result: sampled pages keep the original Mango blue topbar, left menu, tags view, notice bell, settings and user area.

## Known Non-blocking Observation

- E2E reports include an SSE probe `net::ERR_ABORTED` during realtime probing. It did not produce page errors, console errors, failed responses, missing shell chrome or visible route failures in the sampled pages.

## User Decision

- [x] ACCEPTED
- [ ] REJECTED

User note:

```text
2026-05-30 user confirmed: ok了；继续任务吧；
```
