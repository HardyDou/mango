# 内容管理 CMS

## 1. 概览

`mango-cms` 是 Mango 的内容管理能力，用来统一管理站点、栏目、导航、广告投放、内容与内容发布关系，并向官网、帮助中心和门户站点提供只读消费接口。

它对外提供两类能力：

- 站点消费：通过 `/cms/open/**` 公开接口向站点前台提供站点解析、栏目树、导航、广告和已发布内容读取。
- 后台管理：通过 `/cms/**` 管理接口维护站点配置、栏目、导航、广告投放、内容池和发布关系。

业务侧使用时要注意两点：

- `cms` 只负责内容与发布关系管理，不承载站点前台的页面主题与视觉风格，前台由各站点 App 自行实现。
- 公开消费接口只返回已发布、有效期内、可见的数据，不暴露草稿、待审核、驳回或已下线内容。

## 2. 功能清单

| 能力 | 说明 | 使用入口 |
|------|------|----------|
| 站点解析 | 按域名或 siteCode 解析当前站点 | `CmsSiteApi.resolveSite` / `GET /cms/open/sites/resolve` |
| 栏目树 | 查询站点公开栏目树 | `CmsSiteApi.treeCategories` / `GET /cms/open/site-categories/tree` |
| 导航 | 查询站点顶部和底部导航 | `CmsSiteApi.listNavigations` / `GET /cms/open/navigations/list` |
| 广告 | 查询有效广告投放 | `CmsSiteApi.listAdvertisements` / `GET /cms/open/advertisements/list` |
| 内容列表 | 分页查询已发布内容 | `CmsSiteApi.pageContents` / `GET /cms/open/contents/page` |
| 内容详情 | 查询已发布内容详情 | `CmsSiteApi.detailContent` / `GET /cms/open/contents/detail` |
| 公开素材 | 读取站点公开文件素材 | `GET /cms/open/files/public-preview` |
| 站点管理 | 维护站点、站点配置 | 管理接口 / 前端 CMS 管理页面 |
| 内容管理 | 维护内容池、内容分类、标签 | 管理接口 / 前端 CMS 管理页面 |
| 发布记录 | 查看内容发布到哪些站点栏目，维护上下线、置顶和推荐 | 管理接口 / 前端 CMS 管理页面 |

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

`mango-cms-starter-remote` 通过 `CmsRemoteAutoConfiguration` 自动注册以下 Feign 适配器，无需业务应用手工添加 `@EnableFeignClients`：

| 能力 API | FeignClient | HTTP 根路径 |
|----------|-------------|---------------|
| `CmsContentCategoryApi` | `CmsContentCategoryFeignClient` | `/cms` |
| `CmsContentTagApi` | `CmsContentTagFeignClient` | `/cms` |
| `CmsSiteAdminApi` | `CmsSiteAdminFeignClient` | `/cms` |
| `CmsSiteCategoryApi` | `CmsSiteCategoryFeignClient` | `/cms` |
| `CmsContentApi` | `CmsContentFeignClient` | `/cms` |
| `CmsContentPublishApi` | `CmsContentPublishFeignClient` | `/cms` |
| `CmsNavigationApi` | `CmsNavigationFeignClient` | `/cms` |
| `CmsAdvertisementApi` | `CmsAdvertisementFeignClient` | `/cms` |
| `CmsAdDeliveryApi` | `CmsAdDeliveryFeignClient` | `/cms` |
| `CmsSiteSettingApi` | `CmsSiteSettingFeignClient` | `/cms` |
| `CmsSiteApi` | `CmsSiteFeignClient` | `/cms/open` |

`CmsAdminApi` 已删除，不保留聚合兼容门面。原消费者应按职责改为注入上表对应的管理 API；HTTP 路径、请求字段、返回泛型和权限码没有变化。

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

管理后台使用 `@mango/cms` 管理页面包，提供站点、栏目、导航、广告、内容、发布记录页面。

## 5. 快速开始

1. 确认部署应用已启用 `mango-cms-starter`，数据库 migration 已执行。
2. 在 CMS 站点管理页创建站点，填写站点编码、域名、SEO 和版权信息。
3. 创建栏目树，维护栏目类型、访问路径和可见状态。
4. 创建顶部和底部导航，关联栏目或外部链接。
5. 创建内容，编辑富文本正文，设置封面、来源和作者。
6. 将内容发布到站点栏目，设置发布状态、置顶和推荐。
7. 站点前台通过 `@mango/site-shell` 按 `domain` 或 `siteCode` 解析站点并读取已发布内容。

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

公开消费接口前缀为 `/cms/open`，后台管理接口前缀为 `/cms`。接口禁止路径变量，ID 和筛选条件通过 `Query`、`Command` 或 query 参数传递。分页查询统一使用 `XxxPageQuery` 和 `R<PageResult<XxxVO>>`。分页参数统一使用 `page`/`size`；`@mango/cms` 管理包在请求层把 `pageNum`/`pageSize` 映射为 `page`/`size`，后端 `PageQuery` 同时接受 `pageNum`/`pageSize` 别名，两种命名不会静默回落默认分页。

