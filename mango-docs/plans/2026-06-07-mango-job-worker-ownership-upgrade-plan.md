# Mango Job Worker 归属升级计划

## 1. 目标

解决 Mango Job 在多服务、多实例、内嵌 Worker 和远程 Worker 混合部署下的任务归属问题，确保调度系统明确知道“哪个任务只能交给哪些 Worker 执行”。

目标结论：

- 任务定义必须声明执行归属。
- Worker 必须自动或手动注册真实执行能力。
- JobCenter 派发任务前必须按租户、应用、Worker 分组、处理器和能力状态过滤 Worker。
- Worker 收到任务后必须二次校验本进程是否真实拥有对应 handler。
- 内嵌 Worker 不允许手动添加，只能由系统根据当前进程真实 handler 自动注册。
- 单体多实例时，同一服务的多个实例应显示多个真实 `IN_MEMORY` Worker；同一实例不能重复显示。
- A 服务的 `aJob` 不能派发给 B 服务 Worker；配置错误时必须拒绝并记录失败原因，不能猜测执行。

## 2. 当前差距

当前已具备：

- 远程 Worker 注册时上报 `appCode`、`workerAddress`、`transportType` 和 handler 清单。
- 远程 Worker 派发时按 `tenantId + appCode + handlerName + ONLINE` 查询能力。
- Worker 执行前通过 `handlerRegistry.findHandler(appCode, handlerName)` 查找本进程 handler，找不到会拒绝。
- Worker 心跳注册按租户、应用、运行时和地址幂等更新。

当前缺口：

- Job 定义只有 `appCode` 和 `handlerName`，缺少显式 `workerGroup`、`ownerService` 和 job 级 capability 约束。
- 内嵌 Worker 选择逻辑在 `embedded-worker-enabled=true` 时会基于任务定义补写当前进程 Worker 能力，没有先确认当前进程真实拥有 handler。
- Worker capability 只到 `appCode + handlerName`，不能表达“同一 handler 只允许执行某些 jobCode”的细粒度能力。
- Worker 二次校验只校验 handler 存在，未校验 service/group/jobCode capability。
- UI Worker 页面缺少“服务编码、Worker 分组、支持任务/处理器、自动注册来源”的清晰展示。
- 多实例真实运行验证尚未形成固定验收脚本和证据模板。

## 3. 目标模型

### 3.1 任务定义归属

`mango_job_definition` 增加或补齐以下归属字段：

| 字段 | 说明 | 默认规则 |
|---|---|---|
| `owner_service` | 任务所属服务，表达业务服务归属 | 默认等于 `app_code` |
| `worker_group` | Worker 分组，用于同一服务内隔离不同执行池 | 默认等于 `owner_service` |
| `handler_name` | Java handler 名称 | 必填 |
| `job_code` | 任务编码 | 已有，租户和应用内唯一 |

归属匹配键：

```text
tenantId + ownerService + workerGroup + appCode + handlerName
```

当 handler 需要限制具体任务时，再增加：

```text
jobCode
```

### 3.2 Worker 快照

`mango_job_worker_snapshot` 增加或补齐：

| 字段 | 说明 |
|---|---|
| `service_code` | Worker 所属服务 |
| `worker_group` | Worker 分组 |
| `transport_type` | `IN_MEMORY` 或 `HTTP_INTERNAL` |
| `register_source` | `EMBEDDED_AUTO`、`REMOTE_AUTO`、`MANUAL` |
| `instance_id` | 实例标识，进程内唯一 |
| `runtime_address` | 真实地址，内嵌为 `in-memory://...`，远程为 `http(s)://...` |

当前实现的唯一约束：

```text
tenant_id + service_code + worker_group + engine_type + worker_address
```

说明：

