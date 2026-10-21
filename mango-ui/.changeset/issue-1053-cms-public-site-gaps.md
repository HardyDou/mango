---
'@mango/cms': patch
'@mango/site-shell': patch
---

修复 Issue #1053 中仍有真实消费方的公开站点缺口，并删除已被取代的 Banner 能力。

修复：

- `@mango/cms`：发布管理的「发布栏目」下拉按当前站点加载，切换站点立即重载，不再把全部站点栏目拼接。
- `@mango/site-shell`：`SiteContent` 增加 `top` 和 `sort`；`pageContents` 同时接受 `page`/`size` 与 `pageNum`/`pageSize` 并同时返回两套分页字段。
- `mango-common`：`PageQuery` 接受 `pageNum`/`pageSize` 别名，无法识别的分页参数不再静默回落默认分页。
- `mango-cms`：公开内容返回置顶标识与排序值；新增 `CMS_CONTENT_CATEGORY` 演示数据，演示内容的 `categoryId` 改指 `cms_content_category`。

删除 Banner 能力（后台菜单已于 2026-07-14 移除，站点 App 的 E2E 反向断言不请求 `/banners/list`）：

- 删除 `cms_banner` 表、`CMS_BANNER` 资源类型与处理器、`CmsBannerApi`/`CmsBannerController`/`CmsBannerFeignClient` 及配套命令、查询、VO、实体、Mapper、Service。
- 删除公开接口 `GET /cms/open/banners/list` 及其 `API_RESOURCE` 声明、`SiteBannerVO`、`SiteBannerQuery`，`@mango/site-shell` 同步移除 `listBanners` 与 `SiteBanner`。
- 演示站点改用 `GET /cms/open/advertisements/list`，并新增 `HOME_HERO`、`NEWS_HERO` 广告位与投放演示数据。
- 新增 `V2__drop_cms_banner.sql` 执行 `DROP TABLE IF EXISTS cms_banner`，`V1` 不再创建该表。

## 升级步骤

1. 备份并清理历史 Banner 注册行，否则启动阶段 `disableMissing` 会因缺少 `CMS_BANNER` 处理器而失败。SQL 见 `mango/mango-platform/mango-cms/README.md` 第 12.1 节，只删除 `resource_type = 'CMS_BANNER'` 的数据。
2. 需要保留历史 Banner 配置时，先执行 `CREATE TABLE cms_banner_archive AS SELECT * FROM cms_banner;`，`V2` 之后 `cms_banner` 不再存在。
3. 升级并启动，确认 `cms_banner` 表已删除、`resource_registry` 中无 `CMS_BANNER` 行，站点前台主视觉改由 `/cms/open/advertisements/list` 提供。
4. 后台「内容运营 → 投放管理」只剩广告位管理与广告投放管理。

## 未纳入本次变更

内容 `code`/`slug` 编码：三个站点 App 仍按 `contentId` 调用 `detailContent`，没有调用方按编码读取，按最小实现原则不加。
