# Issue #851 Resource Bootstrap 实施与验收台账

## 1. 基线

- 设计：`mango-docs/designs/2026-08-25-issue-851-resource-bootstrap-design.md`
- 源码基线：`origin/main@56eaf0b2703055b5aa5f80f1b60492468905b7c1`
- 风险/模式：L3 / FULL
- 授权：本地实现、测试和验证；不含 Commit、Push、PR、发布或环境操作。

## 2. 原子交付项

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| IMPL-001 | AC-001、AC-002、AC-005 | 稳定模块 manifest/hash | 对规范化模块声明与文件内容计算稳定 SHA-256 | Resource support/sync starter 源码与测试 | 单元测试、Boot JAR consumer | DONE | `mango/mango-platform/mango-resource/mango-resource-sync-starter/src/test` |
| IMPL-002 | AC-003 | 持久化模块 receipt | 以环境、应用、服务、模块为唯一键记录成功状态 | Resource core migration/repository | H2 隔离数据库集成测试 | DONE | `mango/mango-platform/mango-resource/mango-resource-core/src/test` |
| IMPL-003 | AC-001、AC-002 | 未变化模块整模块跳过 | receipt 命中先于内部声明反序列化 | Resource core service | SQL、Handler 和日志计数集成断言 | DONE | `mango/mango-platform/mango-resource/mango-resource-core/src/test` |
| IMPL-004 | AC-003、AC-004 | 变化模块 EXPAND/FINALIZE 状态机 | 同一租约内按依赖拓扑协调，成功后推进 receipt | Resource core service | 失败、重试、缺失依赖、循环依赖和删除隔离测试 | DONE | `mango/mango-platform/mango-resource/mango-resource-core/src/test` |
| IMPL-005 | AC-006 | 文件对象清单与 staged publish 契约 | 构建期内容寻址打包，部署端复用 FileStorage staged publish | Resource artifact writer、FILE_ASSET handler 与 Local/MinIO 性能测试入口 | 真实 Local 与真实 MinIO 均通过对象大小和 SHA-256 回读校验 | DONE | `mango/mango-admin-starter/src/test/java/io/mango/admin/starter/BootstrapResourcePerformanceIntegrationTest.java` |
| IMPL-006 | Issue #807、AC-008 | sealed release manifest 引用资源物料 digest | 复用 Maven candidate JAR size/SHA-256，不建立第二事实源 | 既有 sealed Maven manifest 与 Boot JAR 内物料 | Issue #807 manifest 契约、Invoker JAR 包含性 | DONE | `mango/mango-platform/mango-resource/mango-resource-sync-starter/src/it` |
| VERIFY-001 | AC-007 | 1291 条真实性能目标 | 在一次性 MySQL 8.4 空库执行 cold/warm、单模块变化、失败恢复与并发 Bootstrap | Admin Starter 性能集成入口 | Local cold 9,784 ms、warm 93 ms；MinIO cold 9,402 ms、warm 93 ms | DONE | `mango/mango-admin-starter/src/test/java/io/mango/admin/starter/BootstrapResourcePerformanceIntegrationTest.java` |
| VERIFY-002 | M09、M10、M11 | 修改模块和消费者质量门禁 | 验证直接修改模块、Authorization/File 消费者、最终 JAR 和 fileproc 正常 reactor | Maven verify、Invoker、PMO 静态门禁 | 最终工作区 Maven/PMO 命令 | DONE | `mango-docs/plans/2026-08-25-issue-851-resource-bootstrap-plan.md` |

## 3. 完成条件

全部条目为 DONE 或有经用户确认的 EXCEPTION；测试失败、未验证的真实数据库/存储语义或性能目标不能标记完成。

## 4. 本地验收证据

### 4.1 自动化测试

