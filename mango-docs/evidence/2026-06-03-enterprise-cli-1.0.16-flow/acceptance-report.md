# Enterprise CLI 1.0.16 Flow Acceptance

## Objective

Validate that enterprise teams can consume published Mango materials without developing inside the Mango source repository:

- Install scoped CLI package `@mango/cli`.
- Initialize an independent full preset enterprise project.
- Add a second business module with real backend/frontend CRUD skeleton.
- Build frontend and backend from published artifacts.

## Published Artifacts

| Package | Version | Registry |
| --- | --- | --- |
| `@mango/cli` | `1.0.16` | `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` |
| `@mango/common` | `1.0.7` | `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` |

Consumer registry:

```ini
registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
@mango:registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

## Commands

```bash
mkdir -p /tmp/mango-enterprise-cli-flow-116/cli
printf 'registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/\n@mango:registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/\n' \
  > /tmp/mango-enterprise-cli-flow-116/cli/.npmrc
cd /tmp/mango-enterprise-cli-flow-116/cli
npm install @mango/cli@1.0.16
```

```bash
cd /tmp/mango-enterprise-cli-flow-116
./cli/node_modules/.bin/mango init contract-ops-platform \
  --preset full \
  --topology monolith \
  --package com.acme.contractops \
  --group-id com.acme.contractops \
  --npm-registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ \
  --maven-repository http://nexus.inner.yunxinbaokeji.com/repository/maven-public/

./cli/node_modules/.bin/mango module add procurement-order \
  --aggregate procurement \
  --module-name 采购订单 \
  --project-dir /tmp/mango-enterprise-cli-flow-116/contract-ops-platform
```

```bash
cd /tmp/mango-enterprise-cli-flow-116/contract-ops-platform/frontend
npm install
npm run build

mvn -f /tmp/mango-enterprise-cli-flow-116/contract-ops-platform/backend/pom.xml -DskipTests package
```

## Results

| Check | Result | Evidence |
| --- | --- | --- |
| Install CLI without command-line `--registry` | PASS | `.npmrc` placed in actual npm project directory; `npm install @mango/cli@1.0.16` added 1 package |
| Initialize full preset enterprise project | PASS | `Created Mango full project: contract-ops-platform` |
| Add business module | PASS | `Added business module: procurement-order (procurement)` |
| Published dependency versions | PASS | root, business package, and API package use `@mango/common@1.0.7`; root uses `@mango/admin@1.0.10` and `@mango/admin-shell@1.0.8` |
| Placeholder/history scan | PASS | no `{{`, `保函`, `担保`, `mango-cli@`, `--package mango-cli`, or `npm install.*mango-cli` found in generated project |
| Frontend build | PASS | `vue-tsc` passed and Vite build completed |
| Backend package | PASS | Maven reactor built 7 modules successfully |

Generated business module artifacts include:

- Backend: `procurement-order-api`, `procurement-order-core`, `procurement-order-starter`, `procurement-order-starter-remote`.
- Frontend: `frontend/packages/procurement-order`, `frontend/packages/procurement-order-api`.
- Resource manifest: `backend/modules/procurement-order/procurement-order-starter/src/main/resources/META-INF/mango/resource-manifest.json`.

## Issues Found And Fixed

| Finding | Impact | Fix |
| --- | --- | --- |
| `@mango/cli@1.0.11` published package did not include business module templates | `mango module add` failed in installed CLI with `ENOENT` | packaged business module templates under `templates/business-module` |
| CLI installed outside Mango workspace used stale fallback versions | Generated project could reference old published packages | synchronized fallback versions with published package versions |
| `@mango/common/utils/message.ts` accepted `MessageParams` union unsafely | Generated enterprise frontend build failed in published package typecheck | narrowed message options to `MessageOptions` before object spread and published `@mango/common@1.0.7` |
| Business module frontend templates hardcoded `@mango/common@1.0.4` | `npm install` resolved mixed common versions and failed peer dependency resolution | switched module templates to `{{mangoCommonVersion}}` and `{{mangoAdminPagesVersion}}` |
| `.npmrc` in parent directory is not reliably read with `npm --prefix <child>` | Could silently fall back to public npmjs | docs now state `.npmrc` must be in user config or the actual npm project directory |

## Non-Blocking Risks

- `npm install` reports deprecated transitive dependencies including `vue-i18n@9.2.2` and `codemirror@6.65.7`.
- Vite reports a large chunk warning for the full preset build.
- Follow-up issue: https://github.com/HardyDou/mango/issues/64
- Browser/runtime verification was not run in this report; this report covers initialization and build/package acceptance from published artifacts.
