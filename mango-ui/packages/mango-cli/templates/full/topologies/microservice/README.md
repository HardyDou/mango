# Microservice Topology

## 1. 概览
`topologies/microservice` 说明生成业务项目在微服务模式下的拆分边界。微服务模式把业务服务、调用方、网关和前端后台分开治理：服务提供方依赖 `<module>-starter`，服务调用方依赖 `<module>-starter-remote`，前端通过网关访问后端 API。

核心判断：调用方不能依赖对方 `<module>-core`。

## 2. 适用场景
- 业务模块需要独立部署、独立扩缩容或独立发布。
- 调用方通过 Feign 或网关访问业务服务。
- 需要验证认证、租户、链路追踪、超时、重试和降级。
- 菜单、权限、API 资源需要按服务提供方同步。
- 前端后台需要连接真实网关 API，而不是只连本地单体后端。

## 3. 边界说明
- 当前只需要一个后端进程交付。
- 模块之间大量共享事务和数据库表，尚未完成服务边界设计。
- 调用方必须依赖业务模块 `core` 才能调用；这说明 API 边界还没抽出来。
- 没有服务注册、配置中心、网关、日志和发布平台准备。
- 只是为了“看起来像微服务”而拆分。

## 4. 模块组成
服务提供方：

- 依赖 `<module>-starter`。
- 暴露业务 Controller 和 OpenAPI。
- 执行本服务 Flyway migration。
- 扫描 `module.properties` 和 resource manifest。
- 负责菜单、权限、API 资源同步。

服务调用方：

- 依赖 `<module>-starter-remote`。
- 只通过 `<module>-api` 中的契约调用。
- 不依赖 `<module>-core`、Mapper、Entity 或 ServiceImpl。

前端和网关：

- 前端 `apiBaseUrl` 指向网关或统一 API 入口。
- 网关负责路由、认证头、租户上下文和 trace 透传。

## 5. 接入方式
生成微服务项目：

```bash
mango init {{projectKebab}} --preset {{preset}} --topology microservice
```

提供方依赖：

```xml
<dependency>
    <groupId>{{groupId}}</groupId>
    <artifactId>order-starter</artifactId>
</dependency>
```

调用方依赖：

```xml
<dependency>
    <groupId>{{groupId}}</groupId>
    <artifactId>order-starter-remote</artifactId>
</dependency>
```

前端本地可继续用 `mango start`，但正式验收要改用真实网关 API 目标。

## 6. 配置说明
| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `mango.dev.json` | `topology` 相关生成内容 | `microservice` | 当前拓扑选择 | 影响 runtime config 微前端入口 | CLI 模板渲染 |
| `frontend/public/runtime-config.microservice.json` | `modules.<module>.mode` | `micro` | runtime module 模式 | 前端按远程 entry 加载 | CLI `renderRuntimeModulesJson` |
| `frontend/public/runtime-config.microservice.json` | `modules.<module>.entry` | `http://b.mango.io:5181/` 等 | 微前端入口 | 浏览器加载远程模块 | CLI `renderRuntimeModulesJson` |
| 提供方 `application.yml` | Flyway module 开关 | 服务自定义 | 本服务 migration | 提供方启动时建表 | 服务配置 |
| 提供方 `application.yml` | datasource | 服务自定义 | 本服务数据库 | 决定数据边界 | 服务配置 |
| 调用方配置 | Feign / discovery 配置 | 服务自定义 | remote starter 连接服务 | 影响远程调用 | 部署平台和 Feign 配置 |
| 网关配置 | route、strip prefix、auth header | 服务自定义 | 前端到服务路由 | 影响页面请求 | 网关配置 |

## 7. API 与扩展
| 扩展点 | 提供方 | 调用方 | 说明 |
|--------|--------|--------|------|
| `<module>-api` | 定义接口契约 | 编译期依赖 | Command、Query、VO、API 方法 |
| `<module>-core` | 本地实现 | 禁止依赖 | Entity、Mapper、Service 实现只归提供方 |
| `<module>-starter` | 必须依赖 | 禁止依赖 | 暴露 Controller 和资源清单 |
| `<module>-starter-remote` | 通常不依赖 | 必须依赖 | Feign client 和远程调用自动配置 |
| resource manifest | 提供方同步 | 不同步 | 菜单和权限资源归提供方 |
| runtime config | 前端按网关和 entry 配置 | 无 | 管理前端加载远程模块 |

## 8. 数据与初始化
| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 提供方 Flyway | 提供方服务 migration | 本服务业务表 | Flyway version | 提供方启动 | Flyway history、业务表 |
| 平台公共表 | 平台服务或聚合服务 migration | authorization、identity、system 等 | 各模块 version | 对应服务启动 | 平台服务日志 |
| 资源清单 | 提供方 `<module>-starter` | 菜单、权限、API 资源 | appCode、moduleCode、menuCode、permissionCode | 提供方资源同步 | 授权中心资源树 |
| 调用方数据 | 调用方服务自身 migration | 调用方表 | Flyway version | 调用方启动 | 调用方 Flyway history |

不要让调用方通过依赖 `core` 共享提供方表结构。跨服务读取应通过 API、事件、同步表或专门的数据集成方案设计。

## 9. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 提供方业务页面 | `<module>/<aggregate>/index` | `<module>:<aggregate>:create`、`view`、`update`、`delete` | 提供方 resource manifest | 提供方或授权中心配置 | 提供方 Controller / Service |
| 调用方页面 | 调用方自己登记 | 调用方自己定义 | 调用方 resource manifest | 调用方或授权中心配置 | 调用方 Controller / Service |

微服务验收必须覆盖认证头、租户 ID、用户上下文和 trace 在网关、调用方、提供方之间的透传。菜单显示不等于接口授权通过。

## 10. 快速开始
1. 先设计服务边界、数据库归属和 API 契约。
2. 提供方依赖 `<module>-starter` 并启动服务。
3. 调用方依赖 `<module>-starter-remote`，补充 Feign、服务发现或网关配置。
4. 配置网关路由，确认前端 API base URL 指向网关。
5. 提供方执行 resource manifest 同步，授权中心能看到资源。
6. 做 remote 契约测试、网关链路测试、前端 E2E 和权限租户验收。
7. 交付台账登记跨服务风险、未验证项和回滚方案。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 调用方依赖了 `core` | API 契约没有抽清楚 | 改为依赖 `<module>-starter-remote` |
| 网关 404 | route、prefix 或服务名配置错误 | 检查网关路由和提供方 context path |
| Feign 调用失败 | 服务发现、path 或认证头缺失 | 检查 remote starter、Feign 配置和请求头透传 |
| 菜单显示但接口 403 | 只有菜单资源，没有接口权限或角色授权 | 查授权中心权限码和后端校验 |
| 数据串租 | 网关或调用方没有透传租户上下文 | 补租户透传和服务端校验 |
| 前端微模块加载失败 | runtime entry 不可访问或跨域错误 | 检查 runtime config、域名、CORS 和资源路径 |

## 12. 相关文档
- [开发流程规范](../../business-pmo/mango-baseline/rules/00-dev-flow.md)
- [后端模块规范](../../business-pmo/mango-baseline/rules/backend/05-module.md)
- [后端 API 规范](../../business-pmo/mango-baseline/rules/backend/03-api.md)
- [后端安全规范](../../business-pmo/mango-baseline/rules/backend/06-security.md)
- [交付契约规范](../../business-pmo/mango-baseline/rules/01-delivery-contract.md)

## 13. 历史资料
- [项目 README](../../README.md)
- [Business PMO README](../../business-pmo/README.md)
- [单体拓扑说明](../monolith/README.md)
