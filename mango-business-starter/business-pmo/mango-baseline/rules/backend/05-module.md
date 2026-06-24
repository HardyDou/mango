# 模块分层规范

## 1. 分层

- `mango-app`：应用装配层。
- `mango-platform`：业务平台能力。
- `mango-infra`：基础设施能力。
- `mango-tools`：研发工具。

## 2. 模块角色

每个能力模块按需要拆成：

- `api`：对外接口契约。
- `support`：对外复用支撑能力，不承载接口契约、持久化和自动装配。
- `core`：核心业务实现和持久化。
- `starter`：本地 Spring Boot 装配与对外暴露。
- `starter-*`：特定运行模式或外部系统适配装配，例如 `starter-remote`、`sync-starter`。

## 3. 子模块职责

- `api` 放 `XxxApi`、Command、Query、VO、Enum、注解、SPI 接口和其它对外接口契约。
- `support` 放其它模块 `core` 可复用的非接口契约能力，例如默认实现、扫描器、注册表、执行器、工具和适配辅助。
- `core` 放业务实现、实体、Mapper、转换、内部服务和持久化逻辑。
- `starter` 负责由 `XxxController` 实现 `XxxApi`、自动配置、Bean 注册、扫描、模块信息声明和运行时装配。
- `starter-remote` 负责远程调用适配、Feign adapter 和模块信息解析。
- 其它 `starter-*` 负责特定运行模式的自动配置和运行时装配。

## 4. 依赖规则

- `app` 依赖 `starter` 或具体 `starter-*`。
- `core` 只依赖其它模块 `api` 或 `support`。
- `starter` 依赖本域 `api` 和本域 `core`。
- `starter-remote` 在 `io.mango` 依赖中只允许本域 `api`、本域 `support` 和 `mango-infra-feign-starter`。
- `starter-remote` 的 Feign 能力必须通过 `mango-infra-feign-starter` 引入，禁止直接依赖 `spring-cloud-starter-openfeign`；其它 Spring、Web 等技术依赖按需使用外部框架坐标。
- 安全入口类聚合模块例外：`mango-security-starter-remote` 只允许聚合 `mango-infra-security-starter`、`mango-auth-starter-remote`、`mango-identity-starter-remote`、`mango-authorization-starter-remote`，不得新增业务实现。
- `api` 不依赖 `support`、`core`、`starter` 或 `starter-*`。
- `support` 只依赖其它模块 `api`、其它模块 `support`、`mango-common` 和必要第三方库。
- `support` 禁止依赖任何 `core`、`starter` 或 `starter-*`。
- `mango-infra` 的轻量基础设施契约应拆到 `*-api`，例如注解、实体基类、分页模型、运行期上下文句柄和 Provider/Resolver 接口。
- `mango-infra` 的 `*-starter` 只承载自动配置、Spring Boot 装配、扫描器、运行时实现、Flyway/MyBatis 等具体基础设施。
- `core` 可依赖 infra/platform/business `*-api` 和 `*-support`，禁止为了使用轻量契约或复用能力依赖 `*-core`、`*-starter` 或 `starter-*`。
- 已存在的 `core -> *-starter` 历史依赖必须通过拆分契约到 `*-api` 后逐模块迁移，禁止放松 checker 规则。
- Resource Registry 是平台能力注册边界；非 `mango-resource` 模块默认只能依赖 `mango-resource-api`，禁止直接依赖 `mango-resource-core`、`mango-resource-support`、`mango-resource-starter`、`mango-resource-sync-starter` 或 `mango-resource-starter-remote`。确需例外时，必须人工明确确认并通过 `mango:check` 参数 `-Dmango.check.resourceStarterDependencyExceptions=<artifactId>=<reason>` 记录充分理由。

## 5. 边界规则

- `api` 不放 `Entity`、`Mapper`、`Controller`。
- `api` 不放 `@FeignClient`。
- `api` 只放其它模块会直接或间接依赖的契约类。
- `support` 不放业务接口契约、Controller、Feign adapter。
- `support` 不放 `@AutoConfiguration`、`AutoConfiguration.imports` 或 `module.properties`。
- `support` 不放持久化内容，包括 `Entity`、`Mapper`、`BaseMapper`、`ServiceImpl`、`MangoCrudServiceImpl`、`@TableName`、`db/migration`、MyBatis/Flyway/DataSource/JDBC/JdbcTemplate 接入。
- 其它模块直接依赖包括：注入、继承、实现、方法签名、远程调用契约。
- 本地实现协作用类型，例如 `*Service`、`*Manager`、`*Registry`、`*Session`、`*Dispatcher`，禁止放 `api`。
- Controller 对外接口契约统一声明为 `XxxApi`。
- `XxxController` 必须实现对应 `XxxApi`，只能持有 `IXxxService` 或等效内部服务接口，禁止持有 `XxxApi` 自调用。
- `XxxFeignClient` 必须实现对应 `XxxApi`，只能放在 `starter-remote`。
- `XxxService` 必须实现 `IXxxService`，禁止直接实现 `XxxApi`。
- `IXxxService` 和 `XxxService` 方法签名必须遵守后端 API 规范中的 Service 入参规则。
- `core` 不放 `Controller`。
- `Mapper` 禁止跨域访问其他模块表。
- 跨域调用必须走 `XxxApi`。
- 应用装配层不承载领域实现细节。
- 本地聚合部署优先注入本地 `starter` 实现。
- 远程部署通过 `starter-remote` 调用。
- 远程目标服务不得写死在业务代码中。
- 远程目标必须通过模块信息解析。

