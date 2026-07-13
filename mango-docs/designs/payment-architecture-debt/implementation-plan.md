---
documentId: PLAN-PAYMENT-DEBT
documentType: implementation-plan
pmoVersion: 1.1.1
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: rules/09-test-case-automation-flow.md 中支付、公共契约、租户、持久化、数据一致性和架构门禁变化的 L3 判定
status: APPROVED
action: NEXT
owner: Mango 支付能力负责人
approver: HardyDou
approvalEvidence: review/PLAN-PAYMENT-DEBT.md
upstreamDocumentId: TDD-PAYMENT-DEBT
upstreamDocumentHash: d22ff817f078179985c3bf0a4853ffd9ef7640f69f529548361d72128579a0db
---

# 支付模块历史架构债务治理实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001, DEC-002, TC-001, TC-002, TC-003, TC-004, TC-005, TC-006 | 支付长期自动化 suite 与改造前后结果基线 | payment 四子模块正式测试目录；`mango-docs/evidence/baselines/payment-architecture/latest` | 有效测试补强后生产代码未变时形成 before；改造后同入口形成 after；业务不变量与批准差异逐项可比较 | SAC-001, SAC-002, SAC-004, SAC-005 | 不以覆盖率数字或无价值测试为目标 |
| DEL-002 | DEC-003, DEC-004, DEC-005, MOD-001, MOD-002, MOD-003, MOD-004, API-001, API-002, API-003, API-004 | 支付后端 canonical 契约与实现 | `mango/mango-platform/mango-payment` | API/Controller/Service/Mapper/Entity/Feign/装配/错误契约符合机器门禁，支付行为测试通过，无第二套历史实现 | SAC-002, SAC-003, SAC-004 | 不修改其它模块业务能力 |
| DEL-003 | DEC-006, DM-002, DB-002, SEC-003 | String 租户模型与最终态 V1 | payment core entity/mapper/migration/test | 旧 V3-V102 链与新 V1 的最终结构指纹一致；新库 Flyway、无运行态/敏感种子、索引/唯一约束与双租户读写满足 TC-006 | SAC-005 | 不提供旧 payment Flyway history 原地升级兼容 |
| DEL-004 | DEC-005, MOD-005, API-005, UI-001, IMP-001, IMP-002 | 支付前端与使用材料同步 | `mango-ui/packages/payment`、payment README、统一支付设计说明 | 前端唯一接口目录、主键字符串语义、包构建、支付页面入口和迁移说明一致 | SAC-004 | 不重做支付页面视觉与交互 |
| DEL-005 | DEC-007, MOD-006, ERR-003, IMP-003 | 架构规则回归、完整报告与支付预算归零 | architecture rules tests、完整报告、`debt-budget.json` | 只有已证明误判被修正；payment 1,869→0；其它模块无新增问题指纹；预算只下降 | SAC-003 | 不接受排除、降级、跨模块转移或预算增加 |
| DEL-006 | DEC-008, FLOW-006, DB-002, TC-006 | 全新数据库完整服务终验 | payment V1、workspace 数据库、后端、管理前端与支付 E2E | 工作区旧库丢弃后由 Flyway V1 初始化；服务健康；支付 suite、真实 API 与 payment-center 浏览器用例通过 | SAC-002, SAC-004, SAC-005 | 不调用真实外部支付渠道，不把演示商户凭据写入 Flyway |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-001, DEC-002 | DEL-001 | Dev/QA | payment tests 与 evidence | NONE | 盘点现有测试、接口目录、实体/表和完整架构报告；运行未改代码的现有 suite，确认测试基础设施 | 得到当前测试/架构事实清单和可执行统一入口 | VAL-001, VAL-007 | B0 基线准备 | PLANNED |
| TASK-002 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006 | DEL-001 | Dev/QA | payment api/core/starter/starter-remote tests | TASK-001 | 在不修改生产代码前补金额/状态/幂等/回调/退款/通知/对账/结算、MVC 契约、HMAC、安全、租户和持久化缺口 | 新增测试真实执行目标链路，通过测试质量与替身边界检查 | VAL-001, VAL-002, VAL-003 | B1 before | PLANNED |
| TASK-003 | DEC-003, DEC-004, MOD-001, MOD-002, ERR-001 | DEL-002 | Dev | payment api/core | TASK-002 | 迁移 PaymentCode、协议模型、`I*Service/*Service`、Require 错误契约与转换；删除 `impl` 第二套命名和历史错误入口 | API 与 Service 规则归零，TC-001/002/003/005 通过 | VAL-001, VAL-004 | B2 service/API | PLANNED |
| TASK-004 | DEC-003, DM-001, DB-001 | DEL-002 | Dev | payment core entity/mapper/XML | TASK-003 | 统一 Entity 命名/基类和 Mapper 聚合；API model 在 Service 边界转换为 core 持久化模型 | Mapper/Entity 规则归零，表/列/XML 语义与业务测试通过 | VAL-001, VAL-003, VAL-004 | B3 persistence | PLANNED |
| TASK-005 | DEC-006, DM-002, DB-002, SEC-003 | DEL-003 | Dev/QA | payment migration/entity/mapper tests | TASK-004 | 新增 V102 String 租户 migration，更新所有 tenantId 类型、条件和测试数据，在隔离 MySQL 重放并执行双租户验证 | TC-006 全部值保持与隔离断言通过 | VAL-003 | B3 persistence | PLANNED |
| TASK-006 | DEC-005, MOD-003, MOD-004, API-001, API-002, API-003, API-004, SEC-001, SEC-002 | DEL-002 | Dev/QA | payment starter/starter-remote | TASK-003, TASK-004, TASK-005 | Controller 实现唯一 Api、Feign 同签名适配、路径变量改固定路径/query/body、权限/校验/错误转换保持 | PATH/CTRL/FEIGN/ADAPTER 规则归零，TC-004/005 通过 | VAL-001, VAL-002, VAL-004 | B4 adapters | PLANNED |
| TASK-007 | MOD-005, API-005, UI-001, IMP-001, IMP-002 | DEL-004 | Dev/QA | payment frontend/docs/E2E | TASK-006 | 同步支付 API client、README、设计说明和 payment-center E2E 的批准接口映射与 String 租户测试数据 | 前端包构建、支付页面受影响 P0/P1 用例和文档检查通过 | VAL-005, VAL-006 | B5 consumers | PLANNED |
| TASK-008 | DEC-007, MOD-006, ERR-003 | DEL-005 | Dev | architecture rules tests | TASK-003, TASK-004, TASK-006 | 对仍存在的已证明检查误判先加正反例，再做最小准确性修正 | 正例仍失败、反例通过；不得降低其它规则 | VAL-004, VAL-007 | B6 gate | PLANNED |
| TASK-009 | DEC-001, IMP-003 | DEL-001, DEL-002, DEL-003, DEL-004, DEL-005 | Dev/QA | 全部任务路径 | TASK-007, TASK-008 | 运行与 before 相同 suite、完整 verify、测试质量、前端、生命周期、交付台账、架构预算检查并生成 after 比较 | 所有适用验证通过，payment 问题为 0，台账无未完成项 | VAL-001, VAL-002, VAL-003, VAL-004, VAL-005, VAL-006, VAL-007, VAL-008 | B7 final | PLANNED |
| TASK-010 | DEC-008, FLOW-006, DB-002, TC-006 | DEL-003, DEL-006 | Dev/QA | payment migrations/tests/docs 与 slot 178 完整服务 | TASK-009 | 在临时库重放旧链并固化结构参照；生成单一最终态 V1 并删除 V3-V102；移除运行态模拟数据和演示私钥；在全新工作区库用 Mango CLI 启动完整服务，执行同一 275 条 suite、API 与 payment-center 浏览器终验 | 新旧 schema 指纹一致；V1/Flyway history 正确；服务健康；测试、API、页面均无未解释失败 | VAL-003, VAL-006, VAL-009 | B8 V1/final runtime | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001, TASK-002 | 生命周期 TDD 批准 | 生产代码未变、有效测试已补强、before 基线和 1,869 问题清单已固化 | TASK-002 依赖 TASK-001 | 测试资产盘点与接口/表目录盘点可并行 | 当前代码不能执行统一 suite 或测试基础设施不真实时停止生产改造 | 支付负责人 |
| MS-002 | TASK-003, TASK-004, TASK-005, TASK-006 | MS-001 完成 | 后端四子模块 canonical 迁移完成，业务与接口定向测试通过，支付规则残留收敛到可解释的检查器问题 | 依次迁移契约→Service→persistence→adapters；租户在 persistence 后 | 同一检查点内不同聚合可机械并行，但提交前统一验证 | 业务不变量、migration 值保持或接口目录失败时在当前批次修复 | 支付负责人 |
| MS-003 | TASK-007, TASK-008 | MS-002 完成 | 支付前端/文档同步；检查器正反例通过 | TASK-007 依赖新接口；TASK-008 依赖真实残留事实 | TASK-007 与 TASK-008 可并行 | 不得为进度降低检查器或跳过 UI 入口验证 | 支付负责人 |
| MS-004 | TASK-009 | MS-003 完成 | after、完整架构、预算、前端、文档与交付门禁全部满足 | 全部任务 | NONE | 任一检查失败保持任务未完成并在当前 worktree 修复 | 支付负责人 |
| MS-005 | TASK-010 | MS-004 完成且用户批准仅支持新数据库 | V1 基线、全新库服务与最终功能验收通过 | TASK-010 依赖已有 275 条基线和 canonical 代码 | schema 对比与文档同步可并行准备，服务终验必须在 V1 完成后 | 任一 schema/API/UI 差异均恢复到当前检查点修正，不保留部分重整 | 支付负责人 |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001, TC-002, TC-003 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-006, TASK-009 | 单元/组件/模块集成 | 先执行 `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-api,mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter,mango-platform/mango-payment/mango-payment-starter-remote -am -DskipTests install` 准备依赖，再执行同一模块列表且不带 `-am` 的 `test` | 当前 worktree、JDK/Maven 锁定环境 | 用例自有 `TEST_/IT_` 数据 | 用例声明租户；纯规则明确无租户 | before/after 同一 payment-only 测试入口应通过，用例集合和业务不变量可比较；上游模块测试不混入支付基线 | `mango-docs/evidence/baselines/payment-architecture/latest/report.md` | QA | 失败定位到具体用例；不得删用例或弱化断言 |
| VAL-002 | TC-004, TC-005 | TASK-002, TASK-006, TASK-009 | API/安全/适配器 | 使用 Maven suite 定向运行 payment starter/remote/OpenAPI contract tests，并支持类级单独执行 | 当前 worktree，无外部真实支付请求 | 固定签名向量、合法/非法模型、权限用户 | 有权/无权、同租户/跨租户、过期/重放/IP | verb/path/binding/generic/validation/permission/R/error/HMAC 全部符合 TDD | 同上 | QA | 任一接口未映射或安全边界失败即阻断 |
| VAL-003 | TC-006 | TASK-002, TASK-004, TASK-005, TASK-009 | MySQL migration/Mapper 集成 | 加载 `.mango/dev-workspace.env`，确认 `MANGO_DB_NAME=mango_dev_*`，重放 payment migrations 并运行租户/Mapper integration tests | workspace slot 178 的隔离 MySQL | 两个 `IT_PAY_` 租户、相同业务键、历史 Long tenant 值 | 双租户完全隔离 | V3-V102 成功；值、记录数、索引/约束和读写结果满足 TC-006 | 同上 | QA | 数据库名不安全或任一值差异立即停止 |
| VAL-004 | TC-007 | TASK-003, TASK-004, TASK-006, TASK-008, TASK-009 | Java/Spring 架构 | 运行 architecture-rules tests；执行完整 `mvn -f mango/pom.xml verify` 生成 full-reactor report | 当前 worktree 完整 Reactor | checker fixtures 与全部 compiled classes | 不涉及业务账号 | payment moduleKey 问题为 0，其它模块无新增；报告 schema/模块归属完整 | 同上与 `target/mango-architecture-report.json` | Dev | 禁止排除、changed-only 替代完整报告或降低规则 |
| VAL-005 | TC-008 | TASK-007, TASK-009 | 前端类型/构建/契约 | `pnpm -C mango-ui --filter @mango/payment build` 并执行支付 API client 测试 | 当前 worktree Node/pnpm 锁定环境 | 固定请求输入，不写共享业务库 | ID 使用字符串语义 | package build/type/API mapping 通过 | 同上 | Dev | 修复前端契约，不恢复历史后端路由 |
| VAL-006 | TC-008 | TASK-007, TASK-009 | UI/E2E | 使用 Mango CLI 启动 slot 178 前后端，执行 `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` 受影响 P0/P1 用例，记录页面/console/network/截图 | backend 18178、frontend 30178、`mango_dev_mango_payment_architecture_debt_178` | E2E 自有前缀和清理步骤 | 专用测试账号/租户，不使用超级管理员绕过权限断言 | 页面正常/空/失败/无权限与关键业务结果正确，无未解释错误 | 同上 | QA | 服务/数据库/账号不可用标记 BLOCKED，禁止用接口 200 替代 |
| VAL-007 | TC-007 | TASK-001, TASK-008, TASK-009 | 架构预算 | `node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-payment --write` 下调后，再执行全局无 `--module` 比较 | 同一完整架构报告 | 正式 schema v4 baseline | 不涉及账号 | payment 1,869→0，总量精确下降 1,869，其他模块规则与指纹不增加 | 同上 | Dev | 模块选择不唯一、报告不完整或任何增加均阻断 |
| VAL-008 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008 | TASK-009 | PMO/交付质量 | 执行 test-quality-check、backend test double audit、workspace-layout、四文档 checker/handoff、delivery-contract-check、acceptance-evidence-check | 当前 worktree | 不额外写业务数据 | 审批证据引用当前用户授权 | 所有 checker 通过，台账状态 DONE/有依据的 EXCEPTION，未完成项为 0 | delivery ledger 与 latest baseline | Dev/QA | 失败按 checker 定位修复，禁止自报通过 |
| VAL-009 | TC-006, TC-008 | TASK-010 | 全新数据库服务/API/UI 终验 | 用本地 `@mango/cli` 执行 `pnpm exec mango dev doctor` 与 `pnpm exec mango dev start`；校验 Flyway payment history；执行健康检查、认证后支付管理 API smoke 和 payment-center Playwright Chromium 用例 | slot 178：backend 18178、frontend 30178、全新 `mango_dev_mango_payment_architecture_debt_178` | V1 正式初始化配置；E2E 自有数据并清理 | 使用 E2E 账号/租户与真实权限，不用超级管理员绕过权限断言 | 服务健康，payment 仅 V1，管理 API 与页面主路径、空态/错误态无未解释异常 | latest baseline、服务日志、Playwright report/trace | QA | Flyway、健康、认证、API、console/network 或页面断言任一失败即阻断并修复 |

