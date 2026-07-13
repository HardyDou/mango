# 支付模块历史架构债务治理交付契约

## 1. 目标

在一个任务分支内为 `mango-payment` 建立有业务价值的单元、API、集成与入口测试基线，一次性消除四个支付子模块的 1,869 条历史架构问题；改造后运行同一测试入口，证明支付业务不变量正确、批准后的接口和 String 租户迁移正确，并把正式架构预算下调到 0。

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
- 当前架构预算：`mango-pmo/baselines/architecture/debt-budget.json`，payment 历史问题 1,869。
- 现有 改造前正式执行 268 条支付测试与 `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts`。

## 5. 设计说明

### 5.1 影响模块

支付后端四子模块、支付前端包、支付 E2E、支付 migration/README/设计说明、架构规则测试与正式债务预算。

### 5.2 接口变化

API 变为传输无关契约；Controller 与 Feign 使用同一方法签名分别重声明 HTTP 绑定；路径变量改固定子路径与显式 query/body；`PaymentCode` 移至 `io.mango.payment.api.enums` 并保持 code/message；支付前端同步唯一新版接口目录。

### 5.3 数据变化

所有带租户支付实体使用 canonical `TenantEntity`、String tenantId 与 Long orgId；新增 V102 将历史 payment 表 `tenant_id` 值保持转换为 `VARCHAR(64)` 并补齐可空 `org_id BIGINT`，同步索引/唯一约束和 Mapper 条件，不修改既有 migration。

### 5.4 菜单/页面/权限变化

不新增菜单、页面或权限码；保持现有支付页面、按钮权限与业务状态。新增的固定公网回调 GET/POST 路由以 PUBLIC `API_RESOURCE` 注册，替换历史动态回调入口；同步 String ID/tenant 测试数据和受影响入口用例。

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
| TC-001 | AC-001 | 现有支付 suite 与新增有效用例的 before/after | P0 | API | AUTO | 用例独立 `TEST_/IT_` 前缀 | 同一 Maven 命令、用例集合和业务断言 | payment 四子模块 Maven suite | `mango-docs/evidence/baselines/payment-architecture/latest/` | AUTOMATED |
| TC-002 | AC-002 | 金额、订单/退款状态、重复处理、回调与错误码 | P0 | 单元 | AUTO | 固定金额边界、状态和重复事件 | Money、状态表、幂等键、PaymentCode code/message | payment core 定向测试与 TC-001 | 同上 | AUTOMATED |
| TC-003 | AC-002 | 通知、对账、差错、结算、离线收付正常与失败 | P1 | 组件 | AUTO | 唯一批次、明细、凭证和通知记录 | 状态、汇总金额、差异、重试和审计 | payment core 定向测试与 TC-001 | 同上 | AUTOMATED |
| TC-004 | AC-004 | Api/Controller/Feign 目录、MVC 校验、权限与错误转换 | P0 | API | AUTO | 合法/非法模型和权限用户 | verb/path/binding/generic/validation/permission/R/error | starter/remote tests 与 TC-001 | 同上 | AUTOMATED |
| TC-005 | AC-004 | OpenAPI HMAC、timestamp、nonce、IP、篡改和防重放 | P0 | API | AUTO | 测试专用固定签名向量 | canonical request/signature | OpenAPI tests 与 TC-001 | 同上 | AUTOMATED |
| TC-006 | AC-005 | V3-V102、tenant 值保持、索引/约束、Mapper 与双租户 | P0 | 组件 | AUTO | 隔离 `mango_dev_*`、两个 `IT_PAY_` 租户 | schema metadata、逐行值、记录数和查询结果 | migration/Mapper integration tests | 同上 | AUTOMATED |
| TC-007 | AC-003 | 架构规则正反例、完整扫描与预算比较 | P0 | 单元 | AUTO | Java fixture 与完整 compiled classes | payment 1,869→0、其它新增 0 | rule tests + full verify/report/budget | 同上 | AUTOMATED |
| TC-008 | AC-004 | 支付前端类型、API client 与受影响页面入口 | P1 | E2E | AUTO | 独立测试库、账号、租户和可清理数据 | URL/query/body、页面业务结果和异常状态 | payment build + payment-center E2E | `EXCEPTION: build 与 Playwright 108 条用例收集通过；未在本次 migration 证明库上启动完整页面环境，不宣称页面结果通过` | CANDIDATE |

