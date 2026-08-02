# Mango Infra Bootstrap

## 1. 概览

`mango-infra-bootstrap` 为同一个 Mango 应用制品提供独立的初始化入口和运行入口。数据库迁移、必需 Resource、租户前置与对账在 `bootstrap` 进程完成；`runtime` 只在已存在匹配回执时接收流量。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 统一入口 | 同一个 Jar/镜像通过 `MangoApplication` 选择 `bootstrap` 或 `runtime`。 |
| 初始化编排 | 提供 `plan`、`apply`、`verify`、`finalize`、`abort`，按步骤依赖拓扑执行。 |
| 状态与幂等 | 持久化 control、execution、step 和 runtime lease；相同 fingerprint 可续跑。 |
| 滚动升级 | generation、manifest fingerprint、fencing token 和旧实例 drain 共同约束 finalize。 |
| Runtime 门禁 | Runtime 校验稳定代或候选代回执，登记 lease 并周期续租。 |

## 3. 能力边界

- Bootstrap 是非 Web 运维进程，不提供业务 HTTP API，不启动业务 runner、定时任务或流量监听。
- Runtime 不执行 Flyway，也不执行阻断启动的 Resource 初始化。
- 本模块只负责编排和回执；DDL 属于 Persistence，资源物化属于 Resource 及目标能力模块。
- 同一 `environment-key + generation` 的 manifest fingerprint 不得变化；旧 fencing token 不得继续写入。

## 4. 模块入口

| 模块 | 职责 |
|------|------|
| `mango-infra-bootstrap-api` | 步骤 SPI、阶段、generation fencing 与运行期写权限契约。 |
| `mango-infra-bootstrap-core` | plan/apply/verify/finalize/abort 编排、锁、回执、步骤幂等和 Runtime lease 持久化。 |
| `mango-infra-bootstrap-starter` | `MangoApplication` 入口、命令参数绑定、Bootstrap 进程隔离和 Runtime 门禁。 |

## 5. 接入方式

最终应用引入 starter，官方 `mango-admin-starter` 已聚合该依赖：

```xml
<dependency>
    <groupId>io.mango.infra.bootstrap</groupId>
    <artifactId>mango-infra-bootstrap-starter</artifactId>
</dependency>
```

应用 `main` 方法必须调用：

```java
MangoApplication.run(MyApplication.class, args);
```

业务或基础能力模块需要贡献初始化步骤时，只依赖 `mango-infra-bootstrap-api` 并实现 `BootstrapStepContributor`；不要依赖 core 或 starter。

## 6. 配置说明

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.bootstrap.mode` | 由第一个命令参数设置 | `bootstrap` 或 `runtime`。 |
| `mango.bootstrap.action` | `APPLY` | `PLAN`、`APPLY`、`VERIFY`、`FINALIZE`、`ABORT`。 |
| `mango.bootstrap.strategy` | `ROLLING` | 首次空库使用 `COLD`；后续升级使用 `ROLLING`。 |
| `mango.bootstrap.environment-key` | `default` | 同一环境的锁、回执和 Runtime lease 作用域。 |
| `mango.bootstrap.lock-timeout-seconds` | `30` | 获取数据库级 Bootstrap 锁的最长时间。 |
| `mango.bootstrap.instance-id` | 自动生成 | Runtime 实例 lease 标识。 |
| `mango.bootstrap.runtime-lease-ttl` | `30s` | Runtime lease 过期时间。 |
| `mango.bootstrap.runtime-heartbeat-interval` | `10s` | Runtime lease 续租间隔。 |
| `mango.release.id` | 无 | 必填，当前发布标识。 |
| `mango.release.revision` | 无 | 必填，制品源码 revision。 |
| `mango.release.generation` | `0` | 必填且必须为正数。 |
| `mango.release.fingerprint` | 空 | 可选的构建期预期 fingerprint；填写后严格匹配。 |

## 7. API 与扩展

| 类型 | 用途 |
|------|------|
| `BootstrapStepContributor` | 向全局计划贡献步骤。 |
| `BootstrapStep` | 声明 code、phase、依赖、fingerprint material 和执行逻辑。 |
| `BootstrapExecutionContext` | 向步骤传递 environment、generation、fingerprint 和 fencing token。 |
| `BootstrapGenerationFence` | 目标服务校验写入是否来自权威 generation。 |
| `BootstrapRuntimeAuthorityProvider` | Runtime eventual worker 获取当前写权限。 |
| `BootstrapWriteAuthority` | environment、generation、fingerprint、fencing token 的不可变组合。 |

`apply/cold` 在同一执行中完成 expand 和 finalize。`apply/rolling` 只执行 expand；新 Runtime 接流并排空旧 generation 后，再执行 `finalize`。
如果候选版本在 finalize 前需要撤回，先停止该 generation 的全部 Runtime，再执行 `abort`。该操作保留 expand 产生的兼容 DDL 和资源，清除 candidate，恢复 stable generation 的写权，并递增 fencing token；finalize 开始后不允许 abort，失败状态 `FINALIZE_FAILED` 只能续跑 finalize。

## 8. 数据与初始化

生命周期表由 `mango-infra-bootstrap-core/src/main/resources/db/migration/bootstrap/V1__init_bootstrap_lifecycle.sql` 创建：

该目录由 `BootstrapSchemaMigrator` 独立迁移，不作为 `mango.persistence.flyway.modules` 模块，也不进入 Persistence 的模块 history table。

| 表 | 用途 |
|----|------|
| `mango_bootstrap_control` | 环境的 stable/candidate generation、fingerprint、状态和 fencing token。 |
| `mango_bootstrap_execution` | 每次 plan 之外的执行记录和结果。 |
| `mango_bootstrap_step_execution` | 步骤 fingerprint、状态、摘要与失败信息。 |
| `mango_runtime_instance` | Runtime generation lease 与 drain 判断。 |

运行入口由 `MangoApplication` 注册进程模式，`BootstrapCommandRunner` 执行初始化命令，`RuntimeLeaseManager` 校验回执并维护 Runtime lease。Bootstrap 自身 schema 会在写回执前迁移；业务模块 migration 由 Persistence contributor 执行。

## 9. 管理入口

不适用。本模块没有管理页面或公共 Controller；运维只通过同一应用制品的命令入口和数据库审计记录操作。

## 10. 快速开始

本地 Mango 主仓、CLI 新生成项目以及已迁移到 `MangoApplication.run` 的业务项目，使用 `mango dev start backend` 时由 `@mango/cli` 在当前 worktree 的 `mango_dev_*` 独立数据库中自动准备 bootstrap 回执并启动 runtime。存量 `SpringApplication.run` 项目继续走兼容直启，不会被 CLI 自动切换生命周期。CLI 的本地编排不替代测试、预发或生产发布流程；这些环境仍按下列命令显式管理 generation、策略和切流。

首次空库：

```bash
java -jar app.jar bootstrap plan --mango.release.id=2026.07.27 \
  --mango.release.revision=abc123 --mango.release.generation=1
