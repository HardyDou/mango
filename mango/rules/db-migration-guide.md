# Flyway 数据库迁移指南

## 概述

本文档描述 Mango 脚手架中 Flyway 数据库迁移的使用规范和操作指南。

## 核心概念

### Migration 文件布局

每个领域模块的 migration 文件放在 `core/src/main/resources/db/migration/{module}/` 下：

```
mango-xxx-core/
└── src/main/resources/
    └── db/migration/
        └── user/                 # user 模块
            ├── V1__init.sql     # DDL 建表
            └── V2__seed.sql     # 种子数据
        └── area/                 # area 模块
            └── V1__init.sql     # DDL 建表
```

### 命名规则

- 文件格式：`V{version}__{description}.sql`，例如 `V1__init.sql`、`V2__seed.sql`
- `{version}` 为 Flyway 版本号，每次 DDL 变更新增一个版本（如 V1 → V2 → V3）
- **不在历史文件上修改**，只新增 migration 文件
- `flyway_history` 表不手工删除

### 关键约束

1. 每个模块的 V1 建表，不能依赖另一模块的表（跨域通过 API 调用，Mapper 禁止跨域 SQL）
2. 每次 DDL 变更新增一个 migration 文件，不修改历史文件
3. `flyway_history` 表不手工删除或修改

## 配置说明

### Maven 依赖

引入 `mango-infra-db-starter` 时，Flyway 自动启用：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-db-starter</artifactId>
</dependency>
```

> 注意：Spring Boot 的默认 Flyway 自动配置被本模块禁用（`spring.flyway.enabled=false`）。

### 配置属性

| 配置 | 说明 | 默认值 |
|------|------|--------|
| `mango.flyway.enabled` | 全局开关 | `true` |
| `mango.flyway.modules.<module>.enabled` | 模块级开关 | `true` |
| `mango.flyway.modules.<module>.baseline-on-migrate` | 模块级 baseline | `false` |

### 配置示例

**全部启用（新环境）：**
```yaml
mango:
  flyway:
    enabled: true
```

**禁用某模块（生产已有数据，仅其他模块更新）：**
```yaml
mango:
  flyway:
    enabled: true
    modules:
      i18n:
        enabled: false
```

**仅初始化某模块（新数据库只想先跑 user）：**
```yaml
mango:
  flyway:
    enabled: true
    modules:
      user:
        enabled: true
      area:
        enabled: false
      org:
        enabled: false
```

**已有数据库引入 Flyway（需要 baseline）：**
```yaml
mango:
  flyway:
    modules:
      user:
        baseline-on-migrate: true
```

## 迁移步骤（各域开发者参考）

### 1. 创建 migration 文件

在本模块的 `core/src/main/resources/db/migration/<domain>/` 创建目录和文件：

```
db/migration/user/
├── V1__init.sql    # 从 schema.sql 提取 user 相关 DDL
└── V2__seed.sql    # 从 data.sql 提取 user 相关 DML
```

### 2. 编写 SQL

`V1__init.sql` 示例：
```sql
CREATE TABLE `sys_user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

`V2__seed.sql` 示例：
```sql
INSERT INTO `sys_user` (`username`, `nickname`, `tenant_id`) VALUES
('admin', '管理员', 1),
('test', '测试用户', 1);
```

### 3. 提交 PR

提交后，Flyway 在应用启动时自动扫描并按序执行。

## 常见问题

### Q: 如何回滚？

Flyway 支持按版本回滚：

```bash
# 回滚到指定版本
flyway.undo() # 需要 flyway-undo 插件
```

日常通过新增 migration 而非修改历史文件来管理变更。

### Q: 多模块 migration 执行顺序？

多个模块的 migration 按 Flyway 扫描顺序执行（通常按 classpath JAR 包顺序）。同一版本号在不同模块间互不影响。

### Q: baseline-on-migrate 什么时候用？

当数据库**已有表**但想引入 Flyway 管理新变更时，设置 `baseline-on-migrate=true`。Flyway 会为该模块创建一个 baseline 记录，跳过历史表的 migration。

### Q: 如何禁用 Flyway 彻底？

```yaml
mango:
  flyway:
    enabled: false
```

## 相关文档

- `module-rules.md` §4.2 — core 模块包结构规范
- `persistence-rules.md` — 事务规范
