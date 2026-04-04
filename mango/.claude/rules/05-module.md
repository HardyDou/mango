---
paths:
  - "**/pom.xml"
  - "**/*Starter.java"
  - "**/*Api.java"
---

# 模块分层规范 (module-rules)

## 一，三层架构

> Maven 坐标：`groupId=io.mango`，`artifactId=mango-xxx`（如 `io.mango` / `mango-area-api`）

| 层 | 模块前缀 | 职责 |
|---|---------|------|
| 应用层（BFF） | `mango-bff-*` | 聚合接口（仅跨领域组合），参见 api-rules.md §1.3 |
| 领域层（Domain） | `mango-xxx-api/core/starter/remote` | 业务逻辑，4 子模块 |
| 基础设施层（Infra） | `mango-infra-*` | 第三方中间件集成，零侵入业务 |

---

## 二，领域层结构

每个业务域包含 4 个子模块：

| 子模块 | 后缀 | 职责 | 能有什么 | 禁止有什么 |
|--------|------|------|---------|-----------|
| 接口定义 | `-api` | 暴露跨模块 API | `po`、`vo`、`dto`、`XxxApi` 接口 | `entity`、`service`、`controller`、`mapper` |
| 核心实现 | `-core` | 业务逻辑 | `entity`、`service`、`mapper`、`dto` | `controller` |
| 本地调用 | `-starter` | 实现 api | controller（实现 `XxxApi`）、AutoConfiguration | — |
| 远程调用 | `-starter-remote` | 继承 api | FeignClient（继承 `XxxApi`）、AutoConfiguration | — |

---

## 三，依赖规则

### 3.1 禁止场景

| 禁止 | 说明 |
|------|------|
| BFF 禁止依赖 `api`、`core` | BFF pom 不得直接声明 api/core 依赖 |
| core 禁止被任何子模块依赖 | pom 中不得引入 `mango-xxx-core` |
| api 禁止定义 `entity` | 不得有 `@TableName` 等 DB 注解 |
| core 禁止有 controller | core 包中不得出现控制器 |
| Mapper 禁止跨域 SQL | 不得 JOIN 其他域的表 |
| BFF 禁止新增 controller | BFF 只做跨领域组合，参见 api-rules.md §1.3 |

### 3.2 允许的依赖方向

```
BFF ──► starter ──► api
   │          ▲
   └──► starter-remote ─┘

core ──► 本域 api
   └──► 其他域 api
```

### 3.3 具体规则

| 模块 | 能依赖 | 禁止依赖 |
|------|--------|---------|
| **BFF** | `starter`、`starter-remote` | `api`、`core` |
| **core** | 本域 `api`、其他域 `api` | `starter`、`starter-remote`、其他 `core` |
| **starter** | 本域 `api`、本域 `core` | 其他域 `starter`、`starter-remote`、`core` |
| **starter-remote** | 本域 `api` | 其他域 `api`、`core` |
| **api** | 无业务依赖 | 所有模块 |

### 3.4 代码示例

```java
// ✅ BFF：通过 starter 间接使用 api（传递依赖）
@Autowired
private SysUserApi sysUserApi;  // 由 mango-user-starter 传递而来

// ✅ core：依赖本域 api 和其他域 api
@Autowired
private UserApi userApi;  // 其他域 api
@Autowired
private OrderApi orderApi;  // 其他域 api

// ✅ starter：实现 api，调用 core service
@RestController
public class SysUserController implements SysUserApi {
    @Autowired
    private SysUserService sysUserService;  // 本域 core
}

// ✅ starter-remote：继承 api
@FeignClient(name = "user-service")
public interface SysUserFeignClient extends SysUserApi {
}
```

---

## 四，各子模块包结构

### 4.1 api

```
mango-xxx-api/
└── src/main/java/io/mango/xxx/api/
    ├── XxxApi.java      # XxxApi 接口
    ├── po/              # 纯数据对象（无 DB 注解）
    ├── vo/              # 视图对象
    └── dto/             # 数据传输对象
```

### 4.2 core

