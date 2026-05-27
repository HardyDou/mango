# Mango 业务项目开发模式评审方案

## 1. 目标

明确 Mango 框架基本可用后，业务项目如何基于 Mango 开发、如何启动新项目、如何组织前后端协作、如何继承 PMO 规范，并形成可评审、可拆解、可落地的最终方案。

## 2. 范围

- 定义 Mango 框架与业务项目的代码、包、规范边界。
- 定义业务项目推荐仓库模式。
- 定义业务项目模板和初始化器能力。
- 定义 Mango PMO 与业务 PMO 的继承关系。
- 定义前端提出后端接口调整时的契约变更流程。
- 定义 AI 参与实现时的 owner、review、验收责任。
- 定义落地阶段、评审议题和评审结论记录方式。

## 3. 不做什么

- 不在本方案中实现模板仓库、CLI、Maven BOM 或 npm 发布。
- 不在本方案中新增或修改 Mango PMO 长期规范。
- 不在本方案中新增保函业务代码。
- 不替代后续正式 Sprint 计划和交付台账。

## 4. 设计输入

- 用户要求：业务开发要和框架分离，通过 jar/npm 包依赖框架资源；业务开发也要有类似 PMO 的规范；新项目最好有模板和类似 Vue init 的初始化方式；需要明确前后端单仓/分仓、接口协作、多人同步和避免阻塞机制。
- PMO 输入：`mango-pmo/rules/00-dev-flow.md`、`mango-pmo/rules/01-delivery-contract.md`、`mango-pmo/rules/02-dev-environment.md`。
- 后端规范输入：`mango-pmo/rules/backend/03-api.md`、`mango-pmo/rules/backend/04-db.md`、`mango-pmo/rules/backend/05-module.md`、`mango-pmo/rules/backend/08-test.md`、`mango-pmo/rules/backend/09-versioning.md`、`mango-pmo/rules/backend/10-dev-flow.md`。
- 前端规范输入：`mango-pmo/rules/frontend/01-vue-code.md`、`mango-pmo/rules/frontend/04-test.md`、`mango-pmo/rules/frontend/05-dev-flow.md`、`mango-pmo/rules/frontend/06-monorepo-architecture.md`。

## 5. 总体决策

Mango 采用“框架分仓发布、业务单仓交付”的模式。

```text
Mango Framework
├── 后端框架 jar / BOM
├── 前端 @mango/* npm packages
├── Mango PMO ecosystem baseline
├── 业务项目模板
└── mango init / add / doctor CLI

Business Project
├── backend
├── frontend
├── business-pmo
├── business-docs
├── deploy
└── scripts
```

框架与业务通过包依赖和规范基线分离。业务项目默认单仓管理前后端，使用目录 ownership、CODEOWNERS、任务 worktree、接口契约和交付台账管理多人协作。

## 6. 框架与业务边界

### 6.1 Mango 框架职责

Mango 框架只提供通用平台能力、基础设施能力、研发规范和项目脚手架。

后端以 Maven 形式发布：

```text
io.mango:mango-dependencies
io.mango.common:mango-common
io.mango.infra.*:mango-infra-*
io.mango.platform.*:mango-platform-*-api
io.mango.platform.*:mango-platform-*-starter
io.mango.platform.*:mango-platform-*-starter-remote
```

前端以 npm 形式发布：

```text
@mango/app-runtime
@mango/common
@mango/api-schema
@mango/auth
@mango/rbac
@mango/system
@mango/workflow
@mango/file
@mango/template
@mango/numgen
```

### 6.2 业务项目职责

业务项目只维护当前业务域能力，例如保函、合同、授信、支付业务扩展等。

业务项目不得复制 Mango 源码。业务项目通过 Maven BOM、starter jar、npm 包和 PMO baseline 使用 Mango 能力。

## 7. 业务项目仓库模式

业务项目默认采用 product monorepo。

```text
guarantee-platform/
├── AGENTS.md
├── mango.config.json
├── business-pmo/
├── business-docs/
│   ├── contracts/
│   ├── plans/
│   └── ledgers/
├── backend/
├── frontend/
├── deploy/
└── scripts/
```

默认不拆成前端仓和后端仓。原因是业务能力交付通常同时涉及接口、数据、权限、菜单、页面和测试，单仓更适合 AI 基于完整上下文完成闭环交付。

分仓仅作为例外，用于前端或后端生命周期、权限隔离、合规审批、组织边界明显不同的场景。

## 8. 人员职责与 ownership

人员职责通过目录 ownership 和 PR review 控制，不通过仓库隔离控制。

```text
/backend/        backend team
/frontend/       frontend team
/business-pmo/   tech lead / pmo
/business-docs/  pm / tech lead / qa
/deploy/         devops
/scripts/        platform team
```