- `worker_address` 是当前调度和幂等事实地址：内嵌 Worker 当前使用 `in-memory://{host}/embedded-{pid}@{host}`，远程 Worker 使用 `http(s)://...`。
- 同一个地址允许在不同 `service_code + worker_group` 下登记为不同 Worker，避免服务 A 和服务 B 共用远程入口时被合并。
- 同一个 `service_code + worker_group + worker_address` 重复心跳只更新同一条 Worker，不新增重复记录。
- `runtime_address`、`transport_type`、`register_source`、`instance_id` 作为运行元数据保存和展示；调度时优先使用持久化 `transport_type`，地址前缀只作为兼容回退。

### 3.3 Worker 能力

`mango_job_worker_capability` 增加或补齐：

| 字段 | 说明 |
|---|---|
| `service_code` | 能力所属服务 |
| `worker_group` | 能力所属 Worker 分组 |
| `job_code` | 可执行任务编码；为空表示该 handler 下全部任务 |
| `handler_name` | 处理器名称 |
| `handler_version` | 处理器版本 |
| `enabled` | 能力启用状态 |

能力匹配规则：

```text
tenantId 相等
serviceCode = definition.ownerService
workerGroup = definition.workerGroup
appCode = definition.appCode
handlerName = definition.handlerName
enabled = 1
jobCode is null or jobCode = definition.jobCode
```

### 3.4 Handler 契约

`MangoJobHandler` 保持现有 `appCode()` 和 `handlerName()`，新增默认方法：

```java
default String serviceCode() {
    return appCode();
}

default String workerGroup() {
    return serviceCode();
}

default Set<String> supportedJobCodes() {
    return Set.of();
}
```

兼容策略：

- 老 handler 不实现新方法时，`serviceCode` 默认取 `appCode`。
- `workerGroup` 默认取 `serviceCode`。
- `supportedJobCodes` 为空表示该 handler 接受同服务同分组下绑定该 handler 的任务。

## 4. 调度规则

JobCenter 派发流程升级为：

1. 读取任务定义，解析 `tenantId/appCode/ownerService/workerGroup/jobCode/handlerName`。
2. 查询 Worker capability，必须命中目标归属。
3. 查询 Worker snapshot，必须状态为 `ONLINE`，未 `DISABLED`，未 `DRAINING`，心跳未过期。
4. 按容量、当前负载、最近心跳和策略选择 Worker。
5. 创建 attempt，写入 `workerId`、`workerAddressSnapshot`、`serviceCode`、`workerGroup`、`fencingToken`。
6. 按 Worker transport 派发：
   - `IN_MEMORY`：仅允许本进程内 Worker 执行。
   - `HTTP_INTERNAL`：调用远程 Worker 内部接口执行。
7. Worker 执行前再次校验：
   - 本进程 handler 存在。
   - handler 的 `serviceCode/workerGroup/appCode/handlerName/jobCode` 与任务匹配。
   - 不匹配时返回拒绝结果，不执行业务代码。

禁止规则：

- 禁止所有 Worker 共用一个全局执行池后只按空闲随机派发。
- 禁止 JobCenter 在未确认 handler capability 的情况下补写 Worker 能力。
- 禁止手动添加 `IN_MEMORY` Worker。
- 禁止 UI 将历史过期内嵌 Worker 展示为在线。

## 5. 内嵌 Worker 自动发现

内嵌 Worker 发现不扫描网络，也不手动登记。它只从当前 Spring 容器的 `MangoJobHandler` Bean 注册表派生。

启动或心跳时：

1. 扫描当前进程真实 `MangoJobHandler`。
2. 按 `serviceCode + workerGroup` 分组。
3. 每组生成一个 `IN_MEMORY` Worker snapshot。
4. 为该 Worker 写入真实 handler capability。
5. 对已经不存在的 capability 标记 `enabled=0`。
6. 对同一 `instanceId` 的重复心跳只更新同一条 Worker，不新增重复记录。

单体多实例显示规则：

