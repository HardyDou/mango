# Admin Starter 历史债务与零源码 Reactor 门禁治理记录

## 1. 元数据

- 任务 ID：ADMIN-STARTER-DEBT-20260717
- 状态：IN_PROGRESS
- 交付模式：FULL（架构门禁修改自身，使用单份治理记录，不生成无关产品文档）
- 需求影响：L2，后端聚合 starter 是官方单体和业务项目 full preset 的公共装配入口。
- 方案风险：L3，方案同时修正自托管 Maven 架构门禁对零 Java 源 partial Reactor 的行为。
- 最终风险：L3
- 工作区决策：REUSE，继续使用 `refactor/admin-starter-debt` 独立 worktree。
- 非降级事实：平台架构门禁修改自身。

## 2. 目标与范围

- 目标：清零 `mango-admin-starter` 的依赖架构债务，并让纯依赖聚合模块在 partial Reactor 中得到真实、可审计的架构验证。
- 成功条件：聚合入口只直接依赖本地运行时 starter；零 Java 源 Reactor 仍检查 Maven 依赖并生成报告；官方单体从全新数据库真实启动；能力说明和验收证据与最终源码一致。
- 处理范围：`mango-admin-starter` POM/测试/README，Maven dependency architecture rule、Architecture/Check Mojo 及测试，CLI 前置命令日志输出，单体消费入口验证，能力地图、经验文档和验收证据。
- 不处理范围：平台业务 API、页面、权限语义、各被聚合 starter 的内部实现、AI `/ai/sse` Realtime 迁移（已登记 Issue #567）。

## 3. 可观察要求

| ID | 入口 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|
| GOV-AC-001 | `mango-admin-starter` POM | 直接生产依赖全部是本地 `*-starter`，不依赖 common/api/core/support/starter-remote/app | 任一越层或远程依赖使架构门禁失败 | 依赖规则和聚合契约测试通过，报告 dependency issues 为 0 |
| GOV-AC-002 | 零 Java 源 partial Reactor | 继续执行 Maven 依赖检查并生成 schema v2 报告 | 不得以 ENGINE-005 提前退出，也不得静默跳过非法依赖 | 正例生成 0 Java 问题报告；反例保留依赖阻断 |
| GOV-AC-003 | Admin Starter JAR | 只包含 Maven/JAR 元数据和 `module.properties`，不携带 Java 类、application 配置、migration 或 seed | 出现任一运行实现或宿主配置即失败 | 构件契约测试和 JAR 清单通过 |
| GOV-AC-004 | 官方单体宿主 | 只通过 `mango-admin-starter` 聚合平台依赖，在独立 Fresh DB 上完成迁移、资源初始化并健康启动 | Bean 缺失、迁移失败、模块元数据冲突或健康检查非 UP 均失败 | 真实进程、数据库和 HTTP 入口证据通过 |
| GOV-AC-005 | 对外说明 | README 的依赖清单、边界、配置与验证入口和源码一致 | 文档遗漏或声称不存在的能力即失败 | README 审计、能力文档合同和验收基线通过 |
| GOV-AC-006 | CLI 大型 Reactor 前置安装 | 安装 stdout/stderr 直接写 app 日志，不受进程内默认缓冲上限影响 | 安装成功却因输出过大被 CLI 误判失败 | 生成 1280 KiB 输出后仍进入 Spring Boot 启动命令；真实完整 Reactor 启动通过 |

## 4. ADR：零源码聚合模块的架构验证

### 决定

当 Reactor 没有 Java 源目录时，Architecture Mojo 仍完成 Maven 依赖检查、模块归属和报告生成；Java bytecode、PMD 和命名空间检查以空输入得到 0 项。`mango-admin-starter` 在依赖规则中作为唯一官方本地 starter 聚合入口，只允许依赖 `ModuleRole.STARTER`。

聚合 `mango:check` 只把含 Java compile source 的 Reactor 项目委托给 PMD、Checkstyle、SpotBugs；整个选中 Reactor 没有 Java 源时明确结束静态委托，不回退扫描全仓。CLI 的同步前置安装命令将 stdout/stderr 文件描述符直连 app 日志，保留退出码语义，同时避免 Node.js 收集完整 Maven 输出。