## 6. 模块信息规则

- 模块信息维护属于 `mango-infra-module`。
- 只有本地 `starter` 可以提供 `META-INF/mango/module.properties`。
- `api`、`core`、`starter-remote` 禁止声明 `module.properties`。
- `module.properties` 必须声明 `module-name`、`module-path`。
- `module-name` 必须是 Mango 模块名，禁止使用别名。
- `module-path` 必须在全仓唯一，禁止跨模块重合。
- `module-path` 表示模块正向调用前缀。
- `mango-app` 必须依赖 `mango-infra-module-starter`。
- `mango-infra-module-starter` 负责自动采集模块名、服务名和当前服务真实 contextPath。
- 服务名必须读取当前服务注册名。
- contextPath 必须读取当前服务上下文路径。
- 配置覆盖必须使用 `mango.module.module-service`。
- 禁止新增或使用 `mango.remote.*`。
- 管理后台只负责查看、管理和运维模块信息。
- 管理后台不承载模块信息采集核心逻辑。

## 7. Remote Adapter 规则

- Feign adapter 必须放在 `starter-remote`。
- Feign adapter 必须继承本域唯一一个 `XxxApi`，禁止一个 Feign adapter 继承多个 API。
- `@FeignClient(name = "...")` 必须填写目标模块 `module-name`。
- `@FeignClient(contextId = "...")` 必须显式声明当前 Feign adapter 身份，默认使用 Feign 接口名 lowerCamelCase，例如 `AuthorizationFeignClient` 使用 `authorizationFeignClient`。
- 同一个 `starter-remote` 中相同 `name` 的多个 Feign adapter 必须使用不同 `contextId`。
- `@FeignClient(path = "...")` 必须以目标模块 `module-path` 开头。
- Feign 请求必须通过模块信息解析真实服务名和 contextPath。
- Feign adapter 禁止硬编码真实服务名。
- Feign adapter 禁止硬编码目标服务 contextPath。
- 正向调用路径统一使用 `/{module-path}/...`。
- 反向调用路径统一使用 `/_{module-path}/...`。
- `_` 只表示调用方向反转，不表示 internal / external。

## 8. Controller Path 规则

- 本地 `starter` 的正向 Controller 根路径必须以 `module-path` 开头。
- 本地 `starter` 如需接收其它同类节点的反向调用，Controller 根路径允许以 `/_{module-path}` 开头。
- `starter-remote` 中如果暴露反向接收 Controller，根路径必须以 `/_{module-path}` 开头。
- path 只表达模块归属和调用方向，不表达内外部语义。

## 8.1 Web 边界规则

- `mango-infra-web-api` 只放 Web 边界轻量契约，例如 `@Inner`、请求上下文 provider 接口。
- `mango-infra-web-starter` 只放 Spring Web 自动配置、Filter、扫描器、Servlet 实现和异常处理。
- `*-api` 如需声明内部接口，只允许依赖 `mango-infra-web-api` 并使用 `@Inner`；禁止依赖 `mango-infra-web-starter`。
- 暴露 HTTP Controller 的本地 `starter` 应依赖 `mango-infra-web-starter`，由它统一承接 Spring Web 基础设施。
- 依赖 `mango-infra-web-starter` 的模块禁止重复声明 `spring-boot-starter-web`。
- 已完成 Web 边界收敛的 `common`、`infra-kv`、`infra-realtime`、`infra-web` 不得直接依赖 `spring-boot-starter-web`。
- Spring Boot 原生 Web 配置继续使用 `server.*`、`spring.mvc.*`、`spring.web.*`、`spring.servlet.multipart.*`、`spring.jackson.*` 等前缀。
- Mango 只新增项目横切配置，例如 `mango.web.cors.*`、`mango.web.inner.*`、`mango.web.mdc.*`、`mango.web.request-context.*`，禁止重新包装 Spring Boot 已有通用配置。
- `@Inner` 只表示内部访问保护，不表示登录、权限、租户或业务可见性。
- 内部路径由 `@Inner` 扫描、模块 provider、数据库 provider 等来源聚合，禁止只靠 URL 前缀判定内外路径。

## 9. 命名规则

- 对外接口使用 `XxxApi`。
- 模块内部服务使用 `IXxxService`。
- 对外 API 名称只表达跨模块能力，禁止把 `Registry`、`Dispatcher`、`Manager`、`Session` 等内部实现词放入 `XxxApi`。
- `XxxController` / `XxxFeignClient` 是 `XxxApi` 实现；`XxxService` 是 `IXxxService` 实现。
- DIP 接口使用 `I...Provider`、`I...Checker`、`I...Validator`。

## 10. 禁止事项

- 直接依赖其他域 `core`
- 在 `api` 放数据库对象
- 在 `api` 放 `@FeignClient`
- 在 `core` 放对外接口实现
- 在应用装配层堆业务代码
- 在 `starter-remote` 中硬编码服务发现名
- 在 `starter-remote` 声明模块信息
- 在非 `starter` 模块声明 `module.properties`
- 让管理后台承载模块信息采集核心逻辑

## 11. 相关规范

- 模块测试放置位置、测试分层和有效测试判断，遵循 `mango-pmo/rules/backend/08-test.md`。
