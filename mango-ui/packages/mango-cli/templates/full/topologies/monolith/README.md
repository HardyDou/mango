# Monolith Topology

## 1. 概览
`topologies/monolith` 说明生成业务项目在单体模式下的接入方式。单体模式只有一个后端 app 承载 Mango 平台 starter 和业务模块 starter，前端后台通过本地 `/api` 代理访问这个后端。

核心判断：业务 app 依赖本地 `<module>-starter`，不依赖 `<module>-starter-remote`。

## 2. 功能清单

| 能力 | 使用入口 | 说明 |
|------|----------|------|
| 单后端进程 | `backend/app` | 平台 starter 和业务 starter 都由同一个 Spring Boot app 承载。 |
| 本地业务依赖 | `<module>-starter` | app 直接依赖本地业务模块 starter。 |
| 统一数据库和 Flyway | `application.yml` | 平台和业务 migration 由同一个 app 执行。 |
| 前端本地代理 | `/api` proxy | 前端后台访问同一个后端 API base URL。 |
| 菜单权限同步 | resource manifest | 随 starter 扫描并同步菜单、页面和按钮资源。 |

## 3. 能力边界

| 选择单体 | 不适合单体 |
|----------|------------|
| 当前只需要一个后端进程交付。 | 模块需要独立发布、独立扩缩容、独立数据库账号或独立运维窗口。 |
| 业务模块和平台模块共用数据源、事务边界和 Flyway 入口。 | 调用方必须通过 Feign 或网关访问某个业务服务。 |
| 前端后台直接消费同一个后端 API base URL。 | 不同业务模块之间已有明确服务自治边界。 |
| 更重视启动简单、调试直接和部署成本低。 | 需要验证跨服务认证、租户透传、超时、重试和降级策略。 |

单体 app 不应依赖 `<module>-starter-remote`。remote starter 是微服务调用方依赖，不是本地模块依赖。

## 4. 模块入口
单体模式的后端边界：

- `backend/app` 是唯一运行 app。
- `backend/app/pom.xml` 依赖平台 starter 和业务 `<module>-starter`。
- `<module>-starter` 聚合该业务模块的 API、core、Controller、resource manifest 和 AutoConfiguration。
- `<module>-starter-remote` 只为未来微服务调用方准备，单体运行时不用。

前端边界：

- `frontend/src/main.ts` 注册 Mango 平台页面和业务页面包。
- `frontend/vite.config.ts` 把 `/api` 代理到本机后端。
- 菜单 component key 必须和前端 page registry 一致。

## 5. 接入方式
生成单体项目：

```bash
mango init {{projectKebab}} --preset {{preset}} --topology monolith
```

本地启动：

```bash
scripts/dev-workspace.sh init
mango validate
mango plan
mango start
```

新增业务模块：

```bash
mango module add order --aggregate sales-order --project-dir .
```

单体依赖检查：

| 位置 | 应有内容 | 不应出现 |
|------|----------|----------|
| `backend/pom.xml` | `<module>modules/<module></module>` | 无关服务模块 |
| `backend/app/pom.xml` | `<module>-starter` | `<module>-starter-remote` |
| `application.yml` | `<module>.enabled: true` | 未启用的业务 Flyway 模块 |
| `frontend/package.json` | 业务页面包和 API 包 | 指向 Mango 源码的相对路径依赖 |
| `frontend/src/main.ts` | `register<Module>Pages()` | 重复注册或缺失注册 |

## 6. 配置说明
| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `mango.dev.json` | `groups.default` | 后端 app、前端 app | 单体默认启动顺序 | `mango start` 同时启动后端和前端 | `mango.dev.json` |
| `mango.dev.json` | 后端 app `type` | `spring-boot-maven` | 后端启动类型 | 使用 Spring Boot Maven plugin | `mango.dev.json` |
| `mango.dev.json` | 前端 app `type` | `vite` | 前端启动类型 | 使用 NPM dev script | `mango.dev.json` |
| `.mango/dev-workspace.env` | `MANGO_BACKEND_PORT` | `5555` | 单体后端端口 | 前端 proxy 目标 | `scripts/dev-workspace.sh` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_PORT` | `5176` | 前端端口 | Vite dev server | `scripts/dev-workspace.sh` |
| `frontend/src/main.ts` | `apiBaseUrl` | `/api` | 前端 API base URL | Vite dev proxy 转发到后端 | `frontend/src/main.ts` |
| `frontend/vite.config.ts` | `/api` proxy | `http://127.0.0.1:5555` | 本地代理目标 | 只允许本机 host | `vite.config.ts` |
| `application.yml` | `mango.persistence.flyway.modules.*.enabled` | 平台模块 true | 平台 migration 开关 | 后端启动执行平台表和基础数据 | `application.yml` |
| `application.yml` | `business-flyway-modules` | 由 CLI 追加 | 业务 migration 开关 | 后端启动执行业务表 | `application.yml` |

