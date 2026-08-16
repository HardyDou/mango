---
documentId: PLAN-ISSUE-805-RELEASE-CONSUMER
documentType: implementation-plan
pmoVersion: 1.3.15
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，发布 tuple 的 full 单体消费者有 10 个授权菜单失败且运行证据包含框架告警，完整发布闭包还存在三包运行时循环，阻断平台升级；solution=L3，修复跨越 npm 构建边界、CLI 模板、后端默认配置、FE1/FE2/FE3 依赖方向和真实发布消费者门禁；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Development Agent
approver: HardyDou
approvalEvidence: mango-docs/designs/issue-805-release-consumer/review/APPROVAL.md
upstreamDocumentId: TDD-ISSUE-805-RELEASE-CONSUMER
upstreamDocumentHash: dd28f4d808fb956b414d7dab7d755a62c5716fdc59b9ae17b87cda82c7434638
---

# Issue #805 发布 tuple 全模块消费者修复实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID         | 交付物                                 | 路径或模块                                          | 完成状态定义                                                      | 验收来源               | 不处理边界                      |
| -------- | ------------------ | -------------------------------------- | --------------------------------------------------- | ----------------------------------------------------------------- | ---------------------- | ------------------------------- |
| DEL-001  | DEC-001、MOD-001   | Admin/CMS 唯一页面注册表修复与制品合同 | `mango-ui/packages/admin`、`release-contracts.json` | packed Admin full 保留 CMS external import，真实 CMS 9/9 页面可见 | TC-001、TC-005         | 不改 CMS page key/API/权限      |
| DEL-002  | DEC-002、MOD-002   | full 事件 outbox 默认配置              | CLI full `application.yml`                          | 新项目无附加参数时 `/system/events` 可用                          | TC-002、TC-005         | 不改变非 full preset 与事件 API |
| DEL-003  | DEC-003、MOD-002   | tuple README 与 favicon                | CLI README/template/测试                            | 版本均为当前 lock，favicon 200                                    | TC-003、TC-005         | 不预先决定发布版本              |
| DEL-004  | DEC-004、MOD-003   | 已确认 Element Plus 告警修复           | common/link/rbac/system                             | 既有页面行为不变且目标 warning 为 0                               | TC-004、TC-005         | 不做全仓 Element Plus 3 迁移    |
| DEL-005  | DEC-003、MOD-004   | 发布物消费者防回归                     | CLI/package/release tests                           | candidate 与 pure-registry consumer 阻断本次失效模式              | TC-001、TC-003、TC-005 | 不在本任务执行远端发布          |
| DEL-006  | IMP-001 至 IMP-003 | 能力说明与验收记录                     | 模块 README、能力地图、`.runtime`                   | 文档与实现一致，M08-M14 证据完整                                  | 全部 TC                | 不复制 PMO 长期规则             |
| DEL-007  | DEC-005、MOD-005   | 管理端扩展契约拆环                     | `admin-extension/admin-pages/file/system`           | 三包 SCC 清零，旧 subpath 兼容，完整发布闭包可拓扑排序             | TC-006                 | 不实现发布质量预防治理 Issue    |

## 2. 工作分解

