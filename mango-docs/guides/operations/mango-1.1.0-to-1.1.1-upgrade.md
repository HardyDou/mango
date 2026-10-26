<!-- mango-release-upgrade-guide: generated -->
# Mango 1.1.0 → 1.1.1 升级指南

**适用对象：使用 Mango Maven 1.1.0 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.10.23-maven-1.1.1-pmo-upgrade-record-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.1.0` → `1.1.1`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/pmo@1.4.8`、`@mango/cli@1.3.5`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/pmo` | `1.4.7` | `1.4.8` |
| `@mango/cli` | `1.3.4` | `1.3.5` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- 同一构建版本重复执行 `bootstrap finalize` 不再报 `BOOTSTRAP_CANDIDATE_MISSING`：已 `FINALIZED` 且 fingerprint 未变的同一 generation 直接返回 `FINALIZED`（0 步，控制表与执行表不变化），内容不同时返回 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`。
- Resource Registry 旧 `sync_mode` 由新增的 `V5__normalize_legacy_resource_sync_modes.sql` 一次性归一：`AUTO` → `SYNC_ALWAYS`，`INIT_ONLY`、`MANUAL`、`LOCKED` → `SYNC_ONCE`。
- 文档站不再因为人工白名单遗漏而让升级指南 404：`mango-docs/guides/operations/mango-*-to-*-upgrade.md` 自动收录并进入侧边栏。
- `check-governance-intent` 在 `main` 上的既有失败已修复：CMS Banner 指南与租户 FAQ 改为操作化表述，生成模板同样规避规则化措辞。

## Added

- 每个 Maven 发布批次由 `mango release plan` 自动生成一份升级页面，内容覆盖版本矩阵、变更摘要、升级前检查、依赖升级、迁移与启动验证、业务回归、回滚边界和 Agent 执行清单。
- `check-release-plan` 在 CI 与 `release prepare` 中校验升级页面存在且与计划一致；页面随 `mango-docs-bundle` 交付给业务项目。
- 模块 README 新增「升级与兼容」书写规范：按版本记录适用版本、变更、影响、需要做的事、验证与回滚。

## Changed

- `mango-release` Skill 的 Readiness 增加升级页面要求，并说明生成、校验、发布与交付链路。
- `mango-pmo` 规则索引版本 22 → 23，能力说明规范新增模块升级记录条款（明确不新增阻塞门禁）。
- `mango-docs-bundle` 内容更新：新增升级页面机制、模块升级记录模板与上述指南。

## Versions

- `io.mango:mango-parent`, the non-application Maven reactor and `io.mango:mango-docs-bundle`: `1.1.0` -> `1.1.1`.
- npm: `@mango/pmo` `1.4.7` -> `1.4.8`; `@mango/cli` `1.3.3` -> `1.3.5` via dependency closure (`@mango/cli` `1.3.4` -> `1.3.5`).
- 其余 Maven 坐标与 npm 包随批次统一推进到 `1.1.1` / 依赖闭包版本。

## Published Packages

1. Publish the sealed non-application Maven reactor and `io.mango:mango-docs-bundle:1.1.1`, then verify both Maven registry roles.
2. Publish the machine-planned npm closure (`@mango/pmo@1.4.8`, `@mango/cli@1.3.5`) and verify both npm registry roles.
3. Run one aggregate clean Maven consumer plus a clean npm consumer that installs `@mango/cli@1.3.5` and generates a project without a workspace link.
4. Create Tag `v2026.10.23-maven-1.1.1-pmo-upgrade-record-release` and its GitHub Release only after consume verification succeeds.

## Business Impact

