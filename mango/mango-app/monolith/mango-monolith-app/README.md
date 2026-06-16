# Mango Monolith App

## 1. 概览
`mango-monolith-app` 是 Mango 单体部署入口，在一个 Spring Boot 进程中装配管理后台所需的平台能力、基础设施能力和本地业务 starter。

它适合本地开发、功能验收、小规模部署和业务模块初期开发。业务逻辑仍应放在业务模块，不能写在 app 层。

## 2. 当前装配能力

当前 app 通过 `mango-admin-starter` 聚合平台能力，并在 `application.yml` 打开多数组件配置。

主要能力包括：

- auth、identity、authorization、org、system、captcha。
- file、file-preview、template、workflow、job、notice、calendar、numgen、payment、domain。
- infra web、persistence、kv、event、realtime、ip-location、crypto。

## 3. 启动方式

推荐从仓库根目录使用统一环境文件：

```bash
scripts/dev-env.sh .env.development backend
```

前后端一起启动：

```bash
scripts/dev-env.sh .env.development all
```

直接用 Maven 启动：

```bash
mvn -f mango/pom.xml -pl :mango-monolith-app -am spring-boot:run
```

临时改端口：

```bash
mvn -f mango/pom.xml -pl :mango-monolith-app -am spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=5555"
```

## 4. 配置说明
`.env.development` 常用配置：

| 变量 | 含义 |
|------|------|
| `MANGO_BACKEND_PORT` | 后端端口，默认示例为 `5555`。 |
| `MANGO_DB_URL` | MySQL JDBC 地址。 |
| `MANGO_DB_USERNAME` | 数据库用户名。 |
| `MANGO_DB_PASSWORD` | 数据库密码。 |
| `MANGO_FILE_ROOT` | 本地文件存储根目录。 |
| `MANGO_CRYPTO_SM4_SECRET_KEY` | SM4 密钥。 |
| `MANGO_DEFAULT_TENANT_ID` | 默认租户 id。 |
| `MANGO_JOB_PROBE_ENABLED` | job 探针开关。 |

`application.yml` 关键配置：

| 配置 | 默认值 / 说明 |
|------|---------------|
| `server.port` | `${MANGO_BACKEND_PORT:5555}` |
| `spring.datasource.*` | 使用 `MANGO_DB_*` 环境变量。 |
| `spring.servlet.multipart.max-file-size` | `512MB`。 |
| `spring.servlet.multipart.max-request-size` | `1024MB`。 |
| `mango.kv.store.type` | `jdbc`。 |
| `mango.file.storage-type` | `LOCAL`。 |
| `mango.file.local.root-path` | `${MANGO_FILE_ROOT:./data/files}`。 |
| `mango.ip-location.ip2region.xdb-location` | `file:./config/ip-location/ip2region_v4.xdb`。 |
| `mango.persistence.mybatis-plus.tenant.default-tenant-id` | `${MANGO_DEFAULT_TENANT_ID:1}`。 |

## 5. Flyway / 初始化数据

单体默认通过 `mango.persistence.flyway.modules.*.enabled` 打开平台模块 migration：

- `system`
- `authorization`
- `identity`
- `org`
- `captcha`
- `file`
- `template`
- `workflow`
- `mango-job`
- `kv`
- `notification`
- `calendar`
- `numgen`
- `notice`
- `payment`
- `domain`

正常开发不要关闭 Flyway。清库后，表结构、菜单、权限、字典、租户、默认用户、编号规则和业务模块基础数据都依赖 migration 或初始化器恢复。

## 6. 管理入口
单体装配 authorization、identity、org、system 和 access 能力。登录、菜单、按钮权限、租户和数据权限在同一进程内完成。

业务模块接入时需要确认：

- 菜单和权限 migration 是否启用。
- 菜单 component 是否能命中前端页面 key。
- API 是否声明权限或公开路径。
- 表结构是否包含租户字段和审计字段。

## 7. 业务模块接入

把业务模块本地 starter 加到单体 app 或业务单体宿主：

```xml
<dependency>
    <groupId>io.mango.business.demo</groupId>
    <artifactId>mango-demo-starter</artifactId>
</dependency>
```

然后确认：

1. starter 带入 Controller、Service、Mapper、migration、Runner 或 Initializer。
2. `application.yml` 打开业务模块 Flyway 开关。
3. 前端菜单 component 有对应页面注册。
4. 启动后访问业务接口不返回 404，菜单和权限可见。

zation/menus/user?fmt=tree&appCode=internal-admin` 能返回当前用户菜单。
- `/swagger-ui.html` 或 `/v3/api-docs` 能访问。
- 文件上传、工作流、模板、支付等已启用模块能打开对应后台页面。

## 8. 问题排查
- 表或菜单缺失：检查数据库是否连错、Flyway 是否关闭、starter 是否进入 classpath。
- 文件上传失败：检查 `MANGO_FILE_ROOT` 是否可写，以及请求大小是否超过配置。
- IP 定位失败：检查 `config/ip-location/ip2region_v4.xdb` 是否存在。
- 登录成功但菜单为空：检查 authorization 初始化、角色授权、租户绑定和前端页面 key。
- 根目录启动误执行非 app 模块：当前只有应用模块声明 `mainClass` 并开启 `spring-boot:run`，依赖模块只参与编译。

## 9. 相关文档
- [Mango App README](../../README.md)
- [微服务部署入口](../../microservice/README.md)
- [Admin Starter README](../../../mango-admin-starter/README.md)
- [Authorization README](../../../mango-platform/mango-authorization/README.md)
- [能力说明维护规范](../../../../mango-pmo/rules/08-capability-docs.md)
