# Mango Business Starter

## 1. 概览
`mango-business-starter` 是 Mango 业务项目和业务模块的模板资产目录。它提供三类能力：

- 业务模块模板：后端 `api`、`core`、`starter`、`starter-remote` 四层模块，前端 API 包和页面包。
- 业务 PMO baseline：业务仓库内可独立执行的 preflight、交付契约检查、角色规则和示例台账。
- 拓扑说明：单体和微服务模式下业务模块依赖、远程调用和菜单页面接入方式。

`@mango/cli` 使用本目录的方式有边界：`mango init --preset full` 读取 CLI 包内 `templates/full`；`mango module add` 会优先读取 CLI 包内 `templates/business-module`，当前该目录不存在时回退到本目录。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 维护 mango module add 生成业务模块时使用的回退模板 | CLI / 模板 / 生成产物 |
| 给业务项目生成标准 CRUD 后端、前端页面和资源清单起点 | CLI / 模板 / 生成产物 |
| 给业务仓库下发 business-pmo、business-docs、AGENTS.md、拓扑说明和 CODEOWNERS 模板 | CLI / 模板 / 生成产物 |
| 校验模板变量、必备文件、后端模块结构、前端包结构、菜单权限清单和拓扑文档 | CLI / 模板 / 生成产物 |
| 作为业务开发者阅读生成代码的说明：知道每一层是什么、改哪里、怎么验证 | CLI / 模板 / 生成产物 |

## 3. 适用场景
- 维护 `mango module add` 生成业务模块时使用的回退模板。
- 给业务项目生成标准 CRUD 后端、前端页面和资源清单起点。
- 给业务仓库下发 `business-pmo`、`business-docs`、`AGENTS.md`、拓扑说明和 CODEOWNERS 模板。
- 校验模板变量、必备文件、后端模块结构、前端包结构、菜单权限清单和拓扑文档。
- 作为业务开发者阅读生成代码的说明：知道每一层是什么、改哪里、怎么验证。

## 4. 边界说明
- 不作为 Maven 或 NPM 运行时依赖引入业务项目。
- 不替代业务领域建模；模板只生成单聚合 CRUD 起点。
- 不自动设计复杂权限、租户隔离、数据权限、状态机、审批流和跨服务一致性。
- 不替代 CLI full 项目模板；full 初始化产物以 `mango-ui/packages/mango-cli/templates/full` 为准。
- 不负责把已生成业务项目自动升级到最新模板；已有业务代码需要 CLI sync 或人工迁移。

## 5. 模块组成
本目录负责模板资产和静态契约校验。CLI 负责读取模板、渲染变量、写入生成项目并更新 managed block。生成后的业务项目负责：

- 补充真实字段、索引、唯一约束、枚举、字典和校验规则。
- 补充真实菜单层级、按钮权限、角色授权和租户数据边界。
- 编写业务测试、E2E、交付台账和模块 README。
- 维护业务自己的前端交互、API 契约和部署拓扑。

模板默认生成的是一个业务聚合的 CRUD 管理页面，不是完整业务系统。

## 6. 接入方式
业务开发者通常不直接复制本目录，而是通过 CLI 使用：

```bash
mango module add order --aggregate sales-order --aggregate-name 销售订单 --module-name 订单模块 --project-dir .
```

命令会生成：

| 生成位置 | 内容 | 业务开发下一步 |
|----------|------|----------------|
| `backend/modules/<module>/<module>-api` | API 接口、Command、Query、VO | 明确字段、参数校验、返回模型和接口语义 |
| `backend/modules/<module>/<module>-core` | Entity、Mapper、Service、Flyway SQL | 设计表结构、索引、租户字段、查询条件和业务逻辑 |
| `backend/modules/<module>/<module>-starter` | Controller、自动配置、module metadata、resource manifest | 接入 Web、菜单权限资源和模块启动 |
| `backend/modules/<module>/<module>-starter-remote` | Feign client 自动配置 | 微服务调用方按需依赖 |
| `frontend/packages/<module>-api` | 前端请求函数和 TS 类型 | 和后端 API 契约同步 |
| `frontend/packages/<module>` | 页面注册和 Element Plus CRUD 页面 | 改成真实业务页面 |
| `frontend/src/main.ts` | 业务页面注册 | 确认 `register<Module>Pages()` 已写入 |
| `backend/pom.xml` | 业务模块 Maven module | 确认 `business-modules` managed block 已追加 |
| `backend/app/pom.xml` | app 依赖业务 starter | 确认 `business-dependencies` managed block 已追加 |
| `backend/app/src/main/resources/application.yml` | 业务 Flyway 模块开关 | 确认 `<module>.enabled: true` 已追加 |
| `mango.config.json` | `businessModules` 登记 | 确认 module、aggregate、displayName 已登记 |

