# IP 归属地 Ip Location

## 1. 能力定位

提供 IP 地址归属地解析基础能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

登录日志、审计日志或安全风控需要展示 IP 归属地时使用。

## 3. 不适用场景

不负责风控规则、IP 黑白名单和第三方实时地理服务。

## 4. 模块边界

包含 api/core/starter，读取本地 IP 数据源并提供解析服务。

## 5. 接入方式

后端引入 `mango-infra-ip-location-starter`。

## 6. 配置项

`mango.ip-location` 控制数据文件和解析配置。

## 7. 对外接口 / 扩展点

IP location API 和 `IpLocationAutoConfiguration`。

## 8. 数据库 / 初始化数据

无数据库 migration；数据来自配置的数据文件或资源。

## 9. 菜单 / 权限 / 租户

不涉及菜单和权限；调用方负责审计数据归属。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-ip-location -am test
```

## 11. 业务接入最小闭环

业务在记录登录或审计日志时调用 IP 解析，把结果作为日志字段保存，并保留原始 IP。

## 12. 常见问题

归属地为空时检查 IP 数据文件、配置路径和内网地址处理。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
