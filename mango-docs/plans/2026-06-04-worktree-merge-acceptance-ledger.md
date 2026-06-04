# Worktree 合并与 ER-013 业务 CRUD 模板修复交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| WT-001 | 用户要求 | 逐个分析历史 worktree 是否已到 main | 使用 `git worktree list`、`git cherry`、main 版本状态和改动内容归类 | `2026-06-04-worktree-merge-acceptance-plan.md` 第 6 节 | 审查 worktree 列表和分支合入状态 | DONE | `2026-06-04-worktree-merge-acceptance-plan.md` |
| WT-002 | 用户要求 | 不直接合旧 dirty worktree | 从最新 main 创建 `fix/er013-business-crud-template`，只移植有效 ER-013 改动 | 独立任务分支和本次模板改动 | `git status --short`、`git diff --stat` | DONE | 本地 git 输出 |
| ER013-001 | 持久化规范 | 业务模板必须生成真实数据库 CRUD 骨架 | 保留后端 Entity、Mapper、Service、Controller、migration 模板，补齐 API 契约 | `business-module/backend/**`、`mango-business-starter/backend/**` | 后端生成项目模块 `mvn compile` | DONE | 本地 Maven 输出 |
| ER013-002 | 用户要求 | 生成业务页面必须可做真实 CRUD 操作 | 页面补齐查询、重置、新增、编辑、详情、删除、分页 | `index.vue` 模板 | 模板自检、CLI 自检、生成项目前端 typecheck/build | DONE | 本地 npm 输出 |
| ER013-003 | 用户要求 | 菜单和权限要支撑真实业务模块 | manifest 菜单名使用聚合显示名，权限补齐 create/view/update/delete | `resource-manifest.json` 模板 | CLI 自检扫描生成 manifest | DONE | 本地 CLI 自检输出 |
| ER013-004 | 用户要求 | CLI 支持业务显示名并写入配置 | `mango module add` 增加 `--aggregate-name`，配置记录 `aggregateDisplayName` | `src/index.mjs`、`check-cli.mjs` | CLI 自检和真实生成项目扫描 | DONE | 本地 CLI 自检输出 |
| ER013-005 | 前端独立消费要求 | 企业项目消费 `@mango/common` 时类型检查可通过 | 生成项目前端提供消费侧 `mango-common.d.ts`，只声明请求 API | `templates/full/frontend/src/mango-common.d.ts`、`tsconfig.json` | 生成项目前端 `npm run typecheck`、`npm run build` | DONE | 本地 npm 输出 |
