---
paths:
  - "**/*Controller.java"
  - "**/*Service.java"
  - "**/*Mapper.java"
---

# 安全规范 (security-rules)

## 1. 硬编码禁止

### 1.1 禁止硬编码内容

```java
// ❌ 禁止 - 硬编码密钥
private static final String API_KEY = "abc123";
private static final String SECRET = "xxx";
private static final String JDBC_PASSWORD = "root";

// ❌ 禁止 - 硬编码 IP
private static final String SERVER_IP = "192.168.1.100";

// ✅ 正确 - 从配置读取
@Value("${api.key}")
private String apiKey;

@Value("${db.password}")
private String dbPassword;

@Configuration
public class SecurityConfig {
    @Value("${server.ip}")
    private String serverIp;
}
```

### 1.2 配置示例

```yaml
# application.yml
api:
  key: ${API_KEY}  # 从环境变量读取
  secret: ${API_SECRET}

db:
  password: ${DB_PASSWORD}

server:
  ip: ${SERVER_IP}
```

---

## 2. SQL 注入防护

### 2.1 必须使用参数化查询

```java
// ❌ 禁止 - SQL 拼接
@Select("SELECT * FROM user WHERE name = '" + name + "'")
User findByName(String name);

// ❌ 禁止 - String.format
String sql = String.format("SELECT * FROM user WHERE id = %d", id);

// ✅ 正确 - 参数化查询
@Select("SELECT * FROM user WHERE name = #{name}")
User findByName(@Param("name") String name);

// ✅ 正确 - MyBatis-Plus QueryWrapper
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("name", name);
```

### 2.2 禁止动态 SQL 拼接

```java
// ❌ 禁止
String sql = "SELECT * FROM user WHERE 1=1";
if (name != null) {
    sql += " AND name = '" + name + "'";
}

// ✅ 正确 - 使用 QueryWrapper
QueryWrapper<User> wrapper = new QueryWrapper<>();
if (name != null) {
    wrapper.eq("name", name);
}
```

---

## 3. XSS 防护

### 3.1 输入校验

```java
// ✅ 使用校验注解
@NotBlank(message = "用户名不能为空")
@Size(min = 3, max = 20, message = "用户名长度3-20位")
@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母数字下划线")
private String username;

// ✅ 特殊字符过滤
@Component
public class XssFilter {
    public String filter(String input) {
        if (input == null) return null;
        return input.replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;")
                    .replaceAll("\"", "&quot;")
                    .replaceAll("'", "&#x27;");
    }
}
```

---

## 4. 权限控制

### 4.1 权限码格式 (RBAC)

```
{model}:{module}:{action}
```

| 操作 | 代码 | 说明 |
|------|------|------|
| 列表 | list | 查询列表 |
| 详情 | view | 查看单条 |
| 新增 | add | 新增 |
| 修改 | edit | 修改 |
| 删除 | delete | 删除 |
| 提交 | submit | 提交申请 |
| 审批 | approve | 审批通过 |
| 驳回 | reject | 审批驳回 |
| 取消 | cancel | 取消操作 |
| 导出 | export | 导出数据 |
| 导入 | import | 导入数据 |

### 4.2 权限示例

```java
// ✅ 使用 SpEL 表达式
@PreAuthorize("hasAuthority('user:user:add')")
@PostMapping
public void addUser(@RequestBody User user) { }

@PreAuthorize("hasAuthority('user:user:edit')")
@PutMapping("/{id}")
public void updateUser(@PathVariable Long id, @RequestBody User user) { }

@PreAuthorize("hasAuthority('user:user:delete')")
@DeleteMapping("/{id}")
public void deleteUser(@PathVariable Long id) { }
```

---

## 5. 敏感数据

### 5.1 敏感字段脱敏

```java
// ✅ 使用 @JsonSerialize 自定义序列化
public class User {
    private String name;
    private String idCard;  // 身份证

    @JsonSerialize(using = DesensitizeSerializer.class)
    public String getIdCard() {
        return idCard;
    }
}

// 脱敏结果: 110101199001011234 → 110101********1234
```

### 5.2 日志脱敏

```java
// ✅ 禁止日志输出敏感信息
log.info("用户登录: username={}, password={}", username, password);  // ❌

log.info("用户登录: username={}", username);  // ✅
```

---

## 6. 加密规范

### 6.1 密码加密

```java
// ✅ 使用 BCrypt
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// 存储
user.setPassword(passwordEncoder.encode(rawPassword));

// 校验
if (!passwordEncoder.matches(rawPassword, storedPassword)) {
    throw new BadCredentialsException("密码错误");
}
```

### 6.2 传输加密

```
✅ HTTPS (TLS 1.2+)
❌ HTTP
❌ SSL 3.0
❌ TLS 1.0/1.1
```

---

## 7. 安全检查清单

| 检查项 | 说明 |
|--------|------|
| 硬编码检查 | 代码中不能有密钥、密码、IP |
| SQL 注入检查 | 必须使用参数化查询 |
| XSS 检查 | 输入输出需要校验/转义 |
| 权限检查 | 接口必须有权限控制 |
| 敏感数据 | 密码/身份证等需要脱敏 |
| 加密 | 敏感数据传输必须 HTTPS |

---

## 8. 内部API双重保护

### 8.1 流量架构

| 流量类型 | 路径 | 保护 |
|----------|------|------|
| 外部请求 | 必须过Gateway | Gateway检查path_type |
| 微服务间调用 | 不过Gateway | Feign传X-Internal-Call+签名 |
| 直接访问微服务 | 禁止 | 网络层+InternalCallFilter+签名验签 |

### 8.2 path_type 定义

| type | 说明 | 行为 |
|------|------|------|
| 1 | 公开 | 放行 |
| 2 | 需登录 | 验证Token |
| 3 | 权限专用 | 验证权限码 |
| 4 | 内部专用 | 403拒绝 |

### 8.3 内部调用签名协议

| Header | 说明 |
|--------|------|
| X-Internal-Call | 固定值 "true" |
| X-Internal-Timestamp | 时间戳（毫秒） |
| X-Internal-Nonce | UUID 防重放 |
| X-Internal-Secret-Version | 密钥版本号 |
| X-Internal-Signature | HMAC-SHA256(timestamp:nonce:method:path:query, secret) |

**验签规则：**
1. timestamp 偏差 < 5分钟
2. nonce 不在 IKvStore 黑名单中（防重放）
3. HMAC 签名匹配（支持多版本密钥）

### 8.4 Nonce黑名单规范

| 项目 | 规范 |
|------|------|
| 存储 | IKvStore（infra-kv） |
| Key格式 | `nonce:{nonce}` |
| TTL | = timestamp tolerance (300秒) |
| 用途 | 防重放攻击 |

### 8.5 密钥轮换规范

- 支持多版本密钥同时生效
- X-Internal-Secret-Version header 标识当前版本
- 轮换完成后旧版本密钥可下线

### 8.6 @Inner注解

用于标识内部API（**仅代码文档作用**，无运行时拦截）。
运行时拦截由 InternalCallFilter + Gateway path_type 实现。
