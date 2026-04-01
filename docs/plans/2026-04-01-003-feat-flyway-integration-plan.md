---
title: "feat: Flyway 数据库迁移集成"
type: feat
status: completed
date: 2026-04-01
deepened: 2026-04-03
---

# Flyway 数据库迁移集成

## Overview

在 `mango-infra-db` 中集成 Flyway，实现版本化数据库迁移管理，为后续各领域模块的 migration 文件迁移提供基础设施。BFF 现有的 H2 `schema.sql`/`data.sql` 暂保留（见 Key Decision #4）。

## Problem Frame

当前 `mango-bff-admin` 的 `schema.sql` / `data.sql` 位于 BFF resources 根目录，存在以下问题：
- 无版本化，无法追踪变更历史
- 无法精确回滚到某个版本
- 多域表混在一起（sys_user/area/org/i18n 等），无法按域独立管理
- Flyway 是 MyBatis-Plus + Spring Boot 3.x 项目的标准数据库迁移方案

## Requirements Trace

- R1. `mango-infra-db` 引入 Flyway，实现 Spring Boot 3.x 自动配置
- R2. 支持 per-module 开关：每个领域模块的 migration 可独立启用/禁用
- R3. 框架自动扫描所有 classpath 上的 `db/migration/` 文件并按序执行
- R4. `module-rules.md` 补充 core 模块 `db/migration/` 目录规范
- R5. 产出 `db-migration-guide.md` 开发者指南

> **注：** BFF 现有 H2 `schema.sql`/`data.sql` 不在本次迁移范围（Key Decision #4），由后续 Sprint 按领域模块执行。

## Scope Boundaries

**做：**
- `mango-infra-db` 的 Flyway AutoConfiguration + per-module 开关
- `module-rules.md` 补充 migration 目录规范
- `db-migration-guide.md` 开发者指南

**不做：**
- BFF 现有 H2 `schema.sql`/`data.sql` 迁移（暂保留，维持开发体验，由后续 Sprint 按领域执行）
- 各领域模块的实际 migration 文件迁移（后续 Sprint 执行）
- 生产环境数据库迁移（由运维按需执行）

---

## Key Technical Decisions

- **Decision: Flyway 放在 `mango-infra-db`**。Flyway 是数据库基础设施能力，属于 `infra-db` 职责，无需单独拆 `infra-flyway` 模块。
- **Decision: migration 文件放在领域模块的 `core`**。通过 starter 传递依赖，migrations 自动进入 BFF classpath，Flyway 无需特殊配置即可扫描所有 jar 包内的 `classpath:db/migration/`。
- **Decision: 迁移文件按 module 子目录隔离**。如 `db/migration/user/V1__init.sql`、`db/migration/area/V1__init.sql`，每个 module 独立子目录。这是 Flyway 官方推荐的 multi-module 布局，比 prefix 命名更简洁，且天然支持 per-module 独立扫描。
- **Decision: BFF 的 H2 schema/data 暂保留**。BFF 当前使用 H2 内存数据库进行开发测试，迁移到 Flyway 会破坏现有开发体验。**暂不迁移**，由后续各领域模块按需迁移。

---

## Implementation Units

- [ ] **Unit 1: `mango-infra-db` 添加 Flyway 依赖和 AutoConfiguration（含 per-module 开关）**

**Goal:** 在 `mango-infra-db` 中引入 Flyway 并完成 Spring Boot 3.x 自动配置，支持 per-module 灵活开关

**Requirements:** R1, R2

**Files:**
- Modify: `mango/mango-infra/mango-infra-db/pom.xml`
- Create: `mango/mango-infra/mango-infra-db/src/main/java/io/mango/infra/db/starter/FlywayProperties.java`
- Create: `mango/mango-infra/mango-infra-db/src/main/java/io/mango/infra/db/starter/FlywayAutoConfiguration.java`

