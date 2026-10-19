---
documentId: PLAN-INFO-COLLECTION-001
documentType: implementation-plan
pmoVersion: 1.4.7
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: "继承 TDD-INFO-COLLECTION-001 的 L3：跨租户公开提交、个人信息、真实数据库、管理端页面和服务端 SK。"
status: DRAFT
action: WRITE
owner: Mango 实施负责人
approver: 实施负责人（待审批）
approvalEvidence: 用户已明确要求直接开始研发；正式实施审批尚未补录
upstreamDocumentId: TDD-INFO-COLLECTION-001
upstreamDocumentHash: e677159930c7afa66ea423f5d38c80971d2219aa443e70e38db22e32d174f540
---

# Mango 信息采集管理实施计划

> **先读：这份文档要回答什么**
> - **一句话目标：** 按已确认系统需求和技术设计，交付真实信息采集后端模块、管理端页面、公开托管入口和逐项验收证据。
> - **真实场景：** 先完成数据库和后端入口，再接入管理端页面；最后使用两个租户、两份不同表单和浏览器公开入口验证动态列、SK 和处理闭环。
> - **关键输入 / 输出：** 输入是 TDD、源码模块规范和本地 Mango 工作区；输出是可构建模块、真实页面、真实数据和截图/视频/测试结果。
> - **成功与失败：** 成功必须同时满足真实数据库、权限租户、公开提交和页面交互；构建通过但没有真实数据、权限或截图不算完成。
> - **明确不做：** 不实现 P1 文件上传、工作流、营销自动化、支付、签名和多步骤表单。
> - **阅读顺序：** 先看交付物和任务依赖，再看验证计划、数据步骤、风险和追踪矩阵。
> - **任务图入口：** `node mango-pmo/tools/render-implementation-plan-graph.mjs --document mango-docs/designs/issue-1055-information-collection/implementation-plan.md`

## 0. 使用合同与最小填写方式

