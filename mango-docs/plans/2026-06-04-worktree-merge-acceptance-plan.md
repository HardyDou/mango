# Worktree 合并与 ER-013 业务 CRUD 模板修复计划

## 1. 目标

逐个核对历史 worktree 的改动是否已经进入 `main`，只保留仍然有价值且方向正确的改动，并从最新 `main` 新建干净 worktree 移植 ER-013 相关业务 CRUD 模板能力，避免继续从脏 worktree 合并混杂内容。

## 2. 范围

- 分析 `.mango/worktrees` 下历史 worktree 的合并状态、有效性和清理建议。
- 修复 `mango module add` 生成业务模块时只能生成弱页面和不完整 CRUD 契约的问题。
- 同步 `mango-ui/packages/mango-cli/templates/business-module` 与 `mango-business-starter` 的业务模块模板。
- 增强 CLI 与 starter 模板自检，覆盖真实 CRUD 页面、接口、权限、菜单和配置输出。
- 验证生成后的企业项目可完成前端类型检查、前端构建和后端业务模块编译。

## 3. 不做什么

- 不直接合并旧 dirty worktree。
- 不恢复已废弃的 `create-mango-app`。
- 不处理与 ER-013 无关的历史 evidence、截图、临时 sandbox 或单体 app 改动。
- 不切换到 npmjs 发布方案。
- 不处理浏览器端完整 E2E；本次重点是生成物料和编译构建闭环。

## 4. 设计输入

