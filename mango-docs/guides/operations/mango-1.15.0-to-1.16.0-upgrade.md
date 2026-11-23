<!-- mango-release-upgrade-guide: generated -->
# Mango 1.15.0 → 1.16.0 升级指南

**适用对象：使用 Mango Maven 1.15.0 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.23-maven-1.16.0-workflow-task-status-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.15.0` → `1.16.0`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/http-client@1.1.0`、`@mango/common@2.4.0`、`@mango/admin-extension@1.1.10`、`@mango/ai@1.2.10`、`@mango/auth@1.0.47`、`@mango/file@1.4.1`、`@mango/detail@1.1.16`、`@mango/grid-layout@1.0.36`、`@mango/grid-widgets@1.0.42`、`@mango/home@1.0.35`、`@mango/information@1.2.0`、`@mango/pmo@1.6.4`、`@mango/rbac@1.3.6`、`@mango/site-shell@2.0.8`、`@mango/system@1.1.8`、`@mango/admin-pages@1.3.6`、`@mango/calendar@1.0.63`、`@mango/cms@1.1.8`、`@mango/job@1.0.56`、`@mango/link@1.0.49`、`@mango/notice@1.0.69`、`@mango/numgen@1.0.64`、`@mango/payment@1.0.55`、`@mango/template@1.0.64`、`@mango/workflow@1.4.0`、`@mango/admin-shell@1.1.12`、`@mango/workflow-business-example@1.0.74`、`@mango/admin@2.0.14`、`@mango/cli@1.4.19`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/http-client` | `1.0.2` | `1.1.0` |
| `@mango/common` | `2.3.5` | `2.4.0` |
| `@mango/admin-extension` | `1.1.9` | `1.1.10` |
| `@mango/ai` | `1.2.9` | `1.2.10` |
| `@mango/auth` | `1.0.46` | `1.0.47` |
| `@mango/file` | `1.4.0` | `1.4.1` |
| `@mango/detail` | `1.1.15` | `1.1.16` |
| `@mango/grid-layout` | `1.0.35` | `1.0.36` |
| `@mango/grid-widgets` | `1.0.41` | `1.0.42` |
| `@mango/home` | `1.0.34` | `1.0.35` |
| `@mango/information` | `1.1.9` | `1.2.0` |
| `@mango/pmo` | `1.6.3` | `1.6.4` |
| `@mango/rbac` | `1.3.5` | `1.3.6` |
| `@mango/site-shell` | `2.0.7` | `2.0.8` |
| `@mango/system` | `1.1.7` | `1.1.8` |
| `@mango/admin-pages` | `1.3.5` | `1.3.6` |
| `@mango/calendar` | `1.0.62` | `1.0.63` |
| `@mango/cms` | `1.1.7` | `1.1.8` |
| `@mango/job` | `1.0.55` | `1.0.56` |
| `@mango/link` | `1.0.48` | `1.0.49` |
| `@mango/notice` | `1.0.68` | `1.0.69` |
| `@mango/numgen` | `1.0.63` | `1.0.64` |
| `@mango/payment` | `1.0.54` | `1.0.55` |
| `@mango/template` | `1.0.63` | `1.0.64` |
| `@mango/workflow` | `1.3.2` | `1.4.0` |
| `@mango/admin-shell` | `1.1.11` | `1.1.12` |
| `@mango/workflow-business-example` | `1.0.73` | `1.0.74` |
| `@mango/admin` | `2.0.13` | `2.0.14` |
| `@mango/cli` | `1.4.18` | `1.4.19` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- 已办列表不再丢失本人驳回 / 退回的记录（此前被计入「已取消」）。
- 全新库启动时的 71 条结构校验警告归零：notice 模块 22 张表与 `mango_bootstrap*` / `mango_runtime*` 表不再按平台审计列强制校验，`workflow_process_participant.id` 改用雪花主键。
- 文件预览入口不再硬编码 `/api` 前缀，直连后端的部署与脚本可直接使用。
- `Mango Architecture Debt Inventory` 定时任务不再因 `invalid --base-ref: <sha>^` 长期失败。
- `dev start backend` 在干净环境与本地仓库缓存异常时能解析 reactor 父 POM 链。
- CLI 生成的工程质量配置与 `mango-maven-plugin` ruleset 保持一致；workspace 状态探测不再依赖宿主 `mysql` 客户端。

### Added

- 任务分页支持按「谁办了、怎么结束的」精确筛选：`statuses=[REJECTED]` 只要驳回、`[RETURNED]` 只要退回、`[WITHDRAWN]` 只要撤回、`[TERMINATED]` 只要流程终止或被他人驳回牵连作废，任意组合取并集。
- 任务分页支持「本部门」档：角色在数据范围里配置「本组织」后，列表按**处理人**所属组织收敛（上级部门已由平台递归展开），前端与业务不再传部门参数。
- 活动时间轴接口 `POST /workflow/tasks/activity-timeline-by-business-key`（`@mango/workflow@1.4.0` 同批发布）。
- 信息表单选择字段可绑定平台字典作为选项数据源（`optionSource: {type: "DICT", dictType}`），提交时只对声明字典的字段按启用项校验。
- 新增 PMO 门禁：`check-admin-aggregate`（装配点可达性）与 `check-migration-duplicates`（同模块重复建表）。

### Changed

- **`statuses=[DONE]` 的返回行数会变大**：本人驳回或退回、随后流程实例被删除的任务从「已取消」移入「已办」；被他人驳回牵连作废的任务仍在「已取消」组。
- **`scope=ORG` 与 `deptIds` 下线**：`scope=ORG` 直接报错（`TASK_QUERY_INVALID`，文案提示改用数据范围配置），`deptIds` 非空同样报错；部门档改由数据范围下发，未配置时行为与 `1.15.0` 完全一致。
- 数据范围配成「全部」但用户缺少 `workflow:task:all` 时，降级为本人范围并记 WARN；`scope=ALL` 且无该权限仍返回 403。
- `TENANT` / `CUSTOM` 数据范围模式在任务列表降级为本人范围并记 WARN（`DataScopeApplier` 原实现对二者抛 `Unsupported`）。
- 数据权限 Grant 默认关闭并登记为半成品（`mango.data-grant.enabled=false`），未完成项见 issue #1420。
- 信息表单静态 `options` 只用于渲染建议项，不参与提交值域校验；只有 `optionSource: DICT` 的字段按字典校验值域。

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
<mango.version>1.16.0</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.16.0` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/http-client@1.1.0`、`@mango/common@2.4.0`、`@mango/admin-extension@1.1.10`、`@mango/ai@1.2.10`、`@mango/auth@1.0.47`、`@mango/file@1.4.1`、`@mango/detail@1.1.16`、`@mango/grid-layout@1.0.36`、`@mango/grid-widgets@1.0.42`、`@mango/home@1.0.35`、`@mango/information@1.2.0`、`@mango/pmo@1.6.4`、`@mango/rbac@1.3.6`、`@mango/site-shell@2.0.8`、`@mango/system@1.1.8`、`@mango/admin-pages@1.3.6`、`@mango/calendar@1.0.63`、`@mango/cms@1.1.8`、`@mango/job@1.0.56`、`@mango/link@1.0.49`、`@mango/notice@1.0.69`、`@mango/numgen@1.0.64`、`@mango/payment@1.0.55`、`@mango/template@1.0.64`、`@mango/workflow@1.4.0`、`@mango/admin-shell@1.1.12`、`@mango/workflow-business-example@1.0.74`、`@mango/admin@2.0.14`、`@mango/cli@1.4.19`

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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.23-maven-1.16.0-workflow-task-status-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
