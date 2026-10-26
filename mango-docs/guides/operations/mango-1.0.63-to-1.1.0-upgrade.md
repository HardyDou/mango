<!-- mango-release-upgrade-guide: manual -->
# Mango 1.0.63 → 1.1.0 升级指南

**适用对象：所有使用 Maven `1.0.63` 及更早版本、准备升级到 `1.1.0` 的业务项目。** 本文同时面向人和 Agent：人按章节顺序阅读，Agent 按第 9 节的执行清单逐条落地。

发布批次：`v2026.10.21-maven-1.1.0-resource-sync-ownership-release`。这是一次**破坏性版本**：资源同步模式枚举收敛，CMS Banner 能力删除，`@mango/site-shell` 与 `@mango/admin` 进入主版本。

## 1. 版本矩阵

| 对象 | 升级前 | 升级后 | 是否破坏性 |
|---|---|---|---|
| Maven / docs bundle | `1.0.63` | `1.1.0` | 是（枚举收敛、API 删除） |
| `@mango/site-shell` | `1.0.24` | `2.0.0` | 是（删除 Banner 导出） |
| `@mango/admin` | `1.2.1` | `2.0.0` | 是（聚合多个破坏性升级） |
| `@mango/admin-shell` | `1.0.83` | `1.0.84` | 否 |
| `@mango/cms` | `1.0.41` | `1.0.42` | 否 |
| `@mango/rbac` | `1.0.40` | `1.0.41` | 否 |
| 其余 npm 闭包（`system`、`admin-pages`、`calendar`、`job`、`link`、`notice`、`numgen`、`payment`、`template`、`workflow`、`workflow-business-example`） | — | 依赖闭包内递增 | 否 |
| `@mango/cli` | `1.3.3` | `1.3.4` | 否 |
| `@mango/pmo` | `1.4.7` | 不变 | — |

npm 需要整体升级到本批次闭包，不要只挑其中几个包；`@mango/admin` 2.0.0 依赖新的 `site-shell` 2.0.0。

## 2. 变更摘要

| 变更 | 影响对象 | 需要做的事 |
|---|---|---|
| 资源同步模式收敛为 `SYNC_ALWAYS` / `SYNC_ONCE` | 所有自定义 Resource 声明 | 改写旧值 `AUTO`/`INIT_ONLY`/`MANUAL`/`LOCKED` |
| `V3__normalize_resource_registry_unique_keys.sql` | 老库 `resource_registry` | 升级前检查物理重复身份 |
| `V4__add_resource_registry_ownership.sql` + 资源归属 | 老库 `resource_registry` | 升级后查看归属未证明日志 |
| 缺失资源停用只覆盖 `RESOURCE_MANAGED` + `SYNC_ALWAYS` | 依赖声明自动删除资源的老项目 | 手工确认历史残留资源 |
| CMS Banner 能力删除 | 用到 Banner 的项目 | 见 [CMS Banner 能力删除升级](./cms-banner-removal-upgrade.md) |
| `@mango/site-shell` 删除 `listBanners`/`SiteBanner`/`CmsBanner` | 站点前台 | 改用 `listAdvertisements` |
| 信息采集匿名提交风控 | 使用 `mango-information` 的项目 | 按第 6 节确认阈值与降级策略 |
| Admin 菜单高亮、滚动复位、页签搜索状态 | 所有后台 | 只需刷新构建产物 |

语义说明：

- `SYNC_ALWAYS`：以声明为准。创建缺失资源、覆盖声明变更、声明消失时停用目标。
- `SYNC_ONCE`：只保证资源存在。创建后不覆盖目标业务表，也不因声明消失而停用。
- 旧值读取映射：`AUTO` → `SYNC_ALWAYS`；`INIT_ONLY`、`MANUAL`、`LOCKED` → `SYNC_ONCE`，并输出弃用告警。**新写入只产生新值**，旧值不要继续出现在新声明里。

## 3. 升级前自检（只读）

在项目仓库根目录执行：

```bash
# A. 是否还有旧同步模式
grep -rnE "sync-mode:\s*(AUTO|INIT_ONLY|MANUAL|LOCKED)|syncMode:\s*(AUTO|INIT_ONLY|MANUAL|LOCKED)" \
  --include=*.yml --include=*.yaml --include=*.json --include=*.java \
  --exclude-dir=node_modules --exclude-dir=target .

# B. 是否还引用 Banner 能力
grep -rnE "cms_banner|/cms/banners|banners/list|listBanners|SiteBanner|CmsBanner" \
  --include=*.java --include=*.ts --include=*.vue --include=*.json --include=*.sql \
  --exclude-dir=node_modules --exclude-dir=target --exclude-dir=dist .
```

