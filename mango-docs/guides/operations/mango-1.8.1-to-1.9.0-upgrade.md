<!-- mango-release-upgrade-guide: generated -->
# Mango 1.8.1 → 1.9.0 升级指南

**适用对象：使用 Mango Maven 1.8.1 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.14-maven-1.9.0-file-edit-apply-attribute-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.8.1` → `1.9.0`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/file@1.3.0`、`@mango/detail@1.1.13`、`@mango/rbac@1.3.3`、`@mango/system@1.1.5`、`@mango/admin-pages@1.3.3`、`@mango/calendar@1.0.60`、`@mango/cms@1.1.5`、`@mango/job@1.0.53`、`@mango/link@1.0.46`、`@mango/notice@1.0.66`、`@mango/numgen@1.0.61`、`@mango/payment@1.0.52`、`@mango/template@1.0.61`、`@mango/workflow@1.2.1`、`@mango/admin-shell@1.1.8`、`@mango/workflow-business-example@1.0.70`、`@mango/admin@2.0.10`、`@mango/cli@1.4.9`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/file` | `1.2.7` | `1.3.0` |
| `@mango/detail` | `1.1.12` | `1.1.13` |
| `@mango/rbac` | `1.3.2` | `1.3.3` |
| `@mango/system` | `1.1.4` | `1.1.5` |
| `@mango/admin-pages` | `1.3.2` | `1.3.3` |
| `@mango/calendar` | `1.0.59` | `1.0.60` |
| `@mango/cms` | `1.1.4` | `1.1.5` |
| `@mango/job` | `1.0.52` | `1.0.53` |
| `@mango/link` | `1.0.45` | `1.0.46` |
| `@mango/notice` | `1.0.65` | `1.0.66` |
| `@mango/numgen` | `1.0.60` | `1.0.61` |
| `@mango/payment` | `1.0.51` | `1.0.52` |
| `@mango/template` | `1.0.60` | `1.0.61` |
| `@mango/workflow` | `1.2.0` | `1.2.1` |
| `@mango/admin-shell` | `1.1.7` | `1.1.8` |
| `@mango/workflow-business-example` | `1.0.69` | `1.0.70` |
| `@mango/admin` | `2.0.9` | `2.0.10` |
| `@mango/cli` | `1.4.8` | `1.4.9` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- A file can be replaced without losing its id, so permissions, business bindings and audit history stay attached to the same record instead of a newly created duplicate.
- A replaced file keeps the original name when the caller does not supply a new one, so business views that render by name do not show an empty title.
- `@mango/cms` row saving no longer depends on an editor config that may omit `create` or `update`; the missing branch returns early instead of calling an undefined callback.
- `@mango/rbac` no longer sends an invalid runtime code when a runtime strategy row has no module code.
- Task paging by apply attributes no longer requires joining business tables per query, which removes the previous key-list workaround and its page-size limit.
- Countersign, or-sign and sequential approvals now take effect for role, position and organization approvers: each resolved member gets a task, and a task that already has an assignee is no longer re-filled with the candidate group.

### Added

- File record editing: `FileApi.updateRecord` sends `UpdateFileRecordCommand` to `PUT /file/files`, and only the supplied metadata fields change while an empty string clears a nullable field.
- File content replacement: `FileApi.replaceContent` keeps the file id, defaults to the original file name, and automatically chooses the one-shot `POST /file/files/replace` path or a chunked upload session created with `replaceFileId`.
- File management edit dialog in `@mango/file` that edits name, purpose, access level, business type, business id, directory and business metadata for one file id.
- Business apply attributes: `workflow_business_apply_attribute` stores one row per value, multi-value attributes are supported, and Flyway `V5` creates the table and its indexes.
- `WorkflowBusinessApplyApi.updateFilterAttributes` replaces the registered attributes of one apply, which covers values that only become known after submission, such as a document version status.
- Task paging accepts `applyAttributeFilters` with an AND between conditions and an OR inside one condition, plus `applyTimeBegin`, `applyTimeEnd` and `sortBy=applyTime`.
- Workflow events carry `applyAttributes`, and default notifications expand a template variable per filterable attribute.
- Candidate-group nodes expand role, position and organization members at runtime, then create one task per member for countersign, or-sign and sequential approval modes; an empty member set returns the `EMPTY_ASSIGNEE` sentinel instead of a shared claim task.
- `WorkflowCandidateGroupProvider.memberUserIds` is the single implementation that maps a candidate group to members, and auto assignment reuses it.

### Changed

- The non-application Maven/docs tuple advances from `1.8.1` to `1.9.0` with 201 coordinates because the file module gains two public commands, one upload-session field and a migration, and the workflow module gains a migration, a public update API and new query fields.
- npm advances `@mango/file` `1.2.7` to `1.3.0` for the new edit and replace capability; `@mango/admin-shell` `1.1.7` to `1.1.8`, `@mango/cms` `1.1.4` to `1.1.5`, `@mango/rbac` `1.3.2` to `1.3.3` and `@mango/workflow` `1.2.0` to `1.2.1` for the gate fixes and documentation; the remaining twelve packages plus `@mango/cli` `1.4.8` to `1.4.9` are generated by the exact-version dependency closure.
- `@mango/pmo` is unchanged because no PMO rule, contract, Agent, template or shipped Skill changed; the new release runbook lives in the repository-only `mango-release` Skill.
- The default notification view target now follows the business `customConfig` for pending-approval notifications as well, and `viewActionEnabled=false` leaves the button out.
- Candidate-group approval nodes change behavior: a node that produced one shared claim task now produces one task per resolved member, so approval counts and pending-task counts increase with the number of members.

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
<mango.version>1.9.0</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.9.0` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/file@1.3.0`、`@mango/detail@1.1.13`、`@mango/rbac@1.3.3`、`@mango/system@1.1.5`、`@mango/admin-pages@1.3.3`、`@mango/calendar@1.0.60`、`@mango/cms@1.1.5`、`@mango/job@1.0.53`、`@mango/link@1.0.46`、`@mango/notice@1.0.66`、`@mango/numgen@1.0.61`、`@mango/payment@1.0.52`、`@mango/template@1.0.61`、`@mango/workflow@1.2.1`、`@mango/admin-shell@1.1.8`、`@mango/workflow-business-example@1.0.70`、`@mango/admin@2.0.10`、`@mango/cli@1.4.9`

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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.14-maven-1.9.0-file-edit-apply-attribute-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
