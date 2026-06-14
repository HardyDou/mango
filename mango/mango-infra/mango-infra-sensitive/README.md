# 敏感数据 Sensitive

## 1. 能力定位

提供敏感字段脱敏和敏感处理扩展能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

接口响应、日志或导出数据需要统一脱敏时使用。

## 3. 不适用场景

不替代数据权限、加密存储和密钥管理。

## 4. 模块边界

包含 api/core/starter，提供脱敏注解、策略和自动配置。

## 5. 接入方式

后端引入 `mango-infra-sensitive-starter`。

## 6. 配置项

`SensitiveProperties.PREFIX` 对应敏感处理配置前缀。

## 7. 对外接口 / 扩展点

敏感处理 API、策略和 `SensitiveAutoConfiguration`。

## 8. 数据库 / 初始化数据

无数据库和初始化数据。

## 9. 菜单 / 权限 / 租户

脱敏是展示层保护，不改变权限判断；调用方仍需做权限和租户校验。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-sensitive -am test
```

## 11. 业务接入最小闭环

业务在响应 DTO 或处理链上声明脱敏策略，调用接口断言手机号、证件号等字段按预期遮蔽。

## 12. 常见问题

未脱敏时检查 starter 是否引入、注解位置和序列化链路。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
