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
