# 内容管理 CMS

## 1. 概览

`mango-cms` 是 Mango 的内容管理能力，用来统一管理站点、栏目、导航、Banner、广告、内容与内容发布关系，并向官网、帮助中心和门户站点提供只读消费接口。

它对外提供两类能力：

- 站点消费：通过 `/cms/open/**` 公开接口向站点前台提供站点解析、栏目树、导航、Banner、广告和已发布内容读取。
- 后台管理：通过 `/cms/**` 管理接口维护站点配置、栏目、导航、Banner、广告投放、内容池和发布关系。

业务侧使用时要注意两点：

- `cms` 只负责内容与发布关系管理，不承载站点前台的页面主题与视觉风格，前台由各站点 App 自行实现。
- 公开消费接口只返回已发布、有效期内、可见的数据，不暴露草稿、待审核、驳回或已下线内容。

## 2. 功能清单

| 能力 | 说明 | 使用入口 |
|------|------|----------|
| 站点解析 | 按域名或 siteCode 解析当前站点 | `CmsSiteApi.resolveSite` / `GET /cms/open/sites/resolve` |
| 栏目树 | 查询站点公开栏目树 | `CmsSiteApi.treeCategories` / `GET /cms/open/site-categories/tree` |
| 导航 | 查询站点顶部和底部导航 | `CmsSiteApi.listNavigations` / `GET /cms/open/navigations/list` |
| Banner | 查询有效 Banner | `CmsSiteApi.listBanners` / `GET /cms/open/banners/list` |
| 广告 | 查询有效广告投放 | `CmsSiteApi.listAdvertisements` / `GET /cms/open/advertisements/list` |
| 内容列表 | 分页查询已发布内容 | `CmsSiteApi.pageContents` / `GET /cms/open/contents/page` |
| 内容详情 | 查询已发布内容详情 | `CmsSiteApi.detailContent` / `GET /cms/open/contents/detail` |
| 公开素材 | 读取站点公开文件素材 | `GET /cms/open/files/public-preview` |
| 站点管理 | 维护站点、站点配置 | 管理接口 / 前端 CMS 管理页面 |
| 内容管理 | 维护内容池、内容分类、标签 | 管理接口 / 前端 CMS 管理页面 |
| 发布管理 | 维护内容发布关系、上下线、置顶和推荐 | 管理接口 / 前端 CMS 管理页面 |

## 3. 后端接入

### 3.1 开发依赖

业务模块只需要面向 API 契约编码时，引入 `mango-cms-api`：

```xml
<dependency>
    <groupId>io.mango.platform.cms</groupId>
    <artifactId>mango-cms-api</artifactId>
</dependency>
```

业务代码优先依赖 `CmsSiteApi` 读取公开站点数据：

```java
import io.mango.cms.api.CmsSiteApi;
import io.mango.cms.api.query.SiteResolveQuery;

SiteResolveQuery query = new SiteResolveQuery();
query.setDomain("demo.example.com");
SiteResolveVO site = cmsSiteApi.resolveSite(query).getData();
```

### 3.2 部署依赖

提供内容管理能力的应用启用 starter：

```xml
<dependency>
    <groupId>io.mango.platform.cms</groupId>
    <artifactId>mango-cms-starter</artifactId>
</dependency>
```

微服务中只远程消费站点数据的应用启用 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.cms</groupId>
    <artifactId>mango-cms-starter-remote</artifactId>
</dependency>
```

`mango-cms-starter` 默认随应用启用；需要关闭时配置：

```yaml
mango:
  cms:
    enabled: false
```

## 4. 前端接入

站点前台使用 `@mango/site-shell`，它是站点运行时薄壳，封装 `/cms/open` 公开接口、站点解析、SEO、状态处理和访问策略，供官网、帮助中心和门户站点 App 复用。

```ts
import { cmsSiteApi, createSiteResolveQuery } from '@mango/site-shell';

