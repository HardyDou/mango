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

### 4.5 admin-app（理想状态）

```
mango-admin-app/
├── src/main/java/io/mango/admin/
│   └── MangoAdminAppApplication.java   # 启动类
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
| rbac | `rbac_` | `rbac_menu`（原 `perm_menu`，Phase B 迁移后生效） |
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

## 六，接口命名规范

### 6.1 三种接口类型

| 接口类型 | 命名 | 定义者 | 调用者 | 实现者 | 示例 |
|----------|------|--------|--------|--------|------|
| **暴露型** | `XxxApi` | 本模块 | 外部模块 | 本模块 starter | `AuthApi`、`SysMenuApi` |
| **注入型** | `IxxxProvider` | 本模块 | 本模块 | 他模块注入 | `IUserContextProvider` |
| **内部型** | `IXxxService` | 本模块 | 本模块 starter | 本模块 core | `ISysMenuService` |

### 6.2 暴露型接口（XxxApi）

**定义者**：本模块
**调用者**：外部模块（网关/BFF/其他业务域）
**命名**：`XxxApi`（必须以 `Api` 结尾）

```
mango-auth-api/
└── AuthApi.java              ← 认证接口，外部调用
mango-rbac-api/
├── SysMenuApi.java           ← 菜单接口，外部调用
└── SysRoleApi.java           ← 角色接口，外部调用
```

**特征**：本模块用 `starter` 实现接口，他模块直接调用。

### 6.3 注入型接口（DIP 接口）

**定义者**：本模块
**调用者**：本模块内部（core）
**实现者**：外部模块通过 Spring IoC 注入
**命名**：`I` + 能力 + `Provider` / `Validator` / `Checker`

```
mango-auth-api/
├── IUserContextProvider    ← 用户上下文，rbac/ldap/oauth2 实现
├── ITokenValidator         ← Token 验证器，jwt/redis 实现
└── IAntiBruteForce        ← 防暴力破解，第三方实现
```

| 接口名 | 能力 | 实现者 |
|--------|------|--------|
| `IUserContextProvider` | 用户上下文提供 | rbac / ldap / oauth2 |
| `ITokenValidator` | Token 验证 | jwt / redis / external |
| `IAntiBruteForce` | 防暴力破解 | memory / redis / external |
| `IPermissionChecker` | 权限校验 | rbac / custom |

**特征**：本模块定义接口契约，本模块 core 调用，**他模块实现后注入**。

### 6.4 内部型接口（IXxxService）

**定义者**：本模块
**调用者**：本模块 starter
**实现者**：本模块 core
**命名**：`I` + 能力 + `Service`

```
mango-rbac-api/
├── ISysMenuService.java     ← 菜单内部服务
└── ISysRoleService.java     ← 角色内部服务
mango-rbac-core/
├── SysMenuServiceImpl.java  ← 实现
└── SysRoleServiceImpl.java  ← 实现
```

**特征**：本模块内部使用，不对外暴露。

### 6.5 依赖倒置示例

```
┌─────────────────────────────────────────────────────────┐
│ mango-auth                                               │
│                                                          │
│  auth-api:                                               │
│  ├── AuthApi              ← 暴露型，外部调用              │
│  ├── TokenApi             ← 暴露型，外部调用              │
│  └── IUserContextProvider ← 注入型，外部实现后注入         │
│         ▲                                                │
│         │ Spring IoC 注入                                │
└─────────┼────────────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────────────┐
│ mango-rbac                                              │
│                                                          │
│  rbac-starter:                                          │
│  └── RbacUserContextProvider implements IUserContextProvider
│         │                                                │
│         ▼                                                │
│  rbac-core:                                             │
│  └── SysMenuServiceImpl / SysRoleServiceImpl            │
└──────────────────────────────────────────────────────────┘
```

### 6.6 代码示例

```java
// ✅ 暴露型：本模块定义，外部调用
public interface AuthApi {
    LoginVO login(LoginDTO dto);
}

// ✅ 注入型：本模块定义，他模块实现后注入
public interface IUserContextProvider {
    UserContext getCurrentUser();
    boolean hasRole(String roleCode);
}

// ✅ core：调用注入型接口，不知道谁实现
public class AuthServiceImpl {
    @Autowired
    private IUserContextProvider userContextProvider;  // 不知道谁实现
}

// ✅ starter-remote：继承暴露型接口
@FeignClient(name = "auth-service")
public interface AuthFeignClient extends AuthApi {
}
```

---

## 七，Infra 层

### 7.1 模块结构

Infra 层是基础设施，**不需要 `starter-remote`**（微服务远程调用）。因为基础设施必须跟应用一起部署，不存在独立微服务形态。

```
mango-infra-xxx/
├── mango-infra-xxx-api/       # 接口定义（如 IPermissionService）
├── mango-infra-xxx-core/      # 核心实现（如 DefaultPermissionServiceImpl）
└── mango-infra-xxx-starter/   # 本地注入（AutoConfiguration）
    └── XxxAutoConfiguration.java
```

> **注意**：Infra 层禁止创建 `*-starter-remote` 子模块。基础设施是嵌入式库，不是微服务。

### 7.2 依赖规则

| Infra 子模块 | 能依赖 | 禁止依赖 |
|-------------|--------|---------|
| `*-api` | 无业务依赖 | 所有模块 |
| `*-core` | 本域 api | 其他 infra 模块的 core、starter |
| `*-starter` | 本域 api、本域 core | 其他 infra 模块 |

### 7.3 通用结构

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
