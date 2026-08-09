# Mango Business Starter

## 1. 概览

`mango-business-starter` 是 Mango 业务项目和业务模块的模板资产目录。它提供三类能力：

- 业务模块模板：后端 `api`、`core`、`starter`、`starter-remote` 四层模块，前端 API 包和页面包。
- 业务 PMO baseline：业务仓库内可独立执行的 preflight、交付契约检查、角色规则和示例台账。
- 拓扑说明：单体和微服务模式下业务模块依赖、远程调用和菜单页面接入方式。

`@mango/cli` 使用本目录的方式有边界：`mango init --preset full` 读取 CLI 包内 `templates/full`；`mango module add` 从 `@mango/pmo` 的 `code-templates/business-module` 生成，本目录只保留经过哈希检查的机械投影。

## 2. 功能清单

| 能力                                                                              | 常用入口              |
| --------------------------------------------------------------------------------- | --------------------- |
| 维护 mango module add 生成业务模块时使用的回退模板                                | CLI / 模板 / 生成产物 |
| 给业务项目生成标准 CRUD 后端、前端页面和资源清单起点                              | CLI / 模板 / 生成产物 |
| 给业务仓库下发 business-pmo、business-docs、AGENTS.md、拓扑说明和 CODEOWNERS 模板 | CLI / 模板 / 生成产物 |
| 校验模板变量、必备文件、后端模块结构、前端包结构、菜单权限清单和拓扑文档          | CLI / 模板 / 生成产物 |
| 作为业务开发者阅读生成代码的说明：知道每一层是什么、改哪里、怎么验证              | CLI / 模板 / 生成产物 |

## 3. 能力边界

- 不作为 Maven 或 NPM 运行时依赖引入业务项目。
- 不替代业务领域建模；模板只生成单聚合 CRUD 起点。
- 不自动设计复杂权限、租户隔离、数据权限、状态机、审批流和跨服务一致性。
- 不替代 CLI full 项目模板；full 初始化产物以 `mango-ui/packages/mango-cli/templates/full` 为准。
- 不负责把已生成业务项目自动升级到最新模板；已有业务代码需要 CLI sync/upgrade 或人工迁移。

## 4. 模块入口

本目录负责模板资产和静态契约校验。CLI 负责读取模板、渲染变量、写入生成项目并更新 managed block。生成后的业务项目负责：

- 补充真实字段、索引、唯一约束、枚举、字典和校验规则。
- 补充真实菜单层级、按钮权限、角色授权和租户数据边界。
- 编写业务测试、E2E、交付台账和模块 README。
- 维护业务自己的前端交互、API 契约和部署拓扑。

模板默认生成的是一个业务聚合的 CRUD 管理页面，不是完整业务系统。

`@mango/pmo@1.3.12` 发布 canonical `business-module` code baseline 和 worktree 交付完整性门禁，精确依赖它的 `@mango/cli@1.0.102` 负责项目初始化、PMO 升级和 `mango module add`。不要使用 CLI `1.0.99` 生成业务模块；该不可变版本不能在安装后的 npm/pnpm 布局中定位 PMO code baseline。模板 manifest 同时定义模块/包结构、`moduleKebab` 等输入与派生变量、Mango Checkstyle 和架构规则源，以及 `XxxCode`、`Require`、typed CRUD、tenant、Mapper、资源、migration、前端导出和测试等可执行规范证据。升级只同步受管 baseline/template/Skill，不会批量重写已有业务模块；路径、SHA-256 和历史 PMO 版本均锁定的审批文档不会被当前新增章节追溯改写。

`business-pmo/mango-baseline` 是 canonical `mango-pmo` 的构建投影，维护边界遵循
[文档资产规范](../mango-pmo/rules/06-document-assets.md)。更新 PMO 后执行：

```bash
node mango-business-starter/scripts/sync-pmo-baseline.mjs --write
node mango-business-starter/scripts/check-template.mjs
```

