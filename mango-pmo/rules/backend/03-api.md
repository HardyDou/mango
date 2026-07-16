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
- 新增 API 禁止路径变量和 URI 模板；简单标识统一使用 `@RequestParam`，请求头使用 `@RequestHeader`。
- 1-2 个简单查询参数可以直接放方法签名；路径中不得出现 `{id}`、`{code}` 等变量段。
- 客户端可传入参数超过 2 个时必须收敛为 `Command`、`Query` 或专用请求对象；`HttpServletRequest`、`HttpServletResponse` 等服务端上下文对象不计入客户端入参数量。
- `GET` 禁止使用 `@RequestBody`；复杂 `Query` 对象必须使用 `@ParameterObject` 展开为 query 参数，禁止在 OpenAPI/Knife4j 中显示为 `query={...}` JSON 字符串参数。
- `POST` / `PUT` / `PATCH` 的 `Command`、`Request` 默认使用必填 `@RequestBody` 接收 JSON；只有明确声明为表单提交的接口才允许使用 `@RequestParam` / `@ModelAttribute`。
- `DELETE` 的简单标识优先使用 `@RequestParam`；批量删除等复杂命令使用 `@RequestBody`。
- API 参数必须使用 Bean Validation 校验。
- `Command`、`Query` 字段必须声明 `jakarta.validation` 约束注解。
- 协议模型使用普通 class；禁止用 record 作为 Command/Query/Request/VO/Response，禁止继承非协议模型基类。
- 嵌套 Command/Query/Request 字段必须使用 `@Valid` 级联校验；字段类型只能是标量、枚举、同类协议模型及受支持的单泛型容器。
- 查询参数必须声明校验约束。
- `XxxApi` 统一声明方法参数的 Bean Validation 约束，包括复合入参的 `@Valid`；Controller 使用 `@Validated` 或等效机制开启方法校验。实现 `XxxApi` 的 Controller 禁止在覆盖方法上重复声明参数约束，避免违反 Bean Validation 的继承规则；未实现 API 契约的独立 Controller 才在 `@RequestBody` 参数上直接声明 `@Valid`。

## 2.1 Service 入参规则

- `XxxApi`、Controller、`IXxxService` 和 `XxxService` 的核心业务方法必须保持同一入参模型。
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
- `io.mango.infra.*` 中只在进程内使用、不承载 HTTP/Feign 语义的 Java 能力契约，必须使用
  `@LocalCapabilityContract` 显式标识；该标记默认用于契约接口及其本地输入/输出类型。只有为了
  保持已发布的公开类名和构造方式兼容时，本地能力入口的具体实现类才可同时标记。
- `@LocalCapabilityContract` 禁止用于 `platform`、`business` 或其它非 `io.mango.infra.*` 包，禁止由
  Controller、FeignClient 或其它 HTTP adapter 实现，也不能用来规避 HTTP `XxxApi` 的 `R<T>`、
  Bean Validation、协议模型和适配器一致性规则。
- 本地 JVM 能力契约与 HTTP `XxxApi` 是两类边界：前者保留适合进程内处理的流、路径、字节内容等
  Java 类型，后者必须保持传输无关并遵守本文件全部 HTTP 协议规则。
- `api` 如需声明内部访问边界，允许依赖 `mango-infra-web-api` 并在 `XxxApi` 类或方法上使用 `@Inner`。
- `api` 禁止依赖 `mango-infra-web-starter`。
- Controller 只做协议适配。
- Controller 必须实现对应 `XxxApi`，内部只能依赖 `IXxxService` 或等效服务接口。同域 `support`
  中不访问数据库、不承载 HTTP/Feign 语义的纯 Java `XxxExecutor` 接口可作为框架适配器的等效服务端口；
  其实现仍不得放入 Controller，也不得借此绕过业务 Service 分层。
- Controller 禁止持有 `XxxApi` 字段进行自调用。
- Controller 不直接操作 `Mapper`。
- Controller 不直接返回持久化对象。
- `XxxApi` 只定义能力契约。
- `XxxApi` 保持传输无关，只声明 Java 方法、Bean Validation 和 `R<T>`；禁止声明 Spring MVC mapping、`@RequestBody`、`@RequestParam`、`@RequestHeader` 或 `@PathVariable`。
- `XxxApi` 不继承其它接口，不声明字段、常量、default/static/private 方法；完整契约直接声明在本接口。
- `XxxApi` 禁止声明 `@FeignClient`。
- `XxxService` 禁止直接实现 `XxxApi`；服务层应实现 `IXxxService`。
- `XxxController` 和 `XxxFeignClient` 分别实现同一个 `XxxApi`，只在各自适配层重声明 HTTP 注解；不得在三处发明不同方法签名。

