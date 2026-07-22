# 菜单页面打不开排障

> 2026-07-22 菜单显示文案调整说明：通知中心、审批中心和编号规则分别更名为通知管理、审批管理和编号管理；路由、菜单编码、权限码、页面 key、资源同步和本指南排障步骤均不受影响，历史记录保留原名称。

## 1. 适用场景

用户登录后能看到菜单，但点击菜单出现空白页、404、组件加载失败或接口无权限。

### 2026-07-16 组织模块修复影响

组织和岗位页面的菜单 key、页面入口及权限码不变。`@mango/rbac` 仅把组织成员、负责人和岗位接口调整为与后端 API 一致的固定路径，并使用 query/body 承载 ID；业务项目不要继续拼接旧的 `/org/{orgId}/members` 或 `/org/leader/{orgId}` 路径。若公司租户能看到菜单但组织接口返回 403，同时核对登录响应与 `/auth/info` 的 `tenantId`、`partyId` 和 `system:org:*` 权限是否一致。

### 2026-07-16 System 租户菜单派生边界

新租户资源同步会先形成 System 租户，再创建管理员角色并刷新该租户的 `ROLE_ADMIN` 菜单套餐绑定。若新租户能登录但菜单为空或管理接口返回 403，优先核对租户的 package 绑定、`ROLE_ADMIN` 角色及 `role_menu`派生结果；不要通过关闭权限校验或手工补表规避。现有菜单 key、权限码和页面入口未改变。

### 2026-07-21 后台品牌 Logo 配置影响

后台品牌配置新增折叠 Logo 字段，只影响 Admin Shell 展开态/折叠态 Logo 展示和后台品牌配置保存；不改变菜单 component key、页面注册、动态路由、角色授权、菜单权限和页面打开排障步骤。

## 2. 阅读顺序

| 顺序 | 文档                                                                                     | 关注点                         |
| ---- | ---------------------------------------------------------------------------------------- | ------------------------------ |
| 1    | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 菜单、权限、资源同步、授权关系 |
| 2    | [@mango/rbac README](../../../mango-ui/packages/rbac/README.md)                          | RBAC 前端包和 API              |
| 3    | [RBAC Views README](../../../mango-ui/packages/rbac/src/views/README.md)                 | 页面 key 和组件映射            |
| 4    | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md)            | 页面注册、菜单渲染、登录后装配 |

## 3. 接入检查点

| 环节       | 检查点                                                                   |
| ---------- | ------------------------------------------------------------------------ |
| 菜单数据   | `/authorization/menus/user?fmt=tree&appCode=internal-admin` 返回目标菜单 |
| 页面 key   | 菜单 `component` 字段能匹配前端页面 key                                  |
| 前端注册   | 前端包已引入并完成页面注册或路由映射                                     |
| 角色授权   | 当前用户角色已绑定目标菜单                                               |
| 租户绑定   | 当前租户已绑定目标应用和菜单包                                           |
| 运行态请求 | 浏览器 network 中页面依赖和业务接口没有未解释的 401/403/404              |

## 4. 最小闭环

1. 用目标用户登录。
2. 打开菜单接口，确认返回目标菜单和 component key。
3. 在前端页面 key 文档中确认 component key 存在。
4. 点击菜单，页面组件正常加载。
5. 浏览器 network 中页面资源、菜单接口和业务接口没有未解释的 401/403/404。

## 5. 页面 key 对照

| 能力     | 常见页面 key 文档                                                                          |
| -------- | ------------------------------------------------------------------------------------------ |
| Auth     | [Auth Views README](../../../mango-ui/packages/auth/src/views/README.md)                   |
| File     | [File Components README](../../../mango-ui/packages/file/src/components/README.md)         |
| Job      | [Job Views README](../../../mango-ui/packages/job/src/views/README.md)                     |
| RBAC     | [RBAC Views README](../../../mango-ui/packages/rbac/src/views/README.md)                   |
| System   | [System Components README](../../../mango-ui/packages/system/src/components/README.md)     |
| Workflow | [Workflow Components README](../../../mango-ui/packages/workflow/src/components/README.md) |

## 6. 常见失败

| 现象               | 优先检查                                            |
| ------------------ | --------------------------------------------------- |
| 菜单存在但点击空白 | component key 与前端注册表不一致                    |
| 菜单不存在         | resource manifest、迁移 SQL、角色授权和租户应用绑定 |
| 页面加载但接口 403 | Access、Authorization 和当前用户权限集合            |
| 刷新后页面丢失     | 前端路由 fallback、admin-shell 注册时机             |
| 只有某租户异常     | 租户应用绑定、菜单包绑定、租户初始化数据            |