模板变量由 CLI 渲染：

| 变量 | 示例 | 用途 |
|------|------|------|
| `{{projectKebab}}` | `demo-admin` | 前端业务包 scope 和项目名 |
| `{{projectPascal}}` | `DemoAdmin` | 类名或显示名拼接 |
| `{{moduleKebab}}` | `order` | 模块目录、artifactId、moduleCode、菜单路径 |
| `{{modulePackage}}` | `order` | Java package segment |
| `{{modulePascal}}` | `Order` | Java 类名、注册函数名 |
| `{{moduleCamel}}` | `order` | 前端变量名 |
| `{{moduleName}}` | `订单模块` | 菜单模块名、OpenAPI tag |
| `{{aggregateKebab}}` | `sales-order` | 聚合路径、表名片段、页面路径 |
| `{{aggregatePascal}}` | `SalesOrder` | Entity、Service、Command、VO 类名 |
| `{{aggregateCamel}}` | `salesOrder` | Service 变量名 |
| `{{aggregateName}}` | `销售订单` | 页面文案、菜单名、权限名 |
| `{{basePackage}}` | `com.example.mango` | Java 根包名 |
| `{{basePackagePath}}` | `com/example/mango` | Java 源码路径 |

## 7. 配置说明
本目录自身没有运行时配置类。它通过模板变量、生成项目配置和 CLI managed block 生效。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `mango.config.json` | `businessModules[].module` | CLI 参数 `<module>` | 业务模块 code | 记录已生成业务模块 | `updateBusinessConfig` |
| `mango.config.json` | `businessModules[].aggregate` | `--aggregate` | 聚合 code | 记录模块默认聚合 | `updateBusinessConfig` |
| `mango.config.json` | `businessModules[].package` | module camel case | Java package segment | 生成模块源码路径和包名 | `toJavaSegment` |
| `mango.config.json` | `businessModules[].displayName` | `<Module>模块` 或 `--module-name` | 模块中文名 | 菜单模块名、OpenAPI tag | `addBusinessModule` |
| `mango.config.json` | `businessModules[].aggregateDisplayName` | aggregate Pascal 或 `--aggregate-name` | 聚合中文名 | 页面文案、菜单名、权限名 | `addBusinessModule` |
| `application.yml` | `<module>.enabled` | `true` | 业务 Flyway 模块启用开关 | 后端启动时纳入业务模块 migration | `updateBackendBusinessFlywayConfig` |
| `resource-manifest.json` | `appCode` | `internal-admin` | 菜单权限归属应用 | 资源同步时归入内部管理端 | `resource-manifest.json` |
| `resource-manifest.json` | `moduleCode` | `{{moduleKebab}}` | 菜单权限归属模块 | 菜单、权限唯一归属 | `resource-manifest.json` |
| `module.properties` | `module-name` | `{{moduleKebab}}` | Mango 模块名 | 模块资源发现 | `module.properties` |
| `module.properties` | `module-path` | `{{moduleKebab}}` | Mango 模块路径 | 模块资源发现 | `module.properties` |

生成后把模板默认字段改成真实业务字段，避免只保留 `name` 示例字段交付。

## 8. API 与扩展
### 7.1 后端分层