推荐 `CODEOWNERS`：

```text
/backend/        @backend-team
/frontend/       @frontend-team
/business-pmo/   @tech-lead @pmo
/business-docs/  @pm @tech-lead
/deploy/         @devops
/scripts/        @platform-team
```

规则：

- 任何 `backend/**` 改动必须由后端 owner review。
- 任何 `frontend/**` 改动必须由前端 owner review。
- 任何接口契约、数据结构、权限或流程变化必须由 Tech Lead 或模块 owner review。
- 任何 `business-pmo/**` 改动必须由 PMO 或 Tech Lead review。

## 9. 后端业务模块结构

业务后端按 Mango 模块分层组织：

```text
backend/guarantee-domain/
├── guarantee-api
├── guarantee-support
├── guarantee-core
├── guarantee-starter
└── guarantee-starter-remote
```

约束：

- `api` 只放 `Command`、`Query`、`VO`、`XxxApi`、枚举。
- `support` 只放本域内部共享技术实现。
- `core` 放业务实现、实体、Mapper、转换和内部服务。
- `starter` 放 Controller、自动装配和 `module.properties`。
- `starter-remote` 放远程调用适配。
- `app` 只做装配，不承载业务实现。

## 10. 前端业务模块结构

业务前端按 app 和 package 分层：

```text
frontend/
├── apps/
│   └── guarantee-admin
└── packages/
    ├── guarantee
    └── guarantee-api
```

约束：

- `apps` 只负责布局、路由聚合、权限拦截和全局初始化。
- `packages/guarantee` 放保函页面、组件、路由声明和模块注册。
- `packages/guarantee-api` 放保函 API client 和类型。
- 业务包可以依赖 `@mango/common`、`@mango/api-schema` 和相关 Mango 能力包。
- 公共包不得反向依赖 `apps`。

## 11. PMO 继承关系

Mango PMO 需要拆出业务项目必须继承的生态基线。

建议后续新增：

```text
mango-pmo/rules/ecosystem/
├── 00-business-baseline.md
├── 01-business-backend.md
├── 02-business-frontend.md
├── 03-business-delivery.md
└── 04-business-template.md
```

业务项目规范关系：

```text
Mango ecosystem baseline + 当前业务领域规则
```

业务项目可以维护：

```text
business-pmo/rules/
├── 00-project-flow.md
└── guarantee/
    ├── 01-domain.md
    ├── 02-status-machine.md
    ├── 03-approval.md
    ├── 04-attachment.md
    └── 05-acceptance.md
```

约束：

- Mango baseline 优先于业务规则。
- 业务规则只能补充领域约束，不能放宽 Mango 模块、API、数据库、测试和交付规则。
- 如需调整基础规则，必须回到 Mango PMO 修改。

## 12. 模板仓库与初始化器

Mango 提供业务模板仓库和初始化器。

```bash
npm create mango-business@latest guarantee-platform
```

或：

```bash
mango init guarantee-platform
```

初始化选项：

```text
项目名
业务域
后端包名
部署模式：monolith / microservice
前端模式：admin / micro-frontend
启用能力：auth、authorization、org、workflow、numgen、file、template、notification
数据库：mysql / postgres
是否生成示例模块
```

CLI 能力：

```text
mango init <project>
mango add module <module>
mango add page <page>
mango add api <api>
mango contract add <name>
mango contract change <id>
mango contract check
mango doctor
mango dev
mango build
mango pmo preflight
```

落地优先级：

1. 模板仓库。
2. `mango init`。
3. `mango add module/page/api/contract`。
4. `mango doctor`。
5. 发布和版本检查。

## 13. 接口契约协作流程

前端需要后端调整时，必须发起接口契约变更。

```text
FE 提出接口需求
-> BE / Tech Lead 批阅方案
-> 契约状态 APPROVED
-> FE 基于契约继续页面和 mock 开发
-> BE 或 FE+AI 实现后端
-> BE owner review backend/**
-> FE 联调真实接口
-> 台账 DONE
```

契约文件：

```text
business-docs/contracts/guarantee-api.md
```

契约变更记录必须包含：

```text
接口
字段
语义
来源
空值规则
权限规则
实现 owner
review owner
契约状态
验收方式
```

契约状态：

```text
REQUESTED
APPROVED
BACKEND_READY
INTEGRATED
DONE
EXCEPTION
```

只要状态到 `APPROVED`，前端即可继续开发，不等待真实接口完全实现。

## 14. AI 参与实现规则

AI 可以参与前端、后端、契约和测试实现，但 owner 与验收责任不能转移给 AI。

实现模式：

```text
A. 后端 owner 实现：复杂数据、权限、事务、流程、性能场景。
B. 前端使用 AI 实现后端：简单 VO 字段、简单查询条件、轻微接口扩展。
C. 前后端结对使用 AI：中等复杂页面和接口。
```

