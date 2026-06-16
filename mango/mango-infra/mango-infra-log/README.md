# Mango Infra Log

## 1. 概览
`mango-infra-log` 提供 Mango 后端统一 Logback 配置、结构化日志输出、操作日志注解和日志属性绑定。它解决“日志输出到哪里、保留多久、是否 JSON、操作日志用什么 logger”这些基础问题。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 后端应用需要统一控制 root、Mango、Spring、MyBatis、HTTP 日志级别 | Maven 依赖 / starter / Java API |
| 需要按 profile 输出控制台、普通文件、错误文件、JSON 文件 | Maven 依赖 / starter / Java API |
| 需要用 @Log 标记业务方法的操作类型 | Maven 依赖 / starter / Java API |
| 需要日志中带 request id、trace id、client ip 等 MDC 字段 | Maven 依赖 / starter / Java API |

## 3. 适用场景
- 后端应用需要统一控制 root、Mango、Spring、MyBatis、HTTP 日志级别。
- 需要按 profile 输出控制台、普通文件、错误文件、JSON 文件。
- 需要用 `@Log` 标记业务方法的操作类型。
- 需要日志中带 request id、trace id、client ip 等 MDC 字段。

## 4. 边界说明
- 不负责操作日志落库、审计日志查询页面和日志采集平台部署。
- 不实现 `@Log` 的 AOP 采集和入库逻辑；当前模块只提供注解、枚举和 logger 配置。
- 不负责敏感字段脱敏，日志内容必须由调用方避免输出密码、token、密钥和完整证件号。

## 5. 模块组成
本模块是单 starter 模块，包含 `LogAutoConfiguration`、`LogProperties`、`@Log`、`LogType` 和 `logback-spring.xml`。Web 请求 MDC 写入由 `mango-infra-web` 的 `WebMdcFilter` 完成。

## 6. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.log</groupId>
    <artifactId>mango-infra-log-starter</artifactId>
</dependency>
```

标记操作：

```java
@Log(value = "用户登录", type = LogType.LOGIN)
public LoginResult login(LoginCommand command) {
    return authService.login(command);
}
```

## 7. 配置说明
配置前缀：`mango.log`。同时支持环境变量 `LOG_PATH` 和 `APP_NAME` 控制日志目录和应用名。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `level.root` | `INFO` | root 日志级别。 |
| `level.mango` | `DEBUG` | `io.mango` 包日志级别。 |
| `level.spring` | `WARN` | Spring 相关 logger 级别。 |
| `level.mybatis` | `WARN` | MyBatis / MyBatis Plus logger 级别。 |
| `level.http` | `INFO` | HTTP client / Spring web servlet logger 级别。 |
| `file.max-size` | `100MB` | 普通日志滚动单文件大小。 |
| `file.max-history` | `30` | 普通日志保留天数。 |
| `file.total-size-cap` | `3GB` | 普通日志总容量上限。 |
| `operation.enabled` | `true` | 是否启用操作日志 logger 输出。 |
| `operation.max-history` | `90` | 操作日志保留天数。 |
| `operation.total-size-cap` | `10GB` | 操作日志总容量上限。 |
| `json.enabled` | `false` | JSON 输出开关；当前 logback 主要通过 profile 选择 JSON appender。 |

示例：

```yaml
mango:
  log:
    level:
      root: INFO
      mango: INFO
      spring: WARN
      mybatis: WARN
      http: INFO
    file:
      max-size: 100MB
      max-history: 30
      total-size-cap: 3GB
    operation:
      enabled: true
      max-history: 90
      total-size-cap: 10GB
```

## 8. API 与扩展
- `@Log`：方法级操作标记，字段包括 `value` 和 `type`。
- `LogType`：支持 `LOGIN`、`LOGOUT`、`REGISTER`、`PASSWORD`、`OPERATION`、`SECURITY`、`AUDIT`。
- `LogProperties`：绑定日志配置。
- `logback-spring.xml`：定义 console、file、error、operation、json appender。

profile 行为：

- `default`、`dev`、`local`：控制台输出，操作日志写 plain 文件。
- `test`：控制台、普通文件、错误文件，操作日志写 JSON 文件。
- `prod`：控制台、JSON 文件、错误文件，操作日志写 JSON 文件。

## 9. 数据与初始化
无数据库 migration、无 Runner、无 Initializer、无初始化数据。日志文件默认写到 `${LOG_PATH:-./logs}`。

## 10. 管理入口
本模块不创建菜单和权限。日志中的租户、用户、trace 信息来自 MDC、MangoContext 或业务代码；日志查询权限由外部日志平台或业务审计模块控制。

## 11. 快速开始
1. 接入 `mango-infra-log-starter`。
2. 按环境设置 `LOG_PATH`、`APP_NAME`、`spring.profiles.active` 和 `mango.log`。
3. 业务关键方法用 `@Log` 标记类型和描述。
4. 接入 `mango-infra-web-starter` 让 MDC 中有 trace id。
5. 检查普通日志、错误日志、操作日志滚动和敏感信息。

## 12. 问题排查
- 日志没有 trace id：检查请求是否经过 `WebMdcFilter`。
- `@Log` 没有落库：本模块不提供落库 AOP，落库需要业务审计模块实现。
- prod 日志不是 JSON：检查 active profile 是否为 `prod`，以及应用是否使用该 `logback-spring.xml`。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [能力地图](../../../mango-docs/capabilities/README.md)
