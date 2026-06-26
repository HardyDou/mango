# Mango CMS Issue 214 交付台账

设计说明：`mango-docs/designs/2026-06-22-mango-cms-design.md`

开发计划：`mango-docs/plans/2026-06-22-issue-214-cms-plan.md`

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| CMS-001 | 用户要求 | 优化详细需求文档 | 在 Issue 原需求基础上补齐一期边界、命名、站点 App、`site-shell` 和规范约束 | 需求文档 | 人工检查章节完整性 | DONE | `mango-docs/designs/2026-06-22-mango-cms-requirements.md` |
| CMS-002 | 用户要求 | 多专家评审需求与方案 | 产品、架构、安全、前端、QA 视角评审并形成修订决策 | 评审记录 | 文件存在且包含风险和决策 | DONE | `mango-docs/designs/2026-06-22-mango-cms-review.md` |
| CMS-003 | PMO 要求 | 输出设计说明 | 明确目标、边界、API、DB、权限、菜单、前端结构和测试策略 | 设计说明 | 人工检查设计覆盖关键交付 | DONE | `mango-docs/designs/2026-06-22-mango-cms-design.md` |
| CMS-004 | PMO 要求 | 制定开发计划 | 按 Sprint 拆分并定义每阶段验证命令 | 开发计划 | 人工检查 Sprint 计划 | DONE | `mango-docs/plans/2026-06-22-issue-214-cms-plan.md` |
| CMS-005 | PMO 要求 | 建立交付台账 | 原子化拆分文档、后端、前端、E2E 和验收项 | 交付台账 | delivery-contract-check plan/verify | DONE | `mango-docs/plans/2026-06-22-issue-214-cms-delivery-ledger.md` |
| CMS-006 | 后端规范 | 新增 `mango-cms` 模块 | 按 api/core/starter/starter-remote 结构新增平台模块 | 后端模块 | `mvn -pl mango-platform/mango-cms -am test` | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-007 | 后端规范 | 建立真实数据库表 | 新增 11 张 CMS 表，包含租户、审计、逻辑删除和必要索引 | Flyway migration | Maven 测试和 schema 检查 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-008 | 后端规范 | 后台 API 禁止路径变量 | `/cms/**` 使用 Query、Command 或 query 参数 | Controller/API | 代码检查和接口测试 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-009 | 后端规范 | 分页接口符合规范 | 后台和消费列表使用 `XxxPageQuery` 与 `R<PageResult<XxxVO>>` | API/Service | 后端测试 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-010 | 用户要求 | 内容池和发布关系解耦 | 内容不直接从属于栏目，发布关系连接内容、站点和栏目 | 内容/发布服务 | E2E 验证一文多栏目 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-011 | 用户要求 | 内容状态流转 | 支持草稿、待审核、驳回、已发布、已下线 | 内容服务/API | 状态流转测试 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-012 | 用户要求 | 站点、栏目、导航、Banner、广告和站点配置管理 | 后台提供真实 CRUD 和状态控制 | API/页面 | 后台 E2E | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-013 | 安全规范 | 租户和数据权限符合规范 | 不接收客户端 tenantId，写操作先读后校验，列表和详情接入 DataScopeApplier | Service | 后端测试和代码检查 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-014 | 文件规范 | 文件字段禁止保存 URL | Logo、封面、附件、视频、Banner、广告只保存文件 ID/token | Entity/Command/VO | 代码检查 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-015 | 菜单规范 | 菜单和按钮权限通过 Resource Registry | `cms-common-menu.json` 使用 `AUTH_MENU`，不使用 Flyway 初始化菜单 | 资源文件 | 资源文件检查 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-016 | 用户要求 | `/cms-api` 站点消费接口 | 提供站点解析、栏目、导航、Banner、广告、内容列表和详情 | 消费接口 | API/E2E 验证过滤规则 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-017 | 前端规范 | 新增 `@mango/cms` 管理页面包 | 提供 API 封装、页面、样式和 `admin-pages` 注册 | `mango-ui/packages/cms` | 前端构建和浏览器验收 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-018 | 前端规范 | 适配 Admin 单体和 admin-shell 微前端 | 同一套 CMS 页面注册和样式在两种部署下可用 | admin 模块配置 | 样式检查和浏览器验收 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-019 | 用户要求 | 新增 `@mango/site-shell` | 提供站点解析、Provider、SEO、状态处理、访问策略和 `/cms-api` 封装 | `mango-ui/packages/site-shell` | 构建和站点 App E2E | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-020 | 用户要求 | 交付企业官网 App | 站点 App 自带页面、主题和内容展示，消费 `site-shell` | 企业官网 App | 浏览器 E2E | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-021 | 用户要求 | 交付帮助中心 App | 站点 App 自带页面、主题和文档展示，消费 `site-shell` | 帮助中心 App | 浏览器 E2E | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-022 | QA 要求 | 用仿真数据完成浏览器 E2E | 覆盖后台创建、发布、前台展示、停用、隐藏、下线 | E2E 证据 | 浏览器操作、console/network 检查 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |
| CMS-023 | 用户要求 | 交付验收地址、验收内容和验收方法 | 最终提供可访问 URL、数据和验收步骤 | 最终说明 | 用户按步骤验收 | DONE | `mango-docs/evidence/2026-06-22-issue-214-cms/acceptance-evidence.md` |

## 2. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| CMS-001 | 文档 | 需求边界 | 不适用 | 一期边界、命名、规范已写入 | 不涉及 | 不涉及 | 需求文档 | PASS |
| CMS-002 | 文档 | 专家评审 | 不适用 | 风险和修订决策已记录 | 不涉及 | 不涉及 | 评审记录 | PASS |
| CMS-003 | 文档 | 设计说明 | 不适用 | 设计覆盖 API、DB、权限、前端和测试 | 不涉及 | 不涉及 | 设计说明 | PASS |
| CMS-004 | 文档 | 开发计划 | 不适用 | Sprint 和验证命令完整 | 不涉及 | 不涉及 | 开发计划 | PASS |
| CMS-005 | 文档 | 交付台账 | 不适用 | 台账列完整，进入计划检查 | 不涉及 | 不涉及 | 台账 | PASS |