- 用户要求：逐个 worktree 分析是否改动已到 `main`，确认哪些有用/无用，说明方向、影响、合并计划和验收计划。
- 用户要求：有效改动从最新 `main` 新建干净 worktree 移植，不直接合旧 dirty worktree。
- PMO 规则：`mango-pmo/rules/00-dev-flow.md`、`mango-pmo/rules/03-ai-coding-redlines.md`、`mango-pmo/rules/01-delivery-contract.md`、`mango-pmo/rules/backend/07-persistence.md`、`mango-pmo/rules/frontend/01-vue-code.md`、`mango-pmo/rules/frontend/06-monorepo-architecture.md`。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/mango-cli/src/index.mjs`
- `mango-ui/packages/mango-cli/templates/business-module/**`
- `mango-ui/packages/mango-cli/templates/full/frontend/**`
- `mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `mango-business-starter/**`

### 5.2 接口变化

- CLI：`mango module add` 新增 `--aggregate-name` 参数，用于生成业务聚合的中文显示名。
- 业务模板后端 API：补齐创建、修改、删除、分页查询、详情查询文档与接口契约。
- 业务模板前端 API：补齐 `create`、`page`、`detail`、`update`、`delete` 调用。

### 5.3 数据变化

- 不修改 Mango 主仓数据库 migration。
- 生成后的业务模块模板继续生成真实业务表、实体、Mapper、Service、Controller 和 starter manifest。
- 生成项目的 `mango.config.json` 增加 `aggregateDisplayName`，用于记录聚合显示名。

### 5.4 菜单/页面/权限变化

- 业务模块 resource manifest 的菜单名使用 `{{aggregateName}}管理`。
- 权限补齐 `create`、`view`、`update`、`delete`。
- 生成页面补齐查询、重置、新增、编辑、详情、删除、分页和空状态表达。

### 5.5 测试范围

- 模板静态自检：`node mango-business-starter/scripts/check-template.mjs`
- CLI 自检：`node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- 真实生成项目：`mango init --preset full` 与 `mango module add procurement --aggregate order --aggregate-name 采购订单`
- 生成项目前端：`npm install`、`npm run typecheck`、`npm run build`
- 生成项目后端：编译 `procurement-api`、`procurement-core`、`procurement-starter`

## 6. Worktree 分析结论

| Worktree | 当前判断 | 是否合入 main | 建议 |
|---|---|---:|---|
| `menu-log-contract-fix` | 菜单目录与操作日志修复已通过后续 PR 合入 | 是 | 清理旧 worktree |
| `hotfix-create-mango-app-local-deps` | `create-mango-app` 已废弃并通过后续 PR 删除 | 是 | 清理旧 worktree，不再合并旧改动 |
| `issue-31-e2e-api-baseurl` | main 已有更优 API baseURL 处理 | 是 | 已清理或可清理 |
| `fix-er-013-enterprise-flow` | HEAD 已是 main 祖先，但 dirty 改动混杂；仅业务 CRUD 模板方向有效 | 部分 | 从最新 main 移植有效改动到本分支 |
| `enterprise-business-flow-verify` | 主要为旧验证记录，等价提交已合入 | 是 | 清理旧 worktree |
| `fix-admin-shell-1.0.8-regressions` | admin shell root/tag fallback 已通过后续版本覆盖 | 是 | 清理旧 worktree |
| `fix-admin-shell-directory-redirect-real-runtime` | 旧发布/构建修复已被更高版本覆盖 | 是 | 清理旧 worktree |
| `fix-admin-shell-directory-redirect-runtime` | 旧运行时修复已被更高版本覆盖 | 是 | 清理旧 worktree |
| `fix-issue-71-directory-route-redirect` | 目录路由跳转修复已合入 | 是 | 清理旧 worktree |
| `fix-issue-75-dict-reset` | 字典重置修复已合入 | 是 | 清理旧 worktree |
| `release-admin-shell-1.0.9-cli-1.0.18` | 旧版本发布分支，main 已更高版本 | 是 | 清理旧 worktree |
| `release-mango-cli-1.0.17` | 旧版本发布分支，main 已更高版本 | 是 | 清理旧 worktree |
| `npm-package-boundary` | 旧 1.0.1 npm 包版本改动，已过期 | 否 | 不合并，清理前保留结论 |
| `scope-mango-cli-package` | npmjs 发布方向已被用户暂停，当前私服方案为准 | 否 | 不合并，清理前保留结论 |
| `frontend-productization-plan` | 存在大量 dirty 改动，可能是独立产品化任务残留 | 未确认 | 暂不清理，另起专项分析 |

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| WT-001 | 用户要求 | 逐个分析历史 worktree 是否已到 main | 使用 `git worktree list`、`git cherry`、main 版本状态和改动内容归类 | 本文第 6 节 Worktree 分析结论 | 审查 worktree 列表和分支合入状态 | DONE | 本文第 6 节 |
| WT-002 | 用户要求 | 不直接合旧 dirty worktree | 从最新 main 创建 `fix/er013-business-crud-template`，只移植有效 ER-013 改动 | 独立任务分支和本次模板改动 | `git status --short`、`git diff --stat` | DONE | 本文第 5 节 |
| ER013-001 | 持久化规范 | 业务模板必须生成真实数据库 CRUD 骨架 | 保留后端 Entity、Mapper、Service、Controller、migration 模板，补齐 API 契约 | `business-module/backend/**`、`mango-business-starter/backend/**` | 后端生成项目模块 `mvn compile` | DONE | 验证命令记录 |
| ER013-002 | 用户要求 | 生成业务页面必须可做真实 CRUD 操作 | 页面补齐查询、重置、新增、编辑、详情、删除、分页 | `index.vue` 模板 | 模板自检、CLI 自检、生成项目前端 typecheck/build | DONE | 验证命令记录 |
| ER013-003 | 用户要求 | 菜单和权限要支撑真实业务模块 | manifest 菜单名使用聚合显示名，权限补齐 create/view/update/delete | `resource-manifest.json` 模板 | CLI 自检扫描生成 manifest | DONE | 验证命令记录 |
| ER013-004 | 用户要求 | CLI 支持业务显示名并写入配置 | `mango module add` 增加 `--aggregate-name`，配置记录 `aggregateDisplayName` | `src/index.mjs`、`check-cli.mjs` | CLI 自检和真实生成项目扫描 | DONE | 验证命令记录 |
| ER013-005 | 前端独立消费要求 | 企业项目消费 `@mango/common` 时类型检查可通过 | 生成项目前端提供消费侧 `mango-common.d.ts`，只声明请求 API | `templates/full/frontend/src/mango-common.d.ts`、`tsconfig.json` | 生成项目前端 `npm run typecheck`、`npm run build` | DONE | 验证命令记录 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| ER013-001 | 生成后端模块 | procurement 模块编译 | module=`procurement`，aggregate=`order`，aggregateName=`采购订单` | `procurement-api/core/starter` 编译通过 | 不涉及浏览器 UI | 不涉及 | Maven compile 输出 | DONE |
| ER013-002 | 生成前端页面 | CRUD 页面结构 | `采购订单` | 页面源码包含创建、编辑、详情、删除、分页方法和组件 | 本次未启动浏览器，属于模板/构建验证 | 前端 build warningCount=0 | npm typecheck/build 输出 | DONE |
| ER013-003 | 生成 manifest | 菜单权限 | `procurement:order:*` | manifest 包含 create/view/update/delete | 不涉及浏览器 UI | 不涉及 | CLI 自检输出 | DONE |
| ER013-004 | CLI | `--aggregate-name` | `采购订单` | `mango.config.json` 包含 `aggregateDisplayName` | 不涉及浏览器 UI | 不涉及 | CLI 自检输出 | DONE |
| ER013-005 | 生成项目前端 | 独立消费类型隔离 | `@mango/common` 请求 API | `vue-tsc` 不再检查发布包源码既有类型债务 | 不涉及浏览器 UI | npm deprecated warning 仍存在 | npm typecheck/build 输出 | DONE |

## 9. 风险与限制

- 本次没有启动浏览器做真实 UI 截图；只能证明模板生成、类型检查、构建和后端业务模块编译闭环通过。
- `npm install` 仍出现既有 deprecated warning，涉及 `inflight`、`lodash.isequal`、`glob@7`、`vue-i18n@9`、`codemirror` 等依赖栈，未在本次处理。
- `frontend-productization-plan` worktree 尚未完成独立分析，暂不清理。
- `npm-package-boundary` 和 `scope-mango-cli-package` 未合入是基于当前发布方向判断；未来若恢复 npmjs 发布，需要另起专项设计。
