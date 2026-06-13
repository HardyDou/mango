# Microservice Topology

## 1. 能力定位

`topologies/microservice` 说明 Mango 业务项目的微服务部署拓扑模板，定义业务服务、远程调用方、前端后台和网关 API 的职责边界。

## 2. 适用场景

- 业务模块需要独立服务部署。
- 调用方需要通过 `starter-remote` 访问业务服务。
- 前端通过真实网关 API 访问后端服务。
- 需要按服务边界同步 module properties 和资源清单。

## 3. 不适用场景

- 不适合只需一个进程交付的简单业务项目。
- 不允许调用方直接依赖业务模块 `core`。
- 不替代服务注册、配置中心和部署平台方案。

## 4. 模块边界

业务服务 app 依赖 `mango-admin-starter` 和业务模块 starter，例如 `<module>-starter`，负责暴露 `/{module-path}` API、采集 `module.properties`、同步资源清单。调用方只依赖业务模块 remote starter，例如 `<module>-starter-remote`。

## 5. 接入方式

生成项目时选择微服务拓扑：

```bash
mango init <project> --preset full --topology microservice
```

业务服务依赖本模块 starter，调用方依赖 remote starter，前端后台通过网关访问 API。

## 6. 配置项

拓扑模板自身没有运行时配置项。生成后的服务配置由各服务 `application.yml`、Maven `pom.xml`、`mango.dev.json` 和部署平台配置承载。

## 7. 对外接口 / 扩展点

- 后端服务 API 路径由业务 starter Controller 暴露。
- 远程调用扩展点是业务模块 remote starter，例如 `<module>-starter-remote`。
- 资源同步依赖业务服务内 `module.properties` 和资源清单。
- 前端页面 key 来自业务前端包注册。

## 8. 数据库 / 初始化数据

拓扑说明不包含数据库 migration。业务服务负责自身 Flyway migration 和初始化数据。

## 9. 菜单 / 权限 / 租户

微服务模式下，资源清单和 API 资源同步应在业务服务侧执行。菜单、权限和租户边界由 authorization 统一管理，前端通过网关访问时仍以后端鉴权结果为准。

## 10. 验证方式

代表性验证：

```bash
mvn -pl backend/modules/<module> -am test
pnpm -F <admin-app> build
```

验收时还应覆盖调用方 remote 契约测试和前端连接真实网关 API。

## 11. 业务接入最小闭环

微服务首验顺序：先启动业务服务并确认本地 starter 暴露 API、module properties 和 resource manifest；再启动调用方并通过 `starter-remote` 调用业务 API；然后配置网关路由，最后由前端通过真实网关访问页面。

断言覆盖：调用方没有依赖业务 `core`，remote 契约能调用成功，网关正确透传认证和租户上下文，authorization 能看到业务服务同步的 API 资源和菜单资源。

## 12. 常见问题

- 调用方依赖 `core` 会破坏服务边界，应改为依赖 `starter-remote`。
- 网关 API 404 时检查服务路由、上下文路径和资源同步。
- 前端菜单能显示但页面请求失败时检查网关代理和认证头透传。

## 13. 关联 PMO 规则

- [开发流程规范](../../business-pmo/mango-baseline/rules/00-dev-flow.md)
- [后端模块规范](../../business-pmo/mango-baseline/rules/backend/05-module.md)
- [前端模块规范](../../business-pmo/mango-baseline/rules/frontend/01-vue-code.md)
- [交付契约规范](../../business-pmo/mango-baseline/rules/01-delivery-contract.md)

## 14. 历史设计 / 交付记录

- [Business Starter README](../../README.md)
- [Business PMO README](../../business-pmo/README.md)
