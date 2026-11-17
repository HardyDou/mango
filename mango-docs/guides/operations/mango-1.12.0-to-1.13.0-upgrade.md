<!-- mango-release-upgrade-guide: generated -->
# Mango 1.12.0 → 1.13.0 升级指南

**适用对象：使用 Mango Maven 1.12.0 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.17-maven-1.13.0-task-multi-filter-contract-gate-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.12.0` → `1.13.0`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/pmo@1.6.1`、`@mango/cli@1.4.14`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/pmo` | `1.6.0` | `1.6.1` |
| `@mango/cli` | `1.4.13` | `1.4.14` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- The task page attribute filter no longer silently returns nothing when several business types are queried, because the attribute subquery follows the same business-type choice as the task filter.
- `countTasks` and `selectTasks` cannot diverge for a multi-business-type query, because both include the same business-type fragment.
- The workflow contract sentinel no longer reports a stale expected fingerprint: it prints the actual public-API or HTTP fingerprint with the instruction to update the constant only for an intentional contract change.
- A workflow starter test failure is now visible in the required check; before this batch the module's sixty-three tests ran zero times under the reactor's Surefire 2.17 binding while the build reported success.
- An empty `businessTypes` or `processDefinitionKeys` list no longer risks an invalid `in ()` statement; the mapper falls back to the `1.12.0` equality clause or omits the condition.

### Added

- `WorkflowTaskBusinessPageQuery.businessTypes`: an optional list of up to twenty business types; when it is not empty it becomes `and a.business_type in (...)`, so one request returns project admission, compliance review and contract review tasks in one page with one total.
- `WorkflowTaskBusinessPageQuery.processDefinitionKeys`: an optional list of up to one hundred process definition keys, rendered as `and a.process_definition_key in (...)`, so a workbench can restrict a business type to specific processes.
- One shared mapper fragment applies the business-type condition to the task filter and to the `workflow_business_apply_attribute` subquery, so `applyAttributeFilters` keeps working when a list of business types is queried.
- A CI job `workflow_contract` that executes the whole `mango-workflow-starter` test module with Surefire 3.2.5 and feeds its result into the aggregate `pmo-doc-check` check.
- A four-dimension filter table in the business integration guide that separates business type, process definition key, node definition key and business key, plus a note to display the returned `processKey` and `processName` instead of a domain-level business type.

### Changed

- `businessType` is no longer required at bean-validation level: the service rejects a query only when both `businessType` and `businessTypes` are absent, and it keeps the same `业务类型不能为空` message. Callers that already send `businessType` see no behavioural change.
- When both `businessType` and `businessTypes` are present, the list wins and the single value is ignored; this precedence is documented in the API Javadoc, the module README and the business integration guide.
- `@mango/pmo@1.6.1` records the workflow contract sentinel gate in `rules/05-ai-delivery-quality.md` and refreshes `rules/index.json`; the business PMO baseline projection is synchronised with the same content.
- `mango-docs/guides/operations/mango-1.12.0-to-1.13.0-upgrade.md` is generated from this sealed plan and published with the docs bundle.

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
<mango.version>1.13.0</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.13.0` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/pmo@1.6.1`、`@mango/cli@1.4.14`

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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.17-maven-1.13.0-task-multi-filter-contract-gate-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
