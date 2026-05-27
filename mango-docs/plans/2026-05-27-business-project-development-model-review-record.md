# Mango 业务项目开发模式评审记录

## 1. 评审类型

AI 预评审结果，待人工评审会确认。

本记录不表示已经完成真实会议评审；用于在正式评审前给出可执行的初步结论、风险项和后续 Sprint 建议。

## 2. 评审输入

- `mango-docs/plans/2026-05-27-business-project-development-model-review.md`
- `mango-docs/plans/2026-05-27-business-project-development-model-review-ledger.md`
- `mango-pmo/rules/00-dev-flow.md`
- `mango-pmo/rules/01-delivery-contract.md`
- `mango-pmo/rules/02-dev-environment.md`
- `mango-pmo/rules/backend/03-api.md`
- `mango-pmo/rules/backend/04-db.md`
- `mango-pmo/rules/backend/05-module.md`
- `mango-pmo/rules/backend/08-test.md`
- `mango-pmo/rules/frontend/01-vue-code.md`
- `mango-pmo/rules/frontend/05-dev-flow.md`
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`

## 3. 总体结论

预评审建议通过本方案作为 Mango 业务项目开发模式的基础方向。

通过条件：

- 先做发布物可用性检查，再承诺业务项目完全依赖 jar/npm。
- 先抽取 Mango ecosystem baseline，再创建业务模板。
- 业务项目默认 product monorepo，但必须配套 CODEOWNERS、任务 worktree、契约变更和交付台账。
- 允许前端使用 AI 修改简单后端代码，但后端 owner review 和验收不可省略。

## 4. 议题结论

| 议题 | 预评审结论 | 处理意见 |
|---|---|---|
| 业务项目是否默认 product monorepo | 通过 | 默认采用单仓；分仓仅作为生命周期、权限、合规或组织边界明确时的例外 |
| Mango 框架是否以 Maven BOM 和 npm 包对外提供 | 条件通过 | 下一 Sprint 必须先检查 Maven/npm 发布物完整性、版本锁定和私服配置 |
| Mango PMO 是否新增 ecosystem baseline | 通过 | 从现有 PMO 中抽取业务必须继承的最小基线，避免业务项目复制规范 |
| 业务 PMO 是否只补充领域规则 | 通过 | 业务 PMO 不能放宽 Mango 模块、API、DB、测试、交付规则 |
| `mango-business-starter` 是否作为第一落地资产 | 条件通过 | 在发布物检查和 ecosystem baseline 完成后启动模板仓库 |
| `mango init` 是否作为第二落地资产 | 通过 | 先支持 init，再扩展 add module/page/api/contract |
| 前端接口需求是否必须走契约变更 | 通过 | 契约状态到 `APPROVED` 后前端可继续开发，不等待真实接口完成 |
| FE+AI 修改后端是否允许 | 条件通过 | 只允许简单后端改动；复杂 DB、权限、流程、事务、性能变更必须后端 owner 实现或结对 |
| CODEOWNERS 和 worktree 是否作为默认协作机制 | 通过 | 单仓下必须使用目录 ownership、PR review 和任务 worktree |

## 5. 关键决策

### DEC-001 框架分仓发布，业务单仓交付

Mango 框架继续按框架资产管理，并通过 jar/npm/PMO baseline 对外提供能力。业务项目默认使用单仓管理前后端、业务 PMO、业务文档和部署脚本。

### DEC-002 业务项目不得复制 Mango 源码

业务项目只能通过 Maven BOM、starter jar、`@mango/*` npm 包和 PMO baseline 使用 Mango 能力。

### DEC-003 Mango PMO 需要抽取生态基线

Mango PMO 需要新增 `rules/ecosystem/*`，作为所有 Mango 业务项目必须继承的规范基线。

### DEC-004 接口协作以契约为同步源

前端需要后端调整时，必须发起接口契约变更。接口契约和交付台账是多人协作的同步源，聊天记录和临时 mock 不能作为最终依据。

### DEC-005 AI 可以实现代码，owner 不能让渡

AI 可以参与前端和后端实现。代码 owner、review owner 和验收责任仍归属对应团队或模块 owner。

## 6. 必须补充验证

| ID | 验证项 | 原因 | 建议责任角色 |
|---|---|---|---|
| V-001 | Mango Maven BOM 和 starter 是否可被外部业务项目直接依赖 | 当前尚未验证业务项目脱离源码后的后端构建能力 | Backend Owner |
| V-002 | `@mango/*` npm 包是否可被外部业务前端直接依赖 | 当前尚未验证包导出、peer dependencies、构建产物和私服发布 | Frontend Owner |
| V-003 | PMO ecosystem baseline 规则范围 | 当前只定义方向，尚未抽取正式规则文件 | PMO |
| V-004 | product monorepo 模板能否 clone 后启动 | 当前还没有模板仓库验证 | Tech Lead / DevOps |
| V-005 | 契约变更流程能否支撑一次真实前后端联调 | 当前还没有业务项目试运行数据 | QA / Tech Lead |

## 7. 需人工评审确认的问题

1. 业务项目是否全部默认 product monorepo，还是保留组织级默认分仓选项。
2. `mango-business-starter` 是否单独建仓，还是先放在 Mango 仓库内孵化。
3. `mango init` 的实现归属：放 `mango-tools`，还是单独 npm package。
4. 私服发布策略：Maven 和 npm 是否使用同一版本号和发布节奏。
5. 业务 PMO 的规则同步方式：引用固定 Mango PMO 版本，还是跟随 Mango 版本升级。

## 8. 后续 Sprint

| Sprint | 目标 | 主要交付物 | 前置条件 |
|---|---|---|---|
| Sprint 1 | Mango 对外依赖发布能力检查 | Maven BOM 检查、starter 依赖检查、`@mango/*` 包检查、私服说明 | 本预评审通过 |
| Sprint 2 | Mango ecosystem baseline | `mango-pmo/rules/ecosystem/*`、preflight 路由、业务 AGENTS 模板 | Sprint 1 发现的发布边界已明确 |
| Sprint 3 | 业务模板仓库 | `mango-business-starter`、示例业务域、本地启动脚本、业务 PMO 初始结构 | Sprint 2 完成 |
| Sprint 4 | `mango init` | `npm create mango-app@latest`、变量替换、能力选择、环境检查 | Sprint 3 完成 |
| Sprint 5 | 契约与模块生成器 | `mango add module/page/api`、`mango contract add/change/check` | Sprint 4 完成 |

## 9. 评审状态

当前状态：`AI_PRE_REVIEW_DONE`。

进入正式落地前必须补充人工评审记录，将状态更新为：

```text
HUMAN_REVIEW_APPROVED
```

或：

```text
HUMAN_REVIEW_CHANGES_REQUESTED
```

