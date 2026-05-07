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
- 简单路径参数和查询参数在 1-2 个时可以直接放方法签名。
- 客户端可传入参数超过 2 个时必须收敛为 `Command`、`Query` 或专用请求对象；`HttpServletRequest`、`HttpServletResponse` 等服务端上下文对象不计入客户端入参数量。
- `GET` 禁止使用 `@RequestBody`；复杂 `Query` 对象必须使用 `@ParameterObject` 展开为 query 参数，禁止在 OpenAPI/Knife4j 中显示为 `query={...}` JSON 字符串参数。
- `POST` / `PUT` / `PATCH` 的 `Command`、`PO`、请求对象默认使用 `@RequestBody` 接收 JSON；只有明确声明为表单提交的接口才允许使用 `@RequestParam` / `@ModelAttribute`。
- `DELETE` 的简单标识优先使用 `@RequestParam`；批量删除等复杂命令使用 `@RequestBody`。
- API 参数必须使用 Bean Validation 校验。
- `Command`、`Query` 字段必须声明 `jakarta.validation` 约束注解。
- 路径参数和查询参数必须声明校验约束。
- Controller 或 `Api` 必须使用 `@Validated` 或等效机制开启参数校验。

## 2.1 OpenAPI / Knife4j 文档规则

- 所有对外展示的 Controller 必须声明中文 `@Tag(name, description)`，禁止显示默认的 `xxx-controller`。
- 所有 HTTP 接口方法必须声明中文 `@Operation(summary, description)`；`summary` 说明接口动作，`description` 说明业务语义、访问边界或关键约束。
- 所有客户端可传入参数都必须在文档中展示中文含义。
- 所有简单请求参数必须声明中文 `@Parameter(description = "...")`，包括 `@RequestParam`、`@RequestHeader`、`@PathVariable`（路径变量本身禁止新增）。
- 所有 `Command`、`Query`、`Request`、`Response`、`VO`、`PO`、`DTO` 字段必须声明中文 `@Schema(description = "...")`；字段含义、枚举值或取值约束要能在文档中直接看懂。
- `GET` Query 对象在 Controller 方法参数上必须使用 `@ParameterObject`，并依赖字段级 `@Schema` 展示中文参数含义。
- `POST` / `PUT` JSON body 的 Controller 参数必须使用 `@RequestBody`，并依赖请求对象字段级 `@Schema` 展示中文字段含义。
- 鉴权文档使用名为 `Authorization` 的 header apiKey 安全方案；Knife4j 调试时输入完整 `Bearer <accessToken>`，请求必须发出标准 `Authorization` 请求头。
- 内部接口可以在 `@Operation(description)` 中说明“仅内部调用”；是否显示对内 tag 由文档配置控制，不允许为了文档展示修改真实访问权限。

## 3. 返回规则

- 无数据成功：`R<Void>`
- 单对象：`R<XxxVO>`
- 列表：`R<List<XxxVO>>`
- 分页：`R<PageResult<XxxVO>>`
- 简单状态：`R<Boolean>`
- 简单标识：`R<Long>`、`R<String>`

## 4. 分层规则

- `api` 只放 `XxxApi`、`command`、`query`、`vo`、`enums`。
- `api` 如需声明内部访问边界，允许依赖 `mango-infra-web-api` 并在 `XxxApi` 类或方法上使用 `@Inner`。
- `api` 禁止依赖 `mango-infra-web-starter`。
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
