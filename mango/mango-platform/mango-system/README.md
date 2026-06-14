# 系统 System

## 1. 能力定位

提供系统参数、字典、租户、地区、日志和个人配置等基础管理能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务应用需要统一系统配置、字典、租户管理和系统级查询时使用。

## 3. 不适用场景

不负责业务主数据、认证登录页面和具体业务权限策略设计。

## 4. 模块边界

包含 api/core/starter；对外提供系统基础 API 和管理 Controller。

## 5. 接入方式

后端引入 `mango-system-starter`。HTTP 入口包括 `/system/dict`、`/system/tenant`、`/system/config`、`/system/log`、`/system/area`、`/system/i18n`、`/system/personal-configs` 等。

## 6. 配置项

随 starter 自动配置；具体参数数据来自系统参数表。

## 7. 对外接口 / 扩展点

`DictApi`、`SysTenantApi`、`PersonalConfigApi` 及系统管理 Controller。`/system/events` 归属 `mango-infra-event`。

## 8. 数据库 / 初始化数据

system core migration 管理字典、参数、租户、地区、日志等系统表和初始化数据；事件相关入口归属 `mango-infra-event`。

## 9. 菜单 / 权限 / 租户

接口使用 `system:*` 权限码；租户、字典和个人配置按租户或用户上下文隔离。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-system -am test
```

## 11. 业务接入最小闭环

业务接入后先验证租户、字典、参数、地区、日志和个人配置读取，再在前端通过 system 包展示或选择，最后检查权限码拦截。

## 12. 常见问题

字典或租户数据异常时检查 system migration、租户上下文和授权资源同步。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
