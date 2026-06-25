# Mango CMS Issue 214 设计说明

需求文档：`mango-docs/designs/2026-06-22-mango-cms-requirements.md`

评审记录：`mango-docs/designs/2026-06-22-mango-cms-review.md`

开发计划：`mango-docs/plans/2026-06-22-issue-214-cms-plan.md`

交付台账：`mango-docs/plans/2026-06-22-issue-214-cms-delivery-ledger.md`

## 1. 目标

为 Mango 增加通用内容与站点管理能力，形成内容池、站点、栏目、发布关系、运营配置和前台消费接口的完整一期闭环。

一期交付：

- `mango-platform/mango-cms` 后端平台模块。
- `/cms/**` CMS 后台管理接口。
- `/cms-api/**` 站点消费接口。
- `mango-ui/packages/cms` 管理页面包。
- `mango-ui/packages/site-shell` 前台 CMS 运行时薄壳。
- `mango-ui/apps/mango-site-enterprise-app` 企业官网站点 App。
- `mango-ui/apps/mango-site-help-app` 帮助中心站点 App。
- Resource Registry 菜单、页面和按钮权限资源。
- 数据库 migration、后端测试、前端构建和浏览器 E2E 验证。

## 2. 不做范围

- 页面装修。
- Widget 门户。
- 专题管理。
- 内容推荐算法。
- 全文检索。
- 静态化发布。
- CDN 发布。
- 多语言内容版本。
- 复杂审核流编排。
- 到点自动发布调度。
- 登录访问和指定角色访问的完整前台拦截闭环。
- `site-pages`、`site-theme-*`、`site-shell-*` 或前端 `cms-api` package。

## 3. 架构边界

| 对象 | 位置 | 职责 |
|---|---|---|
| CMS 后端模块 | `mango-platform/mango-cms` | 内容、站点、栏目、发布、导航、Banner、广告、SEO、版权、后台接口和消费接口 |
| CMS 管理页面包 | `mango-ui/packages/cms` | 后台页面、后台 API 封装、`admin-pages` 注册、样式入口 |
| 前台运行时薄壳 | `mango-ui/packages/site-shell` | 站点解析、上下文、SEO、状态处理、访问策略接入点、`/cms-api` 封装 |
| 企业官网 App | `mango-ui/apps/mango-site-enterprise-app` | 企业官网页面、路由、主题、Header/Footer、Banner/广告呈现、列表和详情 |
| 帮助中心 App | `mango-ui/apps/mango-site-help-app` | 帮助中心页面、路由、主题、文档列表、文档详情、FAQ |

依赖规则：

- `mango-cms-api` 不依赖 `core`、`starter`、Controller、Entity、Mapper 或数据库对象。
- `mango-cms-core` 可依赖 `mango-cms-api`、持久化、权限和文件基础能力。
- `mango-cms-starter` 装配 Controller、菜单资源和自动配置。
- `packages/cms` 不依赖 `site-shell` 或站点 App。
- `site-shell` 不依赖管理端、`packages/cms` 或站点 App。
- 站点 App 只依赖 `site-shell` 和通用前端基础包，不依赖 CMS 管理页面。

## 4. 后端模块

```text
mango/mango-platform/mango-cms
  mango-cms-api
    command
    query
    vo
    enums
    api
  mango-cms-core
    entity
    mapper
    service
    converter
    resources/db/migration/cms
  mango-cms-starter
    controller
    resources/META-INF/mango/module.properties
    resources/META-INF/mango/resources/cms-common-menu.json
  mango-cms-starter-remote
```

## 5. API 设计

后台管理接口前缀为 `/cms`：

