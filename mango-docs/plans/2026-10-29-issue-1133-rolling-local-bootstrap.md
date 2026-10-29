# Issue #1133 本地增量启动修复

## 先读

- **目标**：Resource 声明变化后，`mango dev start` 和 IDEA 无参数 `MangoApplication.run` 保留已有本地数据库数据，并通过 rolling apply + finalize 启动新 generation。
- **场景**：已有 stable generation 的本地工作区修改 Resource 声明后再次启动。
- **输入/输出**：输入为本地 workspace 数据库和新的 manifest fingerprint；输出为新的 stable generation 和 runtime identity。
- **成功/失败边界**：fingerprint 未变化时复用 stable generation；变化时 rolling apply 后 finalize；旧 Runtime lease 未排空时 finalize 失败且不启动新 Runtime。
- **不处理**：生产发布流程、`SpringApplication.run` 兼容直启和 Bootstrap 状态机本身的生产语义。
- **阅读顺序**：本记录 -> CLI/Starter 实现 -> 定向测试结果。

## 1. 元数据

- 任务 ID：ISSUE-1133
- 交付模式：STANDARD
- 需求影响：L2 - 本地开发启动和已有数据库数据保留行为变化
- 方案风险：L2 - CLI 与 Java 本地入口共同编排 generation 状态
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：修复 fingerprint mismatch 后错误执行 cold bootstrap 的问题。
- 成功条件：空库仍 cold；已有 stable generation 且 fingerprint 变化时两条本地入口都执行 rolling apply + finalize；无变化仍复用 stable generation。
- 处理范围：`mango-cli` 本地生命周期、`BootstrapCommandRunner` 本地 main 路径、定向测试和能力说明。
- 不处理范围：生产显式 bootstrap/runtime 命令、数据库 schema/state machine 设计和非 Mango 生命周期项目。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-1133-001 | `mango dev start` | 空 workspace 数据库 | generation 1 cold apply 后启动 runtime | bootstrap 失败则不启动 runtime | CLI 回归测试确认 cold strategy |
| REQ-1133-002 | `mango dev start` | stable generation，fingerprint 变化 | 新 generation rolling apply + finalize 后启动 runtime | 旧 Runtime 活跃时 finalize 失败且不启动 runtime | CLI 回归测试确认命令顺序和 strategy |
| REQ-1133-003 | IDEA 无参数 `MangoApplication.run` | stable generation，fingerprint 变化 | rolling apply + finalize 并发布新 runtime identity | 任一步失败则启动失败 | Starter 单元测试确认 APPLY/FINALIZE 和 generation |
| REQ-1133-004 | 两条本地入口 | stable fingerprint 未变化 | 复用 stable generation | 不重复执行 bootstrap step | 现有 CLI/Starter 回归保持通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-1133-001 | REQ-1133-001/002 | CLI 仅对 stable fingerprint mismatch 切换 rolling；空库和失败 candidate 继续 cold | `prepareManagedMangoLifecycle`、bootstrap 参数构造 | 回退 CLI 变更，数据库 stable generation 不变 |
| DEC-1133-002 | REQ-1133-002 | rolling apply 后显式执行 `bootstrap finalize --mango.bootstrap.strategy=rolling`，复用现有旧 Runtime lease 门禁；`FINALIZE_FAILED` 的同 fingerprint candidate 下次启动直接重试 finalize | CLI -> BootstrapCommandRunner -> JdbcBootstrapRepository | 保留 candidate，停止旧 Runtime 后重新 finalize |
| DEC-1133-003 | REQ-1133-003 | Java 本地 main 与 CLI 使用相同 stable fingerprint 判断和 rolling 两阶段调用 | `BootstrapCommandRunner.runLocalStartup` | 回退 starter 变更，生产显式入口不受影响 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-1133-001 | DEC-1133-001 | 1 | `mango-ui/packages/mango-cli/src/index.mjs` | 策略参数化并实现 rolling apply/finalize |
| TASK-1133-002 | DEC-1133-003 | 2 | `BootstrapCommandRunner.java`、`MangoApplication.java` | stable fingerprint 变化使用 rolling 两阶段流程，移除本地入口的固定 cold 默认 |
| TASK-1133-003 | DEC-1133-001/002/003 | 3 | CLI/Starter 测试 | 覆盖空库、candidate、stable fingerprint 变化 |
| TASK-1133-004 | DEC-1133-002 | 4 | CLI/Bootstrap README、Issue #1017 设计文档 | 生命周期说明与实现一致 |
| TASK-1133-005 | REQ-1133-002 | 5 | `BootstrapResourcePerformanceIntegrationTest` | 真实 MySQL 验证 Resource 声明变化的 rolling apply/finalize 与数据保留 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-1133-001 | CLI 检查脚本 | `node mango-ui/packages/mango-cli/scripts/check-cli.mjs` | PASS | 覆盖空库、失败 candidate、stable fingerprint drift |
| REQ-1133-002 | CLI 检查脚本 | 同上，rolling finalize 与 active old Runtime 场景 | PASS | call log 断言 apply/finalize/runtime 顺序 |
| REQ-1133-003 | Starter 定向测试 | `mvn -f mango/mango-infra/mango-infra-bootstrap/pom.xml -pl :mango-infra-bootstrap-starter -am -Dtest=BootstrapCommandRunnerTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | 5 tests, 0 failures；包含 blocked finalize 重试 |
| REQ-1133-004 | Starter 全量模块测试与 CLI receipt 测试 | `mvn -f mango/mango-infra/mango-infra-bootstrap/pom.xml -pl :mango-infra-bootstrap-starter -am -DskipTests=false test`；`node --test mango-ui/packages/mango-cli/tests/dev-bootstrap-receipt.test.mjs` | PASS | Starter 19 tests, 0 failures；CLI 4 tests, 0 failures |
| REQ-1133-005 | 真实 MySQL 集成验收 | `mvn -f mango/mango-admin-starter/pom.xml -Dtest=BootstrapResourcePerformanceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | MySQL 8.4；1 test, 0 failures；workflow generation 1 -> 3 rolling apply/finalize；fileState 保持不变；最终控制状态 `FINALIZED`；证据见 `mango-docs/evidence/2026-10-29-issue-1133-real-mysql-acceptance.md` |

## 7. 决策复核

- 事实与用户视角：本地已有业务数据不能因 Resource 声明变化被 cold baseline 覆盖；两条本地入口应保持一致。
- 技术与风险视角：rolling finalize 继续复用数据库对旧 Runtime lease 的门禁，不绕过状态机；旧实例活跃时保持 candidate，不启动新 Runtime。
- 验证与交付视角：CLI 命令顺序、Java invocation strategy/generation 和空库兼容性分别由定向测试覆盖。
- 外部同行评审：未指定，按 `AGENT_ONLY` 继续；保留测试输出和审查事实供 PR review。

## 8. 例外与剩余风险

- 已完成真实 MySQL 验收；未覆盖真实 IDEA 启动进程的端到端人工启动，仍需在 PR/发布环境补充入口级验证。
