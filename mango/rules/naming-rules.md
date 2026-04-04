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

### 2.1 表命名 (小写下划线 + 模块前缀)

**核心原则：表名必须以模块前缀开头，实现物理隔离和命名清晰。**

```
{module_prefix}_{entity}

module_prefix: 模块英文缩写（小写，2-4个字符）
entity: 实体名称（小写下划线）
```

#### 业务域前缀（归属特定领域）

| 模块 | 前缀 | 示例表 |
|------|------|--------|
| user（业务用户） | `usr_` | `usr_user`（业务用户）、`usr_profile`（用户档案）、`usr_contact`（联系人） |
| area（区域） | `area_` | `area_tree`（区域树）、`area_district`（区县） |
| org（组织） | `org_` | `org_dept`（部门）、`org_post`（岗位） |
| permission（权限） | `perm_` | `perm_menu`（菜单）、`perm_role`（角色）、`perm_resource`（资源） |
| i18n（国际化） | `i18n_` | `i18n_lang`（语言包）、`i18n_message`（消息模板） |
| order（订单） | `ord_` | `ord_order`（订单）、`ord_item`（订单明细） |
| ai | `ai_` | `ai_provider`（AI供应商）、`ai_model`（AI模型） |

#### sys_ 通用前缀（跨域共享基础设施）

**原则：所有业务域都可能依赖，但没有单一域专属的表，归入 `sys_`。**

| 子类 | 示例表 |
|------|--------|
| 认证/授权 | `sys_user`（平台管理员）、`sys_role`（角色）、`sys_oauth_client`（OAuth客户端）、`sys_social_details`（第三方登录） |
| 组织关联 | `sys_user_role`、`sys_user_dept`、`sys_user_post`（用户-角色/部门/岗位关联表） |
| 数据字典 | `sys_dict`（字典）、`sys_dict_item`（字典项） |
| 系统配置 | `sys_system_config`（系统配置）、`sys_public_param`（公共参数） |
| 租户 | `sys_tenant`（租户）、`sys_tenant_user`（租户用户关联） |
| 日志/审计 | `sys_log`（操作日志）、`sys_audit_log`（审计日志） |
| 文件 | `sys_file`（文件元数据）、`sys_file_group`（文件分组） |
| 消息 | `sys_message`（消息）、`sys_message_relation`（消息关联） |
| 任务调度 | `sys_job`（定时任务）、`sys_job_log`（任务日志） |
| 路由 | `sys_route_conf`（路由配置） |

```sql
-- ✅ 正确：带模块前缀
CREATE TABLE usr_user ();           -- 业务用户
CREATE TABLE sys_user ();            -- 平台管理员
CREATE TABLE sys_dict ();            -- 字典
CREATE TABLE sys_log ();            -- 日志

-- ❌ 错误：无前缀
CREATE TABLE user ();               -- 缺少前缀
CREATE TABLE role ();                -- 缺少前缀

-- ❌ 错误：跨域 JOIN（Mapper 禁止跨域 SQL，跨域数据通过 API 获取）
CREATE TABLE usr_user_role ();      -- 应为 sys_user_role（跨域关联表归属 sys）
```

> **注意**：`sys_user`（平台管理员）≠ `usr_user`（业务用户）。前者是平台运营/超管，属于 sys_ 基础设施；后者是业务用户（如投标方联系人），属于 usr_ 业务域。两者为平行关系，不是包含关系。

### 2.1.1 主键 ID 规范（强制）

**所有表的主键必须使用雪花算法（Snowflake），禁止使用自增（ AUTO_INCREMENT）。**

```sql
-- ✅ 正确：雪花算法
id BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
-- 或在实体类使用：
// @TableId(type = IdType.ASSIGN_ID)
private Long id;

-- ❌ 错误：自增
id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
```

