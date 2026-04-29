# Sprint 15 实施输入

## 1. Sprint 15 必须遵守的规则

| Rule ID | Rule | Enforcement |
|---|---|---|
| BE-API-020 | `*-api` 禁止声明 `@FeignClient` | 自动 |
| BE-API-022 | API 契约禁止硬编码服务发现名 | 自动 |
| BE-MOD-003 | `starter` 负责实现 `XxxApi`、自动装配、模块元数据声明 | 半自动 |
| BE-MOD-004 | `starter-remote` 负责远程调用适配和模块信息解析 | 半自动 |
| BE-MOD-007 | `starter-remote` 在 `io.mango` 依赖中只允许本域 `api`、本域 `support` 和 `mango-infra-feign-starter`，Feign 禁止直连 Spring Cloud OpenFeign | 自动 |
| BE-MOD-009 | `api` 不放 `@FeignClient` | 自动 |
| BE-MOD-012 | 远程目标服务不得写死在业务代码中 | 自动 |
| BE-MOD-013 | 远程目标必须通过模块信息解析 | 半自动 |
| BE-MOD-014 | 模块信息维护属于 `mango-infra` | 人工 |
| BE-MOD-015 | 每个本地 `starter` 必须声明自身模块名 | 自动 |

## 2. Sprint 15 代码边界

| Module | Responsibility |
|---|---|
| `mango-infra-module` | 提供模块信息 API、核心模型、注册表实现、自动装配 |
| `mango-infra-module-starter` | 扫描模块元数据并采集当前服务信息 |
| 各业务 `starter` | 提供 `META-INF/mango/module.properties` |
| 各业务 `starter-remote` | 通过模块信息解析远程目标 |
| 各业务 `api` | 保持纯契约，不引入 Feign |
| `mango-admin` | 后续只展示、管理、运维模块信息 |

## 3. 第一阶段实现范围

| Item | Requirement |
|---|---|
| Module API | 定义模块信息注册、查询接口 |
| Module Model | 包含模块名、服务名、contextPath、来源 |
| Memory Registry | 支持单进程聚合部署 |
| Config Override | 支持配置文件覆盖模块到服务的映射 |
| Starter Metadata | 每个 `starter` 提供模块元数据文件 |
| Remote Adapter | `starter-remote` 通过模块信息解析目标服务 |
| Feign Cleanup | `*-api` 移除 `@FeignClient` |

## 4. 明确不做

| Item | Reason |
|---|---|
| Redis 后端 | 第一阶段不引入额外基础设施 |
| DB 后端 | 管理后台展示前再设计持久化 |
| Nacos 后端 | 等服务治理边界稳定后再接入 |
| Raft / Gossip / Blockchain | 超出当前项目复杂度，不作为默认方案 |
| 管理后台页面 | Sprint 15 只做模块信息基础能力 |

## 5. 验收命令

| Scope | Command |
|---|---|
| 工具检查 | `mvn mango:check -Drule=api-contract -DbaseDir=<repo>/mango` |
| 工具检查 | `mvn mango:check -Drule=module-info -DbaseDir=<repo>/mango` |
| 工具检查 | `mvn mango:check -Drule=remote-adapter -DbaseDir=<repo>/mango` |
| 工具检查 | `mvn mango:check -Drule=dependency -DbaseDir=<repo>/mango` |
| 单元测试 | `mvn test -pl <changed-modules>` |
| 集成验证 | `mvn verify -pl <changed-modules> -am` |

## 6. Sprint 15 开工条件

- `BE-API-020`、`BE-API-022`、`BE-MOD-012` 必须先有扫描能力。
- `starter` 模块元数据格式必须固定为 `META-INF/mango/module.properties`。
- `starter-remote` 只允许通过模块信息解析远程目标。
- `api` 模块保持纯契约，不能为了远程调用引入 Feign。
