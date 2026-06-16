# Monolith Topology

## 1. 概览
`topologies/monolith` 说明使用 `mango-business-starter` 生成业务模块后，如何接入单体后端和后台前端。单体模式下，一个 Spring Boot app 依赖 Mango 平台 starter 和业务 `<module>-starter`，一个前端后台 app 注册平台页面和业务页面。

核心判断：单体 app 依赖本地 `<module>-starter`，不依赖 `<module>-starter-remote`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 当前阶段只需要一个后端进程交付 | CLI / 模板 / 生成产物 |
| 业务模块和平台模块共用同一个 Spring Boot app、数据源和 Flyway 执行入口 | CLI / 模板 / 生成产物 |
| 开发团队优先考虑启动简单、调试直接和部署成本低 | CLI / 模板 / 生成产物 |
| 前端后台直接消费同一个后端 API base URL | CLI / 模板 / 生成产物 |
| 业务模块未来可能拆分，但当前不需要独立发布或独立扩缩容 | CLI / 模板 / 生成产物 |

## 3. 适用场景
- 当前阶段只需要一个后端进程交付。
- 业务模块和平台模块共用同一个 Spring Boot app、数据源和 Flyway 执行入口。
- 开发团队优先考虑启动简单、调试直接和部署成本低。
- 前端后台直接消费同一个后端 API base URL。
- 业务模块未来可能拆分，但当前不需要独立发布或独立扩缩容。

## 4. 边界说明
- 模块需要独立发布、独立扩缩容、独立数据库账号或独立运维窗口。
- 调用方必须通过 Feign、网关或服务发现访问某个业务服务。
- 不同业务模块之间已有明确服务自治边界。
- 想在同一个 app 内用 `<module>-starter-remote` 调本地模块。
- 需要重点验证跨服务认证、租户透传、超时、重试和降级策略。

## 5. 模块组成
后端边界：

- 单体后端 app 依赖平台 starter 和业务 `<module>-starter`。
- `<module>-starter` 聚合 API、core、Controller、AutoConfiguration、`module.properties` 和 `resource-manifest.json`。
- `<module>-core` 的 Entity、Mapper、Service 和 Flyway migration 只在本地 app 内执行。
- `<module>-starter-remote` 只为未来微服务调用方准备，单体运行时不用。

前端边界：

- 后台 app 依赖 `@mango/admin` 和业务页面包。
- `main.ts` 调用业务页面注册函数。
- 菜单 component key 必须和前端 page registry 一致。

## 6. 接入方式
生成业务模块：

```bash
mango module add order --aggregate sales-order --aggregate-name 销售订单 --module-name 订单模块 --project-dir .
```

单体后端 app 依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>order-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

前端后台依赖：

```json
{
  "dependencies": {
    "@demo/order": "1.0.0",
    "@mango/admin": "1.0.14"
  }
}
```

依赖检查：

| 位置 | 应有内容 | 不应出现 |
|------|----------|----------|
| `backend/pom.xml` | `modules/<module>` | 无关服务模块 |
| 单体 app POM | `<module>-starter` | `<module>-starter-remote` |
| `application.yml` | `<module>.enabled: true` | 未启用的业务 Flyway 模块 |
| 前端 app `package.json` | 业务页面包和 `@mango/admin` | 指向 Mango 源码的相对路径依赖 |
| 前端 app `main.ts` | `register<Module>Pages()` | 重复注册或缺失注册 |

## 7. 配置说明
拓扑说明目录自身没有运行时配置。单体接入关注生成后的后端 app、业务 starter 和前端 app 配置。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| 单体 app POM | `<module>-starter` 依赖 | CLI 生成 | 接入业务本地能力 | 暴露 Controller、Service、Flyway、资源清单 | CLI managed block |
| `application.yml` | `<module>.enabled` | `true` | 业务 Flyway 模块开关 | 后端启动时执行业务 migration | CLI managed block |
| `module.properties` | `module-name`、`module-path` | module code | 模块元数据 | 资源发现和模块识别 | starter resources |
| `resource-manifest.json` | `appCode`、`moduleCode`、`menus`、`permissions` | CLI 渲染 | 菜单权限资源 | 资源同步时入库 | starter resources |
| 前端 app `package.json` | `@<project>/<module>` | project version | 业务页面包 | 后台 app 可注册页面 | app dependencies |
| 前端 app `main.ts` | `register<Module>Pages()` | CLI 写入 | 页面注册 | 菜单 component key 能加载组件 | app entry |
| 前端 app `vite.config.ts` | `server.port` | `5173` 或环境变量 | 本地前端端口 | 本地开发访问入口 | Vite config |

