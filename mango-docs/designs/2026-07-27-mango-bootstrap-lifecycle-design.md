# Mango Bootstrap 生命周期治理设计

> 状态：已确认，进入实施
> 日期：2026-07-27
> 风险等级：L3 / FULL
> 交付策略：Bootstrap/Runtime 分离、Resource 执行分类、滚动升级治理三个能力在同一个交付中一次完成；不保留旧的“应用启动时自动执行全部初始化”契约。
> 升级原则：已有 Mango 业务升级时保留业务源码与能力实现，允许丢弃旧数据库、业务历史数据和旧 Flyway 执行历史后从空库重建；新生命周期启用后的日常升级仍按 expand/rollout/finalize 保护数据和运行实例，不允许旧实例覆盖新版本声明。
> 确认依据：用户于 2026-07-27 回复“继续”，确认第 19 节五项不可逆契约。

## 1. 背景与问题

当前 Mango 把多个不同性质的动作放在业务应用启动链路中：

1. `PersistenceFlywayAutoConfiguration` 在 Spring 上下文刷新阶段串行执行所有模块 Flyway。
2. `ResourceSyncRunner` 在 `ApplicationRunner` 阶段扫描并同步 Resource Registry。
3. `TenantProvisioningReconciliationRunner` 依赖 Resource 状态完成机构基线和租户对账。
4. 上述动作完成前 readiness 为 `REFUSING_TRAFFIC`，但容器健康检查通常只看到应用长时间 unhealthy。

Baohan 空库发布已经证明该模型存在结构性问题：MySQL 就绪约 81 秒，模块 Flyway 和工作流初始化耗时接近 5 分钟，Spring 打印 `Started` 后仍进入 Resource/租户对账；Docker 在 API 尚未完成初始化时判定 unhealthy 并触发回滚。回滚镜像因为数据库已经被新版本初始化而很快健康，说明失败点不是应用无法运行，而是“初始化任务”和“运行态健康检查”被错误绑定。

同日将健康窗口放宽后的业务复测能够成功，但 API readiness 为 649 秒、发布阶段为 732 秒；API ready 后 Site、Admin、H5 才启动且均返回 HTTP 200，未触发回滚。这证明业务初始化本身可完成，也证明延长 Runtime healthcheck 只能作为临时措施。正式治理以“Bootstrap 允许 20～30 分钟独立执行，Bootstrap 成功后的 Runtime readiness 为秒级”为验收方向。

## 2. 目标、边界与非目标

### 2.1 目标

- Mango 提供同一业务制品的统一生命周期入口，支持 `bootstrap` 与 `runtime` 两种进程模式。
- 首次建库、普通升级、滚动升级都先由 Bootstrap 完成阻断性工作，再允许 Runtime 接流量。
- Flyway 继续负责 DDL；Resource Registry 继续负责声明式业务资源，二者拥有不同执行模型和历史事实表。
- Resource 新增独立的执行阶段维度，区分启动前必需、运行后最终一致和人工执行。
- Bootstrap 形成可校验的 release、generation、manifest fingerprint 和步骤回执。
- 旧 generation 不得覆盖新 generation 的资源，也不得在 finalize 后重新加入集群。
- 支持 expand/contract，破坏性动作只允许在旧实例退出后执行。
- 初始化失败有明确步骤、耗时、错误摘要和可重入语义，不依赖无限延长容器 healthcheck。

### 2.2 明确不保留的历史行为

- Runtime 不再自动执行模块 Flyway。
- `ResourceSyncRunner` 不再承担启动前必需资源的初始化。
- `TenantProvisioningReconciliationRunner` 不再把业务应用启动当作全量租户初始化入口。
- 业务应用必须从 `SpringApplication.run(...)` 切换到 Mango 生命周期入口；不提供静默兼容回退。
- 没有有效 Bootstrap 回执的 Runtime 必须拒绝启动，不能为了兼容旧环境跳过校验。

### 2.3 非目标

- 不把 Resource 声明改写为 SQL，也不把菜单、权限、角色等资源塞进 Flyway。
- 不提供通用数据库在线 DDL 平台；Mango 只提供阶段、门禁和证据，DDL 是否在线安全仍由 migration 作者负责。
- 不自动回滚已成功的 Flyway migration。
- 不在本次引入可视化升级控制台；入口首先面向容器、Jenkins、Kubernetes Job 和 CLI。
- 不为接入本次生命周期治理前的旧数据库提供原地兼容、数据回填或历史回执补录；旧业务工程通过代码迁移并以空库 Bootstrap 完成升级。

