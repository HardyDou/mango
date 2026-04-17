# 模块分层规范

## 1. 分层

- `mango-app`：应用装配层。
- `mango-platform`：业务平台能力。
- `mango-infra`：基础设施能力。
- `mango-tools`：研发工具。

## 2. 业务域模块

每个业务域按需要拆成：

- `api`：接口契约
- `core`：核心业务
- `starter`：本地装配与对外暴露
- `starter-remote`：远程调用适配

## 3. 子模块职责

- `api` 只放契约模型和 `XxxApi`。
- `core` 只放业务实现、实体、Mapper、转换。
- `starter` 负责实现 `XxxApi`、自动装配、模块信息声明。
- `starter-remote` 负责远程调用适配和模块信息解析。

## 4. 依赖规则

- `app` 依赖 `starter` 或 `starter-remote`。
- `core` 只依赖本域 `api` 和其他域 `api`。
- `starter` 依赖本域 `api` 和本域 `core`。
- `starter-remote` 业务依赖只允许本域 `api`。
- `starter-remote` 技术依赖允许 `mango-infra-feign-starter`。
- `api` 不依赖业务实现。

## 5. 边界规则

- `api` 不放 `Entity`、`Mapper`、`Controller`。
- `api` 不放 `@FeignClient`。
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
- 每个本地 `starter` 必须提供 `META-INF/mango/module.properties`。
- `module.properties` 必须声明 `module-name`。
- `module-name` 必须是 Mango 模块名，禁止使用别名。
- `mango-app` 必须依赖 `mango-infra-module-starter`。
- `mango-infra-module-starter` 负责自动采集模块名、服务名和 contextPath。
- 服务名必须读取当前服务注册名。
- contextPath 必须读取当前服务上下文路径。
- 配置覆盖必须使用 `mango.module.module-service`。
- 禁止新增或使用 `mango.remote.*`。
- 管理后台只负责查看、管理和运维模块信息。
- 管理后台不承载模块信息采集核心逻辑。

## 7. Remote Adapter 规则

- Feign adapter 必须放在 `starter-remote`。
- Feign adapter 必须继承本域 `XxxApi`。
- `@FeignClient(name = "...")` 必须填写目标模块名。
- Feign 请求必须通过模块信息解析真实服务名和 contextPath。
- Feign adapter 禁止硬编码真实服务名。
- Feign adapter 禁止硬编码目标服务 contextPath。

## 8. 命名规则

- 对外接口使用 `XxxApi`。
- 模块内部服务使用 `IXxxService`。
- DIP 接口使用 `I...Provider`、`I...Checker`、`I...Validator`。

## 9. 禁止事项

- 直接依赖其他域 `core`
- 在 `api` 放数据库对象
- 在 `api` 放 `@FeignClient`
- 在 `core` 放对外接口实现
- 在应用装配层堆业务代码
- 在 `starter-remote` 中硬编码服务发现名
- 让管理后台承载模块信息采集核心逻辑