## 8. API 与扩展
| 扩展点 | 单体用法 | 说明 |
|--------|----------|------|
| `<module>-starter` | app 直接依赖 | 暴露业务 Controller、资源清单和 AutoConfiguration |
| `<module>-api` | 被 starter 和前端契约间接引用 | 定义 Command、Query、VO、API |
| `<module>-core` | 被 starter 本地依赖 | Entity、Mapper、Service、Flyway |
| `<module>-starter-remote` | 不接入 | 只给微服务调用方使用 |
| 前端页面包 | app 直接依赖并注册 | 提供 component key |
| resource manifest | 随 starter 被扫描 | 初始化菜单和权限资源 |

## 9. 数据与初始化
| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 平台 Flyway | 单体 app 平台 module 配置 | Mango 平台表和基础数据 | Flyway version | 单体 app 启动 | Flyway history、启动日志 |
| 业务 Flyway | `<module>-core/src/main/resources/db/migration/<module>` | 业务表，默认包含 `tenant_id` 和审计字段 | Flyway version | `<module>.enabled: true` 且 app 启动 | 业务表存在、Flyway history |
| 模块元数据 | `<module>-starter` resources | `module-name`、`module-path` | 文件路径、module code | starter 被加载时 | 模块扫描日志 |
| 菜单权限资源 | `<module>-starter` resources | 菜单、页面、按钮权限 | `appCode`、`moduleCode`、`menuCode`、`permissionCode` | 资源同步时 | 菜单树、权限码 |

单体模式共用一个数据库连接。业务模块如果需要独立库或独立 schema，应按微服务或独立部署重新设计。

## 10. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 平台页面 | Mango 平台包登记 | 平台模块定义 | 平台 migration 或 resource manifest | 平台模块或 seed 定义 | 平台 Controller / Service |
| 业务页面 | `<module>/<aggregate>/index` | `<module>:<aggregate>:create`、`view`、`update`、`delete` | 业务 `resource-manifest.json` | 模板不直接授予角色 | 业务 Controller / Service |

租户边界在单体 app 内统一处理。业务 Entity 继承租户基类时，要验证新增、查询、分页、详情、更新和删除都带当前租户上下文。

## 11. 快速开始
1. 运行 preflight，读取后端模块、数据库、前端和交付规则。
2. 用 `mango module add` 生成业务模块。
3. 确认单体 app 依赖 `<module>-starter`，不依赖 `<module>-starter-remote`。
4. 启动后端，确认平台和业务 Flyway 都执行成功。
5. 登录后台，确认菜单显示、component key 能加载页面。
6. 执行 CRUD 链路，确认请求命中单体后端并带认证、权限和租户上下文。
7. 完成后端测试、前端构建、页面 E2E 和交付台账登记。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 单体 app 引入 remote starter | 混淆本地依赖和远程调用依赖 | app 改依赖 `<module>-starter` |
| 页面菜单打开空白 | component key 与前端 registry 不一致 | 检查 resource manifest 和页面注册函数 |
| API 404 | Controller path、前端 API base URL 或菜单路径不一致 | 用浏览器 network 和后端日志排查 |
| 业务表没创建 | 业务 Flyway module 未启用 | 检查 `<module>.enabled: true` 和 migration 路径 |
| 租户数据串租 | 只建了 `tenant_id` 字段，未验证查询过滤 | 补租户上下文测试和数据权限断言 |

## 13. 相关文档
- [开发流程规范](../../business-pmo/mango-baseline/rules/00-dev-flow.md)
- [后端模块规范](../../business-pmo/mango-baseline/rules/backend/05-module.md)
- [数据库规范](../../business-pmo/mango-baseline/rules/backend/04-db.md)
- [后端安全规范](../../business-pmo/mango-baseline/rules/backend/06-security.md)
- [前端开发流程](../../business-pmo/mango-baseline/rules/frontend/05-dev-flow.md)

## 14. 历史资料
- [Business Starter README](../../README.md)
- [Business PMO README](../../business-pmo/README.md)
- [微服务拓扑说明](../microservice/README.md)
