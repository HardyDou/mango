# CMS Banner 能力删除升级指南

**适用对象：所有升级到包含 `V2__drop_cms_banner.sql` 的 Maven 版本的 Mango 业务项目。** 不限于某个项目，也不限于已经接入 CMS 的项目。新版本会在完整成功的 Resource `FINALIZE` 快照中自动收敛缺失资源；本文仍保留升级前归档和升级后验证步骤。

变更来源：Mango Issue #1053 / PR #1088。Banner 能力已由「广告位 + 广告投放」取代，`cms_banner` 表与相关接口整体删除，**表数据删除不可恢复**。

## 1. 变更内容

| 对象 | 变更 | 对业务项目的影响 |
|---|---|---|
| `cms_banner` 表 | `V2__drop_cms_banner.sql` 执行 `DROP TABLE IF EXISTS cms_banner` | 表中历史数据不可恢复 |
| `CMS_BANNER` 资源类型与处理器 | 从框架删除 | 完整 `FINALIZE` 快照会保留 Registry 墓碑并收敛存量行；旧版本未完成 FINALIZE 时按第 3.2 节处理 |
| 后台接口 `/cms/banners/**` 与 Banner 菜单 | 删除 | 后台不再有 Banner 维护入口 |
| 公开接口 `GET /cms/open/banners/list` | 删除 | 站点主视觉统一改用 `GET /cms/open/advertisements/list` |
| `@mango/site-shell` | 移除 `listBanners`、`SiteBanner`、`CmsBanner` | 前端包升级后相关调用编译失败，需改为 `listAdvertisements`、`SiteAdvertisement` |

保留不变：`sites/resolve`、`sites/detail`、`site-categories/tree`、`navigations/list`、`advertisements/list`、`contents/page`、`contents/detail`、`files/public-preview`。

## 2. 先判断你的项目属于哪种情况

在目标库执行以下只读查询（不修改数据）：

```sql
-- A. 是否存在需要手工清理的 Banner 注册行
SELECT COUNT(*) AS banner_registry_rows
FROM resource_registry
WHERE resource_type = 'CMS_BANNER';

-- B. 是否存在 Banner 业务数据（需要留档时先归档）
SELECT COUNT(*) AS banner_table_rows FROM cms_banner;

-- C. 是否存在原 Banner 管理接口的权限资源（框架会自动收敛，仅用于知情）
SELECT COUNT(*) AS banner_api_rows
FROM authorization_api_resource
WHERE resource_code LIKE 'cms:banner%';
```

| 情况 | 判定 | 需要执行的动作 |
|---|---|---|
| 未启用 `mango-cms` | `mango-cms-starter` 不在依赖中 | A=0、B 查询报「表不存在」：直接升级，无需其它动作（仍建议执行第 5 节验证） |
| 启用 CMS，从未使用 Banner | A=0、B=0 | 直接升级；第 3 节可跳过 |
| 启用 CMS，曾有 Banner 数据 | B>0 | 执行第 3.1 归档，再按 A 判断是否清理 |
| 启用 CMS，A>0 | A>0 | 优先直接升级并完成一次完整 `FINALIZE`；若无法完成，按第 3.2 节执行兜底迁移 |
| 站点或后台调用过 Banner 接口 | 见第 4 节代码自检 | 执行第 3 节 + 第 6 节前端/后台适配 |

## 3. 升级前需要执行的动作

### 3.1 归档 Banner 业务数据（可选，B>0 时建议）

```sql
CREATE TABLE IF NOT EXISTS cms_banner_archive AS SELECT * FROM cms_banner;
```

`V2` 执行后 `cms_banner` 不再存在，未归档的数据无法恢复。

### 3.2 旧版本兜底清理 Banner 注册行（无法完成 FINALIZE 时执行）

正常路径不需要手工删除：Resource Registry 只在完整且成功的 `FINALIZE` 快照中回收缺失的 `SYNC_ALWAYS` 资源；历史 `UNKNOWN` 行仅在来源范围一致时接管。回收会删除可定位的目标行，并保留 `resource_registry.status = 'REMOVED'` 墓碑，重复执行幂等。

如果旧版本无法生成完整 `FINALIZE`，或升级前必须先删除已移除资源类型，则按以下顺序执行（可重复执行，无副作用）：

```sql
-- 备份被删除的注册行（可选，建议保留）
CREATE TABLE IF NOT EXISTS resource_registry_bak_cms_banner AS
SELECT * FROM resource_registry WHERE resource_type = 'CMS_BANNER';

-- 删除同步日志与变更日志（按 resource_id 关联）
DELETE l FROM resource_sync_log l
  JOIN resource_registry r ON r.id = l.resource_id
 WHERE r.resource_type = 'CMS_BANNER';

DELETE c FROM resource_change_log c
  JOIN resource_registry r ON r.id = c.resource_id
 WHERE r.resource_type = 'CMS_BANNER';

-- 仅在确认目标表和接口已不存在、且无法完成 FINALIZE 时删除 Registry 行
DELETE FROM resource_registry WHERE resource_type = 'CMS_BANNER';
```

