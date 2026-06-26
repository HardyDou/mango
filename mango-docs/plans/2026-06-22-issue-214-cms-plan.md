# Mango CMS Issue 214 开发计划

设计说明：`mango-docs/designs/2026-06-22-mango-cms-design.md`

交付台账：`mango-docs/plans/2026-06-22-issue-214-cms-delivery-ledger.md`

## 1. 目标

在任务 worktree 内完成 Issue 214 CMS 一期交付，按照“边开发边验证”的方式推进，最终通过浏览器 E2E 用仿真数据验收管理端、`/cms-api`、`site-shell` 和两个站点 App。

## 2. 开发顺序

### Sprint 0：评审、设计和交付合同

交付：

- 需求文档优化。
- 多专家评审记录。
- 设计说明。
- 开发计划。
- 交付台账。

验证：

```bash
node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/2026-06-22-mango-cms-design.md \
  --ledger mango-docs/plans/2026-06-22-issue-214-cms-delivery-ledger.md \
  --mode plan
```

### Sprint 1：后端模块与数据库

交付：

- 新增 `mango-platform/mango-cms` 聚合模块。
- 新增 `mango-cms-api`、`mango-cms-core`、`mango-cms-starter`、`mango-cms-starter-remote`。
- 新增实体、Mapper、Service 基线。
- 新增 11 张 CMS 表 migration。
- 新增模块资源声明。

验证：

```bash
cd mango
mvn -pl mango-platform/mango-cms -am test
```

### Sprint 2：后台管理接口

交付：

- 分类、标签、站点、栏目、内容 CRUD。
- 内容提交审核、审核通过、驳回、下线。
- 发布、批量发布、发布关系下线、置顶、推荐。
- 导航、Banner、广告、站点配置 CRUD。
- 状态流转、删除规则、唯一约束和有效期校验。

验证：

```bash
cd mango
mvn -pl mango-platform/mango-cms/mango-cms-core,mango-platform/mango-cms/mango-cms-starter -am test
```

### Sprint 3：站点消费接口

交付：

- `/cms-api/sites/resolve`
- `/cms-api/sites/detail`
- `/cms-api/site-categories/tree`
- `/cms-api/navigations/list`
- `/cms-api/banners/list`
- `/cms-api/advertisements/list`
- `/cms-api/contents/page`
- `/cms-api/contents/detail`

验证：

```bash
cd mango
mvn -pl mango-platform/mango-cms/mango-cms-starter -am test
```

### Sprint 4：菜单与 CMS 管理页面

交付：

- `cms-common-menu.json`。
- `@mango/cms/admin-pages` 页面注册。
- CMS 管理页面和后台 API 封装。
- Admin 单体和 admin-shell 微前端样式适配。

验证：

```bash
cd mango-ui
pnpm --filter @mango/cms build
pnpm admin:styles:check
pnpm admin:module-styles:check
```

### Sprint 5：`site-shell` 与站点 App

交付：

- `@mango/site-shell` API client、Provider、SEO、状态处理和访问策略接入点。
- 企业官网 App。
- 帮助中心 App。
- 两个 App 均通过 `/cms-api` 读取数据。

验证：

```bash
cd mango-ui
pnpm --filter @mango/site-shell build
pnpm --filter mango-site-enterprise-app build
pnpm --filter mango-site-help-app build
```

### Sprint 6：E2E 与修复

交付：

- 启动后端和前端验收环境。
- 写入或通过接口创建仿真数据。
- 浏览器 E2E 验证后台、单体 Admin、admin-shell、企业官网、帮助中心。
- 修复 E2E 发现的问题，直到核心验收通过。
- 更新交付台账和验收证据。

验证：

```bash
node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/2026-06-22-mango-cms-design.md \
  --ledger mango-docs/plans/2026-06-22-issue-214-cms-delivery-ledger.md \
  --mode verify
```

## 3. 仿真数据

至少准备：

- 企业官网站点：`enterprise`。
- 帮助中心站点：`help`。
- 企业官网栏目：新闻动态、关于我们。
- 帮助中心栏目：快速开始、常见问题。
- 内容：平台升级公告、产品介绍、快速开始文档、FAQ 文档。
- Banner：企业官网首页 Banner、帮助中心首页 Banner。
- 广告：企业官网首页广告、帮助中心列表广告。
- 发布关系：同一篇公告发布到企业官网新闻动态和帮助中心最新动态。

## 4. 验收方式

- 管理端通过浏览器新增、编辑、发布、下线内容。
- `/cms-api` 用浏览器网络面板和页面展示验证只读过滤。
- 企业官网 App 和帮助中心 App 通过 `site-shell` 展示真实接口数据。
- 停用站点、隐藏栏目、下线内容后刷新前台页面，确认不再展示。
- 检查 console 无未处理错误，network 无异常接口。
