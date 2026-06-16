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

- PR #166 工作台自定义布局新增 `@mango/grid-layout` 和 `mango-grid-layout`，仅保存当前登录人的工作台布局 JSON；不改变菜单页面 component key、资源授权、页面注册、权限、租户和菜单运行时行为。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变菜单页面、资源授权、页面 key、权限、租户和运行时行为。