- 后端消费者：`mango-infra-bootstrap-*` 的重复 finalize 行为变为幂等；其余 Maven 坐标仅为版本推进，无契约变化。
- 部署脚本：按 `plan -> apply --strategy=rolling -> finalize -> verify` 调用即可，不再需要自行推断控制表是否需要 finalize。
- PMO 消费者：`@mango/pmo@1.4.8` 带模块升级记录规范与升级页面机制；`mango pmo upgrade` 后业务仓可读到同口径文档。
- 文档：`mango docs pull` 在 `1.1.1` 下能拉到本批次升级页面与模块 README 模板。
- 不包含应用部署、流量切换或数据库回滚。

## Upgrade Estimate

- Audience: 使用 Mango `1.1.0` 或更早版本的业务项目，尤其是自建部署引擎调用 bootstrap 的项目。
- Engineering Effort: low; 替换 Maven/npm 版本并按升级页核对部署脚本即可，无业务代码改动。
- Execution Window: 30 to 60 minutes including database migration, restart and Bootstrap verification; assess lock waits for large tables.
- Service Downtime: one planned restart window including the Resource migration; reserve extra time for large-table locks.
- Rollback Effort: medium; dependency rollback is low effort, but database snapshot recovery is required before returning to versions that cannot read the new sync_mode values.
- Assumptions: MySQL 8 consumer database, Java 21 build, Node 22.23.1 frontend build, and an operator able to run one bootstrap sequence.

## Upgrade Notes

