# {{moduleName}}

## 1. 概览
`{{moduleKebab}}` 是业务模块模板生成的模块根目录，默认包含 `{{aggregateName}}` 的后端 CRUD、前端页面包、Flyway migration 和菜单权限资源清单。

生成代码只是业务起点。交付前需要把示例字段、索引、权限、租户边界、数据权限和页面交互改成真实业务模型。

## 2. Mango 能力入口
本模块复用以下 Mango 能力。业务开发改代码前，先阅读对应能力说明：

| Mango 能力 | 本模块使用位置 | 文档入口 |
|------------|----------------|----------|
| Persistence 持久化 | Entity、Mapper、Service、Controller、Flyway migration | Mango 文档站 -> 能力地图 -> Persistence 持久化 |
| Authorization 授权资源 | `resource-manifest.json` 菜单和按钮权限 | Mango 文档站 -> 能力地图 -> Authorization 授权 |
| Admin Pages 页面注册 | 前端 `register{{modulePascal}}Pages()` 和 component key | Mango 文档站 -> 能力地图 -> Admin Pages |
| Business PMO baseline | 业务开发、验证和交付前 preflight | `business-pmo/mango-baseline/README.md` |

## 3. 模块组成
| 目录 | 作用 |
|------|------|
| `{{moduleKebab}}-api` | 后端 API、Command、Query 和 VO |
| `{{moduleKebab}}-core` | Entity、Mapper、Service 和 Flyway migration |
| `{{moduleKebab}}-starter` | Controller、自动配置、模块元数据和资源清单 |
| `{{moduleKebab}}-starter-remote` | 微服务调用方 Feign client |
| `frontend/packages/{{moduleKebab}}-api` | 前端请求函数和类型 |
| `frontend/packages/{{moduleKebab}}` | 管理端页面注册和页面实现 |

## 4. 接入方式
后端应用通过 `backend/app/pom.xml` 依赖 `{{moduleKebab}}-starter`。Flyway 模块开关在 `backend/app/src/main/resources/application.yml` 中启用。

前端应用通过 `frontend/src/main.ts` 调用 `register{{modulePascal}}Pages()` 注册页面。菜单资源中的 component key 是 `{{moduleKebab}}/{{aggregateKebab}}/index`。

## 5. 数据与初始化
| 类型 | 位置 | 初始化内容 | 生效时机 |
|------|------|------------|----------|
| Flyway migration | `{{moduleKebab}}-core/src/main/resources/db/migration/{{moduleKebab}}/V1__init_{{moduleKebab}}.sql` | `{{moduleKebab}}_{{aggregateKebab}}` 示例业务表 | 后端应用启动且模块 Flyway 开关启用 |
| 资源清单 | `{{moduleKebab}}-starter/src/main/resources/META-INF/mango/resource-manifest.json` | `{{aggregateName}}管理` 菜单和 create/view/update/delete 权限 | 资源同步 starter 随应用处理 |
| 模块元数据 | `{{moduleKebab}}-starter/src/main/resources/META-INF/mango/module.properties` | module name 和 module path | 模块扫描阶段 |

## 6. 快速开始
1. 把 `{{aggregateName}}` 的示例字段替换为真实业务字段。
2. 调整 migration，补齐真实索引、唯一约束、租户字段、审计字段和数据权限字段。
3. 在 Service 中保留 Mango CRUD 基线，复杂查询使用 Mapper XML，并按 Persistence README 示例处理租户和数据权限。
4. 调整 `resource-manifest.json` 的菜单层级、权限码和默认授权。
5. 调整前端表单、表格、详情、权限按钮和空/错/加载态。
6. 补充业务模块 README、交付台账、后端测试和前端 E2E。

## 7. 问题排查
| 现象 | 排查入口 |
|------|----------|
| 分页、租户、数据权限写法不确定 | Mango 文档站 -> 能力地图 -> Persistence 持久化 |
| 菜单存在但页面打不开 | `resource-manifest.json` 的 component key 和前端 `register{{modulePascal}}Pages()` |
| 权限按钮不显示 | `resource-manifest.json` permissionItems、角色授权和前端权限控制 |
| migration 没执行 | `application.yml` Flyway 模块开关和 Flyway history |

## 8. 相关文档
- [业务 PMO baseline](../../../business-pmo/mango-baseline/README.md)
- [单体拓扑说明](../../../topologies/monolith/README.md)
- [微服务拓扑说明](../../../topologies/microservice/README.md)
