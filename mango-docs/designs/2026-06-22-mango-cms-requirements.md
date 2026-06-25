# Mango CMS 内容与站点管理平台需求说明

## 1. 目标

为 Mango 增加通用内容与站点管理能力，支持平台在同一套后台内管理内容池、多站点、多栏目和内容发布关系。

本需求解决的问题：

- 内容目前缺少统一生产、审核、发布和下线能力。
- 多站点、多门户场景下，内容容易被重复维护，修改后难以同步。
- 站点栏目、导航、Banner、广告位、SEO 和版权配置缺少统一运营入口。
- 后续门户建设、帮助中心、知识库和专题站需要稳定的 CMS 基座。

一期目标：

- 建立 `mango-cms` 后端平台模块。
- 建立 `mango-ui/packages/cms` 前端包，承载 CMS 管理页面能力，适配 Mango Admin 单体和 Mango Admin Shell 微前端两种管理端部署形态。
- 建立 `cms-api` 站点消费接口能力，提供给官网、帮助中心、知识库等前台站点读取站点、栏目、导航、Banner、广告和内容；前端消费封装归属于 `site-shell`，不单独拆前端 package。
- 建立 `site-shell` 前台 CMS 运行时薄壳，提供站点消费侧的站点识别、站点上下文、SEO、状态处理、访问策略和 `/cms-api` 封装。
- 交付 2 个站点 App 作为 `site-shell` 的消费示例；站点 App 自带具体页面、主题和风格，验证多站点共享同一套 CMS 内容和站点接口。
- 建立内容池与发布关系模型，避免内容直接从属于栏目。
- 提供 CMS 一期后台管理页面、前台站点读取接口、真实数据库表、菜单和权限资源。

## 2. 产品定位

Mango CMS 是 Mango 平台的通用内容与站点管理能力，不绑定特定行业。

适用场景：

- 企业官网
- 用户门户
- 合作伙伴门户
- 帮助中心
- 知识库
- 新闻门户
- 活动专题站
- 产品文档中心

核心用户：

- 内容编辑：创建、编辑、提交审核和维护内容。
- 内容审核员：审核、驳回、发布或下线内容。
- 站点运营人员：维护站点、栏目、导航、Banner、广告位、SEO 和版权配置。
- 平台管理员：配置菜单、权限、角色授权和模块启用状态。
- 站点访客：通过官网类型站点访问已发布的栏目、导航、内容、Banner 和广告。
- 站点前端开发者：基于 `site-shell` 和 `/cms-api` 站点消费接口创建不同站点 App；每个站点 App 自带具体页面、主题和风格。

## 3. 核心原则

### 3.1 内容与站点解耦

内容维护在内容池中，站点栏目只负责展示入口。

目标模型：

```text
内容池
  |
发布管理
  |
站点栏目
```

一篇内容可以发布到多个站点和多个栏目。

### 3.2 内容唯一来源

同一篇内容只维护一次。内容修改后，所有发布位置读取同一份内容数据。

### 3.3 发布关系独立

内容不属于栏目。内容与站点、栏目之间通过发布关系关联。

### 3.4 站点独立运营

每个站点可以独立维护：

- 站点名称、编码、Logo、描述、域名和状态。
- 栏目树。
- 导航。
- Banner。
- 广告位。
- SEO 配置。
- 版权信息。

## 4. 一期范围

一期交付 CMS 基座、管理端基础能力和官网类型站点消费能力。

### 4.1 内容中心

支持管理以下内容类型：

- 文章
- 图文
- 单页
- 附件
- 视频

内容字段至少包括：

- 标题
- 副标题
- 摘要
- 内容类型
- 封面文件 ID
- 正文
- 外部 URL
- 附件文件 ID
- 视频文件 ID
- 来源
- 作者
- 内容分类
- 标签
- SEO 标题
- SEO 关键词
- SEO 描述
- 状态
- 发布时间
- 下线时间
- 创建人、创建时间、更新人、更新时间

内容操作：

- 新增
- 编辑
- 删除
- 提交审核
- 审核通过
- 审核驳回
- 发布
- 下线
- 查询详情
- 分页查询

### 4.2 内容分类

内容分类用于内容池归档，不代表站点栏目。

支持能力：

- 分类新增、编辑、删除。
- 分类启用、停用。
- 分类树或列表查询。
- 内容按分类筛选。

典型分类：

- 新闻
- 公告
- 帮助文档
- 产品介绍

### 4.3 标签管理

支持能力：

- 标签新增、编辑、删除。
- 标签启用、停用。
- 内容多标签关联。
- 内容按标签筛选。

### 4.4 站点管理

支持维护站点基础信息：

- 站点名称
- 站点编码
- Logo
- 描述
- 域名
- 状态
- 默认语言
- SEO 标题
- SEO 关键词
- SEO 描述
- 页脚版权
- 备案信息
- 联系方式

站点操作：

- 新增站点。
- 编辑站点。
- 启用、停用站点。
- 删除未使用站点。
- 查询站点详情。
- 分页查询站点。

### 4.5 栏目管理

栏目归属于站点，用于控制站点内展示结构。

栏目支持树形结构，至少支持以下栏目类型：

- 列表栏目
- 单页栏目
- 外链栏目

栏目字段至少包括：

- 所属站点
- 父栏目
- 栏目名称
- 栏目编码
- 栏目类型
- 访问路径
- 外链 URL
- 排序
- 可见状态
- 访问权限类型
- SEO 标题
- SEO 关键词
- SEO 描述

访问权限类型：

- 公开访问
- 登录访问
- 指定角色访问

一期只要求保存栏目访问权限配置和后台管理展示；前台访问拦截链路由门户消费侧在后续门户交付中验证。

### 4.6 发布管理

发布关系连接内容、站点和栏目。

发布关系字段至少包括：

- 内容 ID
- 站点 ID
- 栏目 ID
- 发布状态
- 发布时间
- 定时发布时间
- 下线时间
- 是否置顶
- 置顶范围
- 是否推荐
- 推荐类型
- 展示排序

发布操作：

- 单篇内容发布到一个或多个站点栏目。
- 批量发布多篇内容。
- 单篇内容从一个或多个发布位置下线。
- 批量下线。
- 定时发布。
- 取消定时发布。
- 栏目置顶。
- 站点置顶。
- 首页推荐。
- 热门推荐。
- 编辑推荐。