| 模块 | 生成内容 | 依赖 | 修改重点 |
|------|----------|------|----------|
| `<module>-api` | `{{modulePascal}}Api`、`Create...Command`、`Update...Command`、`...PageQuery`、`...VO` | `mango-common`、`mango-infra-persistence-starter`、Spring Web、Validation、Swagger | 定义稳定 API 契约、字段校验、分页查询条件 |
| `<module>-core` | `...Entity`、Mapper、Service 接口和实现、Flyway SQL | `<module>-api`、`mango-infra-persistence-starter` | 表结构、租户字段、查询实现、业务规则 |
| `<module>-starter` | Controller、AutoConfiguration、`module.properties`、`resource-manifest.json` | `<module>-api`、`<module>-core`、`mango-infra-web-starter`、`mango-infra-persistence-web-starter` | Web 暴露、菜单权限资源、应用依赖 |
| `<module>-starter-remote` | Feign client 和 AutoConfiguration | `<module>-api`、`mango-infra-feign-starter` | 微服务调用方远程访问 |

模板 API 形态：

| 能力 | 后端接口方法 | 前端请求函数 | 默认说明 |
|------|--------------|--------------|----------|
| 创建 | `create` | `create{{aggregatePascal}}` | 接收 `Create...Command` |
| 修改 | `update` | `update{{aggregatePascal}}` | 接收 `Update...Command` |
| 删除 | `delete` | `delete{{aggregatePascal}}` | 接收 `DeleteCommand` |
| 分页 | `page` | `page{{aggregatePascal}}` | 接收 `...PageQuery`，返回分页结果 |
| 详情 | `detail` | `get{{aggregatePascal}}Detail` | 按 id 查询 |

Controller 使用 `BaseCrudController`，类级路径由 module 和 aggregate 渲染；接口契约在 `{{modulePascal}}Api` 中声明。微服务调用方使用 `<module>-starter-remote` 中的 Feign client。

### 7.2 前端包

| 包 | 导出 | 依赖 | 适用场景 |
|----|------|------|----------|
| `<module>-api` | TS 类型、CRUD 请求函数 | `@mango/api-schema`、`@mango/common` | 页面包、其他业务前端调用 API |
| `<module>` | `{{moduleCamel}}PageRegistry`、`register{{modulePascal}}Pages()`、API re-export | `<module>-api`、`@mango/admin-pages`、`@mango/common`、Element Plus | 管理后台页面注册 |
| admin app | `createMangoAdminApp()` 调用和业务页面注册 | `@mango/admin` | 业务后台入口 |

页面默认包含查询、重置、新增、刷新、表格、分页、编辑弹窗、详情抽屉和删除确认。它是 CRUD 起点，业务交付时应补齐真实字段、权限控制、空状态、错误态和 E2E。

## 9. 数据与初始化
模板生成一个 Flyway migration 起点和一个菜单权限资源清单。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| Flyway migration | `backend/modules/<module>/<module>-core/src/main/resources/db/migration/<module>/V1__init_<module>.sql` | `<module>_<aggregate>` 业务表示例，字段包含 `id`、`name`、`tenant_id`、审计字段 | 由表主键和 Flyway version 控制 | 后端应用启动，且业务 Flyway 模块启用 | Flyway history、业务表、后端启动日志 |
| Flyway 模块开关 | `backend/app/src/main/resources/application.yml` | `<module>.enabled: true` | module code | `mango module add` 写入后，下次应用启动生效 | application.yml managed block |
| 资源清单 | `<module>-starter/src/main/resources/META-INF/mango/resource-manifest.json` | 模块菜单、聚合列表页、create/view/update/delete 权限 | `appCode`、`moduleCode`、`menuCode`、`permissionCode` | 资源同步 starter 随应用启动处理 | 菜单树、权限码、资源同步日志 |
| 模块元数据 | `<module>-starter/src/main/resources/META-INF/mango/module.properties` | `module-name`、`module-path` | module name | 模块资源发现阶段 | 打包产物和模块扫描日志 |

默认 Entity 继承 `TenantEntity`，默认 SQL 也包含 `tenant_id`。生成后如果业务不使用租户隔离，要明确删除或解释；如果使用租户隔离，要把查询、写入、测试和数据权限补齐。

