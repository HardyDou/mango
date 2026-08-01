# 标准交付记录

任务：Issue 671 cold baseline 审计时间修复

## 1. 元数据

- 任务 ID：GitHub Issue #671
- 交付模式：STANDARD
- 需求影响：L2 - 共享 Maven 插件阻断业务 API cold baseline 制品构建
- 方案风险：L2 - 调整 migration 静态数据确定性判定，但不改变 B SQL 数据内容
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/Work/mango-issue-671-baseline-audit-timestamps` 上的 `fix/issue-671-baseline-audit-timestamps`

## 2. 目标与范围

- 目标：允许使用标准运行审计时间列的已发布 V migration 生成 cold baseline，同时继续阻断业务数据的非确定性。
- 成功条件：`created_at`、`updated_at`、`published_at` 仅有回放时钟差异时生成成功；B SQL 保留第一次回放的真实值；B 回放仍按全部列验证；其它列的非确定值继续触发 `MANGO-BASELINE-040`。
- 处理范围：`mango-maven-plugin` 的 determinism 快照、MySQL 集成回归，以及 cold baseline 能力说明。
- 不处理范围：不修改历史 migration，不规范化或固定审计时间，不增加任意列排除参数，不执行 Maven 制品发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | `mango:baseline-generate` | 两次干净 V 回放的业务数据一致，仅三类审计时间列不同 | 通过确定性门禁并生成 B/manifest | 不得触发 `MANGO-BASELINE-040` | 真实 MySQL 集成测试生成成功 |
| SR-002 | B SQL 生成与 verify schema | V 回放产生真实审计时间 | B 保留完整行数据，verify 按全列与 replay 等价比较 | 任一 B 数据丢失或漂移触发 `MANGO-BASELINE-019` | 生成器完整 generate/verify 链路通过 |
| SR-003 | `mango:baseline-generate` | 非审计业务列使用 `UUID()`、`NOW()` 等非确定值 | 继续阻断构建 | `MANGO-BASELINE-040` | 既有 UUID 负例和新增业务时间负例通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001 | 仅 determinism 快照排除 `created_at`、`updated_at`、`published_at`；普通 replay/verify 快照保持全列 | `MySqlBaselineStore`、`BaselineGenerator` | 恢复 determinism 快照使用普通 `snapshot` |
| TD-002 | SR-001/SR-003 | 排除规则为固定、保守的列名集合，不接受通配符，也不扩大到其它时间列 | `MySqlBaselineStore` | 删除专用快照入口和列过滤 |
| TD-003 | SR-002 | B dump 继续使用全部可插入列，不规范化审计值；B 等价验证继续使用普通全量快照 | `MySqlBaselineStore` | 无数据格式迁移，回退代码即可 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | TD-001/TD-002 | 1 | `mango/mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/baseline` | determinism 使用专用审计列过滤快照 |
| IMP-002 | TD-001/TD-003 | 2 | `mango/mango-tools/mango-maven-plugin/src/test/java/io/mango/plugin/baseline` | 审计时间正例、非审计时间负例和既有 UUID 负例覆盖 |
| IMP-003 | TD-001/TD-003 | 3 | `mango/mango-tools/README.md`、cold baseline 接入指南、能力地图 | 使用者能判断忽略边界和仍然严格的验证范围 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001/SR-002/SR-003 | M10/M11 定向真实 MySQL 回归 | `mvn -f mango/pom.xml -pl :mango-maven-plugin -Dtest=BaselineGeneratorIntegrationTest test`（使用 worktree 隔离 MySQL 8.4） | PASS：6 项，失败 0、错误 0、跳过 0 | Surefire `BaselineGeneratorIntegrationTest` 输出 |
| SR-001/SR-002/SR-003 | M09/M10/M11 模块质量门禁 | `mvn -B -ntp -f mango/pom.xml -pl :mango-maven-plugin verify`（使用 worktree 隔离 MySQL 8.4） | PASS：243 项，失败 0、错误 0、性能专项跳过 1 | Maven verify 输出 |
| SR-001/SR-002 | M11 制品集成 | 独立 revision `1.0.0-mango-018-SNAPSHOT` 依赖准备后，执行 `mvn -B -ntp -f mango/pom.xml -pl :mango-maven-plugin -Drevision=1.0.0-mango-018-SNAPSHOT -DskipTests install` | PASS：Invoker 1/1；2 模块 B/manifest 进入 Boot JAR，生成与验证耗时 2.493 秒；相关临时 schema 残留 0 | Maven Invoker `baseline-boot-package` 输出与 `information_schema.SCHEMATA` 回读 |
| 全部 | M09 测试资产质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS：1 个变更测试文件；mock block/warn 0 | checker 输出 |
| 全部 | M08 能力说明 | `node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS：README 结构、链接和源码事实均无问题 | checker 输出 |

## 7. 例外与剩余风险

- `created_at`、`updated_at`、`published_at` 的真实值仍进入 B，因此不同构建的 B 字节和 manifest checksum 可能不同；这是本任务保留历史审计语义的已知取舍。
- 本任务不执行 Maven 制品发布；提交、Push、PR、合并和 Issue 状态回读按用户本次明确授权执行。