## 4.1 Controller、Service 与错误契约

| 层 | 必须负责 | 绝对禁止 |
|---|---|---|
| `XxxApi` | 传输无关的协议方法、Command/Query/VO、校验约束、统一 `R<T>` 返回 | Entity/PO、Spring MVC/Feign 注解、实现逻辑、裸返回值 |
| `XxxController` | 直接声明 `@RestController`，实现唯一一个 `XxxApi`、协议适配、`@Validated`、必填 `@RequestBody`、继承 API 参数校验、调用 `IXxxService`、直接返回 `R.ok(service.xxx(...))` | 在覆盖方法上重复 API 参数约束、继承 Controller 基类，Mapper/Entity/Feign/具体 Service 依赖，硬编码返回值，自行拼装失败 `R`，业务判断和持久化 |
| `IXxxService`/实现 | 业务编排、业务前置条件、Command/Query 到持久化模型转换、事务和状态结果 | 返回或拼装 `R`，直接实现 `XxxApi`，用 `if/throw`、裸异常或静默 return 代替 `Require` |
| `XxxMapper` | Entity/id/Wrapper/分页及 core 内部 `Criteria`/`Row` 等类型化持久化模型的数据访问 | `Object`、`Map`、Command/Query/Request/Response/VO/DTO、Controller/Feign/Service/传输上下文、注解 SQL、跨域表访问 |

- Controller 的基础字段校验由 Bean Validation 完成；业务存在性、状态、归属、权限、重复和前后置条件必须在 Service 使用 `Require` 校验。
- Service 的业务失败必须使用模块 `XxxCode implements BizCode`（即统一 ErrorCode 契约）；禁止裸数字错误码和临时错误字符串作为新增业务错误契约。
- Controller 只返回成功 `R<T>`；业务失败由 Service 抛出带 `BizCode` 的业务异常，再由统一异常处理器转换为失败 `R<T>`。
- Service 实现类实现 `IXxxService`，默认使用 `XxxService implements IXxxService`；`XxxServiceImpl` 允许作为可选命名，不再作为新增强制约束。
- `IXxxService` 只位于 `core`，只允许继承 canonical `MangoCrudService` / `MangoTypedCrudService`；实现类除 canonical `MangoCrudServiceImpl` 外不得继承业务基类或 MyBatis `ServiceImpl`。
- 以上为不可降级结构红线：新增文件或被修改文件命中时，即使历史基线存在也必须失败，不允许普通例外。

## 4.2 Spring Service 注册与对象创建

### 4.2.1 业务 Service

- **正向要求**：`core` 业务实现放在 `service/impl`，声明 `@Service`，并实现 `IXxxService`；命名建议使用 `XxxService`，`XxxServiceImpl` 允许存在但不再作为强制要求；Controller 和其它业务 Service 通过构造器注入 `IXxxService`，事务、异步、缓存和调度注解只能出现在已证明由 Spring 托管的对象上。
- **禁止项**：禁止业务实现遗漏 `@Service`；禁止用 `@Component`、starter `@Bean`、静态字段或 Service Locator 规避业务 Service 约定；禁止 Controller 或业务 Service 执行 `new XxxService(...)`。
- **正例**：`@Service public class OrderService implements IOrderService`，Controller 构造器接收 `IOrderService`，`@Transactional` 方法由 Spring 代理调用。
- **反例**：`OrderController` 内执行 `new OrderService(mapper)`。错误原因：绕过依赖注入、事务代理、缓存、异步、生命周期和业务项目覆盖能力。

### 4.2.2 starter 默认实现与框架 SPI

