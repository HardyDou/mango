# Mango Infra Log API

## 1. 概览
`mango-infra-log-api` 是日志能力的轻量契约模块，提供操作日志注解和日志类型枚举。它让业务代码可以标记操作含义，而不直接依赖日志 starter、Logback 配置或运行时装配。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 标记业务方法的操作日志名称 | `@Log` |
| 标记登录、登出、审计、安全等操作类型 | `LogType` |
| 让业务模块只依赖日志契约 | Maven API 依赖 |

## 3. 能力边界
- 不提供 Logback 配置。
- 不提供操作日志落库、查询页面或审计报表。
- 不负责 AOP 采集、MDC 写入和日志文件滚动。
- 不依赖 `core`、`starter` 或具体日志实现。

## 4. 模块入口
本模块只暴露日志契约类型：

- `io.mango.infra.log.api.annotation.Log`
- `io.mango.infra.log.api.enums.LogType`

运行时装配由 `mango-infra-log-starter` 提供。

## 5. 接入方式
业务模块只需要编译期使用注解时，依赖 API 模块：

```xml
<dependency>
    <groupId>io.mango.infra.log</groupId>
    <artifactId>mango-infra-log-api</artifactId>
</dependency>
```

需要日志配置和运行时装配时，由应用或 starter 侧接入 `mango-infra-log-starter`。

## 6. 配置说明
本 API 模块没有配置项。日志级别、文件路径、JSON 输出和操作日志 logger 配置都归属 `mango-infra-log-starter`。

## 7. API 与扩展

| 类型 | 说明 |
|------|------|
| `@Log` | 方法级操作日志标记，包含操作描述和 `LogType`。 |
| `LogType` | 操作类型枚举，覆盖登录、登出、注册、密码、安全、审计和普通操作。 |

扩展新的日志采集方式时，应读取这些契约类型，不应要求业务模块依赖具体实现。

## 8. 数据与初始化
无数据库 migration、无资源清单、无启动初始化逻辑。该模块不写入菜单、权限、字典或租户默认数据。

## 9. 管理入口
本模块不创建菜单和权限。日志管理、审计查询和授权入口由实际审计或日志平台模块提供。

## 10. 快速开始
1. 在业务 API、core 或 support 模块中引入 `mango-infra-log-api`。
2. 在需要标记的业务方法上添加 `@Log`。
3. 在应用侧接入 `mango-infra-log-starter` 完成日志输出配置。
4. 确认日志内容不包含密码、token、密钥和完整证件号。

## 11. 问题排查
- `@Log` 没有任何运行效果：确认应用侧是否接入日志采集实现或 `mango-infra-log-starter`。
- 业务模块出现 starter 依赖：只需要注解时应依赖 `mango-infra-log-api`。
- 日志里缺少 trace id：该能力来自 Web MDC 写入，不属于 API 模块。

## 12. 相关文档
- [Mango Infra Log](../mango-infra-log/README.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