当前 PMO 投影中的架构债务预算检查器支持读取超过 1 MiB 的 Git 基线，并在完整写出 JSON 结果后再按检查结论退出。该投影修复不改变业务项目的公开 API、配置、菜单、权限、租户、页面、启动、验收和运行时行为。

当前 PMO baseline 先按事实推荐文档版本，再由用户在同一中文界面选择“直接做、一页纸、标准版、详细版、四文档”，勾选 M01-M16，并用一次回车确认全部。人类摘要只列已勾选项；主工作区例外、破坏性数据库动作和外部写入仍需单独授权。发布、版本和发布恢复继续使用独立发布流程。

delivery-assurance schema revision 5 起，PMO baseline 同时携带 canonical 业务 PR 模板。项目内 `mango pmo sync/upgrade` 在模板缺失时创建文件，在模板存在时只托管 `## Risk / Verification` 区段；`mango pmo check --locked` 会阻断缺失或漂移，区段外业务说明保持不变。该能力由 `@mango/pmo@1.3.4` 与 `@mango/cli@1.0.88` 提供。

能力与 Skill 路由统一由项目 `AGENTS.md` 和 PMO preflight 决定；不要从普通技术术语推断无关能力。完整分类边界见[能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)。

当前 scope classifier 会为 partial 后端 PR 同时输出质量模块 `maven_projects` 和依赖准备模块 `maven_dependency_projects`。标准 workflow 先用后者执行带 `-am` 的跳过测试安装，再用前者执行不带 `-am`、`-amd` 的直接模块质量门禁。这样新 Runner 不依赖历史 Maven 缓存，也不会把上游模块的存量质量问题扩大到当前 PR。

PMO 合同启用前形成、尚未迁移的生命周期文档可以在业务文档根目录的 `.mango-pmo-legacy-documents.json` 中逐文件登记相对路径、SHA-256 和迁移原因。该基线只锁定完全相同的存量内容：文件变化、删除、越界、重复或已经迁移为正式 `documentType` 时都会失败；新生命周期文档不能通过该文件绕过合同。

当 PMO 合同在同一 schema revision 内仅升级 `pmoVersion` 时，重新执行 `mango pmo upgrade` 会为升级前已有且合同明确支持的版本自动登记路径、SHA-256、`pmoVersion` 和迁移原因；不会改写文档正文、审批证据或上游摘要。新建文档的版本口径见 [产品文档生命周期规范](../mango-pmo/rules/product/05-document-lifecycle.md)。

## 5. 接入方式

业务开发者通常不直接复制本目录，而是通过 CLI 使用：

生成或改造业务模块前，先确认本次会用到的 Mango 能力说明：

- 能力索引：[Mango 能力地图](../mango-docs/capabilities/README.md)。
- 持久化基线：[Persistence 持久化](../mango/mango-infra/mango-infra-persistence/README.md)。
- 平台能力：按业务场景进入对应模块 README，例如 Authorization、File、Job、Workflow。
- 前端能力包：按实际依赖进入 `mango-ui/packages/<package>/README.md`。

```bash
mango module add order --aggregate sales-order --aggregate-name 销售订单 --module-name 订单模块 --project-dir .
```

显式传入的 `--module-name` 与 `--aggregate-name` 是中文展示名，至少包含一个中文字符。CLI 在创建目录或修改受管文件前校验；纯英文显示名失败时不改变业务项目。

老业务升级 Mango PMO baseline 时，从业务项目根目录执行：

```bash
mango pmo status --project-dir .
mango pmo check --project-dir .
mango pmo upgrade --project-dir . --dry-run
mango pmo upgrade --project-dir . --sync-shell
```

升级后每个 active worktree 都要重新确认本地工作区：

```bash
mango workspace init
mango workspace status
mango dev doctor
mango dev start
```

只启动后端或前端时使用 `mango dev start backend`、`mango dev start frontend`；`scripts/dev-workspace.sh` 只作为旧命令兼容 shim。

命令会生成：

