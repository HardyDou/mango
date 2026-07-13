# 支付模块历史架构债务治理交付契约

## 1. 目标

在一个任务分支内为 `mango-payment` 建立有业务价值的单元、API、集成与入口测试基线，一次性消除四个支付子模块的 1,843 条历史架构问题；改造后运行同一测试入口，证明支付业务不变量正确、批准后的接口和 String 租户迁移正确，并把正式架构预算下调到 0。

## 2. 范围

- `mango-payment-api/core/starter/starter-remote` 的 API、协议模型、Service、Mapper、Entity、Controller、Feign、装配、错误契约和测试。
- `mango-ui/packages/payment` 及 `payment-center.spec.ts` 中受接口迁移影响的支付自有调用与入口测试。
- payment migration、模块 README、统一支付设计说明、本任务生命周期文档、测试结果基线和架构债务预算。
- 经规则正反例证明的架构检查器准确性修复。

## 3. 不做什么

- 不改造其它模块的业务能力；用户已确认其它模块不依赖支付模块。
- 不保留历史 Java/HTTP/租户契约的第二套兼容实现。
- 不执行版本发布和生产部署；发布进入后续统一批次。
- 不以覆盖率数字、编译成功或接口 200 代替支付行为验证。

## 4. 设计输入

- [BRD](../designs/payment-architecture-debt/business-requirements.md)、[SRS](../designs/payment-architecture-debt/system-requirements.md)、[TDD](../designs/payment-architecture-debt/technical-design.md) 与 [Plan](../designs/payment-architecture-debt/implementation-plan.md)。
- 用户审批记录：`mango-docs/designs/payment-architecture-debt/review/`。
- 当前架构预算：`mango-pmo/baselines/architecture/debt-budget.json`，payment 历史问题 1,843。
- 现有 59 个支付测试类、274 个测试方法与 `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts`。

## 5. 设计说明

### 5.1 影响模块

支付后端四子模块、支付前端包、支付 E2E、支付 migration/README/设计说明、架构规则测试与正式债务预算。

### 5.2 接口变化

API 变为传输无关契约；Controller 与 Feign 使用同一方法签名分别重声明 HTTP 绑定；路径变量改固定子路径与显式 query/body；`PaymentCode` 移至 `io.mango.payment.api.enums` 并保持 code/message；支付前端同步唯一新版接口目录。

### 5.3 数据变化

所有带租户支付实体使用 canonical `TenantEntity` 和 String tenantId；新增 V102 将历史 payment 表 `tenant_id` 值保持转换为 `VARCHAR(64)`，同步索引/唯一约束和 Mapper 条件，不修改既有 migration。

### 5.4 菜单/页面/权限变化

不新增菜单、页面或权限资源；保持现有支付页面、按钮权限与业务状态。仅同步支付前端 API client、String ID/tenant 测试数据和受影响入口用例。

### 5.5 测试范围

现有支付 suite 加订单/金额/状态/幂等/回调/退款/通知/对账/差错/结算、MVC 契约、HMAC/防重放、权限、双租户、migration/Mapper、规则正反例和支付页面入口测试；before/after 使用同一 Maven suite 与相同业务断言。

### 5.6 交付物料同步判断

