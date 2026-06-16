# Mango Common

## 1. 概览
`mango-common` 是 Mango 后端最低层公共契约包，提供统一返回体、业务错误码、业务异常、分页请求、分页返回和轻量断言工具。

它不依赖 Spring runtime，不承载业务事实，也不做技术基础设施实现。业务模块、平台模块和 infra 模块都可以依赖它来保持 API 返回和异常语义一致。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| Controller 返回统一 R&lt;T&gt; | Maven 依赖 / HTTP API / Java API |
| Service 或校验逻辑抛出 BizException | Maven 依赖 / HTTP API / Java API |
| 模块自定义错误码实现 BizCode | Maven 依赖 / HTTP API / Java API |
| 列表接口使用 PageQuery 和 PageResult&lt;T&gt; | Maven 依赖 / HTTP API / Java API |
| 业务断言使用 Require，避免到处手写异常 | Maven 依赖 / HTTP API / Java API |


## 3. 能力边界
- 不放业务实体、VO、DTO 或业务枚举。
- 不放 Spring Bean、AutoConfiguration、ControllerAdvice、Filter、Interceptor。
- 不放数据库、Redis、租户、登录上下文、地区规则、文件或支付等技术/业务实现。
- 不放只服务某个模块的工具类。

## 4. 模块入口
`mango-common` 只提供跨模块稳定 Java 契约。技术实现放到 `mango-infra`，平台业务事实放到 `mango-platform`，业务私有对象放到业务模块自己的 `api` 或 `core`。

当前核心包：

- `io.mango.common.result`
- `io.mango.common.exception`
- `io.mango.common.po`
- `io.mango.common.vo`

## 5. 接入方式
Maven 依赖：

```xml
<dependency>
    <groupId>io.mango.common</groupId>
    <artifactId>mango-common</artifactId>
</dependency>
```

统一返回：

```java
import io.mango.common.result.R;

public R<String> ping() {
    return R.ok("pong");
}
```

业务异常和断言：

```java
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;

Require.notNull(order, "订单不存在");

throw new BizException(400, "订单状态不允许操作");
```

分页返回：

```java
import io.mango.common.po.PageQuery;
import io.mango.common.vo.PageResult;

PageResult<OrderVO> result = PageResult.of(rows, total, query.getPage(), query.getSize());
```

## 6. 配置说明
无运行时配置项。

`PageQuery` 有内置分页约束：

| 字段 | 默认值 | 规则 |
|------|--------|------|
| `page` | `1` | 小于 1 时按 1 处理。 |
| `size` | `10` | 小于等于 0 时按 10 处理。 |
| 最大 `size` | `500` | 超过 500 时按 500 处理。 |

## 7. API 与扩展
| 类型 | 能力 |
|------|------|
| `R<T>` | 统一返回体，字段为 `code`、`success`、`msg`、`data`。 |
| `BizCode` | 业务错误码接口，模块可自定义枚举实现。 |
| `CommonCode` | 通用错误码：成功、参数错误、未登录、权限不足、资源不存在、系统繁忙。 |
| `BizException` | 业务异常，携带 `code` 和异常消息。 |
| `Query` | 查询请求基础类。 |
| `PageQuery` | 分页查询请求，规范化页码和分页大小。 |
| `PageResult<T>` | 分页返回，字段为 `list`、`total`、`page`、`size`、`pages`。 |
| `Require` | 断言工具，失败时抛 `BizException`。 |

业务模块自定义错误码示例：

```java
public enum OrderCode implements BizCode {
    ORDER_NOT_FOUND(140400, "订单不存在");

    private final int code;
    private final String message;
}
```

## 8. 数据与初始化
无数据库、migration、Runner 或 Initializer。

## 9. 管理入口
无菜单、权限和租户能力。它只提供公共 Java 契约，不能用于绕过权限、登录或租户校验。

## 10. 快速开始
1. 业务模块依赖 `mango-common`。
2. Controller 返回 `R<T>` 或由统一 Web 层包装。
3. 查询对象继承或组合 `PageQuery`。
4. 列表接口返回 `PageResult<T>`。
5. 业务异常使用 `BizException` 或 `Require`。
6. 模块级错误码实现 `BizCode`，不要复用 HTTP 状态码表达业务细分错误。

## 11. 问题排查
- 是否可以把业务 DTO 放 common：不可以，放业务模块 api。
- 是否可以在 common 加 Spring 工具：不可以，放 infra。
- 为什么 `PageResult` 会复制 list：避免外部直接修改内部状态。
- 为什么 `PageQuery` 最大 500：保护列表接口，避免无上限分页拖垮数据库。

## 12. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [后端 API 规范](../../mango-pmo/rules/backend/03-api.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [Mango 后端根 README](../README.md)
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