| 任务ID   | 技术设计ID         | 交付物ID           | 责任角色 | 路径或模块                     | 前置任务             | 具体动作                                                                | 完成标准                          | 验证ID  | 实施批次 | 状态    |
| -------- | ------------------ | ------------------ | -------- | ------------------------------ | -------------------- | ----------------------------------------------------------------------- | --------------------------------- | ------- | -------- | ------- |
| TASK-001 | DEC-001            | DEL-001            | Dev      | Admin/release contract         | NONE                 | 补 external，并为 packed full import 建制品断言                         | 不再内联 CMS registrar            | VAL-001 | B1       | PLANNED |
| TASK-002 | DEC-002、DEC-003   | DEL-002、DEL-003   | Dev      | CLI full template/README/tests | NONE                 | 启用 event outbox，修正 PMO tuple，补 favicon fallback 与模板测试       | 生成合同精确通过                  | VAL-002 | B1       | PLANNED |
| TASK-003 | DEC-004            | DEL-004            | Dev      | common/link/rbac/system        | NONE                 | 在 API 边界归一化数字，迁移已确认 deprecated props                      | 类型检查、组件测试和 console 通过 | VAL-003 | B1       | PLANNED |
| TASK-004 | DEC-003            | DEL-005            | Dev      | consumer/release scripts       | TASK-001、TASK-002、TASK-007 | 将制品和生成模板断言接入现有候选/纯仓入口                          | 可注入负例且 fail closed          | VAL-004 | B2       | PLANNED |
| TASK-005 | IMP-001 至 IMP-004 | DEL-006            | Dev      | capability docs/evidence       | TASK-001 至 TASK-004、TASK-007 | 更新当前用法、升级影响、验证入口和验收结果                         | README audit 与文档 checker 通过  | VAL-005 | B2       | PLANNED |
| TASK-006 | FLOW-003           | DEL-001 至 DEL-006 | QA       | fresh consumer runtime         | TASK-005             | 从新缓存生成 full monolith，启动、登录、API、CRUD、授权叶子菜单逐页验收 | 无源码补偿、无附加参数、100% 通过 | VAL-006 | B3       | PLANNED |
| TASK-007 | DEC-005、FLOW-004  | DEL-007            | Dev      | admin-extension/admin-pages/file/system | NONE | 抽出 FE1 扩展契约，迁移 file registrar，保留旧 subpath re-export，并删除已清零 SCC 基线 | manifest/source/combined SCC 为 0，发布顺序稳定 | VAL-007 | B1 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID           | 进入条件            | 完成条件                           | 依赖   | 可并行任务                 | 阻塞升级                             | 责任人 |
| -------- | -------------------- | ------------------- | ---------------------------------- | ------ | -------------------------- | ------------------------------------ | ------ |
| MS-001   | TASK-001 至 TASK-003、TASK-007 | 设计批准            | 消费缺陷与发布拓扑根因均有定向测试 | NONE   | TASK-001/TASK-002/TASK-003/TASK-007 | 新事实超出 #805 时先归因，不顺手扩大 | Dev    |
| MS-002   | TASK-004、TASK-005   | MS-001 完成         | 长期门禁和能力说明同步             | MS-001 | NONE                       | 门禁无法观察制品行为时升级设计       | Dev    |
| MS-003   | TASK-006             | 所有本地 gates 通过 | fresh consumer 达成 Issue 完成标准 | MS-002 | NONE                       | 真实消费失败则回到对应任务修复       | Dev/QA |

## 4. 验证计划