### 2.4 已有业务升级边界

- 保留已有业务 Java 源码、模块装配、Resource 声明、租户扩展和 Flyway migration 源文件，不以重写业务工程作为升级手段。
- 业务应用只需完成入口与发布编排迁移；框架提供统一 starter、命令契约和接入验证。
- 首次升级到新生命周期时允许删除旧库并从空库执行一次 `bootstrap apply --strategy=cold`，因此不承诺旧库数据或旧 Flyway history 可复用。
- 首次 Bootstrap 成功后产生 generation 1 的正式回执；从下一次发布开始使用 generation 单调递增及 `expand -> runtime rollout -> finalize`，不再依赖清库。
- “允许清库”仅是旧体系接入新体系的迁移策略，不等于 Runtime 可以自动重建数据库，也不放宽 finalize 的 fencing 与旧实例排空门禁。

## 3. 核心决策

| ID | 决策 | 说明 |
|---|---|---|
| DEC-001 | 一个制品，两种进程模式 | 同一业务 jar/镜像通过 Mango 入口运行 `bootstrap` 或 `runtime`，避免初始化镜像和运行镜像漂移。 |
| DEC-002 | Bootstrap 是唯一阻断性初始化写入口 | Flyway、引擎元数据、`BOOTSTRAP_REQUIRED` Resource、租户前置和最终对账均由 Bootstrap 编排。 |
| DEC-003 | Runtime 只校验回执 | Runtime 不执行 DDL 和阻断性资源初始化；启动时只做快速数据库回执、generation 和 fingerprint 校验。 |
| DEC-004 | Resource 执行阶段与同步语义正交 | `executionPhase` 决定何时执行；既有 `syncMode` 决定如何合并/覆盖，不能用一个枚举同时表达两件事。 |
| DEC-005 | generation 单调递增且受 fencing 保护 | 小于允许 generation 的新实例拒绝启动；相同 generation 但 fingerprint 不同直接失败；只有权威 generation 能写 Resource。 |
| DEC-006 | expand/contract 强制分离 | upsert、加表加列等兼容动作在 expand；缺失资源禁用、删列删表等破坏性动作在 finalize。 |
| DEC-007 | 一次性交付全部治理能力 | 不先发布半成品兼容层；框架模块、入口、回执、Resource 分类、fencing、finalize 和验证一起交付。 |

## 4. 模块与依赖边界

新增 `mango-infra-bootstrap` 聚合模块，采用 API/CORE/STARTER 分层：

```text
mango-infra-bootstrap
├── mango-infra-bootstrap-api
│   ├── BootstrapMode / BootstrapAction / BootstrapPhase
│   ├── BootstrapStep / BootstrapPlan / BootstrapReceipt
│   ├── BootstrapStepContributor
│   └── RuntimeGenerationStatus
├── mango-infra-bootstrap-core
│   ├── BootstrapOrchestrator
│   ├── BootstrapExecutionRepository
│   ├── BootstrapManifestHasher
│   ├── BootstrapGenerationFence
│   └── RuntimeInstanceLeaseRepository
└── mango-infra-bootstrap-starter
    ├── MangoApplication 生命周期入口
    ├── Bootstrap/Runtime 条件化自动配置
    ├── RuntimeReceiptGate
    └── Actuator/诊断集成
```

现有能力只通过 SPI 向 Bootstrap 贡献步骤：

| 模块 | 贡献步骤 | 允许依赖 |
|---|---|---|
| `mango-infra-persistence-starter` | `FLYWAY_EXPAND`、`FLYWAY_CONTRACT`、schema verify | 依赖 `mango-infra-bootstrap-api`，不得反向依赖 Resource/System。 |
| Flowable/工作流 starter | `ENGINE_METADATA` 校验或显式初始化 | 依赖 Bootstrap API；不得在 Runtime 隐式建表。 |
| `mango-resource-*` | Resource plan、required apply、eventual reconcile、finalize missing | 依赖 Bootstrap API；保留自己的 registry/sync/change log。 |
| `mango-system-core` | `TENANT_PREREQUISITES`、`TENANT_RECONCILIATION` | 依赖 Bootstrap API 和既有 Resource support 契约。 |
| 最终业务应用 | 选择 application class、release 参数和已装配模块 | 依赖 Bootstrap starter；不直接编排步骤。 |

