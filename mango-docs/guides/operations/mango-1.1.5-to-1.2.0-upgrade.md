<!-- mango-release-upgrade-guide: generated -->
# Mango 1.1.5 → 1.2.0 升级指南

**适用对象：使用 Mango Maven 1.1.5 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.09-maven-1.2.0-integration-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.1.5` → `1.2.0`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/common@2.3.1`、`@mango/admin-extension@1.1.5`、`@mango/ai@1.2.5`、`@mango/auth@1.0.42`、`@mango/file@1.2.5`、`@mango/detail@1.1.10`、`@mango/grid-layout@1.0.31`、`@mango/grid-widgets@1.0.37`、`@mango/home@1.0.30`、`@mango/information@1.1.5`、`@mango/rbac@1.3.0`、`@mango/site-shell@2.0.3`、`@mango/system@1.1.2`、`@mango/admin-pages@1.3.0`、`@mango/calendar@1.0.57`、`@mango/cms@1.1.2`、`@mango/job@1.0.50`、`@mango/link@1.0.43`、`@mango/notice@1.0.62`、`@mango/numgen@1.0.58`、`@mango/payment@1.0.49`、`@mango/template@1.0.58`、`@mango/workflow@1.0.66`、`@mango/admin-shell@1.1.2`、`@mango/workflow-business-example@1.0.65`、`@mango/admin@2.0.4`、`@mango/cli@1.3.11`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/common` | `2.3.0` | `2.3.1` |
| `@mango/admin-extension` | `1.1.4` | `1.1.5` |
| `@mango/ai` | `1.2.4` | `1.2.5` |
| `@mango/auth` | `1.0.41` | `1.0.42` |
| `@mango/file` | `1.2.4` | `1.2.5` |
| `@mango/detail` | `1.1.9` | `1.1.10` |
| `@mango/grid-layout` | `1.0.30` | `1.0.31` |
| `@mango/grid-widgets` | `1.0.36` | `1.0.37` |
| `@mango/home` | `1.0.29` | `1.0.30` |
| `@mango/information` | `1.1.4` | `1.1.5` |
| `@mango/rbac` | `1.2.0` | `1.3.0` |
| `@mango/site-shell` | `2.0.2` | `2.0.3` |
| `@mango/system` | `1.1.1` | `1.1.2` |
| `@mango/admin-pages` | `1.2.2` | `1.3.0` |
| `@mango/calendar` | `1.0.56` | `1.0.57` |
| `@mango/cms` | `1.1.1` | `1.1.2` |
| `@mango/job` | `1.0.49` | `1.0.50` |
| `@mango/link` | `1.0.42` | `1.0.43` |
| `@mango/notice` | `1.0.61` | `1.0.62` |
| `@mango/numgen` | `1.0.57` | `1.0.58` |
| `@mango/payment` | `1.0.48` | `1.0.49` |
| `@mango/template` | `1.0.57` | `1.0.58` |
| `@mango/workflow` | `1.0.65` | `1.0.66` |
| `@mango/admin-shell` | `1.1.1` | `1.1.2` |
| `@mango/workflow-business-example` | `1.0.64` | `1.0.65` |
| `@mango/admin` | `2.0.3` | `2.0.4` |
| `@mango/cli` | `1.3.10` | `1.3.11` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- File preview now selects the rendering path by file extension: PDF files use PDF.js directly, Office documents and ODT/ODS/ODP convert asynchronously, and archives, spreadsheets, images, text and media go straight to the engine instead of failing in PDF conversion.
- Office conversion artifacts can be read inside the embedded preview iframe without a second login; `preview-progress`, `preview-content` and `preview-artifact` are public endpoints guarded by the entry token.
- Office conversion prefers LibreOffice and only falls back to Aspose when `mango.fileproc.convert.office-to-pdf-enabled` is explicitly disabled; a conversion `Error` now records the failure and marks the task `FAILED` instead of leaving a stuck task.
- Configuring `mango.file-preview.source-base-url` automatically adds its host to the engine trust list, so gateway deployments no longer duplicate `trust.host` entries.
- Workflow task return is fail-closed: a return that leaves the process on the original node or yields no pending task fails with `TASK_INVALID` and rolls back, and gateway targets resolve upstream to the nearest completed user task.
- `mango-file-preview-engine` no longer passes `org.apache.poi:ooxml-schemas:1.1` to consumers, so POI 5 workbook parsing no longer fails with `NoSuchFieldError ... DocumentFactory Factory`.
- `MangoDialog` is centered under Element Plus `align-center` and its height follows the content instead of being stretched to `--mango-dialog-max-height`.
- Cold baseline resolves database state before reading `B*` resources, skips migration-less declared modules with a WARN, and cleans stale generated baseline resources when the build switch is off.

### Added

- `mango-integration` platform module: connector definitions with config/auth schemas, capability and tag metadata, encrypted credentials with rotation history and read audit, connection instances with connection testing, declaration SPI, auth toolkit and a unified `IntegrationClient` that records calls.
- Admin pages for integration applications, integration connectors and micro applications, registered through `@mango/rbac` and `@mango/admin-pages` with `integration:*` permission codes and `AUTH_MENU` resource declarations.
- Micro-application overview and frontend runtime health check in `mango-authorization`.
- `mango.baseline.enabled` build switch with stale-resource cleanup and the `MANGO_COLD_BASELINE_ENABLED` runtime switch in the CLI full backend template.
- `check-cli` guards for Flyway module declarations, KV capabilities, the security filter chain and the cold baseline switch in generated projects.
- Real MySQL regression coverage for historical tenantless Resource Registry rows.

### Changed

- The complete non-application Maven/docs tuple advances from `1.1.5` to `1.2.0` with 201 coordinates, because the batch introduces the new `mango-integration` module and a consumer-visible file-preview and workflow behavior change.
- npm: `@mango/common` `2.3.0` → `2.3.1`, `@mango/rbac` `1.2.0` → `1.3.0`, `@mango/admin-pages` `1.2.2` → `1.3.0`, `@mango/cli` `1.3.10` → `1.3.11`, plus the generated patch closure.
- The monolith and CLI full backend template declare the `integration` Flyway module and no longer declare the migration-less `notification` module.
- The platform Catalog projection is regenerated so the tracked Maven coordinate list includes the integration modules.

## 3. 升级前检查

1. 确认数据库备份可恢复，尤其是 Resource Registry、Flyway history、Bootstrap receipt 和业务目标表。
2. 阅读当前版本对应的模块 README、能力地图和本文引用的专项指南。
3. 检查业务项目是否引用本批次删除或收紧的 API、配置、枚举、菜单或前端导出。
4. 记录当前 Maven、npm、CLI、PMO 版本以及构建锁文件；不要混用不同发布批次。
5. 先在预发布数据库执行只读检查，再进入维护窗口。

## 4. 依赖升级

### Maven

将业务项目 `backend/pom.xml` 的 Mango 版本锁定为：

```xml
<mango.version>1.2.0</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.2.0` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/common@2.3.1`、`@mango/admin-extension@1.1.5`、`@mango/ai@1.2.5`、`@mango/auth@1.0.42`、`@mango/file@1.2.5`、`@mango/detail@1.1.10`、`@mango/grid-layout@1.0.31`、`@mango/grid-widgets@1.0.37`、`@mango/home@1.0.30`、`@mango/information@1.1.5`、`@mango/rbac@1.3.0`、`@mango/site-shell@2.0.3`、`@mango/system@1.1.2`、`@mango/admin-pages@1.3.0`、`@mango/calendar@1.0.57`、`@mango/cms@1.1.2`、`@mango/job@1.0.50`、`@mango/link@1.0.43`、`@mango/notice@1.0.62`、`@mango/numgen@1.0.58`、`@mango/payment@1.0.49`、`@mango/template@1.0.58`、`@mango/workflow@1.0.66`、`@mango/admin-shell@1.1.2`、`@mango/workflow-business-example@1.0.65`、`@mango/admin@2.0.4`、`@mango/cli@1.3.11`