在目标库执行：

```sql
-- C. V3 前置：同一 resource_id 或同一 (resource_type, biz_key) 是否存在物理重复
SELECT resource_type, biz_key, COUNT(*) AS rows_dup
FROM resource_registry
GROUP BY resource_type, biz_key HAVING COUNT(*) > 1;

SELECT resource_id, COUNT(*) AS rows_dup
FROM resource_registry
GROUP BY resource_id HAVING COUNT(*) > 1;

-- D. 历史归属分布（升级后 V4 会把存量行初始化为 UNKNOWN）
SELECT ownership, COUNT(*) FROM resource_registry GROUP BY ownership;
```

判定表：

| 结果 | 含义 | 下一步 |
|---|---|---|
| A 无命中 | 没有旧同步模式 | 跳过第 4.2 节 |
| A 有命中 | 存在旧声明 | 按第 4.2 节逐条改写 |
| B 有命中 | 依赖 Banner | 先按 Banner 指南处理，再执行本文 |
| C 有结果 | 老库存在物理重复身份 | **先按 `mango-resource` README 14.2 清理**，否则 `V3` 直接失败 |
| C 无结果 | 唯一键可安全收敛 | 继续第 4 节 |

## 4. 升级步骤

### 4.1 备份与窗口

1. 备份数据库（至少 `resource_registry`、`resource_sync_log`、`resource_change_log`、`flyway_schema_history_resource`、`mango_bootstrap_control` 和目标业务表）。
2. 在维护窗口内升级；首次启动会执行 `V3`、`V4` 资源迁移。
3. 同时备份前端构建产物和当前 `package.json` 锁版本，便于回滚。

### 4.2 改写资源同步模式

按语义选择，不要按旧值机械替换：

| 旧值 | 新值 | 判断依据 |
|---|---|---|
| `AUTO` | `SYNC_ALWAYS` | 框架持续维护、应随声明覆盖的数据（菜单、按钮权限、接口资源、字典） |
| `INIT_ONLY` / `MANUAL` / `LOCKED` | `SYNC_ONCE` | 首次铺底后由用户或业务运行时修改的数据（组织、岗位、表单、提示词、支付渠道等） |

```yaml
# 升级前
sync-mode: INIT_ONLY
# 升级后
sync-mode: SYNC_ONCE
```

旧值不会让启动失败，但会持续产生弃用告警；声明里残留旧值会让后续排查噪音变大，本次一次性清理干净。

### 4.3 清理 Banner 残留

用到 CMS Banner 的项目需要先执行 [CMS Banner 能力删除升级](./cms-banner-removal-upgrade.md) 第 3 节：归档 `cms_banner`、删除 `resource_type = 'CMS_BANNER'` 的注册行。**未清理会导致 Bootstrap 在缺失资源停用阶段失败。**

### 4.4 升级依赖

```bash
# Maven
# 将 backend/pom.xml 的 <mango.version> 改为 1.1.0

# npm（在 frontend 目录，按项目实际依赖集）
pnpm add @mango/admin@2.0.0 @mango/site-shell@2.0.0
pnpm add -D @mango/cli@1.3.4
pnpm install
```

`mango.config.json` 中的 `mangoBackendVersion` 与 `mangoFrontendVersions` 要同步改成 `1.1.0` 与本批次版本，否则 `mango docs pull` 和后续 `mango pmo check --locked` 会取到旧口径。

### 4.5 前端适配

站点前台（`@mango/site-shell`）：

```diff
- import { cmsSiteApi, type SiteBanner } from '@mango/site-shell';
- const banners: SiteBanner[] = await cmsSiteApi.listBanners({ ...query, position: 'HOME_HERO' });
- banners[0]?.mediaFileId
+ import { cmsSiteApi, type SiteAdvertisement } from '@mango/site-shell';
+ const heroes: SiteAdvertisement[] = await cmsSiteApi.listAdvertisements({ ...query, position: 'HOME_HERO' });
+ heroes[0]?.imageUrl
```

