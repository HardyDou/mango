# Mango Infra Web

## 1. 能力定位

`mango-infra-web` 提供 HTTP API 契约和 Spring Web 启动器，覆盖请求上下文、内部调用标记、MDC、CORS、全局异常处理和内部路径扫描。

## 2. 适用场景

- 业务 API 需要声明内部接口。
- Web 应用需要把请求头转换为 Mango 上下文。
- 需要全局异常处理、trace id、MDC 和 CORS 基础配置。
- 需要扫描 `@Inner` 接口并提供内部路径集合。

## 3. 不适用场景

- 不负责登录、JWT 解析或 token 签发。
- 不负责角色权限判断和 API 资源策略。
- 不负责网关路由和服务间 Feign 调用。
- 不替代业务 Controller 的参数校验和领域异常建模。

## 4. 模块边界

`mango-infra-web-api` 提供轻量契约，`mango-infra-web-starter` 提供 Spring Web 过滤器、异常处理、上下文 Provider 和扫描器。认证、授权和访问控制由 auth、authorization、access 模块负责。

## 5. 接入方式

声明内部接口或使用 Web 契约：

```xml
<dependency>
    <groupId>io.mango.infra.web</groupId>
    <artifactId>mango-infra-web-api</artifactId>
</dependency>
```

Web 应用接入 starter：

```xml
<dependency>
    <groupId>io.mango.infra.web</groupId>
    <artifactId>mango-infra-web-starter</artifactId>
</dependency>
```

## 6. 配置项

配置前缀：`mango.web`。

已发现配置分组包括 `cors`、`inner`、`mdc`、`requestContext`，来源 `MangoWebProperties`。自动配置还使用 `mango.web.context.enabled` 控制 `MangoContextWebFilter`，`mango.web.request-context.enabled` 只控制 `IRequestContextProvider` 装配。

## 7. 对外接口 / 扩展点

- 注解：`@Inner`
- API：`IRequestContextProvider`、`IInternalPathProvider`、`RequestContextSnapshot`
- 过滤器：`InternalCallFilter`、`MangoContextWebFilter`、`WebMdcFilter`
- 支撑组件：`GlobalExceptionHandler`、`ServletRequestContextProvider`、`InnerMappingScanner`、`AggregatingInternalPathProvider`、`InnerMappingInternalPathProvider`、`WebTraceIdResolver`

## 8. 数据库 / 初始化数据

未发现数据库 migration 或初始化数据。

## 9. 菜单 / 权限 / 租户

本模块不提供菜单或权限资源。请求上下文中可包含租户、用户和 trace 信息，但权限解释由上层模块负责。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-web -am test
```

测试入口位于 `mango-infra-web-starter/src/test/java/io/mango/infra/web/**`。

## 11. 业务接入最小闭环

Web 应用接入 starter 后，入口请求头会被转换为 Mango 请求上下文；需要声明内部接口时在 Controller 或 API 方法上使用 `@Inner`。CORS、MDC、request-context、context 和 inner 按环境配置开启或关闭。

内部调用保护需要配置 `mango.web.inner.secret`，并依赖 infra-kv 存储 nonce 防重放。验收断言覆盖：请求上下文可读取用户/租户/trace，`@Inner` 路径被扫描到，缺失或错误内部签名会被拒绝，trace id 在日志中连续。

## 12. 常见问题

- 下游拿不到上下文时检查入口是否经过 `MangoContextWebFilter`。
- 内部接口未识别时检查 `@Inner` 注解位置和 `InnerMappingScanner` 扫描结果。
- 内部调用保护不生效时检查 infra-kv、`mango.web.inner.secret` 和 nonce 配置。
- trace id 不连续时检查网关、Feign 和 Web MDC 配置是否一致。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