### 备选与取舍

- 不采用增加无意义 marker class：它只为绕过门禁制造生产代码，不能证明聚合依赖合法。
- 不采用 `skip`、exclude 或 suppression：这会同时跳过需要阻断的非法依赖。
- 不把聚合 JAR 改成 POM packaging：现有消费者按普通 Maven dependency 引入 JAR，改变 packaging 会破坏接入契约。
- 选择“依赖规则继续执行、Java 引擎空输入”：与 full Reactor 中无源码模块的既有语义一致，同时保留 fail-closed 依赖边界。

### 失败模式与恢复

- 若错误放宽普通 starter，正反例必须阻断；规则只按精确官方聚合 artifact 识别。
- 若报告无法归属零源码模块，schema v2 模块清单测试必须失败。
- 回滚方式：整体回滚规则、Mojo、Admin POM和测试提交，不涉及数据回滚。

## 5. 实施清单

| ID | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|
| GOV-TASK-001 | 1 | `MavenDependencyChecker` 与测试 | Admin 聚合正例通过，common/core/remote 反例仍失败 |
| GOV-TASK-002 | 2 | `ArchitectureMojo` 与测试 | 零源码 Reactor 不再 ENGINE-005，依赖问题仍进入报告和 gate |
| GOV-TASK-003 | 3 | `mango-admin-starter` POM、测试、README | 多余依赖移除，聚合与构件合同自动化 |
| GOV-TASK-004 | 4 | 单体入口与 Fresh DB 验证 | 真实进程健康、迁移和模块注册通过 |
| GOV-TASK-005 | 5 | 能力地图、经验和 evidence | 当前能力、命令、结果、例外完整 |
| GOV-TASK-006 | 6 | CLI 前置命令与回归测试 | 大输出直接写日志，超过历史缓冲阈值仍进入服务启动 |
| GOV-TASK-007 | 7 | PR 与外部回读 | Required checks 全绿并回读合并结果 |

## 6. 验收映射与结果

| 要求 ID | 措施 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| GOV-AC-001 | M09/M10 | Architecture rules/plugin 与 Admin Starter 定向测试 | PASS | 规则正反例、Admin 5 条契约测试、真实 partial Reactor dependency issues=0 |
| GOV-AC-002 | M09/M10/M14 | Mojo 正反例、源码复核、真实 partial Reactor | PASS | dependency-only Reactor 单测和真实 verify；Java 引擎 0 问题、blocking=0 |
| GOV-AC-003 | M09/M10 | Admin Starter 构件契约测试与 `jar tf` | PASS | JAR 仅含 Maven/JAR 元数据和 `META-INF/mango/module.properties` |
| GOV-AC-004 | M11/M12 | Fresh DB 启动官方单体，回读 health 与模块入口 | PASS | `mango-docs/evidence/baselines/admin-starter/latest/acceptance.md` |
| GOV-AC-005 | M08/M09 | README 审计、能力合同 | PASS | POM 顺序依赖清单由测试锁定；能力/经验文档已更新 |
| GOV-AC-006 | M09/M10/M11 | CLI 大输出回归和真实完整 Reactor 启动 | PASS | 1280 KiB 自动化场景与 54 MiB app 日志真实启动均通过 |
| GOV-AC-001..006 | M14 | 完整 Reactor 门禁与源码复核 | PASS | 211/211 Reactor；架构 blocking=0；静态 new=0、tool failure=0；未扩 baseline |
| GOV-AC-001..006 | M15 | PR Required checks 和合并状态回读 | PENDING | GitHub PR |

## 7. 例外与剩余风险

- UI 未变化，不启用 M13；业务页面行为由已完成的平台模块治理基线覆盖。
- 本次不发布 Maven 新版本；合并后是否进入发布批次由独立发布任务决定。
- 当前 full-reactor changed 报告仍列出已登记 baseline 存量，但 `blockingIssues=0`；本任务不修改债务预算。