1. 升级 Maven/docs 到 `1.1.1`，npm 到 `@mango/pmo@1.4.8` 与 `@mango/cli@1.3.5`。
2. 先读 `mango-docs/guides/operations/` 下与本批次对应的升级页面，按页面执行升级前检查与验证。
3. 部署引擎遵循 `plan -> apply --strategy=rolling -> finalize -> verify` 统一序列；删除自行判断“是否需要 finalize”的预检。
4. 重复执行同一版本发布会返回 `FINALIZED` 而非失败；若返回 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`，说明同一 generation 的内容已变化，需要新 generation。
5. 执行 Resource Flyway 迁移，确认 `V5__normalize_legacy_resource_sync_modes.sql` 成功；查询 `resource_registry.sync_mode` 中 `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED` 均为 0，再确认 `mango_bootstrap_control` 为 `FINALIZED`、`flyway_schema_history_resource` 无失败记录。

## Verification

- Maven: 从 consume registry 解析 `io.mango:mango-infra-bootstrap-core:1.1.1`、`io.mango:mango-infra-bootstrap-starter:1.1.1` 与 `io.mango:mango-docs-bundle:1.1.1` 并比对封存校验和。
- npm: 从 publish 与 consume registry 解析 `@mango/pmo@1.4.8`、`@mango/cli@1.3.5` 并比对封存 integrity。
- Repository gates: `mango release plan`、`mango release prepare`、`mango release publish` 证据，README 双审计与 PMO 门禁。
- Consumer assertion: 升级库重复执行 `bootstrap finalize` 两次均返回 `FINALIZED`；`mango docs pull` 能读到本批次升级页；`mango pmo upgrade --to 1.4.8` 后模块 README 模板含「升级与兼容」。

## Rollback

- 通过正常依赖发布流程把消费者回退到 Maven/docs `1.1.0` 与 PMO `1.4.7`、CLI `1.3.4`；已发布的 `1.1.1` 坐标不可覆盖。
- V5 会持久化写入新的 sync_mode，回退到无法识别 `SYNC_ALWAYS`/`SYNC_ONCE` 的 1.0.63 或更早版本前必须恢复升级前数据库快照；代码回退本身不等于数据回滚。
- 文档回退仅影响可读性，不影响运行时。

### Added

- 每个 Maven 发布批次由 `mango release plan` 自动生成一份升级页面，内容覆盖版本矩阵、变更摘要、升级前检查、依赖升级、迁移与启动验证、业务回归、回滚边界和 Agent 执行清单。
- `check-release-plan` 在 CI 与 `release prepare` 中校验升级页面存在且与计划一致；页面随 `mango-docs-bundle` 交付给业务项目。
- 模块 README 新增「升级与兼容」书写规范：按版本记录适用版本、变更、影响、需要做的事、验证与回滚。

## Changed

- `mango-release` Skill 的 Readiness 增加升级页面要求，并说明生成、校验、发布与交付链路。
- `mango-pmo` 规则索引版本 22 → 23，能力说明规范新增模块升级记录条款（明确不新增阻塞门禁）。
- `mango-docs-bundle` 内容更新：新增升级页面机制、模块升级记录模板与上述指南。

## Versions

- `io.mango:mango-parent`, the non-application Maven reactor and `io.mango:mango-docs-bundle`: `1.1.0` -> `1.1.1`.
- npm: `@mango/pmo` `1.4.7` -> `1.4.8`; `@mango/cli` `1.3.3` -> `1.3.5` via dependency closure (`@mango/cli` `1.3.4` -> `1.3.5`).
- 其余 Maven 坐标与 npm 包随批次统一推进到 `1.1.1` / 依赖闭包版本。

## Published Packages

1. Publish the sealed non-application Maven reactor and `io.mango:mango-docs-bundle:1.1.1`, then verify both Maven registry roles.
2. Publish the machine-planned npm closure (`@mango/pmo@1.4.8`, `@mango/cli@1.3.5`) and verify both npm registry roles.
3. Run one aggregate clean Maven consumer plus a clean npm consumer that installs `@mango/cli@1.3.5` and generates a project without a workspace link.
4. Create Tag `v2026.10.23-maven-1.1.1-pmo-upgrade-record-release` and its GitHub Release only after consume verification succeeds.

## Business Impact

- 后端消费者：`mango-infra-bootstrap-*` 的重复 finalize 行为变为幂等；其余 Maven 坐标仅为版本推进，无契约变化。
- 部署脚本：按 `plan -> apply --strategy=rolling -> finalize -> verify` 调用即可，不再需要自行推断控制表是否需要 finalize。
- PMO 消费者：`@mango/pmo@1.4.8` 带模块升级记录规范与升级页面机制；`mango pmo upgrade` 后业务仓可读到同口径文档。
- 文档：`mango docs pull` 在 `1.1.1` 下能拉到本批次升级页面与模块 README 模板。
- 不包含应用部署、流量切换或数据库回滚。

## Upgrade Estimate

- Audience: 使用 Mango `1.1.0` 或更早版本的业务项目，尤其是自建部署引擎调用 bootstrap 的项目。
- Engineering Effort: low; 替换 Maven/npm 版本并按升级页核对部署脚本即可，无业务代码改动。
- Execution Window: 30 to 60 minutes including database migration, restart and Bootstrap verification; assess lock waits for large tables.
- Service Downtime: one planned restart window including the Resource migration; reserve extra time for large-table locks.
- Rollback Effort: medium; dependency rollback is low effort, but database snapshot recovery is required before returning to versions that cannot read the new sync_mode values.
- Assumptions: MySQL 8 consumer database, Java 21 build, Node 22.23.1 frontend build, and an operator able to run one bootstrap sequence.

## Upgrade Notes

1. 升级 Maven/docs 到 `1.1.1`，npm 到 `@mango/pmo@1.4.8` 与 `@mango/cli@1.3.5`。
2. 先读 `mango-docs/guides/operations/` 下与本批次对应的升级页面，按页面执行升级前检查与验证。
3. 部署引擎遵循 `plan -> apply --strategy=rolling -> finalize -> verify` 统一序列；删除自行判断“是否需要 finalize”的预检。
4. 重复执行同一版本发布会返回 `FINALIZED` 而非失败；若返回 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`，说明同一 generation 的内容已变化，需要新 generation。
5. 执行 Resource Flyway 迁移，确认 `V5__normalize_legacy_resource_sync_modes.sql` 成功；查询 `resource_registry.sync_mode` 中 `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED` 均为 0，再确认 `mango_bootstrap_control` 为 `FINALIZED`、`flyway_schema_history_resource` 无失败记录。