| 验证ID  | 测试或验收ID                           | 任务ID   | 验证层级        | 命令或步骤                                                                                                 | 环境                                       | 测试数据               | 权限或租户边界       | 预期结果                                         | 证据路径                                | 责任人 | 失败处理                 |
| ------- | -------------------------------------- | -------- | --------------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------ | ---------------------- | -------------------- | ------------------------------------------------ | --------------------------------------- | ------ | ------------------------ |
| VAL-001 | TC-001                                 | TASK-001 | M09/M11         | Admin build、package contracts、isolated consumer build                                                    | 本 worktree + fresh store                  | sealed tarballs        | 无新增权限           | external import 与唯一 registry                  | `.runtime/issue-805/admin-consumer/`    | Dev    | 阻断 B2                  |
| VAL-002 | TC-002、TC-003                         | TASK-002 | M10/M11/M12     | CLI tests、packed init、启动后 GET events/favicon                                                          | fresh generated project/MySQL              | 空库                   | 管理员正例、匿名 401 | 版本一致，API/favicon 正常                       | `.runtime/issue-805/cli-full/`          | Dev    | 阻断 B2                  |
| VAL-003 | TC-004                                 | TASK-003 | M09/M10/M13     | package tests/typecheck；打开目标页面并采集 console                                                        | Chromium + real backend                    | 当前管理员授权数据     | 既有权限             | 0 目标 warning                                   | `.runtime/issue-805/frontend-warnings/` | Dev/QA | 回修来源包               |
| VAL-004 | TC-001、TC-003                         | TASK-004 | M10/M11         | release tests、candidate package consumer，执行故障注入负例                                                | isolated temp consumer                     | candidate tarballs     | 不适用               | 正例通过、分裂/漂移负例失败                      | `.runtime/issue-805/release-gate/`      | Dev    | 禁止宣称门禁完成         |
| VAL-005 | TC-001、TC-002、TC-003、TC-004、TC-005 | TASK-005 | M08/M09         | README audits、document set、lifecycle handoff、workspace layout                                           | worktree                                   | 不适用                 | 不适用               | 全部 PASS                                        | `.runtime/issue-805/docs/`              | Dev    | 修正文档/合同            |
| VAL-006 | TC-005                                 | TASK-006 | M11/M12/M13/M14 | fresh npm/pnpm/Maven caches，full monolith 生成/构建/空库启动/登录/CRUD/全部授权叶子菜单巡检，再做独立复核 | 内部 consume registries + MySQL + Chromium | 新库、唯一业务数据前缀 | 管理员+匿名负例      | 100% 菜单，0 404/业务错误/pageerror/框架 warning | `.runtime/issue-805/fresh-consumer/`    | Dev/QA | 任一失败回到根因，不发布 |
| VAL-007 | TC-006                                 | TASK-007 | M09/M10/M11     | architecture check、admin-extension/admin-pages/file build+test、release scope/plan 单测、完整闭包拓扑重算       | 本 worktree + packed tarballs             | 当前 package graph    | 不适用               | 0 SCC、兼容 subpath 同实例、确定性发布顺序         | `.runtime/issue-805/release-graph/`      | Dev    | 任一失败阻断 B2         |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境                     | 前置检查                                | 动作                                 | 顺序       | 数据备份或回填             | 验证                       | 失败停止条件                  | 补偿                   | 责任人 |
| ---------- | ---------- | ------------------------ | --------------------------------------- | ------------------------------------ | ---------- | -------------------------- | -------------------------- | ----------------------------- | ---------------------- | ------ |
| DATA-001   | DB-001     | 一次性 fresh consumer DB | 数据库名匹配 `mango_dev_*` 且无存量数据 | CLI 创建并由 Flyway/Bootstrap 初始化 | VAL-006 前 | 无需备份，不连接现有业务库 | bootstrap/runtime/菜单/API | 任一 migration/bootstrap 失败 | 受控删除该一次性数据库 | Dev/QA |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档                                                  | 变化                                                     | 责任人 | 完成条件           | 检查命令                  | 不适用依据 |
| -------- | ------------------ | --------------------------------------------------------- | -------------------------------------------------------- | ------ | ------------------ | ------------------------- | ---------- |
| DOC-001  | DEL-001            | Admin/CMS/Admin Pages README、能力地图                    | 说明 full registrar external/唯一注册表与排障            | Dev    | 与 packed 行为一致 | README audits             | NONE       |
| DOC-002  | DEL-002、DEL-003   | CLI/Event/Business Starter README 与 full template README | 说明 full 默认事件能力、精确 PMO tuple、favicon/验收入口 | Dev    | 生成后说明一致     | README audits + CLI tests | NONE       |
| DOC-003  | DEL-004            | Common/Link/RBAC/System README                            | 记录兼容性修复，公开 API/菜单/权限不变                   | Dev    | 变更影响明确       | README audits             | NONE       |
| DOC-004  | DEL-005            | release consumer 使用说明/能力地图                        | 说明候选合同与 fresh runtime 分层门禁                    | Dev    | 长期入口可执行     | release tests             | NONE       |
| DOC-005  | DEL-007            | Admin Extension/Admin Pages/File README 与能力地图         | 说明 FE1 扩展契约、兼容 subpath 和依赖方向               | Dev    | 与 package exports 一致 | README audits + architecture | NONE       |

## 7. 风险、阻塞与例外