**Approach:**
- 在 `pom.xml` 添加 `flyway-core` + `flyway-mysql`（v10.x，适配 Spring Boot 3.x）
- `FlywayProperties` 定义三级配置：
  - `mango.flyway.enabled` — 全局开关，默认 true
  - `mango.flyway.modules.<module>.enabled` — 模块级开关，默认 true
  - `mango.flyway.modules.<module>.baseline-on-migrate` — 模块级 baseline，默认 false
- `FlywayAutoConfiguration` 使用 `@AutoConfiguration` + `@EnableConfigurationProperties(FlywayProperties.class)`
- **Per-module 禁用实现方案**：通过 Spring Boot `@ConditionalOnProperty` 为每个 module 创建独立的 `Flyway` Bean，每个 Bean 只扫描自己的 migration 目录（`classpath:db/migration/<module>/`），与现有 prefix 命名 `V1__user__init.sql` 解耦
  - 例如 `userFlyway` Bean 扫描 `classpath:db/migration/user/`
  - 设置 `mango.flyway.modules.user.enabled=false` 时，该 Bean 不创建
- **迁移文件布局调整**（影响 Unit 2 的目录规范）：
  - 原：`db/migration/V1__user__init.sql`（单目录 + prefix 命名）
  - 改：`db/migration/user/V1__init.sql`（module 子目录，无 prefix）
  - 理由：子目录方案更简洁，且是 Flyway 官方推荐的 multi-module 布局

**Patterns to follow:**
- `mango-infra-redis` 的 `RedisAutoConfiguration` — `@AutoConfiguration` + `@EnableConfigurationProperties` 模式（实际代码）
- Spring Boot `ConfigurationProperties` + `@NestedConfigurationProperty`

**Test scenarios:**
- 全局启用时，所有 classpath 上的 migration 均被 Flyway 扫描
- 设置 `modules.i18n.enabled=false` 时，i18n 模块的 SQL 不执行
- 多 module 在 classpath 共存时，各自的 migration 均被正确执行

**Verification:**
- `mvn test` 在带 migration 文件的模块中能正常初始化 H2 数据库
- 禁用特定模块后，该模块 migration 不被执行

---

- [ ] **Unit 2: 更新 `module-rules.md` core 模块包结构**

**Goal:** 在 `module-rules.md` §4.2 core 包结构中，补充 `db/migration/` 目录规范

**Requirements:** R4

**Files:**
- Modify: `mango/rules/module-rules.md` §4.2 core 包结构

**Approach:**
core 模块包结构新增：
```
mango-xxx-core/
├── src/main/java/io/mango/xxx/core/   # 现有内容
└── src/main/resources/
    └── db/migration/                  # SQL 版本化迁移文件（按 module 子目录隔离）
        └── user/                      # user 模块独立目录
            ├── V1__init.sql           # DDL 建表
            └── V2__seed.sql           # 种子数据
```

> **注**：与原 prefix 命名方案不同，改用 module 子目录隔离（`db/migration/user/` vs `V1__user__init.sql`）。子目录方案更简洁，是 Flyway 官方推荐的 multi-module 布局，且天然支持 per-module 独立扫描。

同时在 §4.5 BFF 结构中补充说明：
- BFF 可通过 starter 传递依赖间接引入各域的 migration 文件
- BFF 自身不携带 `db/migration/`（除非 BFF 有独立的 H2 开发数据库）

**Verification:**
- `module-rules.md` §4.2 中 core 模块包含 `db/migration/` 目录规范

---

- [ ] **Unit 3: 产出 `db-migration-guide.md` 开发者指南**

**Goal:** 编写 Flyway 使用指南，包含命名规则、per-module 配置、运维场景

**Requirements:** R5

**Files:**
- Create: `mango/rules/db-migration-guide.md`

**Approach:**