| 生成位置                                           | 内容                                                     | 业务开发下一步                                                                            |
| -------------------------------------------------- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `backend/modules/<module>/<module>-api`            | API 接口、Command、Query、VO                             | 明确字段、参数校验、返回模型和接口语义                                                    |
| `backend/modules/<module>/<module>-core`           | Entity、Mapper、Service、Flyway SQL                      | 设计表结构、索引、租户字段、查询条件和业务逻辑                                            |
| `backend/modules/<module>/<module>-starter`        | Controller、自动配置、module metadata、resource manifest | 接入 Web、菜单权限资源和模块启动                                                          |
| `backend/modules/<module>/<module>-starter-remote` | Feign client 自动配置                                    | 微服务调用方按需依赖                                                                      |
| `frontend/packages/<module>-api`                   | 只依赖 `@mango/api-schema` 的业务 API 工厂和 TS 类型     | 按[前端 Monorepo 规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)维护契约   |
| `frontend/packages/<module>`                       | 页面注册、API 组合层和 Element Plus CRUD 页面            | 页面只管理交互状态；卸载时取消未完成请求                                                  |
| `frontend/src/main.ts`                             | host 请求实例和业务页面注册                              | 每个 app 创建 `@mango/http-client`，通过 `app.provide(MANGO_HTTP_CLIENT_KEY, client)` 注入；页面注册函数保持无参 |
| `backend/pom.xml`                                  | 业务模块 Maven module                                    | 确认 `business-modules` managed block 已追加                                              |
| `backend/app/pom.xml`                              | app 依赖业务 starter                                     | 确认 `business-dependencies` managed block 已追加                                         |
| `backend/app/src/main/resources/application.yml`   | 业务 Flyway 模块开关                                     | 确认 `<module>.enabled: true` 已追加                                                      |
| `mango.config.json`                                | `businessModules` 登记                                   | 确认 module、aggregate、displayName 已登记                                                |

模板变量由 CLI 渲染：

| 变量                  | 示例                | 用途                                       |
| --------------------- | ------------------- | ------------------------------------------ |
| `{{projectKebab}}`    | `demo-admin`        | 前端业务包 scope 和项目名                  |
| `{{projectPascal}}`   | `DemoAdmin`         | 类名或显示名拼接                           |
| `{{moduleKebab}}`     | `order`             | 模块目录、artifactId、moduleCode、菜单路径 |
| `{{modulePackage}}`   | `order`             | Java package segment                       |
| `{{modulePascal}}`    | `Order`             | Java 类名、注册函数名                      |
| `{{moduleCamel}}`     | `order`             | 前端变量名                                 |
| `{{moduleName}}`      | `订单模块`          | 菜单模块名、OpenAPI tag                    |
| `{{aggregateKebab}}`  | `sales-order`       | 聚合路径、表名片段、页面路径               |
| `{{aggregatePascal}}` | `SalesOrder`        | Entity、Service、Command、VO 类名          |
| `{{aggregateCamel}}`  | `salesOrder`        | Service 变量名                             |
| `{{aggregateName}}`   | `销售订单`          | 页面文案、菜单名、权限名                   |
| `{{basePackage}}`     | `com.example.mango` | Java 根包名                                |
| `{{basePackagePath}}` | `com/example/mango` | Java 源码路径                              |

## 6. 配置说明

本目录自身没有运行时配置类。它通过模板变量、生成项目配置和 CLI managed block 生效。