| 能力 | 接口 |
|---|---|
| 内容分类 | `/cms/content-categories/page`、`/cms/content-categories/tree`、`/cms/content-categories/detail`、`/cms/content-categories`、`/cms/content-categories/status` |
| 标签 | `/cms/content-tags/page`、`/cms/content-tags/detail`、`/cms/content-tags`、`/cms/content-tags/status` |
| 内容 | `/cms/contents/page`、`/cms/contents/detail`、`/cms/contents`、`/cms/contents/submit`、`/cms/contents/approve`、`/cms/contents/reject`、`/cms/contents/offline` |
| 站点 | `/cms/sites/page`、`/cms/sites/detail`、`/cms/sites`、`/cms/sites/status` |
| 栏目 | `/cms/site-categories/tree`、`/cms/site-categories/detail`、`/cms/site-categories`、`/cms/site-categories/status` |
| 发布 | `/cms/content-publishes/page`、`/cms/content-publishes/publish`、`/cms/content-publishes/offline` |
| 导航 | `/cms/navigations/page`、`/cms/navigations/detail`、`/cms/navigations`、`/cms/navigations/status` |
| Banner | `/cms/banners/page`、`/cms/banners/detail`、`/cms/banners`、`/cms/banners/status` |
| 广告 | `/cms/advertisements/page`、`/cms/advertisements/detail`、`/cms/advertisements`、`/cms/advertisements/status` |
| 站点配置 | `/cms/site-settings/detail`、`/cms/site-settings` |

站点消费接口前缀为 `/cms-api`：

| 能力 | 接口 |
|---|---|
| 解析站点 | `/cms-api/sites/resolve` |
| 站点信息 | `/cms-api/sites/detail` |
| 栏目树 | `/cms-api/site-categories/tree` |
| 导航 | `/cms-api/navigations/list` |
| Banner | `/cms-api/banners/list` |
| 广告 | `/cms-api/advertisements/list` |
| 内容列表 | `/cms-api/contents/page` |
| 内容详情 | `/cms-api/contents/detail` |

API 规则：

- 禁止新增路径变量。
- `GET` 查询使用 `XxxQuery` 或 `XxxPageQuery`。
- 写操作使用 `XxxCommand` JSON body。
- 分页返回 `R<PageResult<XxxVO>>`。
- 详情返回 `R<XxxVO>`。
- 创建返回 `R<Long>`。
- 状态变更和删除返回 `R<Boolean>`。
- Controller 实现 API 接口，只依赖 Service。
- Mapper 禁止接收 API 协议模型。

## 6. 数据模型

| 表 | 用途 | 关键约束 |
|---|---|---|
| `cms_site` | 站点基础信息 | 租户内站点编码唯一，域名唯一 |
| `cms_site_category` | 站点栏目树 | 租户和站点内栏目编码唯一 |
| `cms_content` | 内容池 | 租户隔离，内容状态独立 |
| `cms_content_category` | 内容分类 | 租户内分类编码唯一 |
| `cms_content_tag` | 标签 | 租户内标签编码唯一 |
| `cms_content_tag_rel` | 内容标签关联 | 内容和标签唯一 |
| `cms_content_publish` | 内容发布关系 | 内容、站点、栏目有效发布关系唯一 |
| `cms_navigation` | 站点导航 | 站点内类型和名称排序 |
| `cms_banner` | 站点 Banner | 站点内展示位置排序 |
| `cms_advertisement` | 广告位 | 站点内广告位编码唯一 |
| `cms_site_setting` | SEO、版权和联系方式 | 站点唯一 |

字段规则：

- 表必须包含租户、审计、逻辑删除和必要的数据权限字段。
- `logo_file_id`、`cover_file_id`、`attachment_file_id`、`video_file_id`、`media_file_id`、`material_file_id` 只保存文件 ID 或 token。
- 不使用数据库外键。
- 不跨库 join。
- 关联完整性由 Service 校验。

## 7. 状态模型

内容状态：

- `DRAFT`
- `PENDING_REVIEW`
- `REJECTED`
- `PUBLISHED`
- `OFFLINE`

发布状态：

- `PENDING`
- `PUBLISHED`
- `SCHEDULED`
- `OFFLINE`

规则：

