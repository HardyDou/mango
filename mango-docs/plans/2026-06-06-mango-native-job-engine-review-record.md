# Mango 原生 Job Engine 多轮同行评审记录

## 1. 评审目标

评审 `mango-docs/designs/mango-native-job-engine-design.md` 是否可以作为 Mango Job 后续研发基线。

评审问题：

- 最终特性目标是否覆盖用户确认的 Mango 特性。
- 是否彻底解决 PowerJob Adapter 双事实源问题。
- 原生调度、实例、Worker、日志、租约和部署设计是否可落地。
- 前后端统一 UI、权限、租户、告警和验收是否闭环。

## 2. 评审输入

- `mango-docs/designs/mango-native-job-engine-design.md`
- `mango-docs/designs/mango-job-design.md`
- `mango-docs/plans/2026-06-05-mango-job-development-plan.md`
- `mango-docs/plans/2026-06-05-mango-job-review-record.md`
- PowerJob 官方 README、Release 页面、Maven Central 查询结果。
- 用户在本轮会话中确认的目标和约束。

## 3. 评审角色

| 轮次 | 角色 | 关注点 |
|---|---|---|
| Round 1 | 调度内核架构专家 | 调度游标、状态机、attempt、租约、fencing token、多实例一致性。 |
| Round 2 | 后端平台专家 | 模块边界、API、MyBatis-Plus、独立 `mango_job` 数据库、事务和幂等。 |
| Round 3 | 产品/前端专家 | 菜单、页面模型、用户可理解性、结构化参数、日志入口、权限。 |
| Round 4 | SRE/QA 专家 | 部署模式、Worker 运维、告警、E2E、压测、回滚。 |

## 4. Round 1：调度内核架构评审

结论：通过，带强制修订项。

核心意见：

- 当前旧 PowerJob Adapter 设计不能局部修补，必须整体替换为 Mango JobCenter 原生调度、原生实例、原生 Worker、原生日志。
- 调度内核必须由 Mango 持有，任务扫描、触发计算、实例创建、分发、超时、重试、补偿、取消、恢复都落在 `mango_job`。
- 必须明确至少一次执行语义，禁止承诺严格 exactly-once。
- 必须引入 attempt、任务租约、租约续期、租约过期回收和 fencing token。
- Worker 在线必须来自真实心跳，不能从配置或第三方表推断。
- 日志必须原生采集和索引，不能依赖第三方引擎内部表。

已修订：

- 新设计明确替代旧 PowerJob Adapter 路线。
- 增加 `mango_job_schedule_cursor`。
- 增加 `mango_job_attempt`。
- 增加 `lease_until`、`lease_owner`、`fencing_token`。
- 增加 Worker capability。
- 增加日志 chunk/index。
- 增加至少一次执行和业务幂等约束。

## 5. Round 2：后端平台评审

结论：通过，后续 Sprint 进入编码前还需补 API 字段级契约。

核心意见：

- `mango-job-api` 只能放 Command、Query、VO、枚举、handler 契约和 context/result，不能引入 core、starter、数据库实体或第三方引擎类型。
- `mango-job-core` 持有原生领域服务和 Mapper，不能再围绕 PowerJob 映射表组织业务服务。
- DDL 必须按 `db/migration/mango-job` 管理，模块默认数据源为 `job`，物理库名为 `mango_job`。
- 调度扫描和状态迁移必须依赖数据库唯一约束、行锁或 CAS，不能只靠 JVM 锁。
- 所有 API 入参和返回不能暴露 Entity。
- TenantId 应由 Mango 租户链路自动注入和过滤，Job 设计中不要求业务代码显式传租户。

已修订：

- 设计中明确 `mango-job-api/core/starter/worker-starter` 边界。
- 数据模型全部放入原生 `mango_job`。
- API 只暴露 Mango 语义。
- 状态一致性依赖数据库约束、租约和 token。

保留待办：

- Sprint A 需要补充字段级 Command、Query、VO 设计。
- TenantId 自动注入属于已登记的专项问题，Job 实现必须接入统一租户能力，不能在业务处理器中散写。

## 6. Round 3：产品/前端评审

结论：通过，页面模型必须按用户可理解对象重做。

核心意见：

- 菜单应为 `平台能力 -> 任务管理`，不再出现“处理器”这种用户不可理解菜单。
- “执行日志”不应作为独立列表菜单，日志应跟随执行实例。
- 执行实例列表必须优先展示任务名称、任务编码、状态、触发来源、频次、Worker、耗时、结果摘要，不应堆 ID。
- 搜索项中的任务必须使用任务名称下拉或远程搜索。
- 参数配置必须默认结构化表单，高级 JSON 只能作为排障入口。
- Worker 页面必须展示真实 Worker 节点、应用、地址、版本、容量、负载、心跳、正在执行数、失败数和支持 handler，不能显示 `N/A`。
- “引擎状态”不能再指向第三方引擎，应改为 Mango 运行状态，展示 JobCenter、数据库 lease、积压、失败率、超时数和 Worker 总览。
- 租户上下文传播必须在设计中说明，定时任务没有 Web 请求，不能依赖请求 Header。