约束：

- 后端接口方案必须由后端 owner 或 Tech Lead 批准。
- 前端使用 AI 修改 `backend/**` 时，后端 owner 必须 review。
- mock 只能用于并行开发，最终验收必须连接真实接口。
- 复杂数据库、权限、流程、跨模块聚合和性能相关变更必须由后端 owner 实现或结对实现。

## 15. 避免前后端互相阻塞的机制

- 先确认契约，不等待实现。
- 契约 `APPROVED` 后，前端可继续页面、类型、表单、表格、状态、空态、错误态和契约 mock。
- 后端优先交付可联调接口，再补齐完整业务规则和测试，但最终交付不得降低验收标准。
- 交付台账记录每个接口、页面、权限、数据、测试项状态。
- 同一任务使用任务 worktree，不在主工作区直接开发。

## 16. 评审组织方案

### 16.1 评审目标

确认本方案是否作为 Mango 业务项目开发模式的基础方案，并确定后续落地 Sprint。

### 16.2 参会角色

- PM：确认业务项目启动和需求交付方式。
- Tech Lead：确认架构边界、仓库模式、接口协作和任务拆分。
- Backend Owner：确认后端模块、API、DB、测试和 owner 规则。
- Frontend Owner：确认前端包、页面、契约 mock 和联调方式。
- QA：确认验收、台账、联调和 E2E 规则。
- PMO：确认规范继承、baseline 和治理方式。
- DevOps：确认模板启动、环境、发布和私服依赖。

### 16.3 评审议题

| 议题 | 结论口径 |
|---|---|
| 业务项目是否默认 product monorepo | 接受 / 调整 / 例外条件 |
| Mango 框架是否以 Maven BOM 和 npm 包对外提供 | 接受 / 调整 |
| Mango PMO 是否新增 ecosystem baseline | 接受 / 调整 |
| 业务 PMO 是否只补充领域规则 | 接受 / 调整 |
| `mango-business-template` 是否作为第一落地资产 | 接受 / 调整 |
| `mango init` 是否作为第二落地资产 | 接受 / 调整 |
| 前端接口需求是否必须走契约变更 | 接受 / 调整 |
| FE+AI 修改后端是否允许，review 责任如何约束 | 接受 / 调整 |
| CODEOWNERS 和 worktree 是否作为默认协作机制 | 接受 / 调整 |

### 16.4 评审前准备

- 本评审文档。
- 本评审台账。
- 当前 Mango Maven 模块清单。
- 当前 Mango UI npm package 清单。
- 当前 PMO 规范清单。

### 16.5 评审输出

评审结束后新增评审记录：

```text
mango-docs/plans/2026-05-27-business-project-development-model-review-record.md
```

记录内容：

```text
参会人
评审结论
已确认决策
需调整项
后续 Sprint
责任人
截止时间
```

## 17. 后续 Sprint 建议

### Sprint 1：Mango 对外依赖发布能力检查

目标：确认业务项目能否不依赖 Mango 源码，仅通过 jar/npm 使用框架。

交付物：

- Maven BOM 发布检查。
- 后端 starter 对外依赖检查。
- `@mango/*` npm 包发布检查。
- 版本锁定和私服配置说明。

### Sprint 2：Mango ecosystem baseline

目标：把业务必须遵守的规则从 Mango PMO 中抽成生态基线。

交付物：

- `mango-pmo/rules/ecosystem/*`。
- preflight 路由规则。
- 业务项目 AGENTS 模板。

### Sprint 3：业务模板仓库

目标：提供 clone 后可运行的业务项目模板。

交付物：

- `mango-business-template`。
- 示例业务域。
- `business-pmo` 和 `business-docs` 初始结构。
- 本地启动脚本。

### Sprint 4：`mango init`

目标：提供类似 Vue init 的项目初始化体验。

交付物：

- `npm create mango-business@latest`。
- `mango init`。
- 变量替换、能力选择、环境检查。

### Sprint 5：契约与模块生成器

目标：支持持续业务开发。

交付物：

- `mango add module`。
- `mango add page`。
- `mango add api`。
- `mango contract add/change/check`。

## 18. 风险与限制

- 当前方案尚未验证 Mango Maven 和 npm 发布物是否完整满足业务外部依赖。
- 当前方案尚未实现 `mango-pmo/rules/ecosystem/*`。
- 当前方案尚未实现业务模板仓库和 CLI。
- 单仓模式要求 CODEOWNERS、PR 规则和 worktree 流程严格执行，否则仍可能产生职责混乱。
- FE+AI 修改后端只能在后端 owner review 下使用，不能成为绕过后端质量责任的方式。