```
mango-xxx-core/
├── src/main/java/io/mango/xxx/core/
│   ├── entity/          # DB 实体（@TableName 等注解）
│   ├── service/         # 业务逻辑
│   │   ├── IXxxService.java
│   │   └── impl/
│   ├── mapper/          # 数据访问
│   └── dto/             # 内部 DTO
└── src/main/resources/
    └── db/migration/    # Flyway 数据库迁移文件（按 module 子目录隔离）
        └── user/        # user 模块独立目录（其他模块同理）
            ├── V1__init.sql           # DDL 建表
            └── V2__seed.sql           # 种子数据
```

> **注：** `db/migration/` 目录下的子目录按领域模块命名（如 `user/`、`area/`），每个子目录下放置该模块的 Flyway migration 文件（`V{version}__{description}.sql`）。详见 `db-migration-guide.md`。

### 4.3 starter

```
mango-xxx-starter/
└── src/main/java/io/mango/xxx/starter/
    ├── controller/
    │   └── XxxController.java  # implements XxxApi
    └── XxxAutoConfiguration.java
```

### 4.4 starter-remote

```
mango-xxx-starter-remote/
└── src/main/java/io/mango/xxx/starter/remote/
    └── XxxFeignClient.java     # extends XxxApi
```

### 4.5 BFF（理想状态）

```
mango-bff-admin/
├── src/main/java/io/mango/bff/admin/
│   └── BffAdminApplication.java   # 启动类
└── src/main/resources/
    └── application.yml             # 配置文件
```

**禁止：** BFF 新增 controller、service 等业务代码。

**说明：** BFF 可通过 starter 传递依赖间接引入各领域模块的 migration 文件（来自各模块的 `core/src/main/resources/db/migration/`）。BFF 自身不携带 `db/migration/`（除非有独立的 H2 开发数据库）。

---

## 五，表命名与 Mapper 跨域规范

### 5.1 表前缀规范

**核心原则：每个模块的表必须以模块前缀开头，参见 `naming-rules.md` §2.1。**

| 模块 | 表前缀 | 示例 |
|------|--------|------|
| user | `usr_` | `usr_user` |
| area | `area_` | `area_tree` |
| org | `org_` | `org_dept` |
| permission | `perm_` | `perm_menu` |
| i18n | `i18n_` | `i18n_lang` |
| system | `sys_` | `sys_config` |

### 5.2 Mapper 跨域规范

**核心原则：Mapper SQL 禁止跨域 JOIN，表前缀即域边界。**

```
areaMapper 只写 area_ 表
跨域数据 → 通过 XxxApi 接口查询
```

```xml
<!-- ❌ 禁止：AreaMapper 跨域 JOIN -->
SELECT a.*, u.username FROM area_tree a LEFT JOIN usr_user u ON ...

<!-- ✅ 正确：跨域通过 API -->
UserVO user = userApi.getUserById(area.getUserId());
```

---

## 七，Infra 层

```
mango-infra-xxx/
└── src/main/java/io/mango/infra/xxx/
    ├── annotation/      # 自定义注解
    ├── aspect/          # AOP 切面
    ├── impl/            # 核心实现
    └── starter/         # 配置类
        ├── XxxProperties.java
        └── XxxAutoConfiguration.java
```

- **零侵入**业务代码
- 可被 Domain 层或 BFF 层按需引入
- 配置前缀 `mango.xxx`

---

## 八，部署场景

| 场景 | BFF 依赖 | 调用方式 |
|------|---------|---------|
| 单体 | `mango-xxx-starter` | Controller 本地调用 |
| 微服务 | `mango-xxx-starter-remote` | FeignClient 远程调用 |

切换：修改 BFF 的 `pom.xml` 依赖，不改动业务代码。

---

## 九，新建服务

1. `mango-xxx-api` — 定义 `XxxApi` 接口、`po`、`vo`、`dto`
2. `mango-xxx-core` — 实现 `entity`、`service`、`mapper`
3. `mango-xxx-starter` — Controller **实现** `XxxApi`
4. `mango-xxx-starter-remote` — FeignClient **继承** `XxxApi`

---

## 十，SPI 自动配置

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.mango.xxx.starter.XxxAutoConfiguration
```