## 7. API 与扩展
| 扩展点 | 单体用法 | 说明 |
|--------|----------|------|
| `<module>-starter` | app 直接依赖 | 暴露业务 Controller、资源清单和 AutoConfiguration |
| `<module>-api` | 被 starter 和前端契约间接引用 | 定义 Command、Query、VO、API |
| `<module>-core` | 被 starter 本地依赖 | 承载 Entity、Mapper、Service、Flyway |
| `<module>-starter-remote` | 不接入 | 只给微服务调用方使用 |
| 前端页面包 | app 直接依赖并注册 | 提供 component key |
| resource manifest | 随 starter 被扫描 | 初始化菜单和权限资源 |

## 8. 数据与初始化
| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 平台 Flyway | `application.yml` 平台 module 开关 | Mango 平台表和基础数据 | Flyway version | 单体 app 启动 | Flyway history、启动日志 |
| 业务 Flyway | `business-flyway-modules` managed block | 业务模块表 | module code、Flyway version | `mango module add` 后下次启动 | 业务表存在 |
| 菜单权限资源 | 业务 `<module>-starter` resource manifest | 菜单、页面、按钮权限 | appCode、moduleCode、menuCode、permissionCode | 资源同步 starter 启动 | 菜单树和权限码 |

单体模式共用一个数据库连接。业务模块如果需要独立库或独立 schema，应按微服务或独立部署重新设计。

## 9. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 平台页面 | Mango 平台包登记 | 平台模块定义 | 平台 migration 或 resource manifest | 平台模块或 seed 定义 | 平台 Controller / Service |
| 业务页面 | `<module>/<aggregate>/index` | `<module>:<aggregate>:create`、`view`、`update`、`delete` | 业务 resource manifest | 业务授权流程定义 | 业务 Controller / Service |

租户边界在单体 app 内统一处理。业务 Entity 继承租户基类时，要验证新增、查询、分页、详情、更新和删除都带当前租户上下文。

## 10. 快速开始
1. 运行 preflight，读取后端模块、数据库、前端和交付规则。
2. 用 `mango module add` 生成业务模块。
3. 确认 app 依赖 `<module>-starter`，不依赖 `<module>-starter-remote`。
4. 启动后端，确认平台和业务 Flyway 都执行成功。
5. 登录后台，确认菜单显示、component key 能加载页面。
6. 执行 CRUD 链路，确认请求命中单体后端并带认证、权限和租户上下文。
7. 完成后端测试、前端构建、E2E 和验收证据登记。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 单体 app 引入 remote starter | 混淆本地依赖和远程调用依赖 | app 改依赖 `<module>-starter` |
| 页面菜单打开空白 | component key 与前端 registry 不一致 | 检查 resource manifest 和 `register<Module>Pages()` |
| API 404 | Controller path、Vite proxy 或菜单路径不一致 | 用 `mango plan` 和浏览器 network 排查 |
| 业务表没创建 | business Flyway module 未启用 | 检查 `application.yml` managed block |
| 租户数据串租 | 只建了 `tenant_id` 字段，未验证查询过滤 | 补租户上下文测试和数据权限断言 |

## 12. 相关文档
- [开发流程规范](../../business-pmo/mango-baseline/rules/00-dev-flow.md)
- [后端模块规范](../../business-pmo/mango-baseline/rules/backend/05-module.md)
- [数据库规范](../../business-pmo/mango-baseline/rules/backend/04-db.md)
- [模块菜单规范](../../business-pmo/mango-baseline/rules/backend/11-module-menu.md)
- [前端开发流程](../../business-pmo/mango-baseline/rules/frontend/05-dev-flow.md)

- [项目 README](../../README.md)
- [Business PMO README](../../business-pmo/README.md)
- [微服务拓扑说明](../microservice/README.md)
