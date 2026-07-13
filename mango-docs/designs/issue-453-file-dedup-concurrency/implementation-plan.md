---
documentId: PLAN-ISSUE-453
documentType: implementation-plan
pmoVersion: 1.1.0
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: rules/09-test-case-automation-flow.md 中并发与数据一致性变更的 L3 判定
status: APPROVED
action: NEXT
owner: Mango 文件能力负责人
approver: HardyDou
approvalEvidence: review/PLAN-ISSUE-453.md
upstreamDocumentId: TDD-ISSUE-453
upstreamDocumentHash: 52f28971dc5499c9c18f4c189bcafd858db56f859943e29aac31351af63a70a1
---

# 相同内容并发保存实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, TC-453, IMP-001 | 文件对象与哈希映射并发幂等实现、五并发集成测试、设计与验收证据 | `mango-file-core`、`mango-docs/designs/issue-453-file-dedup-concurrency`、`mango-docs/evidence/issue-453-file-dedup-concurrency` | 五并发断言、现有模块测试、质量门禁和交付台账全部满足，分支提交并创建 PR | SAC-001, TC-453 | 不处理日志字段长度、前端、公开契约、配置、表结构及 KV 实现 |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001 | DEL-001 | Dev | `mango-file-core` 文件服务实现 | NONE | 将物理对象和哈希映射创建收敛为唯一键竞争后的当前读复用，补偿清理失败方不同对象名，并保持非目标异常与公开行为不变 | 编译通过；所有保存入口复用同一幂等实现；不增加 KV、配置、公开契约和数据库变更 | VAL-001 | B1 | PLANNED |
| TASK-002 | TC-453, IMP-001 | DEL-001 | Dev 与 QA | `mango-file-core` 测试及任务证据 | TASK-001 | 增加五线程真实 Mapper 与事务集成测试，执行受影响测试和质量门禁，记录基线并准备 PR | 五次成功、一对象、一映射、五结果、引用数五、存储对象一份；全部规定检查通过且证据可复核 | VAL-001, VAL-002 | B1 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001, TASK-002 | TDD-ISSUE-453 已批准且生命周期移交通过 | 实现、五并发测试、模块门禁、证据、提交和 PR 均达到交付要求 | TASK-002 依赖 TASK-001 | NONE | 真实持久化测试不能稳定重现或质量门禁失败时停止提交，定位根因后在当前分支修复 | Mango 文件能力负责人 |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-453 | TASK-001, TASK-002 | 并发集成与模块回归 | 执行 `FileServiceConcurrentSaveIntegrationTest`，随后执行 `mango-file-core` 测试 | 当前任务 worktree；H2 MySQL 模式隔离内存库；线程安全存储替身 | `IT_453_` 前缀、相同字节、五线程、同租户与同存储配置 | 测试租户 1001、测试用户 2001，不连接共享业务库 | 五次成功、一对象、一映射、五结果、引用数五、存储对象一份；现有回归不变 | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | QA | 任一失败阻断提交；保留报告并在当前任务分支定位修复 |
| VAL-002 | TC-453 | TASK-002 | 静态与交付门禁 | 执行 test-quality-check、Mockito 审计、delivery-contract-check、受影响模块 verify、PMD、Checkstyle 和 Mango 检查 | 当前任务 worktree与本地 Maven 环境 | 不写业务数据 | 不涉及账号或共享租户 | 检查全部通过，无新增测试替身风险和未说明变更 | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | Dev | 失败即在当前任务分支修复并重新执行完整受影响验证 |

## 5. 数据、升级、发布与回滚步骤

| 发布步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 兼容窗口 | 验证 | 失败停止条件 | 回滚或补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| REL-001 | DEC-001, DB-001, IMP-001 | PR 与后续后端修复版本 | 生命周期、交付台账、测试与质量门禁通过 | 提交任务分支、推送并创建关联 Issue 453 的 PR；本任务不执行发布 | 验证后提交，合并由 PR 门禁决定，发布进入后续统一批次 | 无结构变化，不需要备份或回填 | 公开契约和数据结构保持兼容 | PR 检查与后续版本回查 | 任一门禁失败或存在未说明风险时停止合并与发布 | 回滚代码提交即可；已有数据无需处理 | Mango 文件能力负责人 |

## 6. 文档与能力同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001, DEL-001, TASK-002 | 本任务 BRD、SRS、TDD、Plan、交付台账和测试结果基线 | 记录 Issue 453 的永久并发模型、测试入口、执行结果、风险和业务开发交接 | Dev | 文档 checker、生命周期、交付台账检查通过且证据路径存在 | `node mango-pmo/tools/check-document-set.mjs --root mango-docs/designs/issue-453-file-dedup-concurrency` | README 与能力地图不变，因为使用方式、接口、配置和数据结构均未变化 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 并发测试未让五个事务同时越过首次查询，导致竞态未真实发生 | 测试可能恒真而不能证明修复 | 存储写入处使用五方线程屏障，使用真实 Mapper、唯一约束和 Spring 事务，并由测试质量检查阻断弱断言 | 检查插入冲突路径和测试报告；无法稳定触发时停止提交并调整测试同步点 | QA | 2026-07-13 | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑发布文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, TC-453, IMP-001 | DEL-001 | TASK-001, TASK-002 | VAL-001, VAL-002 | MS-001, REL-001, DOC-001, RISK-001 | 所有技术设计均映射到实现、并发测试、质量门禁、证据、PR 和风险控制 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/issue-453-file-dedup-concurrency/implementation-plan.md` |
| 生命周期 handoff | PASS | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd mango-docs/designs/issue-453-file-dedup-concurrency/business-requirements.md --srs mango-docs/designs/issue-453-file-dedup-concurrency/system-requirements.md --tdd mango-docs/designs/issue-453-file-dedup-concurrency/technical-design.md --plan mango-docs/designs/issue-453-file-dedup-concurrency/implementation-plan.md --risk L3` |
| 依赖图 | PASS | TASK-001 到 TASK-002 单向依赖，无循环 |
| 未关闭阻断数量 | 0 | RISK-001 已关闭，无 BLOCKER 或 EXCEPTION |
| 实施审批 | APPROVED | `review/PLAN-ISSUE-453.md` |
