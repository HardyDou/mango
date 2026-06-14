# 日志 Log

## 1. 能力定位

提供 Mango 日志配置和日志自动配置入口。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

需要统一后端日志开关、格式或扩展日志上下文时使用。

## 3. 不适用场景

不负责业务审计表、操作日志页面和日志采集平台部署。

## 4. 模块边界

单模块 starter，提供 `LogAutoConfiguration` 和 `LogProperties`。

## 5. 接入方式

后端引入 `mango-infra-log-starter`。

## 6. 配置项

`mango.log` 控制日志能力。

## 7. 对外接口 / 扩展点

`LogAutoConfiguration`；未发现 HTTP Controller 或业务 API。

## 8. 数据库 / 初始化数据

无数据库和初始化数据。

## 9. 菜单 / 权限 / 租户

无菜单和权限；日志内容不记录敏感凭据。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-log -am test
```

## 11. 业务接入最小闭环

业务启用后通过日志配置输出请求或业务上下文，验证日志中有必要上下文且无密码、token。

## 12. 常见问题

日志缺上下文时检查 context、web filter 和日志配置。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