Bootstrap core 不依赖具体平台模块。步骤通过 `BootstrapStepContributor` 注册，并用稳定的 step code、依赖、阶段、fingerprint material 和执行器描述自己。编排器对依赖图做拓扑排序；重复 code、循环依赖、缺失前置条件在 plan 阶段直接失败。

## 5. 统一入口与运行模式

最终应用入口改为：

```java
public static void main(String[] args) {
    MangoApplication.run(BaohanSystemApplication.class, args);
}
```

命令契约：

```bash
# 只读生成计划，不改数据库
java -jar app.jar bootstrap plan \
  --mango.release.id=2026.07.27.1 \
  --mango.release.generation=42

# 冷启动：在一个 Bootstrap 进程中执行 plan -> expand -> verify -> finalize
java -jar app.jar bootstrap apply \
  --mango.bootstrap.strategy=cold \
  --mango.release.id=2026.07.27.1 \
  --mango.release.generation=1

# 滚动升级第一段
java -jar app.jar bootstrap apply \
  --mango.bootstrap.strategy=rolling \
  --mango.bootstrap.phase=expand \
  --mango.release.id=2026.07.27.1 \
  --mango.release.generation=42

# 滚动升级旧实例排空后的收尾
java -jar app.jar bootstrap finalize \
  --mango.release.id=2026.07.27.1 \
  --mango.release.generation=42

# 运行态
java -jar app.jar runtime \
  --mango.release.id=2026.07.27.1 \
  --mango.release.generation=42
```

Bootstrap 模式强制：

- `WebApplicationType.NONE`；不启动 Tomcat、Controller、WebSecurity、任务 worker 和业务定时任务。
- 默认 lazy initialization，只实例化计划和步骤实际需要的 Bean。
- 关闭 Runtime runner；Bootstrap 完成后按退出码结束进程，成功为 0，计划不兼容/执行失败使用不同非零退出码。
- secret 只从既有 Spring 配置和 secret store 获取，不写入回执、计划或日志。

Runtime 模式强制：

- 关闭 Flyway migrate、Flowable 自动建表、required Resource 同步和租户全量对账。
- 在开放 readiness 前校验 Bootstrap control/receipt；不满足条件直接启动失败。
- 注册带 TTL 的实例 lease 并周期续租，供 finalize 判断旧 generation 是否真正退出。

## 6. Bootstrap 步骤模型

### 6.1 标准顺序

```text
PLAN
  -> ACQUIRE_FENCE
  -> FLYWAY_EXPAND
  -> ENGINE_METADATA
  -> TENANT_PREREQUISITES
  -> RESOURCE_REQUIRED
  -> TENANT_RECONCILIATION
  -> POST_VERIFY
  -> WRITE_EXPANDED_RECEIPT
  -> [启动新 Runtime，排空旧 Runtime]
  -> RESOURCE_FINALIZE
  -> FLYWAY_CONTRACT
  -> POST_FINALIZE_VERIFY
  -> WRITE_FINALIZED_RECEIPT
```

租户前置和最终对账分开是必要的：现有真实空库场景中，Resource 声明可能引用 System 创建的内置角色。前置步骤只建立 Resource 所需的最小幂等基线，不标记最终租户对账完成；Resource 成功后再执行全量最终对账。

### 6.2 步骤状态

`PLANNED -> RUNNING -> SUCCEEDED | FAILED | SKIPPED`

- 每个步骤必须声明幂等键：`generation + phase + stepCode + stepFingerprint`。
- 同 fingerprint 的成功步骤重试时复用结果；失败步骤可重入。
- 同 generation、同 step code 但 fingerprint 不同属于制品漂移，禁止覆盖。
- 步骤只记录结构化摘要和错误摘要，不记录 SQL 正文、资源 secret 或业务敏感数据。

### 6.3 锁与 fencing token

- Bootstrap apply/finalize 必须先取得数据库级独占 lease，默认实现使用当前支持数据库的 advisory lock；不支持该能力时 fail closed。
- 取得锁后递增 fencing token；每次更新 execution/control 都带 token 条件，过期进程不能提交结果。
- 资源远程目标命令必须携带 generation、fingerprint 和 fencing token，目标端再次校验，不能只相信调用方。

## 7. 数据模型与事实来源

新增表由 `bootstrap` 模块自己的 Flyway migration 创建，不能由 JPA/MyBatis 运行时自动建表：

### 7.1 `mango_bootstrap_control`

单环境/数据源控制行：

