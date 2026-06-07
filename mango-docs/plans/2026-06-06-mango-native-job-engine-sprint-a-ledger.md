# Mango 原生 Job Engine Sprint A 交付台账

## 1. 目标

按 `mango-docs/designs/mango-native-job-engine-design.md` 启动 Sprint A，建立原生 Job Engine 的模型、状态机、transport 契约和旧 PowerJob Adapter 默认路径下线方案。

## 2. 范围

本 Sprint 只处理：

- 原生模型 DDL。
- API 枚举和核心契约。
- 状态机、幂等键、租约 token 的基础单元测试。
- `IN_MEMORY` / `HTTP_INTERNAL` transport 类型契约。
- 旧 PowerJob Adapter 默认装配隔离。

不处理：

- 完整 JobCenter 调度扫描。
- 完整 Worker runtime。
- 前端页面改造。
- 远程 Worker HTTP 接口实现。
- E2E 验收。

## 3. 原子交付项

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| JOB-A-001 | 设计 8.2 | 建立调度游标模型 | 新增 `mango_job_schedule_cursor`，用于多 JobCenter 抢占调度窗口 | Flyway migration、Entity/Mapper | Maven 测试和 migration 检查 | DONE | `V4__native_job_engine_foundation.sql`、`MangoJobScheduleCursorEntity`、`MangoJobScheduleCursorMapper`；`mvn -pl mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter -am test` BUILD SUCCESS |
| JOB-A-002 | 设计 8.4 | 建立执行尝试模型 | 新增 `mango_job_attempt`，表达一次 Worker 执行尝试 | Flyway migration、Entity/Mapper | Maven 测试 | DONE | `V4__native_job_engine_foundation.sql`、`MangoJobAttemptEntity`、`MangoJobAttemptMapper`；Maven BUILD SUCCESS |
| JOB-A-003 | 设计 8.6 | 建立 Worker 能力模型 | 新增 `mango_job_worker_capability`，支持 handler 匹配 | Flyway migration、Entity/Mapper | Maven 测试 | DONE | `V4__native_job_engine_foundation.sql`、`MangoJobWorkerCapabilityEntity`、`MangoJobWorkerCapabilityMapper`；Maven BUILD SUCCESS |
| JOB-A-004 | 设计 8.8 | 建立事件模型 | 新增 `mango_job_event`，用于审计、排障、告警异步消费 | Flyway migration、Entity/Mapper | Maven 测试 | DONE | `V4__native_job_engine_foundation.sql`、`MangoJobEventEntity`、`MangoJobEventMapper`；Maven BUILD SUCCESS |
| JOB-A-005 | 设计 8.7 | 建立日志分片模型 | 新增 `mango_job_log_chunk`，日志不依赖 PowerJob 内部表 | Flyway migration、Entity/Mapper | Maven 测试 | DONE | `V4__native_job_engine_foundation.sql`、`MangoJobLogChunkEntity`、`MangoJobLogChunkMapper`；Maven BUILD SUCCESS |
| JOB-A-006 | 设计 9 | 状态机契约 | 新增/修正任务、实例、attempt、Worker 状态枚举和状态机校验 | API enum、core service/test | 单元测试 | DONE | `JobAttemptStatus`、`JobInstanceStatus`、`JobWorkerStatus`、`MangoJobStateMachineTest`；Maven BUILD SUCCESS |
| JOB-A-007 | 设计 10 | 幂等键契约 | 定时、手动/API 触发可生成稳定 idempotency key | core service/test | 单元测试 | DONE | `MangoJobIdempotencyKeyService`、`MangoJobIdempotencyKeyServiceTest`；Maven BUILD SUCCESS |
| JOB-A-008 | 设计 11 | 租约 token 契约 | attempt 写终态必须携带 fencing token | core service/test | 单元测试 | DONE | `MangoJobLeaseService`、`MangoJobLeaseServiceTest`；Maven BUILD SUCCESS |
| JOB-A-009 | 设计 13 | transport 类型契约 | 首轮明确 `IN_MEMORY` 和 `HTTP_INTERNAL`，单体禁止绕端口 HTTP | API enum/config/test | 单元测试 | DONE | `JobTransportType`、`JobTransportTypeTest`；Maven BUILD SUCCESS |
| JOB-A-010 | 设计 18 | 旧 PowerJob 默认路径隔离 | PowerJob Adapter 不再由 Job starter 默认导入 | starter config/test | 单元测试 | DONE | `JobAutoConfiguration`、`PowerJobAutoConfiguration`、`PowerJobAutoConfigurationTest`；Maven BUILD SUCCESS |

## 4. 验证计划

```bash
cd mango
mvn -pl mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter -am test
```

验证结果：2026-06-06 执行通过，`mango-job-core` 与 `mango-job-starter` 均为 `BUILD SUCCESS`，共运行 29 个 job 相关测试，无失败、无错误、无跳过。

交付前执行：

```bash
node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/mango-native-job-engine-design.md \
  --ledger mango-docs/plans/2026-06-06-mango-native-job-engine-sprint-a-ledger.md \
  --mode verify
```