| 配置入口                 | 字段 / Key                               | 默认值                                 | 含义                     | 影响行为                         | 源码入口                            |
| ------------------------ | ---------------------------------------- | -------------------------------------- | ------------------------ | -------------------------------- | ----------------------------------- |
| `mango.config.json`      | `businessModules[].module`               | CLI 参数 `<module>`                    | 业务模块 code            | 记录已生成业务模块               | `updateBusinessConfig`              |
| `mango.config.json`      | `businessModules[].aggregate`            | `--aggregate`                          | 聚合 code                | 记录模块默认聚合                 | `updateBusinessConfig`              |
| `mango.config.json`      | `businessModules[].package`              | module camel case                      | Java package segment     | 生成模块源码路径和包名           | `toJavaSegment`                     |
| `mango.config.json`      | `businessModules[].displayName`          | `<Module>模块` 或 `--module-name`      | 模块中文名               | 菜单模块名、OpenAPI tag          | `addBusinessModule`                 |
| `mango.config.json`      | `businessModules[].aggregateDisplayName` | aggregate Pascal 或 `--aggregate-name` | 聚合中文名               | 页面文案、菜单名、权限名         | `addBusinessModule`                 |
| `application.yml`        | `<module>.enabled`                       | `true`                                 | 业务 Flyway 模块启用开关 | 后端启动时纳入业务模块 migration | `updateBackendBusinessFlywayConfig` |
| typed Resource declaration | `appCode`                              | `internal-admin`                       | 菜单权限归属应用         | Bootstrap 资源同步时归入内部管理端 | `META-INF/mango/resources/*.json`   |
| typed Resource declaration | `moduleCode`                           | `{{moduleKebab}}`                      | 菜单权限归属模块         | 菜单、权限唯一归属               | `META-INF/mango/resources/*.json`   |
| `module.properties`      | `module-name`                            | `{{moduleKebab}}`                      | Mango 模块名             | 模块资源发现                     | `module.properties`                 |
| `module.properties`      | `module-path`                            | `{{moduleKebab}}`                      | Mango 模块路径           | 模块资源发现                     | `module.properties`                 |

生成后把模板默认字段改成真实业务字段，避免只保留 `name` 示例字段交付。

## 7. API 与扩展

### 7.1 后端分层

| 模块                      | 生成内容                                                                               | 依赖                                                                                              | 修改重点                                  |
| ------------------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| `<module>-api`            | `{{modulePascal}}Api`、`Create...Command`、`Update...Command`、`...PageQuery`、`...VO` | `mango-common`、`mango-infra-persistence-starter`、Spring Web、Validation、Swagger                | 定义稳定 API 契约、字段校验、分页查询条件 |
| `<module>-core`           | `...Entity`、Mapper、Service 接口和实现、Flyway SQL                                    | `<module>-api`、`mango-infra-persistence-starter`                                                 | 表结构、租户字段、查询实现、业务规则      |
| `<module>-starter`        | Controller、AutoConfiguration、`module.properties`、typed Resource declarations        | `<module>-api`、`<module>-core`、`mango-infra-web-starter`、`mango-infra-persistence-web-starter` | Web 暴露、菜单权限资源、应用依赖          |
| `<module>-starter-remote` | Feign client 和 AutoConfiguration                                                      | `<module>-api`、`mango-infra-feign-starter`                                                       | 微服务调用方远程访问                      |

模板 API 形态：

| 能力 | 后端接口方法 | 前端请求函数                   | 默认说明                          |
| ---- | ------------ | ------------------------------ | --------------------------------- |
| 创建 | `create`     | `create{{aggregatePascal}}`    | 接收 `Create...Command`           |
| 修改 | `update`     | `update{{aggregatePascal}}`    | 接收 `Update...Command`           |
| 删除 | `delete`     | `delete{{aggregatePascal}}`    | 接收 `DeleteCommand`              |
| 分页 | `page`       | `page{{aggregatePascal}}`      | 接收 `...PageQuery`，返回分页结果 |
| 详情 | `detail`     | `get{{aggregatePascal}}Detail` | 按 id 查询                        |

Controller 使用 `BaseCrudController`，类级路径由 module 和 aggregate 渲染；接口契约在 `{{modulePascal}}Api` 中声明。微服务调用方使用 `<module>-starter-remote` 中的 Feign client。

### 7.2 前端包

| 包             | 导出                                                                                                    | 依赖                                                                                     | 适用场景                     |
| -------------- | ------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- | ---------------------------- |
| `<module>-api` | TS 类型、`create<Aggregate>Api(HttpClient)` CRUD 工厂                                                   | `@mango/api-schema`                                                                      | 页面包、其他业务前端调用 API |
| `<module>`     | `{{moduleCamel}}PageRegistry`、`register{{modulePascal}}Pages()`、API re-export、公开样式入口 | `<module>-api`、`@mango/admin-pages`、`@mango/api-schema`、`@mango/common`、Element Plus | 管理后台页面注册             |
| admin app      | `createMangoAdminApp()` 调用和业务页面注册                                                              | `@mango/admin`                                                                           | 业务后台入口                 |

