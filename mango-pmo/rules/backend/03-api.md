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

## 2.1 Service 入参规则

- `XxxApi`、Controller、`IXxxService` 和 `XxxServiceImpl` 的核心业务方法必须保持同一入参模型。
- 创建、更新、查询、批量操作和复杂业务动作必须使用 `Command`、`Query` 或专用请求对象。
- Service 方法超过 2 个业务字段入参时，必须收敛为 `Command`、`Query` 或专用请求对象。
- 允许直接展开的参数仅限 `id`、状态枚举、布尔控制位等 1-2 个简单控制参数。
- Controller 调 Service 时直接传 `Command`、`Query` 或专用请求对象；Service 负责校验后的业务编排和模型转换。
- Service 调 Mapper 时不得继续传 API 协议模型，必须转换为 Entity、id、Wrapper、分页对象或 core 内部持久化查询对象。

## 2.2 文件字段规则

- 业务 API 的创建、更新入参禁止接收需要持久化的文件访问地址字段，例如 `url`、`previewUrl`、`downloadUrl`、`directPreviewUrl`、`directDownloadUrl`。
- 业务 API 应接收文件中心标识字段，例如 `fileId`、`fileIds`、`attachmentIds`、`iconFileId`，或明确的 `mango-file:{id}` token。
- 业务 VO 可以返回文件 ID；如页面需要临时预览或下载地址，由前端通过 `mango-file` 查询，或由业务聚合层在响应时临时组装，禁止反写到业务表。
- 预签名 URL、对象存储直连地址只允许作为单次响应中的运行时字段，不允许成为业务 Command、Entity 或持久化 JSON 的字段。

## 2.3 OpenAPI / Knife4j 文档规则

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

## 3.1 数据来源规则

- API 返回的数据必须来自数据库、配置、枚举契约、字典服务、远程 API 或明确的运行时计算。
- 本应由数据库维护的数据，禁止在 Controller、Service、Converter 或 VO 组装逻辑中写死。
- 菜单、权限、公共路径、租户默认数据、字典、业务状态、流程配置、计费规则等可运营数据必须有数据库、migration、配置中心或后台维护入口。
- 临时调试数据和 mock 数据禁止进入最终 API 实现。
- 接口尚未接入真实数据源时，必须标记为未完成或阻塞，禁止声明可供前端替换 mock。

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

## 4.1 Controller、Service 与错误契约

| 层 | 必须负责 | 绝对禁止 |
|---|---|---|
| `XxxApi` | 协议方法、Command/Query/VO、校验约束、统一 `R<T>` 返回 | Entity/PO、实现逻辑、`@FeignClient`、裸返回值 |
| `XxxController` | 实现 `XxxApi`、协议适配、`@Validated`、`@RequestBody @Valid`、调用 `IXxxService`、包装成功 `R<T>` | Mapper/Entity/Feign/具体 Service 依赖，自行拼装失败 `R`，业务判断和持久化 |
| `IXxxService`/实现 | 业务编排、业务前置条件、Command/Query 到持久化模型转换、事务和状态结果 | 返回或拼装 `R`，直接实现 `XxxApi`，用 `if/throw`、裸异常或静默 return 代替 `Require` |
| `XxxMapper` | Entity/id/Wrapper/分页及 core 内部查询对象的数据访问 | Command/Query/VO/Controller 请求对象、注解 SQL、跨域表访问 |

- Controller 的基础字段校验由 Bean Validation 完成；业务存在性、状态、归属、权限、重复和前后置条件必须在 Service 使用 `Require` 校验。
- Service 的业务失败必须使用模块 `XxxCode implements BizCode`（即统一 ErrorCode 契约）；禁止裸数字错误码和临时错误字符串作为新增业务错误契约。
- Controller 只返回成功 `R<T>`；业务失败由 Service 抛出带 `BizCode` 的业务异常，再由统一异常处理器转换为失败 `R<T>`。
- 以上为不可降级结构红线：新增文件或被修改文件命中时，即使历史基线存在也必须失败，不允许普通例外。

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
- 业务 API 用文件访问地址代替文件 ID
- API 写死本应来自数据库、配置、字典服务或真实接口的数据
- Controller 缺少 `@Validated` 或 `@RequestBody` 缺少 `@Valid`
- Controller 未实现 `XxxApi`，或依赖 Mapper、Entity、FeignClient、具体 `XxxService`
- Controller/API HTTP 方法不返回 `R<T>`，或返回 Entity/PO
- Service 返回 `R<T>`、调用 `R.ok/R.fail`、直接抛裸运行时/业务异常
- Service 业务动作没有使用 `Require + XxxCode implements BizCode` 执行业务前置条件校验
