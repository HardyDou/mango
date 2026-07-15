# Mango Infra Doc

## 1. 概览
`mango-infra-doc` 提供 Mango 后端 OpenAPI 文档能力，负责生成基础 OpenAPI 信息、Authorization 调试头、默认 API 分组，以及基于模块元数据的 Swagger / Knife4j 分组。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 后端应用需要在开发、联调、测试环境暴露 OpenAPI 文档 | Maven 依赖 / starter / Java API |
| 需要按 Mango 模块拆分接口分组，便于业务开发者查找模块 API | Maven 依赖 / starter / Java API |
| 需要在 OpenAPI 中标记接口范围，区分内部接口和外部接口 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不负责生产接口授权和 API 访问控制。
- 不负责菜单、权限资源同步。
- 不替代正式接口契约评审、验收用例和安全评审。

## 4. 模块入口
本模块只生成接口文档。接口是否允许访问由 `mango-authorization`、`mango-auth`、`mango-access`、网关和业务安全配置控制。

## 5. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.doc</groupId>
    <artifactId>mango-infra-doc-starter</artifactId>
</dependency>
```

自动配置入口为 `DocAutoConfiguration`。模块分组复用 `mango-infra-module-starter` 的运行时
`ModuleInfoRegistry`；各模块 jar 仍通过 `META-INF/mango/module.properties` 提供 classpath 默认值：

```properties
module-name=mango-payment
module-path=/payment
```

## 6. 配置说明
配置前缀：`mango.doc`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否启用文档自动配置。 |
| `title` | `Mango API` | OpenAPI 标题。 |
| `description` | `Mango Scaffold API Documentation` | OpenAPI 描述。 |
| `version` | `1.0.0` | OpenAPI 版本。 |
| `group` | `public-api` | 默认分组名称。 |
| `paths-to-match` | `/api/**` | 默认分组匹配路径。 |
| `module-grouping.enabled` | `true` | 是否按模块元数据生成分组。 |
| `module-grouping.include-default-group` | `true` | 是否保留默认全局分组。 |
| `module-grouping.include-scope-tags` | `true` | 是否为接口添加 scope tag 和 OpenAPI extension。 |
| `contact.name` | `Mango Team` | 文档联系人名称。 |
| `contact.email` | `mango@example.com` | 文档联系人邮箱。 |
| `license` | `Apache 2.0` | OpenAPI license 名称。 |

模块部署路径由 `mango.module.module-service.modules.<module-name>.module-path` 覆盖时，文档分组读取
registry 中的最终路径，不继续使用同名 classpath 旧路径。该行为与 Feign 路由、authorization 资源同步
共享同一运行时模块事实。

示例：

```yaml
mango:
  doc:
    enabled: true
    title: Mango Admin API
    version: 2026.06
    paths-to-match:
      - /api/**
      - /system/**
    module-grouping:
      enabled: true
      include-default-group: true
      include-scope-tags: true
```

## 7. API 与扩展
- `DocAutoConfiguration`：注册 `OpenAPI` 和默认 `GroupedOpenApi`。
- `ModuleGroupedOpenApiRegistrar`：扫描 classpath 元数据和显式模块配置，按 `module-name` 注册分组 Bean。
- `ModuleGroupedOpenApiFactoryBean`：分组实例化时读取 `ModuleInfoRegistry` 的最终模块路径；registry 不可用时回退到 classpath 路径。
- `MangoApiScopeOperationCustomizer`：为 OpenAPI operation 添加接口范围信息，并注册 Authorization 调试头。

模块分组规则：

- `module-path=/payment` 与 `module-path=/payment/` 都会归一为 `/payment`，匹配 `/payment` 和 `/payment/**`。
- `module-path=/` 会归一为根分组模式 `/**`，不会生成双斜杠路径。
- 多个路径可在 `module-path` 中用逗号分隔。
- `module-name` 重复时只注册第一个分组。

## 8. 数据与初始化
无数据库 migration、无 Runner、无 Initializer、无初始化数据。

## 9. 管理入口
本模块不创建菜单、权限和租户数据。文档接口暴露给谁，由宿主应用安全配置决定；生产环境如果开启文档，必须通过网关、安全过滤器或部署网络限制访问。

## 10. 快速开始
1. 应用接入 `mango-infra-doc-starter`。
2. 每个业务能力模块提供 `META-INF/mango/module.properties`，登记模块名和默认模块路径。
3. 部署路径与默认值不同时，通过 `mango.module.module-service` 配置实际路径；Doc、Feign 和 authorization 会读取同一 registry 结果。
4. 开发、联调环境开启 `mango.doc.enabled`，生产环境按安全策略关闭或限制访问。
5. 联调前确认模块分组能展示该模块 Controller，接口权限仍通过 authorization/access 验证。

## 11. 问题排查
- 看不到模块分组：检查 jar 内是否存在 `META-INF/mango/module.properties`，以及 `module-path` 是否为空。
- 分组仍显示旧路径：检查 `mango-infra-module-starter` 是否启用，以及 `mango.module.module-service.modules.<module-name>.module-path` 是否绑定到目标模块名。
- 只有默认分组：检查 `mango.doc.module-grouping.enabled` 是否关闭。
- 文档能调通不代表生产有权限：OpenAPI 只是调试入口，权限以运行时鉴权结果为准。

## 12. 验证入口

```bash
mvn -f mango/pom.xml \
  -pl :mango-infra-doc-starter \
  -am test -DskipTests=false \
  -Dtest='MangoApiScopeOperationCustomizerTest,DocPropertiesTest,ModuleGroupedOpenApiRegistrarTest,DocOpenApiFlowTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

`DocOpenApiFlowTest` 使用随机 Tomcat 端口和真实 `/v3/api-docs/{group}` HTTP 请求，验证当前 Module
registry 配置覆盖、尾斜杠与多路径归一、OpenAPI 信息、公开/内部 scope、tag 和 Authorization 安全要求。
消费者兼容验证把当前 Doc、Module 与 `mango-file-preview-app`、`mango-business-app` 分别放入同一 reactor，
避免旧本地 JAR 造成假结果。

## 13. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 补充资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