- Resource Support/Core/Sync Starter 最终 `verify`：Support 17、Core 67、Sync Starter 28，合计 112 条通过；其中主要 Resource Registry 集成类 40/40。
- 构建物 Loader/Writer：8/8；覆盖不透明声明加载、未知 schema、损坏结构、不可读 JSON、确定性内容寻址、checksum 错误无半成品、unsafe path 和内容去重。
- `FileAssetResourceHandlerTest`：15/15；覆盖稳定身份、发布失败清理 staging、checksum/位置漂移、外部资产根目录与越界/符号链接防护。
- File Core：80/80；其中 `FileServiceConcurrentSaveIntegrationTest` 使用真实一次性 MySQL 8.4.8，3/3 通过。
- Authorization consumer：新增 repository 后，`FrontendRuntimeResourceSyncIntegrationTest` 2/2 通过。
- Maven Invoker consumer：1/1；`process-classes` 生成 manifest/files bundle，最终 Spring Boot JAR 内 entry、manifest 和对象字节一致。
- fileproc 从根正常选择 starter 并 `-am verify`：11 个 reactor 模块全部成功，Core 64/64、Starter 17/17。完整 reactor 先前以 `-rf` 恢复时出现的 `PdfToOfdConvertProvider` `NoClassDefFoundError` 已证实为恢复命令未带齐前序当前 classpath，不是 #851 回归。
- Test quality：10 个本次相关测试文件全部通过；mock audit 仅 report-only，仓库既有全局 mock 债务不作为本次真实落库证据。

### 4.2 真实 Local + MySQL 8.4.8 生命周期

数据规模为 3 个模块、1291 条 Resource、4 个 Workflow、15 个文件、35,651,584 bytes：

| 阶段 | 耗时 |
|---|---:|
| schema setup | 1,722 ms |
| cold bootstrap | 9,784 ms |
| warm bootstrap | 93 ms |
| 新 generation 同内容 | 141 ms |
| Workflow 单模块变化 | 512 ms |
| File 单模块变化与故障恢复 | 791 ms |
| 同 generation 并发 APPLY | 481 ms |

逐对象大小与 SHA-256 回读通过；覆盖 Local 稳定路径故障、staging PUT 成功后 publish 失败、修复后同 generation 恢复，staging 最终为空。并发执行收敛为 executed steps 3 和 0；旧 generation VERIFY 与同 generation fingerprint 漂移均 fail closed。

### 4.3 官方 MinIO + MySQL 8.4.8 生命周期

- MinIO：`RELEASE.2025-10-15T17-29-55Z`，commit `9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a`。
- 官方 Homebrew bottle SHA-256：`f8e0395a3145bc094c61b7da79df655d708d9c610b8aec82d765fdeca940d6cd`。

| 阶段 | 耗时 |
|---|---:|
| schema setup | 1,868 ms |
| cold bootstrap | 9,402 ms |
| warm bootstrap | 93 ms |
| 新 generation 同内容 | 120 ms |
| Workflow 单模块变化 | 494 ms |
| File 单模块变化与故障恢复 | 541 ms |
| 同 generation 并发 APPLY | 474 ms |

15 个对象全部按大小和 SHA-256 回读；changed file 覆盖、staging 清空、并发收敛及 generation/fingerprint fail closed 均通过。凭据仅存在于一次性本地进程环境，未写入源码或文档。

### 4.4 失败发现与修复

1. Local 真实故障注入首次发现稳定对象发布失败后 staging 残留；`FileAssetResourceHandler.publish()` 已补充失败清理，并验证清理失败作为 suppressed exception 保留。
2. 完整 reactor 首次发现 Authorization 手工 Spring 测试配置缺少 `ResourceModuleReceiptRepository`；测试配置已补齐并定向通过。
3. File Core 完整测试要求 `${MANGO_DB_*}`；提供专用真实 MySQL 数据库后 80/80 通过，不将缺少外部测试环境误记为代码失败。

### 4.5 边界

- `statObject` 未实现：它是 Issue 中建议的下载优化，不是正确性前提；当前 Handler 仍下载并校验对象 SHA-256。
- sealed release manifest 未新增脚本：Issue #807 已逐 Maven candidate JAR 记录 size/SHA-256，JAR 内物料由外层 digest 密封。
- `ResourceSyncAutoConfigurationTest` 中 Mapper/JdbcTemplate/ILeaseLocker test double 只验证 Spring 自动配置装配；落库结论来自 Core 隔离数据库和真实 MySQL 生命周期。
- 未测试真实公有云 OSS/COS/Kodo 的 IAM、TLS、区域 endpoint、限流与网络故障；本地 MinIO 只验证 S3-compatible 路径，不代表云厂商生产验收。
- 不含生产部署、发布、Commit、Push、PR 或 Merge。

### 4.6 最终门禁与清理