- **正向要求**：框架 SPI、默认适配器或可被业务项目替换的实现可以保持普通 Java 类，但必须由拥有运行时装配责任的 starter 通过 `@Bean + @ConditionalOnMissingBean` 注册；实现类与 starter 可以跨模块，机器门禁以完整 Reactor 证明注册关系。
- **禁止项**：禁止同时给同一实现声明 `@Service` 和 `@Bean`；禁止默认实现只写 `@Bean` 而缺少 `@ConditionalOnMissingBean`；禁止把注册逻辑放进 Controller 或业务 Service。
- **正例**：`SystemEventService` 不声明 stereotype，`DomainEventAutoConfiguration` 使用 `@Bean @ConditionalOnMissingBean` 返回该对象，业务项目可提供同类型 Bean 覆盖默认实现。
- **反例**：默认加密实现既声明 `@Service`，starter 又无条件 `@Bean` 构造一次。错误原因：存在重复 Bean、覆盖失效和运行时装配歧义。

### 4.2.3 纯 Java 对象与静态状态

- **正向要求**：无事务、异步、缓存、调度、注入和生命周期需求的算法、策略、codec、factory、calculator 可以由调用方显式创建；名称必须表达纯对象职责，不使用 `Service` 冒充容器服务。
- **禁止项**：禁止可变 static 字段保存 `I*Service` 或 `*Service`；禁止新增静态 `getService/setService` 桥接容器；禁止在未注册对象上声明 `@Transactional`、`@Async`、`@Scheduled`、`@Cacheable`、`@CacheEvict` 或 `@CachePut`。
- **正例**：`MoneyCalculator calculator = new MoneyCalculator(roundingPolicy)`，对象无 Spring 注解、无容器依赖且不命名为 Service。
- **反例**：`private static volatile IOrderService current` 配合静态 setter。错误原因：形成隐式全局依赖，测试隔离、并发可见性、生命周期和多上下文行为不可证明。

## 4.3 Remote Adapter 规则

- `XxxFeignClient` 只能位于 `starter-remote`，必须且仅能继承一个本域 `XxxApi`。
- `@FeignClient` 必须显式声明与模块信息一致的 `name`、唯一的 lowerCamelCase `contextId` 和以模块 `module-path` 开头的绝对 `path`。
- Controller 根路径和 Feign path 必须是静态字面量，禁止 `${...}`、`#{...}`、URI 模板和多路径/条件 mapping。
- Feign 只重声明继承方法所需的 mapping、body/query/header 注解，不定义新业务能力、不持有 Mapper/Entity/Service、不包含业务判断。
- Controller 根路径与 Feign `path` 必须表达同一模块和资源根；方法级 mapping 必须逐一一致。
- 正例：`OrderFeignClient extends OrderApi`，`name = "order"`、`contextId = "orderFeignClient"`、`path = "/order/orders"`，方法使用 `@GetMapping("/detail") + @RequestParam("id")`。
- 反例：Feign 放进 `api`；继承两个 API；缺 `contextId`；Controller 使用 `/detail?id=` 而 Feign 使用 `/detail/{id}`；在 Feign 内增加 API 不存在的方法。

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
- `@PathVariable` 或 mapping 路径中的 `{variable}`
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
- Controller 缺少 `@Validated`；独立 Controller 的 `@RequestBody` 缺少 `@Valid`；或实现 `XxxApi` 时在覆盖方法上重复 API 参数约束
- Controller 未实现 `XxxApi`，或依赖 Mapper、Entity、FeignClient、具体 `XxxService`
- Controller/API HTTP 方法不返回 `R<T>`，或返回 Entity/PO
- Service 返回 `R<T>`、调用 `R.ok/R.fail`、直接抛裸运行时/业务异常
- Service 业务动作没有使用 `Require + XxxCode implements BizCode` 执行业务前置条件校验
- 业务 `XxxService implements IXxxService` 缺少 `@Service`，或 Controller/业务 Service 手工 `new` Spring Service
- starter 默认实现缺少 `@Bean + @ConditionalOnMissingBean`，或同一 Service 同时使用 stereotype 与 `@Bean`
- 未注册对象使用事务、异步、缓存、调度注解，或通过可变 static Service 字段实现 Service Locator

## 8. 机器门禁

以下 ID 与 `MangoJavaArchitectureRule`、`MangoArchUnitChecker`、`MavenDependencyChecker`、`ArchitectureMojo` 和 `CheckMojo` 当前实现逐项对应。禁止用区间概述代替具体 ID。

### 8.1 API、路径、HTTP 与协议模型