## Verification

- Maven: 从 consume registry 解析 `io.mango:mango-infra-bootstrap-core:1.1.1`、`io.mango:mango-infra-bootstrap-starter:1.1.1` 与 `io.mango:mango-docs-bundle:1.1.1` 并比对封存校验和。
- npm: 从 publish 与 consume registry 解析 `@mango/pmo@1.4.8`、`@mango/cli@1.3.5` 并比对封存 integrity。
- Repository gates: `mango release plan`、`mango release prepare`、`mango release publish` 证据，README 双审计与 PMO 门禁。
- Consumer assertion: 升级库重复执行 `bootstrap finalize` 两次均返回 `FINALIZED`；`mango docs pull` 能读到本批次升级页；`mango pmo upgrade --to 1.4.8` 后模块 README 模板含「升级与兼容」。

## Rollback

- 通过正常依赖发布流程把消费者回退到 Maven/docs `1.1.0` 与 PMO `1.4.7`、CLI `1.3.4`；已发布的 `1.1.1` 坐标不可覆盖。
- V5 会持久化写入新的 sync_mode，回退到无法识别 `SYNC_ALWAYS`/`SYNC_ONCE` 的 1.0.63 或更早版本前必须恢复升级前数据库快照；代码回退本身不等于数据回滚。
- 文档回退仅影响可读性，不影响运行时。

### Changed

- `mango-release` Skill 的 Readiness 增加升级页面要求，并说明生成、校验、发布与交付链路。
- `mango-pmo` 规则索引版本 22 → 23，能力说明规范新增模块升级记录条款（明确不新增阻塞门禁）。
- `mango-docs-bundle` 内容更新：新增升级页面机制、模块升级记录模板与上述指南。

## Versions

- `io.mango:mango-parent`, the non-application Maven reactor and `io.mango:mango-docs-bundle`: `1.1.0` -> `1.1.1`.
- npm: `@mango/pmo` `1.4.7` -> `1.4.8`; `@mango/cli` `1.3.3` -> `1.3.5` via dependency closure (`@mango/cli` `1.3.4` -> `1.3.5`).
- 其余 Maven 坐标与 npm 包随批次统一推进到 `1.1.1` / 依赖闭包版本。

## Published Packages

1. Publish the sealed non-application Maven reactor and `io.mango:mango-docs-bundle:1.1.1`, then verify both Maven registry roles.
2. Publish the machine-planned npm closure (`@mango/pmo@1.4.8`, `@mango/cli@1.3.5`) and verify both npm registry roles.
3. Run one aggregate clean Maven consumer plus a clean npm consumer that installs `@mango/cli@1.3.5` and generates a project without a workspace link.
4. Create Tag `v2026.10.23-maven-1.1.1-pmo-upgrade-record-release` and its GitHub Release only after consume verification succeeds.

## Business Impact

- 后端消费者：`mango-infra-bootstrap-*` 的重复 finalize 行为变为幂等；其余 Maven 坐标仅为版本推进，无契约变化。
- 部署脚本：按 `plan -> apply --strategy=rolling -> finalize -> verify` 调用即可，不再需要自行推断控制表是否需要 finalize。
- PMO 消费者：`@mango/pmo@1.4.8` 带模块升级记录规范与升级页面机制；`mango pmo upgrade` 后业务仓可读到同口径文档。
- 文档：`mango docs pull` 在 `1.1.1` 下能拉到本批次升级页面与模块 README 模板。
- 不包含应用部署、流量切换或数据库回滚。

## Upgrade Estimate

- Audience: 使用 Mango `1.1.0` 或更早版本的业务项目，尤其是自建部署引擎调用 bootstrap 的项目。
- Engineering Effort: low; 替换 Maven/npm 版本并按升级页核对部署脚本即可，无业务代码改动。
- Execution Window: 30 to 60 minutes including database migration, restart and Bootstrap verification; assess lock waits for large tables.
- Service Downtime: one planned restart window including the Resource migration; reserve extra time for large-table locks.
- Rollback Effort: medium; dependency rollback is low effort, but database snapshot recovery is required before returning to versions that cannot read the new sync_mode values.
- Assumptions: MySQL 8 consumer database, Java 21 build, Node 22.23.1 frontend build, and an operator able to run one bootstrap sequence.