| 物料 | 是否需要更新 | 路径或 EXCEPTION 依据 |
|---|---|---|
| 代码 | 是 | `mango/mango-platform/mango-payment`、`mango-ui/packages/payment`、必要时 `mango/mango-tools/mango-architecture-rules` |
| README/使用说明 | 是 | `mango/mango-platform/mango-payment/README.md`、`mango-ui/packages/payment/README.md` |
| 需求文档 | 是 | `mango-docs/designs/payment-architecture-debt/business-requirements.md` 与 `system-requirements.md` |
| 详细设计文档 | 是 | `mango-docs/designs/payment-architecture-debt/technical-design.md` 与 `implementation-plan.md` |
| E2E 脚本 | 是 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` |
| 测试结果基线 | 是 | `mango-docs/evidence/baselines/payment-architecture/latest/` |

### 5.7 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | AC-001 | 现有支付 suite 与新增有效用例的 before/after | P0 | API | AUTO | 用例独立 `TEST_/IT_` 前缀 | 同一 Maven 命令、用例集合和业务断言 | payment 四子模块 Maven suite | `mango-docs/evidence/baselines/payment-architecture/latest/` | CANDIDATE |
| TC-002 | AC-002 | 金额、订单/退款状态、重复处理、回调与错误码 | P0 | 单元 | AUTO | 固定金额边界、状态和重复事件 | Money、状态表、幂等键、PaymentCode code/message | payment core 定向测试与 TC-001 | 同上 | CANDIDATE |
| TC-003 | AC-002 | 通知、对账、差错、结算、离线收付正常与失败 | P1 | 组件 | AUTO | 唯一批次、明细、凭证和通知记录 | 状态、汇总金额、差异、重试和审计 | payment core 定向测试与 TC-001 | 同上 | CANDIDATE |
| TC-004 | AC-004 | Api/Controller/Feign 目录、MVC 校验、权限与错误转换 | P0 | API | AUTO | 合法/非法模型和权限用户 | verb/path/binding/generic/validation/permission/R/error | starter/remote tests 与 TC-001 | 同上 | CANDIDATE |
| TC-005 | AC-004 | OpenAPI HMAC、timestamp、nonce、IP、篡改和防重放 | P0 | API | AUTO | 测试专用固定签名向量 | canonical request/signature | OpenAPI tests 与 TC-001 | 同上 | CANDIDATE |
| TC-006 | AC-005 | V3-V102、tenant 值保持、索引/约束、Mapper 与双租户 | P0 | 组件 | AUTO | 隔离 `mango_dev_*`、两个 `IT_PAY_` 租户 | schema metadata、逐行值、记录数和查询结果 | migration/Mapper integration tests | 同上 | CANDIDATE |
| TC-007 | AC-003 | 架构规则正反例、完整扫描与预算比较 | P0 | 单元 | AUTO | Java fixture 与完整 compiled classes | payment 1,843→0、其它新增 0 | rule tests + full verify/report/budget | 同上 | CANDIDATE |
| TC-008 | AC-004 | 支付前端类型、API client 与受影响页面入口 | P1 | E2E | AUTO | 独立测试库、账号、租户和可清理数据 | URL/query/body、页面业务结果和异常状态 | payment build + payment-center E2E | 同上 | CANDIDATE |

## 6. 风险与限制

- L3 级联改造只允许通过已批准接口映射改变契约，业务不变量任何差异均阻断。
- String tenant migration 必须在隔离 MySQL 全量重放；不得连接共享业务库写测试数据。
- 检查器只在正反例证明误判时修改；不能用支付治理降低全仓红线。
- UI 入口依赖本地服务、数据库与账号；如环境不可用只可记录 BLOCKED，不可扩大后端验证结论。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求；SAC-001 | 补有价值测试并建立改造前基线 | 生产代码未变时先补测试并运行统一 suite | payment 四子模块正式测试目录 | payment README 测试入口 | BRD/SRS/TDD/Plan | payment-center.spec.ts | latest payment architecture baseline | TC-001 至 TC-006、测试质量检查 | IN_PROGRESS | `mango-docs/evidence/baselines/payment-architecture/latest/` |
| TASK-002 | 用户要求；SAC-002 | 支付逻辑与特性保持正确 | canonical 分层迁移，业务断言不变 | payment api/core/starter/remote | payment backend README | TDD DEC-003/004 | payment-center.spec.ts | 同上 | TC-001/002/003/005 before-after | IN_PROGRESS | 同上 |
| TASK-003 | 用户批准；SAC-004 | Java/HTTP/Feign 接口一次迁移到位 | 唯一固定路径/query/body 与适配器 parity，不保留历史入口 | payment api/starter/starter-remote | backend/frontend README 迁移表 | TDD API-001 至 API-005 | payment-center.spec.ts | 同上 | TC-004/005/008 | IN_PROGRESS | 同上 |
| TASK-004 | 用户批准；SAC-005 | String tenant 值保持与隔离正确 | TenantEntity + V102 + 真实 MySQL 双租户 | core entity/mapper/migration/tests | backend README schema 要求 | TDD DEC-006/DB-002 | payment-center.spec.ts 的 String tenant 数据 | 同上 | TC-006 | IN_PROGRESS | 同上 |
| TASK-005 | 用户要求；SAC-003 | payment 历史架构债务归零 | 修真实违规；误判必须先有正反例；模块预算只下降 | payment 代码、条件性 checker 修复、debt budget | payment README 架构/升级说明 | TDD DEC-007/IMP-003 | payment-center.spec.ts | 同上 | TC-007、完整报告和预算检查 | IN_PROGRESS | `target/mango-architecture-report.json` 与同上 |
| TASK-006 | PMO；SAC-004 | 支付前端、文档和入口测试同步 | 后端/前端/remote 同一版本批次 | `mango-ui/packages/payment` 与 E2E | 两个 payment README | lifecycle docs 与 ledger | payment-center.spec.ts | 同上 | build、E2E、文档 checker | IN_PROGRESS | 同上 |
| TASK-007 | 用户要求；SAC-001 至 SAC-005 | 改造后跑同一测试并对比 | after 复用 before 命令/业务断言，批准接口差异独立列出 | 全部任务代码 | 测试交接与升级说明 | 交付台账/验收证据 | payment-center.spec.ts | latest before-after report | 所有 VAL-001 至 VAL-008 | IN_PROGRESS | 同上 |

## 8. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 至 TASK-007 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008 | payment Java/HTTP/数据库/管理页面 | before/after、业务不变量、批准接口、String tenant、架构归零 | 用例独立前缀、隔离库、专用账号/租户 | TDD TC-001 至 TC-008 | payment-center 受影响 P0/P1 页面与状态 | 最终记录无未解释错误或真实 BLOCKED | `mango-docs/evidence/baselines/payment-architecture/latest/` | IN_PROGRESS |

## 9. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001 至 TASK-007 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | implementation-plan VAL-001 至 VAL-008 | worktree slot 178；版本由报告记录 | `.mango/workspace.json` 独立库；用例独立数据 | 专用支付测试账号/租户；不记录凭据 | before/after 执行后填写精确通过/失败数与架构 1,843→0 | 执行后逐项记录 | `mango-docs/evidence/baselines/payment-architecture/latest/` | 业务不变量应为 0 非批准变化；接口差异按批准映射 |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 新版 payment Java/HTTP/String tenant 接入、测试用例清单、before/after 结果和升级步骤 | payment 两个 README、TDD/Plan 与 latest baseline | VAL-001 至 VAL-008；支持单类、payment suite 与 payment-center E2E | 隔离 `mango_dev_*`；测试专用账号/租户；用例自行清理 | 查看 latest 报告定位用例；Mango 能力问题按 Issue Runbook 升级 | IN_PROGRESS |
