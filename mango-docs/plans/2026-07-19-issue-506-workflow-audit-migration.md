# 标准交付记录

任务：Issue #506 Workflow 既有数据库审计列前向迁移。

## 1. 元数据

- 任务 ID：MANGO-ISSUE-506
- 交付模式：STANDARD
- 需求影响：L2 - Maven `1.0.20` 已执行 Workflow V1 的数据库缺少实体所需审计列，升级后会发生持久化失败或被 Flyway checksum 校验阻断。
- 方案风险：L2 - 增加已发布 checksum 精确兼容 callback 和增量 DDL；改动限定在 Workflow migration，未知 checksum 保持阻断，回退不删除已补列。
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-506-workflow-migration`，`fix/issue-506-workflow-audit-migration`）

## 2. 目标与范围

- 目标：让 Maven `1.0.20` 已执行 Workflow V1 的既有数据库通过正常 Flyway 启动路径升级，并补齐 7 个缺失审计列。
- 成功条件：已知 `1.0.20` V1 checksum 可升级到 V2；当前 fresh V1 已有列时不重复添加；未知 checksum 仍校验失败；历史 V1 内容不再修改。
- 处理范围：Workflow Flyway callback、V2、migration 契约与 MySQL 集成测试、Workflow README、能力地图和业务接入升级说明。
- 不处理范围：业务数据回填、API、流程状态、权限、租户、菜单、前端页面和发布动作。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | Mango Flyway Workflow 模块 | 数据库已执行 Maven `1.0.20` V1，history checksum 为 `-840523381` | callback 精确修复为当前已发布 checksum，V2 补齐 7 个审计列 | V2 DDL 任一失败则启动失败且保留 Flyway 失败证据 | V2 成功记录，7 列定义与当前 V1 一致，重复 migrate 执行 0 个迁移 |
| SR-002 | Mango Flyway Workflow 模块 | 空库使用当前 V1，7 列已存在 | V2 检测已有列并安全跳过 | 重复列或 SQL 语法失败则启动失败 | 首次执行 V1+V2，第二次执行 0 个迁移 |
| SR-003 | Mango Flyway Workflow 模块 | V1 history checksum 不是已知发布值 | callback 不修改 history，Flyway validation 阻断 | 返回 migration version 1 checksum mismatch | checksum 和表结构均不被兼容逻辑改变 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001/SR-003 | `beforeValidate` callback 只匹配 V1、脚本名、成功状态和已发布 `1.0.20` checksum，再写入当前已发布 checksum；不关闭 Flyway validation | Workflow migration callback | 发布前可删除 callback；发布后保留兼容，未知 checksum 仍阻断 |
| TD-002 | SR-001/SR-002 | MySQL 8.4 不支持 `ADD COLUMN IF NOT EXISTS`，V2 使用 `information_schema.columns` 与动态 DDL 逐列处理 | Workflow V2 | 已执行的新增列不做破坏性回滚，应用代码与实体继续兼容 |
| TD-003 | 全部 | V1 保持当前内容不改，API、权限、租户和业务数据均不变化 | Workflow core 与说明文档 | 代码回退时保留已添加列和 Flyway history |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-001 | TD-001 | 1 | `beforeValidate__workflow_v1_checksum_compatibility.sql` | 精确兼容已发布 checksum，未知值不变 |
| IMPL-002 | TD-002 | 2 | `V2__add_workflow_audit_columns.sql` | 7 个目标列在缺失和已有状态均可执行 |
| IMPL-003 | TD-001/TD-002 | 3 | Workflow core test 与 test resource | 旧库、fresh 库、未知 checksum 三类真实 MySQL 场景自动验证 |
| IMPL-004 | TD-003 | 4 | Workflow README、能力地图、业务接入指南 | 升级入口、兼容范围和核验方式可直接定位 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M11 真实 MySQL/Flyway | 使用发布的 `mango-workflow-core:1.0.20` jar 执行 V1，再切换当前 migration location 执行 migrate | PASS：V1 checksum `-840523381` 修复为 `-1500222187`，V2 执行 1 个迁移并补齐 7 列 | 本任务命令输出；隔离库 `mango_dev_*_flyway_probe` 已清理 |
| SR-001/SR-002/SR-003 | M11 集成测试 | `set -a; source .mango/dev-workspace.env; set +a; mvn -f mango/mango-platform/mango-workflow/mango-workflow-core/pom.xml -Dtest=WorkflowMigrationUpgradeIntegrationTest test` | PASS（3/3） | `mango-workflow-core/target/surefire-reports` |
| 全部 | M09/M10 契约与模块测试 | `mvn -f mango/mango-platform/mango-workflow/mango-workflow-core/pom.xml test` | PASS（46/46，失败 0，错误 0，跳过 0） | `mango-workflow-core/target/surefire-reports` |
| 全部 | M09 直接模块质量门禁 | Workflow core 与 architecture verification 定向 `verify`，不使用 `-am/-amd` 扩大质量扫描 | PASS（architecture/PMD 阻断 0，Java 质量门禁新增问题 0；94 项均为非本次文件的存量项） | Maven 输出与 `mango/target/mango-static-report.json` |
| 全部 | M09/M10 测试质量检查 | `test-quality-check.mjs --base origin/main`；`audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS（3 个测试文件；Mock block 0、warn 0） | 命令输出 |
| 全部 | M08 能力说明检查 | `node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS | 命令输出 |

## 7. 例外与剩余风险

- V2 是加列 DDL，MySQL 不为整份迁移提供跨语句原子事务；若运行中断，重新启动会按列检查并继续补齐，其它未知失败仍需按 Flyway 失败记录排查。
- callback 只覆盖 Maven `1.0.20` 的已发布 V1 checksum；人工修改或其它未知 V1 必须先核对来源和 schema，不能关闭 validation 绕过。
- MySQL 集成测试只在 `MANGO_DB_NAME` 匹配 `mango_dev_*` 时启用，并创建、销毁派生隔离数据库；普通无 MySQL 环境会跳过该测试，契约测试仍执行。
