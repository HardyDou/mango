---
paths:
  - "**/*Service.java"
  - "**/*Controller.java"
---

# 持久化规范 (persistence-rules)

## 1. 事务配置切换

### 1.1 配置模式

| 部署方式 | 配置 | 注解 |
|---------|------|------|
| 单体/聚合部署 | `mango.transaction.mode = local` | @Transactional |
| 微服务部署 | `mango.transaction.mode = seata` | @MangoTransactional |

### 1.2 配置示例

```yaml
# application.yml
mango:
  transaction:
    mode: seata  # local 或 seata
```

---

## 2. @MangoTransactional 注解

### 2.1 注解定义

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MangoTransactional {
    // 事务模式：默认读取配置
    String mode() default "";

    // 回滚异常
    Class<? extends Throwable>[] rollbackFor() default {Exception.class};
}
```

### 2.2 AOP 切面实现

```java
@Aspect
@Component
public class MangoTransactionAspect {

    @Around("@annotation(MangoTransactional)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String mode = getTransactionMode();

        if ("seata".equals(mode)) {
            return handleGlobalTransaction(point);
        } else {
            return handleLocalTransaction(point);
        }
    }

    private String getTransactionMode() {
        // 优先读取注解配置，否则读取全局配置
        return environment.getProperty("mango.transaction.mode", "local");
    }
}
```

---

## 3. Seata AT 模式原理

### 3.1 分支提交

| 特性 | 说明 |
|------|------|
| 分支 commit | 每个分支都 commit，但通过 undo_log 可回滚 |
| 全局协调 | TC 协调所有分支，要么一起提交，要么一起回滚 |
| 性能 | 聚合部署性能更好（减少跨服务调用） |

### 3.2 undo_log 表

```sql
CREATE TABLE IF NOT EXISTS `undo_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `branch_id` bigint NOT NULL,
    `xid` varchar(100) NOT NULL,
    `context` varchar(128) NOT NULL,
    `rollback_info` longblob NOT NULL,
    `log_status` int NOT NULL,
    `log_created_at` datetime NOT NULL,
    `log_modified_at` datetime NOT NULL,
    UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
```

---

## 4. 事务使用规范

### 4.1 读写分离

```java
// ✅ 读方法不需要事务
@MangoTransactional
public User findById(Long id) {  // 不需要事务
    return userMapper.findById(id);
}

// ✅ 写方法需要事务
@MangoTransactional
public void save(User user) {
    userMapper.insert(user);
    // ...
}
```

### 4.2 事务边界

```java
// ✅ 正确 - 事务边界清晰
@MangoTransactional
public void createOrder(CreateOrderCommand command) {
    // 校验
    validateOrder(command);
    // 创建订单
    Order order = orderMapper.insert(command);
    // 扣库存
    inventoryService.deduct(command.getProductId(), command.getQuantity());
    // 发送消息
    messageService.send("order.created", order);
}
```

### 4.3 嵌套事务

```java
// ✅ 外层方法
@MangoTransactional
public void methodA() {
    methodB();  // 会被纳入同一个事务
}

// ✅ 内层方法 - 不要重复加事务
public void methodB() {
    // 不加 @MangoTransactional
    // ...
}
```

---

## 5. 事务隔离级别

### 5.1 隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---------|------|----------|------|
| READ_UNCOMMITTED | ✓ | ✓ | ✓ |
| READ_COMMITTED | ✗ | ✓ | ✓ |
| REPEATABLE_READ | ✗ | ✗ | ✓ |
| SERIALIZABLE | ✗ | ✗ | ✗ |

### 5.2 配置

```yaml
mango:
  transaction:
    isolation: READ_COMMITTED  # 默认读已提交
```

---

## 6. 常见问题

### 6.1 事务不生效

| 原因 | 解决方案 |
|------|---------|
| 方法内部调用 | 注入自身或用 AopContext |
| 非 public 方法 | 改为 public |
| 异常被 catch | 重新抛出或配置 rollbackFor |
| 异常类型不匹配 | 配置正确的 rollbackFor |

### 6.2 分布式事务问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 空回滚 | Seata 未注册成功 | 检查 TC 连接 |
| 悬挂 | 分支注册晚于全局事务超时 | 设置合理超时时间 |
| 幂等 | 重试导致重复执行 | 使用幂等注解或防重表 |
