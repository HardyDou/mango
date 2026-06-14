# Mango Infra Doc

## 1. 能力定位

`mango-infra-doc` 提供 OpenAPI、Swagger UI、Knife4j 和模块分组文档能力，面向开发、联调和接口说明生成。

## 2. 适用场景

- Spring Boot 应用需要暴露 OpenAPI 文档。
- 需要按 Mango module 分组生成 API 文档。
- 需要标记内部接口范围，辅助联调识别接口暴露边界。

## 3. 不适用场景

- 不属于核心生产运行时能力。
- 不负责接口权限控制、资源同步或网关聚合实现。
- 不替代正式 API 契约、验收文档和安全评审。

## 4. 模块边界

本模块只负责文档生成和 OpenAPI 分组。接口访问策略归属 `mango-authorization`，运行时访问控制归属 auth/access/security 相关模块。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.infra.doc</groupId>
    <artifactId>mango-infra-doc-starter</artifactId>
</dependency>
```

自动配置入口为 `DocAutoConfiguration`。

## 6. 配置项

配置前缀：`mango.doc`。

已发现字段包括 `enabled`、`title`、`description`、`version`、`group`、`pathsToMatch`、`moduleGrouping`、`contact`、`license`。Spring Boot 配置使用 kebab-case，例如 `mango.doc.module-grouping.enabled`、`mango.doc.module-grouping.include-default-group`、`mango.doc.contact.name`。

## 7. 对外接口 / 扩展点

- `MangoApiScopeOperationCustomizer`
- `ModuleGroupedOpenApiRegistrar`
- 自动注册 `OpenAPI` 和 `GroupedOpenApi`

未发现本模块定义业务 Controller 或 Feign Client。

## 8. 数据库 / 初始化数据

未发现数据库 migration 或初始化数据。

## 9. 菜单 / 权限 / 租户

本模块不提供菜单、权限资源或租户数据。文档访问权限由宿主应用的安全配置控制。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-doc -am test
```

代表性测试入口：`MangoApiScopeOperationCustomizerTest`。模块分组验收应准备 `META-INF/mango/module.properties`，确认 `ModuleGroupedOpenApiRegistrar` 注册 GroupedOpenApi、pathsToMatch 和 scope extension 生效。

## 11. 业务接入最小闭环

业务应用接入 `mango-infra-doc-starter` 后，在开发和联调环境开启 `mango.doc.enabled=true`，生产环境按安全策略关闭或限制访问。最小配置包括 title、version、paths-to-match、module-grouping 和 contact。

验收时访问宿主应用的 OpenAPI / Swagger / Knife4j 入口，确认默认分组和模块分组都能展示接口；内部接口 scope 只作为文档标记，不作为权限控制依据。接口权限仍通过 authorization/access 验证。

## 12. 常见问题

- 文档未分组时检查 `META-INF/mango/module.properties` 和 `mango.doc.module-grouping`。
- Swagger UI 不应作为生产接口权限依据。
- 内部接口标记只影响文档呈现，不等同于安全拦截。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
