# Mango CMS Issue 214 验收证据

## 1. 验收范围

- 页面：`@mango/cms` 管理页面包、企业官网 App、帮助中心 App。
- 接口：后台 `/cms/**`、公开站点 `/cms-api/**`、健康检查 `/actuator/health`。
- 权限：后台接口权限码、Resource Registry 菜单、公开站点接口访问边界。
- 数据：CMS 站点、栏目、导航、Banner、广告、内容、发布关系、站点配置。
- 部署形态：后台单体 Admin、`site-shell`、企业官网 App、帮助中心 App。

## 2. 执行环境

- 前端地址：Admin Shell `http://a.mango.io:5176`，CMS App `http://e.mango.io:5184`，Enterprise `http://127.0.0.1:5191`，Help `http://127.0.0.1:5192`。
- 后端地址：`http://127.0.0.1:18970`，健康检查 `http://127.0.0.1:18970/actuator/health`。
- 数据库或租户：当前 worktree 本地 Mango 开发库；健康检查确认 MySQL `UP`。
- 测试数据：后台 CMS E2E 使用时间戳隔离数据并调用真实后端；两个站点 App E2E 使用 Playwright 拦截的 `/cms-api` 仿真数据。
- 浏览器：Playwright Chromium。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| CMS-001 | 需求文档 | 需求边界、命名、规范约束 | Issue 214 与用户追加说明 | 文档包含 CMS 管理页、`cms-api`、`site-shell`、两个站点 App、禁止路径变量、租户、分页和数据权限要求 | 文档类交付，检查章节和验收标准可执行 | 文档类交付，未产生浏览器请求 | `mango-docs/designs/2026-06-22-mango-cms-requirements.md` | PASS |
| CMS-002 | 评审记录 | 多专家需求与方案评审 | 产品、架构、后端、前端、QA、安全视角 | 评审记录包含阻断风险、非阻断风险、修订决策和例外口径 | 文档类交付，检查风险闭环和决策记录 | 文档类交付，未产生浏览器请求 | `mango-docs/designs/2026-06-22-mango-cms-review.md` | PASS |
| CMS-003 | 设计说明 | 方案边界 | CMS 后端、管理页、`site-shell`、站点 App | 设计覆盖模块边界、API、DB、权限、菜单、部署形态和测试策略 | 文档类交付，检查表格和章节完整 | 文档类交付，未产生浏览器请求 | `mango-docs/designs/2026-06-22-mango-cms-design.md` | PASS |
| CMS-004 | 开发计划 | 分阶段开发与验证 | Sprint 计划 | 计划包含文档、后端、前端、E2E 和交付收尾步骤 | 文档类交付，检查每阶段有验证方式 | 文档类交付，未产生浏览器请求 | `mango-docs/plans/2026-06-22-issue-214-cms-plan.md` | PASS |
| CMS-005 | 交付台账 | 原子验收项 | CMS-001 至 CMS-023 | 台账逐项记录来源、要求、交付物、验收方式、状态和证据 | 文档类交付，检查无遗留 TODO 状态 | PMO 台账检查覆盖状态合法性 | `mango-docs/plans/2026-06-22-issue-214-cms-delivery-ledger.md` | PASS |
| CMS-006 | `mango-platform/mango-cms` | 后端模块装配 | `mango-cms-api/core/starter/starter-remote` | Maven 聚合测试通过，CMS 模块被 `mango-admin-starter` 引入 | 后端能力交付，页面交互不适用 | Maven 测试未报告失败 | `mvn -pl mango-platform/mango-cms/mango-cms-core,mango-platform/mango-cms/mango-cms-starter -am test` | PASS |
| CMS-007 | Flyway migration | CMS 业务表 | 站点、栏目、内容、发布、导航、Banner、广告、配置表 | migration 包含租户、审计、逻辑删除、唯一约束和索引；domain 改为全局唯一以支撑匿名域名解析 | 后端数据交付，页面交互不适用 | Maven 测试覆盖 schema 和服务规则 | `mango/mango-platform/mango-cms/mango-cms-core/src/main/resources/db/migration/mango-cms/V1__init_mango_cms.sql` | PASS |
| CMS-008 | `/cms/**` Controller | 禁止路径变量 | 后台 CMS API | Controller 契约测试确认未使用 `@PathVariable`，接口使用 query/body 参数 | 后端接口交付，页面交互不适用 | Maven 测试未报告失败 | `CmsControllerContractTest`，后端 Maven 测试命令 | PASS |
| CMS-009 | `/cms/**`、`/cms-api/**` | 分页规范 | `PageQuery`、`PageResult` | 后台和公开内容分页返回 `PageResult`；公开内容 join 查询按 `content_id` 去重 | 后端接口交付，页面交互不适用 | Maven 测试未报告失败 | 后端 Maven 测试命令；`CmsContentMapper` | PASS |
| CMS-010 | 内容/发布服务 | 内容池和发布关系解耦 | 一篇内容发布到站点栏目 | 内容实体与发布关系分表，消费接口按发布关系读取有效内容 | 站点页面展示内容列表，不直接依赖管理端页面 | 站点 E2E network 记录 `/cms-api/contents/page` 请求 | `site-e2e-network-summary.json` | PASS |
| CMS-011 | 内容服务 | 内容状态流转 | 草稿、待审核、驳回、已发布、已下线 | 服务实现内容状态变更和公开接口有效状态过滤 | 管理页有状态操作入口，后台 CMS E2E 已覆盖完整管理链路 | Maven 测试未报告失败 | 后端 Maven 测试命令 | PASS |
| CMS-012 | `/cms/**` 管理接口与页面包 | 站点、栏目、导航、Banner、广告和配置管理 | CMS 管理资源 | 后台 API 和 `@mango/cms` 页面包已提供 CRUD、状态控制和页面注册 | 已完成构建与包级页面集成；后台 `cms-management.spec.ts` 已覆盖真实后端管理链路 | 前端构建与样式检查通过 | 前端 build 与 admin 样式检查命令 | PASS |
| CMS-013 | 后台租户与数据权限 | 租户、数据权限 | 当前安全上下文租户和 CMS org 字段 | 列表与详情对象级读取均接入 `DataScopeApplier`，使用 `created_by`、`org_id`、`tenant_id` 映射；写操作先按带数据权限的详情读取目标对象 | 后台 E2E 覆盖当前租户业务流；单测覆盖 data-scope 调用和字段映射 | `CmsAdminServiceSecurityTest` 断言 `cms:site:list`、`cms_site`、`created_by`、`org_id`、`tenant_id` 映射；Maven 测试通过 | Persistence README 数据权限规范；`CmsAdminServiceSecurityTest`；后端 Maven 测试命令 | PASS |
| CMS-014 | 文件字段 | 禁止保存文件访问 URL | Logo、封面、附件、Banner、广告素材字段 | 代码只保存 fileId/token，不保存下载 URL、预览 URL 或对象存储直连地址；保存时调用 `FileApi` 校验存在性、可见性、完成状态、归档状态和媒体类型 | 文件字段属于后端数据规则，页面交互不适用 | 代码检查未发现 URL 字段作为持久化引用；单测覆盖不存在、未完成、类型不匹配和合法图片 | `CmsAdminService#validateFile`；`CmsAdminServiceSecurityTest` | PASS |
| CMS-015 | Resource Registry | 菜单和按钮权限 | `cms-common-menu.json` | CMS 菜单使用 Resource Registry，未用 Flyway 初始化菜单和按钮权限 | Admin 聚合样式检查通过，菜单资源文件与页面 key 对齐 | 样式聚合检查未报告缺失 | `cms-common-menu.json`；`pnpm admin:styles:check && pnpm admin:module-styles:check` | PASS |
| CMS-016 | `/cms-api/**` | 公开站点消费接口 | 域名 `127.0.0.1`，导航、Banner、广告、栏目、内容 | 匿名场景默认按 domain 解析；无租户上下文时禁止匿名 `siteCode`，公开 VO 不暴露后台字段 | 企业官网和帮助中心页面按接口数据显示站点内容 | network 摘要记录 10 个 `/cms-api` 请求，错误数组为空 | `site-e2e-network-summary.json`，三张站点截图 | PASS |
| CMS-017 | `@mango/cms` | CMS 管理页面包 | API 封装、页面、`admin-pages`、样式入口 | `@mango/cms` 构建通过，并被 `@mango/admin` 默认集成 | 页面包构建通过，后台 CMS E2E 已通过 | 构建命令未报告失败 | `pnpm -F @mango/cms build` | PASS |
| CMS-018 | Admin 单体与 admin-shell | 灵活部署适配 | `@mango/admin`、`mango-admin-shell` | 单体聚合和样式依赖构建通过；admin-shell 通过 `cms-management.spec.ts` 完整浏览器 E2E 覆盖 CMS 管理资源 | Admin Shell 地址保留供人工验收；E2E 已验证页面打开、创建、发布、状态流转和站点配置 | `cms-management.spec.ts` 通过；`/admin build:style-deps` 通过；`mango-admin-cms-app build` 通过 | 后台 CMS E2E 命令；前端构建命令 | PASS |
| CMS-019 | `@mango/site-shell` | 站点运行时薄壳 | 站点解析、API client、SEO、访问策略 | `site-shell` 构建通过，站点 App 通过它请求 `/cms-api`，默认按当前域名解析 | 两个站点 App 页面自含风格和页面，`site-shell` 不承载页面主题 | network 摘要确认请求带 `domain=127.0.0.1` 且未带默认 `siteCode` | `pnpm -F @mango/site-shell build`；`site-e2e-network-summary.json` | PASS |
| CMS-020 | 企业官网 App | 官网首页展示 | 域名 `127.0.0.1`，导航、Banner、内容列表 | 首页能展示站点信息、导航、Banner 和内容列表；请求链路全部走 `/cms-api` | 截图显示企业官网首页布局和内容区域，未依赖管理端页面 | network 摘要记录 enterprise 站点 5 个公开接口请求，错误数组为空 | `enterprise-site-home.png`；`pnpm -F mango-site-enterprise-app test:e2e` | PASS |
| CMS-021 | 帮助中心 App | 帮助中心首页和搜索 | 域名 `127.0.0.1`，关键词 `权限` | 首页、广告、栏目、内容列表和搜索请求走 `/cms-api`，搜索请求包含 keyword | 截图显示帮助中心首页和搜索结果状态 | network 摘要记录 help 站点 5 个公开接口请求，错误数组为空 | `help-site-home.png`，`help-site-search.png`；`pnpm -F mango-site-help-app test:e2e` | PASS |
| CMS-022 | 浏览器 E2E | 仿真数据验收 | 后台真实后端时间戳数据；两个站点 App 仿真 `/cms-api` 数据 | 后台 CMS 管理端浏览器 E2E 已覆盖站点、栏目、内容、发布、导航、Banner、广告、站点配置和公开接口探测；两个站点 App E2E 已覆盖仿真 `/cms-api` 渲染 | 后台 E2E 无失败；站点页面完成首页、搜索和公开接口渲染断言 | 后台 `cms-management.spec.ts` 通过；两个站点 App E2E 通过；network 错误数组为空 | 后台 CMS E2E 命令；`site-e2e-network-summary.json`；三张站点截图 | PASS |
| CMS-023 | 验收交付 | 地址、内容、方法 | Admin、后端、Enterprise、Help | 最终报告提供验收地址、验收内容、命令和证据 | 服务保留运行供用户人工验收 | 健康检查返回 `UP`，服务状态显示 backend/admin 运行中 | 本验收证据文件和最终说明 | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 后端 CMS | `/cms-api/sites/resolve` | 匿名按 domain 解析站点 | 匿名默认不使用 siteCode | 接口类抽查，确认公开 VO 边界 | `site-e2e-network-summary.json` | PASS |
| 企业官网 App | `http://127.0.0.1:5191` | 首页加载站点内容 | Banner 和内容列表展示 | 首页首屏有业务内容，布局可读 | `enterprise-site-home.png` | PASS |
| 帮助中心 App | `http://127.0.0.1:5192` | 首页加载文档内容 | 关键词搜索刷新列表 | 首页和搜索结果有业务内容，布局可读 | `help-site-home.png`，`help-site-search.png` | PASS |
| Admin Shell | `http://a.mango.io:5176` | CMS 包纳入 Admin Shell 聚合 | 样式聚合检查和后台 CMS E2E | 服务保留运行供人工验收 | `cms-management.spec.ts`；`pnpm -F @mango/admin build:style-deps` | PASS |

## 5. 后续增强项

| 项目 | 当前结论 | 后续处理 | 交付影响 |
|---|---|---|---|
| 跨租户和跨组织手工矩阵 | 后端已通过 `DataScopeApplier` 单测覆盖字段映射和调用链，后台 E2E 覆盖当前租户完整业务流 | 后续可补多账号、多组织、多租户手工矩阵和浏览器用例 | 非阻塞，不影响本次交付验收 |
| 文件中心真实素材联调 | 后端已调用 `FileApi` 校验文件存在性、可见性、完成状态、归档状态和媒体类型，单测覆盖异常分支 | 后续可接入更多真实图片、视频、附件样例做素材库联调 | 非阻塞，不影响本次交付验收 |
| 视觉截图矩阵扩展 | 企业官网、帮助中心和后台 CMS E2E 已通过，当前证据包含站点截图和命令记录 | 后续可补更多断点、主题色和暗色模式截图 | 非阻塞，不影响本次交付验收 |
