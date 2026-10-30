<!-- mango-release-upgrade-guide: generated -->
# Mango 1.1.2 → 1.1.3 升级指南

**适用对象：使用 Mango Maven 1.1.2 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.10.29-maven-1.1.3-dict-captcha-cms-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.1.2` → `1.1.3`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/common@2.3.0`、`@mango/admin-extension@1.1.3`、`@mango/ai@1.2.3`、`@mango/auth@1.0.41`、`@mango/file@1.2.3`、`@mango/detail@1.1.8`、`@mango/grid-layout@1.0.30`、`@mango/grid-widgets@1.0.36`、`@mango/home@1.0.29`、`@mango/information@1.1.3`、`@mango/pmo@1.4.9`、`@mango/rbac@1.2.0`、`@mango/site-shell@2.0.2`、`@mango/system@1.1.0`、`@mango/admin-pages@1.2.1`、`@mango/calendar@1.0.55`、`@mango/cms@1.1.0`、`@mango/job@1.0.48`、`@mango/link@1.0.41`、`@mango/notice@1.0.60`、`@mango/numgen@1.0.56`、`@mango/payment@1.0.47`、`@mango/template@1.0.56`、`@mango/workflow@1.0.64`、`@mango/admin-shell@1.1.0`、`@mango/workflow-business-example@1.0.63`、`@mango/admin@2.0.2`、`@mango/cli@1.3.7`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/common` | `2.2.0` | `2.3.0` |
| `@mango/admin-extension` | `1.1.2` | `1.1.3` |
| `@mango/ai` | `1.2.2` | `1.2.3` |
| `@mango/auth` | `1.0.40` | `1.0.41` |
| `@mango/file` | `1.2.2` | `1.2.3` |
| `@mango/detail` | `1.1.7` | `1.1.8` |
| `@mango/grid-layout` | `1.0.29` | `1.0.30` |
| `@mango/grid-widgets` | `1.0.35` | `1.0.36` |
| `@mango/home` | `1.0.28` | `1.0.29` |
| `@mango/information` | `1.1.2` | `1.1.3` |
| `@mango/pmo` | `1.4.8` | `1.4.9` |
| `@mango/rbac` | `1.1.0` | `1.2.0` |
| `@mango/site-shell` | `2.0.1` | `2.0.2` |
| `@mango/system` | `1.0.52` | `1.1.0` |
| `@mango/admin-pages` | `1.2.0` | `1.2.1` |
| `@mango/calendar` | `1.0.54` | `1.0.55` |
| `@mango/cms` | `1.0.43` | `1.1.0` |
| `@mango/job` | `1.0.47` | `1.0.48` |
| `@mango/link` | `1.0.40` | `1.0.41` |
| `@mango/notice` | `1.0.59` | `1.0.60` |
| `@mango/numgen` | `1.0.55` | `1.0.56` |
| `@mango/payment` | `1.0.46` | `1.0.47` |
| `@mango/template` | `1.0.55` | `1.0.56` |
| `@mango/workflow` | `1.0.63` | `1.0.64` |
| `@mango/admin-shell` | `1.0.85` | `1.1.0` |
| `@mango/workflow-business-example` | `1.0.62` | `1.0.63` |
| `@mango/admin` | `2.0.1` | `2.0.2` |
| `@mango/cli` | `1.3.6` | `1.3.7` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- 修复 Resource Registry FINALIZE 缺失资源回收、API Resource 软删除恢复、字典清空保存和 CMS 发布记录一致性问题。

### Added

- 新增 FLAT/TREE 字典、字典分组过滤、DictDisplay、API Resource 人工接管和非生产固定验证码测试支持。

### Changed

- 统一图形验证码行为，改进 CMS 发布历史模型，调整本地 rolling bootstrap，并同步 PMO/CLI 治理契约。

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
<mango.version>1.1.3</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.1.3` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/common@2.3.0`、`@mango/admin-extension@1.1.3`、`@mango/ai@1.2.3`、`@mango/auth@1.0.41`、`@mango/file@1.2.3`、`@mango/detail@1.1.8`、`@mango/grid-layout@1.0.30`、`@mango/grid-widgets@1.0.36`、`@mango/home@1.0.29`、`@mango/information@1.1.3`、`@mango/pmo@1.4.9`、`@mango/rbac@1.2.0`、`@mango/site-shell@2.0.2`、`@mango/system@1.1.0`、`@mango/admin-pages@1.2.1`、`@mango/calendar@1.0.55`、`@mango/cms@1.1.0`、`@mango/job@1.0.48`、`@mango/link@1.0.41`、`@mango/notice@1.0.60`、`@mango/numgen@1.0.56`、`@mango/payment@1.0.47`、`@mango/template@1.0.56`、`@mango/workflow@1.0.64`、`@mango/admin-shell@1.1.0`、`@mango/workflow-business-example@1.0.63`、`@mango/admin@2.0.2`、`@mango/cli@1.3.7`

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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.10.29-maven-1.1.3-dict-captcha-cms-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