| 规则 | 说明 |
|------|------|
| 类型 | `BIGINT(20)`，符号位，值范围约 -2^63 ~ 2^63-1 |
| 生成方式 | 雪花算法（Snowflake），由应用层填充 |
| 禁止 | 禁止 `AUTO_INCREMENT`、禁止 `UUID` |
| 理由 | 雪花算法天然有序（适合索引）、不依赖数据库连接、分布式唯一 |

> **注**：雪花算法需引入 `mybatis-plus` 的 `IdType.ASSIGN_ID` 或自行封装 `SnowflakeIdWorker`。每个服务实例需配置 workerId/datacenterId。

### 2.2 字段命名 (小写下划线)

```sql
-- ✅ 正确
user_name VARCHAR(50);
user_status VARCHAR(20);
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

-- 普通索引: idx_{表前缀}_{字段}
CREATE INDEX idx_usr_user_name ON usr_user(user_name);

-- 唯一索引: uk_{表前缀}_{字段}
CREATE UNIQUE INDEX uk_sys_user_email ON sys_user(user_email);

-- 联合索引: idx_{表前缀}_{字段1}_{字段2}
CREATE INDEX idx_usr_user_age_name ON usr_user(age, user_name);
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
GET    /user             # 用户列表
GET    /user/{id}        # 用户详情
POST   /user             # 创建用户
PUT    /user/{id}        # 更新用户
DELETE /user/{id}        # 删除用户
```

> 注意：`/api` 前缀由网关/Vite 代理在接入层添加，后端服务不需要也不应该添加 `/api` 前缀（参见 api-rules.md 第 1.1 节）。

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

## 5. API 层对象命名

### 5.1 命名规则

| 类型 | 后缀 | 所在包 | 职责 | 示例 |
|------|------|--------|------|------|
| API 输入参数 | `Po` | `api/po/` | 接收外部请求参数，定义在 api 层 | `SysRolePo`、`DictTypePo` |
| API 返回参数 | `VO` | `api/vo/` | 返回给外部调用方，定义在 api 层 | `SysRoleVO`、`DictTypeVO` |
| 内部传输对象 | `DTO` | `core/dto/` | core 内部模块间数据传输，不跨层暴露 | `RoleMenuDTO`、`DictQueryDTO` |

### 5.2 职责边界

```
外部调用方
    │
    ▼
┌─────────────────┐
│   api/po/       │  ← XxxPo：API 入参，跨模块传递
└────────┬────────┘
         │ 被 starter/controller 调用，转换为 DTO
         ▼
┌─────────────────┐
│   core/dto/      │  ← XxxDTO：内部传输，跨 service 层传递
└────────┬────────┘
         │ service 层使用，转换为 entity
         ▼
┌─────────────────┐
│   core/entity/   │  ← 实体，与数据库一一对应
└─────────────────┘
```

### 5.3 代码示例

```java
// ✅ 正确：api 层——入参和返回
// SysRolePo.java（api 层，输入参数）
public class SysRolePo {
    private String roleCode;
    private String roleName;
    private Integer roleType;
}

// SysRoleVO.java（api 层，返回参数）
public class SysRoleVO {
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Integer roleType;
    private String tenantName;
}

// ✅ 正确：core 层——内部传输
// SysRoleDTO.java（core 层，内部使用）
public class SysRoleDTO {
    private Long roleId;
    private Long tenantId;
    private List<Long> menuIds;
}

// ❌ 错误：混淆职责
// DTO 放在 api 层（应为 Po/VO）
// Po 放在 core 层（应仅限 api 层）
```

### 5.4 存储对象（PO vs Entity）

| 类型 | 后缀 | 位置 | 说明 |
|------|------|------|------|
| 存储对象 | `PO` | `api/po/` | API 层入参，含校验注解，与前端交互 |
| 实体 | `Entity` | `core/entity/` | 数据库实体，含 `@TableName`，仅限 core 层 |

> **注**：`api/po/` 中的 PO（Persistence Object）与传统 DO/DTO 不同，此处专指 API 入参对象，用于外部请求数据接收。

---

## 6. 测试命名

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