| 字段 | 语义 |
|---|---|
| `environment_key` | 环境与主数据源稳定标识。 |
| `stable_generation` | 已 finalize、仅允许继续运行的最低稳定 generation。 |
| `candidate_generation` | 已 expand、允许新 Runtime 启动的候选 generation。 |
| `authoritative_generation` | 当前唯一允许执行 Resource 写入的 generation。 |
| `candidate_fingerprint` | 候选制品规范化指纹。 |
| `state` | `EMPTY/EXPANDING/EXPANDED/FINALIZING/FINALIZE_FAILED/FINALIZED/FAILED`。 |
| `fencing_token` | Bootstrap 写入栅栏令牌。 |
| 审计字段 | release、revision、操作者、开始/完成时间。 |

### 7.2 `mango_bootstrap_execution`

记录一次 `plan/apply/finalize/abort/verify` 执行：release ID、generation、build revision、manifest fingerprint、action、phase、status、耗时、错误类型/摘要和 fencing token。

### 7.3 `mango_bootstrap_step_execution`

记录步骤 code、step fingerprint、依赖、状态、开始/结束时间、重试序号、结构化结果摘要。它聚合发布过程，但不替代以下事实来源：

- Flyway history table 仍是 DDL 是否执行的唯一事实。
- Resource registry、sync log、change log 仍是资源物化的唯一事实。
- Bootstrap step 表只证明某 release/generation 对这些事实完成了校验。

### 7.4 `mango_runtime_instance`

记录 instance ID、release、generation、fingerprint、启动时间、最后心跳、draining 状态和 lease 到期时间。Finalize 只考虑未过期 lease；任何旧 generation 活跃实例都会阻断 contract。

## 8. Manifest 与 fingerprint

构建产物包含 `META-INF/mango/bootstrap/manifest.json`。fingerprint 对规范化 JSON 做 SHA-256，至少覆盖：

- release ID、build revision 和 generation 输入约束；
- 每个模块 Flyway migration 的 version、description、checksum、phase 和数据源标识；
- 每个 Resource 声明规范化内容、执行阶段、sync mode、module/target 所有权；
- Bootstrap step code、依赖图和 step fingerprint；
- 需要参与回执的租户 provisioner/handler 稳定 ID 与版本。

规则：

- generation 必须单调递增。
- 相同 generation + 相同 fingerprint：允许幂等重试。
- 相同 generation + 不同 fingerprint：制品污染，必须失败。
- 更低 generation：stale，必须失败。
- 更高 generation 但未完成 expand：Runtime 必须失败。
- manifest 内出现未分类 migration、重复 Resource ID、循环步骤或不稳定 provider ID：plan 必须失败。

## 9. Flyway 治理

### 9.1 职责不变

Flyway 继续管理表、列、索引、约束和必要的数据迁移。已执行 migration 不修改、不重命名、不改 checksum；各模块 history table 保持不变。

### 9.2 阶段分类

每个新 migration 必须在 Bootstrap manifest 中分类：

- `EXPAND`：加表、加 nullable 列、兼容索引、双写准备、可向前/向后兼容的数据回填。
- `CONTRACT`：删表、删列、收紧约束、移除旧结构、不可逆清理。

既有已发布 migration 只作为历史事实读取；首次接入 manifest 时按当前已执行状态建立基线，不重新执行。新 migration 未分类时构建和 `bootstrap plan` 均失败。

Flyway executor 从 Spring 自动初始化器中抽出为显式服务：Runtime 配置不注册迁移 initializer；Bootstrap expand/finalize 分别按 manifest 的目标版本执行。仍使用同一模块 history table，不能建立平行的“Bootstrap migration history”。

### 9.3 空库快速基线

当空库逐模块重放历史 migration 明显偏慢时，允许显式启用 `mango.persistence.flyway.cold-baseline`。基线仍按模块和逻辑数据源隔离，不生成全应用单一 SQL：

- 只在除 Bootstrap 自身表之外没有任何用户表的真正空库执行；非空库直接 fail closed。
- 每个启用模块必须随制品提供且只提供一份 `db/baseline/{module}/B{version}__baseline.sql`；`B{version}` 覆盖当前模块的最高 migration 版本。
- 发布候选准备阶段由模块 Owner 从当前 schema 维护并评审该文件；部署现场不拼接、不生成 SQL。
- 每个模块 SQL 成功后，Bootstrap 把该模块原有 history table baseline 到 `B{version}`，再由 `FLYWAY_EXPAND` 执行版本之后的增量。
- 逻辑数据源 key、模块归属、基线版本和 SQL SHA-256 进入 manifest fingerprint；连接地址和凭据不进入。
- 历史 `V*.sql` 继续保留在源码和制品中；已有库不走 fast cold baseline，仍使用原 history 增量升级。
- MySQL DDL 非事务性；快照中途失败可能留下半初始化结构。该状态不得自动 baseline，按本次“旧库数据可丢弃”约束删除并重建空库后重试。