## Upgrade Notes

1. 升级 Maven/docs 到 `1.1.1`，npm 到 `@mango/pmo@1.4.8` 与 `@mango/cli@1.3.5`。
2. 先读 `mango-docs/guides/operations/` 下与本批次对应的升级页面，按页面执行升级前检查与验证。
3. 部署引擎遵循 `plan -> apply --strategy=rolling -> finalize -> verify` 统一序列；删除自行判断“是否需要 finalize”的预检。
4. 重复执行同一版本发布会返回 `FINALIZED` 而非失败；若返回 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`，说明同一 generation 的内容已变化，需要新 generation。
5. 执行 Resource Flyway 迁移，确认 `V5__normalize_legacy_resource_sync_modes.sql` 成功；查询 `resource_registry.sync_mode` 中 `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED` 均为 0，再确认 `mango_bootstrap_control` 为 `FINALIZED`、`flyway_schema_history_resource` 无失败记录。

## Verification

- Maven: 从 consume registry 解析 `io.mango:mango-infra-bootstrap-core:1.1.1`、`io.mango:mango-infra-bootstrap-starter:1.1.1` 与 `io.mango:mango-docs-bundle:1.1.1` 并比对封存校验和。
- npm: 从 publish 与 consume registry 解析 `@mango/pmo@1.4.8`、`@mango/cli@1.3.5` 并比对封存 integrity。
- Repository gates: `mango release plan`、`mango release prepare`、`mango release publish` 证据，README 双审计与 PMO 门禁。
- Consumer assertion: 升级库重复执行 `bootstrap finalize` 两次均返回 `FINALIZED`；`mango docs pull` 能读到本批次升级页；`mango pmo upgrade --to 1.4.8` 后模块 README 模板含「升级与兼容」。

## Rollback

- 通过正常依赖发布流程把消费者回退到 Maven/docs `1.1.0` 与 PMO `1.4.7`、CLI `1.3.4`；已发布的 `1.1.1` 坐标不可覆盖。
- V5 会持久化写入新的 sync_mode，回退到无法识别 `SYNC_ALWAYS`/`SYNC_ONCE` 的 1.0.63 或更早版本前必须恢复升级前数据库快照；代码回退本身不等于数据回滚。
- 文档回退仅影响可读性，不影响运行时。

## 3. 升级前检查

1. 确认数据库备份可恢复，尤其是 Resource Registry、Flyway history、Bootstrap receipt 和业务目标表。
2. 阅读当前版本对应的模块 README、能力地图和本文引用的专项指南。
3. 对 Resource Registry 统计旧 sync_mode 数量，记录迁移前结果；确认数据库备份可恢复。
4. 检查业务项目是否引用本批次删除或收紧的 API、配置、枚举、菜单或前端导出。
5. 记录当前 Maven、npm、CLI、PMO 版本以及构建锁文件；不要混用不同发布批次。
6. 先在预发布数据库执行只读检查，再进入维护窗口。

## 4. 依赖升级

### Maven

将业务项目 `backend/pom.xml` 的 Mango 版本锁定为：

```xml
<mango.version>1.1.1</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.1.1` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/pmo@1.4.8`、`@mango/cli@1.3.5`

```bash
pnpm install
pnpm exec mango pmo check --locked
pnpm exec mango docs pull
```

CLI 生成项目使用本批次的 `@mango/cli`；不要用旧的全局 CLI 覆盖项目内锁定版本。

## 5. 数据库与启动验证

