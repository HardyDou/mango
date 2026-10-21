---
'@mango/cms': patch
'@mango/site-shell': patch
---

修复 Issue #1053 中仍有真实消费方的公开站点缺口，并删除已被广告投放取代的 Banner 能力。

## 修复

- `@mango/cms`：发布管理的「发布栏目」下拉按当前站点加载，切换站点立即重载，不再把全部站点栏目拼接。
- `@mango/site-shell`：`SiteContent` 增加 `top` 和 `sort`；`pageContents` 同时接受 `page`/`size` 与 `pageNum`/`pageSize` 并同时返回两套分页字段。
- `mango-common`：`PageQuery` 接受 `pageNum`/`pageSize` 别名，无法识别的分页参数不再静默回落默认分页。
- `mango-cms`：公开内容返回置顶标识与排序值；新增 `CMS_CONTENT_CATEGORY` 演示数据，演示内容的 `categoryId` 改指 `cms_content_category`。

## 删除 Banner 能力

后台菜单已于 2026-07-14 移除，站点 App 的 E2E 反向断言不请求 `/banners/list`，这些能力没有消费方：

| 对象 | 处置 |
|---|---|
| `cms_banner` 表 | `V2__drop_cms_banner.sql` 执行 `DROP TABLE IF EXISTS cms_banner`；`V1` 不再创建 |
| `CMS_BANNER` 资源类型与处理器 | 删除 |
| 后台接口 `/cms/banners/**`、`CmsBannerApi`/Controller/FeignClient/Service/Entity/Mapper/VO | 删除 |
| 公开接口 `GET /cms/open/banners/list` 及其 `API_RESOURCE` 声明 | 删除 |
| `@mango/site-shell` 的 `listBanners`、`SiteBanner`、`CmsBanner` | 删除 |

## 升级说明（适用于所有升级到本版本的项目）

本次包含**不可恢复的数据删除**：`cms_banner` 表会被 DROP，历史 Banner 数据不保留。下面按项目情况给出各自必须执行的动作，升级负责人必须在启动新版本前逐项确认。

### 步骤 0：判断项目属于哪种情况

```sql
SELECT COUNT(*) AS banner_registry_rows FROM resource_registry WHERE resource_type = 'CMS_BANNER';
SELECT COUNT(*) AS banner_table_rows FROM cms_banner;
```

| 项目情况 | 动作 |
|---|---|
| 未启用 `mango-cms` | 无需清理与适配，仅执行步骤 4 验证启动正常 |
| 启用 CMS，两个计数均为 0 | 直接升级，跳过步骤 2 |
| 启用 CMS，`cms_banner` 有数据 | 先执行步骤 2.2 归档，再按 `banner_registry_rows` 判断是否执行 2.3 |
| 启用 CMS，`banner_registry_rows > 0` | 必须执行步骤 2.3，否则启动失败 |
| 项目代码引用过 Banner 接口 | 额外执行步骤 5 的前端/后台适配 |

### 步骤 1：代码自检

```bash
grep -rnE "cms_banner|/cms/banners|banners/list|listBanners|SiteBanner|CmsBanner" \
  --include=*.java --include=*.ts --include=*.vue --include=*.json --include=*.sql \
  --exclude-dir=node_modules --exclude-dir=target --exclude-dir=dist .
```

无命中表示项目没有代码依赖，只需按步骤 0 处理数据；有命中时按步骤 5 适配。

### 步骤 2：升级前数据处置

`banner_registry_rows > 0` 时，不执行本步骤会导致启动时 Bootstrap 资源同步失败，报 `缺失资源禁用时未找到处理器: CMS_BANNER`，`bootstrap control` 停在 `FAILED`。两个计数均为 0 时可跳过本节。

```sql
-- 2.1 备份注册行（可选，建议保留）
CREATE TABLE IF NOT EXISTS resource_registry_bak_cms_banner AS
SELECT * FROM resource_registry WHERE resource_type = 'CMS_BANNER';

-- 2.2 归档 Banner 业务数据（可选，删表后无法恢复）
CREATE TABLE IF NOT EXISTS cms_banner_archive AS SELECT * FROM cms_banner;

-- 2.3 删除同步日志与变更日志（按 resource_id 关联）
DELETE l FROM resource_sync_log l
  JOIN resource_registry r ON r.id = l.resource_id
 WHERE r.resource_type = 'CMS_BANNER';

DELETE c FROM resource_change_log c
  JOIN resource_registry r ON r.id = c.resource_id
 WHERE r.resource_type = 'CMS_BANNER';

-- 2.4 删除注册行
DELETE FROM resource_registry WHERE resource_type = 'CMS_BANNER';
```

这些语句只作用于 `resource_type = 'CMS_BANNER'`，不影响其它资源类型、租户或站点数据；可重复执行，重复执行无副作用。

### 步骤 3：升级并启动

升级 Maven 版本并启动应用，`V2__drop_cms_banner.sql` 会自动删表。

### 步骤 4：升级后验证（所有项目都要执行）

```sql
-- 迁移已执行且成功（启用 CMS 的项目应有 V2 且 success=1）
SELECT version, description, success FROM flyway_schema_history_resource
ORDER BY installed_rank DESC LIMIT 3;

SHOW TABLES LIKE 'cms_banner';                                              -- 必须为空
SELECT COUNT(*) FROM resource_registry WHERE resource_type = 'CMS_BANNER';  -- 必须为 0
```

应用侧确认：

1. 所有项目：启动日志出现 `Mango resource registry sync complete`，`bootstrap control` 最终为 `FINALIZED`。
2. 未启用 CMS 的项目：以上两条查询命中 0 或表不存在，只需确认启动与本次发布正常。
3. 启用 CMS 的项目：后台「内容运营 → 投放管理」只有「广告位管理」和「广告投放管理」。
4. 启用站点前台的项目：主视觉来自 `GET /cms/open/advertisements/list` 且返回 200，不再请求 `/cms/open/banners/list`（该接口返回 404 属预期）。

### 步骤 5：前端适配（仅使用 site-shell 的站点）

```diff
- const banners = await cmsSiteApi.listBanners({ ...query, position: 'HOME_HERO' });
- banners[0]?.mediaFileId
+ const heroes = await cmsSiteApi.listAdvertisements({ ...query, position: 'HOME_HERO' });
+ heroes[0]?.imageUrl
```

广告位需在后台「广告位管理」按 `HOME_HERO`、`PAGE_BANNER` 等位置创建，`positionType` 选 `BANNER`；投放接口直接返回 `imageUrl`/`coverUrl`，前端不需要自己拼接 `public-preview`。

只使用 `/cms/open` 公开接口、从不调用 `banners/list` 的项目无需改前端代码，但仍需执行步骤 1–4。

### 回滚边界

- 代码与 Maven 版本可回退到删除前的版本。
- `cms_banner` 表和数据不可回滚，只能依靠步骤 2.2 的归档表或数据库备份。
- 已清理的 `resource_registry` 行由回退版本重新同步生成，`target_id` 会重新分配。

完整说明（含按项目情况的判断表、后台权限残留说明、常见问题）见 `mango-docs/guides/operations/cms-banner-removal-upgrade.md`。

## 未纳入本次变更

内容 `code`/`slug` 编码：三个站点 App 仍按 `contentId` 调用 `detailContent`，没有调用方按编码读取，按最小实现原则不加。
