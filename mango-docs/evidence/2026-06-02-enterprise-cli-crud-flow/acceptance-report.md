# Enterprise CLI CRUD Flow Acceptance

## Scope

- Generated an enterprise full project with `mango-cli`.
- Added business module `contract` and aggregate `seal`.
- Verified real backend migration, CRUD API, frontend page, and browser interactions.

## Commands

```bash
node mango-ui/packages/mango-cli/src/index.mjs init enterprise-crud-fixed --preset full --topology monolith --package com.example.enterprise --group-id com.example.enterprise --force --npm-registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ --maven-repository http://nexus.inner.yunxinbaokeji.com/repository/maven-public/
node mango-ui/packages/mango-cli/src/index.mjs module add contract --aggregate seal --module-name 合同管理 --project-dir /tmp/mango-enterprise-crud-verify/enterprise-crud-fixed
mvn -f backend/pom.xml -q -DskipTests package
mvn -f backend/pom.xml -q -DskipTests install
VITE_ADMIN_PROXY_PATH=http://127.0.0.1:5565 npm run dev -- --port 5179
```

## Backend Results

- `contract` Flyway migration executed successfully.
- `contract_seal` table was created with `id`, `name`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`.
- Login with `admin/admin123` and `tenantId=1` succeeded.
- `POST /contract/seals/create` created `验收合同章`.
- `GET /contract/seals/page` returned the created record.
- `GET /contract/seals/detail` returned the created record detail.

## Browser Results

- Login page accepted `admin/admin123`.
- Home page loaded top-level menu including `合同管理`.
- `/contract/seals` rendered `Seal管理` without 404.
- Initial table displayed backend-created `验收合同章`.
- Clicking `新增` with `浏览器新增章` inserted a real DB record.
- Clicking `查询` filtered and displayed `浏览器新增章`.
- Latest console after authenticated business page validation had 0 errors and 0 warnings.

## Screenshots

- `after-login.png`: authenticated home page.
- `contract-seals-page.png`: business page with existing record.
- `contract-seals-after-create.png`: business page after real create.
- `contract-seals-after-search.png`: business page after search.

## Findings

- Starting frontend without `VITE_ADMIN_PROXY_PATH` pointed the generated project proxy to default backend port `5555`; the verification backend used `5565`, causing `/api/authorization/menus/user` proxy 500. The generated README now explicitly says the proxy port must match the running backend port.
- Unauthenticated first load triggers a menu request and logs a 401 console error before redirecting to login. Authenticated validation has no console errors.
- Full `npm run build` fails with published `@mango/common@1.0.3` TypeScript errors in `utils/message.ts` and `utils/request.ts`. The source package fix in this branch passes `pnpm -F @mango/common build`; a new frontend package release is required before generated enterprise projects can pass full typecheck against published packages.
- Backend startup still reports existing schema validation warnings for Flowable/platform tables. The generated `contract_seal` table is not in those warnings.
