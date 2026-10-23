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

`BootstrapStepContributor.supportsResourceBaselineBuild()` 默认返回 `false`。该方法只供 Mango Resource 构建链隔离正式 Resource 步骤；普通业务、租户和运行期 contributor 不得 opt-in，仍在目标环境 Bootstrap 中执行。

## 6. 配置说明

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.bootstrap.mode` | 由第一个命令参数或本地托管入口设置 | `bootstrap` 或 `runtime`。无参数 `MangoApplication.run(App.class)` 仅用于本地开发，会自动完成 bootstrap 后再启动 runtime。 |
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
| `BootstrapStepContributor` | 向全局计划贡献步骤；默认不进入构建期 Resource baseline。 |
| `BootstrapStep` | 声明 code、phase、依赖、fingerprint material 和执行逻辑。 |
| `BootstrapExecutionContext` | 向步骤传递 environment、generation、fingerprint 和 fencing token。 |
| `BootstrapGenerationFence` | 目标服务校验写入是否来自权威 generation。 |
| `BootstrapRuntimeAuthorityProvider` | Runtime eventual worker 获取当前写权限。 |
| `BootstrapWriteAuthority` | environment、generation、fingerprint、fencing token 的不可变组合。 |

`apply/cold` 在同一执行中完成 expand 和 finalize。`apply/rolling` 只执行 expand；新 Runtime 接流并排空旧 generation 后，再执行 `finalize`。
对已 `FINALIZED` 且 fingerprint 未变的同一 generation 再次执行 `finalize` 直接返回 `FINALIZED`（执行 0 步，不写 control 和 execution），用于同一构建版本重复发布；generation 相同但 fingerprint 不同时返回 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`。
如果候选版本在 finalize 前需要撤回，先停止该 generation 的全部 Runtime，再执行 `abort`。该操作保留 expand 产生的兼容 DDL 和资源，清除 candidate，恢复 stable generation 的写权，并递增 fencing token；finalize 开始后不允许 abort，失败状态 `FINALIZE_FAILED` 只能续跑 finalize。

## 7.1 部署引擎调用契约

本节是平台对部署引擎（含 `mango-deploy-shell`）的调用契约。引擎按契约调用，不要反推控制表状态。

### 7.1.1 统一调用序列

| 场景 | 序列 |
|------|------|
| 空库首次发布 | `plan` → `apply --strategy=cold` → `verify` → 接流 |
| 版本升级 | `plan` → `apply --strategy=rolling` → 接流并排空旧 generation → `finalize` → `verify` |
| 同一构建版本重发 | 沿用升级序列；`apply` 和 `finalize` 都幂等返回，不需要任何预检 |
| 数据重建（RESET） | `verify` |

数据升级判定为跳过、控制表身份判断、"是否需要 finalize" 这类预检不属于契约。引擎必须走统一序列，由平台自身的幂等保证收敛。

### 7.1.2 幂等保证

同一 `environment-key + generation + fingerprint` 的重复调用：

| 动作 | 重复调用结果 | 是否修改持久状态 |
|------|--------------|------------------|
| `plan` | `PLANNED` | 否 |
| `apply`（该 generation 已是 stable） | `FINALIZED`，0 步 | 否 |
| `apply`（该 generation 已有候选） | 复用已成功步骤，只补未完成步骤 | 只追加新执行步骤 |
| `finalize`（该 generation 已是 stable） | `FINALIZED`，0 步 | 否 |
| `finalize`（状态为 `FINALIZE_FAILED`） | 续跑 finalize | 是，推进到 `FINALIZED` |
| `verify`（stable 或已 `EXPANDED` 候选） | `VERIFIED` | 否 |

### 7.1.3 成功判定

- 唯一成功判据是命令退出码为 0。
- 引擎禁止依赖：`executionId` 是否为空、`mango_bootstrap_execution` 或 `mango_bootstrap_step_execution` 是否新增行、日志文本、控制表内部字段。
- 原因：幂等返回不产生执行记录和 `executionId`，这些是诊断和审计数据，不是发布结论的契约。审计表行数也不代表本次发布是否执行过步骤。

