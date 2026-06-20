# Mango Backend

## 1. 概览
`mango/` 是 Mango 后端工程根目录，提供 Java 21、Spring Boot、Spring Cloud、MyBatis-Plus、Flyway、KV、权限、认证、文件、工作流、支付等后端能力的源码、依赖管理和可部署应用入口。

业务开发者主要通过这里判断：能力模块在哪里、接入哪个 starter、单体还是微服务怎么装配、配置写在哪里、初始化数据由谁负责、交付前跑哪些验证。


## 2. 能力边界
- 不在根工程直接写业务 Controller、Service、Mapper 或 migration。
- 不把业务配置硬编码进根 `pom.xml`。
- 不在 app 层实现领域规则；app 只负责部署装配。
- 不把 README 当成长期规范源，长期规则以 `mango-pmo/rules/**` 为准。

## 3. 模块入口
后端目录按职责分层：

| 目录 | 职责 | 业务接入时关注点 |
|------|------|------------------|
| `mango-parent` | 父 POM 和插件默认配置 | 依赖版本、构建默认值。 |
| `mango-common` | 公共返回体、异常、分页、工具和通用契约 | API 返回、错误码、分页模型。 |
| `mango-infra` | 技术基础设施 | Web、持久化、KV、事件、日志、加解密、文档、Feign、实时通信。 |
| `mango-platform` | 平台业务能力 | 认证、身份、授权、组织、系统、文件、工作流、模板、支付等。 |
| `mango-admin-starter` | 单体管理后台聚合 starter | 本地开发和单体装配。 |
| `mango-app` | Spring Boot 部署入口 | 单体、微服务、文件预览独立应用。 |
| `mango-extension` | 可选扩展能力 | 扩展能力按需接入。 |
| `mango-tools` | Maven 插件和规则检查 | 生成模块、生成 CRUD、规则门禁。 |

平台能力一般采用 `api`、`core`、`starter`、`starter-remote` 拆分：服务提供方引入本地 `starter`，服务调用方引入 `starter-remote`。

## 4. 接入方式
业务项目推荐优先用 `mango-business-starter` 或 CLI 生成工程，再按模块 README 接入能力。

单体装配：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-admin-starter</artifactId>
</dependency>
```

`mango-admin-starter` 已包含本地 Resource Registry runtime 和资源声明同步 runtime；业务按官方单体入口接入时不需要再额外声明 `mango-resource-starter` 或 `mango-resource-sync-starter`。

微服务提供方装配本地能力：

```xml
<dependency>
    <groupId>io.mango.platform.system</groupId>
    <artifactId>mango-system-starter</artifactId>
</dependency>
```

微服务调用方装配远程客户端：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-starter-remote</artifactId>
</dependency>
```

常用命令：

```bash
mvn -f mango/pom.xml -pl :mango-monolith-app -am test
mvn -f mango/pom.xml -pl :mango-monolith-app -am spring-boot:run
mvn -f mango/pom.xml mango:check
mvn -f mango/pom.xml mango:gen-module -Dname=demo
mvn -f mango/pom.xml mango:gen-crud -Dmodule=demo -Dentity=Demo -Dtable=demo_record
```

## 5. 配置说明
根工程只管理 Maven 版本和模块清单，运行时配置在具体 app 的 `application.yml` 或环境文件中。

| 配置位置 | 关键配置 | 含义 |
|----------|----------|------|
| `mango/pom.xml` | `revision` | 当前后端模块版本，默认 `1.0.0-SNAPSHOT`。 |
| `mango/pom.xml` | `maven.compiler.release` | Java 编译版本，当前为 21。 |
| `mango/pom.xml` | `spring-boot.version` | Spring Boot 版本，当前为 3.5.14。 |
| `mango/pom.xml` | dependency management | 管理 Mango 内部模块和第三方依赖版本。 |
| `.env.development` | `MANGO_BACKEND_PORT`、`MANGO_DB_URL`、`MANGO_DB_USERNAME`、`MANGO_DB_PASSWORD`、`MANGO_FILE_ROOT` | 本地单体联调常用环境变量。 |
| `mango-app/**/application.yml` | `mango.persistence.flyway.modules.*.enabled` | 控制各模块 Flyway migration 是否执行。 |
| `mango-app/**/application.yml` | `mango.kv.store.type` | KV 存储类型，常见为 `jdbc`、`redis`、`memory`。 |
| `mango-app/**/application.yml` | `mango.security.jwt.secret` | JWT 密钥，生产环境改成正式密钥。 |