**核心命名规则：**
- 每个 module 在 `db/migration/` 下有独立子目录：`db/migration/user/`、`db/migration/area/` 等
- 文件格式：`V{version}__{description}.sql`，在各自 module 目录下（如 `db/migration/user/V1__init.sql`）
- 示例：`db/migration/user/V1__init.sql`、`db/migration/user/V2__seed.sql`、`db/migration/area/V1__init.sql`
- 每增加一个 DDL 变更，新增一个版本号（如 V2, V3...），不在历史文件上修改

**per-module 运维配置：**
```yaml
# 全部启用（新环境）
mango:
  flyway:
    enabled: true

# 生产已有数据，仅更新某模块
mango:
  flyway:
    enabled: true
    modules:
      i18n:
        enabled: false

# 仅初始化 user 模块
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

**已有 DB 引入 Flyway（baseline）：**
```yaml
mango:
  flyway:
    modules:
      user:
        baseline-on-migrate: true
```

**迁移步骤（各域开发者参考）：**
1. 在本域 `core/src/main/resources/db/migration/<domain>/` 创建目录
2. 在本域目录下创建 `V1__init.sql`，从现有 `schema.sql` 提取本域 DDL，删除其他域内容
3. 在本域目录下创建 `V2__seed.sql`，从现有 `data.sql` 提取本域 DML
4. 提交 PR，由 Flyway 按 module 子目录独立扫描执行

**关键约束：**
- 每个域的 V1 建表，不能依赖另一域的表（跨域通过 API，Mapper 禁止跨域 SQL）
- `flyway_history` 表不手工删除
- 每次 DDL 变更新增一个 migration 文件，不修改历史 V1

**Verification:**
- 产出 `db-migration-guide.md` 包含完整命名规则、配置示例、迁移步骤

---

## System-Wide Impact

- **Flyway 执行时机**：每个 module 的 Flyway Bean 由 Spring 统一管理在 `ApplicationRunner` 之前完成 migration，多个 module 的 Flyway Bean 按 Spring 注入顺序执行
- **多 module 共存**：每个 module 独立目录（如 `db/migration/user/`），天然隔离无冲突
- **BFF 开发体验**：BFF 当前 H2 `schema.sql`/`data.sql` 保持不变，不破坏现有开发流程
- **Infra 层职责**：`mango-infra-db` 提供 Flyway 基础设施，各领域模块自行管理自己的 migration 文件

## Risks & Dependencies

| 风险 | 缓解 |
|------|------|
| 多 module migration 文件名冲突 | 强制 `V{version}__{module}__` 前缀隔离 |
| 已有 DB 的环境引入 Flyway | 使用 `baseline-on-migrate=true`，文档说明步骤 |
| Per-module 禁用机制实现 | 通过 `@ConditionalOnProperty` 为每个 module 创建独立 Flyway Bean，每个扫描独立目录，无需自定义 MigrationResolver |
| BFF 的 H2 schema 迁移未完成 | 暂不迁移，保持现状；由后续 Sprint 按域执行 |

## Documentation / Operational Notes

- `mango/rules/db-migration-guide.md` — 新建，供开发者参考的 Flyway 使用指南
- `mango/rules/module-rules.md` — §4.2 core 包结构补充 migration 目录规范
- Flyway 配置前缀：`mango.flyway.*`
  - `mango.flyway.enabled` — 全局开关（默认 true）
  - `mango.flyway.modules.<module>.enabled` — 模块开关（默认 true）
  - `mango.flyway.modules.<module>.baseline-on-migrate` — 模块级 baseline（默认 false）

## Sources & References

- `mango/mango-infra/mango-infra-db/pom.xml` — 当前 db 模块依赖（无 Flyway）
- `mango/mango-infra/mango-infra-redis/src/main/java/io/mango/infra/redis/starter/RedisAutoConfiguration.java` — AutoConfiguration 参考模式（`@AutoConfiguration + @EnableConfigurationProperties`）
- `mango/rules/module-rules.md` — 当前模块规范
