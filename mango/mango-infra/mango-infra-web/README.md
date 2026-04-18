# mango-infra-web

> Web 层基础设施 - Spring MVC、CORS、响应封装、请求上下文 Web 增强

## 已实现

- **CORS 跨域配置** - 允许所有来源/方法/头部，支持凭证
- **WebMvcConfigurer 集成** - Spring Boot 3.x 标准配置方式
- **Actuator 端点** - 健康检查等运维端点
- **RequestContextContributor** - 为请求型 infra 能力提供 `request` / `headers` / `cookies` 属性

## 配置属性

```yaml
mango:
  web:
    enabled: true
```

## 使用方式

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-web-starter</artifactId>
</dependency>
```

```yaml
# application.yml
mango:
  web:
    enabled: true
```

## 待实现

| 功能 | 状态 | 说明 |
|------|------|------|
| 全局异常处理 | 待开发 | @ControllerAdvice 统一异常响应 |
| 响应封装 | 待开发 | 统一 R<T> 响应格式 |
| 请求限流 | 待开发 | 基于 IP/用户名的限流 |
| 接口文档 | 待开发 | SpringDoc OpenAPI 集成 |
| 静态资源处理 | 待开发 | 自定义静态资源路径 |
| Validation 国际化 | 待开发 | 参数校验消息国际化 |

## 设计决策

- 使用 `spring-boot-starter-actuator` 提供运维端点
- CORS 配置开放所有来源，便于开发环境；生产环境建议通过网关统一处理
- 采用 `@AutoConfiguration` + `WebMvcConfigurer` 方式，不影响用户自定义配置
- Web 运行时变量增强由 `mango-infra-web` 提供，`mango-common` 只定义扩展协议，`mango-infra-kv-core` 不直接依赖 Servlet API
