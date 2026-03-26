# API 规范 (api-rules)

## 1. RESTful URL 设计

### 1.1 URL 格式规范

| 方法 | URL | 说明 |
|------|-----|------|
| GET | /api/users | 列表 |
| GET | /api/users/{id} | 详情 |
| POST | /api/users | 创建 |
| PUT | /api/users/{id} | 全量更新 |
| PATCH | /api/users/{id} | 部分更新 |
| DELETE | /api/users/{id} | 删除 |

### 1.2 URL 命名规则

```
- 使用小写字母
- 使用横杠分隔单词: /api/user-orders
- 禁用下划线: /api/user_orders (❌)
- 禁用动词: /api/getUser (❌)
- 资源用复数: /api/users (不是 /api/user)
```

---

## 2. 请求格式

### 2.1 请求头

```
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
```

### 2.2 分页请求

```json
GET /api/users?page=1&size=20&sort=createdAt,desc

{
  "page": 1,
  "size": 20,
  "sort": "createdAt,desc"
}
```

### 2.3 搜索请求

```json
GET /api/users?keyword=张三&status=ACTIVE

{
  "keyword": "张三",
  "status": "ACTIVE"
}
```

---

## 3. 响应格式

### 3.1 标准响应

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 3.2 列表响应（带分页）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "totalPages": 5
    }
  }
}
```

### 3.3 错误响应

```json
{
  "code": 400,
  "message": "参数错误",
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    }
  ]
}
```

---

## 4. 状态码

### 4.1 HTTP 状态码

| 状态码 | 含义 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功（无内容） |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 4.2 业务状态码

```json
{
  "code": 200,   // HTTP 状态码
  "subCode": "USER_NOT_FOUND",  // 业务子码
  "message": "用户不存在"
}
```

---

## 5. 版本管理

### 5.1 URL 版本

```
/api/v1/users
/api/v2/users
```

### 5.2 Header 版本

```
Accept: application/vnd.mango.v1+json
```

---

## 6. API 文档

### 6.1 Swagger/OpenAPI 注解

```java
@RestController
@RequestMapping("/api/users")
@Api(tags = "用户管理")
public class UserController {

    @GetMapping("/{id}")
    @ApiOperation("获取用户详情")
    @ApiImplicitParam(name = "id", value = "用户ID", required = true)
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

---

## 7. API 安全

### 7.1 鉴权

```java
// ✅ 需要登录
@GetMapping("/api/user")
@PreAuthorize("isAuthenticated()")
public User getCurrentUser() { }

// ✅ 需要特定权限
@DeleteMapping("/api/users/{id}")
@PreAuthorize("hasAuthority('user:delete')")
public void deleteUser(@PathVariable Long id) { }
```

### 7.2 限流

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1640995200
```