后台（`@mango/cms`）：Banner 维护入口已删除，统一走「广告位管理 + 广告投放管理」。若项目自建过 Banner 页面，删除页面注册项，不要保留指向已删除接口的路由。

## 5. 资源归属与缺失停用

`V4` 为 `resource_registry` 增加 `ownership`：

| 值 | 含义 | 声明能否覆盖/停用 |
|---|---|---|
| `RESOURCE_MANAGED` | 由 Resource 声明创建或更新 | 可以 |
| `MANUAL_TAKEOVER` | 运行时被人工接管 | 不可以 |
| `UNKNOWN` | 升级前的历史行，无法证明来源 | 不可以 |

- 升级后存量行默认 `UNKNOWN`，所以**升级本身不会删除任何历史资源**。
- 每次 `finalize` 输出一行 `Mango resource ownership unproven`，汇总无法证明归属、因此保持原状的缺失资源条数与类型分布。这是只读日志。
- 想交还声明管理：`PUT /resource/registries/sync-mode` 切回 `SYNC_ALWAYS`，归属回到 `RESOURCE_MANAGED`。
- 确认某条 `UNKNOWN` 资源确实应被删除时，按目标表反查真实归属后人工改归属，或提供一次性收敛脚本；不要直接删 `resource_registry` 行。

## 6. 信息采集匿名提交风控

新版本对 `POST /information/open/forms/submit` 增加固定窗口风控，默认开启：

| 配置 | 默认值 | 行为 |
|---|---|---|
| `mango.information.public-submit.protection.enabled` | `true` | 关闭后不计数、不触发验证码 |
| `mango.information.public-submit.protection.window-seconds` | `60` | 固定窗口长度 |
| `mango.information.public-submit.protection.risk-threshold` | `10` | 达到后需要携带一次性验证码 |
| `mango.information.public-submit.protection.max-requests` | `30` | 超过返回 HTTP 429，且不写入采集记录 |
| `mango.information.public-submit.protection.key-dimension` | `IP_AND_FORM` | 可选 `IP` |
| `mango.information.public-submit.protection.fail-open` | `true` | KV 故障时放行；`false` 时返回 503 |

多实例生产环境应使用共享 KV，并按安全要求决定 `fail-open`。接入方在达到阈值后需提交 `X-Captcha-Key`、`X-Captcha-Code`（可选 `X-Captcha-Type`、`X-Captcha-Point`）。

## 7. 升级后验证

数据库：

```sql
-- 迁移成功
SELECT version, success FROM flyway_schema_history_resource ORDER BY installed_rank DESC LIMIT 3;
-- ownership 列存在
SHOW COLUMNS FROM resource_registry LIKE 'ownership';
-- 唯一键已收敛
SHOW INDEX FROM resource_registry WHERE Key_name LIKE 'uk_%';
-- 旧同步模式清零（应返回 0 行）
SELECT sync_mode, COUNT(*) FROM resource_registry GROUP BY sync_mode;
```

启动与运行：

1. 启动日志出现 `Mango resource registry sync complete`，`mango_bootstrap_control` 推进到 `FINALIZED`。
2. 日志中 `Mango resource ownership unproven` 的条数与第 3 节 D 的预期一致；不是 0 时需要人工确认，不算失败。
3. 后台菜单、按钮权限、字典与升级前一致；被接管的数据没有被覆盖。
4. 站点前台主视觉来自 `/cms/open/advertisements/list`；`/cms/open/banners/list` 返回 404 属预期。
5. 匿名提交在阈值内正常写入；超过 `max-requests` 返回 429 且无采集记录。
6. Admin 菜单树父级高亮正常，切换路由后内容区滚动复位，页签搜索状态保留。

## 8. 回滚边界

| 对象 | 能否回滚 | 做法 |
|---|---|---|
| Maven / npm 版本 | 可以 | 回退到 `1.0.63` 与上一版 npm tuple |
| `V3` 唯一键 | 可回滚但代价高 | 迁移只改索引与唯一键，失败时 MySQL 8 多子句 `ALTER` 原子回退；已成功的 `V3` 不建议手工逆转 |
| `V4` ownership 列 | 可忽略 | 旧版本不读该列，保留无副作用；不要为回滚删列 |
| `cms_banner` 表与数据 | **不可回滚** | 只能依赖第 4.3 节的归档表或数据库备份 |
| 已发布的 `1.1.0` 坐标 | 不可覆盖 | 只能发新版本 |

