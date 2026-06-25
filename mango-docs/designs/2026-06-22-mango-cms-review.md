# Mango CMS Issue 214 需求与方案评审记录

关联 Issue：`https://github.com/HardyDou/mango/issues/214`

需求文档：`mango-docs/designs/2026-06-22-mango-cms-requirements.md`

评审日期：2026-06-22

## 1. 评审结论

Issue 214 可以进入开发，但一期必须收敛为“CMS 管理端基座 + 公开站点消费最小闭环”。本轮交付保留 `mango-cms` 后端模块、`@mango/cms` 管理页面包、`@mango/site-shell` 前台运行时薄壳、2 个站点 App 示例和 `/cms-api` 公开消费接口；页面装修、专题管理、全文检索、推荐算法、静态化、CDN、多语言和复杂审核流不进入一期。

关键边界通过：

- `@mango/cms` 是 CMS 管理页面包，不是后台 Shell。
- `@mango/cms/admin-pages` 是管理页面注册入口。
- `/cms-api/**` 是后端站点消费接口前缀，不新增同名前端 package。
- `@mango/site-shell` 只做前台运行时、站点解析、站点上下文、SEO、状态处理、访问策略接入点和 `/cms-api` 封装。
- 站点 App 是具体网站，拥有自己的页面、主题、Header/Footer、Banner 呈现、广告呈现、列表页和详情页。

## 2. 产品策略评审

### 2.1 通过项

- 内容池与发布关系解耦正确，能支撑同一内容多站点、多栏目复用。
- 站点运营对象完整覆盖站点、栏目、导航、Banner、广告、SEO 和版权配置。
- 管理端与前台消费端分开建模，避免官网前台能力污染后台壳层。
- 一期明确裁剪页面装修、专题管理和全文检索，避免范围失控。

### 2.2 修订项

| 风险 | 修订决策 |
|---|---|
| 一期同时做完整门户产品化，范围过大 | 交付口径收敛为管理端基座、公开站点消费和 2 个示例站点 App |
| 定时发布口径不清 | 一期只保存计划发布时间，并在 `/cms-api` 读取时按有效期过滤，不引入自动调度 |
| 前台登录访问和角色访问容易扩大范围 | 一期保存配置并提供 `site-shell` 扩展点，E2E 只验公开访问闭环 |
| Issue 原文包含专题管理 | 专题管理进入二期，不进入本轮验收 |
| 内容类型过多 | 文章、单页、附件闭环；图文、视频保留字段和枚举，不做复杂展示验收 |

## 3. 工程架构评审

### 3.1 后端模块结构

```text
mango/mango-platform/mango-cms
  mango-cms-api
  mango-cms-core
  mango-cms-starter
  mango-cms-starter-remote
```

后台管理接口使用 `/cms/**`，站点消费接口使用 `/cms-api/**`。接口禁止路径变量，ID 和筛选条件通过 `Query`、`Command` 或 query 参数传递。分页查询统一使用 `XxxPageQuery` 和 `R<PageResult<XxxVO>>`。

### 3.2 数据模型

一期保留 11 张核心表：

- `cms_site`
- `cms_site_category`
- `cms_content`
- `cms_content_category`
- `cms_content_tag`
- `cms_content_tag_rel`
- `cms_content_publish`
- `cms_navigation`
- `cms_banner`
- `cms_advertisement`
- `cms_site_setting`

所有业务表按 Mango 持久化规范保留租户、审计、逻辑删除和必要的数据权限字段。文件字段只保存文件 ID 或 token，不保存 URL。

### 3.3 前端结构

```text
mango-ui/packages/cms
mango-ui/packages/site-shell
mango-ui/apps/mango-site-enterprise-app
mango-ui/apps/mango-site-help-app
```

`packages/cms` 不依赖 `site-shell`；站点 App 不依赖 `packages/cms` 或后台宿主；`site-shell` 不依赖管理端。

## 4. 安全与权限评审

- CMS 菜单和按钮权限必须通过 Resource Registry 的 `AUTH_MENU` 声明，禁止通过 Flyway SQL 初始化菜单和授权。
- 菜单 `component` 必须与 `@mango/cms/admin-pages` 页面 key 一致。
- 后台接口必须服务端鉴权，不能只依赖前端按钮隐藏。
- 列表、详情和写操作都必须校验租户与数据权限；写操作先读取目标对象，再校验状态和权限。
- `/cms-api` 不返回草稿、待审核、驳回、已下线、站点停用、栏目隐藏、未到发布时间或超过下线时间的数据。

## 5. UX 与前端评审

- 管理端页面遵守 Mango Admin 列表页、表单页、弹窗、状态和权限展示规范。
- 所有核心管理页需要 loading、empty、error、表单校验、危险操作二次确认和无权限状态。
- CMS 页面只实现一套，通过 `@mango/cms/admin-pages` 适配单体 Admin 和 admin-shell 微前端。
- 站点 App 必须是实际网站，不是空运行入口；Banner、广告、列表、详情等视觉呈现由站点 App 自己负责。

## 6. QA 验收建议

核心 E2E 必须覆盖：

- 后台创建站点、栏目、分类、标签、内容。
- 内容提交审核、驳回、审核通过、发布、下线。
- 同一内容发布到多个栏目后，前台两个位置读取同一内容。
- 站点停用、栏目隐藏、内容下线、未到发布时间、已过下线时间时 `/cms-api` 不返回。
- 管理端单体和 admin-shell 微前端均能打开 CMS 页面。
- 企业官网 App 和帮助中心 App 均通过 `site-shell` 读取真实 `/cms-api` 数据。

## 7. 最终决策

评审通过，按以下顺序开发：

1. 设计说明、开发计划、交付台账。
2. 后端 `mango-cms` 模块、数据库、实体、服务、管理接口和消费接口。
3. Resource Registry 菜单和权限资源。
4. `@mango/cms` 管理页面。
5. `@mango/site-shell` 前台运行时薄壳。
6. 2 个站点 App。
7. 单体 Admin、admin-shell、站点 App 的浏览器 E2E 验证。