- `MANGO-ARCH-PATH-001`：禁止 `@PathVariable`；`MANGO-ARCH-PATH-002`：禁止 mapping 字面量中的 URI 模板；`MANGO-ARCH-PATH-003`：禁止 HTTP 路径使用 `${...}` 或 `#{...}` 运行时占位。
- `MANGO-ARCH-API-001`：Api 传输无关；`MANGO-ARCH-API-002`：复合入参 `@Valid`、简单入参 Jakarta constraint；`MANGO-ARCH-API-003`：禁止服务端 transport context；`MANGO-ARCH-API-004`：客户端入参最多两个；`MANGO-ARCH-API-005`：禁止字段、常量和方法实现；`MANGO-ARCH-API-006`：入参只允许标量、枚举、Command、Query、Request；`MANGO-ARCH-API-007`：Api 不继承其它接口。
- `MANGO-ARCH-HTTP-001`：必须返回参数化 `R<T>`；`MANGO-ARCH-HTTP-002`：返回禁止 Entity/PO；`MANGO-ARCH-HTTP-003`：Api 禁止 Feign 且入参禁止 Entity/PO；`MANGO-ARCH-HTTP-004`：禁止 raw R；`MANGO-ARCH-HTTP-005`：禁止 `Map<String,Object>`；`MANGO-ARCH-HTTP-006`：入参与返回禁止 DTO；`MANGO-ARCH-HTTP-007`：R payload 只允许标量、枚举、VO、Response、集合或 PageResult。
- `MANGO-ARCH-MODEL-001`：字段要求非空中文 Schema；`MANGO-ARCH-MODEL-002`：输入字段要求 Jakarta constraint；`MANGO-ARCH-MODEL-003`：禁止协议 record；`MANGO-ARCH-MODEL-004`：输入字段类型白名单；`MANGO-ARCH-MODEL-005`：输出字段类型白名单；`MANGO-ARCH-MODEL-006`：只允许同类协议模型继承和 Serializable；`MANGO-ARCH-MODEL-007`：嵌套输入要求 `@Valid`。
- `MANGO-ARCH-OPENAPI-001`：Controller 要求 Tag；`MANGO-ARCH-OPENAPI-002`：HTTP 方法要求 Operation；`MANGO-ARCH-OPENAPI-003`：简单参数要求 Parameter；`MANGO-ARCH-OPENAPI-004`：Query 要求 ParameterObject；`MANGO-ARCH-OPENAPI-005`：Tag name/description 要求非空中文；`MANGO-ARCH-OPENAPI-006`：Operation summary/description 要求非空中文；`MANGO-ARCH-OPENAPI-007`：Parameter description 要求非空中文。

### 8.2 类型位置与 Controller

- `MANGO-ARCH-TYPE-001`：Controller 位于 starter；`MANGO-ARCH-TYPE-002`：Controller 直接且只实现一个本域 Api；`MANGO-ARCH-TYPE-003`：字段只能是非 static 的 `I*Service`；`MANGO-ARCH-TYPE-004`：Service 实现位于 core；`MANGO-ARCH-TYPE-005`：`XxxService` 实现 `IXxxService`；`MANGO-ARCH-TYPE-006`：Mapper 位于 core；`MANGO-ARCH-TYPE-007`：业务 Entity 位于 core；`MANGO-ARCH-TYPE-008`：Service 禁止实现 HTTP API；`MANGO-ARCH-TYPE-009`：Api 位于 api；`MANGO-ARCH-TYPE-010`：业务 api 只允许契约类型，`io.mango.infra.persistence.api` 作为 canonical 持久化类型系统可提供基础抽象和不可变值类型；`MANGO-ARCH-TYPE-011`：`IXxxService` 位于 core。
- `MANGO-ARCH-CTRL-001`：要求 `@Validated`；`MANGO-ARCH-CTRL-002`：源码字段只依赖 `I*Service`；`MANGO-ARCH-CTRL-003`：独立 Controller 的 body 要求 `@Valid`，实现 Api 的覆盖方法必须继承 Api 参数约束且禁止重复 `@Valid`；`MANGO-ARCH-CTRL-004`：只允许直接返回 canonical `R.ok(...)`；`MANGO-ARCH-CTRL-005`：逐项重声明 Api mapping、保留精确泛型且禁止 Api 外方法；`MANGO-ARCH-CTRL-006`：GET 禁止 body；`MANGO-ARCH-CTRL-007`：写 Command/Request 使用 required body；`MANGO-ARCH-CTRL-008`：唯一根路径匹配 module-path；`MANGO-ARCH-CTRL-009`：禁止 Controller 继承；`MANGO-ARCH-CTRL-010`：禁止 throw；`MANGO-ARCH-CTRL-011`：直接声明 canonical RestController；`MANGO-ARCH-CTRL-012`：根 mapping 只能一个 path 且无 method/params/headers/consumes/produces 条件；`MANGO-ARCH-CTRL-013`：`R.ok` 必须包装 `I*Service` 调用结果，禁止硬编码 payload。