### 4.7 导航管理

导航归属于站点。

支持导航类型：

- 顶部导航
- 底部导航
- 快捷导航

导航字段至少包括：

- 所属站点
- 导航类型
- 名称
- 跳转类型
- 栏目 ID
- 内容 ID
- 外部 URL
- 打开方式
- 排序
- 状态

### 4.8 Banner 管理

Banner 归属于站点。

支持媒体类型：

- 图片
- 视频

Banner 字段至少包括：

- 所属站点
- 展示位置
- 标题
- 副标题
- 媒体类型
- 媒体文件 ID
- 跳转 URL
- 开始时间
- 结束时间
- 排序
- 状态

### 4.9 广告位管理

广告位归属于站点。

支持广告位置：

- 首页广告
- 列表广告
- 详情广告

广告字段至少包括：

- 所属站点
- 广告位编码
- 广告位名称
- 广告类型
- 素材文件 ID
- 跳转 URL
- 开始时间
- 结束时间
- 排序
- 状态

### 4.10 SEO 和版权配置

SEO 配置可以在站点、栏目和内容三个层级维护。

支持字段：

- Title
- Keywords
- Description

版权配置归属于站点。

支持字段：

- 页脚版权
- 备案信息
- 联系方式

### 4.11 前端壳层与包边界

CMS 一期涉及管理端和官网前台两类运行场景，二者需要分开建模。

边界定义：

| 对象 | 建议位置 | 职责 | 不负责 |
|---|---|---|---|
| 管理后台壳 | `@mango/admin-shell` | 后台登录、菜单、权限、主题、布局、TagsView、单体和微前端运行时 | CMS 业务页面实现、官网前台展示 |
| 页面注册表 | `@mango/admin-pages` | 管理端 `moduleCode + component` 到页面 loader 的注册表 | 业务页面、管理后台布局、官网前台展示 |
| CMS 管理页面包 | `mango-ui/packages/cms` / `@mango/cms` | CMS 后台页面、后台 API 封装、`@mango/cms/admin-pages` 注册入口、CMS 管理端样式 | 后台壳层、官网前台壳、站点主题 |
| `site-shell` 前台运行时包 | `mango-ui/packages/site-shell` / `@mango/site-shell` | 前台站点识别、站点上下文、SEO 处理、状态处理、访问策略、`/cms-api` 消费封装和运行时 Provider | 后台登录、后台菜单、后台按钮权限、CMS 管理页面、具体站点页面、视觉风格、Banner 呈现和广告位呈现 |
| 站点 App | `mango-ui/apps/mango-site-enterprise-app`、`mango-ui/apps/mango-site-help-app` | 具体官网、帮助中心等站点的页面、主题、风格、布局组合、路由定义、Banner 呈现和广告位呈现 | 站点识别、CMS 消费接口底层封装、CMS 管理页面、后台接口调用 |
| 站点消费接口 | 后端 `/cms-api/**` | 给 `site-shell` 和站点 App 读取公开站点数据 | 前端 package、后台管理写接口 |

`site-shell` 需要独立存在，但只作为薄运行时包。原因是官网、帮助中心和门户都需要按域名或 siteCode 解析站点、封装 `/cms-api`、处理 SEO、站点停用、内容下线、无权限和接口错误；这些运行逻辑不适合放入 `admin-shell`，也不应由每个站点 App 重复实现。

`site-shell` 只建设一套前台 CMS 运行时薄壳。企业官网、帮助中心、知识库、专题站等差异由站点 App 内部的页面、主题和风格表达，不为每一种站点类型复制一套 Shell。

一期不新增 `site-pages`、`site-theme-*`、`site-shell-*` 或 `cms-api` 前端 package。若后续多个站点 App 出现稳定复用的展示组件，再评估抽取 `site-components`；若多个站点 App 出现稳定复用的视觉体系，再评估抽取 `site-theme`。

部署形态要求：

| 场景 | 部署方式 | 要求 |
|---|---|---|
| CMS 管理端单体 | `mango-admin` 聚合 `@mango/cms` 页面和样式 | 后端菜单、页面 key、权限点与微前端模式一致 |
| CMS 管理端微前端 | `mango-admin-shell` 按 runtime config 本地或远程加载 CMS 管理页面 | CMS 子应用显式引入自身依赖样式，不依赖宿主样式穿透 |
| 官网前台单体 | 一个站点 App 集成 `site-shell` 后独立构建 | 适合单官网、单帮助中心等简单部署；页面和风格在站点 App 内闭环 |
| 官网前台多站点 | 多个站点 App 共用 `site-shell` 和 `/cms-api` | 按域名、siteCode 或部署配置解析站点；不同风格由不同站点 App 表达 |

### 4.12 管理端部署适配

CMS 管理页面只维护一套 `mango-ui/packages/cms` 能力。

`mango-ui/packages/cms` 需要适配：

- Mango Admin 单体：作为 `mango-admin` 的官方业务模块注册，随单体管理端聚合构建。
- Mango Admin Shell 微前端：作为可独立运行的管理端子应用或模块页面，接入 `mango-admin-shell` 的布局、登录态、权限、菜单和页面注册机制。

管理端页面要求：

- 页面能力放在业务 package，不在宿主 app 中复制第二份实现。
- 管理页面通过 `@mango/cms/admin-pages` 注册入口暴露页面 key。
- package 样式跟随 `packages/cms` 自身发布，并提供公开样式入口。
- 单体模式通过管理端样式聚合链引入 `packages/cms` 样式。
- 微前端模式由 CMS 管理子应用显式引入自身依赖样式，不依赖宿主样式穿透隔离容器。
- 菜单 `component` 与 `packages/cms` 页面 key 保持一致。
- 页面遵守 Mango Admin UI 列表、表单、弹窗、状态和权限展示规范。

### 4.13 `site-shell` 前台 CMS 运行时

新增 `site-shell` 前台 CMS 运行时薄壳，用于为面向访客的站点 App 提供公共运行能力。

`site-shell` 职责：

