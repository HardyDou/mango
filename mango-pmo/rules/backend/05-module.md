# 模块分层规范

## 1. 分层

- `mango-app`：应用装配层。
- `mango-platform`：业务平台能力。
- `mango-infra`：基础设施能力。
- `mango-tools`：研发工具。

## 2. 业务域模块

每个业务域按需要拆成：

- `api`：接口契约
- `support`：仅供本域 `core` / `starter-remote` 复用的内部共享技术实现
- `core`：核心业务
- `starter`：本地装配与对外暴露
- `starter-remote`：远程调用适配

## 3. 子模块职责

- `api` 只放契约模型和 `XxxApi`。
- `support` 只放本域内部共享实现，不对业务模块暴露。
- `core` 只放业务实现、实体、Mapper、转换。
- `starter` 负责由 `XxxController` 实现 `XxxApi`、自动装配、模块信息声明。
- `starter-remote` 负责远程调用适配和模块信息解析。

## 4. 依赖规则

- `app` 依赖 `starter` 或 `starter-remote`。
- `core` 只依赖本域 `api` 和其他域 `api`。
- `core` 可依赖本域 `support`。
- `starter` 依赖本域 `api` 和本域 `core`。
- `starter-remote` 在 `io.mango` 依赖中只允许本域 `api` 和本域 `support`。
- `starter-remote` 的 Feign、Spring、Web 等技术依赖使用外部框架坐标，不通过其他 Mango starter 透传。
- `api` 不依赖业务实现。

## 5. 边界规则

- `api` 不放 `Entity`、`Mapper`、`Controller`。
- `api` 不放 `@FeignClient`。
- `api` 只放其它模块会直接或间接依赖的契约类。
- `support` 不放业务契约、Controller、Feign adapter。
- 其它模块直接依赖包括：注入、继承、实现、方法签名、远程调用契约。
- 本地实现协作用类型，例如 `*Service`、`*Manager`、`*Registry`、`*Session`、`*Dispatcher`，禁止放 `api`。
- Controller 对外接口契约统一声明为 `XxxApi`。
- `XxxController` 必须实现对应 `XxxApi`，只能持有 `IXxxService` 或等效内部服务接口，禁止持有 `XxxApi` 自调用。
- `XxxFeignClient` 必须实现对应 `XxxApi`，只能放在 `starter-remote`。
- `XxxService` 必须实现 `IXxxService`，禁止直接实现 `XxxApi`。
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
- Feign adapter 必须继承本域 `XxxApi`。
- `@FeignClient(name = "...")` 必须填写目标模块 `module-name`。
- `@FeignClient(path = "...")` 必须以目标模块 `module-path` 开头。
- Feign 请求必须通过模块信息解析真实服务名和 contextPath。
- Feign adapter 禁止硬编码真实服务名。
- Feign adapter 禁止硬编码目标服务 contextPath。
- 正向调用路径统一使用 `/{module-path}/...`。
- 反向调用路径统一使用 `/_{module-path}/...`。
- `_` 只表示调用方向反转，不表示 internal / external。
- 禁止新增 `/internal/...` 作为模块规范路径。

## 8. Controller Path 规则

- 本地 `starter` 的正向 Controller 根路径必须以 `module-path` 开头。
- 本地 `starter` 如需接收其它同类节点的反向调用，Controller 根路径允许以 `/_{module-path}` 开头。
- `starter-remote` 中如果暴露反向接收 Controller，根路径必须以 `/_{module-path}` 开头。
- path 只表达模块归属和调用方向，不表达内外部语义。

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
- 使用 `/internal/...` 充当模块标准 path
- 让管理后台承载模块信息采集核心逻辑

## 11. 相关规范

- 模块测试放置位置、测试分层和有效测试判断，遵循 `mango-pmo/rules/backend/08-test.md`。
