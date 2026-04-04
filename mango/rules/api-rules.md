# API 规范 (api-rules)

## 1. RESTful URL 设计

### 1.1 URL 格式规范

**核心原则：路径段数由业务语义决定，不强制固定段数。**

| 方法 | URL 示例 | 说明 |
|------|---------|------|
| GET | /sys/user | 列表 |
| GET | /sys/user/{id} | 详情 |
| POST | /sys/user | 创建 |
| PUT | /sys/user/{id} | 全量更新 |
| PATCH | /sys/user/{id} | 部分更新 |
| DELETE | /sys/user/{id} | 删除 |

### 1.2 URL 命名规则

```
- 使用小写字母
- 使用横杠分隔单词: /sys/user-order
- 禁用下划线: /sys/user_order (❌)
- 禁用动词: /sys/getUser (❌)
- 资源用复数: /sys/user (不是 /sys/users)
- 无 /api 前缀
```

### 1.3 路径归属规范

**核心原则：路径由 Controller 所在层决定，不由功能名称（admin/captcha）决定。**

#### 判断标准

| Controller 位置 | 路径格式 | 判断标准 |
|----------------|---------|---------|
| BFF 层（聚合接口） | `/bff/admin/{module}` | 聚合**多个**领域模块的数据 |
| 独立领域模块 Starter | `/{module}/...` | 该模块**自己的**接口 |

#### 判断流程

```
这个接口聚合了多个模块的数据？
  是 → BFF 层，路径 /bff/admin/...
  否 → 独立模块，路径 /{module}/...
```

**反面案例（今天踩的坑）**：
```
❌ CaptchaController 放在 BFF 层，路径 /bff/admin/captcha
   → 验证码是独立模块，不是聚合接口

✅ CaptchaController 在 mango-captcha-starter，路径 /captcha
   → 独立模块的接口，直接用模块路径
```

#### 公开端点 vs 认证端点

独立模块的接口，无论是否需要认证，都用模块自身路径：
- 公开：`/captcha/types`（无需认证获取类型）
- 认证：`/captcha/verify`（提交验证）
- **都不是** `/bff/admin/...`，因为它们不是聚合接口

#### BFF 聚合的场景

| 场景 | 路径 | 说明 |
|------|------|------|
| 聚合 user + role + permission | `/bff/admin/user/detail` | BFF 一次性返回用户、角色、权限 |
| 聚合 dashboard 数据 | `/bff/admin/dashboard/home` | BFF 聚合多个统计接口 |

> i18n 模块**不参与** BFF 聚合，所有 i18n 接口统一由 `/i18n/...` 提供。

#### i18n 模块路径

所有 i18n 接口统一由 i18n 模块负责，路径 `/i18n/...`：

| 场景 | 路径 | 说明 |
|------|------|------|
| 获取支持的语言 | `/i18n/languages` | 返回支持的语言列表 |
| 获取语言包 | `/i18n/{lang}` | 返回指定语言的语言包 |
| 公开翻译 | `/i18n/public` | 无需认证的翻译接口 |

**禁止**：不允许通过 BFF 聚合 i18n 接口。所有 i18n 能力由 i18n 模块直接提供。

#### 第一层路径占用

```
/area      — 只能属于 area 领域
/org       — 只能属于 org 领域
/user      — 只能属于 user 领域
/captcha   — 只能属于 captcha 领域
/bff/...   — BFF 层保留前缀，不占用独立领域
```

其他领域不得使用已占用的第一层路径。`/bff/` 是 BFF 专属前缀，独立模块不得使用。

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
GET /sys/user?page=1&size=20&sort=createdAt,desc

{
  "page": 1,
  "size": 20,
  "sort": "createdAt,desc"
}
```

### 2.3 搜索请求

```json
GET /sys/user?keyword=张三&status=ACTIVE

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

| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | 成功 | |
| 201 | 创建成功 | |
| 204 | 删除成功（无内容） | |
| 400 | 参数错误 | |
| 401 | 未认证 | |
| 403 | 无权限 | |
| 404 | 资源不存在 | |
| 409 | 资源冲突 | 如重复创建 |
| 428 | Precondition Required | 请求缺少前置条件（如验证码） |
| 500 | 服务器错误 | |

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
/v1/sys/user
/v2/sys/user
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
@RequestMapping("/sys/user")
@Tag(name = "用户管理", description = "用户管理接口")
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

> **注意**：使用 SpringDoc（`@Tag` + `@Operation`），而非旧版 Swagger（`@Api` + `@ApiOperation`）。

---

## 7. API 安全

### 7.1 鉴权

```java
// ✅ 需要登录
@GetMapping("/sys/user/current")
@PreAuthorize("isAuthenticated()")
public User getCurrentUser() { }

// ✅ 需要特定权限
@DeleteMapping("/sys/user/{id}")
@PreAuthorize("hasAuthority('sys:user:delete')")
public void deleteUser(@PathVariable Long id) { }
```

### 7.2 限流

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1640995200
```

---

## 8. 内部API规范

### 8.1 @Inner注解

用于标识内部API（**仅代码文档作用**，无运行时拦截）。
运行时拦截由 InternalCallFilter + Gateway path_type 实现。

### 8.2 验证码发送

验证码发送仅通过 CaptchaApi，不对外暴露HTTP接口。

业务方调用 CaptchaApi 发送验证码：

```java
// 用户注册
CaptchaSendRequest request = new CaptchaSendRequest();
request.setType(CaptchaType.SMS);
request.setTarget("13800138000");
request.setBusinessType("REGISTER");
request.setExpireSeconds(300L);
String key = captchaApi.send(request);

// 找回密码
request.setType(CaptchaType.EMAIL);
request.setTarget("user@example.com");
request.setBusinessType("FORGOT_PASSWORD");
```

**CaptchaSendRequest 字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | CaptchaType | 是 | SMS 或 EMAIL |
| target | String | 是 | 手机号或邮箱 |
| businessType | String | 是 | 业务类型：REGISTER, LOGIN, FORGOT_PASSWORD, CHANGE_MOBILE 等 |
| expireSeconds | Long | 否 | 有效期，默认300秒 |

### 8.3 内部API双重保护

参见 security-rules.md 第8节。
