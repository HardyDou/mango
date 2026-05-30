# Sprint 1 Summary

Status: `PENDING_USER_ACCEPTANCE`

## Completed

- Compared failed branch `worktree/frontend-productization-plan` against `main`.
- Generated diff evidence and category summary.
- Confirmed failed branch changed 570 files and mixed too many scopes.
- Confirmed current admin shell has duplicated layout implementations between app and package.
- Identified `开发中心` menu drift source in `packages/admin-shell/src/runtime/menuHost.ts`.
- Confirmed style/package facade is still incomplete in the correction baseline.
- Produced KEEP/REWORK/DROP salvage matrix.
- Produced Sprint 2 entry rules.

## Evidence Files

- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/diff-name-status.txt`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/diff-stat.txt`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/diff-category-summary.json`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/current-shell-layout-diff.txt`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/menu-source-report.txt`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/style-export-report.txt`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/salvage-audit.md`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/salvage-matrix.md`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-1/manual-acceptance.md`

## Verification

Sprint 1 is audit-only, so no product runtime E2E was rerun. Sprint 0 remains the runtime baseline.

Commands:

```bash
node mango-pmo/tools/pmo-preflight.mjs --role tech-lead --phase design --task "Sprint 1 salvage audit for failed frontend productization branch: compare against main, classify changes KEEP REWORK DROP, produce reimplementation map without product code changes" --paths "mango-docs/evidence,mango-docs/plans,mango-ui,create-mango-app,package.json,pnpm-workspace.yaml"
git status --short --branch
git log --oneline -5
git diff --name-status main...worktree/frontend-productization-plan
git diff --stat main...worktree/frontend-productization-plan
diff -qr mango-ui/apps/mango-admin/src/layout mango-ui/packages/admin-shell/src/layout
rg -n "开发中心|createDevRouteMenus|capabilityMenus|businessMenus|fallbackMenus|mergeShellMenus|/authorization/menus/user|menuLoader|config/menu" mango-ui/packages/admin-shell/src mango-ui/apps/mango-admin/src
rg -n "style\\.css|index\\.css|index\\.scss|theme/index.scss|exports|createMangoAdmin|createMangoAdminApp|ShellView" mango-ui/packages/admin-shell mango-ui/apps/mango-admin mango-ui/packages/*/package.json
```

## Sprint 2 Gate

Sprint 2 may start only after manual acceptance of this Sprint 1 report.

Sprint 2 target:

- single-source admin shell extraction.
- no copied or approximate shell.
- no menu drift masking.
- real E2E screenshots and visual inspection before acceptance.