- 根据域名、站点编码或部署配置识别当前站点。
- 封装站点配置、SEO、版权、栏目、导航、Banner、广告位、内容列表和内容详情的 `/cms-api` 读取能力。
- 提供站点上下文 Provider，例如 siteCode、siteId、栏目树、站点状态和访问策略。
- 提供 SEO 处理能力，例如站点、栏目和内容维度的 title、keywords、description。
- 提供统一状态处理能力，例如加载中、站点不存在、站点停用、栏目不存在、内容不存在、内容已下线、无权限和接口错误。
- 提供公开访问、登录访问和指定角色访问的前台访问策略接入点。
- 为站点 App 提供路由包装和运行时上下文，不设计独立 `site-pages` 注册表。
- 统一接入 `/cms-api` 站点消费接口，避免每个站点 App 直接拼接接口细节或重复处理数据有效性。

`site-shell` 不负责：

- 后台内容编辑。
- 后台权限菜单。
- 页面装修编辑器。
- 静态化发布。
- CDN 发布。
- 后台管理端的单体或微前端 runtime config。
- 企业官网、帮助中心、知识库等具体页面。
- 具体站点的主题、风格和视觉组件组合。
- Header、Footer、Banner、广告位、列表页和详情页的最终视觉呈现。
- 为不同官网风格拆分 `site-shell-a`、`site-shell-b`。

### 4.14 站点 App

一期交付 2 个站点 App，验证同一 `site-shell` 薄运行时支撑多个站点。站点 App 不是空运行入口，而是一套可独立构建部署的具体网站。

建议站点 App：

- 企业官网 App：面向企业官网场景，包含首页、新闻列表、新闻详情、关于页面和通用内容详情。
- 帮助中心 App：面向帮助中心场景，包含首页、文档栏目、文档列表、文档详情和常见问题。

站点 App 职责：

- 提供站点主题、视觉风格、页面组合和内容展示结构。
- 提供自身路由定义，例如首页、列表页、详情页、单页栏目页和 FAQ 页。
- 使用 `site-shell` 提供的站点上下文。
- 使用 `/cms-api` 读取已发布内容，不直接访问 CMS 后台管理接口。
- 支持单体构建和独立站点 App 构建。
- 支持同一套站点 App 根据 siteCode 或域名渲染不同站点数据。

站点 App 不得：

- 复制 `packages/cms` 管理页面。
- 复制 `site-shell` 的站点识别、`/cms-api` 封装和状态处理能力。
- 直接依赖管理端宿主 app。
- 使用静态假数据作为最终展示数据。
- 把主题或风格拆成独立 package 作为一期交付前置条件。

## 5. 不做范围

以下内容不进入一期交付：

- 页面装修。
- Widget 门户。
- 专题管理。
- 内容推荐算法。
- 全文检索。
- 静态化发布。
- CDN 发布。
- 多语言内容版本。
- 外部搜索引擎 sitemap 自动提交。
- 富文本编辑器深度定制。
- 内容版本对比和回滚。
- 复杂审核流编排。
- 超出 2 个示例站点 App 的更多行业站点模板。
- 站点 App 在线拖拽装修和低代码页面搭建。

## 6. 业务场景

### 6.1 多站点复用

内容编辑创建《平台升级公告》。

运营人员将同一篇内容发布至：

- 企业官网 / 新闻动态
- 用户门户 / 系统公告
- 帮助中心 / 最新动态
- 合作伙伴门户 / 通知公告

后续编辑该内容正文后，所有发布位置展示同一份更新后的内容。

### 6.2 帮助中心

站点为“帮助中心”。

栏目包括：

- 快速开始
- 使用指南
- 常见问题
- 版本更新

内容编辑统一维护帮助文档，通过发布关系将文档放入不同栏目。

### 6.3 知识库

站点为“知识库”。

栏目包括：

- 产品文档
- 技术文档
- 开发指南
- API 文档

内容分类用于归档知识类型，站点栏目用于展示结构。

### 6.4 活动专题站

站点为“活动专题”。

栏目包括：

- 活动介绍
- 活动规则
- 活动资讯
- 获奖名单

一期仅支持通过站点、栏目、内容和发布关系维护专题站基础内容；专题页面装修属于后续范围。

## 7. 影响模块

### 7.1 后端

建议新增：

- `mango/mango-platform/mango-cms`
- `mango/mango-platform/mango-cms/mango-cms-api`
- `mango/mango-platform/mango-cms/mango-cms-core`
- `mango/mango-platform/mango-cms/mango-cms-starter`
- `mango/mango-platform/mango-cms/mango-cms-starter-remote`

建议接入：

- `mango-resource`：菜单、权限、业务域和模块资源声明。
- `mango-authorization`：后台页面和按钮权限。
- `mango-infra-web`：Controller、统一响应和访问控制。
- `mango-infra-persistence`：数据库、审计字段、租户自动隔离、分页和 Flyway。
- `mango-file`：媒体、附件、Logo、封面图和素材文件标识。

后端能力划分：

- CMS 后台管理接口：面向登录后台用户，提供内容生产、审核、站点配置、发布关系和运营配置管理。
- `cms-api` 站点消费接口：面向 `site-shell` 和站点 App，提供只读站点数据、导航、栏目、Banner、广告和已发布内容。
- `cms-api` 不暴露草稿、待审核、驳回、已下线内容。
- `cms-api` 不接收客户端传入租户 ID；站点归属由域名、siteCode 或服务端配置解析。
- `cms-api` 需要明确公开访问、登录访问和指定角色访问栏目策略。

### 7.2 前端

建议新增：

- `mango-ui/packages/cms`
- `mango-ui/packages/site-shell`
- `mango-ui/apps/mango-site-enterprise-app`
- `mango-ui/apps/mango-site-help-app`

`mango-ui/packages/cms` 提供：

- `admin-pages` 注册入口。
- CMS 后台管理 API 封装。
- 内容中心页面。
- 内容分类页面。
- 标签管理页面。
- 站点管理页面。
- 栏目管理页面。
- 发布管理页面。
- 导航管理页面。
- Banner 管理页面。
- 广告位管理页面。
- 站点 SEO 和版权配置页面。

`site-shell` 提供：

- 站点识别和站点上下文。
- `/cms-api` 站点消费接口类型和 API 封装。
- 站点信息、栏目树、导航、Banner、广告、内容列表和内容详情的只读数据模型。
- SEO 处理能力。
- 站点状态、内容状态、接口错误、空态和访问限制的统一处理能力。
- 公开访问、登录访问和指定角色访问的前台访问策略接入点。
- 站点 App 路由包装和运行时 Provider。

