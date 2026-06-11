# @mango/cli

Mango project CLI.

## Install

The CLI package is published as `@mango/cli` to avoid collisions with the public `mango-cli` package name. The installed command names remain `mango` and `mango-cli`.

```bash
npm install -g @mango/cli --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
mango init mango-admin-demo --preset full --topology monolith
```

## Upgrade

Check the latest published CLI version:

```bash
npm view @mango/cli version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

Upgrade the global CLI:

```bash
npm install -g @mango/cli@latest --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

Read version changes in [CHANGELOG.md](./CHANGELOG.md) before upgrading a business project.

After upgrading, show the same release notes directly from the installed CLI:

```bash
mango changelog
```

Existing generated projects are not modified automatically by upgrading the global CLI. For generated project template changes, either regenerate the project with the new CLI or copy the affected generated files from a clean project created by the target CLI version.

For `1.0.27`, existing generated projects should specifically review:

- `scripts/dev-workspace.sh`
- `scripts/backend-dev.sh`
- `README.md`
- `AGENTS.md`
- `frontend/package.json`

## Init

```bash
mango init mango-admin-demo --preset full --topology monolith
```

`full` preset generates a standalone Mango Admin consumer project. The generated frontend consumes published Mango npm packages through `@mango/admin/full` and `@mango/admin/style-full.css`; the generated backend consumes the Maven `mango-admin-starter`.

Use `custom` when the business project only needs selected optional modules:

```bash
mango init claim-admin --preset custom --modules workflow,template --topology monolith
```

`custom` always keeps the required authorization/system group and only adds the optional modules listed in `--modules`. `workflow-example` automatically adds `workflow`.

Supported optional modules:

- `file`: 文件中心
- `template`: 模板管理
- `notice`: 通知中心
- `numgen`: 编号规则
- `calendar`: 工作日历
- `workflow`: 审批中心
- `workflow-example`: 审批示例

`--modules all` selects every optional module. `--modules none` creates only the required core group.

## Add

Add optional modules to an existing generated project:

```bash
mango add notice --project-dir ./claim-admin
```

`add` updates CLI-managed integration files only:

- `mango.config.json`
- `frontend/package.json`
- `frontend/src/main.ts`
- `frontend/public/runtime-config*.json`
- `backend/pom.xml`

Business-owned files are not rewritten by `add`.

## PMO Baseline Sync

Sync or upgrade the Mango PMO baseline in an existing business project:

```bash
mango pmo sync --project-dir ./claim-admin
```

`pmo sync` creates or updates:

- `business-pmo/mango-baseline/**`
- `business-pmo/README.md`
- missing `business-docs/plans/` examples

It writes the Mango commit, CLI version, and sync time into the baseline README. It does not overwrite business-owned `business-pmo/rules/**` files, and it leaves existing business docs in place.

Sync generated startup shell scripts at the same time:

```bash
mango pmo sync --project-dir ./claim-admin --sync-shell
```

`--sync-shell` updates:

- `scripts/dev-workspace.sh`
- `scripts/backend-dev.sh`

Use `--dry-run` first to review the files that will change.

Preview changes without writing files:

```bash
mango pmo sync --project-dir ./claim-admin --dry-run
```

When a root `AGENTS.md` points to an external `/Users/.../mango-pmo`, migrate it to the project-local baseline entry with:

```bash
mango pmo sync --project-dir ./claim-admin --write-agents
```

## Business Module

Generate an enterprise-owned business module in an existing generated project:

```bash
mango module add contract --aggregate seal --module-name 合同管理 --project-dir ./claim-admin
```

`module add` renders the business starter backend module and frontend packages, then updates:

- `backend/pom.xml`
- `backend/app/pom.xml`
- `backend/app/src/main/resources/application.yml`
- `frontend/package.json`
- `frontend/src/main.ts`
- `mango.config.json`

The generated backend module uses Mango persistence (`mango-infra-persistence-starter`) and MyBatis-Plus through the framework starter. It enables the business module Flyway migration in the generated app configuration, creates a real table, and is not a fixed-response demo.

## Registry Credentials

Credentials are not written into generated files. Maven credentials stay in the user's Maven `settings.xml`; npm credentials stay in user-level npm config or CI secrets.

## Scope

The CLI generates Mango consumer projects from released Maven and npm materials:

- full stack frontend and backend project generation
- monolith and microservice topology skeletons
- private Maven and npm registry configuration without credentials
- unified Mango framework versions rendered into generated files
- Mango PMO baseline documents in generated projects
- Mango PMO baseline sync and upgrade for existing business projects
- optional Mango module selection for custom business projects
- additive optional module integration through `mango add`
- enterprise-owned business module generation through `mango module add`