## 6. 风险与限制

- L3 级联改造只允许通过已批准接口映射改变契约，业务不变量任何差异均阻断。
- String tenant migration 必须在隔离 MySQL 全量重放；不得连接共享业务库写测试数据。
- 检查器只在正反例证明误判时修改；不能用支付治理降低全仓红线。
- UI 入口依赖本地服务、数据库与账号；如环境不可用只可记录 BLOCKED，不可扩大后端验证结论。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求；SAC-001 | 补有价值测试并建立改造前基线 | 生产代码未变时先补测试并运行统一 suite | `mango/mango-platform/mango-payment` | `mango/mango-platform/mango-payment/README.md` | `mango-docs/designs/payment-architecture-debt/implementation-plan.md` | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | TC-001 至 TC-006、测试质量检查 | DONE | `mango-docs/evidence/baselines/payment-architecture/latest/` |
| TASK-002 | 用户要求；SAC-002 | 支付逻辑与特性保持正确 | canonical 分层迁移，业务断言不变 | `mango/mango-platform/mango-payment` | `mango/mango-platform/mango-payment/README.md` | `mango-docs/designs/payment-architecture-debt/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | TC-001/002/003/005 before-after | DONE | 同上 |
| TASK-003 | 用户批准；SAC-004 | Java/HTTP/Feign 接口一次迁移到位 | 唯一固定路径/query/body 与适配器 parity，不保留历史入口 | `mango/mango-platform/mango-payment` | `mango/mango-platform/mango-payment/README.md` | `mango-docs/designs/payment-architecture-debt/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | TC-004/005/008 | DONE | 同上 |
| TASK-004 | 用户批准；SAC-005 | String tenant 值保持与隔离正确 | TenantEntity + V102 + 真实 MySQL 双租户 | `mango/mango-platform/mango-payment/mango-payment-core` | `mango/mango-platform/mango-payment/README.md` | `mango-docs/designs/payment-architecture-debt/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | TC-006 | DONE | 同上 |
| TASK-005 | 用户要求；SAC-003 | payment 历史架构债务归零 | 修真实违规；误判必须先有正反例；模块预算只下降 | `mango/mango-platform/mango-payment` | `mango/mango-platform/mango-payment/README.md` | `mango-docs/designs/payment-architecture-debt/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | TC-007、完整报告和预算检查 | DONE | `mango/target/mango-architecture-report.json` 与同上 |
| TASK-006 | PMO；SAC-004 | 支付前端、文档和入口测试同步 | 后端/前端/remote 同一版本批次 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango/mango-platform/mango-payment/README.md` | `mango-docs/designs/payment-architecture-debt/implementation-plan.md` | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | package build、Playwright list、文档 checker；浏览器执行见 TC-008 EXCEPTION | DONE | 同上 |
| TASK-007 | 用户要求；SAC-001 至 SAC-005 | 改造后跑同一测试并对比 | after 复用 before 命令/业务断言，批准接口差异独立列出 | `mango/mango-platform/mango-payment` | `mango/mango-platform/mango-payment/README.md` | `mango-docs/designs/payment-architecture-debt/implementation-plan.md` | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | 所有 VAL-001 至 VAL-008 | DONE | 同上 |

## 8. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001, TASK-007 | TC-001 | payment 四子模块 Maven suite | before/after 同入口回归 | 用例独立 `TEST_/IT_` 前缀 | 同一 suite 从 268/268 到 275/275，失败、错误、跳过均为 0 | 不涉及页面交互；API 与组件行为由自动化断言验证 | Maven 执行无失败，报告记录各模块计数 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | DONE |
| TASK-002, TASK-007 | TC-002 | payment core 金额、状态、幂等与回调 | 核心业务不变量 | 固定金额边界、状态与重复事件 | Money、订单/退款状态、幂等键及 PaymentCode 断言全部通过 | 不涉及页面交互；核心逻辑由单元测试验证 | 定向用例并入 275 条 suite，无未解释错误 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | DONE |
| TASK-002, TASK-007 | TC-003 | payment core 通知、对账、差错与结算 | 扩展业务链路不变量 | 唯一批次、明细、凭证与通知记录 | 状态、汇总金额、差异、重试与审计断言全部通过 | 不涉及页面交互；组件行为由自动化测试验证 | 定向用例并入 275 条 suite，无未解释错误 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | DONE |
| TASK-003, TASK-007 | TC-004 | Payment API、Controller 与 Feign | 固定路径、绑定、校验、权限与错误转换 | 合法/非法模型及权限用户 | verb/path/body/query/generic/validation/permission/R 契约断言通过 | 不涉及管理页面；HTTP 契约由 MVC 测试验证 | starter 契约测试无 4xx/5xx 非预期结果 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | DONE |
| TASK-003, TASK-007 | TC-005 | `/openapi/pay/**` | HMAC、timestamp、nonce、IP、篡改和防重放 | 固定签名向量与 `tenant-alpha` | 8/8 OpenAPI 集成用例通过，String tenant 保持原值 | 不涉及管理页面；开放接口由集成测试验证 | 合法请求成功，非法签名与重放按契约拒绝 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | DONE |
| TASK-004, TASK-007 | TC-006 | payment V3-V102 migration 与 Mapper | tenant 值保持、orgId、索引约束和隔离 | 独立库 44 表、152 行、tenant 178 | 44 列由 BIGINT 转 VARCHAR(64)，44 个 org_id，152 行值全部保持 | 不涉及页面交互；schema 与逐行数据由 SQL 证明 | migration 执行无失败，双租户查询断言通过 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | DONE |
| TASK-005, TASK-007 | TC-007 | payment 完整架构扫描与正式预算 | 历史债务归零且不改写其它模块条目 | 212 Reactor 模块完整报告 | payment 1,869→0，四子模块均为 0，非 payment 模块预算条目未改变 | 不涉及页面交互；规则正反例与完整扫描验证 | full verify 212/212 BUILD SUCCESS，blocking 为 0 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | DONE |
| TASK-003, TASK-006 | TC-008 | payment-center 页面入口与 OpenAPI 请求脚本 | 新固定 POST 路径、JSON body 与 String tenant | Playwright 108 条三浏览器用例清单 | 原因：本次只完成 `@mango/payment` build 和 Playwright 用例收集，未在 migration 证明库上启动完整页面环境 | 请求辅助函数已同步；未执行真实浏览器页面交互 | build/list 通过；因未启动前后端，未产生真实 network 结果 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | EXCEPTION |

## 9. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001 至 TASK-007 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | implementation-plan VAL-001 至 VAL-008 | worktree slot 178；版本由报告记录 | `.mango/workspace.json` 独立库；用例独立数据 | 专用支付测试账号/租户；不记录凭据 | before 268/268、after 275/275，架构 1,869→0；package build 与 Playwright 108 条收集通过 | TC-008 浏览器执行 EXCEPTION；changed-mode 全仓报告中的规则误报减少未用于改写范围外 209 个模块预算 | `mango-docs/evidence/baselines/payment-architecture/latest/` | 业务不变量 0 非批准变化；接口差异符合批准映射；不声明页面结果通过 |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 新版 payment Java/HTTP/String tenant 接入、测试用例清单、before/after 结果和升级步骤 | `mango/mango-platform/mango-payment/README.md`、`mango-docs/designs/payment-architecture-debt/technical-design.md`、`mango-docs/evidence/baselines/payment-architecture/latest/report.md` | `mango-docs/designs/payment-architecture-debt/implementation-plan.md` 的 VAL-001 至 VAL-008 | 隔离 `mango_dev_*`；测试专用账号/租户；用例自行清理 | 查看 latest 报告定位用例；Mango 能力问题按 Issue Runbook 升级 | DONE |