两个站点 App 提供：

- 企业官网 App：使用 `site-shell` 和 `/cms-api` 渲染企业官网站点，包含自己的首页、列表页、详情页、Banner 呈现、广告位呈现、主题和风格。
- 帮助中心 App：使用 `site-shell` 和 `/cms-api` 渲染帮助中心站点，包含自己的首页、文档栏目页、文档列表、文档详情、FAQ、主题和风格。

前端依赖要求：

- `apps/*` 可以依赖 `packages/*`。
- `packages/cms` 和 `site-shell` 不得依赖任何 `apps/*`。
- 站点 App 不得依赖管理端宿主和 `packages/cms` 页面。
- `packages/cms` 不得依赖 `site-shell` 或站点 App。
- 样式必须跟随所属 package，通过公开样式入口暴露。
- 单体和微前端模式都必须验证样式生效。

### 7.3 菜单

建议菜单入口：

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

菜单资源要求：

- CMS 菜单、按钮权限和页面路由由 `mango-cms-starter` 自身声明。
- 菜单和按钮权限通过 Resource Registry 的 `AUTH_MENU` 资源注入。
- 禁止通过 Flyway SQL 初始化 CMS 菜单、按钮权限、菜单运行时配置、套餐授权和默认角色授权。
- `mango-cms-starter` 需要提供 `META-INF/mango/module.properties` 和 `META-INF/mango/resources/cms-common-menu.json`。
- 菜单 `component` 必须匹配 `@mango/cms/admin-pages` 注册的页面 key。
- 菜单 `path` 必须匹配前端真实路由。

## 8. 接口变化

一期需要提供后台管理接口。接口路径建议以 `/cms` 为模块前缀。

接口约束：

- 新增接口不得使用路径变量。对象标识、站点标识和父级标识统一通过 `Query` 或 `Command` 字段传递。
- `GET` 查询使用 `XxxQuery` 或 `XxxPageQuery`，复杂查询参数在 OpenAPI 中展开为 query 参数。
- `POST`、`PUT` 和复杂业务动作使用 `Command` JSON body。
- `DELETE` 的简单标识使用 query 参数；批量删除使用 `Command` JSON body。
- 分页接口返回 `R<PageResult<XxxVO>>`。
- 单对象详情返回 `R<XxxVO>`。
- 写操作无返回数据时返回 `R<Void>`；创建后需要回显标识时返回 `R<Long>`。
- 业务文件字段只接收文件中心标识，例如 `fileId`、`fileIds`、`coverFileId`、`logoFileId`、`mediaFileId`、`attachmentFileIds` 或 `mango-file:{id}` token，不接收需要持久化的访问 URL。
- API、Controller 和 Service 的核心方法保持同一入参模型；Mapper 入参不得使用 API 协议模型。
- 所有 `Command`、`Query` 和简单参数必须具备 Bean Validation 约束。
- 所有对外接口必须声明中文 OpenAPI 文档、中文参数含义和访问边界说明。

### 8.1 内容接口

- `GET /cms/contents/page`：内容分页查询。
- `GET /cms/contents/detail`：内容详情，使用 `ContentDetailQuery.id`。
- `POST /cms/contents`：新增内容。
- `PUT /cms/contents`：编辑内容，使用 `UpdateContentCommand.id`。
- `DELETE /cms/contents`：删除内容，使用 query 参数 `id`。
- `POST /cms/contents/submit`：提交审核，使用 `SubmitContentCommand.id`。
- `POST /cms/contents/approve`：审核通过，使用 `ApproveContentCommand.id`。
- `POST /cms/contents/reject`：审核驳回，使用 `RejectContentCommand.id` 和驳回原因。
- `POST /cms/contents/publish`：发布内容，使用 `PublishContentCommand`。
- `POST /cms/contents/offline`：下线内容，使用 `OfflineContentCommand`。

### 8.2 内容分类接口

- `GET /cms/content-categories/tree`：分类树查询。
- `POST /cms/content-categories`：新增分类。
- `PUT /cms/content-categories`：编辑分类，使用 `UpdateContentCategoryCommand.id`。
- `DELETE /cms/content-categories`：删除分类，使用 query 参数 `id`。
- `PUT /cms/content-categories/status`：调整状态，使用 `UpdateContentCategoryStatusCommand`。

### 8.3 标签接口

- `GET /cms/tags/page`：标签分页查询。
- `GET /cms/tags/options`：标签选项查询。
- `POST /cms/tags`：新增标签。
- `PUT /cms/tags`：编辑标签，使用 `UpdateContentTagCommand.id`。
- `DELETE /cms/tags`：删除标签，使用 query 参数 `id`。
- `PUT /cms/tags/status`：调整状态，使用 `UpdateContentTagStatusCommand`。

### 8.4 站点接口

- `GET /cms/sites/page`：站点分页查询。
- `GET /cms/sites/options`：站点选项查询。
- `GET /cms/sites/detail`：站点详情，使用 `SiteDetailQuery.id`。
- `POST /cms/sites`：新增站点。
- `PUT /cms/sites`：编辑站点，使用 `UpdateSiteCommand.id`。
- `DELETE /cms/sites`：删除站点，使用 query 参数 `id`。
- `PUT /cms/sites/status`：调整状态，使用 `UpdateSiteStatusCommand`。

### 8.5 栏目接口

- `GET /cms/site-categories/tree`：站点栏目树查询，使用 `SiteCategoryTreeQuery.siteId`。
- `GET /cms/site-categories/detail`：栏目详情，使用 `SiteCategoryDetailQuery.id`。
- `POST /cms/categories`：新增栏目。
- `PUT /cms/categories`：编辑栏目，使用 `UpdateSiteCategoryCommand.id`。
- `DELETE /cms/categories`：删除栏目，使用 query 参数 `id`。
- `PUT /cms/categories/status`：调整状态，使用 `UpdateSiteCategoryStatusCommand`。

### 8.6 发布接口

- `GET /cms/publishes/page`：发布关系分页查询。
- `POST /cms/publishes`：发布到站点栏目。
- `POST /cms/publishes/batch`：批量发布。
- `POST /cms/publishes/offline`：下线发布关系，使用 `OfflineContentPublishCommand.id`。
- `POST /cms/publishes/batch-offline`：批量下线。
- `PUT /cms/publishes/pin`：置顶调整，使用 `UpdateContentPublishPinCommand`。
- `PUT /cms/publishes/recommend`：推荐调整，使用 `UpdateContentPublishRecommendCommand`。
- `DELETE /cms/publishes`：删除未发布或已下线关系，使用 query 参数 `id`。

