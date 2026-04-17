# Sprint 15 实施输入

## 1. Sprint 15 必须遵守的规则

| Rule ID | Rule | Enforcement |
|---|---|---|
| BE-API-020 | `*-api` 禁止声明 `@FeignClient` | 自动 |
| BE-API-022 | API 契约禁止硬编码服务发现名 | 自动 |
| BE-MOD-003 | `starter` 负责实现 `XxxApi`、自动装配、能力自动注册 | 半自动 |
| BE-MOD-004 | `starter-remote` 负责远程调用适配和能力解析 | 半自动 |
| BE-MOD-007 | `starter-remote` 只依赖本域 `api` | 自动 |
| BE-MOD-009 | `api` 不放 `@FeignClient` | 自动 |
| BE-MOD-012 | 远程目标服务不得写死在业务代码中 | 自动 |
| BE-MOD-013 | 远程目标必须通过能力注册表或配置解析 | 半自动 |
| BE-MOD-014 | 服务能力维护属于 `mango-infra` | 人工 |
| BE-MOD-015 | 每个 `starter` 必须注册自己提供的能力 | 半自动 |

## 2. Sprint 15 代码边界

| Module | Responsibility |
|---|---|
| `mango-infra` | 提供能力注册 API、核心模型、注册表实现、自动装配 |
| `mango-infra-*-starter` | 装配能力注册默认实现 |
| 各业务 `starter` | 注册自身提供的 `XxxApi` 能力 |
| 各业务 `starter-remote` | 通过能力注册表或配置解析远程目标 |
| 各业务 `api` | 保持纯契约，不引入 Feign |
| `mango-admin` | 后续只展示、管理、运维能力注册数据 |

## 3. 第一阶段实现范围

| Item | Requirement |
|---|---|
| Registry API | 定义能力注册、查询、刷新接口 |
| Capability Model | 包含能力名、提供模块、服务名、基础路径、部署模式 |
| Memory Registry | 支持单进程聚合部署 |
| Config Registry | 支持配置文件声明服务目标 |
| Starter Registration | 每个 `starter` 自动注册自身能力 |
| Remote Adapter | `starter-remote` 通过注册表解析目标服务 |
| Feign Cleanup | `*-api` 移除 `@FeignClient` |

## 4. 明确不做

| Item | Reason |
|---|---|
| Redis Registry | 第一阶段不引入额外基础设施 |
| DB Registry | 管理后台展示前再设计持久化 |
| Nacos Registry | 等服务治理边界稳定后再接入 |
| Raft / Gossip / Blockchain | 超出当前项目复杂度，不作为默认方案 |
| 管理后台页面 | Sprint 15 只做能力基础设施 |

## 5. 验收命令

| Scope | Command |
|---|---|
| 工具检查 | `mvn mango:check -Drule=api-contract -Dmode=report` |
| 工具检查 | `mvn mango:check -Drule=module-boundary -Dmode=report` |
| 工具检查 | `mvn mango:check -Drule=remote-adapter -Dmode=report` |
| 单元测试 | `mvn test -pl <changed-modules>` |
| 集成验证 | `mvn verify -pl <changed-modules> -am` |

## 6. Sprint 15 开工条件

- `BE-API-020`、`BE-API-022`、`BE-MOD-012` 必须先有扫描能力。
- `starter` 能力注册格式必须先固定。
- `starter-remote` 只允许通过能力注册表或配置解析远程目标。
- `api` 模块保持纯契约，不能为了远程调用引入 Feign。

