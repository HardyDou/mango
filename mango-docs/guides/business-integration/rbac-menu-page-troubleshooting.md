# 菜单页面打不开排障

## 1. 适用场景

用户登录后能看到菜单，但点击菜单出现空白页、404、组件加载失败或接口无权限。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 菜单、权限、资源同步、授权关系 |
| 2 | [@mango/rbac README](../../../mango-ui/packages/rbac/README.md) | RBAC 前端包和 API |
| 3 | [RBAC Views README](../../../mango-ui/packages/rbac/src/views/README.md) | 页面 key 和组件映射 |
| 4 | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md) | 页面注册、菜单渲染、登录后装配 |

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 菜单数据 | `/authorization/menus/user?fmt=tree&appCode=internal-admin` 返回目标菜单 |
| 页面 key | 菜单 `component` 字段能匹配前端页面 key |
| 前端注册 | 前端包已引入并完成页面注册或路由映射 |
| 角色授权 | 当前用户角色已绑定目标菜单 |
| 租户绑定 | 当前租户已绑定目标应用和菜单包 |
| 运行态请求 | 浏览器 network 中页面依赖和业务接口没有未解释的 401/403/404 |

## 4. 最小闭环

1. 用目标用户登录。
2. 打开菜单接口，确认返回目标菜单和 component key。
3. 在前端页面 key 文档中确认 component key 存在。
4. 点击菜单，页面组件正常加载。
5. 浏览器 network 中页面资源、菜单接口和业务接口没有未解释的 401/403/404。

## 5. 页面 key 对照

| 能力 | 常见页面 key 文档 |
|------|------------------|
| Auth | [Auth Views README](../../../mango-ui/packages/auth/src/views/README.md) |
| File | [File Components README](../../../mango-ui/packages/file/src/components/README.md) |
| Job | [Job Views README](../../../mango-ui/packages/job/src/views/README.md) |
| RBAC | [RBAC Views README](../../../mango-ui/packages/rbac/src/views/README.md) |
| System | [System Components README](../../../mango-ui/packages/system/src/components/README.md) |
| Workflow | [Workflow Components README](../../../mango-ui/packages/workflow/src/components/README.md) |

## 6. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 菜单存在但点击空白 | component key 与前端注册表不一致 |
| 菜单不存在 | resource manifest、迁移 SQL、角色授权和租户应用绑定 |
| 页面加载但接口 403 | Access、Authorization 和当前用户权限集合 |
| 刷新后页面丢失 | 前端路由 fallback、admin-shell 注册时机 |
| 只有某租户异常 | 租户应用绑定、菜单包绑定、租户初始化数据 |

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

- PR #183 只治理后端测试规范、Mockito 审计和授权资源处理器测试样板；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- v2026.06.27-workflow-history-dialog-release 同步发布工作流 UI 修复批次和 `@mango/admin@1.0.33`、`@mango/cli@1.0.46` 版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

- v2026.06.27-admin-shell-menu-redirect-release 发布 `@mango/admin-shell@1.0.28`、`@mango/admin@1.0.32` 和 `@mango/cli@1.0.45`，让业务项目可通过 npm 包消费 Issue #274 的目录菜单 redirect 修复；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定和本场景排障步骤。业务项目如仍复现顶层目录跳到无权限 redirect 页面，应先确认前端依赖已升级到本发布批次。

- Issue #274 修复 `@mango/admin-shell` 目录型菜单 redirect 解析：目录菜单的 `redirect` 只有命中当前用户可见且可运行的菜单时才生效，否则会进入当前可见菜单树中的第一个可运行子页面；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定和本场景排障步骤。排查“点击顶层目录进入无权限页面”时，应同时确认当前用户可见菜单树中是否包含 redirect 目标以及是否存在可运行子页面。

- v2026.06.27-system-component-release 同步发布 `@mango/system@1.0.11` 及其前端依赖批次，仅对齐 npm 物料和 CLI/starter 版本锁；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、租户应用绑定、页面路由和本场景排障步骤。业务项目排查页面加载异常时，仍先确认前端包批次一致、页面 key 已注册、后端菜单资源已同步。

- PR #267 将通知公告能力拆分为管理端 `通知中心` 和用户端 `消息中心`：管理端 `通知中心` 下包含公告管理、消息配置、发送任务、渠道配置、发送记录、失败重试，用户端 `消息中心` 下包含我的消息、公告，`接收设置` 保留为隐藏辅助路由。排查通知相关菜单时，需要确认后端菜单资源、当前用户角色授权、`component` key 和前端 `@mango/notice` 页面注册是否匹配；这次不改变菜单树接口、页面注册机制、角色授权关系、登录态权限聚合、租户绑定和通用排障步骤。

- PR #256 将后台工作台默认布局调整为页面内固定配置，仅影响没有个人工作台配置或恢复默认后的首页卡片排布；不改变菜单 `component` key、菜单树接口、页面注册方式、角色授权关系、按钮权限关系、登录态权限聚合、租户绑定、页面路由和本场景排障步骤。

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
