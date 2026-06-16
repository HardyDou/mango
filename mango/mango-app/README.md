# Mango App

## 1. 概览
`mango-app` 定义 Mango 后端可部署 Spring Boot 启动入口。它不实现业务能力，只负责把 `mango-platform`、`mango-infra` 和业务 starter 装配成可运行进程。

业务开发者主要在这里决定：本地开发用单体还是微服务、业务 starter 加到哪个 app、运行时配置写在哪里、启动后从哪里确认菜单和接口可用。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 本地启动完整 Mango 后端 | Maven 依赖 / HTTP API / Java API |
| 选择单体拓扑做业务开发和联调 | Maven 依赖 / HTTP API / Java API |
| 选择微服务拓扑拆分网关、平台服务、业务服务和文件预览服务 | Maven 依赖 / HTTP API / Java API |
| 为新增业务模块提供部署宿主 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 本地启动完整 Mango 后端。
- 选择单体拓扑做业务开发和联调。
- 选择微服务拓扑拆分网关、平台服务、业务服务和文件预览服务。
- 为新增业务模块提供部署宿主。

## 4. 边界说明
- 不在 app 模块写业务 Controller、Service、Mapper、Entity 或 migration。
- 不在 app 模块放长期领域规则。
- 不把微服务调用方依赖本地 `core` 实现；调用方应使用 `starter-remote`。

## 5. 模块组成
| 模块 | 默认端口 | 职责 |
|------|----------|------|
| `monolith/mango-monolith-app` | `5555` | 单进程装配完整管理后台能力。 |
| `microservice/mango-gateway-app` | `8080` | 网关、路由、边界鉴权和网关资源同步。 |
| `microservice/mango-platform-app` | `8081` | 平台能力提供方，装配 auth、identity、authorization、system、file、workflow、payment 等本地 starter。 |
| `microservice/mango-business-app` | `8082` | 业务服务宿主，默认依赖平台能力 remote starter。 |
| `microservice/mango-file-preview-app` | `8083` | 文件预览独立部署入口。 |

app 只控制进程边界和依赖装配。能力的配置、API、migration、菜单和权限仍看各模块 README。

## 6. 接入方式
本地单体启动：

```bash
mvn -f mango/pom.xml -pl :mango-monolith-app -am spring-boot:run
```

本地统一环境脚本：

```bash
scripts/dev-env.sh .env.development backend
scripts/dev-env.sh .env.development all
```

微服务分别启动：

```bash
mvn -f mango/pom.xml -pl :mango-gateway-app -am spring-boot:run
mvn -f mango/pom.xml -pl :mango-platform-app -am spring-boot:run
mvn -f mango/pom.xml -pl :mango-business-app -am spring-boot:run
```

新增业务 starter：

- 单体：加入 `mango-monolith-app` 或业务自己的单体 app。
- 微服务提供方：加入 `mango-business-app`，使用本地 `*-starter`。
- 微服务调用方：加入调用服务，使用 `*-starter-remote`。

## 7. 配置说明
| 配置位置 | 字段 | 含义 |
|----------|------|------|
| `.env.development` | `MANGO_BACKEND_PORT` | 单体本地端口。 |
| `.env.development` | `MANGO_DB_URL`、`MANGO_DB_USERNAME`、`MANGO_DB_PASSWORD` | 单体本地数据库。 |
| `.env.development` | `MANGO_FILE_ROOT` | 本地文件存储根目录。 |
| `.env.development` | `MANGO_CRYPTO_SM4_SECRET_KEY` | SM4 密钥，`scripts/dev-workspace.sh` 可生成。 |
| `mango-monolith-app/application.yml` | `mango.persistence.flyway.modules.*.enabled` | 控制单体各能力模块 migration。 |
| `mango-gateway-app/application.yml` | `spring.cloud.gateway.routes` | 微服务路由到 platform 和 business。 |
| `mango-platform-app/application.yml` | YAML 中 `mango` -> `flyway` -> `enabled` | 当前微服务示例默认关闭 Flyway。 |
| `mango-business-app/application.yml` | YAML 中 `mango` -> `flyway` -> `enabled` | 当前业务服务示例默认关闭 Flyway。 |

微服务示例默认使用 H2 内存库并关闭 Flyway，更适合拓扑验证。真实部署需要改为 MySQL、打开目标模块 migration，并补齐注册发现、配置中心、网关路由和安全配置。

## 8. API 与扩展
app 层对外暴露的是 Spring Boot 进程和路由，不暴露复用 Java API。

常见验证入口：

- `/actuator/health`：健康检查。
- `/v3/api-docs`、`/swagger-ui.html`：平台 app 和单体的 OpenAPI 文档入口。
- `/auth/**`、`/identity/**`、`/authorization/**`、`/system/**`：平台能力接口。
- `/biz/**`、`/business/**`：微服务业务路由示例。

## 9. 数据与初始化
app 层不拥有业务表。数据库和初始化来自被装配的 starter：

- 单体 `mango-monolith-app` 默认打开 system、authorization、identity、org、captcha、file、template、workflow、job、kv、notice、calendar、numgen、payment、domain 等模块 migration。
- 微服务示例 app 当前默认关闭 Flyway，需要真实联调时按服务边界打开对应模块 migration。
- 菜单和权限初始化来自 authorization 及各业务模块 migration 或初始化器。

新增业务模块时，必须确认业务 starter 被 app 引入，否则 migration、Runner、Initializer、Controller 和 Service 都不会进入运行时。

## 10. 管理入口
app 层只装配权限能力。菜单、权限和租户数据由 `mango-authorization`、`mango-system` 和业务模块初始化。

拓扑选择对权限的影响：

- 单体：本地 starter 直接提供权限、菜单和业务接口。
- 微服务平台 app：提供 auth、identity、authorization、system 等平台接口。
- 微服务业务 app：通过 remote starter 调用平台能力，并同步自己的业务资源。
- 网关：通过 `mango-access-gateway-starter` 做边界鉴权和路由。

## 11. 快速开始
1. 本地开发优先选择单体，减少服务间调用和配置复杂度。
2. 业务模块完成 `api/core/starter` 拆分后，把本地 starter 加入单体或业务服务。
3. 如果业务服务需要调用平台能力，加入对应 `starter-remote`。
4. 在 app 配置中打开业务模块 migration 和必要能力开关。
5. 启动 app，验证数据库表、菜单、权限、接口和前端页面。
6. 再根据部署需要拆成网关、平台服务、业务服务和文件预览服务。

## 12. 问题排查
- 业务接口 404：检查 starter 是否加到当前启动 app。
- 表没创建：检查 migration 是否随 starter 进入 classpath，Flyway 模块开关是否打开。
- 单体能用、微服务不能用：检查调用方是否使用 remote starter、网关路由是否覆盖目标路径。
- 菜单初始化了但页面打不开：检查前端页面 key 和菜单 component 是否一致。
- 微服务示例没有真实数据：示例默认 H2 和关闭 Flyway，真实联调要改配置。

## 13. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [后端测试规范](../../mango-pmo/rules/backend/08-test.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [单体部署入口](./monolith/mango-monolith-app/README.md)
- [微服务部署入口](./microservice/README.md)
- [Mango 后端根 README](../README.md)
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
