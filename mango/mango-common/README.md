# Mango Common

`mango-common` 是 Mango 后端的公共内核与稳定契约包。

## 定位

`mango-common` 只承载低耦合、跨模块稳定、没有运行时技术实现和平台业务语义的公共契约。它是所有层可以依赖的最小公共层，不是工具箱，也不是业务模型中转站。

## 当前保留内容

| 类别 | 类 | 准入原因 |
|------|----|----------|
| 统一返回结构 | `R<T>` | API 稳定响应契约 |
| 错误码契约 | `BizCode`、`CommonCode` | 各模块错误码枚举的公共接口与默认基线 |
| 业务异常契约 | `BizException` | 统一业务异常载体 |
| 分页契约 | `PageQuery`、`PageResult<T>` | 跨模块分页请求与响应结构 |
| 轻量契约断言 | `Require` | 只抛出 `BizException`，不依赖 Web/DB/Security 等技术实现 |
| 请求上下文扩展协议 | `RequestContext`、`RequestContextContributor` | 为多模块提供请求级运行时属性扩展点，不反向绑定 Web/Security/RPC 实现 |

## 准入规则

允许进入 `mango-common` 的代码必须同时满足：

1. 被多个模块长期稳定复用。
2. 不表达具体业务域事实。
3. 不依赖 Web、DB、Redis、Security、Crypto、Feign、Gateway 等技术实现。
4. 不依赖 Spring 运行时装配、外部中间件客户端或具体存储。
5. 可以作为 API 契约、错误契约、分页契约、基础注解或极轻量契约辅助存在。
6. 如需支持多模块共同扩展的运行时协议，只能定义最小接口，不得夹带具体技术栈上下文对象。

新增类进入 `mango-common` 前必须先说明：

- 使用方模块。
- 为什么不能放在具体业务模块或 infra 模块。
- 是否会把业务模型或技术实现反向带入公共层。

## 禁止进入

以下内容不得进入 `mango-common`：

- `SysUser`、用户资料、用户角色关系等身份/授权侧模型。
- 角色、菜单、按钮权限、公共路径等权限模型。
- 组织、部门、岗位等组织模型。
- 消息、验证码、AI、租户、字典、配置、审计日志、路由等平台业务模型。
- Web、DB、Redis、Security、Crypto、Feign、Gateway 等技术实现。
- `HttpServletRequest`、`SecurityContext`、RPC request 等具体运行时上下文来源。
- 地区或业务规则明显的校验实现，例如中国大陆手机号、身份证号校验。
- 历史兼容 shim、迁移过渡类、重复工具类。

## Phase 1 结论

Phase 1 删除了无外部引用且带地区语义的校验类：

- `io.mango.common.valid.Phone`
- `io.mango.common.valid.PhoneValidator`
- `io.mango.common.valid.IdCard`
- `io.mango.common.valid.IdCardValidator`

`mango-common` 不再因为这些校验类依赖 `spring-boot-starter-validation`。

后续如需要手机号、身份证号等校验能力，应放在具体业务域、地区能力模块或独立校验能力中，不得回填到 `mango-common`。

## RequestContext 扩展协议

`mango-common` 当前新增了一个受限的运行时扩展协议：

- `io.mango.common.spi.request.RequestContext`
- `io.mango.common.spi.request.RequestContextContributor`

它的职责不是提供某种具体技术框架上下文，而是定义一个最小公共协议，让不同 infra 模块可以把“本次请求相关”的运行时属性注入给消费方。

### 为什么是 RequestContext

这里刻意没有继续使用更大的命名，比如 `ExpressionContext`、`RuntimeContext`。

原因：

1. 当前真实需求是“请求相关变量增强”，例如 `headers`、`cookies`、`request`、后续可能的 `principal`、`traceId`。
2. 如果抽象成 `ExpressionContext`，很容易演变成任意表达式系统的总线，被规则引擎、脚本、模板、动态 SQL 等场景滥用。
3. 收敛到 `RequestContext` 后，边界更清楚：它只表达“请求级运行时上下文”，不是通用表达式平台。

### 协议边界

`RequestContext` 只提供最小写入能力：

- `setAttribute(String name, Object value)`

`RequestContextContributor` 只负责一件事：

- 往当前请求上下文里贡献属性

`mango-common` 不负责：

- 生成请求对象
- 保存线程上下文
- 决定变量来源
- 解释这些属性如何被 SpEL、规则引擎或其它机制消费

### 正确分层

推荐分层如下：

- `mango-common`：只定义 `RequestContext`、`RequestContextContributor`
- `mango-infra-web`：提供 `headers`、`cookies`、`request`
- `mango-infra-security`：后续可提供 `principal`、`roles`
- `mango-infra-trace` 或其它模块：后续可提供 `traceId`、`spanId`
- `mango-infra-kv`：作为消费方，把这些请求属性映射到自己的 SpEL 求值上下文

### 明确禁止

以下做法不允许：

- 在 `mango-common` 中引入 `HttpServletRequest`、`ServerWebExchange`、`SecurityContext`
- 在 `mango-common` 中定义具体变量名来源实现
- 把 `RequestContext` 扩展成万能上下文容器
- 让业务模块直接依赖某个具体 contributor 实现

### 当前结论

本次补充的请求上下文扩展协议只定义“如何注入请求级属性”，不定义属性来源。Web、Security、Trace、RPC 等运行时属性应由各自 infra 模块提供实现，不得反向把这些技术依赖压回 `mango-common`。

## 参考文档

- [Phase 1 类归属表](../../mango-docs/plans/2026-04-17-phase-1-common-class-ownership.md)
- [后端模块级重构计划](../../mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md)

## 验证

从 `mango/` 目录执行：

```bash
mvn -q -DskipTests compile
```
