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
