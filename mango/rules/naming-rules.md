# 命名规范 (naming-rules)

## 1. Java 命名

### 1.1 类命名 (PascalCase)

```java
// ✅ 正确
public class UserService { }
public class OrderController { }
public class ProductMapper { }

// ❌ 错误
public class userService { }
public class order_service { }
```

### 1.2 方法命名 (camelCase)

```java
// ✅ 正确
public User findById(Long id) { }
public List<User> findAllUsers() { }
public void updateOrderStatus(Long orderId, String status) { }

// ❌ 错误
public User FindById(Long id) { }
public List<User> find_by_id(Long id) { }
```

### 1.3 常量命名 (UPPER_SNAKE_CASE)

```java
// ✅ 正确
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_LANGUAGE = "zh-CN";

// ❌ 错误
public static final int maxRetryCount = 3;
public static final String defaultLanguage = "zh-CN";
```

### 1.4 变量命名 (camelCase)

```java
// ✅ 正确
private User currentUser;
private List<Order> orderList;
private Map<String, Object> configMap;

// ❌ 错误
private User CurrentUser;
private List<Order> OrderList;
```

---

## 2. 数据库命名

### 2.1 表命名 (小写下划线)

```sql
-- ✅ 正确
CREATE TABLE sys_user ();
CREATE TABLE order_info ();
CREATE TABLE product_category ();

-- ❌ 错误
CREATE TABLE SysUser ();
CREATE TABLE OrderInfo ();
CREATE TABLE ProductCategory ();
```

### 2.2 字段命名 (小写下划线)

```sql
-- ✅ 正确
user_name VARCHAR(50);
order_status VARCHAR(20);
created_at TIMESTAMP;

-- ❌ 错误
userName VARCHAR(50);
orderStatus VARCHAR(20);
createdAt TIMESTAMP;
```

### 2.3 索引命名

```sql
-- 主键索引
PRIMARY KEY (id)

-- 普通索引: idx_表名_字段
CREATE INDEX idx_sys_user_name ON sys_user(user_name);

-- 唯一索引: uk_表名_字段
CREATE UNIQUE INDEX uk_sys_user_email ON sys_user(user_email);

-- 联合索引: idx_表名_字段1_字段2
CREATE INDEX idx_sys_user_age_name ON sys_user(age, name);
```

---

## 3. 包命名

### 3.1 Java 包 (小写点分隔)

```java
// ✅ 正确
com.mango.user.service
com.mango.order.controller
com.mango.product.mapper

// ❌ 错误
com.Mango.User.Service
com.mango_order_service
```

---

## 4. REST API 命名

### 4.1 URL 路径 (小写横杠分隔)

```
GET    /api/user         # 用户列表
GET    /api/user/{id}    # 用户详情
POST   /api/user         # 创建用户
PUT    /api/user/{id}    # 更新用户
DELETE /api/user/{id}    # 删除用户
```

### 4.2 变量命名 (驼峰)

```json
// 请求体
{
  "userName": "张三",
  "orderStatus": "PENDING"
}

// ❌ 不要用蛇形
{
  "user_name": "张三",
  "order_status": "PENDING"
}
```

---

## 5. 测试命名

### 5.1 测试类命名

```java
// ✅ 正确
class UserServiceTest { }
class OrderControllerTest { }

// ❌ 错误
class TestUserService { }
class UserServiceTests { }
```

### 5.2 测试方法命名

```java
// ✅ 正确 - 方法_场景_预期结果
@Test
void findById_existingId_returnsUser() { }

@Test
void save_nullUser_throwsException() { }

// ❌ 错误
@Test
void testFindById() { }
@Test
void test() { }
```