公开消费接口标注 `@ApiAccess(mode = PUBLIC)`，并在 `META-INF/mango/resources/cms-common-public-api.yml` 中按 `API_RESOURCE` 声明 `accessMode=PUBLIC`：站点解析、站点详情、栏目树、导航、广告、内容分页、内容详情共 7 个接口，以及公开素材接口 `GET /cms/open/files/public-preview`。

匿名访问只有这一个判定入口：请求由 `ApiResourceAuthorizationManager` 按 `authorization_api_resource` 匹配，资源未匹配时按 `LOGIN` 处理并返回 401。因此新增 `/cms/open` 公开接口必须同时补声明，且应用必须装配 `mango-authorization-resource-access-starter`（`mango-admin-starter` 已包含）并完成 Resource Registry 同步。CMS 不再向 `mango.auth.security.permit-paths` 注入通配放行路径，`application.yml` 也不需要手工配置 `/cms/open/**`。

公开素材接口仅返回站点 Logo、已发布内容封面/附件和有效广告投放引用的文件。

## 9. 数据与初始化

CMS 按数据用途分层登记：

- Flyway 只负责 DDL。新库通过 `db/migration/mango-cms/V1__init_mango_cms.sql` 一次创建最终表结构，不写菜单、业务数据或演示数据。
- 正式资源放在 `META-INF/mango/resources/`。CMS 当前登记 `cms-common-menu.json` 和 `cms-common-public-api.yml`；此类资源默认同步，其中 `cms-common-public-api.yml` 是匿名放行的唯一判定来源。
- 必须初始化的业务数据也应按资源类型放在 `META-INF/mango/resources/`，不能写进 Flyway。业务项目可以通过 `CMS_CONTENT_CATEGORY` 声明内置内容分类；这些分类属于正式业务数据，不需要开启 Demo。
- 演示资源按 CMS 业务类型拆分放在 `META-INF/mango/demo/`，默认不加载；仅在明确开启 Demo 时同步。

业务项目声明内置内容分类时，资源文件放在业务模块的 `META-INF/mango/resources/`，使用稳定的 `targetId`、业务唯一的 `categoryCode`，并把根分类的 `parentId` 设为 `0`。示例：

```yaml
mango:
  resource:
    schema-version: 1
    module-code: example-content
    module-name: 示例内容
    declarations:
      CMS_CONTENT_CATEGORY:
        - id: "2070000000000000001"
          version: 1
          biz-key: example-content.category.news
          name: 新闻分类
          target-module: cms
          sync-mode: SYNC_ONCE
          fields:
            targetId: { type: LONG, value: 2070000000000000001 }
            tenantId: { type: STRING, value: "1" }
            createdBy: { type: LONG, value: 1 }
            updatedBy: { type: LONG, value: 1 }
            parentId: { type: LONG, value: 0 }
            categoryCode: { type: STRING, value: NEWS }
            categoryName: { type: STRING, value: 新闻 }
            sort: { type: INT, value: 10 }
            status: { type: STRING, value: ENABLED }
            remark: { type: STRING, value: 内置内容分类 }
```

`CMS_CONTENT_CATEGORY` 的目标表是 `cms_content_category`。Resource Registry 只负责声明同步，运行时用户新增或修改的分类仍通过 CMS 管理接口维护。

本地显式开启 Demo：

```yaml
mango:
  resource:
    registry:
      demo-enabled: true
```

也可以使用启动参数 `--mango.resource.registry.demo-enabled=true`。Demo 声明使用 `INIT_ONLY`，只用于空白开发/演示环境，不覆盖已存在的业务数据。

主要表：

| 表 | 用途 |
|----|------|
| `cms_site` | 站点基础信息与 SEO 配置 |
| `cms_site_category` | 站点栏目树 |
| `cms_navigation` | 顶部和底部导航 |
| `cms_advertisement` | 广告位 |
| `cms_ad_delivery` | 广告投放 |
| `cms_content_category` | 内容池通用分类 |
| `cms_content` | 内容池 |
| `cms_content_publish` | 内容发布关系 |
| `cms_site_setting` | 站点扩展配置 |

显式 Demo 共登记 77 条资源，包括 3 个站点及其配置、栏目、内容分类、导航、广告、投放、内容和发布关系。文件 ID 保持为空，因此 Demo 不依赖文件模块的预置记录。内容的 `categoryId` 指向 `cms_content_category`，发布关系的 `categoryId` 指向 `cms_site_category`，两者不能混用。

## 10. 管理入口

CMS 管理页面通过 `@mango/cms` 注册，菜单由 `cms-common-menu.json` 声明。管理接口入口为 `/cms/**`，按 `cms:*` 权限码控制。

## 11. 问题排查

