# Mango Extension

## 1. 能力定位

承载 Mango 可选扩展能力。

主要使用者：Mango 维护者、Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

AI、第三方能力或非核心平台能力需要独立扩展时使用。

## 3. 不适用场景

不放核心平台必需能力和业务项目私有逻辑。

## 4. 模块边界

当前包含 `mango-ai` 扩展；扩展能力按 api/core/starter 分层。

## 5. 接入方式

按需依赖具体扩展 starter，例如 `mango-ai-starter`。

## 6. 配置项

配置由具体扩展模块声明。

## 7. 对外接口 / 扩展点

扩展 API 由具体子模块提供。

## 8. 数据库 / 初始化数据

数据库和初始化数据由具体扩展模块声明。

## 9. 菜单 / 权限 / 租户

菜单、权限、租户由具体扩展模块声明。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-extension -am test
```

## 11. 业务接入最小闭环

业务确认需要扩展能力后只引入对应 starter，配置真实 provider，并验证真实接口返回。

## 12. 常见问题

不要把未稳定的核心能力临时放到 extension 规避模块边界。

## 13. 关联 PMO 规则

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../mango-docs/capabilities/README.md)