### 8.3 Service、Mapper 与 Entity

- `MANGO-ARCH-SVC-001`：Service 禁止返回 R；`MANGO-ARCH-SVC-002`：禁止调用或构造 R；`MANGO-ARCH-SVC-003`：Require 使用模块 `XxxCode implements BizCode`；`MANGO-ARCH-SVC-004`：写业务动作要求 Require；`MANGO-ARCH-SVC-005`：实现类默认命名 `XxxService`（`XxxServiceImpl` 兼容）；`MANGO-ARCH-SVC-006`：禁止直接 throw；`MANGO-ARCH-SVC-007`：CRUD 继承 canonical `MangoCrudServiceImpl`；`MANGO-ARCH-SVC-008`：CRUD 实现 `MangoTypedCrudService`；`MANGO-ARCH-SVC-009`：禁止同名伪造 Mango CRUD 类型；`MANGO-ARCH-SVC-010`：公共方法最多两个业务入参；`MANGO-ARCH-SVC-011`：Typed CRUD 六类泛型与 Mapper/Entity 聚合对齐；`MANGO-ARCH-SVC-012`：Service interface 只声明 abstract 契约；`MANGO-ARCH-SVC-013`：Service interface 传输无关；`MANGO-ARCH-SVC-014`：禁止直接继承 MyBatis ServiceImpl；`MANGO-ARCH-SVC-015`：只允许直接继承 canonical MangoCrudServiceImpl，否则直接继承 Object；`MANGO-ARCH-SVC-016`：`IXxxService` 只继承 canonical Mango CRUD contract。
- `MANGO-ARCH-BEAN-001`：业务 `IXxxService` 实现要求 `@Service`；`MANGO-ARCH-BEAN-002`：普通框架 `XxxService` 要求 starter `@Bean + @ConditionalOnMissingBean`；`MANGO-ARCH-BEAN-003`：禁止 `@Service` 与 `@Bean` 双重注册；`MANGO-ARCH-BEAN-004`：Controller/业务 Service 禁止直接构造托管 Service；`MANGO-ARCH-BEAN-005`：事务、异步、调度和缓存注解要求 Spring Bean 注册证明；`MANGO-ARCH-BEAN-006`：禁止可变 static Service Locator。
- `MANGO-ARCH-MAPPER-001`：禁止注解/Provider SQL；`MANGO-ARCH-MAPPER-002`：入参禁止 API model；`MANGO-ARCH-MAPPER-003`：返回禁止 API model；`MANGO-ARCH-MAPPER-004`：必须是 `@Mapper` interface；`MANGO-ARCH-MAPPER-005`：直接继承 `BaseMapper<XxxEntity>`；`MANGO-ARCH-MAPPER-006`：Mapper/Entity 聚合名一致；`MANGO-ARCH-MAPPER-007`：入参与返回禁止 Object、Map、transport、Controller、FeignClient、Service 等非类型化边界。
- `MANGO-ARCH-ENTITY-001`：持久化类命名 `XxxEntity`；`MANGO-ARCH-ENTITY-002`：要求非空 `@TableName`；`MANGO-ARCH-ENTITY-003`：普通 Entity 继承 canonical `TenantEntity`；`MANGO-ARCH-ENTITY-004`：全局 Entity manifest 表名与 `@TableName` 一致。

### 8.4 Feign、Adapter 与模块依赖