页面默认使用 `@mango/common` 的 `MangoListPage`、`MangoSearchPanel`、`MangoListPanel` 和 `Pagination` 组织查询、功能区、表格区和分页区，短表单使用 `MangoDialog`，列表上下文中的短详情继续使用 Element Plus Drawer。独立详情页使用 `MangoDetailPage` 和 `MangoPageSection`，独立表单页使用 `MangoFormPage` 和 `MangoPageSection`。搜索区默认启用常用项折叠，业务把高频条件放在前面，展开后显示全部条件。业务交付时应补齐真实字段、权限控制、空状态、错误态和 E2E。

生成项目的 PMO required check 会对本次新增或修改的 `views/**/*.vue` 执行页面基线检查。列表页缺少列表四件套、标准弹框直接新增原生 `ElDialog` 时会失败。机器误判或页面确属特殊场景时，可在 `.vue` 文件中登记按类型例外 `<!-- mango-page-baseline-exception list: <具体、可复核的原因> -->`；整个页面均不适用默认骨架时，登记整页例外 `<!-- mango-page-baseline-exception all: <具体、可复核的原因> -->`。支持的类型及原因要求见项目内 [Admin UI 通用规范](./business-pmo/mango-baseline/rules/frontend/07-admin-ui-common.md)。

## 8. 数据与初始化

模板生成一个 Flyway migration 起点和一个菜单权限资源清单。

| 类型             | 位置                                                                                                    | 初始化内容                                                                      | 幂等键 / 唯一键                                       | 生效时机                                    | 排查入口                             |
| ---------------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- | ----------------------------------------------------- | ------------------------------------------- | ------------------------------------ |
| Flyway migration | `backend/modules/<module>/<module>-core/src/main/resources/db/migration/<module>/V1__init_<module>.sql` | `<module>_<aggregate>` 业务表示例，字段包含 `id`、`name`、`tenant_id`、审计字段 | 由表主键和 Flyway version 控制                        | 后端应用启动，且业务 Flyway 模块启用        | Flyway history、业务表、后端启动日志 |
| Flyway 模块开关  | `backend/app/src/main/resources/application.yml`                                                        | `<module>.enabled: true`                                                        | module code                                           | `mango module add` 写入后，下次应用启动生效 | application.yml managed block        |
| 资源声明         | `<module>-starter/src/main/resources/META-INF/mango/resources/<module>-common-menu.json`              | 模块菜单、聚合列表页、create/view/update/delete 权限                            | declaration id、version、bizKey 和目标模块 | Bootstrap `BOOTSTRAP_REQUIRED` 处理 | 菜单树、权限码、资源同步日志         |
| 模块元数据       | `<module>-starter/src/main/resources/META-INF/mango/module.properties`                                  | `module-name`、`module-path`                                                    | module name                                           | 模块资源发现阶段                            | 打包产物和模块扫描日志               |

默认 Entity 继承 `TenantEntity`，默认 SQL 也包含 `tenant_id`。生成后如果业务不使用租户隔离，要明确删除或解释；如果使用租户隔离，要把查询、写入、测试和数据权限补齐。

## 9. 管理入口

模板默认资源清单：

| 菜单 / 页面 | component key                              | 权限码                                                                  | 入库来源                 | 默认套餐 / 角色                  | 后端校验入口                               |
| ----------- | ------------------------------------------ | ----------------------------------------------------------------------- | ------------------------ | -------------------------------- | ------------------------------------------ |
| 模块目录    | 无                                         | 无                                                                      | typed Resource declaration | 由授权模块资源同步和角色授权决定 | 无直接 Controller                          |
| 聚合管理页  | `{{moduleKebab}}/{{aggregateKebab}}/index` | `{{moduleKebab}}:{{aggregateKebab}}:create`、`view`、`update`、`delete` | typed Resource declaration | 模板不直接授予角色               | `{{modulePascal}}Controller`、业务 Service |