### 8.7 导航接口

- `GET /cms/navigations/page`：导航分页查询。
- `POST /cms/navigations`：新增导航。
- `PUT /cms/navigations`：编辑导航，使用 `UpdateNavigationCommand.id`。
- `DELETE /cms/navigations`：删除导航，使用 query 参数 `id`。
- `PUT /cms/navigations/status`：调整状态，使用 `UpdateNavigationStatusCommand`。

### 8.8 Banner 接口

- `GET /cms/banners/page`：Banner 分页查询。
- `POST /cms/banners`：新增 Banner。
- `PUT /cms/banners`：编辑 Banner，使用 `UpdateBannerCommand.id`。
- `DELETE /cms/banners`：删除 Banner，使用 query 参数 `id`。
- `PUT /cms/banners/status`：调整状态，使用 `UpdateBannerStatusCommand`。

### 8.9 广告位接口

- `GET /cms/advertisements/page`：广告分页查询。
- `POST /cms/advertisements`：新增广告。
- `PUT /cms/advertisements`：编辑广告，使用 `UpdateAdvertisementCommand.id`。
- `DELETE /cms/advertisements`：删除广告，使用 query 参数 `id`。
- `PUT /cms/advertisements/status`：调整状态，使用 `UpdateAdvertisementStatusCommand`。

### 8.10 站点配置接口

- `GET /cms/site-settings/detail`：读取站点 SEO 和版权配置，使用 `SiteSettingQuery.siteId`。
- `PUT /cms/site-settings`：保存站点 SEO 和版权配置，使用 `SaveSiteSettingCommand.siteId`。

### 8.11 站点消费接口

站点消费接口用于 `site-shell` 和站点 App。接口路径建议以 `/cms-api` 为前缀，与后台管理 `/cms` 接口隔离。

消费接口约束：

- 只返回已启用站点、可见栏目、有效导航、有效 Banner、有效广告和已发布内容。
- 不返回后台审核字段、内部操作状态、权限配置明细和未发布内容。
- 不使用路径变量。
- 站点识别使用 `SiteResolveQuery`，支持域名、siteCode 或部署配置传入。
- 内容列表使用 `SiteContentPageQuery`，返回 `PageResult<SiteContentSummaryVO>`。
- 内容详情使用 `SiteContentDetailQuery`，返回 `SiteContentDetailVO`。
- 栏目访问权限为公开访问时可匿名读取；登录访问和指定角色访问需要按站点访问策略校验。

接口清单：

- `GET /cms-api/sites/resolve`：解析当前站点。
- `GET /cms-api/sites/detail`：读取站点公开信息。
- `GET /cms-api/sites/settings`：读取站点公开 SEO、版权和联系方式。
- `GET /cms-api/categories/tree`：读取站点公开栏目树。
- `GET /cms-api/categories/detail`：读取栏目公开详情。
- `GET /cms-api/navigations/list`：读取站点导航。
- `GET /cms-api/banners/list`：读取有效 Banner。
- `GET /cms-api/advertisements/list`：读取有效广告位。
- `GET /cms-api/contents/page`：读取已发布内容分页。
- `GET /cms-api/contents/detail`：读取已发布内容详情。
- `GET /cms-api/contents/recommendations`：读取人工推荐内容。

## 9. 数据变化

一期建议新增以下数据表：

| 表 | 说明 |
|---|---|
| `cms_site` | 站点基础信息、域名、状态、SEO 和版权摘要。 |
| `cms_site_category` | 站点栏目树。 |
| `cms_content` | 内容池主表。 |
| `cms_content_category` | 内容分类。 |
| `cms_content_tag` | 内容标签。 |
| `cms_content_tag_rel` | 内容与标签关联。 |
| `cms_content_publish` | 内容、站点、栏目发布关系。 |
| `cms_navigation` | 站点导航。 |
| `cms_banner` | 站点 Banner。 |
| `cms_advertisement` | 站点广告位。 |
| `cms_site_setting` | 站点 SEO、版权和扩展配置。 |

通用字段：

- `id`
- `tenant_id`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- `deleted`

数据层要求：

- CMS 普通业务表默认都是租户业务表，Entity 应继承租户实体基线或包含 `tenant_id`。
- 新增、更新审计字段和租户字段由 persistence 基线自动填充。
- 普通 CRUD、分页查询和自定义 Mapper 查询默认依赖租户插件自动追加 `tenant_id` 条件。
- 业务代码不得把客户端传入的 `tenantId` 作为普通 CRUD 隔离条件。
- 业务代码不得在普通租户表查询中重复手写租户过滤条件；确需跨租户运营或初始化时必须在设计中显式建模。
- CMS 不新增租户过滤例外表；如实现阶段确需例外，必须说明所有权、访问权限和测试口径。
- 自定义 Mapper/XML 查询必须有租户隔离集成测试。
- Mapper 只访问 CMS 本域表；跨域数据通过对应模块 API 获取。
- 业务表不使用数据库外键，关联完整性由业务服务校验。
- migration 路径使用 `db/migration/cms/V{version}__{description}.sql`。
- 所有 DDL 变更必须通过 Flyway migration。

关键唯一性约束：

- 站点编码在同一租户内唯一。
- 同一站点内栏目编码唯一。
- 内容分类编码在同一租户内唯一。
- 标签编码在同一租户内唯一。
- 同一内容、站点、栏目只能存在一条有效发布关系。
- 同一站点内广告位编码唯一。

文件字段持久化要求：

- `cms_site.logo_file_id` 保存 Logo 文件标识。
- `cms_content.cover_file_id`、`attachment_file_ids`、`video_file_id` 保存内容相关文件标识。
- `cms_banner.media_file_id` 保存 Banner 图片或视频文件标识。
- `cms_advertisement.material_file_id` 保存广告素材文件标识。
- 业务表不得持久化预签名 URL、预览 URL、下载 URL 或对象存储直连地址。

站点消费数据要求：

