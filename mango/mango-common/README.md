# Mango Common

## 1. 能力定位

提供 Mango 后端最低层稳定公共契约。

主要使用者：Mango 维护者、Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

跨模块需要统一返回、错误码、业务异常、分页和轻量断言时使用。

## 3. 不适用场景

不放业务模型、技术实现、运行时上下文和地区规则校验。

## 4. 模块边界

公共层无 Spring runtime 依赖；技术扩展放到 infra，平台业务事实放到 platform。

## 5. 接入方式

后端模块按需依赖 `mango-common`。

## 6. 配置项

无配置项。

## 7. 对外接口 / 扩展点

`R<T>`、`BizCode`、`CommonCode`、`BizException`、`PageQuery`、`PageResult<T>`、`Require`。

## 8. 数据库 / 初始化数据

无数据库和初始化数据。

## 9. 菜单 / 权限 / 租户

无菜单、权限和租户能力；只承载公共契约。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-common -am test
```

## 11. 业务接入最小闭环

业务或平台模块引用 common 契约，错误响应、分页响应和断言异常保持统一。

## 12. 常见问题

新增 common 类前先确认不会引入业务语义或技术实现依赖。

## 13. 关联 PMO 规则

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../mango-docs/capabilities/README.md)