该路径把业务已验证的一分钟级手工 SQL 纳入 Mango 的锁、回执、指纹和失败门禁，不允许部署脚本在框架外直接导入后伪造成功回执。

### 9.4 contract 约束

- finalize 前必须证明旧 generation lease 为 0。
- contract migration 默认必须声明 `onlineSafe=true`；否则要求显式维护窗口参数并保持 Runtime readiness 关闭。
- finalize 失败时不把 generation 标记为稳定；已执行成功的 migration 由 Flyway 记录，后续同 generation 重入继续完成。

## 10. Resource Registry 治理

### 10.1 两个正交维度

新增 `executionPhase`：

| 值 | 语义 | 是否阻断 Runtime |
|---|---|---|
| `BOOTSTRAP_REQUIRED` | 菜单、权限、内置角色、工作流定义等 Runtime 前必须存在的资源 | 是 |
| `RUNTIME_EVENTUAL` | 不影响基础流量，可在当前权威 generation 运行后最终收敛的资源 | 否 |
| `MANUAL` | 只进入 plan，由管理员显式执行 | 否，但 plan/诊断必须展示 pending |

保留现有 `syncMode`：`AUTO / INIT_ONLY / MANUAL / LOCKED`。例如 `BOOTSTRAP_REQUIRED + INIT_ONLY` 表示启动前必须确保初值存在，但用户修改后不覆盖；`RUNTIME_EVENTUAL + AUTO` 表示运行后由当前 generation 持续对账。

不显式声明 `executionPhase` 的现有正式资源在新契约中按 `BOOTSTRAP_REQUIRED` 处理，避免 Runtime 在缺关键资源时接流量。demo 资源继续由最终应用显式启用。

### 10.2 apply 与 finalize

- expand 只执行新增/更新/upsert，不因 manifest 缺失禁用资源。
- `disableMissing`、显式 `REMOVED` 的破坏性物化和物理删除只在 finalize 执行。
- `disableMissing` 只能由 `authoritative_generation` 发起，且请求必须包含 generation、manifest fingerprint、fencing token 和完整 managed module 集合。
- Resource Registry 服务端原子校验 control row；旧实例即使仍持有旧声明，也无法覆盖新 generation。
- Runtime eventual worker 每次批次前重新校验权威 generation；一旦失去权威立即停止写入并转为只读诊断。
- Resource 模块保留 registry/sync/change log；Bootstrap 回执记录本次声明总 fingerprint 和按模块结果摘要。

### 10.3 文件资产 Resource

业务模块需要预置工作流附件、打印模板或其它二进制文件时，使用 `FILE_ASSET` 声明，禁止继续在 Runtime
启动回调中读取 classpath 后调用普通上传接口。声明只包含稳定元数据和制品位置，二进制内容独立打包在业务模块
Jar 的 `META-INF/mango/assets/{module}/` 下：

- `fileId` 是业务代码引用的稳定 `file_record.id`；
- `objectName` 是跨重建保持不变的逻辑对象位置，必须位于 `mango-assets/` 托管前缀；
- `content` 必须是 `FILE` 字段且只允许 `classpath:` 来源；
- `sha256` 是必填的制品完整性校验，不从文件名或构建时间推导；
- `storageConfigId` 引用当前环境的文件存储配置，声明和 fingerprint 不包含 endpoint、AccessKey 或 SecretKey。

`FILE_ASSET` 依赖 `FILE_STORAGE_CONFIG`。Bootstrap 处理器流式读取 classpath 内容并计算 SHA-256，先写入
`.mango-staging/{generation-or-version}/{resourceId}`，校验后由 File 存储适配器发布到固定 `objectName`，最后在数据库
事务中幂等写入 `file_object` 和固定 ID 的 `file_record`。LOCAL 使用临时文件加原子移动；对象存储使用服务端
copy/move 后删除 staging 对象。数据库与对象存储不能组成单一事务，因此重入规则必须覆盖以下中间状态：

- staging 上传失败时不写 `COMPLETED` 元数据；
- 稳定对象已发布而数据库未提交时，下次执行按 `objectName + sha256` 补写元数据；
- 数据库记录存在但稳定对象缺失时重新上传并修复；
- `fileId + storageConfigId + objectName + sha256` 全部一致且对象可读时直接跳过；
- disable missing 只逻辑停用文件记录，默认不物理删除可能仍被历史业务引用的对象。

