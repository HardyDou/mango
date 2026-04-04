---
paths:
  - "**/*.java"
---

# 代码规范 (code-rules)

## C1: 重复检测

### C1.1 重复代码检测规则

| 指标 | 阈值 | 检测工具 |
|------|------|---------|
| 重复代码率 | ≤ 3% | PMD CPD |
| 重复块最小行数 | ≥ 10 行 | PMD CPD |

### C1.2 触发条件

每次代码提交前必须检查，违规禁止提交。

---

## C2: 代码长度限制

### C2.1 方法长度

| 类型 | 最大行数 | 规则名称 |
|------|---------|---------|
| 普通方法 | ≤ 50 行 | C2.1 |
| 复杂方法 | ≤ 100 行 | C2.2 |

### C2.2 类长度

| 类型 | 最大行数 | 规则名称 |
|------|---------|---------|
| 普通类 | ≤ 500 行 | C2.3 |
| Controller | ≤ 200 行 | C2.4 |
| Service | ≤ 300 行 | C2.5 |

---

## C3: 异常处理

### C3.1 异常捕获规则

| 规则 | 说明 |
|------|------|
| C3.1 | 禁止捕获 Exception/Throwable |
| C3.2 | 必须处理已捕获的异常 |
| C3.3 | 禁止生吞异常 (empty catch) |

### C3.2 正确示例

```java
// ✅ C3.2 正确 - 处理异常
try {
    doSomething();
} catch (SpecificException e) {
    log.error("操作失败", e);
    throw new BusinessException("操作失败", e);
}

// ❌ C3.3 错误 - 生吞异常
try {
    doSomething();
} catch (Exception e) {
    // 不要这样写！
}
```

### C3.3 异常分类

| 类型 | 使用场景 |
|------|---------|
| BusinessException | 业务逻辑错误 |
| ValidationException | 参数校验错误 |
| SystemException | 系统级错误 |

---

## C4: 安全规范

### C4.1 硬编码禁止

```java
// ❌ 禁止 - 硬编码密钥
private static final String API_KEY = "abc123";

// ✅ 正确 - 从配置读取
@Value("${api.key}")
private String apiKey;
```

### C4.2 SQL 注入防护

```java
// ❌ 禁止 - SQL 拼接
String sql = "SELECT * FROM user WHERE name = '" + name + "'";

// ✅ 正确 - 使用参数化查询
@Select("SELECT * FROM user WHERE name = #{name}")
User findByName(@Param("name") String name);
```

### C4.3 XSS 防护

```java
// 输入校验
@NotBlank
@Size(min = 1, max = 100)
private String username;
```

---

## C5: 代码质量检查流程

### C5.1 检查工具

| 用途 | 工具 |
|------|------|
| 代码规范 | Alibaba P3C |
| 重复代码 | PMD CPD |
| Bug 检测 | SpotBugs（可选） |

### C5.2 Mango CLI 命令

```bash
# 运行所有检查
mvn mango:check

# 单项检查
mvn mango:check -Drule=duplicate
mvn mango:check -Drule=method-length
mvn mango:check -Drule=class-length
mvn mango:check -Drule=naming
mvn mango:check -Drule=exception-handling
mvn mango:check -Drule=security
```

---

## 检查规则配置

```yaml
# mango-check.yaml
rules:
  duplicate:
    enabled: true
    threshold: 3  # 重复率 ≤ 3%
    min-lines: 10

  method-length:
    enabled: true
    max: 50
    max-complex: 100

  class-length:
    enabled: true
    max: 500
    controller-max: 200
    service-max: 300

  naming:
    enabled: true
    standard: java

  exception-handling:
    enabled: true
    allow-empty-catch: false
```