## 5. 数据、升级、发布与回滚步骤

| 发布步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 兼容窗口 | 验证 | 失败停止条件 | 回滚或补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| REL-001 | DEC-006, DB-002, IMP-002 | 本地/CI/后续部署环境 | 数据库版本、V3-V101 状态、tenant_id 类型/值/索引清单与备份确认 | 随应用版本执行 V102，将 payment tenant_id 改为 VARCHAR(64) | 先备份和 migration，再启动新版应用，随后业务 smoke | 不需要业务值回填；类型转换即值保持；生产升级前必须备份 | 应用与 schema 同批，不支持旧应用继续写新版 schema | VAL-003 与启动健康/双租户 smoke | 任一 migration、值、索引或租户断言失败 | 停止应用升级并从备份恢复 schema/数据，回到上一应用版本 | 发布负责人 |
| REL-002 | IMP-001, IMP-003 | PR 与后续 Mango 平台版本 | 全部任务验证、交付台账和完整预算检查通过 | 提交任务分支、合并最新 main、复验、push 并创建 PR；本任务不执行发布 | after 验证后提交；发布进入后续统一批次 | 无额外数据回填 | Java/HTTP/前端/remote 同一版本批次直接切换 | PR required check 与后续仓库回查 | 任一门禁失败或存在未说明风险 | PR 内修复；未合并前可回滚提交，已执行数据升级按 REL-001 恢复 | 支付负责人 |
| REL-003 | DEC-008, DB-002 | 本地/CI/后续未发布环境 | 确认数据库为可丢弃的 `mango_dev_*` 新库且不存在需要保留的 payment Flyway history | 删除旧测试库后由单一 V1 初始化并启动新版应用 | 先丢弃工作区库，再启动服务；禁止在旧 V3-V102 history 上执行 | 不迁移旧数据；测试/E2E 数据由用例生成和清理 | 仅支持新库，无双轨和兼容窗口 | VAL-003、VAL-009 | 数据库不可丢弃、V1 失败或结构指纹不一致 | 停止启动，恢复代码到重整前 migration 链并重建测试库 | 发布负责人 |

