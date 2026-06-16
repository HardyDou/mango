# Mango Infra Module

## 1. 概览
`mango-infra-module` 提供 Mango 模块运行时元数据注册和解析能力。它把 classpath 中的模块声明、配置文件中的服务映射统一注册为 `ModuleInfo`，供文档分组、Feign 模块路由、authorization 资源同步等能力判断“模块部署在哪个服务、访问根路径是什么”。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 平台需要按 module name 找到 service name、context path、module path | Maven 依赖 / starter / Java API |
| API 文档需要按模块分组 | Maven 依赖 / starter / Java API |
| Feign 调用或内部模块路由需要解析模块目标 | Maven 依赖 / starter / Java API |
| authorization 同步菜单、权限或 API 资源时，需要知道模块元数据来源 | Maven 依赖 / starter / Java API |

## 3. 适用场景
- 平台需要按 module name 找到 service name、context path、module path。
- API 文档需要按模块分组。
- Feign 调用或内部模块路由需要解析模块目标。
- authorization 同步菜单、权限或 API 资源时，需要知道模块元数据来源。

## 4. 边界说明
- 不实现业务模块功能。
- 不负责菜单和权限入库，只提供模块信息。
- 不负责服务发现和负载均衡；service name 只是给上层路由能力使用的目标名。

## 5. 模块组成
- `mango-infra-module-api`：`ModuleInfo`、`ModuleInfoRegistry`、`ModuleInfoResolver`。
- `mango-infra-module-core`：内存 registry。
- `mango-infra-module-starter`：扫描 `META-INF/mango/module.properties`，读取 `mango.module.module-service` 配置并注册模块。

## 6. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.module</groupId>
    <artifactId>mango-infra-module-starter</artifactId>
</dependency>
```

模块 jar 中声明元数据：

```properties
module-name=mango-payment
module-path=/payment
```

业务代码解析：

```java
ModuleInfo moduleInfo = moduleInfoResolver.resolve("mango-payment")
        .orElseThrow();
String runtimeBasePath = moduleInfo.runtimeBasePath();
```

## 7. 配置说明
配置前缀：`mango.module.module-service`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否启用模块信息自动配置。 |
| `modules.<module-name>.service-name` | `spring.application.name` 或 `application` | 模块所在服务名。 |
| `modules.<module-name>.context-path` | `server.servlet.context-path`、`spring.webflux.base-path` 或空 | 模块所在服务的上下文路径。 |
| `modules.<module-name>.module-path` | 由 module name 推导，如 `mango-payment` -> `/payment` | 模块业务根路径；支持逗号分隔多个路径。 |

示例：

```yaml
mango:
  module:
    module-service:
      enabled: true
      modules:
        mango-rbac:
          service-name: mango-admin-app
          context-path: /admin
          module-path: /rbac
        mango-system:
          service-name: mango-platform-app
          context-path: /
          module-path: /system,/system/config
```

## 8. API 与扩展
- `ModuleInfo`：字段为 module name、service name、context path、module path、source；`runtimeBasePath()` 返回 context path + module path。
- `ModuleInfoRegistry`：注册、按 module name 解析、列出全部模块；支持按 module path 或 request path 匹配。
- `ModuleInfoResolver`：轻量解析接口。
- `ModuleMetadataLoader`：扫描 classpath 下 `META-INF/mango/module.properties`。
- `MemoryModuleInfoRegistry`：默认内存实现。

注册来源：

- `source=classpath`：来自模块 jar 的 `META-INF/mango/module.properties`。
- `source=config`：来自 `mango.module.module-service.modules` 配置。

配置来源会覆盖同名模块的注册结果，适合微服务部署时把某个模块映射到实际服务。

## 9. 数据与初始化
无数据库 migration、无 Runner、无 Initializer。模块信息在应用启动时由自动配置扫描 classpath 和配置项后写入内存 registry。

## 10. 管理入口
本模块不创建菜单、权限和租户数据。authorization 可以消费模块信息和模块资源来同步菜单、权限、API 资源；真正的入库、幂等同步、角色授权由 authorization 模块负责。

## 11. 快速开始
1. 每个能力模块提供 `META-INF/mango/module.properties`，至少包含 `module-name` 和 `module-path`。
2. 单体部署可使用 classpath 默认值。
3. 微服务部署在应用配置中补充 `service-name`、`context-path`、`module-path`。
4. 在 Feign、doc 或 authorization 接入点用 `ModuleInfoResolver` 验证模块能被解析。

## 12. 问题排查
- 模块分组或资源同步找不到模块：检查 `META-INF/mango/module.properties` 是否打进 jar。
- 路径匹配不准：检查 `context-path` 和 `module-path` 是否重复拼接。
- 微服务目标错误：用配置覆盖 module service 映射，不要在业务调用处硬编码。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [能力地图](../../../mango-docs/capabilities/README.md)