## 10. 管理入口
模板默认资源清单：

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 模块目录 | 无 | 无 | `resource-manifest.json` | 由授权模块资源同步和角色授权决定 | 无直接 Controller |
| 聚合管理页 | `{{moduleKebab}}/{{aggregateKebab}}/index` | `{{moduleKebab}}:{{aggregateKebab}}:create`、`view`、`update`、`delete` | `resource-manifest.json` | 模板不直接授予角色 | `{{modulePascal}}Controller`、业务 Service |

生成后重点检查：

- `menuCode` 是否符合业务模块命名。
- `path`、`redirect`、`component` 是否能和前端页面注册对上。
- 权限码是否覆盖页面按钮和后端接口；模板页面默认没有按钮级权限判断，需要业务补齐。
- 租户隔离是否和 `TenantEntity`、`tenant_id`、查询条件、当前登录上下文一致。
- 默认资源清单只登记资源，不代表用户已经拥有权限；角色授权仍由授权模块或业务初始化流程完成。

## 11. 快速开始
新增业务模块的最小闭环：

1. 运行 `mango module add` 生成模块。
2. 修改 `<module>-api`：补齐 Command、Query、VO 字段和 validation。
3. 修改 `<module>-core`：补齐 Entity、SQL、Mapper、Service 业务逻辑和租户条件。
4. 修改 `<module>-starter`：确认 Controller 路径、资源清单、权限码和模块元数据。
5. 修改前端 API 包：同步 TS 类型和请求函数。
6. 修改前端页面包：补齐真实表单、表格、按钮权限、错误态和空态。
7. 启动后端，确认 Flyway、菜单资源和权限资源已初始化。
8. 执行后端测试、前端构建、页面 E2E 和交付台账登记。
9. 为该业务模块补 README，说明模块是什么、配置在哪里、菜单权限和数据初始化在哪里确认生效。

单体部署：业务 app 依赖 `<module>-starter`，不依赖 `<module>-starter-remote`。

微服务部署：服务提供方依赖 `<module>-starter`；调用方依赖 `<module>-starter-remote`，不要为了远程调用直接依赖对方 `core`。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 生成后菜单打不开 | 前端页面 key、resource manifest component、注册函数不一致 | 检查 `register<Module>Pages()` 和 component key |
| 后端启动没有建业务表 | Flyway 模块未启用或 migration 路径不在扫描范围 | 检查 `application.yml` 的 `<module>.enabled: true` 和 migration 路径 |
| 页面有按钮但权限不生效 | 模板页面默认没有按钮级权限判断 | 接入前端权限指令或组件，并在后端接口补权限校验 |
| 数据跨租户可见 | 模板只提供 `tenant_id` 起点，业务未补查询和写入约束 | 检查 `TenantEntity`、当前租户上下文、Mapper 查询和测试数据 |
| 微服务调用方依赖了 core | 混淆了提供方和调用方依赖 | 调用方改依赖 `<module>-starter-remote` |
| 模板校验通过但业务链路失败 | `check-template.mjs` 只校验模板静态契约 | 生成项目后继续跑 Maven、前端构建、后端启动和 E2E |
| 已生成项目升级模板困难 | 业务代码已经改过，不能直接覆盖 | 用 CLI managed block 同步可管理部分，其余人工迁移 |

## 13. 相关文档
- [PMO 总流程](../mango-pmo/rules/00-dev-flow.md)
- [AI 编码红线](../mango-pmo/rules/03-ai-coding-redlines.md)
- [交付质量门禁](../mango-pmo/rules/05-ai-delivery-quality.md)
- [文档资产边界](../mango-pmo/rules/06-document-assets.md)
- [能力说明维护](../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [业务 PMO 说明](./business-pmo/README.md)
- [业务 baseline](./business-pmo/mango-baseline/README.md)
- [单体拓扑说明](./topologies/monolith/README.md)
- [微服务拓扑说明](./topologies/microservice/README.md)
- [Mango 能力地图](../mango-docs/capabilities/README.md)
