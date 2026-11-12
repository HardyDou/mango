<!-- mango-release-upgrade-guide: generated -->
# Mango 1.5.0 → 1.6.0 升级指南

**适用对象：使用 Mango Maven 1.5.0 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.13-maven-1.6.0-workflow-business-type-query-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.5.0` → `1.6.0`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/app-runtime@1.0.8`、`@mango/common@2.3.3`、`@mango/admin-extension@1.1.7`、`@mango/ai@1.2.7`、`@mango/auth@1.0.44`、`@mango/file@1.2.7`、`@mango/detail@1.1.12`、`@mango/grid-layout@1.0.33`、`@mango/grid-widgets@1.0.39`、`@mango/home@1.0.32`、`@mango/information@1.1.7`、`@mango/link-openapi@1.0.5`、`@mango/link-page@1.0.10`、`@mango/pmo@1.6.0`、`@mango/rbac@1.3.2`、`@mango/site-shell@2.0.5`、`@mango/system@1.1.4`、`@mango/admin-pages@1.3.2`、`@mango/calendar@1.0.59`、`@mango/cms@1.1.4`、`@mango/job@1.0.52`、`@mango/link@1.0.45`、`@mango/notice@1.0.65`、`@mango/numgen@1.0.60`、`@mango/payment@1.0.51`、`@mango/template@1.0.60`、`@mango/workflow@1.0.68`、`@mango/admin-shell@1.1.5`、`@mango/workflow-business-example@1.0.67`、`@mango/admin@2.0.7`、`@mango/cli@1.4.5`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/app-runtime` | `1.0.7` | `1.0.8` |
| `@mango/common` | `2.3.2` | `2.3.3` |
| `@mango/admin-extension` | `1.1.6` | `1.1.7` |
| `@mango/ai` | `1.2.6` | `1.2.7` |
| `@mango/auth` | `1.0.43` | `1.0.44` |
| `@mango/file` | `1.2.6` | `1.2.7` |
| `@mango/detail` | `1.1.11` | `1.1.12` |
| `@mango/grid-layout` | `1.0.32` | `1.0.33` |
| `@mango/grid-widgets` | `1.0.38` | `1.0.39` |
| `@mango/home` | `1.0.31` | `1.0.32` |
| `@mango/information` | `1.1.6` | `1.1.7` |
| `@mango/link-openapi` | `1.0.4` | `1.0.5` |
| `@mango/link-page` | `1.0.9` | `1.0.10` |
| `@mango/pmo` | `1.5.0` | `1.6.0` |
| `@mango/rbac` | `1.3.1` | `1.3.2` |
| `@mango/site-shell` | `2.0.4` | `2.0.5` |
| `@mango/system` | `1.1.3` | `1.1.4` |
| `@mango/admin-pages` | `1.3.1` | `1.3.2` |
| `@mango/calendar` | `1.0.58` | `1.0.59` |
| `@mango/cms` | `1.1.3` | `1.1.4` |
| `@mango/job` | `1.0.51` | `1.0.52` |
| `@mango/link` | `1.0.44` | `1.0.45` |
| `@mango/notice` | `1.0.64` | `1.0.65` |
| `@mango/numgen` | `1.0.59` | `1.0.60` |
| `@mango/payment` | `1.0.50` | `1.0.51` |
| `@mango/template` | `1.0.59` | `1.0.60` |
| `@mango/workflow` | `1.0.67` | `1.0.68` |
| `@mango/admin-shell` | `1.1.4` | `1.1.5` |
| `@mango/workflow-business-example` | `1.0.66` | `1.0.67` |
| `@mango/admin` | `2.0.6` | `2.0.7` |
| `@mango/cli` | `1.4.4` | `1.4.5` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- Business list pages no longer need one workflow query per application: a single request returns all tasks for the requested business keys, including returned, withdrawn and terminated ones, with a machine-readable `cancelCause`.
- `assigneeIds` mapping now resolves engine user names through `identity_user.username` and validates tenant membership, so same-named users in other tenants cannot leak tasks into the result.
- The task history `START_TIME_` column is used for creation time instead of the non-existent `CREATE_TIME_`, and empty `endTime` / `claimTime` sort last in both directions.
- `scope=SELF` and `scope=ALL` reject `deptIds` instead of silently ignoring it, and `scope=ORG` is rejected in the current version so callers cannot assume organization filtering happened.
- Frontend quality gate findings that blocked `main` are resolved without changing component behavior: contracts are complete, formatting is stable and the workflow test regex no longer emits an ESLint warning.

### Added

- `POST /workflow/tasks/page-by-business-key` with `scope=SELF` (default) and `scope=ALL`, `statuses`, `assigneeIds`, `applyStatus`, `returned`, `deptIds`, pagination and sorting by `startTime`, `endTime`, `claimTime` and `createTime`.
- `mango.workflow.task-query.unpaged-max-rows` (default `2000`) bounds unpaged responses while `total` always reports the true filtered count.
- `cancelCause` on terminated tasks with `RETURNED`, `WITHDRAWN`, `TERMINATED` and `OTHER`, plus `lastReturnAt` and `lastReturnTarget` derived from the latest `RETURN` action.
- Maven plugin properties `mango.check.reportOnlyRules`, `mango.check.checkstyle.cyclomaticComplexityMax` and `mango.check.checkstyle.fileLengthMax`.
- Business PMO baseline projection with the reduced required-check set.

### Changed

- Branch protection requires only `pr-contract-check` and `pmo-doc-check`; `frontend-pr-quality` still runs on `main` push and by manual trigger but no longer blocks merges.
- The Maven plugin's ordinary check gate now runs a partial Reactor and only falls back to the full Reactor for POM, rulesets, architecture/baseline code and architecture rules.
- The non-application Maven/docs tuple advances from `1.4.2` to `1.5.0` with 201 coordinates.
- npm: `@mango/pmo` `1.4.9` → `1.5.0`, `@mango/common` `2.3.1` → `2.3.2`, `@mango/rbac` `1.3.0` → `1.3.1`, `@mango/system` `1.1.2` → `1.1.3`, `@mango/admin-shell` `1.1.3` → `1.1.4`, `@mango/workflow` `1.0.66` → `1.0.67`, `@mango/cli` `1.4.3` → `1.4.4`, plus the generated patch closure.

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
<mango.version>1.6.0</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.6.0` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/app-runtime@1.0.8`、`@mango/common@2.3.3`、`@mango/admin-extension@1.1.7`、`@mango/ai@1.2.7`、`@mango/auth@1.0.44`、`@mango/file@1.2.7`、`@mango/detail@1.1.12`、`@mango/grid-layout@1.0.33`、`@mango/grid-widgets@1.0.39`、`@mango/home@1.0.32`、`@mango/information@1.1.7`、`@mango/link-openapi@1.0.5`、`@mango/link-page@1.0.10`、`@mango/pmo@1.6.0`、`@mango/rbac@1.3.2`、`@mango/site-shell@2.0.5`、`@mango/system@1.1.4`、`@mango/admin-pages@1.3.2`、`@mango/calendar@1.0.59`、`@mango/cms@1.1.4`、`@mango/job@1.0.52`、`@mango/link@1.0.45`、`@mango/notice@1.0.65`、`@mango/numgen@1.0.60`、`@mango/payment@1.0.51`、`@mango/template@1.0.60`、`@mango/workflow@1.0.68`、`@mango/admin-shell@1.1.5`、`@mango/workflow-business-example@1.0.67`、`@mango/admin@2.0.7`、`@mango/cli@1.4.5`

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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.13-maven-1.6.0-workflow-business-type-query-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
