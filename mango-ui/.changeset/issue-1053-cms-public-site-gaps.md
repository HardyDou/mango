---
'@mango/cms': patch
'@mango/site-shell': patch
---

修复 Issue #1053 登记的 7 个 CMS 公开站点交付缺口，公开站点不再需要业务侧手工绕行。

- `@mango/cms`：新增「Banner 管理」页面，站点选项与媒体上传完整；发布管理的「发布栏目」下拉按当前站点加载，切换站点立即重载；内容编辑和详情新增「内容编码」字段。
- `@mango/cms`：`pageBanners`、`createBanner`、`updateBanner`、`updateBannerStatus`、`deleteBanner` 补齐到管理 API 客户端。
- `@mango/site-shell`：`SiteBanner` 增加 `mediaUrl`，`SiteContent` 增加 `code`、`top`、`sort`；`pageContents` 同时接受 `page`/`size` 与 `pageNum`/`pageSize` 并在响应中同时返回两套分页字段；`detailContent` 支持按内容编码读取。

配套 Maven 侧（随同一批次 Maven release 发布）：

- `mango-cms`：公开 Banner 返回可直接访问的媒体地址；公开内容返回内容编码与置顶/排序；`/cms/open/contents/detail` 支持 `code` 查询。
- `mango-cms`：`V2__add_content_code.sql` 为 `cms_content` 增加 `code` 列和租户内唯一键，内容编码由运营填写并按租户去重。
- `mango-cms`：新增 `CMS_CONTENT_CATEGORY` 演示数据，演示内容的 `categoryId` 改指向 `cms_content_category`，与发布关系的 `site_category` 分离。
- `mango-common`：`PageQuery` 同时接受 `pageNum`/`pageSize` 别名，无法识别的分页参数不再静默回落默认分页。

业务项目升级后需要：同步 Maven 新版本、执行 CMS Flyway 迁移 `V2`（新增 `cms_content.code`）、完成 Resource Registry 同步（`AUTH_MENU` 新增 Banner 菜单与 `cms:banner:*` 权限）。内容编码为可空列，存量内容不受影响；需要 SEO 友好地址时再逐条补编码。