## 9. Agent 执行清单

按顺序执行，任一步判据不满足就停止并报告，不要跳步。

| # | 动作 | 判据 | 停止条件 |
|---|---|---|---|
| 1 | 读 `mango-docs/capabilities/README.md`、`mango/mango-platform/mango-resource/README.md`、目标模块 README | 拿到当前 `sync-mode`、ownership、迁移口径 | 文档与本文冲突时以当前分支 README 为准，先报告冲突 |
| 2 | 执行第 3 节 A/B grep | 得到待改声明与 Banner 引用清单 | 有 Banner 引用则先做 Banner 指南，不并行 |
| 3 | 执行第 3 节 C 查询 | 无物理重复身份 | 有重复则按 README 14.2 清理后再继续，不要跳过 |
| 4 | 备份数据库与前端锁版本 | 备份可恢复 | 无备份不继续 |
| 5 | 改写旧 `sync-mode`（第 4.2 节） | 声明中不再出现旧值 | 语义不明时按“是否由用户/运行时修改”判断，不确定则保持 `SYNC_ONCE` 并登记待确认 |
| 6 | 升级 Maven 到 `1.1.0`，npm 到本批次闭包 | `pnpm install` 成功，无 peer 冲突 | 出现 `SiteBanner`/`listBanners` 编译错误属预期，按 4.5 改写后再装 |
| 7 | 同步 `mango.config.json` 版本字段 | `mangoBackendVersion=1.1.0` | 字段缺失时先 `mango pmo sync` 补齐 |
| 8 | 启动并观察迁移 | 第 7 节 SQL 全部符合预期，`bootstrap control` 为 `FINALIZED` | `V3` 失败按 README 14.2；`CMS_BANNER` 处理器缺失按 Banner 指南 3.2 |
| 9 | 回归第 7 节运行项 | 6 项全部通过 | 任一项失败保留日志与 receipt，不重复重启试错 |
| 10 | 记录证据 | 迁移输出、ownership 未证明条数、回归结果写入交付记录 | 无证据视为未完成 |

Agent 不要事项：

- 不要用 `mango.resource.registry.fail-on-conflict=false` 绕过注册冲突，`V3` 之后唯一键仍会拦下。
- 不要为消除冲突直接改 `resource_id`。
- 不要把 `UNKNOWN` 归属批量改成 `RESOURCE_MANAGED` 来“让日志安静”。
- 不要手工改 `flyway_schema_history_resource` 的 `success` 位了事，先确认重复身份已清理。
- 不要在未读当前 README 的情况下照抄历史 migration 作为新声明模板。

## 10. 常见问题

**启动报 `缺失资源禁用时未找到处理器: CMS_BANNER`**
Banner 注册行未清理，执行 Banner 指南 3.2。

**`V3` 报 `Duplicate entry ... for key 'uk_resource_registry_type_biz_key'`**
老库存在物理重复身份，按 `mango-resource` README 14.2 清理并删除 `version=3, success=0` 记录后重跑。

**升级后某些旧资源没有被自动删除**
预期行为。存量行是 `UNKNOWN` 归属，缺失停用只处理 `RESOURCE_MANAGED` + `SYNC_ALWAYS`。按第 5 节人工确认。

**日志持续出现 sync-mode 弃用告警**
声明里还有旧值，按第 4.2 节改写。

**前端编译报 `listBanners` 不存在**
`@mango/site-shell` 2.0.0 已删除该导出，按 4.5 改为 `listAdvertisements`。

**匿名提交突然返回 429**
触发了 `max-requests` 默认上限，调整 `window-seconds`/`max-requests`，或确认是否存在真实刷量。

## 11. 关联记录

- 发布批次：`v2026.10.21-maven-1.1.0-resource-sync-ownership-release`
- 主要 PR：#1082、#1084、#1085、#1088、#1089、#1091、#1097、#1098（含 #1101、#1102、#1103、#1104）
- 模块说明：`mango/mango-platform/mango-resource/README.md`、`mango/mango-platform/mango-information/README.md`、`mango/mango-platform/mango-cms/README.md`
- 专项指南：[CMS Banner 能力删除升级](./cms-banner-removal-upgrade.md)
- 发布说明：仓库根目录 `CHANGELOG.md` 的 `v2026.10.21-maven-1.1.0-resource-sync-ownership-release` 章节