1. 备份后在维护窗口启动一次升级候选，等待 Flyway 与 Resource Bootstrap 完成。
2. 迁移前记录以下查询结果，确认旧值数量和 Registry 行数：

   ```sql
   SELECT sync_mode, COUNT(*)
   FROM resource_registry
   GROUP BY sync_mode;
   ```

3. 确认 `V5__normalize_legacy_resource_sync_modes.sql` 执行成功；迁移只更新 `resource_registry.sync_mode`，不重放目标 Handler。
4. 迁移后确认以下查询返回 0 行，并核对 Registry 行数、ownership、版本、Hash、状态和目标 ID 未变化：

   ```sql
   SELECT sync_mode, COUNT(*)
   FROM resource_registry
   WHERE sync_mode IN ('AUTO', 'INIT_ONLY', 'MANUAL', 'LOCKED')
   GROUP BY sync_mode;
   ```

5. 启动验证完成后确认 Bootstrap receipt 最终为 `FINALIZED`。
6. 确认所有新增迁移成功，Bootstrap receipt 最终为 `FINALIZED`。
7. 对 Resource Registry 检查唯一键、同步模式、归属字段和冲突诊断。
8. 对固定 API_RESOURCE 声明执行 Controller 扫描重叠检查。`resource-provider.enabled` 在 1.0.63、1.1.0 和 1.1.1 默认均为启用；同一接口若保留固定声明，应在 `mango.authorization.resource-sync.exclude-paths` 中排除对应路径。不要删除目标业务资源或关闭 fail-closed 冲突校验。
9. 对业务模块执行本文第 2 节涉及的专项验证；迁移失败或身份冲突时停止，不要通过手工修改 Flyway history 绕过。

## 6. 业务回归

- 后端 API、配置、权限、菜单、初始化数据和租户边界。
- 前端构建、路由、页面注册、公开站点和已变更的 npm 导出。
- CLI 生成项目、PMO baseline、docs bundle 和 clean consumer。
- 对本批次 release notes 的 `Fixed`、`Added`、`Changed` 项逐项保留验证证据。
- API Resource 声明来源检查：固定 `META-INF/mango/resources/*.yml|yaml|json` 声明与 Controller 自动扫描不得为同一 `resourceType + bizKey` 生成不同资源 ID；升级后至少等待一个 eventual 对账周期并检查 `Resource bizKey already registered by another id`。

## 7. 回滚边界

- 已发布的 Maven/npm 坐标不可覆盖，只能发布新版本或回退消费者依赖。
- Resource V5 是一次性数据归一迁移；不可逆数据操作需要在升级前完成归档或备份，回退旧版本前恢复数据库快照。
- 代码回退不等于数据回滚；保留升级前数据库快照、Bootstrap receipt、迁移日志和前端构建产物。

## 8. Agent 执行清单

| 步骤 | 动作 | 通过判据 | 停止条件 |
|---|---|---|---|
| 1 | 读取能力地图、模块 README、本文和专项升级指南 | 当前事实源与目标版本一致 | 文档冲突，先报告，不套用历史路径 |
| 2 | 盘点 API、配置、枚举、前端导出和数据库迁移影响 | 形成命中清单 | 发现破坏性引用未给出迁移方案 |
| 3 | 备份数据库和构建锁文件 | 可恢复证据已保存 | 无可验证备份 |
| 4 | 升级 Maven/npm/CLI 全闭包 | 依赖解析、构建与锁文件通过 | peer 冲突、坐标解析失败或混批次 |
| 5 | 执行迁移并启动 Bootstrap | Flyway 成功，receipt=FINALIZED | 迁移失败、冲突、ownership 未证明项未处理 |
| 6 | 执行 API、前端、CLI 和业务回归 | release notes 变更项均有证据 | 任一关键回归失败 |
| 7 | 保存升级证据并决定放行 | 命令、版本、日志、结果齐全 | 缺证据不要宣告完成 |

## 9. 关联记录

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.10.23-maven-1.1.1-pmo-upgrade-record-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
