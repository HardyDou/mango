# mango-infra-doc

> API 文档基础设施 - OpenAPI/Swagger UI

## 已实现

- **SpringDoc OpenAPI 3 集成** - Spring Boot 3.x 兼容
- **Swagger UI** - 在线 API 文档界面
- **`@ConfigurationProperties` 模式** - `mango.doc.*` 前缀
- **分组 API** - 支持多组 API 文档

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

### 添加 API 注解

```java
@RestController
@RequestMapping("/api/users")
@Api(tags = "用户管理")
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "获取用户", description = "根据 ID 获取用户详情")
    @ApiResponse(responseCode = "200", description = "成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public User getUser(@PathVariable Long id) {
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
- 默认只暴露 `public` 分组，避免内部 API 泄露
- Swagger UI 默认启用，可通过配置禁用