- `MANGO-ARCH-FEIGN-001`：Feign 只位于 starter-remote；`MANGO-ARCH-FEIGN-002`：直接且只继承一个 Api；`MANGO-ARCH-FEIGN-003`：name/contextId 非空；`MANGO-ARCH-FEIGN-004`：path 为绝对路径；`MANGO-ARCH-FEIGN-005`：contextId 唯一；`MANGO-ARCH-FEIGN-006`：contextId 等于接口名 lowerCamelCase；`MANGO-ARCH-FEIGN-007`：name/path 匹配 module.properties；`MANGO-ARCH-FEIGN-008`：逐项重声明 Api mapping、保留泛型且禁止 Api 外方法；`MANGO-ARCH-FEIGN-009`：禁止字段/常量，方法保持 abstract。
- `MANGO-ARCH-ADAPTER-001`：Controller/Feign 的 verb、完整 path、参数绑定一致；`MANGO-ARCH-ADAPTER-002`：每个参数恰有一个显式 binding，query/header 显式命名；`MANGO-ARCH-ADAPTER-003`：恰有一个 verb、最多一个 path；`MANGO-ARCH-ADAPTER-004`：方法使用具体 verb annotation，禁止方法级 RequestMapping；`MANGO-ARCH-ADAPTER-005`：Command/Request body 必填；`MANGO-ARCH-ADAPTER-006`：禁止 mapping 条件；`MANGO-ARCH-ADAPTER-007`：verb/model/binding 兼容，Controller Query 用 ParameterObject/ModelAttribute，Feign Query 用 SpringQueryMap。
- `MANGO-ARCH-DEP-001`：api 禁止依赖 core/support/starter；`MANGO-ARCH-DEP-002`：core 禁止依赖 core/starter；`MANGO-ARCH-DEP-003`：starter-remote 只依赖本域 api/support 和 infra-feign；`MANGO-ARCH-DEP-004`：starter-remote 禁止直依赖 OpenFeign starter；`MANGO-ARCH-DEP-005`：非 resource 模块禁止 resource runtime；`MANGO-ARCH-DEP-006`：support 禁止依赖 core/starter；`MANGO-ARCH-DEP-007`：starter 只依赖本域实现和明确 infra starter；`MANGO-ARCH-DEP-008`：app 只依赖 Mango starter 与 mango-common。

### 8.5 引擎 Fail-Closed 与执行边界

- `MANGO-ARCH-ENGINE-001`：Reactor 不可读；`MANGO-ARCH-ENGINE-002`：ArchUnit 导入失败；`MANGO-ARCH-ENGINE-003`：compiled classes 缺失/为空；`MANGO-ARCH-ENGINE-004`：class 无法映射 Reactor；`MANGO-ARCH-ENGINE-005`：Java source 缺失/不可扫描；`MANGO-ARCH-ENGINE-006`：PMD 失败；`MANGO-ARCH-ENGINE-007`：报告不可写；`MANGO-ARCH-ENGINE-008`：Git base 不可解析；`MANGO-ARCH-ENGINE-009`：module.properties 无效/冲突；`MANGO-ARCH-ENGINE-010`：PMD classpath 缺失；`MANGO-ARCH-ENGINE-011`：Reactor artifact 不完整；`MANGO-ARCH-ENGINE-012`：class 目录无法映射 artifact。
- `MANGO-ARCH-ENGINE-013`：禁止 excludedModules；`MANGO-ARCH-ENGINE-014`：全局 Entity manifest 缺失或合同/审批/期限无效；`MANGO-ARCH-ENGINE-015`：禁止 `mango.architecture.skip=true`；`MANGO-ARCH-ENGINE-016`：治理构建禁止 `requireFullReactor=false`；`MANGO-ARCH-ENGINE-017`：业务 Java 必须声明 package 且不得冒用 Mango/Spring/MyBatis/Jakarta 保留命名空间。
- 正式入口为 Reactor `mvn verify`。`mango:check` 同时固定 `rule=all`、`gate=all`、`staticFailurePolicy=block`、`changedOnly=false`、execution-root baseDir；`codeLevelExcludedModules`、缩小 rule/gate/baseDir 或把静态失败改为 report 都会阻断。
- `MODULE_INFO` 校验模块元数据；`PERSISTENCE_SCHEMA` 校验 migration、Entity 与 Mapper XML 表归属；聚合静态分析执行 PMD/P3C、Checkstyle、SpotBugs。
- 正例：完整生成项目执行 `mvn clean verify` 并产出 architecture/quality report。反例：只编译 core、changed-only 或排除模块后声明通过。
- 门禁只对上述静态事实承诺确定性。Require 不证明业务条件正确；Mapper/XML 不证明 SQL 业务语义；Controller/Feign parity 不替代权限、租户、异常和真实链路测试。任何失败不得由 Agent 自报、普通 baseline 或文档例外覆盖。