java -jar app.jar bootstrap apply --mango.bootstrap.strategy=cold \
  --mango.release.id=2026.07.27 --mango.release.revision=abc123 \
  --mango.release.generation=1
java -jar app.jar runtime --mango.release.id=2026.07.27 \
  --mango.release.revision=abc123 --mango.release.generation=1
```

后续滚动升级：

```text
bootstrap plan -> bootstrap apply --strategy=rolling -> 启动新 Runtime
-> 切流并排空旧 Runtime -> bootstrap finalize
```

finalize 前撤回候选版本：

```bash
java -jar app.jar bootstrap abort --mango.release.id=2026.07.27 \
  --mango.release.revision=abc123 --mango.release.generation=2
```

## 11. 问题排查

| 现象 | 处理 |
|------|------|
| 未传 `bootstrap`/`runtime` 即退出 | 最终应用已经使用强制生命周期入口，补充第一个进程参数。 |
| `BOOTSTRAP_RECEIPT_MISSING` | 当前 generation 尚未成功执行 `bootstrap apply`。 |
| `BOOTSTRAP_FINGERPRINT_MISMATCH` | 同 generation 的制品或步骤内容发生漂移；使用新 generation，不能覆盖旧回执。 |
| `OLD_RUNTIME_INSTANCES_ACTIVE` | 旧 generation lease 尚未排空，停止旧实例并等待 TTL 后再 finalize。 |
| `CANDIDATE_RUNTIME_INSTANCES_ACTIVE` | 候选 generation 仍有活跃 lease，停止候选实例并等待 TTL 后再 abort。 |
| Runtime 变为 `REFUSING_TRAFFIC` | 检查回执权威代、fingerprint 和 lease 续租；不要让 Runtime 自动补做初始化。 |

## 12. 相关文档

- [Bootstrap 生命周期设计](../../../mango-docs/designs/2026-07-27-mango-bootstrap-lifecycle-design.md)
- [Persistence](../mango-infra-persistence/README.md)
- [Resource](../../mango-platform/mango-resource/README.md)
- [能力地图](../../../mango-docs/capabilities/README.md)

## 13. Issue #690 升级与回滚合同

Maven `1.0.30`/`1.0.3x` 业务仓升级必须消费包含 #690 修复的完整 release tuple，不得单独替换 Bootstrap 相关 jar。升级前保存 `.mango`、Bootstrap 四张审计表和应用日志；既有数据库不重建。

既有环境按 `plan -> apply --strategy=rolling -> verify -> runtime -> finalize` 执行。`apply` 只写入候选 generation，确认新 Runtime receipt 的 environment、revision、generation、fingerprint 和 fencing token 一致且旧 lease 排空后才可 finalize。finalize 前失败使用 `abort`，它清除 candidate、恢复 stable 写权并递增 fencing token；finalize 开始后只能续跑 finalize。Runtime 不执行 Flyway、Resource 初始化或自动修复缺失回执。