- `test-quality-check --base origin/main`：PASS，10 个相关测试文件。
- workspace layout、5 份 business guide、capability docs（41 个变更文件）、module README、README source facts：全部 PASS。
- delivery contract：8/8 DONE、0 EXCEPTION；测试替身由 test-quality 专项审计，契约扫描覆盖 TODO/FIXME/伪代码/延期标记。
- `git diff --check`：PASS；新增差异未包含凭据值。`declaredSha256` 的 `null` 是未提供合法可选 checksum 的领域语义，不是空实现。
- 两个专用数据库 `mango_issue_851_bootstrap_resource_perf`、`mango_issue_851_file_core_concurrency` 已删除并回读为空；MySQL 和 MinIO 已正常退出，端口 33385、19385、19386 均关闭。
- `.runtime/issue-851-mysql` 与 `.runtime/issue-851-minio` 已移入系统废纸篓，可恢复；工作区 `.runtime` 不再包含本任务运行数据。

## 5. 1.0.42 Resource 增量跟进

### 5.1 基线与授权

- 源码基线：`origin/main@557807086e1f9c2792a1ef7f36f37effd577cd69`。
- 工作区：`M01=REUSE`，`/Users/hardy/Work/mango-issue-851-runtime`，分支 `feat/issue-851-resource-incremental-release`。
- 授权：实现、文档、静态验证、Commit、当前任务分支 Push 和创建 PR；不含 Merge、发布或部署。

### 5.2 交付项

| ID | 要求 | 交付物 | 状态 |
|---|---|---|---|
| IMPL-007 | 变化模块内按 Resource hash 只调度变化声明 | Resource Registry changed-only 编排与依赖重放移除 | DONE |
| IMPL-008 | 完整批次只作关系解析上下文 | `upsertBatchWithContext`、远程 Command/VO、`AUTH_MENU`/`API_RESOURCE` changed-only 实现 | DONE |
| IMPL-009 | 后台修改后增量发布退避 | `SYSTEM_CONFIG.updated_at` 与 Registry `last_sync_time` 同步标记、`PRESERVED` 结果 | DONE |
| VERIFY-003 | 覆盖未变依赖、Registry 不推进、配置退避和 Authorization changed-only | Core/System 集成测试与 Authorization 单元测试资产；测试源码编译通过，本地执行受当前 Agent `simple` Skill 限制，剩余风险交由 PR CI 回读 | EXCEPTION |
| DOC-001 | 公开能力、System 用法、设计、台账和业务排障说明同步 | Resource/System README、能力地图、本设计、台账及菜单/按钮/租户配置业务指南 | DONE |

### 5.3 当前边界

- 模块 hash 与安装状态写入 `resource_module_receipt`；逐 Resource canonical hash 与上次成功同步时间写入 `resource_registry.source_hash`、`resource_registry.last_sync_time`。
- `PRESERVED` 只写 `resource_sync_log`，不推进逐 Resource hash/同步时间；模块 receipt 可以完成当前发布，后续模块内容变化时该 Resource 会再次进入判断。
- 当前只有 `SYSTEM_CONFIG` 启用通用单行 `updated_at` 退避。`AUTH_MENU`、`API_RESOURCE` 只保证未变化 Resource 不写；多表/多行资源由具体 Handler 决定受管状态。
- 首次 cold apply 仍执行 `BOOTSTRAP_REQUIRED` Resource；现有 cold baseline 不包含 Handler 物化状态。本 Issue 继续保持“部分完成”，不声明重置发布的完整 Resource baseline 已落地。

### 5.4 本地验证

- 直接修改模块 `mvn verify -DskipTests`：Resource API/Support/Core/Remote Starter、Authorization Core/Starter、System Core 共 7 个模块全部 `SUCCESS`；主源码与测试源码编译通过，测试执行按当前 Agent `simple` Skill 约束跳过。
- `audit-module-readmes.mjs`：PASS。
- `audit-readme-source-facts.mjs`：PASS。
- `check-capability-docs.mjs --base origin/main --head HEAD`：PASS，覆盖 Resource/System README、能力地图和三份业务集成指南。
- `workspace-layout-check.mjs --root .`：PASS。
- 既有 #851 `delivery-contract-check.mjs --mode verify`：8/8 DONE、0 EXCEPTION。
- `git diff --check`：PASS。