| 风险ID   | 风险等级 | 类型 | 触发条件                               | 影响                  | 预防                                 | 应对                         | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
| -------- | -------- | ---- | -------------------------------------- | --------------------- | ------------------------------------ | ---------------------------- | ------ | -------- | ---- | ---------- | -------------- |
| RISK-001 | L3       | RISK | packed 合同通过但真实运行仍分裂        | 发布后 CMS 404        | 同时保留 fresh runtime 全菜单验收    | 真实结果失败即阻断           | Dev/QA | 交付前   | OPEN | NONE       | NONE           |
| RISK-002 | L2       | RISK | event outbox 默认启用增加后台组件      | full 项目资源使用变化 | 限定 full preset并复用已有 KV outbox | 启动/空库/重启验证，文档说明 | Dev    | 交付前   | OPEN | NONE       | NONE           |
| RISK-003 | L2       | RISK | 修复公共 Pagination 导致消费者兼容变化 | 页面分页显示异常      | 保留 `small` prop，仅映射到 `size`   | 组件和真实页面回归           | Dev    | 交付前   | OPEN | NONE       | NONE           |
| RISK-004 | L3       | RISK | 新扩展包或兼容 re-export 解析成两个 registry 实例 | 页面注册再次分裂或发布闭包不可解 | admin-pages 只从 admin-extension re-export，仓内 FE2 直接依赖新包 | packed identity/architecture/release plan 失败即回修 | Dev | 交付前 | OPEN | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID                                                                                                                                                                                   | 交付物ID           | 任务ID               | 验证ID                    | 里程碑数据文档或风险项ID                       | 覆盖说明                                                                     |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | -------------------- | ------------------------- | ---------------------------------------------- | ---------------------------------------------------------------------------- |
| DEC-001                                                                                                                                                                                      | DEL-001            | TASK-001             | VAL-001、VAL-006          | MS-001、RISK-001、DOC-001                      | CMS 制品和运行覆盖                                                           |
| DEC-002                                                                                                                                                                                      | DEL-002            | TASK-002             | VAL-002、VAL-006          | MS-001、DATA-001、RISK-002、DOC-002            | 配置/API/空库运行覆盖                                                        |
| DEC-003                                                                                                                                                                                      | DEL-003、DEL-005   | TASK-002、TASK-004   | VAL-002、VAL-004、VAL-006 | MS-002、DOC-002、DOC-004                       | 生成、候选、纯消费覆盖                                                       |
| DEC-004                                                                                                                                                                                      | DEL-004            | TASK-003             | VAL-003、VAL-006          | MS-001、RISK-003、DOC-003                      | 定向与全菜单 warning 覆盖                                                    |
| IMP-001 至 IMP-003                                                                                                                                                                           | DEL-006            | TASK-005             | VAL-005                   | MS-002、DOC-001 至 DOC-004                     | M08 与交付资产覆盖                                                           |
| FLOW-003                                                                                                                                                                                     | DEL-001 至 DEL-006 | TASK-006             | VAL-006                   | MS-003、DATA-001、RISK-001 至 RISK-003         | fresh consumer 最终覆盖                                                      |
| DEC-005、MOD-005、FLOW-004、ERR-004、IMP-004、TC-006                                                                                                                                        | DEL-007            | TASK-007             | VAL-007                   | MS-001、RISK-004、DOC-005                     | 发布依赖拆环与兼容覆盖                                                       |
| MOD-001、MOD-002、MOD-003、MOD-004、MOD-005、DM-001、DM-002、FLOW-001、FLOW-002、FLOW-004、API-001、DB-001、SEC-001、ERR-001、ERR-002、ERR-003、ERR-004、UI-001、UI-002、TC-001、TC-002、TC-003、TC-004、TC-005、TC-006、IMP-002、IMP-004 | DEL-001 至 DEL-007 | TASK-001 至 TASK-007 | VAL-001 至 VAL-007        | MS-001 至 MS-003、DATA-001、DOC-001 至 DOC-005 | 承接 TDD 的模块、模型、流程、API、数据、安全、异常、页面、测试和能力说明设计 |

## 9. 阶段判定与审批

| 检查项           | 结果     | 证据                                                               |
| ---------------- | -------- | ------------------------------------------------------------------ |
| 实施计划 checker | PASS     | 按 PMO 1.3.15 implementation-plan schema 编写，实施前执行 checker  |
| 生命周期 handoff | PASS     | 上游 TDD SHA-256 已锁定，范围与风险一致                            |
| 依赖图           | PASS     | B1 根因修复 -> B2 门禁/文档 -> B3 fresh consumer                   |
| 未关闭阻断数量   | 0        | 风险均有阻断条件与回修路径                                         |
| 实施审批         | APPROVED | `mango-docs/designs/issue-805-release-consumer/review/APPROVAL.md` |
