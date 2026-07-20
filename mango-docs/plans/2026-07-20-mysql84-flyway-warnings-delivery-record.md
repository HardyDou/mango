# MySQL 8.4 Flyway 告警治理交付记录

## 1. 元数据

- 任务 ID：mysql84-flyway-warnings
- 交付模式：STANDARD
- 需求影响：L2 - 调整 Workflow 空库结构基线及业务项目 Flyway 运行时版本。
- 方案风险：L2 - 涉及数据库 migration、Maven 依赖管理和 CLI 生成模板，但允许删除测试数据库并重建。
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：消除 MySQL 8.4 空库迁移中的 UTF8MB3、整数显示宽度和 Flyway 数据库版本支持告警。
- 成功条件：Workflow 空库迁移不再使用 `utf8`、`utf8_bin`，所有默认 migration 不再使用整数显示宽度；框架与 CLI 1.0.88 生成项目统一解析 Flyway 11.20.3；MySQL 8.4 空库迁移成功且不出现错误码 3719、3778、1681 或 Flyway 版本升级建议。
- 处理范围：平台默认 migration 中的整数显示宽度、Workflow V1 字符集、Workflow migration 契约测试、Mango 根 POM、发布 Parent、CLI 后端模板、相关能力说明与生成契约。
- 不处理范围：生产数据库字符集转换、日志级别屏蔽；仅保留现有两个已知 Workflow V1 发布校验和的升级兼容。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AR-001 | 平台空库迁移 | MySQL 8.4 空数据库 | Workflow V1 使用 utf8mb4 二进制排序规则，所有默认 migration 均不声明整数显示宽度 | Flyway 输出 MySQL 3719、3778 或 1681 告警 | 定向契约测试和全 migration 静态扫描通过，真实迁移日志无目标告警 |
| AR-002 | Mango 单体与平台应用 | Maven 解析依赖 | `flyway-core` 与 `flyway-mysql` 同时解析为 11.20.3 | MySQL 8.4 被判定为高于已测试版本 | dependency tree 为 11.20.3，真实连接无升级建议 |
| AR-003 | CLI 新建业务项目 | 使用当前模板生成后端 | 生成 POM 显式管理 Flyway 11.20.3 | 业务项目回落到 Spring Boot BOM 的 Flyway 11.7.2 | CLI 生成契约断言版本和两个 Flyway 依赖 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AR-001 | 按用户确认直接重写当前空库基线：`utf8/utf8_bin` 改为 `utf8mb4/utf8mb4_bin`，默认 migration 的 `tinyint(1)` 改为 `tinyint`；不提供历史库兼容迁移 | Workflow、Authorization、Home、Link migration 与契约测试 | 回退对应 SQL 和测试改动，重建测试库 |
| TD-002 | AR-002 | 保持 Flyway 11 大版本，显式管理 11.20.3；该版本的 MySQL 支持上限为 9.4 | `mango/pom.xml`、`mango-parent/pom.xml` | 删除显式依赖管理，恢复 Spring Boot BOM 版本 |
| TD-003 | AR-003 | 生成项目不依赖 BOM 内部版本属性，显式管理 `flyway-core` 与 `flyway-mysql` | CLI 模板和生成检查 | 移除模板的 Flyway 显式管理 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-001 | TD-001 | 1 | Workflow、Authorization、Home、Link migration，迁移契约测试，Workflow README | 无 UTF8MB3 和整数显示宽度声明，契约测试通过 |
| IM-002 | TD-002 | 2 | Mango 根 POM、Mango Parent | 依赖树解析为 Flyway 11.20.3 |
| IM-003 | TD-003 | 3 | CLI 后端 POM 模板、CLI 检查 | 生成模板断言和 CLI 定向测试通过 |
| IM-004 | TD-001/TD-002 | 4 | MySQL 8.4 空库 | 完整迁移成功且目标告警为零 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AR-001 | M09 静态验证、M10 契约测试、M11 MySQL 集成验证 | `rg -ni "\\b(tinyint\|smallint\|mediumint\|int\|integer\|bigint)\\s*\\([0-9]+\\)" mango --glob '**/src/main/resources/db/migration/**/*.sql'`；`mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowMigrationContractTest,WorkflowMigrationUpgradeIntegrationTest test`；`mango dev start backend` 后检查 health 和日志 | PASS：静态扫描 0 项；8 个 migration 测试通过且无跳过，覆盖两个已知历史 V1 checksum、未知 checksum fail closed 与全新基线；MySQL 8.4 空库完成全部迁移，health `UP`，3719/3778/1681/版本升级建议匹配数为 0 | `.mango/run/logs/mango-backend.log`（本机运行态，不提交） |
| AR-002 | M09 Maven 依赖树、M11 MySQL 集成验证 | `mvn -f mango/mango-app/monolith/mango-monolith-app/pom.xml dependency:tree -Dincludes=org.flywaydb:flyway-core,org.flywaydb:flyway-mysql`；`mvn -q -f mango/pom.xml -DskipTests verify` | PASS：两个 Flyway artifact 均为 11.20.3；211 模块全 Reactor verify 退出码 0；MySQL 8.4 运行日志无版本升级建议 | 命令输出与本文件 |
| AR-003 | M09 发布影响检查、M10 CLI 生成契约测试、M11 生成后端验收、M08 能力文档审计 | `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`；`pnpm -C mango-ui --filter @mango/cli test`；`MANGO_BACKEND_GATE_VERSION=1.0.0-SNAPSHOT node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`；两项 README audit | PASS：CLI 1.0.88 版本影响检查通过；CLI 检查及 27 个 Node 测试通过；生成后端 9 次 Maven 正反向门禁通过；两项 README 审计退出码 0 | 命令输出与本文件 |

## 7. 例外与剩余风险

- 用户明确确认当前无人使用且数据库可以删除重建，因此不增加生产数据字符集转换 migration；现有 checksum 回调仍兼容 `1.0.20` 与 `1.0.21/1.0.22` 两个已知 V1 校验和，未知校验和继续 fail closed。