同一 `fileId` 不允许改变 tenant、storageConfigId 或 objectName。内容变更只有在新内容同时兼容 N 和 N+1 Runtime 时
才允许沿用原 ID 和位置；破坏性文件变更必须声明新的 `fileId` 和 `objectName`，旧资产在 finalize 后再逻辑停用。
这保证滚动发布的 expand 阶段不会让旧实例读到不兼容内容。

## 11. Runtime 与滚动升级协议

### 11.1 Runtime 启动门禁

Runtime 在 readiness 开放前校验：

1. 数据库存在 Bootstrap control 和对应成功回执。
2. 当前 generation 是 stable，或是状态为 `EXPANDED/FINALIZING` 的 candidate。
3. 当前 manifest fingerprint 与回执完全一致。
4. 所有 `BOOTSTRAP_REQUIRED` 步骤成功且无 pending Flyway expand。
5. 同 generation 不存在另一个 fingerprint。

任一失败均输出稳定 reason code 并退出，不退化为 WARN。

### 11.2 滚动升级时序

```text
稳定态：stable=N
  |
  | bootstrap apply --phase=expand --generation=N+1
  v
候选态：stable=N, candidate=N+1, authoritative=N+1
  |  N 旧实例可继续服务，但失去 Resource 写权
  |  N+1 新实例凭 EXPANDED receipt 启动
  |  流量切换并排空 N
  v
bootstrap finalize --generation=N+1
  |  校验 N 活跃 lease=0
  |  Resource missing disable / contract
  |  Flyway contract / post verify
  v
稳定态：stable=N+1, candidate=NULL, authoritative=N+1
```

已运行的 N 实例在候选窗口内允许继续服务，便于正常 drain；新的 N 实例启动会被拒绝。Finalize 后如果仍有异常存活的 N 实例，其周期 gate 必须切换为 `REFUSING_TRAFFIC` 并停止所有框架写任务。

### 11.3 回滚

- finalize 前：停止 N+1，执行 `bootstrap abort` 清除 candidate（保留审计）、恢复 N 流量。`abort` 要求 N+1 活跃 lease 为 0，并递增 fencing token 使候选代旧写令牌失效。Expand DDL 和新资源保留，因为它们按定义必须向后兼容；不执行 down migration。进入 `FINALIZING` 后失败记为 `FINALIZE_FAILED`，只能按相同 generation/fingerprint 续跑 finalize，禁止 abort。
- finalize 后：禁止直接回滚到 N。此时旧结构/资源可能已经删除，只允许发布更高 generation 的前向修复，或按正式备份恢复流程处理数据。
- Bootstrap 失败不会更新稳定 generation；重试必须使用同 generation、同 fingerprint。

## 12. 首次初始化与普通升级

### 12.1 空库

`bootstrap apply --strategy=cold` 取得 generation 1，执行 expand、校验和 finalize。不存在旧实例时 finalize 无需等待 drain。成功退出后才启动 Runtime；容器 healthcheck 不再承担数据库初始化超时。

### 12.2 停机升级

停机后可使用 `strategy=cold` 对下一 generation 一次完成 expand/finalize，再启动 Runtime。该名称表示无并存 Runtime，不表示删除或重建数据库。

### 12.3 滚动升级

必须显式执行 expand 和 finalize 两段，中间由部署系统完成新实例启动、流量切换与旧实例排空。Jenkins/Kubernetes 的超时分别配置给 Bootstrap Job 和 Runtime readiness，互不借用。

### 12.4 快速空库基线

快速空库初始化不在部署现场把全部 migration 动态拼成一份 SQL。每个模块在发布候选准备阶段维护且只维护一份当前基线：

```text
db/baseline/{module}/B{version}__baseline.sql
```

`B{version}` 表示该 SQL 已包含的最高模块 migration 版本。历史 `V*.sql` 继续保留在源码中，用于审计、增量升级和
基线之后的 migration；基线 SQL 进入版本控制、评审和制品 fingerprint，部署阶段只执行已签名制品，不生成 SQL。

