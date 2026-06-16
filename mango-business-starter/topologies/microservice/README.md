# Microservice Topology

## 1. 概览
`topologies/microservice` 说明使用 `mango-business-starter` 生成业务模块后，如何按微服务边界接入。它不是可执行模板目录，而是业务项目里的拓扑说明：服务提供方依赖本地业务 starter，服务调用方依赖 remote starter，前端后台通过网关或统一 API 入口访问服务。

核心判断：调用方不能依赖对方业务模块的 `core`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务模块需要独立部署、独立发布或独立扩缩容 | CLI / 模板 / 生成产物 |
| 调用方需要通过 Feign、网关或统一 API 入口访问业务服务 | CLI / 模板 / 生成产物 |
| 菜单、权限、API 资源由服务提供方随 starter 同步 | CLI / 模板 / 生成产物 |
| 需要验证认证头、租户上下文、trace、超时、重试和降级 | CLI / 模板 / 生成产物 |
| 前端后台要连真实网关 API，而不是只连本地单体进程 | CLI / 模板 / 生成产物 |


## 3. 能力边界
- 业务项目只需要一个后端进程交付。
- 模块之间共享同一事务边界或大量直接访问对方表。
- 调用方必须依赖提供方 `core`、Mapper、Entity 或 ServiceImpl 才能完成业务。
- 没有服务注册、网关、配置、日志和发布平台准备。
- 只是为了拆分而拆分，服务边界和数据归属还没设计清楚。

## 4. 模块入口
服务提供方接入：

- 依赖 `<module>-starter`。
- 通过 starter 暴露 Controller、AutoConfiguration、`module.properties` 和 `resource-manifest.json`。
- 执行本服务自己的 Flyway migration。
- 同步菜单、权限和 API 资源。

服务调用方接入：

- 依赖 `<module>-starter-remote`。
- 通过 `<module>-api` 中的契约访问提供方。
- 不依赖 `<module>-core`、Mapper、Entity、Service 实现或数据库 migration。

前端接入：

- 业务页面包仍注册 component key，例如 `<module>/<aggregate>/index`。
- API base URL 应指向网关或统一 API 入口。
- 菜单能打开只说明页面注册成功，接口还要单独验证鉴权和租户透传。

## 5. 接入方式
生成业务模块：

```bash
mango module add order --aggregate sales-order --aggregate-name 销售订单 --module-name 订单模块 --project-dir .
```

服务提供方依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>order-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

服务调用方依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>order-starter-remote</artifactId>
    <version>${project.version}</version>
