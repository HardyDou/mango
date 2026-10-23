# 标准交付记录：Issue #1106 同一 generation 重复 finalize 幂等

> **写作与决定门禁：** 遵循 [`Agent 文本输出规范`](../../mango-pmo/rules/13-agent-text-output.md)。交付决定按 [`决策复核规范`](../../mango-pmo/rules/14-decision-expert-review.md)记录，本轮评审选择为 `AGENT_ONLY`。

## 1. 元数据

- 任务 ID：Issue #1106
- 交付模式：STANDARD
- 需求影响：L2 - 改变 Bootstrap 发布状态机对已稳定 generation 的响应，影响发布结论的可观察行为
- 方案风险：L1 - 单个方法内新增 guard，无新抽象、新配置、新依赖，回滚为删除该分支
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-1106`，分支 `fix/issue-1106-finalize-idempotent`）

## 2. 目标与范围

- 目标：同一构建版本（同 generation、同 fingerprint）重复执行 `bootstrap finalize` 返回成功，不再报 `BOOTSTRAP_CANDIDATE_MISSING` 并进入人工接管。
- 成功条件：重复 finalize 返回 `FINALIZED`、执行 0 步、不写 `mango_bootstrap_control` 和 `mango_bootstrap_execution`；内容不同的同 generation 请求仍失败。
- 处理范围：`mango-infra-bootstrap-core` 的 `finalizeCandidate`、对应单测与真库集成测试、`mango-infra-bootstrap/README.md` 的 apply/finalize 语义与排障表。
- 不处理范围：ABORT 的对称缺口、`disableMissing` 的 `tenantId` 问题、`mango-deploy-shell` 引擎侧行为、已失败环境的现场恢复。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | 发布引擎 `bootstrap finalize` | control 已 `FINALIZED`、`stable_generation == 请求 generation`、fingerprint 相同、`candidate_generation IS NULL` | 返回 `FINALIZED`，执行 0 步，不改控制表和执行记录 | 无失败 | 真库用例断言控制表行与 execution/step 计数不变 |
| R2 | 发布引擎 `bootstrap finalize` | 同 generation 但 fingerprint 不同 | 拒绝执行 | `BOOTSTRAP_GENERATION_ALREADY_RELEASED: generation=N` | 真库用例断言控制表不变 |
| R3 | 发布引擎 `bootstrap finalize` | generation 既非 stable 且无 candidate | 保持原行为拒绝 | `BOOTSTRAP_CANDIDATE_MISSING: generation=N` | 单测断言错误信息不变 |
| R4 | 发布引擎 `bootstrap finalize` | `stable == 请求 generation`，但存在更高 candidate 在途 | 保持原行为拒绝，不谎报成功 | `BOOTSTRAP_CANDIDATE_MISSING: generation=N` | 单测断言不调用 `markFinalized` |
| R5 | 正常滚动候选 finalize | `candidate_generation == 请求 generation` | 与修复前完全一致，执行 FINALIZE 步骤并提交 | 原失败语义不变 | 既有 12 条单测全部通过 |
| R6 | 部署引擎（`mango-deploy-shell`） | 同一构建版本重发，`DATA_UPGRADE=skipped` | 按统一序列 `plan → apply → finalize → verify` 调用可获得退出码 0；无需预检控制表 | 引擎不会因幂等返回而误判 | README §7.1 契约文本存在且与实现一致 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R1、R2 | 幂等放行只比较 `stable_generation`、`stable_fingerprint`、`candidate_generation IS NULL`，与 `apply` 的 warm 分支判定一致 | `BootstrapOrchestrator.finalizeCandidate` 返回 `FINALIZED`，`executionId=null` | 删除该 guard 分支 |
| D2 | R1、R2 | 不使用 `assertStableReleaseIdentity()` 做放行条件。该方法依赖 `release_id`/`build_revision`，而 `abortCandidate` 不恢复这两列，会把放行条件变成不可达或误判 | 无额外查询，仅内存比较 | 无 |
| D3 | R4 | 保留 `candidate_generation IS NULL` 前置条件，避免滚动升级中误报成功并掩盖悬空候选 | 候选并存时继续走原错误分支 | 无 |
| D4 | R2 | 新增错误码 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`，区别于"未 apply"的 `BOOTSTRAP_CANDIDATE_MISSING` | 排障输出与 README 排障表 | 改回原错误码 |
| D5 | R1 | 幂等路径不写 execution 行、不加状态，只在 README 说明"执行 0 步即未实际执行" | 审计历史不变 | 无 |
| D6 | R1-R5 | 平台定义部署引擎调用契约，引擎按契约调用且不得反推控制表（README §7.1）：统一序列、幂等保证、只看退出码、错误语义、回滚契约 | `mango-deploy-shell` 的 `commit_release` 删除 `DATA_UPGRADE=skipped` 身份比对与 reconcile 分支 | 引擎回退到原分支即可，平台契约不依赖引擎实现 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I1 | D1-D4 | 1 | `mango/mango-infra/mango-infra-bootstrap/mango-infra-bootstrap-core/src/main/java/io/mango/infra/bootstrap/core/BootstrapOrchestrator.java` | guard 位于 candidate 校验之前，命中返回 `FINALIZED` |
| I2 | R1-R4 | 2 | 同模块 `src/test/java/.../BootstrapOrchestratorTest.java` | 新增 4 条单测覆盖 R1/R2/R3/R4 |
| I3 | R1、R2 | 3 | 同模块 `src/test/java/.../BootstrapOrchestratorJdbcIntegrationTest.java` | 真 MySQL 断言控制表与 execution/step 计数不变，并验证 `assertStableReleaseIdentity` 仍可通过 |
| I4 | D4 | 4 | `mango/mango-infra/mango-infra-bootstrap/README.md` | apply/finalize 语义段、§7.1 部署引擎调用契约、排障表包含幂等说明和新错误码 |
| I5 | R1、R5 | 5 | `mango/mango-admin-starter/src/test/java/io/mango/admin/starter/BootstrapResourcePerformanceIntegrationTest.java` | 真实 Resource/File/Workflow 声明下重复 finalize 幂等，控制表与审计表不变 |
| I6 | R1、R5 | 6 | `mango-docs/evidence/2026-10-23-issue-1106-bootstrap-finalize-idempotent/` | 真实进程级验收报告、控制表前后快照、`mango-deploy` 脚本调整提案 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R1、R5 | M10 单元测试 | `mvn -pl :mango-infra-bootstrap-core,:mango-infra-bootstrap-starter test -Dmango.bootstrap.test.jdbc-url=...serverTimezone=Asia/Shanghai` | PASS，core 23 条 + starter 18 条 | `target/surefire-reports/` |
| R1、R2 | M11 真实 MySQL 集成测试 | `-Dtest=BootstrapOrchestratorJdbcIntegrationTest`（真实 orchestrator + lock + migrator + repository） | PASS，2 条 | `target/surefire-reports/...BootstrapOrchestratorJdbcIntegrationTest.txt` |
| R1、R5 | M11 真实 app 装配（官方脚本） | `bash scripts/tests/bootstrap-performance.sh` | PASS，Resource 性能用例 21.8s、SQL 冷启用例 4.8s | `.runtime/bootstrap-performance-20261023-162736.log` |
| R1、R5 | M16 真实进程级验收 | `java -jar mango-monolith-app.jar bootstrap apply/finalize/verify/abort`（9 个用例） | PASS：重复 finalize 幂等，正常升级不变 | `mango-docs/evidence/2026-10-23-issue-1106-bootstrap-finalize-idempotent/acceptance-report.md` |
| R1、R2 | 反向对照 | 还原 `BootstrapOrchestrator.java` 后分别跑单测、真库集成测试、真实 app 装配 | 三层均按预期失败并复现 `BOOTSTRAP_CANDIDATE_MISSING` | 同 Acceptance Report 的“反向对照”章节 |
| R1-R4 | M09 静态与架构门禁 | `mvn -f mango/pom.xml -pl :mango-infra-bootstrap-core,:mango-admin-starter -DskipTests -Dmango.architecture.mode=changed -Dmango.architecture.requireFullReactor=false -Dmango.architecture.base=3bc5904afe59eeec09943a07a2dcf5e77c05d4f9 -Dmango.check.changedOnly=true -Dmango.check.gate=no-new-violations -Dmango.check.baseRef=3bc5904afe59eeec09943a07a2dcf5e77c05d4f9 verify` | PASS | BUILD SUCCESS |
| R1-R4 | M09 代码规范 | `mvn -pl :mango-infra-bootstrap-core checkstyle:check` | PASS，0 violation | 终端输出 |
| R6 | M08 能力说明 | README §7.1 部署引擎调用契约（统一序列、幂等保证、成功判定、错误语义、回滚契约） | 已写入 | `mango/mango-infra/mango-infra-bootstrap/README.md` §7.1 |
| R6 | 引擎侧适配提案 | 删除 `DATA_UPGRADE=skipped` 身份比对与 reconcile 分支，改为统一序列 | 提案已出，待 `mango-deploy` 仓评审 | `mango-docs/evidence/2026-10-23-issue-1106-bootstrap-finalize-idempotent/mango-deploy-script-change-proposal.md` |
| - | 测试资产质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/audit-backend-test-mocks.mjs --changed-only --base origin/main` | PASS，3 file；block=0，warn=0 | 终端输出 |

测试环境：MySQL 8.4.8（`127.0.0.1:3306`，隔离库 `mango_it_issue1106` / `mango_it_app_1106`），Java 21.0.12，Maven 3.9.13，JVM 与 MySQL 均为 `+0800` 并统一 `serverTimezone=Asia/Shanghai`。用例自行清理 `mango_bootstrap_*` 数据，不写共享库。

## 7. 例外与剩余风险

- 首次验证时 `-Dmango.bootstrap.test.jdbc-url` 误用 `serverTimezone=UTC`，导致 `JdbcBootstrapRepositoryIntegrationTest` 的 2 条 lease 用例失败。改为 `Asia/Shanghai`（JVM 与 MySQL 实际时区）后 4 条全部通过。这是本地连接参数错误，不是代码缺陷，也不属于既有失败。
- 幂等路径不写 `mango_bootstrap_execution`，重复提交在审计表中无独立记录；真实进程验证确认执行表行数不增长，排障依赖 INFO 日志中的 `executedSteps=0` 与 `executionId=null`。
- 若重复请求的 `mango.release.id`/`revision` 与首次不同，指纹不同，仍返回 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`，需要新 generation 重新 plan/apply。这是期望的显式拒绝，已在真实进程用例 4 验证。
- 已停在 `FAILED_MAINTENANCE` 的环境不会自动恢复：需先确认 `state=FINALIZED`、`candidate_generation IS NULL`，再由引擎侧重放 commit 并关闭维护。
- `BootstrapCommandRunner` 在 `FINALIZED` 后仍执行 `assertStableReleaseIdentity`。正常重发场景该断言成立（release/revision 未变，真实进程已写出回执）；若中间存在被 abort 的候选，该断言可能失败。此场景修复前后都会失败，错误码不同，不构成回归，未纳入本次范围。
- 引擎依赖面已确认，不再属于未验证风险：`mango-deploy-shell` 的 `commit_release` 只依据入口复核结果、bootstrap 子命令退出码、`persist_stable_release` 和 `write_phase SUCCEEDED` 判定成功，不读取 `executionId` 或 `mango_bootstrap_execution`。因此幂等路径返回 `executionId=null` 且不写执行记录不会造成引擎误判。
- 引擎侧既有设计缺口（不属于本次范围）：`write_phase FINALIZING` 发生在清维护与入口复核之前，任何 finalize 失败都会停在 `FAILED_MAINTENANCE` 且拒绝自动回滚。本次修复消除了该触发源，但设计本身仍在，建议单独登记。
- 未修改 ABORT 的对称缺口：真实进程用例 9 确认对已 finalize 的 generation 再次 abort 仍报 `BOOTSTRAP_CANDIDATE_MISSING`，建议单独登记。
- `mango-deploy-shell` 引擎侧行为未验证，组件不在本仓库；受管环境（5.114 / 5.118）需在升级后按上述恢复步骤现场确认。- 本轮未发起外部同行评审（`AGENT_ONLY`）。