具体配置字段以对应模块 README 和 app `application.yml` 为准。

## 6. API 与扩展
根工程本身不暴露业务 API，扩展点由各模块提供：

- Web 和统一异常：`mango-infra-web`。
- 持久化、审计字段、租户、Flyway：`mango-infra-persistence`。
- 认证登录：`mango-auth`。
- 用户和身份事实：`mango-identity`。
- 菜单、权限、角色和资源同步：`mango-authorization`。
- 系统字典、配置、租户、日志：`mango-system`。
- 文件、预览、模板、工作流、支付等按各模块 README 接入。

## 7. 数据与初始化
根工程不直接拥有业务表。数据库结构、菜单、权限、字典、租户、种子数据由各能力模块的 migration、Runner 或 Initializer 提供。

接入新模块时重点确认：

- starter 已被 app 引入。
- migration 被 `mango.persistence.flyway.modules.<module>.enabled` 打开。
- 菜单资源写入 authorization 的菜单和权限表。
- 初始化逻辑是幂等的，重复启动不会重复插入脏数据。

## 8. 管理入口
根工程不配置具体菜单。菜单、权限、租户由 `mango-authorization`、`mango-system` 和各业务模块 migration 协作初始化。

业务开发新增后端能力时要同时登记：

- API 是否需要 `@Permission` 或公开路径。
- 菜单 component 是否能命中前端页面 key。
- 租户字段是否需要进入表结构和查询条件。
- 初始化菜单和权限是否包含在 migration 或初始化器中。

## 9. 快速开始
1. 通过 PMO preflight 读取本次任务需要遵守的规则。
2. 在能力地图中确认要接入的 Mango 模块。
3. 阅读目标模块 README，确认 starter、配置、API、migration、菜单和权限。
4. 单体场景把本地 `*-starter` 加入宿主 app；微服务调用场景把 `*-starter-remote` 加入调用方。
5. 配置数据库、KV、JWT、文件目录和 Flyway 模块开关。
6. 启动 app，验证 health、登录、菜单、权限和目标业务接口。
7. 为新增模块补 README 和验收命令，避免能力信息丢失。

## 10. 问题排查
- 启动后表不存在：检查 app 是否引入 starter、Flyway 模块开关是否打开、数据库是否连接到正确库。
- 菜单看不到：检查 authorization 菜单初始化、角色授权、租户应用绑定和前端页面 key。
- 接口 401 或 403：检查认证 token、公开路径、`@Permission`、角色权限和网关鉴权。
- 本地端口冲突：优先通过 `.env.development` 或启动参数修改端口。
- 业务逻辑写到 app 层：应移动到业务模块的 core/starter，app 只保留部署装配。

## 11. 相关文档
- [后端代码规范](../mango-pmo/rules/backend/01-code.md)
- [后端 API 规范](../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../mango-pmo/rules/backend/06-security.md)
- [持久化规范](../mango-pmo/rules/backend/07-persistence.md)
- [后端测试规范](../mango-pmo/rules/backend/08-test.md)
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)

## 12. 补充资料
- [Mango 能力地图](../mango-docs/capabilities/README.md)
- [App 拓扑 README](./mango-app/README.md)
- [Admin Starter README](./mango-admin-starter/README.md)
- [Common README](./mango-common/README.md)
- [Mango PMO 规则索引](../mango-pmo/rules/index.json)
