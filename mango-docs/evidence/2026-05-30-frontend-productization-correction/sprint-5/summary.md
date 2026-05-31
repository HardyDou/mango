# Sprint 5 Summary

## Status

`DONE`

Sprint 5 proved that Mango frontend materials can be consumed from Nexus by an independent business app without workspace source paths.

## Completed

- Reframed Sprint 5 as npm/Nexus independent consumption verification.
- Moved `mango-cli` implementation to Sprint 6/7, after package consumption is proven.
- Recorded `mango-cli` responsibilities and credential rules.
- Audited current frontend package metadata for entry, types, style, peer dependencies and workspace dependencies.
- Verified a clean consumer can install packed package tarballs, run typecheck and build without workspace links.
- Published Mango frontend package closure to Nexus as `1.0.2`.
- Found a real independent-consumption gap: `@mango/admin` exposed an outdated hand-written type contract that did not include `devCenter.deployEnv`.
- Fixed the `@mango/admin` public type contract and published `@mango/admin@1.0.3`.
- Verified a clean consumer can install `@mango/admin@1.0.3` from Nexus group registry, run typecheck and build.
- Verified a Nexus-installed consumer can run against the real backend, login, load menus and render Mango shell/pages with screenshots.

## Current Result

- `@mango/admin@1.0.3` is available from `http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Clean Nexus consumer result: `install`, `typecheck` and `build` passed.
- Runtime E2E result: login, shell, home, development center, system management, workflow, platform capability and notice pages passed.
- Real backend checks passed: health 200, login 200, menu API 200, backend returned 4 business top-level menus and 218 menu rows.
- Screenshots are stored under `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/screenshots/`.

## Evidence

- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/packed-consumer-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/nexus-consumer-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/nexus-runtime-e2e-report.json`

## Next

1. Sprint 6 starts `mango-cli init` full preset on top of the verified Nexus npm package path.
2. Keep credential storage outside the repository: user-level npm config or CI Secret only.