本计划只拆解 TDD 已确定的模块、API、数据、页面和测试，不在计划中改变上游业务规则或技术设计。所有任务初始为 `PLANNED`，验证结果在研发和 QA 阶段回写交付证据，不预写通过结论。

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | MOD-001~003, DB-001~003 | 信息采集后端模块 | `mango/mango-platform/mango-information` | api/core/starter 可构建，migration、资源菜单和真实接口具备 | SAC-001~010 | 不修改 CMS 表和接口 |
| DEL-002 | MOD-004, UI-001~005 | 管理端和托管表单页面 | `mango-ui/packages/information`, admin manifest | 页面注册、真实 API、样式和构建接入 | SAC-001~012 | 不复制 CMS 页面实现 |
| DEL-003 | TC-001~008 | 后端和前端测试资产 | 对应模块 `src/test`、`packages/information/src/__tests__`、admin e2e | 用例可单独执行并使用真实测试数据边界 | SAC-001~012 | 不把 mock 当作真实链路证据 |
| DEL-004 | IMP-001 | 能力说明和验收记录 | `mango/mango-platform/mango-information/README.md`, `mango-docs/evidence/issue-1055` | 使用方式、命令、环境、截图、视频和结果可复核 | SAC-009~012 | 不保存密码、token 或 SK |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | MOD-001, MOD-002, DB-001~003 | DEL-001 | 后端开发 | `mango-information-api/core` | NONE | 落实协议模型、状态枚举、错误码、Entity、Mapper 和 migration。 | 模块结构符合 API/core/persistence 边界，表归属可检查。 | VAL-001 | B1 | PLANNED |
| TASK-002 | FLOW-001, FLOW-002, DM-001~003, SEC-001~003 | DEL-001 | 后端开发 | `mango-information-core` | TASK-001 | 落实表单定义、字段校验、提交快照、处理状态、租户和 SK 服务。 | Service 使用真实 Mapper、Require 和事务，失败不写半条记录。 | VAL-002 | B1 | PLANNED |
| TASK-003 | API-001~007, ERR-001~005 | DEL-001 | 后端开发 | `mango-information-starter` | TASK-002 | 落实后台、公开定义、公开提交和服务端提交入口。 | HTTP 契约、权限、租户、错误反馈和 OpenAPI 注解符合设计。 | VAL-003 | B2 | PLANNED |
| TASK-004 | DEC-001, IMP-001 | DEL-001, DEL-004 | 后端开发 | platform parent、admin starter、Resource Registry | TASK-003 | 接入 Maven reactor、starter 聚合、模块声明和单一后台菜单资源。 | 干净启动能同步 `information` 菜单，菜单路径和页面 key 一致。 | VAL-004 | B2 | PLANNED |
| TASK-005 | MOD-004, UI-001~003 | DEL-002 | 前端开发 | `mango-ui/packages/information` | TASK-003 | 落实 API factory、表单列表、详情和配置页面。 | 页面使用真实 API、标准 Mango 页面骨架并覆盖加载/空/失败态。 | VAL-005 | B2 | PLANNED |
| TASK-006 | UI-004~005 | DEL-002 | 前端开发 | `mango-ui/packages/information` | TASK-005 | 落实动态提交列、详情抽屉、处理状态、导出和托管表单。 | 当前表单上下文、动态列、已关闭和公开提交路径可操作。 | VAL-006 | B3 | PLANNED |
| TASK-007 | MOD-004, IMP-001 | DEL-002, DEL-004 | 前端开发 | admin modules、full registrar、styles | TASK-006 | 接入 admin full package、样式聚合、构建入口和模块页面注册。 | 单体管理端构建和样式边界检查通过。 | VAL-007 | B3 | PLANNED |
| TASK-008 | TC-001~007 | DEL-003 | 后端开发与 QA | backend test modules | TASK-003 | 编写真实数据库、API、租户、SK、状态和失败分支测试。 | 用例独立准备测试数据并执行 P0/P1 后端验证。 | VAL-008 | B4 | PLANNED |
| TASK-009 | TC-001~008 | DEL-003 | 前端开发与 QA | `mango-admin/e2e`, information tests | TASK-007 | 编写页面状态、操作路径、真实 API、截图和视频验收脚本。 | 每个页面/功能具备业务断言、UI 断言、console/network 检查。 | VAL-009 | B4 | PLANNED |
| TASK-010 | UI-001~005, TC-008 | DEL-004 | QA | local workspace evidence | TASK-008, TASK-009 | 启动工作区并逐页面复核原型、SRS 和真实数据结果。 | 正常、空、错误、无权、停用、处理和返回场景均有证据。 | VAL-010 | B5 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001, TASK-002 | TDD 可执行且模块路径确认。 | 数据模型、migration 和核心 Service 可构建。 | NONE | NONE | 数据模型或租户边界不清时回到 TDD。 | Tech Lead |
| MS-002 | TASK-003, TASK-004, TASK-005 | 后端核心可用。 | 真实后端入口、菜单资源和前三个管理页面可访问。 | MS-001 | TASK-004 与 TASK-005 可并行。 | API 或菜单契约失败时停止页面联调。 | Dev Lead |
| MS-003 | TASK-006, TASK-007 | 管理页面基础链路可用。 | 提交记录、托管表单、动态列和 admin 构建接入完成。 | MS-002 | TASK-006 与 TASK-007 可并行。 | 动态列或样式边界失败时停止验收。 | Dev Lead |
| MS-004 | TASK-008, TASK-009, TASK-010 | 代码和页面链路可运行。 | 后端、前端、截图、视频和人工验收证据全部登记。 | MS-003 | TASK-008 与 TASK-009 可并行。 | 任一 P0 失败不得进入交付。 | QA Lead |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001 | TASK-001 | M09/M11 | `mvn -pl :mango-information-api,:mango-information-core -am verify` | 当前 worktree reactor | Flyway V1、测试租户 | 租户表字段和索引可读 | 模块构建、migration 和 schema 检查通过 | `.runtime/issue-1055/val-001` | 后端开发 | 修复模块结构或 migration 后重跑 |
| VAL-002 | TC-002 | TASK-002 | M10/M11 | `mvn -pl :mango-information-core -am test` | 隔离测试库 | 两表单、字段快照和提交记录 | 两租户、授权账号 | 状态、校验、快照、事务和隔离断言通过 | `.runtime/issue-1055/val-002` | 后端开发 | 修复业务规则或测试数据后重跑 |
| VAL-003 | TC-004, TC-007 | TASK-003 | M12 | `mvn -pl :mango-information-starter -am verify` | 单体入口 | 停用表单、非法值、两个租户、有效/失效 SK | 匿名、服务端、跨租户账号 | 错误契约、租户、SK 和失败一致性通过 | `.runtime/issue-1055/val-003` | 后端开发 | 任何越权或半条记录均阻断 |
| VAL-004 | TC-001 | TASK-004 | M09/M11 | 启动单体并检查 Resource Registry 菜单同步和页面 key | 当前工作区 | 空库初始化和管理员账号 | 当前租户管理员 | 只有 `信息采集 / 表单管理` 菜单且权限一致 | `.runtime/issue-1055/val-004` | 后端开发 | 回到资源声明和菜单契约 |
| VAL-005 | TC-001, TC-003 | TASK-005 | M09/M12/M13 | `pnpm --filter @mango/information test && pnpm --filter @mango/information build` | mango-ui workspace | 真实 API 测试替身只隔离 HTTP 客户端 | 管理员和无权状态 | 列表、详情、配置页面类型、构建和状态分支通过 | `.runtime/issue-1055/val-005` | 前端开发 | 修复 API factory 或页面状态后重跑 |
| VAL-006 | TC-002, TC-003, TC-004, TC-006 | TASK-006 | M12/M13 | admin 浏览器验证提交记录、动态列、抽屉、处理和托管提交 | Mango workspace | 两份字段不同表单和历史快照 | 当前租户管理员、匿名访客 | 动态列、失败反馈、已关闭和托管提交符合 SRS | `.runtime/issue-1055/val-006` | 前端开发 | 记录页面/接口失败并修复后重测 |
| VAL-007 | TC-008 | TASK-007 | M09 | `pnpm admin:styles:check && pnpm admin:module-styles:check && pnpm --filter mango-admin build` | mango-ui workspace | information package manifest | 单体 admin | 页面注册、样式聚合和生产构建通过 | `.runtime/issue-1055/val-007` | 前端开发 | 修复 manifest 或样式入口 |
| VAL-008 | TC-001~007 | TASK-008 | M10/M11/M12 | 按单用例、能力标签和全量套件执行后端测试 | 隔离数据库和测试租户 | `IT_INFO_1055_*` 数据 | 两租户、管理员、匿名和服务端入口 | P0/P1 后端用例全部通过 | `.runtime/issue-1055/val-008` | QA Lead | 失败用例补回归测试 |
| VAL-009 | TC-001~008 | TASK-009 | M13 | `pnpm --dir mango-ui/apps/mango-admin playwright test --grep information` | 浏览器 Chromium | 测试租户和真实后端 | 管理员、无权账号、匿名访客 | 页面业务/UI/网络/控制台断言和截图视频齐全 | `.runtime/issue-1055/val-009` | QA Lead | 页面未通过则不得进入人工验收 |
| VAL-010 | TC-008 | TASK-010 | M16 | 按 `acceptance-evidence.md` 逐项复核页面和功能，保存截图/视频 | Mango workspace | 真实表单、提交、状态和历史快照 | 每个角色和租户边界 | 与 BRD/SRS/原型逐项一致，无未测阻断 | `mango-docs/evidence/issue-1055` | QA Lead | 记录 FAIL/BLOCKED 并回到原任务 worktree 修复 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| DATA-001 | DB-001~003 | 本地空库 | workspace 数据库名来自 `.mango/dev-workspace.env`，确认不是共享库 | 执行信息采集模块 Flyway migration | 1 | 空库，无回填 | 表、索引、tenant_id 和审计字段存在 | migration 失败停止后端启动 | 删除当前 worktree 测试库后重建 | 后端开发 |
| DATA-002 | DB-001~003 | 隔离测试库 | 测试库和测试租户可用 | 创建两租户、两份表单和可清理提交记录 | 2 | 唯一 `IT_INFO_1055_` 前缀 | 读回表单、提交、快照和处理结果 | 出现跨租户记录或残留数据停止验收 | 清理本用例创建数据 | QA Lead |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | DEL-001, DEL-002, DEL-004 | information module README、前端 package README、验收证据 | 增加模块用途、入口、权限、接入和验证命令 | Dev Lead | 文档不包含密码、token、SK；命令可执行 | `node mango-pmo/tools/check-document-reading-entry.mjs` | NONE |
| DOC-002 | DEL-003 | `mango-docs/evidence/issue-1055` | 增加测试结果、截图、视频索引和未测项 | QA Lead | 每个 SAC 都有结果和证据路径 | `node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/evidence/issue-1055/acceptance.md` | NONE |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 公开提交、动态 JSON、SK 或租户隔离实现与现有基线不一致 | 可能导致数据越权或半条记录 | 先执行真实数据库/API/浏览器测试，不使用 mock 作为完成证据 | 立即阻断发布，回到 TDD 和原任务 worktree 修复 | Tech Lead、QA Lead | 研发验收前 | OPEN | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| MOD-001~003 | DEL-001 | TASK-001~004 | VAL-001~004 | MS-001, MS-002 | 后端模块和菜单资源。 |
| MOD-004 | DEL-002 | TASK-005~007 | VAL-005~007 | MS-002, MS-003 | 前端 package、页面和样式。 |
| DB-001~003 | DEL-001 | TASK-001~002 | VAL-001, VAL-002 | DATA-001, DATA-002 | 真实数据库。 |
| API-001~007 | DEL-001 | TASK-003 | VAL-003, VAL-006 | MS-002, MS-003 | 后台和公开入口。 |
| SEC-001~003 | DEL-001, DEL-003 | TASK-002, TASK-003, TASK-008 | VAL-002, VAL-003, VAL-008 | RISK-001 | 租户、权限和 SK。 |
| ERR-001~005 | DEL-001 | TASK-002, TASK-003 | VAL-002, VAL-003 | RISK-001 | 错误和失败边界。 |
| UI-001~005 | DEL-002 | TASK-005~007 | VAL-005~007, VAL-009, VAL-010 | MS-003, MS-004 | 页面和交互。 |
| TC-001~008 | DEL-003, DEL-004 | TASK-008~010 | VAL-008~010 | MS-004, DOC-002 | 测试和证据。 |
| IMP-001 | DEL-004 | TASK-004, TASK-007, TASK-010 | VAL-004, VAL-007, VAL-010 | DOC-001, DOC-002 | 能力说明和交付记录。 |
| UI-001 | DEL-002 | TASK-005 | VAL-005 | MS-002 | 列表页面。 |
| UI-002 | DEL-002 | TASK-005 | VAL-005 | MS-002 | 详情页面。 |
| UI-003 | DEL-002 | TASK-005 | VAL-005 | MS-002 | 配置页面。 |
| UI-004 | DEL-002 | TASK-006 | VAL-006 | MS-003 | 提交记录页面。 |
| UI-005 | DEL-002 | TASK-006 | VAL-006 | MS-003 | 托管表单。 |
| TC-006 | DEL-003 | TASK-009 | VAL-009 | MS-004 | 历史快照页面验收。 |
| TC-008 | DEL-004 | TASK-009, TASK-010 | VAL-009, VAL-010 | MS-004, DOC-002 | 页面状态和人工验收。 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | 待执行 | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/issue-1055-information-collection/implementation-plan.md` |
| 生命周期 handoff | 待执行 | TDD checker、TDD hash 和技术审批待完成。 |
| 依赖图 | 待执行 | `node mango-pmo/tools/render-implementation-plan-graph.mjs --document mango-docs/designs/issue-1055-information-collection/implementation-plan.md` |
| 未关闭阻断数量 | 1 | RISK-001 为开放风险，不是例外审批；真实入口验证必须完成。 |
| 实施审批 | 待实施负责人审批 | 用户已明确要求直接开始研发，不等同于实施负责人签批。 |