已修订：

- 设计菜单调整为任务定义、执行实例、Worker 节点、运行状态、告警规则。
- 日志作为执行实例详情和行内日志按钮。
- 参数 schema 和结构化表单成为目标能力。
- Worker 手动添加、禁用、排空、下线写入目标能力。
- 运行状态页面替代旧“引擎状态”页面。
- 补充定时任务租户上下文生成和 Worker 执行前上下文恢复。

## 7. Round 4：SRE/QA 评审

结论：通过，验收必须覆盖真实调度闭环。

核心意见：

- 单体内嵌、单体多实例、独立 JobCenter + 远程 Worker 都必须有验证场景。
- 单体模式也必须走同一套数据库事实、租约和状态机，不能因同进程绕过核心链路。
- 每 1 分钟 CRON 任务必须观察至少 3 个调度窗口。
- 日志验收必须同时覆盖 `System.out`、`System.err`、logger 和处理器返回结果。
- Worker 失联、租约过期、attempt 重试必须有集成测试。
- 告警只验证 Job 产生通知事件和调用 `mango-notice`，不重复验证各通信平台。
- 告警需要覆盖连续失败、恢复通知、同任务聚合和静默窗口。
- 远程 Worker 注册、心跳、拉取任务、上报结果必须验证服务身份认证。
- `mango_job` 独立库必须验证可迁移、可备份、可清理，主库不保存 Job 治理表。
- 单体内嵌模式必须使用内存通信，不能通过 HTTP、本机回环地址或独立 Worker 端口绕行。
- 微服务模式必须支持 Worker 通过独立端口与 JobCenter 进行 HTTP 内部通信。
- PowerJob 通信协议只作为参考：其远程通信层主要支持 `AKKA` 和 `HTTP`，Mango 首轮不采用 Akka，也不采用 UDP。

已修订：

- 新设计增加 12 个 E2E 验收场景。
- 新设计增加 Sprint B/C/D/E/F，分别覆盖内嵌、多实例、远程 Worker、统一 UI 和生产验收。
- 新设计明确 `IN_MEMORY` transport 不能绕过状态机。
- 新设计补充告警降噪、远程 Worker 内部调用安全和独立库验收。
- 新设计补充单体内存通信和微服务独立端口 HTTP 通信。

## 8. 总体结论

评审结论：通过，建议提交用户审核。

通过条件：

- 后续研发以 `mango-docs/designs/mango-native-job-engine-design.md` 为唯一 Job 新方案设计输入。
- 旧 PowerJob Adapter 设计仅作为历史记录，不再指导实现。
- 编码前必须按 Sprint A 先建立原生模型、状态机和旧方案下线策略。
- 不允许跳过 attempt、租约、fencing token、原生日志和真实 Worker 心跳。

## 9. 必须进入研发的验收红线

- 至少一次执行语义明确，业务幂等键可见。
- 多 JobCenter 不重复创建同一调度窗口实例。
- Worker 在线来自真实心跳。
- Worker 失联后 attempt 可回收。
- 日志能在执行实例中看到 `System.out`、`System.err`、logger 和返回结果。
- 结构化参数表单可创建、编辑、触发并回显。
- 单体内嵌和远程 Worker 不要求修改任务定义。
- 单体内嵌验收必须证明未开放 Worker 独立端口，调度执行通过 `IN_MEMORY` 完成。
- 微服务验收必须证明 Worker 通过独立端口和 `HTTP_INTERNAL` 完成注册、心跳、任务获取、日志和结果上报。
- 权限和租户隔离真实生效。
- UI 不再出现 PowerJob、引擎状态、处理器菜单和独立执行日志菜单。
- 全链路验收不得依赖 PowerJob Server、PowerJob 表或 PowerJob Worker。

## 10. 未关闭风险

- 当前分支已有 PowerJob Adapter 相关代码、DDL、E2E 和截图，后续迁移需要明确删除或隔离范围。
- Maven Central 与 GitHub Release 对 PowerJob 最新版本存在差异，这进一步说明不能绑定 PowerJob 作为运行时前提。
- `IN_MEMORY` transport 捕获 `System.out/System.err` 存在线程隔离风险，Sprint A/B 需要技术 spike。
- 远程 Worker 内部调用安全需要和 Mango 现有安全组件对齐。

## 11. 评审状态

状态：`AI_MULTI_ROUND_REVIEW_PASSED_USER_APPROVAL_PENDING`。

下一步：提交用户审核。用户审核通过后，才能进入 Sprint A 编码。
