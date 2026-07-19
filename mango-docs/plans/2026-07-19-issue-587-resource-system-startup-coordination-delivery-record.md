# Issue #587 Resource/System 启动协调交付记录

## 1. 元数据

- 任务 ID：ISSUE-587-RESOURCE-SYSTEM-STARTUP
- 交付模式：FULL（涉及机构权限基线和跨模块启动协作；本记录承载实现与验证证据，Issue 正文承载已确认的问题、范围和验收目标，不伪造 BRD/SRS/TDD 人工审批）
- 需求影响：L3 - 新库首次启动可能在资源不完整时提前完成机构资源、角色和权限基线对账。
- 方案风险：L3 - 调整 Resource 与 System 的启动状态、事件、重试和幂等协作，错误实现可能造成基线缺失或重复执行。
- 最终风险：L3
- 工作区决策：REUSE（worktree `/Users/hardy/Work/mango-business-impact-issues-587`，分支 `fix/business-impact-issues-587`）
- 适用措施：M01、M08、M09、M10、M11、M14。
- 非降级事实：TENANT、PERMISSION、跨模块启动协调。

## 2. 事实、目标与范围

- Issue：[HardyDou/mango#587](https://github.com/HardyDou/mango/issues/587)。
- 改前事实：`ResourceSyncRunner` 首次同步失败后正常结束启动 Runner，仅由定时任务重试；`TenantProvisioningReconciliationRunner` 仍按数字顺序执行一次，无法知道资源是否真正同步完成。
- 目标：资源同步被延后时不得提前标记机构基线最终完成；声明依赖内置机构角色时先建立幂等前置基线并立即重试资源；资源成功后自动触发一次最终幂等对账；对账自身瞬时失败时可以继续重试。
- 成功条件：初次失败明确保持未完成状态；前置基线不设置最终完成态；重试成功发布单次完成事件；事件与定时重试并发时最终对账只成功完成一次；已有无 Resource Sync 的部署保持原启动行为。
- 不处理范围：Resource HTTP 契约、资源注册算法、机构/角色/权限业务模型、数据库结构、菜单、前端、部署拓扑和发布版本。

## 3. 可观察系统要求

| ID | 场景或入口 | 前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 应用首次启动 | 资源同步成功 | 机构基线对账按既有 Runner 顺序执行一次 | 对账失败保持既有启动失败语义 | 状态为完成，对账执行一次 |
| SR-002 | 应用首次启动 | 资源同步失败并转入重试 | 先重放幂等前置基线，但不得把机构最终对账标记为完成 | 前置基线失败保持既有启动失败语义 | 前置调用 1 次，资源和最终对账状态仍未完成 |
| SR-003 | Resource 定时重试 | 之前未完成，本次首次成功 | 状态原子切换为完成并发布一次完成事件 | 监听器失败不得把已成功的资源同步反转为失败 | 多次调度只发布一个完成事件 |
| SR-004 | System 完成事件监听 | 资源同步已完成 | 自动执行全部启用机构的幂等基线对账 | 瞬时失败记录错误并保持未完成，等待独立重试 | 后续重试成功，重复事件或调度不重复完成 |
| SR-005 | 不启用 Resource Sync 的部署 | 容器中没有同步状态 Bean | 保持 1.0.22 以前的 System 启动对账行为 | 不因可选能力缺失阻断启动 | 对账直接执行一次 |
| SR-006 | 资源声明依赖机构内置角色 | 初次同步因 `ROLE_ADMIN` 不存在而延后 | System 前置基线完成后通知 Resource 立即重试，成功后同步执行最终对账 | 资源仍失败时保持未完成并由既有调度继续重试 | 同一启动 Runner 链中形成“前置基线 -> 资源完成 -> 最终对账”顺序 |

## 4. 架构与技术决定

| ID | 对应要求 | 决定 | 理由与竞态处理 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001/SR-002 | 在 `mango-resource-support` 定义只读 `ResourceSynchronizationStatus`，由 `ResourceSyncRunner` 实现 | System 依赖稳定 support 契约，不依赖 sync starter 实现；数字 Runner 顺序不再冒充成功条件 | 删除状态契约并恢复原 Runner 行为 |
| TD-002 | SR-003 | 定义进程内 `ResourceSynchronizationCompletedEvent`；仅从未完成到完成的首次重试成功发布 | 初次成功由后续 System Runner 读取状态；延后成功需要事件唤醒；原子 CAS 防止重复发布 | 删除事件与发布逻辑 |
| TD-003 | SR-003 | 事件监听异常与资源同步结果隔离 | Resource Registry 已成功时不得因下游对账失败被错误标为未完成；下游拥有独立恢复责任 | 恢复同步调用内传播，但会重新引入状态混淆 |
| TD-004 | SR-002/SR-004 | System 同时使用完成状态、完成事件和独立定时重试；两个原子状态阻止并发和重复完成 | 覆盖“先事件后 Runner”“Runner 检查期间事件到达”“事件与调度并发”三类时序 | 回退新增协调逻辑 |
| TD-005 | SR-005 | `ObjectProvider` 没有状态 Bean 时视为完成 | 保持未启用 Resource Sync 的单体和测试部署兼容 | 无需数据回滚 |
| TD-006 | SR-002/SR-006 | 定义 `ResourceSynchronizationPrerequisitesReadyEvent`；System 前置基线后发布，Resource 同步监听并立即重试 | 真实空库证明资源声明可能依赖 System 创建的内置角色；只等待 Resource 会形成循环依赖，异步调度又晚于业务 Runner | 删除前置事件并恢复仅等待完成事件，但会恢复空库启动循环 |

## 5. 接口、数据、权限与兼容边界

- Java 支持契约新增 `ResourceSynchronizationStatus`、`ResourceSynchronizationPrerequisitesReadyEvent` 与 `ResourceSynchronizationCompletedEvent`；无 HTTP API、DTO 或 Controller 变化。
- 无数据库结构、Flyway、初始化数据或历史数据变化。
- 无菜单、页面、权限码、租户隔离和机构业务规则变化；只修正机构权限基线开始执行的前置条件。
- 新增可选配置 `mango.system.tenant-provisioning.retry-interval`，默认 `10s`；Resource 继续沿用 `mango.resource.registry.remote.retry-interval`。
- 依赖方向保持 `mango-system-core -> mango-resource-support`；System 不依赖 `mango-resource-sync-starter`。

## 6. 实施清单

| ID | 对应决定 | 改动路径 | 完成条件 | 状态 |
|---|---|---|---|---|
| IMPL-001 | TD-001/TD-002/TD-003/TD-006 | `mango-resource-support`、`mango-resource-sync-starter` | 状态、前置就绪事件、单次完成事件和异常隔离实现并测试 | DONE |
| IMPL-002 | TD-004/TD-005/TD-006 | `mango-system-core` | 前置基线不标记完成，资源成功后最终对账，对账失败可重试，无状态 Bean 时兼容 | DONE |
| IMPL-003 | 全部 | Resource/System 测试 | 初次失败、重试成功、单次事件、单次对账和瞬时失败重试有自动断言 | DONE |
| IMPL-004 | 全部 | Resource/System README、能力地图、租户空数据排障指南和本记录 | 接入、配置、启动排障、验证和剩余风险可回读 | DONE |

## 7. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 |
|---|---|---|---|
| SR-001 至 SR-005 | 三模块同 Reactor 测试与生命周期验证 | `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-support,mango-platform/mango-resource/mango-resource-sync-starter,mango-platform/mango-system/mango-system-core -DskipITs verify` | PASS：support 8、sync starter 4、system core 60，共 72；失败 0、错误 0、跳过 0 |
| SR-002/SR-003/SR-006 | `ResourceSyncRunnerTest` | 首次调用失败，前置就绪事件立即重试，后续重试成功，再重复调度 | PASS：失败后状态为未完成，成功后状态为完成，只发布一个完成事件 |
| SR-002/SR-004/SR-005/SR-006 | `TenantProvisioningReconciliationRunnerTest` | 完成状态、前置基线、延后事件、瞬时失败重试和无状态兼容 | PASS：未完成时前置调用 1 次且不标记最终完成；资源完成后最终调用 1 次；瞬时失败后重试收敛 |
| SR-001 至 SR-006 | Baohan 1.0.22 真实消费项目空库 | 从业务 `main` 合并提交 `d2b4c5319` 建立隔离 worktree，关闭业务补偿 Runner，使用隔离 Maven 仓库加载本修复，CLI slot 9 删除并重建数据库后执行 `mango dev start backend` | PASS：健康持续 `UP`；日志严格出现一次“资源因 `ROLE_ADMIN` 延后 -> 前置基线完成 -> 1964 条资源同步完成 -> 最终对账完成”；246 表、1964 资源、981 API 资源、15 角色、56 角色数据范围、12 业务域、5 工作流定义 |
| 全部 | Mango 架构与 Java 静态质量门禁 | 受影响三个模块与 `mango-architecture-verification` 执行 changed 模式 `verify`，静态 gate 为 `no-new-violations` | PASS：dependency 0、PMD 0、blocking 0；静态问题 0、新增问题 0、工具失败 0；存在 1 个与本次无关的既有非阻断 Controller 根路径 ArchUnit 记录 |
| 全部 | 测试资产和 Mock 门禁 | `test-quality-check.mjs --base origin/main`；`audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS：2 个变更测试文件；Mock block=0、warn=0 |
| 全部 | README、源码事实、业务指南、能力说明和补丁检查 | `audit-module-readmes.mjs`；`audit-readme-source-facts.mjs`；`check-business-guides.mjs`；`check-capability-docs.mjs`；`git diff --check` | PASS |

## 8. 发布、回滚与业务交接

- 本任务不在当前工作区执行版本发布；合并后进入下一次 Maven 版本发布。
- 业务项目升级到包含本修复的版本后，可删除等待资源可见并手工二次执行机构对账的临时补偿。
- 回滚可整体回退状态、事件和 System 协调逻辑；没有数据库或数据回滚步骤。
- 真实空库的资源数、角色数和权限绑定数取决于业务声明；本次已在 Baohan 最新合并态关闭消费方补偿后验证既有数量基线。其它业务项目升级时仍应回读自己的声明基线。

## 9. 剩余风险

- 完成状态和错误日志提供进程内 fail-closed 判断与审计线索，但本次没有新增 Actuator HealthIndicator；长期同步失败仍通过既有 Resource 重试告警暴露。
- 其它业务项目如果存在非机构角色类的额外资源前置条件，仍需在升级验收中确认其幂等前置基线可以由同一机制建立；长期同步失败不标记最终对账完成。