- 站点前台接口返回 401：确认 `mango-cms-starter` 已启用，`cms-common-public-api.yml` 的 `API_RESOURCE` 声明已同步到 `authorization_api_resource`，且应用装配了 `mango-authorization-resource-access-starter`；任一条件缺失时请求按默认 `LOGIN` 处理。
- 站点解析返回"站点不存在或不唯一"：确认请求提供的 `domain` 或 `siteCode` 在当前租户范围内只对应一个启用站点；解析范围由当前租户上下文或部署配置的默认租户限定，只有部署关闭租户拦截时匿名请求才覆盖全部租户。
- 内容列表为空：确认内容状态为 `PUBLISHED`，发布关系 `publish_status` 为 `PUBLISHED`，且在有效期内。
- 公开素材 403：确认文件被站点 Logo、已发布内容封面/附件或有效广告投放引用。

## 12. 升级与清理

Banner 管理能力已下线，公开数据显示统一走广告位 + 广告投放。升级到包含 `V2__drop_cms_banner.sql` 的版本时，按以下顺序执行。

### 12.1 升级前：清理历史 Banner 注册行

历史环境可能仍保留 `CMS_BANNER` 的 `resource_registry` 行。`V2` 会删除 `cms_banner` 表，但不会改动平台注册中心；如果不清掉这些行，启动阶段的 `disableMissing` 会因找不到 `CMS_BANNER` 处理器而失败，并明确报错 `缺失资源禁用时未找到处理器: CMS_BANNER`。

先备份，再按顺序执行（两次 `DELETE` 只删 Banner 相关行，`SELECT` 用于核对范围）：

```sql
-- 0. 备份（按需保留）
CREATE TABLE IF NOT EXISTS resource_registry_bak_cms_banner AS
SELECT * FROM resource_registry WHERE resource_type = 'CMS_BANNER';

-- 1. 核对将被删除的注册行
SELECT id, tenant_id, resource_type, biz_key, status
FROM resource_registry
WHERE resource_type = 'CMS_BANNER';

-- 2. 删除同步日志与变更日志（按 resource_id 关联，避免全表扫描）
DELETE l FROM resource_sync_log l
  JOIN resource_registry r ON r.id = l.resource_id
 WHERE r.resource_type = 'CMS_BANNER';

DELETE c FROM resource_change_log c
  JOIN resource_registry r ON r.id = c.resource_id
 WHERE r.resource_type = 'CMS_BANNER';

-- 3. 删除注册行本身
DELETE FROM resource_registry WHERE resource_type = 'CMS_BANNER';
```

清理只删除 `resource_type = 'CMS_BANNER'` 的数据，不影响其它资源类型、租户或站点数据。

### 12.2 升级时：`cms_banner` 表由迁移删除

`V2__drop_cms_banner.sql` 执行 `DROP TABLE IF EXISTS cms_banner`，表中的历史 Banner 数据不可恢复。需要保留历史配置时，请在第 12.1 步之前自行导出：

```sql
CREATE TABLE cms_banner_archive AS SELECT * FROM cms_banner;
```

### 12.3 升级后：确认

- `cms_banner` 表不存在。
- `resource_registry` 中不再有 `CMS_BANNER` 行。
- 后台「内容运营 → 投放管理」只剩广告位管理和广告投放管理。
- 站点前台主视觉、页头图改由 `GET /cms/open/advertisements/list` 提供。

## 13. 相关文档

- [站点运行时包](../../../mango-ui/packages/site-shell/README.md)
- [演示站点 App](../../../mango-ui/apps/mango-site-demo-app)
- [企业官网站点 App](../../../mango-ui/apps/mango-site-enterprise-app)
- [帮助中心站点 App](../../../mango-ui/apps/mango-site-help-app)
- [Mango 1.0.63 → 1.1.0 升级](../../../mango-docs/guides/operations/mango-1.0.63-to-1.1.0-upgrade.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 变更影响记录

- 后台 66 个管理方法由 `CmsAdminApi` 拆分到 11 个能力 API，每个 API 具有唯一 Controller 和 FeignClient；`CmsSiteApi` 继续作为公开站点只读聚合模型。
- `mango-cms-starter-remote` 不再是空制品，现通过 Spring Boot 自动配置注册 12 个 `mango-cms` FeignClient。
- CMS 演示数据按站点、配置、栏目、导航、广告、投放、内容和发布关系分别登记在
  `META-INF/mango/demo/`；仅在 `demo-enabled=true` 时按 `INIT_ONLY` 同步。
- Banner 管理能力（表 `cms_banner`、资源类型 `CMS_BANNER`、后台 Banner 管理接口与公开 Banner 列表接口）已整体删除，站点主视觉统一走广告位 + 广告投放；升级清理步骤见第 12 节。
- Flyway 继续只负责 DDL，`META-INF/mango/resources/` 只承载默认同步的菜单和未来必须初始化的数据。
- 公开接口、后台管理接口、权限码、租户隔离和内容发布语义不变。

## 资源声明同步模式（#1098）

资源声明的 `sync-mode` 已收敛为 `SYNC_ALWAYS` 和 `SYNC_ONCE`：`AUTO` 映射为 `SYNC_ALWAYS`，`INIT_ONLY`、`MANUAL`、`LOCKED` 映射为 `SYNC_ONCE`。旧值仍可读取并会在启动时输出弃用告警，新声明只写新值。语义与迁移规则见 `mango-resource` README 的 `sync-mode` 章节。