所有语句都带 `resource_type = 'CMS_BANNER'` 条件，不会影响其它资源类型、租户或业务数据。

### 3.3 权限与菜单残留（无需手工处理）

- 原 Banner 管理接口的 `API_RESOURCE` 声明随控制器删除而消失。完整 `FINALIZE` 会调用通用 Handler 删除 `authorization_api_resource` 目标行，并将 Registry 保留为 `REMOVED`，无需手工处理。
- `cms:banner:*` 菜单已由历史迁移 `V4__remove_cms_banner_menu.sql` 移除；若某环境仍有残留行，不影响启动和鉴权，可在业务空闲时按需清理。

## 4. 代码自检（所有项目都建议执行）

在项目仓库根目录执行，确认没有对已删除接口的引用：

```bash
grep -rnE "cms_banner|/cms/banners|banners/list|listBanners|SiteBanner|CmsBanner" \
  --include=*.java --include=*.ts --include=*.vue --include=*.json --include=*.sql \
  --exclude-dir=node_modules --exclude-dir=target --exclude-dir=dist .
```

- 无命中：项目没有代码依赖，只按第 2、3 节处理数据即可。
- 有命中：逐条改为广告位 + 广告投放，参见第 6 节。

## 5. 升级后验证（所有项目）

```sql
-- 迁移已执行且成功
SELECT version, description, success FROM flyway_schema_history_resource
ORDER BY installed_rank DESC LIMIT 3;

-- Banner 表已删除（应返回空结果）
SHOW TABLES LIKE 'cms_banner';

-- Registry 仅保留墓碑，不应仍有 ACTIVE 行
SELECT status, COUNT(*) FROM resource_registry
WHERE resource_type = 'CMS_BANNER'
GROUP BY status;

-- 原 API 权限目标行应已物理删除
SELECT COUNT(*) FROM authorization_api_resource
WHERE path_pattern = '/cms/open/banners/list';
```

应用侧确认：

1. 启动日志出现 `Mango resource registry sync complete`，`bootstrap control` 最终为 `FINALIZED`；若发生回收失败，不应写入新的 `FINALIZED` 回执，下一次启动会重试。
2. 未启用 CMS 的项目：以上 SQL 中 `cms_banner` 相关项本就不存在，只需确认启动与发布正常。
3. 启用 CMS 的项目：后台「内容运营 → 投放管理」只有「广告位管理」和「广告投放管理」。
4. 站点前台主视觉来自 `GET /cms/open/advertisements/list` 且返回 200；不再请求 `/cms/open/banners/list`（该接口现在返回 404 属预期）。

## 6. 前端与后台适配（仅命中第 4 节代码的项目）

`@mango/site-shell` 站点：

```diff
- const banners = await cmsSiteApi.listBanners({ ...query, position: 'HOME_HERO' });
- banners[0]?.mediaFileId
+ const heroes = await cmsSiteApi.listAdvertisements({ ...query, position: 'HOME_HERO' });
+ heroes[0]?.imageUrl
```

- 广告位在后台「广告位管理」按 `HOME_HERO`、`PAGE_BANNER` 等位置创建，`positionType` 选 `BANNER`。
- 广告投放接口直接返回 `imageUrl`、`coverUrl` 等可访问地址，前端不需要自行拼接 `public-preview`。
- 后台若曾使用 `@mango/cms` 的 Banner 页面，升级后该页面注册项消失；Banner 维护统一迁移到广告位 + 广告投放。

## 7. 回滚边界

- 代码与 Maven 版本可回退到删除前的版本。
- `cms_banner` 表与数据不可回滚，只能依靠 3.1 的归档表或数据库备份。
- 已清理的 `resource_registry` 行会由回退版本重新同步生成，`target_id` 重新分配。

## 8. 常见问题

**升级后仍出现 CMS_BANNER 相关资源同步失败**
说明本次没有完成完整 `FINALIZE`，或数据库中存在无法证明来源的历史行。先检查 Bootstrap 回执和 `resource_registry` 的 `ownership/sync_mode/status`，确认无法完成 FINALIZE 时再执行第 3.2 节兜底迁移。

**`V2__drop_cms_banner.sql` 报 `Table doesn't exist`**
迁移使用 `DROP TABLE IF EXISTS`，不会报错；若看到其它 DDL 错误，说明连到的是不同 schema，核对数据源。

**站点主视觉空白**
广告位未创建或未投放。在后台「广告位管理」创建对应 `position` 的广告位，再在「广告投放管理」投放素材。

**项目从未使用 CMS，也要执行这些吗**
不需要清理数据，但仍建议执行第 2 节查询与第 5 节启动验证，确认环境里没有历史 CMS 残留。

## 9. 关联记录

- 缺陷与处置记录：Mango Issue #1053
- 实现：Mango PR #1088
- 模块说明与清理 SQL 原文：`mango/mango-platform/mango-cms/README.md` 第 12 节
