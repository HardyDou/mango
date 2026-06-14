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
| 角色授权 | 角色已绑定对应按钮权限 |
| 用户上下文 | 当前用户拥有该角色，且角色在当前租户和组织上下文内生效 |
| 登录态权限 | 登录后权限集合包含目标 permissionCode |
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

- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变按钮权限的公开 API、配置、权限、租户、页面和运行时行为。
