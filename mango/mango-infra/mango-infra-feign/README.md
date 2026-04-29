# mango-infra-feign

> OpenFeign RPC 基础设施 - 服务间声明式调用

## 已实现

- **OpenFeign 自动配置** - 超时、重试、日志、请求拦截器
- **FeignProperties** - `mango.feign.*` 配置属性
- **FeignRequestInterceptor** - 透传 MangoContext 与 Authorization token
- **Retryer** - 可配置重试机制

## 依赖

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-feign-starter</artifactId>
</dependency>
```

## 配置

```yaml
mango:
  feign:
    enabled: true
    connect-timeout: 5000      # 连接超时 (ms)
    read-timeout: 10000       # 读取超时 (ms)
    retry: 3                   # 重试次数
    logger-level: BASIC       # 日志级别: NONE/BASIC/HEADERS/FULL
    interceptor-enabled: true # 启用请求拦截器
```

## 使用方式

### Feign 声明式调用

```java
@FeignClient(name = "user-service", path = "/user")
public interface UserFeignClient {

    @GetMapping("/{id}")
    R<User> getById(@PathVariable Long id);

    @GetMapping("/by-username/{username}")
    R<User> getByUsername(@PathVariable String username);
}
```

### Fallback 降级

```java
@Component
public class UserFeignClientFallback implements UserFeignClient {
    @Override
    public R<User> getById(Long id) {
        return R.fail("User service unavailable");
    }
}

@FeignClient(name = "user-service", path = "/user", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    // ...
}
```

## 设计决策

- OpenFeign 是服务间 RPC 的主要方式，注解驱动，类型安全
- 重试机制基于 Retryer.Default，支持指数退避
- 请求拦截器用于传递 MangoContext 请求头（`X-Mango-*`）与 `Authorization`
- 日志级别默认 BASIC，生产环境按需调整为 FULL
