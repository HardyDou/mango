# 初始化种子 Seed

## 1. 能力定位

提供 Mango 官方基础初始化数据装载入口。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务或平台应用需要初始化基础租户、管理员、角色和应用绑定时使用。

## 3. 不适用场景

不替代 Flyway migration，不负责运行期业务数据同步，也不提供业务模块自定义资源扫描机制。

## 4. 模块边界

包含 `mango-seed-starter`，负责按配置写入 Mango 官方基础数据。

## 5. 接入方式

后端引入 `mango-seed-starter`。

## 6. 配置项

`mango.seed.enabled` 控制是否启用；`mango.seed.admin.initial-password` 提供初始管理员密码。`MangoSeedProperties` 读取种子配置。

## 7. 对外接口 / 扩展点

`MangoSeedAutoConfiguration` 提供自动配置入口；未发现 HTTP Controller。

## 8. 数据库 / 初始化数据

不直接拥有业务表；当前种子数据写入 `sys_tenant`、`identity_user`、`tenant_member`、`authorization_role`、`frontend_tenant_app_binding` 等基础表。

## 9. 菜单 / 权限 / 租户

种子数据包含基础租户、管理员、角色和应用绑定，表归属以目标模块声明为准。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-seed -am test
```

## 11. 业务接入最小闭环

业务宿主启用 seed 后启动应用，并检查基础租户、管理员、角色和应用绑定是否写入。业务模块自己的初始化数据仍通过对应模块 migration 或明确的模块初始化能力处理。

## 12. 常见问题

种子未执行时检查 `mango.seed.enabled`、`mango.seed.admin.initial-password`、目标模块 migration 顺序和目标表写入日志。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
