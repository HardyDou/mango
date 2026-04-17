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
- `starter` 负责实现 `XxxApi` 和自动装配。
- `starter-remote` 负责远程调用适配。

## 4. 依赖规则

- `app` 依赖 `starter` 或 `starter-remote`。
- `core` 只依赖本域 `api` 和其他域 `api`。
- `starter` 依赖本域 `api` 和本域 `core`。
- `starter-remote` 只依赖本域 `api`。
- `api` 不依赖业务实现。

## 5. 边界规则

- `api` 不放 `Entity`、`Mapper`、`Controller`。
- `core` 不放 `Controller`。
- `Mapper` 禁止跨域访问其他模块表。
- 跨域调用必须走 `XxxApi`。
- 应用装配层不承载领域实现细节。

## 6. 命名规则

- 对外接口使用 `XxxApi`。
- 模块内部服务使用 `IXxxService`。
- DIP 接口使用 `I...Provider`、`I...Checker`、`I...Validator`。

## 7. 禁止事项

- 直接依赖其他域 `core`
- 在 `api` 放数据库对象
- 在 `core` 放对外接口实现
- 在应用装配层堆业务代码