### 7.1.4 错误语义与引擎动作

| 错误 | 含义 | 引擎动作 |
|------|------|----------|
| `BOOTSTRAP_RECEIPT_MISSING` | 该环境从未成功 apply | 执行统一序列的 `apply` 起始步骤 |
| `BOOTSTRAP_CANDIDATE_MISSING` | 请求的 generation 既不是当前 stable，也没有已准备候选 | 调用方参数错误；核对清单里的 release、revision、generation |
| `BOOTSTRAP_GENERATION_ALREADY_RELEASED` | 该 generation 已用不同内容发布 | 不可重试；用新 generation 重新 plan/apply |
| `BOOTSTRAP_FINGERPRINT_MISMATCH` | 内容或发布标识与环境记录不一致 | 核对完整 release tuple；用新 generation |
| `STALE_BOOTSTRAP_GENERATION` / `STALE_RUNTIME_GENERATION` | 请求 generation 落后于环境 | 使用环境当前 generation |
| `BOOTSTRAP_FINALIZE_RETRY_REQUIRED` | 状态为 `FINALIZING` 或 `FINALIZE_FAILED` | 只能续跑 `finalize`，禁止 `abort` |
| `OLD_RUNTIME_INSTANCES_ACTIVE` | 旧 generation 的 lease 未排空 | 停止旧实例，等待 lease TTL 后续跑 `finalize` |
| `CANDIDATE_RUNTIME_INSTANCES_ACTIVE` | 候选 generation 仍有活跃 lease | 停止候选实例后再 `abort` |

### 7.1.5 回滚契约

| 当前状态 | 是否可 `abort` | 引擎动作 |
|----------|----------------|----------|
| `EXPANDING`、`EXPANDED`、`FAILED` | 可以 | 停止候选实例 → `abort` → 环境回到上一 stable |
| `FINALIZING`、`FINALIZE_FAILED` | 禁止 | 只能续跑 `finalize` |
| `FINALIZED` | 不适用 | 不要调用 `abort`；回到上一版本用上一版本清单重新走统一序列 |

对已 `FINALIZED` 的 generation 调用 `abort` 返回 `BOOTSTRAP_CANDIDATE_MISSING`，这是调用方错误，不是可重试的临时故障。

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

本地 Mango 主仓、CLI 新生成项目以及已迁移到 `MangoApplication.run` 的业务项目，使用 `mango dev start backend` 或直接在 IDEA 运行无参数 `main()` 时，都会在当前 worktree 的 `mango_dev_*` 独立数据库中自动准备 bootstrap 回执、执行必要 Flyway 增量迁移并启动 runtime。两种入口复用同一 Bootstrap/Runtime 校验；CLI 会向进程传递 `MANGO_LOCAL_LIFECYCLE_KEY`，保证 lifecycle identity 一致。存量 `SpringApplication.run` 项目继续走兼容直启，不会被 CLI 自动切换生命周期。CLI 与 IDEA 的本地编排不替代测试、预发或生产发布流程；这些环境仍按下列命令显式管理 generation、策略和切流。

本地托管入口从当前目录向上查找 `.mango/dev-workspace.env`，读取 workspace、数据库和端口配置；当 `MANGO_DB_AUTO_CREATE=true` 时，仅允许自动创建 `mango_dev_*` 数据库。应用仍必须通过 `MangoApplication.run(App.class, args)` 接入，不能用 `SpringApplication.run` 绕过 Mango 门禁。显式传入 `bootstrap` 或 `runtime` 时保持生产生命周期语义，不启用本地托管。

