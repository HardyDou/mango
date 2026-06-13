# Mango Parent

## 1. 能力定位

提供 Mango Maven 父 POM 和统一依赖版本管理。

主要使用者：Mango 维护者、Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

Mango 后端模块需要继承统一 Java、Spring、插件和依赖版本时使用。

## 3. 不适用场景

不提供运行时代码、业务能力和配置。

## 4. 模块边界

只管理 Maven 版本、插件和构建基线；子模块负责自己的依赖和能力说明。

## 5. 接入方式

后端模块在 pom 中继承或通过 reactor 使用 `mango-parent`。

## 6. 配置项

无运行时配置。

## 7. 对外接口 / 扩展点

Maven parent POM，无 Java API。

## 8. 数据库 / 初始化数据

无数据库和初始化数据。

## 9. 菜单 / 权限 / 租户

无菜单、权限和租户能力。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-parent -am validate
```

## 11. 业务接入最小闭环

新增后端模块时确认 parent 版本、插件和依赖管理生效，再运行模块测试。

## 12. 常见问题

改 parent 会影响全仓构建，提交前需要扩大 Maven 验证范围。

## 13. 关联 PMO 规则

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../mango-docs/capabilities/README.md)