- `cms-api` 读取的数据来自 CMS 业务表和发布关系，不维护第二套站点内容表。
- 站点 App 只读 CMS 发布数据，不写入内容池和发布关系。
- 站点解析可以使用站点域名、站点编码或部署配置；解析结果必须落到唯一有效站点。
- 同一内容被发布到多个站点栏目时，`cms-api` 按当前站点和栏目过滤发布关系。
- 已下线、未到定时发布时间、站点停用、栏目隐藏、Banner 过期和广告过期的数据不能出现在消费接口。

分页要求：

- 分页查询对象使用 `XxxPageQuery`。
- 分页响应统一为 `PageResult<XxxVO>`。
- 页面分页切换、筛选和重置必须与接口分页结果保持一致。
- 分页查询必须同时验证租户隔离和数据权限过滤。

## 10. 状态模型

### 10.1 内容状态

内容状态：

- `DRAFT`：草稿。
- `PENDING_REVIEW`：待审核。
- `REJECTED`：已驳回。
- `PUBLISHED`：已发布。
- `OFFLINE`：已下线。

允许流转：

```text
DRAFT -> PENDING_REVIEW
PENDING_REVIEW -> REJECTED
PENDING_REVIEW -> PUBLISHED
REJECTED -> DRAFT
PUBLISHED -> OFFLINE
OFFLINE -> DRAFT
```

### 10.2 发布状态

发布关系状态：

- `SCHEDULED`：定时发布。
- `PUBLISHED`：已发布。
- `OFFLINE`：已下线。

### 10.3 配置状态

站点、栏目、分类、标签、导航、Banner 和广告使用统一启停状态：

- `ENABLED`
- `DISABLED`

## 11. 权限要求

CMS 菜单和按钮需要接入 Mango 权限。

接口权限要求：

- 受保护接口必须做后端权限校验，不能只依赖前端按钮隐藏。
- 权限码必须与菜单资源、前端 `v-auth` 权限点和后端接口访问策略一致。
- 页面查询权限和数据权限使用同一业务资源码或明确映射关系。
- 权限相关查询必须显式控制范围，禁止越权读取其它租户、其它站点或无权组织数据。

建议权限码：

| 页面 | 权限码示例 |
|---|---|
| 内容中心 | `cms:content:list`、`cms:content:create`、`cms:content:edit`、`cms:content:delete`、`cms:content:review`、`cms:content:publish`、`cms:content:offline` |
| 内容分类 | `cms:category:list`、`cms:category:create`、`cms:category:edit`、`cms:category:delete`、`cms:category:status` |
| 标签管理 | `cms:tag:list`、`cms:tag:create`、`cms:tag:edit`、`cms:tag:delete`、`cms:tag:status` |
| 站点管理 | `cms:site:list`、`cms:site:create`、`cms:site:edit`、`cms:site:delete`、`cms:site:status` |
| 栏目管理 | `cms:site-category:list`、`cms:site-category:create`、`cms:site-category:edit`、`cms:site-category:delete`、`cms:site-category:status` |
| 发布管理 | `cms:publish:list`、`cms:publish:create`、`cms:publish:offline`、`cms:publish:pin`、`cms:publish:recommend` |
| 导航管理 | `cms:navigation:list`、`cms:navigation:create`、`cms:navigation:edit`、`cms:navigation:delete`、`cms:navigation:status` |
| Banner 管理 | `cms:banner:list`、`cms:banner:create`、`cms:banner:edit`、`cms:banner:delete`、`cms:banner:status` |
| 广告位管理 | `cms:advertisement:list`、`cms:advertisement:create`、`cms:advertisement:edit`、`cms:advertisement:delete`、`cms:advertisement:status` |
| 站点配置 | `cms:setting:view`、`cms:setting:edit` |

## 12. 数据权限要求

CMS 一期需要为后台列表和详情查询预留并接入数据权限。

数据权限资源映射：

| 入口 | 数据权限资源码 | 归属字段 |
|---|---|---|
| 内容中心分页、详情、编辑、删除、审核、发布、下线 | `cms:content:list` | `created_by`、`org_id`、`tenant_id` |
| 内容分类树、编辑、删除、启停 | `cms:category:list` | `created_by`、`org_id`、`tenant_id` |
| 标签分页、编辑、删除、启停 | `cms:tag:list` | `created_by`、`org_id`、`tenant_id` |
| 站点分页、详情、编辑、删除、启停 | `cms:site:list` | `created_by`、`org_id`、`tenant_id` |
| 栏目树、详情、编辑、删除、启停 | `cms:site-category:list` | `created_by`、`org_id`、`tenant_id` |
| 发布关系分页、发布、下线、置顶、推荐 | `cms:publish:list` | `created_by`、`org_id`、`tenant_id` |
| 导航分页、编辑、删除、启停 | `cms:navigation:list` | `created_by`、`org_id`、`tenant_id` |
| Banner 分页、编辑、删除、启停 | `cms:banner:list` | `created_by`、`org_id`、`tenant_id` |
| 广告位分页、编辑、删除、启停 | `cms:advertisement:list` | `created_by`、`org_id`、`tenant_id` |
| 站点配置读取和保存 | `cms:setting:view` | `created_by`、`org_id`、`tenant_id` |

数据权限行为：

- `ALL` 只表示当前租户内全部 CMS 数据，不能跨租户。
- 未安装授权数据权限能力时，CMS 仍必须保持租户隔离和接口权限校验。
- 安装授权数据权限能力后，CMS 查询和依赖详情的写操作需要按资源码应用数据权限。
- 写操作必须先读取目标对象并确认当前用户对目标对象有操作权限。
- 发布内容到站点栏目时，需要同时校验内容、站点和栏目的可访问范围。
- CMS 业务表应保留 `org_id` 字段用于组织范围过滤；组织来源在实现设计中明确。

## 13. 页面要求

### 13.1 内容中心页面

页面能力：

- 查询内容列表。
- 按标题、内容类型、状态、分类、标签、发布时间筛选。
- 新增内容。
- 编辑内容。
- 删除草稿或下线内容。
- 提交审核。
- 审核通过。
- 审核驳回并填写原因。
- 发布到站点栏目。
- 下线内容。
- 查看发布位置。

页面状态：

- 加载态。
- 空态。
- 查询错误态。
- 保存错误提示。
- 删除、发布、下线二次确认。

### 13.2 分类和标签页面

