# mango-cli

Mango project CLI.

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

## Registry Credentials

Credentials are not written into generated files. Maven credentials stay in the user's Maven `settings.xml`; npm credentials stay in user-level npm config or CI secrets.

## Scope

The CLI generates Mango consumer projects from released Maven and npm materials:

- full stack frontend and backend project generation
- monolith and microservice topology skeletons
- private Maven and npm registry configuration without credentials
- unified Mango framework versions rendered into generated files
- Mango PMO baseline documents in generated projects
- optional Mango module selection for custom business projects
- additive optional module integration through `mango add`

This package does not generate business logic code.