Mango 源码仓或派生 CI 的选定模块安装使用 `-pl :mango-bom,<backend-module> -am`。Maven `-am` 不会仅因为后端 POM 导入 BOM 就自动把本地 `mango-bom` 放进 Reactor；缺少该 selector 时，CI-friendly revision 可能从远端解析到错误坐标。普通业务仓继续从发布仓库消费 BOM，不需要加入 Mango 源码模块。

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
| `BOOTSTRAP_CANDIDATE_MISSING` | 该 generation 既不是当前 stable，也没有已 prepare 的 candidate；确认是否漏了 `bootstrap apply`，或本次是否本应复用已 `FINALIZED` 的 generation。 |
| `BOOTSTRAP_GENERATION_ALREADY_RELEASED` | 该 generation 已用另一组 release/revision 或不同内容发布；比对 `mango.release.id`、`mango.release.revision`，内容确实变化时用新 generation 重新 plan/apply。 |
| `BOOTSTRAP_FINGERPRINT_MISMATCH` | 比较异常中的 expected/actual，确认源码、完整 release tuple、environment 和 generation。`1.0.31` 已隔离 Bootstrap/Runtime 的宿主 Jackson 差异；旧组合留下的不一致应升级完整 tuple 后使用新 generation 重新 plan/apply/verify，不能删库、覆盖旧回执或手工改审计表。 |
| `OLD_RUNTIME_INSTANCES_ACTIVE` | 旧 generation lease 尚未排空，停止旧实例并等待 TTL 后再 finalize。 |
| `CANDIDATE_RUNTIME_INSTANCES_ACTIVE` | 候选 generation 仍有活跃 lease，停止候选实例并等待 TTL 后再 abort。 |
| Runtime 变为 `REFUSING_TRAFFIC` | 检查回执权威代、fingerprint 和 lease 续租；不要让 Runtime 自动补做初始化。 |

## 12. 升级与兼容

| 适用版本 | 变更 | 影响 | 需要做的事 | 验证 | 回滚 |
|----------|------|------|------------|------|------|
| `1.0.63 → 后续版本` | `finalize` 对已 `FINALIZED` 且 fingerprint 未变的同一 generation 返回 `FINALIZED`（0 步，不改 control/execution）；同 generation 但内容不同时返回 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`；新增部署引擎调用契约（§7.1） | 同一构建版本重发不再报 `BOOTSTRAP_CANDIDATE_MISSING`；正常升级行为不变 | 升级 Maven `mango-infra-bootstrap-core` 与 `mango-infra-bootstrap-starter`；部署引擎按 §7.1 统一序列调用，删除自行推断控制表的预检 | 同一版本连续执行两次 `bootstrap finalize` 均返回 `FINALIZED`，且 `mango_bootstrap_control` 与执行表不变化 | 回退对应 Maven 版本；已写入的 `FINALIZED` 状态无需修复 |

## 13. 相关文档

- [Bootstrap 生命周期设计](../../../mango-docs/designs/2026-07-27-mango-bootstrap-lifecycle-design.md)
- [Persistence](../mango-infra-persistence/README.md)
- [Resource](../../mango-platform/mango-resource/README.md)
- [能力地图](../../../mango-docs/capabilities/README.md)

## 14. Issue #690 升级与回滚合同

Maven `1.0.30`/`1.0.3x` 业务仓升级必须消费 Maven `1.0.31`、`@mango/pmo@1.3.9`、`@mango/cli@1.0.96` 和匹配前端矩阵组成的完整 release tuple，不得单独替换 Bootstrap 相关 jar。升级前保存 `.mango`、Bootstrap 四张审计表和应用日志；既有数据库不重建。

既有环境按 `plan -> apply --strategy=rolling -> verify -> runtime -> finalize` 执行。`apply` 只写入候选 generation，确认新 Runtime receipt 的 environment、revision、generation、fingerprint 和 fencing token 一致且旧 lease 排空后才可 finalize。finalize 前失败使用 `abort`，它清除 candidate、恢复 stable 写权并递增 fencing token；finalize 开始后只能续跑 finalize。Runtime 不执行 Flyway、Resource 初始化或自动修复缺失回执。

Maven `1.0.31` 的 Resource 步骤 fingerprint 使用独立的 canonical JSON mapper，不再受 Bootstrap 非 Web 与 Runtime Web/HTTP Jackson 序列化差异影响。诊断时可开启 DEBUG 查看 Runtime 总 fingerprint、逐步骤 hash、Resource 模块数和声明数；日志不输出完整业务声明或敏感字段。