页面能力：

- 维护内容分类。
- 维护标签。
- 启停分类和标签。
- 删除前提示影响。

### 13.3 站点管理页面

页面能力：

- 查询站点。
- 新增站点。
- 编辑站点基础信息。
- 启用、停用站点。
- 删除未使用站点。
- 进入站点栏目、导航、Banner、广告位和配置管理。

### 13.4 栏目管理页面

页面能力：

- 按站点查看栏目树。
- 新增一级栏目和子栏目。
- 编辑栏目。
- 调整排序。
- 启用、停用栏目。
- 删除未使用栏目。

### 13.5 发布管理页面

页面能力：

- 查询发布关系列表。
- 按站点、栏目、内容、发布状态筛选。
- 发布内容到栏目。
- 批量发布。
- 批量下线。
- 设置置顶。
- 设置推荐。
- 设置定时发布。

### 13.6 导航、Banner、广告位页面

页面能力：

- 按站点筛选。
- 新增、编辑、删除。
- 启用、停用。
- 配置跳转链接或关联栏目、内容。
- 配置排序和有效期。

### 13.7 站点配置页面

页面能力：

- 维护站点 SEO。
- 维护页脚版权。
- 维护备案信息。
- 维护联系方式。

### 13.8 管理端部署页面要求

CMS 管理页面需要同时满足：

- 在 `mango-admin` 单体管理端中可访问。
- 在 `mango-admin-shell` 微前端管理端中可访问。
- 两种模式使用同一套 `packages/cms` 页面源码、后台管理 API 封装、权限点和样式入口。
- 列表页遵守搜索区、功能区、表格区、分页区结构。
- 表单页按业务判断路径分组，不按接口字段顺序机械排列。
- 删除、下线、驳回、批量下线等危险操作使用 danger 语义和二次确认。
- 页面覆盖 loading、empty、error 和无权限状态。
- 微前端模式下页面样式不能依赖宿主穿透隔离容器。

### 13.9 `site-shell` 运行时要求

`site-shell` 需要提供薄运行时能力：

- 站点加载状态。
- 站点不存在或已停用状态。
- 栏目不存在状态。
- 内容不存在或已下线状态。
- 无权限访问状态。
- SEO title、keywords、description 处理。
- `/cms-api` 调用错误和空数据处理。
- 站点 App 路由包装和运行时 Provider。

`site-shell` 不要求提供企业官网首页、帮助中心首页、列表页、详情页、Header、Footer、Banner 轮播或广告位视觉组件。

### 13.10 站点 App 页面要求

企业官网 App 至少提供：

- 首页。
- 新闻或公告列表。
- 新闻或公告详情。
- 单页栏目展示。
- 通用内容详情。

帮助中心 App 至少提供：

- 首页。
- 文档栏目树。
- 文档列表。
- 文档详情。
- 常见问题列表和详情。

站点 App 页面必须：

- 使用 `site-shell` 提供的站点上下文。
- 使用 `/cms-api` 获取真实站点数据。
- 自行实现 Header、Footer、Banner 呈现、广告位呈现、列表页、详情页和主题风格。
- 支持空栏目、空内容、接口错误和无权限状态。
- 在桌面和移动宽度下无文本重叠、导航挤压或内容遮挡。

## 14. 验收标准

### 14.1 内容池

- 可以新增文章、图文、单页、附件和视频类型内容。
- 内容可以按标题、类型、状态、分类和标签查询。
- 内容详情可以正确回显基础信息、正文、分类、标签和 SEO 字段。
- 内容状态可以按定义流转。
- 非法状态流转会返回明确错误。

### 14.2 分类和标签

- 可以维护内容分类和标签。
- 内容可以关联一个分类和多个标签。
- 停用的分类和标签不能用于新内容选择。
- 已被内容使用的分类和标签删除时有明确限制或提示。

### 14.3 站点和栏目

- 可以新增、编辑、启停站点。
- 同一租户内站点编码不能重复。
- 可以维护站点栏目树。
- 同一站点内栏目编码不能重复。
- 栏目类型为外链时可以维护外链 URL，URL 只作为栏目跳转目标，不作为文件访问地址。
- 栏目类型为单页时可以关联单篇内容或为后续发布关系预留单页展示配置。

### 14.4 发布管理

- 一篇内容可以发布到多个站点栏目。
- 同一内容、站点、栏目不能重复有效发布。
- 修改内容后，已发布位置读取同一内容详情。
- 发布关系可以下线。
- 支持定时发布时间保存和查询。
- 支持置顶和推荐标记保存、查询和取消。

### 14.5 站点运营配置

- 可以维护导航、Banner 和广告位。
- 导航可以关联栏目、内容或外部链接。
- Banner 可以维护图片或视频文件 ID。
- 广告位可以按站点和位置查询。
- 站点 SEO、版权、备案和联系方式可以保存并回显。

### 14.6 菜单和权限

- CMS 菜单资源可以随 starter 注入。
- 管理端可以通过 `@mango/cms/admin-pages` 注册 CMS 页面。
- 页面按钮按权限码控制展示和操作。
- 无权限调用后台接口时返回权限错误。

### 14.7 管理端部署适配

- 同一套 `packages/cms` 页面可以在 `mango-admin` 单体管理端打开。
- 同一套 `packages/cms` 页面可以在 `mango-admin-shell` 微前端管理端打开。
- 单体模式和微前端模式下菜单、权限、页面 key、接口调用和样式表现一致。
- 微前端模式下 CMS 页面关键元素 computed style 正常，不依赖宿主样式穿透。
- `pnpm admin:styles:check` 和 `pnpm admin:module-styles:check` 在涉及官方模块清单或样式聚合时通过。

### 14.8 `site-shell` 和站点 App

- `site-shell` 可以根据域名或 siteCode 解析站点。
- `site-shell` 可以封装 `/cms-api` 并向站点 App 提供站点信息、栏目、导航、Banner、广告、内容列表和内容详情数据。
- `site-shell` 可以处理站点停用、栏目不存在、内容下线、无权限和接口错误状态。
- 企业官网 App 可以通过 `site-shell` 展示企业官网站点数据，并使用自身主题和页面风格呈现首页、列表和详情。
- 帮助中心 App 可以通过 `site-shell` 展示帮助中心站点数据，并使用自身主题和页面风格呈现首页、文档列表、文档详情和 FAQ。
- 两个站点 App 共用 `/cms-api`，不访问后台管理接口。
- 站点 App 只展示已发布、未下线、当前站点可见的数据。
- 站点停用、栏目隐藏、内容下线后，前台页面不再展示对应数据。
- 同一内容发布到两个站点时，两个站点详情页读取同一内容来源。

