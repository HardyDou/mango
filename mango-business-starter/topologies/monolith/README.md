# Monolith Topology

## 1. 能力定位

`topologies/monolith` 说明 Mango 业务项目的单体部署拓扑模板，定义单体后端应用、业务模块 starter、前端后台应用和本地页面注册方式。

## 2. 适用场景

- 业务项目希望一个后端进程交付。
- 业务 app 直接依赖本地业务模块 starter，例如 `<module>-starter`。
- 前端后台 app 直接依赖业务页面包。
- 开发和部署复杂度优先低于服务拆分诉求。

## 3. 不适用场景

- 不适合需要独立扩缩容、独立发布或独立数据库治理的模块。
- 单体 app 不应依赖业务模块的 remote starter，例如 `<module>-starter-remote`。
- 不替代未来微服务拆分设计。

## 4. 模块边界

单体后端 app 聚合平台 starter 和业务 starter，统一暴露 HTTP API。业务模块仍保持 `api/core/starter/starter-remote` 分层，以便未来拆分；单体运行时只接入本地 starter。

## 5. 接入方式

生成项目时选择单体拓扑：

```bash
mango init <project> --preset full --topology monolith
```

后端 app 依赖 `mango-admin-starter` 和业务模块 starter。前端后台 app 依赖 `@mango/admin` 和业务页面包。

## 6. 配置项

拓扑模板自身没有运行时配置项。生成后的配置由单体后端 `application.yml`、前端 `.env`、`mango.dev.json` 和本地 `.mango` 配置承载。

## 7. 对外接口 / 扩展点

- 后端本地 starter 暴露业务 Controller。
- 前端业务包注册页面 key，例如 `<module>/<aggregate>/index`。
- 后续如拆分微服务，可启用 `starter-remote` 给调用方使用。

## 8. 数据库 / 初始化数据

拓扑说明不包含数据库 migration。单体 app 启动时执行平台模块和业务模块 Flyway migration。

## 9. 菜单 / 权限 / 租户

单体模式下，菜单和权限资源仍归 authorization 管理。业务页面 key 需要与菜单配置一致，租户边界由后端上下文和持久化层控制。

## 10. 验证方式

代表性验证：

```bash
mvn -pl backend -am test
pnpm -F <admin-app> build
```

浏览器验收：菜单 component key，例如 `<module>/<aggregate>/index`，可加载并能访问单体后端 API。

## 11. 业务接入最小闭环

单体首验顺序：在业务项目根目录执行 PMO preflight；后端运行 `mvn -pl backend -am test` 并启动单体 app；前端运行后台应用 build 或 dev；登录后台后打开菜单 component key，例如 `<module>/<aggregate>/index`，确认请求命中单体后端。

断言覆盖：单体 app 只依赖本地 starter，菜单可见且页面不空白，CRUD API 带当前用户和租户上下文，误接 `starter-remote` 时在依赖审查中失败。

## 12. 常见问题

- 单体中误接 `starter-remote` 会引入不必要的远程调用复杂度。
- 页面注册 key 和菜单配置不一致会导致菜单打开空白。
- 单体拆分前应先确认 API 契约、数据库边界和远程调用方。

## 13. 关联 PMO 规则

- [开发流程规范](../../business-pmo/mango-baseline/rules/00-dev-flow.md)
- [后端模块规范](../../business-pmo/mango-baseline/rules/backend/05-module.md)
- [前端模块规范](../../business-pmo/mango-baseline/rules/frontend/01-vue-code.md)
- [交付契约规范](../../business-pmo/mango-baseline/rules/01-delivery-contract.md)

## 14. 历史设计 / 交付记录

- [Business Starter README](../../README.md)
- [Business PMO README](../../business-pmo/README.md)