- 一个进程内同一 `serviceCode + workerGroup` 显示一个内嵌 Worker。
- 多个进程显示多个内嵌 Worker。
- 同一进程因多个任务或多个 handler 不能重复显示多个同组 Worker。
- 进程重启后旧 `instanceId` 超过心跳窗口必须标记 `EXPIRED`，不能继续显示在线。

## 6. 远程 Worker 自动注册和手动登记

远程 Worker 推荐由 `mango-job-starter-remote` 自动注册。

自动注册必须上报：

- `tenantId`
- `serviceCode`
- `workerGroup`
- `appCode`
- `workerAddress`
- `transportType=HTTP_INTERNAL`
- `instanceId`
- handler capability
- 支持 jobCode 清单

手动登记只允许：

- `transportType=HTTP_INTERNAL`
- 指定 `serviceCode`
- 指定 `workerGroup`
- 指定 `workerAddress`
- 指定 handler capability

手动登记不负责启动 Worker 进程，只登记治理事实。真实执行仍必须由远程 Worker 接口二次校验。

## 7. UI 调整

### 7.1 Worker 节点

列表必须展示：

- 服务编码
- Worker 分组
- 通信方式
- 注册来源
- 实例标识
- Worker 地址
- 状态
- 最近心跳
- 支持处理器数量
- 支持任务数量

行内详情展示 capability：

```text
serviceCode | workerGroup | appCode | handlerName | supportedJobCodes | enabled
```

登记 Worker 表单：

- 只允许登记 `HTTP_INTERNAL`。
- 必填 `服务编码`、`Worker 分组`、`Worker 地址`、`处理器`。
- `Worker 地址` 必须是 `http(s)://`。
- 不显示或不允许选择 `IN_MEMORY`。

### 7.2 任务定义

任务定义表单增加：

- 所属服务，默认等于所属应用。
- Worker 分组，默认等于所属服务。
- 处理器下拉只显示匹配 `所属服务 + Worker 分组 + 所属应用` 的可用 handler。
- 保存或启用任务时校验目标 capability 是否存在。

## 8. 实施阶段

### 阶段 1：契约和数据模型（已完成）

目标：

- 补齐 API command/query/VO 字段。
- 新增 migration。
- 扩展 handler 契约默认方法。
- 保持老任务默认值兼容。

交付：

- `SaveMangoJobDefinitionCommand` 增加归属字段。
- `RegisterMangoJobWorkerCommand`、`CreateMangoJobWorkerCommand` 增加服务、分组、来源和 capability 字段。
- `MangoJobDefinitionVO`、`MangoJobWorkerSnapshotVO`、`MangoJobHandlerVO` 增加展示字段。
- Flyway migration 增加字段、索引和默认值回填。

已验证：

- 字段默认值兼容测试。
- migration 空库创建、V5 归属字段和关键唯一索引验证。
- API 参数校验测试。
- `mango_job_worker_capability.job_code` 存储层统一为 `NOT NULL DEFAULT ''`，空串表示该 handler 不限制具体 `jobCode`；查询兼容旧 `NULL` 数据。

### 阶段 2：真实能力注册（已完成）

目标：

- 内嵌 Worker 只根据当前进程真实 handler 注册能力。
- 远程 Worker 自动注册上报服务、分组和 jobCode capability。
- 手动登记只支持 `HTTP_INTERNAL`。

交付：

- 新增 Worker heartbeat/register 聚合逻辑。
- 删除或替换基于任务定义补写内嵌 Worker capability 的逻辑。
- Worker 过期和 capability 失效规则补齐。

已验证：

- 单进程同组多 handler 只生成一个 Worker。
- 同进程重复心跳不新增 Worker。
- handler 移除后 capability 失效。
- 手动登记 `IN_MEMORY` 被拒绝。
- 同一远程地址可在不同 `serviceCode + workerGroup` 下注册为不同 Worker。

### 阶段 3：调度过滤和 Worker 二次校验（已完成）