## 6. 文档与能力同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001, IMP-002, DEL-002, DEL-003, DEL-004 | payment 后端/前端 README 与统一支付系统设计说明 | 更新 Java/HTTP 路由、PaymentCode import、String tenant、V102 和升级步骤 | Dev | 文档与实际代码/契约目录一致，无历史入口残留 | README 链接/diff review、前端/后端构建 | NONE |
| DOC-002 | IMP-003, DEL-001, DEL-005 | 本任务 BRD/SRS/TDD/Plan、delivery ledger、latest test baseline | 记录 before/after、测试交接、批准差异、架构 1,869→0 和验证结果 | Dev/QA | document-set、lifecycle、delivery/acceptance checker 通过 | `node mango-pmo/tools/check-document-set.mjs --root mango-docs/designs/payment-architecture-debt` | NONE |
| DOC-003 | DEL-004, TASK-007 | payment-center E2E | 同步批准接口路径与 String tenant 测试数据；保留业务语义锚点 | QA | 受影响 P0/P1 可按单条、payment 标签和文件执行 | Playwright list/test | NONE |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 级联重构出现编译通过但行为转换遗漏 | 支付业务结果变化 | before 特征测试、接口目录、检查点定向回归和最终同入口 suite | 在当前批次定位差异并修复；非批准变化不得进入下一里程碑 | Dev/QA | 2026-07-13 | CLOSED | NONE | NONE |
| RISK-002 | L3 | RISK | tenant_id 类型升级遗漏表/索引或改变查询 | 数据串租/启动失败 | 自动枚举历史 schema、隔离 MySQL 全量重放、逐表 metadata/value 对比 | 停止升级，修正 V102 与实体条件后从干净 schema 重跑 | Dev/QA | 2026-07-13 | CLOSED | NONE | NONE |
| RISK-003 | L3 | RISK | 为消除误报而放宽检查器 | 全仓架构红线失效 | 只对真实残留实施，先加正反例，完整报告比较其它模块 | 回滚检查器变化，改支付代码或收紧类型识别 | Dev | 2026-07-13 | CLOSED | NONE | NONE |
| RISK-004 | L3 | RISK | 支付 UI 入口依赖本地数据库、账号或渠道环境 | UI 验收阻塞 | 使用 workspace 独立库、现有 payment-center fixtures 与可控虚拟支付渠道 | 记录真实 BLOCKED 原因；后端/API/数据结论不冒充 UI 通过 | QA | 2026-07-13 | CLOSED | NONE | NONE |
| RISK-005 | L3 | RISK | 把 V3-V102 重整为 V1 时丢失最终列/索引/配置，或把演示私钥继续带入初始化 | 新库启动、支付配置或安全边界异常 | 以旧链临时库生成 information_schema 指纹；敏感/运行态种子扫描；全新库完整服务/API/UI 终验 | 任一差异恢复 V3-V102 后重新生成 V1，禁止人工忽略差异 | Dev/QA | 2026-07-13 | OPEN | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑发布文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, DEC-003, DEC-004, DEC-005, DEC-006, DEC-007, DEC-008, MOD-001, MOD-002, MOD-003, MOD-004, MOD-005, MOD-006, DM-001, DM-002, DM-003, FLOW-001, FLOW-002, FLOW-003, FLOW-004, FLOW-005, FLOW-006, API-001, API-002, API-003, API-004, API-005, DB-001, DB-002, SEC-001, SEC-002, SEC-003, ERR-001, ERR-002, ERR-003, UI-001, TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008, IMP-001, IMP-002, IMP-003 | DEL-001, DEL-002, DEL-003, DEL-004, DEL-005, DEL-006 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-007, TASK-008, TASK-009, TASK-010 | VAL-001, VAL-002, VAL-003, VAL-004, VAL-005, VAL-006, VAL-007, VAL-008, VAL-009 | MS-001, MS-002, MS-003, MS-004, MS-005, REL-001, REL-002, REL-003, DOC-001, DOC-002, DOC-003, RISK-001, RISK-002, RISK-003, RISK-004, RISK-005 | 所有技术设计映射到测试基线、后端/数据/前端实现、完整验证、文档、PR 与风险控制 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/payment-architecture-debt/implementation-plan.md` |
| 生命周期 handoff | PASS | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd mango-docs/designs/payment-architecture-debt/business-requirements.md --srs mango-docs/designs/payment-architecture-debt/system-requirements.md --tdd mango-docs/designs/payment-architecture-debt/technical-design.md --plan mango-docs/designs/payment-architecture-debt/implementation-plan.md --risk L3` |
| 依赖图 | PASS | TASK-001→002→003→004/005/006→007/008→009，无循环 |
| 未关闭阻断数量 | 0 | RISK-001 至 RISK-004 已有预防、应对和责任人，无开放 BLOCKER/EXCEPTION |
| 实施审批 | APPROVED | `review/PLAN-PAYMENT-DEBT.md` |