</dependency>
```

依赖边界：

| 位置 | 应接入 | 不应接入 |
|------|--------|----------|
| 服务提供方 app | `<module>-starter` | `<module>-starter-remote` |
| 服务调用方 app | `<module>-starter-remote` | `<module>-starter`、`<module>-core` |
| 前端后台 app | `@<project>/<module>`、`@<project>/<module>-api` | 指向 Mango 源码的相对路径依赖 |

## 6. 配置说明
拓扑说明目录自身没有运行时配置。微服务配置分散在生成后的业务项目、服务提供方、调用方和网关中。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `<module>-starter` POM | `<module>-starter` 依赖 | CLI 生成 | 提供方本地业务能力 | 暴露 Controller、资源清单和 AutoConfiguration | starter `pom.xml` |
| `<module>-starter-remote` POM | `<module>-starter-remote` 依赖 | CLI 生成 | 调用方远程访问能力 | 引入 API 契约和 Feign starter | remote starter `pom.xml` |
| `application.yml` | `<module>.enabled` | `true` | 业务 Flyway 模块开关 | 提供方启动时执行 migration | CLI managed block |
| `module.properties` | `module-name`、`module-path` | module code | 模块元数据 | 资源发现和模块识别 | starter resources |
| `resource-manifest.json` | `appCode`、`moduleCode`、`menus`、`permissions` | CLI 渲染 | 菜单权限资源 | 提供方资源同步 | starter resources |
| 调用方配置 | Feign、服务发现、超时、重试 | 项目自定 | 远程调用连接方式 | 影响 remote starter 调用 | 调用方 app 配置 |
| 网关配置 | route、prefix、header 透传 | 项目自定 | 前端到服务路由 | 影响 API 可达性和鉴权 | 网关配置 |
| 前端配置 | API base URL | 项目自定 | 后台请求入口 | 指向网关或统一 API 入口 | 前端 app 配置 |

## 7. API 与扩展
| 扩展点 | 提供方 | 调用方 | 说明 |
|--------|--------|--------|------|
| `<module>-api` | 定义接口契约 | 编译期依赖 | Command、Query、VO、API 方法 |
| `<module>-core` | 本地实现 | 禁止依赖 | Entity、Mapper、Service、Flyway 只归提供方 |
| `<module>-starter` | 必须依赖 | 禁止依赖 | Controller、AutoConfiguration、资源清单 |
| `<module>-starter-remote` | 通常不依赖 | 必须依赖 | Feign client 和远程调用自动配置 |
| 前端页面包 | 管理后台依赖 | 管理后台依赖 | 注册页面 component key |
| resource manifest | 提供方同步 | 不同步 | 菜单、页面、按钮权限归提供方 |

## 8. 数据与初始化
| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 提供方 Flyway | `<module>-core/src/main/resources/db/migration/<module>` | 业务表，默认包含 `tenant_id` 和审计字段 | Flyway version | 提供方服务启动 | Flyway history、业务表 |
| Flyway 开关 | 提供方 `application.yml` | `<module>.enabled: true` | module code | CLI 写入后下次启动 | 配置文件和启动日志 |
| 模块元数据 | `<module>-starter` resources | `module-name`、`module-path` | 文件路径、module code | starter 被加载时 | 打包产物和模块扫描日志 |
| 资源清单 | `<module>-starter` resources | 模块菜单、聚合页面、按钮权限 | `appCode`、`moduleCode`、`menuCode`、`permissionCode` | 提供方资源同步 | 授权中心资源树 |
| 调用方数据 | 调用方自己的 migration | 调用方业务表 | Flyway version | 调用方服务启动 | 调用方 Flyway history |

调用方不要通过依赖 `core` 共享提供方表结构。跨服务读取应通过 API、事件、同步表或专门的数据集成方案设计。

## 9. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 提供方业务页面 | `<module>/<aggregate>/index` | `<module>:<aggregate>:create`、`view`、`update`、`delete` | 提供方 `resource-manifest.json` | 模板不直接授予角色 | 提供方 Controller / Service |
| 调用方页面 | 调用方自己登记 | 调用方自己定义 | 调用方资源清单 | 调用方授权流程 | 调用方 Controller / Service |

微服务验收必须覆盖：

- 网关把认证头、租户 ID、用户上下文和 trace 透传给提供方。
- 提供方后端接口按权限码校验，不只依赖前端按钮隐藏。
- 页面 component key 与资源清单一致。
- 用户拿到角色授权后才能看到菜单并调用接口。

## 10. 快速开始
1. 先设计服务边界、数据库归属和 API 契约。
2. 生成业务模块，并把提供方 app 接入 `<module>-starter`。
3. 调用方 app 接入 `<module>-starter-remote`，补服务发现、Feign 或网关配置。
4. 提供方启动，确认 Flyway、模块元数据和 resource manifest 生效。
5. 配置网关路由，确认前端 API base URL 指向网关。
6. 做 remote 契约测试、网关链路测试、前端 E2E 和权限租户验收。
7. 在业务模块 README 和交付台账中登记验证命令、证据和未覆盖风险。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 调用方依赖了 `core` | API 契约没有抽清楚 | 改为依赖 `<module>-starter-remote` |
| 调用方依赖了 `<module>-starter` | 混淆提供方和调用方边界 | 调用方只接 remote starter |
| 网关 404 | route、prefix 或服务名配置错误 | 检查网关路由和提供方 context path |
| 远程调用失败 | 服务发现、path、认证头或租户头缺失 | 检查 remote starter、Feign 配置和 header 透传 |
| 菜单显示但接口 403 | 只有菜单资源，没有接口权限或角色授权 | 查授权中心权限码和后端校验 |
| 数据串租 | 网关或调用方没有透传租户上下文 | 补租户透传和服务端租户断言 |

## 12. 相关文档
- [开发流程规范](../../business-pmo/mango-baseline/rules/00-dev-flow.md)
- [后端模块规范](../../business-pmo/mango-baseline/rules/backend/05-module.md)
- [后端 API 规范](../../business-pmo/mango-baseline/rules/backend/03-api.md)
- [后端安全规范](../../business-pmo/mango-baseline/rules/backend/06-security.md)
- [交付契约规范](../../business-pmo/mango-baseline/rules/01-delivery-contract.md)

## 13. 补充资料
- [Business Starter README](../../README.md)
- [Business PMO README](../../business-pmo/README.md)
- [单体拓扑说明](../monolith/README.md)
