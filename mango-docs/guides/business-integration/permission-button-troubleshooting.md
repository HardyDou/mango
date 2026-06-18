# 按钮权限不显示排障

## 1. 适用场景

用户能打开页面，但新增、编辑、删除、导出、审批等按钮不显示或点击后返回无权限。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Access 后端 README](../../../mango/mango-platform/mango-access/README.md) | 接口权限、上下文、拦截边界 |
| 2 | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 权限资源、角色授权、菜单关系 |
| 3 | [@mango/rbac README](../../../mango-ui/packages/rbac/README.md) | 前端授权数据和管理页面 |
| 4 | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md) | 登录后权限集合和按钮指令 |

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 按钮资源 | 后端资源中存在按钮 permissionCode |
| 菜单关系 | 菜单资源与按钮权限存在父子关系 |
| 角色授权 | 角色管理「分配权限」弹框能看到按钮节点，且角色已绑定对应按钮权限 |
| 用户上下文 | 当前用户拥有该角色，且角色在当前租户和组织上下文内生效 |
| 登录态权限 | 登录后权限集合包含目标 permissionCode |
| 按钮展示规则 | 登录后 `buttonRules` 中目标按钮的 `displayRule` 执行结果为显示 |
| 前端判断 | 前端按钮使用的权限码与后端资源一致 |
| 接口校验 | 接口层权限校验与前端按钮权限码一致或有清晰映射 |

## 4. 最小闭环

1. 给测试角色授权目标菜单和按钮。
2. 用测试用户重新登录。
3. 打开页面确认按钮出现。
4. 点击按钮并确认接口返回业务成功或明确的业务校验错误。
5. 取消按钮授权后重新登录，按钮不可见或接口返回无权限。

## 5. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 按钮完全不显示 | 前端 `v-auth` 或权限判断使用的 permissionCode |
| 按钮显示但接口 403 | 接口权限注解、Access 上下文、后端授权集合 |
| 管理员可见普通用户不可见 | 角色授权、用户角色绑定、组织/岗位限制 |
| 菜单可见按钮不可见 | 菜单授权和按钮授权是否分开配置 |
| 有按钮权限但按钮仍隐藏 | 按钮展示规则、`v-auth` 对象写法和页面传入的 `row` / `pageState` / `query` / `selectedRows` |
| 角色授权弹框看不到按钮 | 后端可分配菜单树是否返回按钮节点、按钮资源是否挂在页面菜单下 |
| 重新授权后仍不生效 | 登录态权限缓存、前端刷新、token 重新获取 |

## 6. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-access,mango-platform/mango-authorization -am test
pnpm -F @mango/rbac build
pnpm -F @mango/admin-shell build
```

模块验证入口：

- [Access 验证方式](../../../mango/mango-platform/mango-access/README.md#10-验证方式)
- [Authorization 验证方式](../../../mango/mango-platform/mango-authorization/README.md#10-验证方式)
- [RBAC Frontend 验证方式](../../../mango-ui/packages/rbac/README.md#10-验证方式)
- [Admin Shell 验证方式](../../../mango-ui/packages/admin-shell/README.md#10-验证方式)

## 7. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 编码红线](../../../mango-pmo/rules/03-ai-coding-redlines.md)

## 8. 变更影响记录

- PR #181 将授权数据权限提供者改为由 `AuthorizationAutoConfiguration` 显式注册，修复业务同时引入授权与持久化 starter 时 `DataScopeApplier` 未创建导致的启动失败；不改变按钮 permissionCode、登录态权限集合、角色按钮授权关系、接口鉴权、租户边界、授权 API、配置项、页面和本场景排障步骤。业务只需要刷新后端 Maven `1.0.0-SNAPSHOT` 依赖，不需要升级 npm 包。
- PR #179 修复持久化 starter 与授权 starter 组合使用时 `DataScopeApplier` 自动配置顺序，并补齐授权 core 服务泛型契约；不改变按钮 permissionCode、登录态权限集合、角色按钮授权关系、接口鉴权、租户边界、授权 API、配置、页面、启动方式和本场景排障步骤。
- PR #176 新增按钮展示规则配置，按钮可能在拥有权限后继续按 `displayRule` 判断显隐；不改变按钮权限码、角色按钮授权关系、接口鉴权和租户边界。排查“有权限但不可见”时，需要同时确认登录态 `buttonRules`、按钮 `menuCode` 与 `v-auth` 的 `code` 是否一致。
- PR #171 新增角色数据权限配置入口，数据权限只影响接入 `DataScopeApplier` 的业务查询范围；不改变按钮 permissionCode、登录态权限集合、角色按钮授权关系、接口鉴权和租户边界。
- PR #170 新增 `@mango/common` 的 `MangoDialog` 并在应用管理页面替换新增/编辑弹框外壳，只影响弹框布局和内容滚动体验；不改变按钮权限码、登录态权限集合、角色授权关系、接口鉴权、租户边界和按钮权限排障路径。
- PR #169 角色授权弹框改为展示后端可分配菜单树中的按钮节点，业务排障时可直接在角色管理中确认按钮权限是否可被勾选；不改变按钮权限码、登录态权限集合、接口鉴权和租户边界。
- PR #166 工作台自定义布局新增 `@mango/grid-layout` 和 `mango-grid-layout`，布局组件不接管按钮权限判断；不改变按钮权限的公开 API、配置、权限码、租户、页面和运行时行为。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变按钮权限的公开 API、配置、权限、租户、页面和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变按钮 permissionCode、登录态权限集合、角色按钮授权关系、接口鉴权、租户边界、页面和本场景排障步骤。