- 审核通过只改变内容状态，不自动创建发布关系。
- 发布操作显式创建或更新发布关系。
- 内容下线后，所有 `/cms-api` 内容读取都不可见。
- 发布关系下线只影响该站点栏目位置，不改变内容池状态。
- `/cms-api` 只返回已启用站点、可见栏目、有效导航/Banner/广告、已发布内容和有效发布关系。
- 定时发布时间未到或下线时间已过的数据不返回。

## 8. 租户与数据权限

- 后台接口不接收客户端 `tenantId` 作为 CRUD 隔离条件。
- 租户隔离由 Mango 安全上下文和持久化能力处理。
- 数据权限字段按现有规范保留，列表、详情和写操作都必须覆盖。
- 写操作先读目标对象，再校验租户、数据权限、状态和关联关系。
- `/cms-api` 站点解析不得信任客户端租户字段，按域名、siteCode 和服务端配置解析站点。

## 9. 菜单与权限

CMS 菜单由 `mango-cms-starter` 通过 Resource Registry 注入：

- `META-INF/mango/module.properties`
- `META-INF/mango/resources/cms-common-menu.json`

菜单入口：

```text
平台能力
  内容运营
    内容管理
      内容中心
      内容分类
      标签管理
      发布管理
    站点管理
      站点列表
      栏目管理
      导航管理
      站点配置
    投放管理
      Banner 管理
      广告位管理
```

规则：

- 资源类型使用 `AUTH_MENU`。
- 禁止用 Flyway SQL 初始化菜单、按钮权限、菜单运行时配置、套餐授权和默认角色授权。
- 菜单 `component` 与 `@mango/cms/admin-pages` 页面 key 一致。
- 前端按钮权限和后端接口权限码保持一致。

## 10. 前端设计

`@mango/cms` 页面 key：

| 页面 | key |
|---|---|
| 内容中心 | `cms/contents/index` |
| 内容分类 | `cms/content-categories/index` |
| 标签管理 | `cms/content-tags/index` |
| 站点管理 | `cms/sites/index` |
| 栏目管理 | `cms/site-categories/index` |
| 发布管理 | `cms/content-publishes/index` |
| 导航管理 | `cms/navigations/index` |
| Banner 管理 | `cms/banners/index` |
| 广告位管理 | `cms/advertisements/index` |
| 站点配置 | `cms/site-settings/index` |

`site-shell` 提供：

- `resolveSite`
- `createCmsApiClient`
- `SiteProvider`
- `useSiteContext`
- `useCmsSeo`
- `createSiteAccessStrategy`
- 站点状态组件和错误状态模型

两个站点 App：

- 企业官网 App：首页、新闻列表、新闻详情、关于页面、通用内容详情。
- 帮助中心 App：首页、文档栏目、文档列表、文档详情、FAQ。

## 11. 部署形态

| 场景 | 方式 | 验证 |
|---|---|---|
| 后台单体 | Admin 聚合 `@mango/cms` | 菜单打开 CMS 页面，样式生效 |
| 后台微前端 | admin-shell 加载 CMS 页面 | 页面 key、权限和样式与单体一致 |
| 企业官网 | 企业官网 App 集成 `site-shell` | 展示站点、导航、Banner、内容列表和详情 |
| 帮助中心 | 帮助中心 App 集成 `site-shell` | 展示栏目、文档列表、文档详情和 FAQ |

## 12. 测试策略

- 后端单测覆盖状态流转、唯一约束、删除规则、有效期过滤。
- 后端接口测试覆盖分页、详情、发布、下线、站点解析和公开消费接口。
- 前端构建覆盖 `@mango/cms`、`@mango/site-shell` 和两个站点 App。
- 样式检查覆盖 `pnpm admin:styles:check` 和 `pnpm admin:module-styles:check`。
- 浏览器 E2E 使用仿真数据覆盖后台管理、公开消费、站点停用、栏目隐藏和内容下线。
