# 后端 API 规范

## 1. 基本规则

- API 只暴露协议模型。
- 不暴露 `Entity`、`PO`。
- 写操作使用 `Command`。
- 查询使用 `Query`。
- 返回统一使用 `VO`。

## 2. 入参规则

- 创建使用 `CreateXxxCommand`。
- 更新使用 `UpdateXxxCommand`。
- 查询使用 `XxxQuery` 或 `XxxPageQuery`。
- 简单路径参数和查询参数直接放方法签名。
- `GET` 默认不用 `@RequestBody`。
- API 参数必须使用 Bean Validation 校验。
- `Command`、`Query` 字段必须声明 `jakarta.validation` 约束注解。
- 路径参数和查询参数必须声明校验约束。
- Controller 或 `Api` 必须使用 `@Validated` 或等效机制开启参数校验。

## 3. 返回规则

- 无数据成功：`R<Void>`
- 单对象：`R<XxxVO>`
- 列表：`R<List<XxxVO>>`
- 分页：`R<PageResult<XxxVO>>`
- 简单状态：`R<Boolean>`
- 简单标识：`R<Long>`、`R<String>`

## 4. 分层规则

- `api` 只放 `XxxApi`、`command`、`query`、`vo`、`enums`。
- Controller 只做协议适配。
- Controller 必须实现对应 `XxxApi`，内部只能依赖 `IXxxService` 或等效服务接口。
- Controller 禁止持有 `XxxApi` 字段进行自调用。
- Controller 不直接操作 `Mapper`。
- Controller 不直接返回持久化对象。
- `XxxApi` 只定义能力契约。
- `XxxApi` 禁止声明 `@FeignClient`。
- `XxxService` 禁止直接实现 `XxxApi`；服务层应实现 `IXxxService`。

## 5. DTO 规则

- 仓内业务 API 禁止使用 `DTO` 作为默认入参或返回命名。
- `DTO` 只允许用于第三方集成、外部回调、历史兼容层。

## 6. 兼容策略

- 新增接口必须遵守本规范。
- 老接口按模块逐步迁移。
- 迁移期间允许在 service 或 convert 层做兼容转换。
- 不允许继续新增 `DTO`、`PO`、`Entity` 直出直入接口。

## 7. 禁止事项

- `@RequestBody XxxEntity`
- `R<XxxEntity>`
- `R<PageResult<XxxEntity>>`
- `UserDTO` 作为仓内业务 API 返回
- `Map<String, Object>` 作为通用业务接口模型
- 无校验注解的 `Command` / `Query`
- API 参数绕过 Bean Validation 直接进入业务层
- 在 `*-api` 中写 `@FeignClient`
- 在 API 契约中硬编码服务发现名