## 7. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-authorization -am test
pnpm -F @mango/rbac build
pnpm -F @mango/admin-shell build
```

模块验证入口：

- [Authorization 验证方式](../../../mango/mango-platform/mango-authorization/README.md#10-验证方式)
- [RBAC Frontend 验证方式](../../../mango-ui/packages/rbac/README.md#10-验证方式)
- [Admin Shell 验证方式](../../../mango-ui/packages/admin-shell/README.md#10-验证方式)

## 8. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 9. 变更影响记录

- Issue #606 只为 Workflow demo 默认管理员补齐流程定义管理的数据范围，并保护已有 `INIT_ONLY` 目标配置；不改变菜单树 API、`component` key、角色菜单授权、租户应用绑定或页面注册协议。流程定义菜单已显示但列表为空时，应额外检查 `ROLE_ADMIN + workflow:definition:list` 数据范围；其它菜单问题仍按本指南链路排查。

- MySQL 8.4 告警治理仅将 Authorization 空库 V1 中两个布尔列改为无显示宽度的 `tinyint`；菜单树 API、页面 key、角色授权、资源声明和本指南排障步骤均不改变。既有数据库不执行结构转换，新建数据库按相同业务语义初始化。

- PR #557 将 Resource 收敛为六个发布物，并补强微服务乱序启动、锁竞争重试和内部调用验签；不改变菜单树 API、`component` key、菜单权限码、角色授权、租户应用绑定或页面注册协议。菜单缺失时仍按资源同步日志、角色菜单关系、租户绑定和前端页面 key 链路排查。

- PR #541 的 Auth 历史债务修复不改变菜单树接口、`component` key、角色菜单授权、租户应用绑定或页面注册协议。Admin Shell 退出登录现在会先调用服务端 `/auth/logout` 撤销令牌并清除 HttpOnly Cookie，再清理本地会话；退出后出现的 401 属于已撤销会话的预期结果，重新登录后的菜单问题仍按本指南排查。

- v2026.07.14-maven-1.0.21-platform-debt-release 仅同步 Payment、CMS、Workflow、Notice 修复及配套前端版本锁，不改变 RBAC 菜单接口、页面 component key、角色授权或本指南排查步骤。

- v2026.07.11-maven-1.0.14-cli-release 仅将当前后端实现向前发布为 Maven `1.0.14` 并更新 CLI 后端版本锁；不改变 RBAC 菜单页面 key、菜单/按钮权限码、角色授权、租户边界或排障步骤。

- v2026.07.11-npm-readme-forward-release 仅向前发布已更正的 package README 并传播精确 npm 依赖版本；不改变 RBAC 菜单页面 key、菜单与按钮权限码、角色授权、租户边界或本场景排障步骤。

- PR #425 仅新增可复用登录流程 Hook，并允许 `/login` 注入业务自定义登录组件；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户绑定、页面路由和本场景排障步骤。业务侧自定义登录 UI 后如遇页面入口缺失，仍按菜单资源同步、角色授权、租户绑定和前端页面 key 注册链路排查。

- v2026.07.08-admin-page-layout-release 只发布后台统一页面骨架组件、运营列表页 CLI/starter 模板和前端 npm 版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户绑定、页面路由和本场景排障步骤。业务项目升级时按发布说明成组升级前端 `@mango/*` 包和 `@mango/cli`。

- 本次 PR 将菜单可见性和接口权限码拆分：`menuCode` 只决定菜单授权和菜单树可见性，`apiCodes` 表示该菜单页面需要的接口权限码。Resource `AUTH_MENU` 不再声明 `permissionItems`/`permissionCode` 按钮节点；业务模块需要给页面自动带上的接口权限，应写在目标菜单的 `apiCodes` 中。角色授权菜单后会获得该菜单的 `apiCodes`，但只拿到接口权限不会反向带出父菜单。若要给匿名或登录用户配置基础接口权限，使用隐藏菜单并绑定 `ROLE_ANONYMOUS` 或 `ROLE_LOGIN`，隐藏菜单不会出现在用户菜单树中。

- 本次 PR 修复 Resource Registry 依赖重放：`AUTH_MENU` 明确依赖 `AUTH_ROLE`，同一轮同步会先写角色再写菜单，并在角色声明创建或更新后重放依赖角色的菜单声明以补齐 `roleCodes` 授权；不改变菜单 `component` key、菜单树接口、页面注册方式、租户绑定、页面路由和前端排障步骤。排查菜单不存在或角色看不到菜单时，优先确认 `AUTH_ROLE`、`AUTH_MENU` 同步日志和 `authorization_role_menu` 绑定结果，不要绕过 Resource API 直接写授权表或调用 core service。

- v2026.07.07-maven-1.0.9-api-contract-release 仅发布 workflow API 边界治理物料、前端聚合包版本锁、CLI/starter 版本锁和 Mango Docs 版本快照；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。业务项目升级时按发布说明成组升级后端 `<mango.version>`、前端 `@mango/*` 包和 `@mango/cli`。

- PR #389 修复首页管理页面集成和编辑体验，`home/templates/index`、`home/list/index`、`home/user/index` 页面 key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定和通用排障步骤不变。排查首页管理页面打不开时，优先确认业务开发环境是否使用源码 alias 或已升级到同批次 `@mango/home`、`@mango/admin-shell`、`@mango/admin`，避免 `homePageApi` 等导出从过期可选 peer 包解析。

- PR #388 新增站内消息结构化动作协议，消息动作可跳转注册路由、流程入口或触发后端事件；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定和通用排障步骤。排查通知消息动作跳转失败时，除确认 `@mango/notice` 页面注册外，额外确认动作 `targetType`、`targetKey` 与业务注册路由或流程入口是否匹配。

- PR #358 新增 `mango-link` 网址导航能力，后端通过 `META-INF/mango/resources/link-common-menu.json` 注入 `网址导航` 和 `网址管理` 菜单，前端通过 `@mango/link/admin-pages` 注册 `link/company/index`、`link/favorites/index`、`link/my-links/index`、`link/categories/index`、`link/items/index` 页面 key。本场景排障步骤不变；排查网址导航页面空白、404 或 403 时，额外确认 `mango-link-starter` 是否启用、resource 是否同步、角色是否获得 `platform_admin` 或 `institution_collaboration` 菜单套餐，以及 `@mango/link` 是否随 admin 聚合入口注册。

- v2026.06.30-maven-1.0.1-admin-branding-cli-release 发布固定后端 Maven `1.0.1` 和后台品牌配置前端批次；不改变菜单 `component` key 解析、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。业务项目排查品牌配置页面时，额外确认资源同步和 `system:admin-branding:query`、`system:admin-branding:edit` 授权。

- PR #327 扩展 `AUTH_SUBJECT_ROLE` 基线声明，支持通过 `subjectId`、`subjectCode`、`memberNo` 或 `username` 解析租户成员后绑定角色；不改变菜单 `component` key、菜单树接口、页面注册方式、角色菜单授权关系、按钮权限关系、租户应用绑定和页面路由。排查清库初始化后的菜单可见性时，如依赖成员角色绑定基线，需要额外确认声明中的稳定主体键能解析到未离租的 `tenant_member`。

- PR #314 修复授权 API 资源运行时匹配和同步覆盖：同 method + path 的旧模块 active 资源会在新扫描结果注册时被禁用，精确路径优先于通配符或路径变量匹配。菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定和页面路由不变。排查页面能打开但基础接口 403 时，除角色菜单授权外，需要确认基础接口是否声明为 `LOGIN`/`PUBLIC`、资源同步是否已执行、`authorization_api_resource` 中同路由旧 `PERMISSION` 记录是否已禁用，并刷新 API 资源运行时缓存。

- 本次 PR 允许授权快照从页面菜单 `permissions` 读取权限码，页面级 `menuType=2` 权限和按钮级 `menuType=3` 权限都会进入登录态权限集合；不改变菜单 `component` key、菜单树接口、页面注册方式、租户应用绑定和页面路由。排查页面能打开但接口 403 时，需要同时确认角色是否授权页面菜单及其 `permissions` 是否包含目标接口权限码。

- v2026.06.29-workflow-return-cli-db-release 只发布本地开发 CLI/PMO 基线、工作流退回和前端聚合版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定、页面路由和本场景排障步骤。

- PR #295 只治理 Issue #183 后端测试规范、Mockito 审计和核心 service/resource handler 集成测试；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- PR #183 只治理后端测试规范、Mockito 审计和授权资源处理器测试样板；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- v2026.06.27-workflow-history-dialog-release 同步发布工作流 UI 修复批次和 `@mango/admin@1.0.33`、`@mango/cli@1.0.46` 版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- v2026.06.27-admin-shell-menu-redirect-release 发布 `@mango/admin-shell@1.0.28`、`@mango/admin@1.0.32` 和 `@mango/cli@1.0.45`，让业务项目可通过 npm 包消费 Issue #274 的目录菜单 redirect 修复；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定和本场景排障步骤。业务项目如仍复现顶层目录跳到无权限 redirect 页面，应先确认前端依赖已升级到本发布批次。

- Issue #274 修复 `@mango/admin-shell` 目录型菜单 redirect 解析：目录菜单的 `redirect` 只有命中当前用户可见且可运行的菜单时才生效，否则会进入当前可见菜单树中的第一个可运行子页面；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定和本场景排障步骤。排查“点击顶层目录进入无权限页面”时，应同时确认当前用户可见菜单树中是否包含 redirect 目标以及是否存在可运行子页面。

- v2026.06.27-system-component-release 同步发布 `@mango/system@1.0.11` 及其前端依赖批次，仅对齐 npm 物料和 CLI/starter 版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定、页面路由和本场景排障步骤。业务项目排查页面加载异常时，仍先确认前端包批次一致、页面 key 已注册、后端菜单资源已同步。

- PR #267 将通知公告能力拆分为管理端 `通知中心` 和用户端 `消息中心`：管理端 `通知中心` 下包含公告管理、消息配置、发送任务、渠道配置、发送记录、失败重试，用户端 `消息中心` 下包含我的消息、公告，`接收设置` 保留为隐藏辅助路由。排查通知相关菜单时，需要确认后端菜单资源、当前用户角色授权、`component` key 和前端 `@mango/notice` 页面注册是否匹配；这次不改变菜单树接口、页面注册机制、角色授权关系、登录态权限聚合、租户绑定和通用排障步骤。

- PR #256 将后台工作台默认布局调整为页面内固定配置，仅影响没有个人工作台配置或恢复默认后的首页卡片排布；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- v2026.07.02-maven-1.0.6-home-widgets-cli-release 仅调整首页小组件 package 归属、`@mango/admin@1.0.37` / `@mango/admin-shell@1.0.32` 版本锁和 generated backend baseline；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。业务项目升级时按发布说明成组升级后端 `<mango.version>`、前端 `@mango/*` 包和 `@mango/cli`。

- PR #253 新增 Resource Registry 的 `AUTH_ROLE`、`AUTH_ROLE_DATA_SCOPE`、`AUTH_SUBJECT_ROLE` 基线声明，可让角色、角色数据权限和成员角色绑定随资源同步注入；不改变菜单 `component` key、菜单树接口、页面注册方式、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。排查清库初始化后的菜单可见性时，可额外确认角色基线声明是否先于 `AUTH_MENU.roleCodes` 完成同步。

- Issue #264 发布 `@mango/grid-widgets@1.0.3`、`@mango/system@1.0.10`、`@mango/admin-pages@1.0.11`、`@mango/admin-shell@1.0.25`、`@mango/admin@1.0.29`、`@mango/cli@1.0.42`，补齐此前未进入 npm 物料的工作台日历小组件和新版系统配置页面；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。业务项目排查首页日历缺失或系统配置页面旧版时，应先确认前端包和 CLI/starter 锁已升级到本批次。
- PR #246 发布 `@mango/grid-widgets@1.0.2`、`@mango/admin-shell@1.0.23`、`@mango/admin@1.0.27`、`@mango/cli@1.0.39`，用于修复 grid widgets 样式发布产物并对齐业务项目版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。

- PR #244 发布 `@mango/cms@1.0.0`、`@mango/site-shell@1.0.0`、`@mango/admin@1.0.26`、`@mango/cli@1.0.38` 并对齐 CMS 发布物料；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。业务升级时按发布说明成组刷新 CMS/Admin/CLI 版本即可，排查菜单页面仍按本指南闭环执行。

- PR #243 新增 CMS 管理页面、`mango-admin-cms-app` 微前端运行态和 `@mango/cms` 页面注册；不改变既有菜单 `component` key 归一化规则、菜单树接口、页面注册机制、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。排查 CMS 菜单时按 `mango-cms` 模块 README 与 `@mango/cms` README 中的页面 key 对照确认。

- PR #241 发布 `@mango/admin-shell@1.0.22`、`@mango/admin@1.0.25` 并新增工作流业务回传路径与审批任务详情页布局优化；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。回传路径仅作用于工作流任务详情页返回按钮，不影响菜单与页面注册协议。

- PR #235 发布 `@mango/admin-shell@1.0.21`、`@mango/admin@1.0.24` 并对齐 `@mango/cli@1.0.36` 的发布版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- 本次 PR 调整 `@mango/admin-shell` 在布局 1、2、4 下的 footer 贴边和内容区底部安全距离；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- 本次 PR 新增 `@mango/grid-widgets` 我的申请系统小组件，并在工作台默认布局中展示；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。我的申请小组件只复用 `workflow:task:list` 权限、`/workflow/task/initiated` 页面入口和新增 `/workflow/business-applies/my/summary` 统计接口，不新增菜单授权协议。

- 本次 PR 新增 `@mango/grid-widgets` 我的待办系统小组件，并在工作台默认布局中展示；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。我的待办小组件只复用已有 `/workflow/task/todo`、`/workflow/task/copied` 页面入口和 `workflow:task:list` 权限，不新增菜单授权协议。

- 本次 PR 为管理端 Element Plus 全局中文 locale 配置，确保分页等内置组件默认文案使用中文；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、启动方式和本场景排障步骤。

- 本次 PR 新增 `@mango/grid-widgets` 消息中心系统小组件，并在工作台默认布局中展示；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。消息中心小组件仅消费当前登录人的站内消息接口和已有 `/notice/site-message` 页面入口，不新增菜单授权协议。

- 本次 PR 新增 `@mango/grid-widgets` 用户信息系统小组件，并在工作台 runtime 透传头像、角色、应用标识和租户展示字段；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。用户信息小组件只消费已登录上下文和已有 `/profile`、`/password` 跳转入口，不新增菜单授权协议。

- PR #216 加固前端 `@mango/*` npm 包发布边界，非 CLI 包不再发布 `src` 等源码目录，并补充发布包 tarball 和业务消费 typecheck 基线；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。业务项目应继续使用公开 package 入口和样式入口，升级到后续发布的新包版本后重新运行前端 typecheck。

- PR #215 新增 `@mango/grid-widgets` 小组件注册聚合能力与工作台快捷入口小组件；本次不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。工作台快捷入口只消费登录后可见菜单数据做本地快捷入口展示与跳转，不新增菜单授权协议。

- 本次 PR 仅纠正菜单管理页面字段文案、RBAC views README 页面 key，并补齐授权聚合测试中按钮展示规则断言；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。
- Issue #250 在用户管理页新增锁定状态、密码状态、解锁按钮常驻展示和重置密码弹窗，按钮是否可点取决于 `system:user:unlock`、`system:user:reset-password` 和当前登录态权限集合；不改变菜单 `component` key、菜单树接口、页面注册方式、租户绑定和本场景排障步骤。若点击解锁报无权限，需要同时确认角色是否已拿到 `system:user:unlock`，以及当前登录态是否刷新到最新权限。

- PR #207 补齐 `mango-resource-api` 中的
  `ResourceTypes.FRONTEND_APP_REGISTRY` 和
  `ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY` Java 常量，并保持授权侧
  `AuthorizationResourceTypes` 兼容别名；不改变菜单 `component` key、菜单树接口、
  页面注册方式、角色授权、按钮权限、租户绑定、前端运行态同步资源类型字符串和本场景排障步骤。
- PR #206 新增授权侧前端运行态 Resource Registry 同步和 `runtimeDescriptor` 返回的部署 profile、前端应用注册、模块运行策略信息；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权、按钮权限、租户绑定和本场景排障步骤。清库重建或 1.0 rebase 升级后，若菜单能返回但前端运行态应用或模块策略缺失，需要额外确认 `FRONTEND_APP_REGISTRY` 和 `FRONTEND_MODULE_RUNTIME_STRATEGY` 声明是否已同步到授权前端运行态表。
- PR #199 将平台菜单数据从 Flyway 菜单种子迁移为 Resource Registry 的 `AUTH_MENU` 声明注入，并加固菜单码、权限码和 starter 边界；不改变前端 component key、页面注册、角色授权、按钮权限、租户绑定和菜单渲染协议。清库重建或 1.0 rebase 升级后，排查菜单缺失、菜单层级异常或页面 403 时，需要同时确认 `AUTH_MENU` 声明、Resource Registry 同步日志、目标 handler 消费结果和角色/租户绑定是否完成。
- PR #195 加固前端 `@mango/*` 包的 `exports`、`types` 和生成声明文件，使业务项目通过发布后的 `dist` 产物独立消费；不改变菜单页面 component key、页面注册、角色授权、按钮权限、租户绑定、菜单运行时加载、启动方式和本场景排障步骤。业务项目应继续使用公开 package 入口和 `./style.css`，不要依赖包内 `src` 路径。
- PR #194 发布资源注册中心版本并升级 `@mango/admin@1.0.23`、`@mango/admin-shell@1.0.20`、`@mango/rbac@1.0.8`、`@mango/common@1.0.10`、`@mango/cli@1.0.34` 等前端包；不改变菜单页面 component key、页面注册、角色授权、按钮权限、租户绑定、菜单运行时加载和本场景排障步骤。业务升级时应成组升级前端 `@mango/*` 包并刷新后端 Mango `1.0.0-SNAPSHOT` 依赖。
- PR #193 新增 `mango-resource` 注册中心并将授权接口资源迁移为资源声明同步；不改变菜单页面 component key、前端页面注册、角色授权、按钮权限、租户绑定和菜单运行时加载。排查菜单不存在时，除原有 migration/resource manifest 外，还需要确认 `API_RESOURCE` 声明是否已同步到授权资源表。
- PR #181 将授权数据权限提供者改为由 `AuthorizationAutoConfiguration` 显式注册，修复业务同时引入授权与持久化 starter 时 `DataScopeApplier` 未创建导致的启动失败；不改变菜单页面 component key、页面注册、角色授权、按钮权限、租户绑定、授权 API、配置项和本场景排障步骤。业务只需要刷新后端 Maven `1.0.0-SNAPSHOT` 依赖，不需要升级 npm 包。
- PR #179 修复持久化 starter 与授权 starter 组合使用时 `DataScopeApplier` 自动配置顺序，并补齐授权 core 服务泛型契约；不改变菜单页面 component key、页面注册、角色授权、按钮权限、租户绑定、授权 API、配置、启动方式和本场景排障步骤。
- PR #176 新增按钮展示规则配置和登录态 `buttonRules` 返回；不改变菜单页面 component key、页面注册、菜单运行时加载、租户绑定和菜单页面排障路径。排查菜单可见但按钮显示异常时，可转到按钮权限排障并同时确认按钮展示规则。
- PR #174 将 `@mango/admin` 默认样式、full 样式和 full registrar 聚合收敛到 `admin-modules.json` 生成，并修复 payment 样式作用域；不改变菜单页面 component key、页面注册、角色授权、按钮权限、租户绑定和菜单运行时加载。排查菜单可见但页面样式缺失或异常时，需要确认应用入口使用 `@mango/admin/style.css` 或 `@mango/admin/style-full.css`，并执行 `pnpm admin:styles:check`、`pnpm admin:module-styles:check` 验证聚合文件未漂移。
- PR #173 Payment 模块接入 `@mango/admin/full`、admin 样式聚合和 mango-cli 可选模块清单；不改变菜单页面 component key、页面注册、角色授权、按钮权限、租户绑定和菜单运行时加载。排查页面可见但样式缺失时，需要同时确认目标 package 是否在 admin 样式聚合链或微前端自身入口中引入。
- PR #171 新增角色数据权限配置入口，角色页面增加行内数据权限配置；不改变菜单页面 component key、页面注册、菜单运行时加载和租户绑定。排查菜单可见但列表数据为空时，需要同时确认角色是否配置了限制性数据权限。
- PR #170 新增 `@mango/common` 的 `MangoDialog` 并在应用管理页面替换新增/编辑弹框外壳，只影响弹框布局和内容滚动体验；不改变菜单页面 component key、页面注册、菜单运行时加载、角色授权、按钮权限、租户绑定和菜单页面排障路径。
- PR #169 角色授权弹框改为展示后端可分配菜单树中的按钮节点，只影响角色授权时的可选节点展示；不改变菜单页面 component key、页面注册、菜单运行时加载、租户绑定和菜单页面排障路径。
- PR #166 工作台自定义布局新增 `@mango/grid-layout` 和 `mango-grid-layout`，仅保存当前登录人的工作台布局 JSON；不改变菜单页面 component key、资源授权、页面注册、权限、租户和菜单运行时行为。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变菜单页面、资源授权、页面 key、权限、租户和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变菜单页面 component key、页面注册、角色授权、按钮权限、租户绑定、菜单运行时加载和本场景排障步骤。
- 本次用户信息小组件视觉优化 PR 仅调整 `@mango/grid-widgets` 中用户信息卡片展示和后台工作台默认布局高度；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。
- PR 本次新增 `@mango/grid-widgets` 日历系统小组件，并在工作台默认布局中展示；不改变菜单页面 component key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。
- 本次 PR 隐藏后台布局配置抽屉中的深色模式、组件大小、缓存 Tagsview 和页面动画入口，仅收口未开放或未完整生效的个人偏好配置展示；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。
- 本次 PR 仅优化工作台系统小组件视觉、提示文案、组件库宽度、快捷入口排布和日历日期字号；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。
- 本次 PR 仅在进入登录页或退出登录时清理后台 TagsView 当前打开标签缓存，避免换账号后点击上一账号标签出现 404；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。
- 本次 PR 修复未登录访问管理端深链后登录成功回跳原站内路径；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定和本场景排障步骤。排查菜单页面不可见或 404 时仍按当前实际落地页面、菜单授权和页面注册链路定位。

- Issue #259 新增后台品牌配置页面 `system/admin-branding/index`、菜单和语义化接口，仅影响后台 Logo、系统标题、版权、favicon、登录页背景等品牌展示配置；不改变菜单 `component` key 解析、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。若后台品牌配置页面出现 404 或 403，按菜单资源同步、角色授权、页面 key 注册和接口权限 `system:admin-branding:query`、`system:admin-branding:edit` 顺序排查。
- Issue #354 为 Resource Registry 增加资源类型依赖排序，仅改变同一同步批次内 handler 执行顺序，例如身份用户、角色、组织资源先于其绑定关系同步；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。清库重建时仍按资源声明同步日志、目标 handler 消费结果、角色授权和页面 key 注册顺序排查。

- Issue #368 新增用户多首页工作台能力，固定首页菜单仍使用 `/home` 和原有 `home` component key；新增 `/home/:homeId` 是隐藏运行时路由，复用同一个首页宿主并由 `mango-home` 校验当前用户是否拥有该首页。不改变菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合和租户绑定。若指定首页路由出现 404，除原排障步骤外，额外确认 admin app 是否注册隐藏路由 `/home/:homeId`，以及后端是否启用 `mango-home-starter` 和 `home` Flyway migration。

- Issue #372 新增 `平台能力 / 首页管理` 菜单目录和 `home/templates/index`、`home/list/index`、`home/user/index` 页面注册，用于首页模板、首页列表和用户首页管理；不改变菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合和租户绑定。排查首页管理菜单不可见、页面 404 或 403 时，除本指南原闭环外，额外确认 `mango-home-starter` 的 `AUTH_MENU` 资源已同步、角色已授权首页管理菜单/按钮资源、前端已注册三个页面 key。

- Issue #322 仅放宽 Mango 前端包在当前已认证主版本内的 `peerDependencies` 范围，并明确 `pinia@3`、`vue-i18n@10+`、`vue-router@5` 暂未纳入当前认证范围；不改变菜单树接口、页面 `component` key、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。业务项目安装依赖时如出现 peer warning，应先按 `mango-ui/README.md` 的认证范围对齐前端包批次，再回到本指南排查菜单、页面和权限链路。

- v2026.07.04-maven-1.0.8-platform-release 仅发布首页管理后续 UI/API、通知动作和文件能力的版本批次；不新增菜单 component key 解析规则，不改变菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。首页管理页面仍按 Issue #372 的菜单资源同步、角色授权和前端页面 key 注册链路排查。

- Issue #396 仅治理 system 日志、行政区划和 authorization 权限码 API 契约承载位置：API Bean 改由 starter controller 承载，service/core 不再实现 API；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定、登录态权限聚合、页面路由和本场景排障步骤。

- PR #414 收口首页工作台小组件权限展示：工作流首页卡片会同时校验权限码和目标页面入口，缺少任一项时在卡片内显示“缺少权限”并禁用交互；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定、登录态权限聚合、页面路由和本场景排障步骤。若首页卡片缺少页面入口，按菜单资源同步、角色授权和前端页面 key 注册链路排查，不再通过点击卡片进入 404 页面定位。

- v2026.07.07-maven-1.0.13-menu-api-codes-release 仅发布 menuCode/apiCodes 权限模型的 Maven、npm 和 CLI 版本批次；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定、登录态权限聚合、页面路由和本场景排障步骤。业务项目升级时按发布说明成组升级后端 `<mango.version>`、前端 `@mango/*` 包和 `@mango/cli`。

- 本次 PR 仅在开发中心组件库新增 `MangoSearchPanel` 搜索面板示例入口和示例页面；不改变菜单 `component` key 解析、菜单树接口、业务页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户应用绑定、页面路由和本场景排障步骤。

## search-panel-layout-enhance 影响记录

- 本次仅增强开发中心搜索面板示例和 `@mango/common` 搜索面板固定列布局能力，不改变菜单 `component` key、菜单树接口、页面注册方式、角色菜单授权关系、租户绑定、页面路由和本场景排障步骤。

- 本次 PR 仅调整 `@mango/common` `MangoSearchPanel` 为无外壳视觉边界，并补充开发中心极简示例；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户应用绑定、页面路由和本场景排障步骤。

- v2026.07.09-common-search-panel-form-layout-release 仅调整 `MangoSearchPanel` 的表单尺寸、label 展示、默认列数、折叠行数和展开控件布局；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、租户绑定、页面路由和本场景排障步骤。

- v2026.07.11-npm-lock-sync-release 仅同步 `@mango/*` npm 发布批次、CLI/starter 版本锁和包消费者验证；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、租户绑定、页面路由和本场景排障步骤。业务项目应将相关前端包与 `@mango/cli` 成组升级。

- Resource 历史债务治理仅将进程内 Provider/Handler/Dispatcher 从 `mango-resource-api` 迁到 `mango-resource-support`，不改变菜单资源类型、`component` key、角色菜单关系、权限聚合、租户绑定、HTTP 接口和本场景排障步骤。

- File Preview 历史债务治理补齐文件下载权限资源；Resource target 保持既有 `/resource/targets` 路由且不新增重复模块元数据。不改变菜单 `component` key、菜单树接口、角色菜单关系、权限聚合、租户绑定、页面路由和本场景排障步骤。

- Issue #553 修复通知铃铛把 `menuCode` 当作 Vue 命名路由的问题，并让 `mango-admin` 装载 Notice admin-pages 声明的隐藏接收设置路由。业务页面仍按既有 `component` key 注册；仅“我的消息/接收设置”入口异常时，额外确认内置 `ROLE_LOGIN`、Notice 菜单资源同步和 `registerMangoNoticeAdminPages()`，其它菜单页面继续按本指南原闭环排查。

- Issue #575 将用户端“接收配置”从“通知中心”归入“消息中心”，并把用户端“公告”更名为“系统公告”。接收配置的规范路由调整为 `/message-center/receive-setting`，`notice/receive-setting/index` component key 和 `notice:receive-setting` menuCode 保持不变；旧 `/notice/receive-setting` 仅作为隐藏兼容路由。若升级后仍看到旧菜单层级或点击进入 404，依次确认 Notice 菜单资源已同步到第 3 版、前端已注册规范路由，以及业务侧没有缓存旧菜单数据。

## 2026-07-19 前端规范候选影响

- 本次前端规范候选统一公开包合同、显式样式入口、Host 请求客户端注入和单体/微前端运行时边界；不改变菜单 `component` key、菜单树接口、页面注册结果、角色授权关系、登录态权限聚合、租户绑定和本场景排障步骤。业务项目只有主动升级完整前端包矩阵时才需要重新执行菜单、页面和权限冒烟验证。

- 本次 PR 仅在开发中心 Editor 示例中展示 `@mango/common` 富文本工具栏精简配置和图片值写入类型；不改变菜单 `component` key、菜单树接口、页面注册结果、角色授权关系、登录态权限聚合、租户绑定和本场景排障步骤。

## 2026-07-22 Resource Registry 启动可靠性影响

- Issues #620/#621 不改变菜单 `component` key、菜单树接口、角色菜单关系、按钮权限码或前端页面注册方式。空库启动时 readiness 会在 Resource Registry 和租户对账均完成后才转为 UP；若菜单资源未形成，应先查看 Health 中的同步状态和脱敏失败类型，再核对对应 `AUTH_MENU`、`API_RESOURCE` 声明的租户与业务键，避免把初始化未完成误判为页面注册问题。