Bootstrap 根据 `PersistenceModuleDataSourceResolver` 或模块显式 datasource 配置，将模块按逻辑数据源分组并按
数据源 key、模块 code 的稳定顺序执行。每个数据源在导入任何模块前单独验证为空；随后依次导入该组模块基线，
并以各自 `B{version}` 创建模块独立的 Flyway history。全部逻辑数据源成功后才允许写全局 Bootstrap 回执。
数据源 URL、用户名和密码不进入 fingerprint；fingerprint 只包含逻辑 key、模块归属、基线版本和 SQL SHA-256。

同一逻辑数据源组部分完成后进程失败，重入通过已建立的模块 history 判断完成项；不同逻辑数据源部分成功时全局
回执保持失败。锁按逻辑数据源 key 排序获取，避免并发 Bootstrap 在多库间形成死锁。普通已有库升级不执行基线，
仍走 `V*.sql` 的 expand/finalize 协议。

性能门禁不是只计算 SQL 文本或 Resource 元数据。仓库基准必须真实执行 MySQL DDL/DML、`WORKFLOW_DEFINITION`
到 Flowable 的 BPMN 发布，以及 `FILE_ASSET` 到文件存储层的二进制写入和 SHA-256 回读。当前 5 倍保函参考负载为
5 模块、375 表、37,500 行、16,372,270 SQL 字节，以及 1,255 条 Resource、75 MiB 文件和 20 个八级审批流程。
Resource 规模以保函只读统计的 232 个声明、4 个启动发布 Workflow、15 个启动物化文件为基准，三个维度分别达到 5 倍，不能用普通声明填充量替代 Workflow/File 覆盖。MySQL 8.4 实测 SQL 2.267 秒、完整 Bootstrap Resource 冷注入 13.049 秒、同代热重入 53 毫秒，均低于一分钟目标。

## 13. 安全、租户与多数据源

- Bootstrap 是运维入口，不暴露公共 HTTP Controller；执行身份、release 和 revision 写入审计字段。
- 数据库账号至少具有本次 plan 所需只读权限；apply/finalize 使用明确的 migration/resource 写权限。
- Resource 和租户步骤逐租户执行，失败摘要包含 tenant ID，但不记录租户 secret 或资源敏感字段。
- 多数据源的每个 migration group 都有独立步骤和 fingerprint；主 control row 只在全部必需 group 成功后提交全局回执。
- 部分数据源成功、部分失败时允许同 fingerprint 重入；禁止用新 fingerprint 覆盖未完成 generation。

## 14. 可观测性与健康语义

Bootstrap 日志统一包含：`executionId/releaseId/generation/phase/stepCode/fencingToken/duration/status`。提供稳定退出码和 JSON 计划/结果文件，方便 Jenkins 归档。

Runtime Actuator 增加：

- `bootstrapReceiptHealthIndicator`：回执、generation、fingerprint 和 pending step。
- `runtimeGenerationHealthIndicator`：stable/candidate、当前实例 generation、lease 和 draining 状态。
- Resource eventual 状态独立展示，不再把非阻断资源同步映射为整个应用 `OUT_OF_SERVICE`。

liveness 只表示进程存活；readiness 表示当前实例是否允许接流量。Bootstrap 进程不使用 Runtime healthcheck。

## 15. CI/CD 接入

Jenkins 发布顺序改为：

```text
拉取同一 digest 镜像
-> docker compose run --rm api bootstrap plan
-> docker compose run --rm api bootstrap apply (--strategy=cold 或 --phase=expand)
-> 启动/滚动更新 api runtime
-> 检查 Runtime readiness 与业务 smoke test
-> rolling 模式排空旧实例
-> docker compose run --rm api bootstrap finalize
-> 最终 verify
-> 启动前端或标记发布成功
```

健康探针仍可使用 `/actuator/health/readiness`，但不再通过放宽 `start_period` 掩盖初始化耗时。当前脚本中对 API 容器内部 `127.0.0.1:5577/actuator/health` 的 curl 形式本身可用，问题是它检测了包含 Bootstrap 工作的 Runtime 容器；架构拆分后该探针只负责 Runtime。

## 16. 验收矩阵

