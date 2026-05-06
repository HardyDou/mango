# mango-infra-doc

> 可选开发体验模块 - OpenAPI/Swagger UI

## 已实现

- **SpringDoc OpenAPI 3 集成** - Spring Boot 3.x 兼容
- **Swagger UI** - 在线 API 文档界面
- **`@ConfigurationProperties` 模式** - `mango.doc.*` 前缀
- **模块分组 API** - 基于 `META-INF/mango/module.properties` 按模块生成 API 文档分组
- **接口范围标记** - 基于 `@ApiAccess(mode = INTERNAL)` 或 `@InternalApi` 标记对内接口，其余接口标记为对外接口
- **单体/微服务兼容** - 单体应用直接暴露本应用文档；微服务可由网关聚合各服务 `/v3/api-docs/{group}` 后在 Swagger UI 下拉切换模块

`mango-infra-doc` 只面向开发和联调体验，不属于核心运行时能力。

## 依赖

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-doc-starter</artifactId>
</dependency>
```

## 配置属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `mango.doc.enabled` | `true` | 是否启用 |
| `mango.doc.title` | `Mango API` | 文档标题 |
| `mango.doc.description` | `Mango Scaffold API Documentation` | 文档描述 |
| `mango.doc.version` | `1.0.0` | API 版本 |
| `mango.doc.group` | `public-api` | API 分组名 |
| `mango.doc.pathsToMatch` | `/api/**` | 包含的路径 |
| `mango.doc.moduleGrouping.enabled` | `true` | 是否按模块元数据生成分组 |
| `mango.doc.moduleGrouping.includeDefaultGroup` | `true` | 是否保留默认全局分组 |
| `mango.doc.moduleGrouping.includeScopeTags` | `true` | 是否标记对内/对外接口 |
| `mango.doc.contact.name` | `Mango Team` | 联系人 |
| `mango.doc.contact.email` | `mango@example.com` | 联系邮箱 |

## 使用示例

```yaml
mango:
  doc:
    enabled: true
    title: User Management API
    description: 用户管理服务 API 文档
    version: 1.0.0
    group: user-api
    pathsToMatch:
      - /api/users/**
      - /api/roles/**
    moduleGrouping:
      enabled: true
      includeDefaultGroup: true
      includeScopeTags: true
    contact:
      name: Mango Team
      email: api@mango.com
```

### 访问 Swagger UI

```
GET /swagger-ui.html          # Swagger UI
GET /v3/api-docs              # OpenAPI 3 JSON
GET /v3/api-docs.yaml          # OpenAPI 3 YAML
GET /v3/api-docs/user-api     # 分组 API 文档
```

### 按模块查看接口

模块分组依赖各 starter 包中的 `META-INF/mango/module.properties`：

```properties
module-name=mango-auth
module-path=/auth
```

引入 `mango-infra-doc-starter` 后，Swagger UI 的分组下拉框会出现 `mango-auth` 这类模块名。选择模块后，会展示该模块路径下的所有接口，例如 `/auth/**`。

接口范围标记规则：

- 标注了 `@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)` 或 `@InternalApi` 的 Controller、方法或接口方法会标记为 `对内接口`。
- 未标注 `@ApiAccess(INTERNAL)` 的接口会标记为 `对外接口`。
- `@PublicApi`、`@LoginApi`、`@PermissionAccess` 会被识别为对外接口。
- OpenAPI 操作会同时带上 `x-mango-api-scope`，值为 `internal` 或 `external`。

### 微服务网关访问

微服务模式建议由 Gateway 暴露统一 Swagger 入口：

- 网关依赖 `mango-infra-doc-starter` 和服务发现能力。
- 各业务服务仍各自生成 `/v3/api-docs/{module}`。
- 网关聚合这些 OpenAPI 地址，在 Swagger UI 分组下拉中展示模块名。
- Try it out 请求统一走网关域名，鉴权请求携带 `Authorization` 和 `permissionCode` query 参数。

单体模式不需要聚合，直接访问当前应用的 `/swagger-ui.html` 和 `/v3/api-docs/{module}` 即可。

### 添加 API 注解

```java
@RestController
@RequestMapping("/api/users")
@Api(tags = "用户管理")
public class UserController {

    @GetMapping("/detail")
    @Operation(summary = "获取用户", description = "根据 ID 获取用户详情")
    @ApiResponse(responseCode = "200", description = "成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public User getUser(@RequestParam Long id) {
        return userService.getUser(id);
    }
}
```

## 待实现

| 功能 | 状态 | 说明 |
|------|------|------|
| Knife4j UI | 待开发 | 更强大的文档 UI |
| 离线文档导出 | 待开发 | PDF/Markdown 导出 |
| API 认证 | 待开发 | Swagger UI 认证配置 |
| 多语言支持 | 待开发 | 国际化文档 |

## 设计决策

- 使用 SpringDoc 而非 springfox，SpringFox 已停止维护且不兼容 Spring Boot 3.x
- SpringDoc 2.3.0 完整支持 OpenAPI 3.0 规范
- 默认保留全局分组，同时按 Mango 模块元数据生成模块分组
- 对内/对外接口在同一模块分组内展示，避免切换模块后看不到内部接口
- Swagger UI 默认启用，可通过配置禁用