```bash
pnpm install
pnpm exec mango pmo check --locked
pnpm exec mango docs pull
```

CLI 生成项目使用本批次的 `@mango/cli`；不要用旧的全局 CLI 覆盖项目内锁定版本。

## 5. 数据库与启动验证

1. 备份后启动一次升级候选，等待 Flyway 与 Resource Bootstrap 完成。
2. 确认所有新增迁移成功，Bootstrap receipt 最终为 `FINALIZED`。
3. 对 Resource Registry 检查唯一键、同步模式、归属字段和冲突诊断。
4. 对业务模块执行本文第 2 节涉及的专项验证；迁移失败或身份冲突时停止，不要通过手工修改 Flyway history 绕过。

## 6. 业务回归

- 后端 API、配置、权限、菜单、初始化数据和租户边界。
- 前端构建、路由、页面注册、公开站点和已变更的 npm 导出。
- CLI 生成项目、PMO baseline、docs bundle 和 clean consumer。
- 对本批次 release notes 的 `Fixed`、`Added`、`Changed` 项逐项保留验证证据。

## 7. 回滚边界

- 已发布的 Maven/npm 坐标不可覆盖，只能发布新版本或回退消费者依赖。
- 数据库迁移是否可逆以对应模块 README 为准；不可逆数据操作需要在升级前完成归档或备份。
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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.09-maven-1.2.0-integration-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
