# 组织 Org

## 1. 能力定位

提供组织、岗位和组织树查询管理。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

用户归属、部门选择、岗位授权和组织树展示时使用。

## 3. 不适用场景

不负责认证登录和角色授权本身。

## 4. 模块边界

包含 API/core/starter/remote；组织 Controller 位于 core，岗位 Controller 位于 starter。

## 5. 接入方式

后端引入 `mango-org-starter`；远程调用引入 `mango-org-starter-remote`。HTTP 入口 `/org`、`/post`。

## 6. 配置项

未发现独立配置前缀；随 starter 自动配置 Mapper 和服务。

## 7. 对外接口 / 扩展点

`OrgApi`、`PostApi`、`OrgFeignClient`、`PostFeignClient`、`SysOrgController`、`PostController`。

## 8. 数据库 / 初始化数据

org core migration 管理组织、岗位及相关初始化数据。

## 9. 菜单 / 权限 / 租户

接口使用 `system:org:*`、`system:post:*` 权限码，数据按租户上下文隔离。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-org -am test
```

## 11. 业务接入最小闭环

业务先维护组织和岗位，再在用户、审批或数据权限中引用 org/post id，并校验当前租户只能查询本租户树。

## 12. 常见问题

组织树为空时检查租户上下文、组织初始化数据和用户组织绑定。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