目标：

- 调度层严格按归属和 capability 选择 Worker。
- Worker 层拒绝不匹配任务。

交付：

- `selectWorker` 统一走 capability 匹配，不因 `embedded-worker-enabled` 绕过查询。
- `IN_MEMORY` dispatch 只能派发到本进程已注册 Worker。
- `MangoJobWorkerExecutor` 增加 service/group/jobCode 校验。
- attempt 记录 service/group 快照。

已验证：

- A 服务任务只能派发到 A Worker。
- B Worker 收到 A 任务时拒绝，业务 handler 不执行。
- 禁用 Worker 后不再派发。
- Worker 过期后不再派发。

### 阶段 4：UI 和 E2E（已完成基础治理，真实多实例截图待补）

目标：

- 后台清楚展示 Worker 归属、能力和注册来源。
- 任务定义只能选择可执行 handler。
- E2E 覆盖多实例、多服务和错误归属。

交付：

- Worker 列表和详情调整。
- 任务定义归属字段和 handler 下拉调整。
- E2E 用例和截图证据。

已验证：

- UI 不允许手动登记内嵌 Worker。
- Worker 页面展示服务编码、Worker 分组、通信方式、注册来源、运行地址和实例标识。
- Worker 手动登记、禁用、恢复已通过 Playwright 后台 E2E。

待补：

- UI 可看到多个内嵌 Worker 分别属于不同实例。
- UI handler 下拉不会把 B 服务 handler 给 A 任务选择；当前后端已做调度和 Worker 侧强校验，前端过滤仍需跟随可用 handler capability 进一步收敛。

### 阶段 5：真实多实例验收（已完成本地验证）

目标：

- 用真实后端进程验证多实例 Worker 行为。

部署拓扑：

```text
Mango Monolith #1  18657  embedded worker enabled
Mango Monolith #2  18658  embedded worker enabled
共享 primary DB
共享 mango_job DB
```

必须验证：

- 两个实例健康检查均为 `UP`。
- `mango_job_worker_snapshot` 中出现两个不同 `IN_MEMORY` Worker。
- 同一实例重复心跳不产生重复 Worker。
- 每分钟 Cron 不重复创建同一调度窗口实例。
- 执行实例记录真实 `workerAddressSnapshot`。
- 禁用其中一个 Worker 后，新实例不再派发给它。
- 停止其中一个实例后，对应 Worker 超时转为 `EXPIRED`。

验证结论：

- 2026-06-07 使用当前 worktree 干净主库 `mango_dev_a1ce46` 和 Job 独立库 `mango_dev_a1ce46_job` 完成真实双进程验证。
- `http://127.0.0.1:18657/actuator/health` 和 `http://127.0.0.1:18658/actuator/health` 均返回 `UP`，两个数据源 `primary/job` 均为 `UP`。
- `mango_job_worker_snapshot` 中存在两个不同 `IN_MEMORY/EMBEDDED_AUTO/ONLINE` Worker：`embedded-29094@MacBookPro.local` 和 `embedded-35634@MacBookPro.local`。
- 每分钟示例任务 `mango_job_example_chromium_every_minute_cron_probe` 最近 12 个调度窗口均为 1 条 `SUCCESS` 实例，重复窗口数 0。
- GitHub Issue `#109` 被确认是旧本地库 `mango_dev_job_runtime_dual_0607` 的历史脏库问题；当前 worktree 主库授权 V43 checksum 与源码一致，不再阻塞本地验收。

## 9. 验证矩阵

