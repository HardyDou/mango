# 数据库规范 (db-rules)

## 1. 表设计原则

### 1.1 基本规范

| 规范 | 说明 |
|------|------|
| 单表行数 | ≤ 2000 万行 |
| 索引数 | ≤ 5 个/表 |
| 字段数 | ≤ 30 个/表 |
| 外键 | 慎用，优先应用层关联 |

### 1.2 表命名

```sql
-- 格式：{业务模块}_{实体}
-- ✅ 正确
CREATE TABLE sys_user ();
CREATE TABLE order_info ();
CREATE TABLE product_category ();

-- ❌ 错误
CREATE TABLE User ();
CREATE TABLE OrderInfo ();
CREATE TABLE T_USER ();
```

---

## 2. 字段设计

### 2.1 字段命名

```sql
-- ✅ 正确 - 小写下划线
user_name VARCHAR(50);
order_status VARCHAR(20);
created_at TIMESTAMP;
updated_at TIMESTAMP;

-- ❌ 错误 - 驼峰
userName VARCHAR(50);
orderStatus VARCHAR(20);
```

### 2.2 通用字段（必须）

```sql
-- 每个表必须包含以下统一字段
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_by VARCHAR(64) DEFAULT NULL COMMENT '修改人',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-已删除',
version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
```

### 2.3 租户字段（按需）

```sql
-- 开启多租户时必须包含
tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
```

### 2.4 Auto Fill 配置

MyBatis-Plus 自动填充字段，无需手动处理：

| 字段 | 填充时机 | 填充值 |
|------|---------|--------|
| `create_by` | INSERT | 当前用户ID/用户名 |
| `create_time` | INSERT | 当前时间 |
| `update_by` | INSERT/UPDATE | 当前用户ID/用户名 |
| `update_time` | INSERT/UPDATE | 当前时间 |
| `del_flag` | INSERT | 0（正常） |

配置示例：
```java
@Component
public class MangoMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUser());
        this.strictInsertFill(metaObject, "updateBy", String.class, getCurrentUser());
        this.strictInsertFill(metaObject, "delFlag", Integer.class, 0);
        this.strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUser());
    }
}
```

### 2.5 字段类型选择

### 2.3 字段类型选择

| 数据类型 | 使用场景 |
|---------|---------|
| BIGINT | 主键、金额、数量 |
| VARCHAR(N) | 字符串，N 根据实际长度 |
| TEXT | 大文本 (>5000字符) |
| INT | 状态码、计数 |
| TINYINT | 布尔、状态标记 |
| DATETIME | 时间日期 |
| DECIMAL(10,2) | 精确金额 |

---

## 3. 索引规范

### 3.1 索引命名

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

### 3.2 索引创建原则

```sql
-- ✅ 频繁查询的字段加索引
WHERE user_name = ? → idx_user_name

-- ✅ 频繁排序的字段加索引
ORDER BY created_at → idx_created_at

-- ✅ 联合索引，区分度高的放前面
WHERE age = ? AND name = ? → idx_age_name

-- ❌ 不要在区分度低的字段加索引
WHERE deleted = 0  -- deleted 只有 0/1，不适合单独索引
```

---

## 4. SQL 规范

### 4.1 必须使用参数化查询

```sql
-- ❌ 禁止 SQL 拼接
SELECT * FROM user WHERE name = '" + name + "'

-- ✅ 正确
SELECT * FROM user WHERE name = #{name}
```

### 4.2 批量操作

```sql
-- ✅ 批量插入
INSERT INTO user (name, age) VALUES
('张三', 20),
('李四', 25),
('王五', 30);

-- ✅ 批量更新
UPDATE user SET status = 'ACTIVE'
WHERE id IN (1, 2, 3);
```

---

## 5. 事务规范

### 5.1 事务传播行为

| 传播行为 | 说明 |
|---------|------|
| REQUIRED | 如果当前有事务，加入该事务 |
| REQUIRES_NEW | 挂起当前事务，创建新事务 |
| NESTED | 嵌套事务（不支持则创建新事务） |

### 5.2 分布式事务

| 部署方式 | 配置 | 注解 |
|---------|------|------|
| 单体/聚合部署 | `mango.transaction.mode = local` | @Transactional |
| 微服务部署 | `mango.transaction.mode = seata` | @MangoTransactional |

详见 `rules/persistence-rules.md`

---

## 6. 数据库设计评审

### 6.1 评审清单

| 检查项 | 说明 |
|--------|------|
| 表名 | 是否符合命名规范 |
| 字段 | 是否有统一字段（id, create_by, create_time, update_by, update_time, del_flag, version, remark） |
| 主键 | 是否使用 BIGINT AUTO_INCREMENT |
| 索引 | 是否有合适索引（注意 del_flag 不适合单独索引） |
| 字段类型 | 是否合适 |
| 注释 | 是否有注释 |
| 敏感数据 | 是否脱敏 |
| 租户 | 多租户表是否包含 tenant_id |

### 6.2 DDL 模板

```sql
-- ============================================
-- 表名: sys_user
-- 说明: 用户表
-- 创建时间: 2026-03-26
-- ============================================
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    mobile VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    -- 统一字段（必须）
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    -- 租户字段（按需）
    -- tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    INDEX idx_username (username),
    INDEX idx_mobile (mobile),
    INDEX idx_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```
