---
'@mango/cms': patch
'@mango/site-shell': patch
---

修复 Issue #1053 中仍有真实消费方的公开站点缺口。

- `@mango/cms`：发布管理的「发布栏目」下拉按当前站点加载，切换站点立即重载，不再把全部站点栏目拼接。
- `@mango/site-shell`：`SiteBanner` 增加 `mediaUrl`；`SiteContent` 增加 `top` 和 `sort`；`pageContents` 同时接受 `page`/`size` 与 `pageNum`/`pageSize` 并同时返回两套分页字段。
- `mango-common`：`PageQuery` 接受 `pageNum`/`pageSize` 别名，无法识别的分页参数不再静默回落默认分页。

配套 Maven 侧（随同一批次 Maven release 发布）：

- `mango-cms`：公开 Banner 返回可直接访问的媒体地址；公开内容返回置顶标识与排序值。
- `mango-cms`：新增 `CMS_CONTENT_CATEGORY` 演示数据，演示内容的 `categoryId` 改指 `cms_content_category`，与发布关系的 `site_category` 分离。

业务项目升级后需要同步 Maven 新版本并完成 Resource Registry 同步；演示数据仅在显式开启 Demo 时同步。

## 未纳入本次变更

以下两项经核对没有被任何消费方使用，按最小实现原则不加：

- 内容 `code`/`slug` 编码：三个站点 App 仍按 `contentId` 读取详情，没有调用方使用编码。
- Banner 管理菜单与页面：`cms:banner` 菜单已在 2026-07-14 通过 `V4__remove_cms_banner_menu.sql` 主动移除，站点 App 的 E2E 反向断言不请求 `/banners/list`，后台统一使用广告投放。`cms_banner` 表与 `/cms/banners/**` 接口保留为存量能力，不恢复菜单入口。