const site = await cmsSiteApi.resolveSite(createSiteResolveQuery(undefined, window.location.host));
```

管理后台使用 `@mango/cms` 管理页面包，提供站点、栏目、导航、Banner、广告、内容和发布管理页面。

## 5. 快速开始

1. 确认部署应用已启用 `mango-cms-starter`，数据库 migration 已执行。
2. 在 CMS 站点管理页创建站点，填写站点编码、域名、SEO 和版权信息。
3. 创建栏目树，维护栏目类型、访问路径和可见状态。
4. 创建顶部和底部导航，关联栏目或外部链接。
5. 创建内容，编辑富文本正文，设置封面、来源和作者。
6. 将内容发布到站点栏目，设置发布状态、置顶和推荐。
7. 站点前台通过 `@mango/site-shell` 按 `domain` 解析站点并读取已发布内容。

## 6. 配置说明

YAML 配置用于控制 CMS 能力是否启用，以及 Flyway 模块迁移是否执行。

```yaml
mango:
  cms:
    enabled: true
  persistence:
    flyway:
      modules:
        mango-cms:
          enabled: true
```

## 7. 运行时配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.cms.enabled` | `true` | 是否启用本应用内的 CMS starter。 |
| `mango.persistence.flyway.modules.mango-cms.enabled` | `false` | 是否执行 CMS 模块 Flyway 迁移；单体应用需显式开启。 |

## 8. API 与扩展

公开消费接口前缀为 `/cms/open`，后台管理接口前缀为 `/cms`。接口禁止路径变量，ID 和筛选条件通过 `Query`、`Command` 或 query 参数传递。分页查询统一使用 `XxxPageQuery` 和 `R<PageResult<XxxVO>>`。

公开消费接口标注 `@ApiAccess(mode = PUBLIC)`，由 `CmsPermitPathBeanPostProcessor` 自动注入 `/cms/open/**` 到认证放行路径，无需应用手工配置。公开素材接口 `/cms/open/files/public-preview` 仅返回站点 Logo、Banner 媒体、已发布内容封面/附件和有效广告投放引用的文件。

## 9. 数据与初始化

CMS 表通过 Flyway 迁移创建，迁移路径为 `db/migration/mango-cms/V{version}__{description}.sql`，按模块隔离。

主要表：

| 表 | 用途 |
|----|------|
| `cms_site` | 站点基础信息与 SEO 配置 |
| `cms_site_category` | 站点栏目树 |
| `cms_navigation` | 顶部和底部导航 |
| `cms_banner` | 站点 Banner |
| `cms_advertisement` | 广告位 |
| `cms_ad_delivery` | 广告投放 |
| `cms_content` | 内容池 |
| `cms_content_publish` | 内容发布关系 |
| `cms_site_setting` | 站点扩展配置 |

种子数据迁移 `V5__seed_demo_site.sql` 提供演示站点、栏目、导航和 Banner，供本地开发预览。

## 10. 管理入口

CMS 管理页面通过 `@mango/cms` 注册，菜单由 `cms-common-menu.json` 声明。管理接口入口为 `/cms/**`，按 `cms:*` 权限码控制。

## 11. 问题排查

- 站点前台接口返回 401：确认 `mango-cms-starter` 已启用，`CmsPermitPathBeanPostProcessor` 已将 `/cms/open/**` 注入放行路径。
- 站点解析返回"站点不存在或不唯一"：确认请求按 `domain` 解析，且库中对应域名存在唯一启用站点。
- 内容列表为空：确认内容状态为 `PUBLISHED`，发布关系 `publish_status` 为 `PUBLISHED`，且在有效期内。
- 公开素材 403：确认文件被站点 Logo、Banner 媒体、已发布内容或有效广告投放引用。

## 12. 相关文档

- [站点运行时包](../../../mango-ui/packages/site-shell/README.md)
- [演示站点 App](../../../mango-ui/apps/mango-site-demo-app)
- [企业官网站点 App](../../../mango-ui/apps/mango-site-enterprise-app)
- [帮助中心站点 App](../../../mango-ui/apps/mango-site-help-app)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