### 14.9 数据、租户和数据权限

- CMS 数据按租户隔离。
- 查询接口不能返回其它租户数据。
- 新增数据写入当前租户。
- 删除使用逻辑删除。
- 不允许客户端通过传入 `tenantId` 改变普通 CRUD 的租户隔离范围。
- 分页、详情和依赖详情的写操作必须验证数据权限生效。
- 发布内容到站点栏目时，必须同时校验内容、站点和栏目的租户边界与数据权限。

### 14.10 分页

- 内容、标签、站点、发布关系、导航、Banner 和广告位分页接口返回 `PageResult<XxxVO>`。
- 前端分页切换后列表行数、总数、页码和查询条件与接口响应一致。
- 搜索、重置和分页切换不能丢失当前租户和数据权限过滤。

### 14.11 前端交互

- CMS 管理页面使用真实接口。
- 页面具备加载态、空态、错误态和二次确认。
- 表单校验覆盖必填字段、编码格式、外链 URL、跳转 URL、时间范围和状态选择。
- 主要页面在桌面宽度下无文本重叠、按钮挤压或表格操作不可用问题。

## 15. 测试范围

### 15.1 后端测试

需要覆盖：

- 内容 CRUD 和状态流转。
- 内容分类 CRUD。
- 标签 CRUD。
- 站点 CRUD。
- 栏目树 CRUD。
- 发布、批量发布、下线、置顶、推荐和定时发布。
- 导航、Banner、广告 CRUD。
- 站点配置保存和读取。
- 站点消费接口：站点解析、栏目树、导航、Banner、广告、内容分页、内容详情。
- 站点消费接口只返回已发布和有效期内数据。
- 租户隔离。
- 数据权限过滤。
- 分页查询和分页元数据。
- 唯一性约束。
- 非法状态流转。
- 权限注解或访问控制。
- 文件字段只保存文件 ID 或 token，不保存访问 URL。

### 15.2 前端测试

需要覆盖：

- CMS 页面注册。
- 菜单页面可打开。
- 列表查询。
- 表单新增和编辑。
- 状态调整。
- 发布和下线操作。
- 空态、错误态和表单校验。
- 分页切换、搜索和重置。
- 无权限提示和按钮权限展示。
- 单体管理端 CMS 页面主流程。
- admin-shell 微前端 CMS 页面主流程。
- `site-shell` 站点解析、状态处理、`/cms-api` 封装和站点上下文。
- 企业官网 App 首页、列表、详情。
- 帮助中心 App 首页、文档列表、文档详情。

### 15.3 集成验收

建议验收链路：

1. 创建站点“帮助中心”。
2. 创建栏目“快速开始”和“常见问题”。
3. 创建内容分类“帮助文档”。
4. 创建标签“入门”。
5. 创建文章《快速开始》并提交审核。
6. 审核通过并发布到“帮助中心 / 快速开始”。
7. 将同一文章再次发布到另一个栏目。
8. 修改文章摘要后，查看两个发布位置读取同一内容。
9. 下线其中一个发布位置，另一个发布位置保持有效。
10. 配置站点导航、Banner、广告位、SEO 和版权信息。
11. 在 `mango-admin` 单体管理端打开 CMS 内容中心并完成一次查询。
12. 在 `mango-admin-shell` 微前端管理端打开 CMS 内容中心并完成一次查询。
13. 通过集成 `site-shell` 的企业官网 App，看到对应站点导航、Banner 和内容列表。
14. 通过集成 `site-shell` 的帮助中心 App，看到对应站点栏目树和文档详情。
15. 停用一个栏目后，对应站点 App 不再展示该栏目内容。

## 16. 风险与限制

| 风险 | 说明 | 处理方式 |
|---|---|---|
| Issue 范围较大 | CMS 一期包含多个后台对象和页面 | 一期只交付基座和管理端，页面装修等拆到后续 |
| 审核流复杂度 | 企业可能需要多级审核 | 一期使用固定状态流转，复杂流程后续接入 Workflow |
| 前台门户未交付 | 一期只提供后台管理和数据能力 | 前台站点运行时、静态化和 CDN 另行设计 |
| 富文本编辑器差异 | 不同项目可能使用不同编辑器 | 一期仅要求正文字段和基础编辑能力 |
| 文件能力依赖 | Logo、封面、Banner、广告素材需要文件标识 | 复用 Mango 文件能力，CMS 只保存文件 ID 或 token |
| 定时发布执行 | 保存定时发布与自动执行涉及调度能力 | 一期至少保存定时发布关系；自动执行可在实现设计中评估是否接入 Mango Job |
| 部署形态增加 | 同时覆盖单体管理端、admin-shell 微前端、`site-shell` 和站点 App | 交付台账必须拆分管理端、`site-shell` 和站点 App 验收项 |
| 站点主题差异 | 两个站点 App 可能出现重复展示代码 | 公共运行逻辑放入 `site-shell`，页面布局、主题和视觉组件保留在站点 App |
| 前台访问权限 | 栏目支持公开、登录和角色访问 | 一期至少验证公开访问；登录和角色访问在设计中明确鉴权接入点 |

## 17. 后续阶段

二期候选能力：

- 页面装修。
- Widget 门户。
- 专题管理。
- 内容推荐算法。
- 全文检索。
- 静态化发布。
- CDN 发布。
- 多语言内容版本。
- 内容版本管理。
- 复杂审核流。
- 更多站点模板。
- 站点主题在线配置。
- 官网静态化和 CDN 发布。

## 18. 交付物

本需求文档的后续交付物建议包括：

- CMS 设计说明。
- CMS 交付台账。
- 后端 `mango-cms` 模块。
- 前端 `packages/cms` 管理页面包。
- 前端 `site-shell` 薄运行时包。
- 企业官网站点 App。
- 帮助中心站点 App。
- Flyway migration。
- starter 菜单和权限资源。
- 后端测试。
- 前端 E2E 或组件测试。
- 验收证据。