生成后重点检查：

- `menuCode` 是否符合业务模块命名；声明文件位置为 `META-INF/mango/resources/`，并使用当前 schema。
- `path`、`redirect`、`component` 是否能和前端页面注册对上。
- 权限码是否覆盖页面按钮和后端接口；模板页面默认没有按钮级权限判断，需要业务补齐。
- 租户隔离是否和 `TenantEntity`、`tenant_id`、查询条件、当前登录上下文一致。
- 默认资源清单只登记资源，不代表用户已经拥有权限；角色授权仍由授权模块或业务初始化流程完成。

## 10. 快速开始

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

## 11. 问题排查

| 问题                                           | 原因                                                                                              | 处理方式                                                                                                                      |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| 生成后菜单打不开                               | 前端页面 key、resource manifest component、注册函数或请求实例注入不一致                           | 检查 `register<Module>Pages()`、host 的 `app.provide(MANGO_HTTP_CLIENT_KEY, client)` 和 component key                          |
| 后端启动没有建业务表                           | Flyway 模块未启用或 migration 路径不在扫描范围                                                    | 检查 `application.yml` 的 `<module>.enabled: true` 和 migration 路径                                                          |
| 页面有按钮但权限不生效                         | 模板页面默认没有按钮级权限判断                                                                    | 接入前端权限指令或组件，并在后端接口补权限校验                                                                                |
| 数据跨租户可见                                 | 模板只提供 `tenant_id` 起点，业务未补查询和写入约束                                               | 检查 `TenantEntity`、当前租户上下文、Mapper 查询和测试数据                                                                    |
| 微服务调用方依赖了 core                        | 混淆了提供方和调用方依赖                                                                          | 调用方改依赖 `<module>-starter-remote`                                                                                        |
| 模板校验通过但业务链路失败                     | `check-template.mjs` 只校验模板静态契约                                                           | 生成项目后继续跑 Maven、前端构建、后端启动和 E2E                                                                              |
| partial PR 报 Mango 上游 SNAPSHOT 找不到       | Runner 本地仓库为空，旧 workflow 直接进入质量阶段                                                 | 升级业务 PMO baseline，确认依赖准备步骤使用 `maven_dependency_projects` 和 `-am install`，质量步骤仍不带 `-am`                |
| 嵌套静态分析报架构治理属性缺失                 | 旧版 Mango Maven 插件把 `architecture-verification` 带入了 PMD、Checkstyle、SpotBugs 的二次 Maven | 升级到包含治理聚合模块过滤的 Mango Maven 插件；外层 `mvn verify` 继续保留架构门禁                                             |
| 纯升级 PR 被旧实施计划缺少 `documentType` 阻断 | PMO 合同启用前的历史计划尚未迁移                                                                  | 使用 PMO 1.2.5+，将每份存量文档按路径和当前 SHA-256 登记到 `.mango-pmo-legacy-documents.json`；内容变化后需迁移或重新审批基线 |
| PR required check 因 Risk / Verification 缺失或旧字段失败 | 业务仓缺少 PR 模板，或模板未随 delivery-assurance 合同升级 | 安装包含 schema revision 5 的 PMO/CLI 补丁版后执行项目内 `mango pmo upgrade` 或 `sync`；已创建 PR 直接编辑正文 |
| 已生成项目升级模板困难                         | 业务代码已经改过，不能直接覆盖                                                                    | 用 CLI managed block 同步可管理部分，其余人工迁移                                                                             |

## 12. 相关文档