| ID | 场景 | 预期结果 |
|---|---|---|
| AC-001 | 空 MySQL 执行 cold bootstrap | 全部模块 migration、Resource required、租户对账成功；生成 FINALIZED receipt；进程退出 0。 |
| AC-002 | 未 Bootstrap 直接启动 Runtime | 以稳定 reason code 快速失败，不开放 readiness。 |
| AC-003 | 同 generation、同 fingerprint 重跑 | 已成功步骤幂等复用，失败步骤继续执行，不重复破坏数据。 |
| AC-004 | 同 generation、不同 fingerprint | plan/apply/runtime 全部 fail closed。 |
| AC-005 | N 到 N+1 rolling expand | N 继续服务；N+1 可以启动；只有 N+1 能写 Resource；missing 不被禁用。 |
| AC-006 | N 未排空执行 finalize | finalize 被旧 lease 阻断，不执行 destructive Resource/DDL。 |
| AC-007 | N 排空后 finalize | missing Resource 和 contract migration 执行，stable 切到 N+1。 |
| AC-008 | finalize 后启动 N | stale generation 被拒绝；异常存活 N 进入拒绝流量状态。 |
| AC-009 | Resource `INIT_ONLY` 被用户修改 | Bootstrap 验证存在性但不覆盖用户值。 |
| AC-010 | `RUNTIME_EVENTUAL` 失败 | Runtime 保持 ready，独立 health/detail 告警；当前 generation 最终重试收敛。 |
| AC-011 | `BOOTSTRAP_REQUIRED` 失败 | 不写成功 receipt，Runtime 无法启动。 |
| AC-012 | Bootstrap 中途失锁/进程被杀 | 旧 fencing token 无法提交；同 fingerprint 新进程可安全续跑。 |
| AC-013 | 多数据源部分成功 | 全局 receipt 不成功；重入只补未完成步骤。 |
| AC-014 | finalize 前回滚 N+1 | N 可恢复流量，新结构/资源保留且兼容；不执行 down migration。 |
| AC-015 | 真实 Baohan 空库基线 | 初始化由独立 Bootstrap Job 完成；Runtime 在预期短窗口内 ready；Jenkins 不再因 7 分钟初始化把 API 判 unhealthy。 |

## 17. 实施范围与一次性交付顺序

虽然交付一次完成，代码实施和验证仍按依赖顺序推进：

1. 新建 Bootstrap API/core/starter、入口、执行表、manifest/fingerprint、锁和 Runtime gate。
2. 将 Persistence Flyway 从自动初始化器抽为显式 expand/contract contributor。
3. 为 Resource 增加 execution phase、generation/fingerprint/fencing，并拆分 apply/finalize/eventual。
4. 将 System 租户前置/最终对账迁入 Bootstrap contributor，移除 Runtime 启动编排。
5. 增加 runtime lease、rolling finalize 门禁、Actuator 和诊断。
6. 更新 BOM、最终应用模板、模块 README、能力地图、业务接入指南和 Jenkins 示例。
7. 完成单元、MySQL 8.4 集成、空库、已有库升级、并发 Bootstrap、滚动 N/N+1、回滚和 Baohan 消费验证。

本交付不以“阶段 1 可用”作为完成；只有 AC-001 至 AC-015 和仓库质量门禁全部通过，才算三项治理能力完成。

## 18. 主要风险与控制

| 风险 | 控制 |
|---|---|
| Bootstrap context 误启动业务服务 | `WebApplicationType.NONE`、模式条件、lazy init、架构测试禁止 Bootstrap 模式注册 Web/worker/scheduler。 |
| 旧实例覆盖新资源 | Runtime 不执行 required sync；所有 Resource 写命令服务端校验 generation/fingerprint/fencing。 |
| migration 被错误分类 | 构建期 manifest 检查 + plan fail closed + contract 必须等待旧 lease 为 0。 |
| lease 假死阻塞 finalize | TTL + instance ID + 可审计的人工 force 参数；force 默认关闭并要求维护窗口。 |
| expand 后无法回滚应用 | expand 强制向后兼容；不满足时只能选择停机策略，不伪装成 rolling。 |
| Bootstrap 表自身尚不存在 | Bootstrap module migration 作为固定前导步骤；首次执行先用数据库 advisory lock，再创建/校验控制表。 |
| 外部业务仍调用 `SpringApplication.run` | 编译/架构门禁扫描最终应用入口并阻断；业务升级指南给出唯一替换方式。 |

## 19. 评审结论请求

本设计请求确认以下不可逆契约：

1. 接受 Runtime 无有效 Bootstrap receipt 时直接失败。
2. 接受所有最终应用切换到 `MangoApplication.run`，不保留旧自动初始化路径。
3. 接受滚动发布必须分为 expand、Runtime 切换、finalize；冷启动可用一个命令串行完成。
4. 接受 finalize 后禁止直接回滚到旧 generation，只能前向修复或走备份恢复。
5. 接受 Resource `executionPhase` 与 `syncMode` 分离，missing disable 只在 finalize 执行。

上述五项确认后进入开发 preflight、正式实施计划与代码实现。
