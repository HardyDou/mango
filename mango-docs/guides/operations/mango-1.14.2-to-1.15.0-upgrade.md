<!-- mango-release-upgrade-guide: generated -->
# Mango 1.14.2 → 1.15.0 升级指南

**适用对象：使用 Mango Maven 1.14.2 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.21-maven-1.15.0-preview-fileid-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.14.2` → `1.15.0`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/common@2.3.5`、`@mango/admin-extension@1.1.9`、`@mango/ai@1.2.9`、`@mango/auth@1.0.46`、`@mango/file@1.4.0`、`@mango/detail@1.1.15`、`@mango/grid-layout@1.0.35`、`@mango/grid-widgets@1.0.41`、`@mango/home@1.0.34`、`@mango/information@1.1.9`、`@mango/rbac@1.3.5`、`@mango/site-shell@2.0.7`、`@mango/system@1.1.7`、`@mango/admin-pages@1.3.5`、`@mango/calendar@1.0.62`、`@mango/cms@1.1.7`、`@mango/job@1.0.55`、`@mango/link@1.0.48`、`@mango/notice@1.0.68`、`@mango/numgen@1.0.63`、`@mango/payment@1.0.54`、`@mango/template@1.0.63`、`@mango/workflow@1.3.2`、`@mango/admin-shell@1.1.11`、`@mango/workflow-business-example@1.0.73`、`@mango/admin@2.0.13`、`@mango/cli@1.4.18`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/common` | `2.3.4` | `2.3.5` |
| `@mango/admin-extension` | `1.1.8` | `1.1.9` |
| `@mango/ai` | `1.2.8` | `1.2.9` |
| `@mango/auth` | `1.0.45` | `1.0.46` |
| `@mango/file` | `1.3.1` | `1.4.0` |
| `@mango/detail` | `1.1.14` | `1.1.15` |
| `@mango/grid-layout` | `1.0.34` | `1.0.35` |
| `@mango/grid-widgets` | `1.0.40` | `1.0.41` |
| `@mango/home` | `1.0.33` | `1.0.34` |
| `@mango/information` | `1.1.8` | `1.1.9` |
| `@mango/rbac` | `1.3.4` | `1.3.5` |
| `@mango/site-shell` | `2.0.6` | `2.0.7` |
| `@mango/system` | `1.1.6` | `1.1.7` |
| `@mango/admin-pages` | `1.3.4` | `1.3.5` |
| `@mango/calendar` | `1.0.61` | `1.0.62` |
| `@mango/cms` | `1.1.6` | `1.1.7` |
| `@mango/job` | `1.0.54` | `1.0.55` |
| `@mango/link` | `1.0.47` | `1.0.48` |
| `@mango/notice` | `1.0.67` | `1.0.68` |
| `@mango/numgen` | `1.0.62` | `1.0.63` |
| `@mango/payment` | `1.0.53` | `1.0.54` |
| `@mango/template` | `1.0.62` | `1.0.63` |
| `@mango/workflow` | `1.3.1` | `1.3.2` |
| `@mango/admin-shell` | `1.1.10` | `1.1.11` |
| `@mango/workflow-business-example` | `1.0.72` | `1.0.73` |
| `@mango/admin` | `2.0.12` | `2.0.13` |
| `@mango/cli` | `1.4.17` | `1.4.18` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- 内嵌部署 `fileId` 预览 403（信任过滤器不看票据）。
- 图片/音视频预览把取源票据当资源地址，页内 401/404。
- `fileId` 通道下 `xlsx`/`csv` 转换任务被枚举判定短路，导致「任务成功但无产物、页面回落引擎表格页」。
- 独立部署预览跳转带 `/api` 前缀导致 404。
- `baseline-generate` 误把测试夹具当迁移源，触发 `MANGO-BASELINE-009`。
- checkstyle 复杂度指纹嵌入阈值，调整阈值即让存量基线条目变成「假新增」。
- 压缩包解包内容可被匿名读取（解包目录落在 `file-dir` 内且被 `/preview-file/**` 映射），现已移出映射根并改为受控端点读取。

### Added

- 独立部署：`mango-file-preview-starter-remote`（引擎侧装机）+ `mango.file-preview.remote.enabled` / `remote.base-url`，引擎可直接回源应用取 `fileId` 字节。
- 独立部署：`mango.file-preview.engine-public-base-url`，浏览器跳转直指引擎 origin；配置后平台自动剥离应用网关 `/api` 前缀。
- `FilePreviewRenderMode`（`PDF`/`IMAGE`/`VIDEO`/`AUDIO`/`CONVERTING`/`PAGE`/`NOT_SUPPORTED`）与 `FilePreviewLinkVO.renderMode`，前端按它分派渲染。
- `mango.fileproc.aspose.fonts-folders` 与 `mango.fileproc.aspose.font-check-fatal`（默认 `false`：字体缺失只告警，授权不可用仍启动失败）。

### Changed

- 默认转换引擎换成 Aspose（LibreOffice 兜底）。这是业务可见的行为变更。
- 废弃格式类 checkstyle 规则 11 条；仓库基线 15,067 → 2,948。
- 前端预览地址类 props（`preview`、`previewUrl`、`documentPreviewUrl`、`directPreviewUrl`、`previewProviderUrl`、`previewExternalExtensions`）标记弃用，下一个 major 移除。
- `url` 通道保留但不推荐；`fileId` 为推荐路径。

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
<mango.version>1.15.0</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.15.0` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/common@2.3.5`、`@mango/admin-extension@1.1.9`、`@mango/ai@1.2.9`、`@mango/auth@1.0.46`、`@mango/file@1.4.0`、`@mango/detail@1.1.15`、`@mango/grid-layout@1.0.35`、`@mango/grid-widgets@1.0.41`、`@mango/home@1.0.34`、`@mango/information@1.1.9`、`@mango/rbac@1.3.5`、`@mango/site-shell@2.0.7`、`@mango/system@1.1.7`、`@mango/admin-pages@1.3.5`、`@mango/calendar@1.0.62`、`@mango/cms@1.1.7`、`@mango/job@1.0.55`、`@mango/link@1.0.48`、`@mango/notice@1.0.68`、`@mango/numgen@1.0.63`、`@mango/payment@1.0.54`、`@mango/template@1.0.63`、`@mango/workflow@1.3.2`、`@mango/admin-shell@1.1.11`、`@mango/workflow-business-example@1.0.73`、`@mango/admin@2.0.13`、`@mango/cli@1.4.18`

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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.21-maven-1.15.0-preview-fileid-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