- [PMO 总流程](../mango-pmo/rules/00-dev-flow.md)
- [AI 编码红线](../mango-pmo/rules/03-ai-coding-redlines.md)
- [交付质量门禁](../mango-pmo/rules/05-ai-delivery-quality.md)
- [文档资产边界](../mango-pmo/rules/06-document-assets.md)
- [能力说明维护](../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料

- v2026.07.21 发布候选将业务 PMO baseline 对齐到 `@mango/pmo@1.3.4` 和 `@mango/cli@1.0.88`，同步 delivery-assurance schema revision 5、canonical PR 模板、文档合同 fixture、plugin manifest 投影和 release package 元数据；业务项目升级后只影响 PMO 检查、preflight、文档合同和发布治理物料，不改变业务 API、菜单、权限、租户、页面、启动方式或运行时业务逻辑。
- v2026.07.19 本地候选将业务 API 改为 `createXxxApi(HttpClient)`，host 使用 `@mango/http-client@1.0.0` 注入，页面传递取消信号且不再引用 Axios/全局 request；真实 CMS 页面和 Wujie 多实例宿主链路已纳入验证，候选锁为 `@mango/pmo@1.3.2`、`@mango/cli@1.0.84` 和同批前端包。当前未发布，业务项目只能在本地 tarball/candidate 验证后试用。
- v2026.07.18 将项目治理锁更新到 `@mango/pmo@1.3.1` 和 `@mango/cli@1.0.82`；Mango 发布只路由到项目内 `mango-release` Skill，并补齐发布说明预检、不可变制品恢复、CHANGELOG 回填和环境清理。Mango Maven、运行时前端包、菜单、权限、租户和业务逻辑不变。
- v2026.07.18 CLI 热修将项目生成版本更新到 `@mango/cli@1.0.81`，为新项目补齐 pnpm 11 所需的前端 workspace 构建白名单；Mango Maven `1.0.22`、`@mango/pmo@1.3.0`、菜单、权限、租户和运行时业务逻辑不变。
- v2026.07.18 将业务锁对齐到 `@mango/admin@1.0.49`、`@mango/cli@1.0.80`、`@mango/pmo@1.3.0` 和 Mango Maven `1.0.22`。Maven `1.0.22` 不兼容地以 `ResourceDeclarationApi` 替换 `ResourceRegistryApi`，业务代码需自行迁移，并使用 `mango-resource-sync-starter` 承接资源同步。
- v2026.07.14 将管理端锁对齐到 `@mango/admin@1.0.46`，并配套 `@mango/cli@1.0.78`、`@mango/pmo@1.2.6` 和 Mango Maven `1.0.21`；模板结构、模块生成协议、菜单权限初始化方式和运行时业务逻辑不变。
- [业务 PMO 说明](./business-pmo/README.md)
- [业务 baseline](./business-pmo/mango-baseline/README.md)
- [单体拓扑说明](./topologies/monolith/README.md)
- [微服务拓扑说明](./topologies/microservice/README.md)
- [Mango 能力地图](../mango-docs/capabilities/README.md)

## 14. Issue #690 升级登记

使用过 Mango Maven `1.0.30` 或其它 `1.0.3x` 组合的业务仓属于 #690 回归影响范围。修复 tuple 是 Maven `1.0.31`、`@mango/pmo@1.3.9`、`@mango/cli@1.0.96` 和根 `CHANGELOG.md` 中列出的完整前端矩阵；旧 `1.0.30`/CLI `1.0.95`/PMO `1.3.8` 组合只作为升级来源。升级按 [CLI 升级合同](../mango-ui/packages/mango-cli/README.md) 和 [1.0.30 到 1.0.31 升级指南](../mango-docs/guides/business-integration/mango-1.0.30-to-1.0.31-upgrade.md) 成组执行。

升级时保留数据库和工作区审计证据，先升级 CLI/PMO，再统一更新 `<mango.version>` 或 `mango-bom`，执行冻结安装、完整 Maven Reactor、前端检查、`workspace init`、Bootstrap receipt 校验和真实业务验收。已有数据库不重建、不删除业务数据；滚动升级在 `finalize` 前可用 `bootstrap abort` 撤回候选 generation。模块资源迁移到 `META-INF/mango/resources/*.json|yml|yaml` typed declarations，Flyway 只负责 DDL 和大 SQL。
