# 标准交付记录

## Issue #1111 Resource Registry 旧 sync_mode 归一化

## 1. 元数据

- 任务 ID：Issue #1111
- 交付模式：STANDARD
- 需求影响：L2 - 升级后的历史 Registry 数据和启动诊断
- 方案风险：L2 - 一次性数据库更新可能产生锁等待并影响启动
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/mango-issue-1111`
- 分支：`fix/issue-1111-sync-mode-normalization`

## 2. 目标与范围

- 目标：将 `resource_registry.sync_mode` 中的旧持久化值一次性归一为当前值，消除重复弃用告警。
- 成功条件：`AUTO` 变为 `SYNC_ALWAYS`；`INIT_ONLY`、`MANUAL`、`LOCKED` 变为 `SYNC_ONCE`；其他 Registry 字段和非旧值不被迁移修改。
- 处理范围：V5 Flyway 迁移、迁移契约测试、Resource README 和 1.1.0→1.1.1 升级说明。
- 不处理范围：不修改目标业务表、ownership、版本、Hash、状态、目标 ID；不改变读取时兼容逻辑。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | Flyway 升级 | `resource_registry` 存在旧 sync_mode | 只更新旧值并写入新值 | 迁移失败时由 Flyway 阻断升级，不静默跳过 | 旧值查询为 0；新旧映射正确 |
| R2 | 重复执行/重试 | 迁移已完成 | 无旧值时不产生额外变更 | 重试不重复改变数据 | 第二次执行结果与第一次一致 |
| R3 | Resource 文档使用者 | 阅读升级指南或模块 README | 能确认迁移范围、验证 SQL 和不变数据边界 | 文档缺失会导致升级验收不可观察 | 文档包含 V5 说明和旧值清零查询 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R1/R2 | 新增 `V5__normalize_legacy_resource_sync_modes.sql`，使用单条 `UPDATE ... CASE`；`AUTO→SYNC_ALWAYS`，其余三个旧值→`SYNC_ONCE`，仅以旧值作为 WHERE 条件 | Resource Flyway 迁移目录；升级时对 Registry 表加更新锁 | 发布前恢复数据库备份；发布后只能显式反向 SQL，需业务确认 |
| D2 | R1 | 不采用读取时回写，不调用 Handler，不更新 ownership、版本、Hash、状态或目标 ID | 避免只处理被读取行和避免重放业务副作用 | 移除 V5 仅适用于尚未执行迁移的环境；已执行环境按备份恢复 |
| D3 | R3 | 升级指南和 Resource README 明确迁移边界及旧值清零查询 | 运维升级和验收 | 文档修订不影响运行数据 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I1 | D1 | 1 | `mango/mango-platform/mango-resource/mango-resource-core/src/main/resources/db/migration/resource/V5__normalize_legacy_resource_sync_modes.sql` | V5 映射全部旧值且只更新 `sync_mode` |
| I2 | D1/D2 | 2 | `mango/mango-platform/mango-resource/mango-resource-core/src/test/java/io/mango/resource/core/migration/ResourceMigrationContractTest.java` | 覆盖版本、映射和不修改目标字段的 SQL 契约 |
| I3 | D3 | 3 | `mango-docs/guides/operations/mango-1.1.0-to-1.1.1-upgrade.md`、`mango/mango-platform/mango-resource/README.md` | 升级说明和模块说明可回读 |
| I4 | - | 4 | 本记录 | 保留验证命令、结果和剩余风险 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R1 | 单元/契约测试 | `mvn -f mango/pom.xml -pl :mango-resource-core -Dtest=ResourceMigrationContractTest test` | PASS，4 tests | Maven Surefire 输出 |
| R1 | MySQL 行为验证 | 将本机 `baohan_test` 测试环境数据只读复制到 `mango_dev_mango_issue_1111_004`，执行 V5 | PASS：2652 行受影响；`AUTO=1765→SYNC_ALWAYS`、`INIT_ONLY=887→SYNC_ONCE`；旧值为 0；排除 `sync_mode` 和数据库自动更新的 `updated_at` 后，受保护字段 SHA-256 不变 | `mango-docs/evidence/2026-10-26-issue-1111-sync-mode-normalization/real-data-validation-summary.md`；原始输出 `.runtime/issue-1111/real-*` |
| R2 | MySQL 幂等性验证 | 在上述真实数据副本连续执行 V5 两次 | PASS：第二次影响 0 行，旧值仍为 0；旧资源 ID 集合与新模式集合 SHA-256 一致 | `mango-docs/evidence/2026-10-26-issue-1111-sync-mode-normalization/real-data-validation-summary.md`；原始输出 `.runtime/issue-1111/real-v5-second-run.tsv` |
| R4 | baohan-system 启动与冒烟 | 按正常测试环境做法清理副本中的旧 Registry 登记行，启动后端并验证真实公开接口正常/异常边界 | PASS（Issue #1111 范围）：health=UP、Bootstrap=`FINALIZED`；`api.mango-link...` 重新登记为 `resource_id=807245613893456101`，`target_id=6673508641616300253` 保持不变；有效租户请求返回 24 条真实数据，缺少租户时按预期返回业务 `400`。EXCEPTION：无额外配置时发现独立的 `guarantee-verification` eventual 资源冲突；临时增加 `exclude-paths` 后等待约 70 秒验证冲突为 0，但正式配置需另行评审 | `mango-docs/evidence/2026-10-26-issue-1111-sync-mode-normalization/real-data-validation-summary.md`；日志 `.runtime/issue-1111/baohan-system-smoke-round2.log`、`.runtime/issue-1111/baohan-system-smoke-exclude-path.log`；独立问题：https://git.inner.yunxinbaokeji.com/baohan/baohan-system/issues/1829 |
| R3 | 文档/差异检查 | `git diff --check`、回读 README/升级指南 | PASS | Git 检查输出及文件路径 |
| R1/R2 | 模块完整验证 | `mvn -f mango/pom.xml -pl :mango-resource-core verify` | PASS，93 tests，0 failures/errors | Maven Surefire 输出 |
| 全局 | 测试质量门禁 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS；block=0、warn=0 | 工具输出 |

## 7. 例外与剩余风险

- 已完成本机 `baohan_test` 数据副本的真实迁移、字段不变性和幂等性验证；完整候选制品的 Flyway 自动发现 V5 尚未验证。
- 已按正常测试环境清理方式在副本中备份并删除旧 `mango-link` Registry 行；源库 `baohan_test` 未修改。
- `baohan-system` 启动与 Bootstrap 已通过，但后台 eventual worker 仍报告另一条 `guarantee-verification` 身份冲突；该冲突不属于 Issue #1111，已登记为 baohan-system Issue #1829，不能作为本次修复的失败依据，也不能把整套业务环境声明为无告警。
- 对 Mango 源码固定 `API_RESOURCE` 声明执行了独立审计：5 个文件、27 条固定声明，其中 24 条与默认 `io.mango` MVC 扫描范围存在可确认重叠；该问题未混入 Issue #1111，审计记录见 `mango-fixed-api-resource-audit.md`，后续需单独评审唯一声明来源方案。
- 大表更新可能产生锁等待；本次 2652 行迁移在本机副本执行成功，发布前仍应评估目标库行数、低峰窗口和超时/回滚预案。
- 回退到无法识别 `SYNC_ALWAYS`/`SYNC_ONCE` 的 1.0.63 版本仍存在兼容风险，回退前需使用备份恢复或确认旧版本兼容策略。
- 已创建 baohan-system Issue #1829；Mango worktree 已完成本地 commit `dac282e086cef0bb4cf1f3576feeb57fcc5c5d83`，未执行 push、PR 或发布。