| ID | 场景 | 验证方式 | 通过标准 |
|---|---|---|---|
| OWN-UNIT-001 | handler 默认归属 | 单元/集成测试 | 老 handler 默认 `serviceCode=appCode`、`workerGroup=serviceCode` | DONE |
| OWN-UNIT-002 | capability 匹配 | 集成测试 | 不同 service/group/jobCode 不能互相匹配 | DONE |
| OWN-UNIT-003 | Worker 二次校验 | 单元测试 | handler 不存在或归属不匹配时拒绝执行 | DONE |
| OWN-INT-001 | 内嵌自动注册 | 集成测试 | 当前进程真实 handler 自动注册，非真实 handler 不注册 | DONE |
| OWN-INT-002 | 内嵌幂等心跳 | 集成测试 | 重复心跳更新同一 Worker | DONE |
| OWN-INT-003 | 手动登记限制 | 集成测试 | `IN_MEMORY` 手动登记失败，`HTTP_INTERNAL` 可登记 | DONE |
| OWN-INT-004 | A/B 服务隔离 | 集成测试 | A 任务只派发给 A Worker，B Worker 不执行 A 任务 | DONE |
| OWN-INT-005 | Worker 禁用 | 集成测试 | 禁用后不再派发 | DONE |
| OWN-INT-006 | Worker 过期 | 集成测试 | 心跳超时后状态变更且不再派发 | DONE |
| OWN-INT-007 | 同地址不同归属 | 集成测试 | 同一 HTTP 地址可注册 service A/B 两个 Worker，A 任务只选 A Worker | DONE |
| OWN-E2E-001 | 单体多实例 | 真实双进程验证 | 两个实例显示两个内嵌 Worker，无重复在线 Worker | DONE |
| OWN-E2E-002 | Cron 去重 | 真实双进程验证 | 最近 12 个调度窗口均为单实例 `SUCCESS`，重复窗口数 0；生产前仍需预发 2-4 小时长跑 | DONE |
| OWN-E2E-003 | UI 归属展示 | Playwright | Worker 页面显示服务、分组、来源和能力 | DONE |
| OWN-E2E-004 | UI 手动登记 | Playwright | 表单只允许 HTTP Worker | DONE |
| OWN-E2E-005 | 错误归属拒绝 | 后端集成或 E2E | B Worker 收到 A 任务时拒绝且业务输出不存在 | DONE |

## 10. 投产门槛

本升级完成真实多实例验收后，Mango Job 已具备本地单体多实例验收证据；生产级多节点单体部署仍需按投产计划完成预发长跑和运维验证。

当前投产判断：

- 单进程内嵌 Worker、远程 HTTP Worker、A/B 服务隔离、同地址不同服务隔离、重复注册幂等、Worker 禁用/恢复、每分钟 Cron 稳定性后端集成测试已完成。
- 后台 UI 的 Worker 治理、任务定义、执行实例、日志、告警规则基础 E2E 已完成。
- 真实双进程单体多实例 Worker 展示和 Cron 去重已有本地运行证据。
- 旧本地库 Flyway V43 checksum mismatch 已确认为历史脏库问题，当前干净 worktree 主库不复现；Issue `#109` 不再阻塞本地 Job 验收。
- 生产投产仍需预发 2-4 小时 Cron 稳定性、真实通知通道、权限矩阵和发布物验证。

完成后投产门槛：

- Job 模块 Maven 测试通过。
- Job 模块 checkstyle/PMD 通过。
- 多实例真实验证通过并保存证据。
- 远程 Worker E2E 通过。
- 管理后台 Job E2E 通过。
- Worker 页面不存在不可解释占位 Worker 或不可解释在线 Worker。
- 预发连续 2-4 小时 Cron 稳定性验证通过。
- A/B 服务隔离验证通过。
- PR 描述列出归属模型、兼容策略和验证证据。

## 11. 不纳入本次升级

- 不改 Mango infra 层公共依赖方向。
- 不引入 MQ、gRPC 或服务网格。
- 不支持 UDP 或 Akka。
- 不做跨服务业务幂等实现，平台只提供 `idempotencyKey`。
- 不自动删除历史未知 Worker 数据，历史清理由单独脚本和 DBA 评审处理。
