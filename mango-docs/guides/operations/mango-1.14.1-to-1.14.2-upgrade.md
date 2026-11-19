<!-- mango-release-upgrade-guide: generated -->
# Mango 1.14.1 → 1.14.2 升级指南

**适用对象：使用 Mango Maven 1.14.1 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.19-maven-1.14.2-disabled-module-resources-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.14.1` → `1.14.2`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/cli@1.4.17`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/cli` | `1.4.16` | `1.4.17` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- `mango-infra-persistence-starter` 与 `mango-resource-sync-starter` 新增模块启停契约 `PersistenceModuleEnablement`：某模块的 Flyway 迁移被关闭后，该模块在 `META-INF/mango/resources` 下的便携资源声明与该模块的打包清单条目一并跳过，不再出现 `Table ... doesn't exist` 的启动失败。
- 跳过行为只影响声明装载，不改变迁移开关本身；`skip-reason` 仍是配置项，跳过日志同时打印模块名、声明数量、跳过原因与来源文件，便于定位是哪个模块被关闭。
- `/actuator/health` 之后的探针分组路径（`readiness`、`liveness`）现在命中平台默认白名单规则，不再因为规则只写精确路径而落到登录校验上返回 401。
- 白名单匹配把 IPv4 映射地址按 IPv4 比较、精确地址与网段比较使用同一套字节比较，缩写网段（如 `10/8`、`172.16/12`、`192.168/16`）不再静默匹配到错误网段。

### Changed

- 平台单体默认白名单规则从 `path-pattern: /actuator/health` 调整为 `/actuator/health/**`，因为 `/actuator/**` 不参与 API 资源声明同步，探针路径只能依赖该白名单。
- `mango-access-web-starter` 在启动装配阶段对 IP 白名单规则做写法自检：网段未写满四段、掩码越界或地址无法解析时输出 WARN `IP 白名单 CIDR 不是完整网段写法`，不阻断启动。
- 冷基线操作指南新增模块顺序、分组与隐式跨模块依赖三节：发现顺序为模块名字典序、`MANGO-BASELINE-023` 只比较集合成员、业务模块向平台表写种子数据时按依赖顺序显式列出完整模块序列。
- 本批次不涉及 `sys_dict_type`、`sys_dict_data`、`authorization_role` 的列集或约束变更，业务模块向这些表写种子数据的声明无需调整。

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
<mango.version>1.14.2</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.14.2` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/cli@1.4.17`

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

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.19-maven-1.14.2-disabled-module-resources-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
