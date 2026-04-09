# Sprint 07: mango-auth × mango-rbac DIP 重构 + 命名规范化

- 起始日期：2026-04-08
- 状态：待执行
- 所属任务：T7
- 关联 plan：`2026-04-07-sprint-00-mango-module-architecture-plan.md`

---

## 问题描述

### 1. 依赖关系违规（核心问题）

`mango-auth-core` 存在两处违规：

| 违规 | 位置 | 说明 |
|------|------|------|
| `ISysMenuService` | `AuthServiceImpl:35` | Service 接口，禁止跨域调用 |
| `ISysRoleService` | `AuthServiceImpl:35` | Service 接口，禁止跨域调用 |

**根本原因**：`AuthServiceImpl` 直接知道了"菜单"、"角色"这些 RBAC 数据模型，而不是只知道自己需要的"权限校验能力"。

---

### 2. 重复实现（DRY 违规）

`mango-auth-core` 中存在两份 RBAC 服务实现，与 `mango-rbac-core` 重复：

| 重复文件 | 行数 | 违规说明 |
|----------|------|----------|
| `SysMenuServiceImpl.java` | 154 | 使用 `rbac-core` 的 `SysMenuMapper`、`SysRoleMenuMapper` |
| `SysRoleServiceImpl.java` | 167 | 使用 `rbac-core` 的 `SysRoleMapper`、`SysUserRoleMapper`、`SysRoleMenuMapper` |

---

### 3. 暴露型接口命名错误

| 错误写法 | 规范写法 | 规范依据 |
|----------|----------|----------|
| `IAuthService` | `AuthApi` | §6.2：暴露型接口必须用 `XxxApi` |
| `AuthController implements IAuthApi` | `implements AuthApi` | §6.2 |

---

### 4. 深层问题：DIP 违规（本次 Sprint 解决）

**核心问题**：Auth 应该只关心"能力"（权限校验），不应该知道 RBAC 的数据模型（菜单、角色）。

```
Auth 的领域知识 = "用户有没有权限"
Auth 不知道 = "菜单是什么"、"角色怎么配置"
```

**当前违规架构**：
```
AuthServiceImpl ──► SysMenuApi   （知道了菜单）
AuthServiceImpl ──► SysRoleApi   （知道了角色）
```

**改进后架构（DIP）**：
```
mango-auth-api:
└── IPermissionChecker      ← Auth 定义"有没有权限"能力接口
        ▲
        │ Spring IoC 注入
        │
mango-rbac-starter:
└── RbacPermissionChecker implements IPermissionChecker
                                  ↓
                          查询 rbac_menu / rbac_role 表
```

**关键效果**：
- Auth **编译时**完全不感知 RBAC（通过接口调用，编译依赖只有 api）
- 换 LDAP/OAuth2 只需新写一个实现，不改 Auth 代码

---

### 5. 模块命名不准确

`mango-permission` 实现的是 **RBAC**（Role-Based Access Control），不是 ACL。

---

## 架构原则

### 接口类型说明（05-module.md §6）

| 接口类型 | 命名规范 | 示例 | 可跨域调用？ |
|----------|----------|------|-------------|
| **暴露型** | `XxxApi` | `AuthApi`、`SysMenuApi` | ✅ 通过 HTTP/Feign |
| **注入型** | `IxxxProvider/Checker` | `IPermissionChecker` | ✅ 通过 Spring IoC |
| **内部型** | `IXxxService` | `ISysMenuService`、`ISysRoleService` | ❌ 仅本域 core 内 |

---

## 重构方案

### Phase A: DIP 重构 + 依赖修复

#### 核心：新增 `IPermissionChecker` DIP 接口

**Step 1. 在 `mango-auth-api` 新增 `IPermissionChecker` 接口**

```java
// mango-auth-api/src/main/java/io/mango/auth/api/IPermissionChecker.java
public interface IPermissionChecker {
    boolean hasPermission(Long userId, String permission);
    List<String> getUserPermissions(Long userId);
}
```

**Step 2. 在 `mango-rbac-starter` 实现 `RbacPermissionChecker`**

```java
// mango-rbac-starter/src/main/java/io/mango/rbac/starter/RbacPermissionChecker.java
@Component
public class RbacPermissionChecker implements IPermissionChecker {
    @Autowired
    private SysMenuService sysMenuService;

    public boolean hasPermission(Long userId, String permission) {
        // 查询 rbac_menu / rbac_role / rbac_user_role 表
    }

    public List<String> getUserPermissions(Long userId) {
        // 返回 permission 码列表
    }
}
```

**Step 3. 修改 `AuthServiceImpl`**

```java
// 删除：
// - private final SysMenuApi sysMenuApi;
// - private final SysRoleApi sysRoleApi;

// 新增：
@Autowired
private IPermissionChecker permissionChecker;

// 调用方式：
// permissionChecker.hasPermission(userId, "user:admin:read")
// permissionChecker.getUserPermissions(userId)
```

**Step 4. 新建 `SysRoleApi`**（Phase A 修复跨域违规必需）

- 位置：`mango-permission-api/src/main/java/io/mango/permission/api/SysRoleApi.java`
- 定义角色 CRUD 方法

**Step 5. 新建 `SysRoleController implements SysRoleApi`**

- 委托给 `ISysRoleService`（内部调用）

**Step 6. 重命名 `IAuthService` → `AuthApi`**

- 文件重命名 + 所有 implements/extends 引用

**Step 7. 修改 `mango-auth-core/pom.xml`**

- 删除 `mango-permission-core` 依赖
- 删除 `mango-permission-api` 依赖（不再需要 `SysMenuApi`/`SysRoleApi`）
- 确保 `mango-auth-api` 可传递引入

**Step 8. 删除冗余文件**

| 文件 | 说明 |
|------|------|
| `mango-auth-core/service/ISysMenuService.java` | 冗余，RBAC 数据模型 |
| `mango-auth-core/service/ISysRoleService.java` | 冗余，RBAC 数据模型 |
| `mango-auth-core/service/impl/SysMenuServiceImpl.java` | 重复实现 |
| `mango-auth-core/service/impl/SysRoleServiceImpl.java` | 重复实现 |
| `AuthServiceImpl.java` 中的 `SysMenuApi`、`SysRoleApi` 字段 | 改用 `IPermissionChecker` |

**Step 9. 删除/修改对应的 test 文件**

---

### Phase B: 模块重命名（依赖 Phase A 完成）

| 当前名称 | 目标名称 |
|----------|----------|
| `mango-permission` | `mango-rbac` |
| `mango-permission-api` | `mango-rbac-api` |
| `mango-permission-core` | `mango-rbac-core` |
| `mango-permission-starter` | `mango-rbac-starter` |
| `mango-permission-starter-remote` | `mango-rbac-starter-remote` |

### package 路径变更

| 当前 | 目标 |
|------|------|
| `io.mango.permission` | `io.mango.rbac` |

### 数据库表前缀

| 当前 | 目标 |
|------|------|
| `perm_menu` | `rbac_menu` |
| `perm_role` | `rbac_role` |
| `perm_user_role` | `rbac_user_role` |
| `perm_role_menu` | `rbac_role_menu` |

---

## 依赖关系图（重构后）

```
Layer 1: 基础设施
─────────────────────────────────────────────────────────────────
mango-infra-security
├── IPermissionService      ← @Perm 注解接口
├── ITokenService           ← JWT Token 原语
└── PermAspect              ← AOP 切面

Layer 2: 业务域
─────────────────────────────────────────────────────────────────
mango-auth (用户认证)
├── mango-auth-api
│   ├── AuthApi              ← 认证接口（暴露型）✅ IAuthService → AuthApi
│   └── IPermissionChecker  ← 权限校验接口（注入型）✅ 新增，Auth 定义
├── mango-auth-core
│   └── AuthServiceImpl
│       │
│       │ ──── @Autowired IPermissionChecker ────► 由 RBAC 实现注入
│       │          (注入型接口，编译时只依赖 auth-api) ✅ 新架构
│       │
│       └── @Autowired AuthApi（内部调用）
├── mango-auth-starter
│   └── AuthController implements AuthApi
└── mango-auth-starter-remote
    └── AuthFeignClient extends AuthApi

mango-rbac (权限管理) [原 mango-permission]
├── mango-rbac-api
│   ├── SysMenuApi           ← 菜单接口（暴露型）✅ 已有
│   ├── SysRoleApi           ← 角色接口（暴露型）✅ 新建
│   ├── ISysMenuService      ← 菜单接口（内部型）
│   └── ISysRoleService      ← 角色接口（内部型）
├── mango-rbac-core
│   ├── SysMenuServiceImpl   ← 菜单实现
│   └── SysRoleServiceImpl   ← 角色实现
├── mango-rbac-starter
│   ├── SysMenuController implements SysMenuApi ✅ 已有
│   ├── SysRoleController implements SysRoleApi ✅ 新建
│   └── RbacPermissionChecker implements IPermissionChecker ✅ 新增
└── mango-rbac-starter-remote
    └── SysMenuFeignClient extends SysMenuApi
```

> **关键区别**：`AuthServiceImpl` 不知道 `SysMenuApi`、`SysRoleApi`，只知道 `IPermissionChecker`。这是 DIP，RBAC 实现注入，不是跨域调用。

---

## 文件变更清单

### Phase A 新建

| 文件 | 说明 |
|------|------|
| `mango-auth-api/.../IPermissionChecker.java` | Auth 定义权限校验能力接口（注入型） |
| `mango-permission-api/.../SysRoleApi.java` | 角色 Controller 接口（暴露型） |
| `mango-permission-starter/controller/SysRoleController.java` | 实现 `SysRoleApi` |
| `mango-rbac-starter/.../RbacPermissionChecker.java` | 实现 `IPermissionChecker` |

### Phase A 测试场景

#### RbacPermissionChecker（新增）

**文件路径：** `mango-rbac-starter/src/test/java/io/mango/rbac/starter/RbacPermissionCheckerTest.java`

| 场景 | 输入 | 预期 |
|------|------|------|
| `hasPermission` 命中 | userId=1, permission="user:admin:read", 用户有该角色 | true |
| `hasPermission` 未命中 | userId=1, permission="user:admin:delete", 用户无该角色 | false |
| `hasPermission` 超管 | userId=1, 用户有超管角色（*:*） | true |
| `getUserPermissions` 正常 | userId=1, 用户有3个权限码 | 返回3个元素的 List |
| `getUserPermissions` 空 | userId=1, 用户无任何权限 | List.of() |
| `getUserPermissions` 停用用户 | userId=1, 用户已停用 | 返回空 List 或抛出异常 |
| 双重角色同一权限 | userId=1, 用户同时属于 roleA 和 roleB，均有相同权限 | true（去重后返回） |

#### AuthServiceImpl（重写）

**文件路径：** `mango-auth-core/src/test/java/io/mango/auth/core/service/impl/AuthServiceImplTest.java`

| 场景 | 说明 |
|------|------|
| login + IPermissionChecker 命中 | mock `IPermissionChecker.getUserPermissions()` 返回列表，验证 `LoginResponse` 含正确 roles/permissions |
| login + IPermissionChecker 空 | mock 返回空列表，验证 `LoginResponse` 含空 List |
| login 失败路径 | 验证 `sysUserApi` 异常时正常返回 null，不抛异常 |
| refreshToken + IPermissionChecker 正常 | 同 login 场景，覆盖 refreshToken 路径 |
| refreshToken + IPermissionChecker 空 | 同上，空权限场景 |
| logout | 验证 token 解析正确，不依赖 IPermissionChecker |
| validateToken | 验证 token 校验，不依赖 IPermissionChecker |

### Phase A 删除

| 文件 | 说明 |
|------|------|
| `mango-auth-core/service/ISysMenuService.java` | 冗余接口 |
| `mango-auth-core/service/ISysRoleService.java` | 冗余接口 |
| `mango-auth-core/service/impl/SysMenuServiceImpl.java` | 重复实现 |
| `mango-auth-core/service/impl/SysRoleServiceImpl.java` | 重复实现 |
| `AuthServiceImpl.java` 中的 `SysMenuApi` 字段 | 改用 `IPermissionChecker` |
| `AuthServiceImpl.java` 中的 `SysRoleApi` 字段 | 改用 `IPermissionChecker` |
| `AuthServiceImplTest.java` 中对应测试 | 删除相关测试用例 |

### Phase A 重命名

| 当前路径 | 目标路径 | 说明 |
|----------|----------|------|
| `IAuthService.java` | `AuthApi.java` | 暴露型接口命名规范 |

### Phase A 依赖变更

| 文件 | 变更 |
|------|------|
| `mango-auth-core/pom.xml` | 删除 `mango-permission-core`，删除 `mango-permission-api` |
| `mango-auth-starter/pom.xml` | 确保传递依赖 `mango-auth-api` |
| `mango-auth-starter/pom.xml` | 确保传递依赖 `mango-rbac-starter`（提供 `RbacPermissionChecker`） |

### Phase B 重命名

| 当前路径 | 目标路径 |
|----------|----------|
| `mango/mango-permission/` | `mango/mango-rbac/` |
| `mango/mango-permission-api/` | `mango/mango-rbac-api/` |
| `mango/mango-permission-core/` | `mango/mango-rbac-core/` |
| `mango/mango-permission-starter/` | `mango/mango-rbac-starter/` |
| `mango/mango-permission-starter-remote/` | `mango/mango-rbac-starter-remote/` |

---

## 实施步骤

### Phase A: DIP 重构 + 依赖修复

- [ ] A0. **提取 `loadUserRolesAndPermissions()` 私有方法**（`AuthServiceImpl`）
  - 范围：`login()` 和 `refreshToken()` 中的重复角色/权限加载代码
  - 理由：减少后续 DIP 重构时的变更面
  - 文件：`AuthServiceImpl.java` — 已完成
- [ ] A1. **新增 `IPermissionChecker` 接口**（`mango-auth-api`）
  - `boolean hasPermission(Long userId, String permission)`
  - `List<String> getUserPermissions(Long userId)`
- [ ] A2. **新增 `SysRoleApi` 接口**（`mango-permission-api`）
  - 定义 `list()`, `get(Long id)`, `getUserRoles(Long userId)` 等方法
- [ ] A3. **新增 `SysRoleController implements SysRoleApi`**（`mango-permission-starter`）
  - 委托给 `ISysRoleService`
- [ ] A4. **新增 `RbacPermissionChecker implements IPermissionChecker`**（`mango-rbac-starter`）
  - 实现 `hasPermission()`、`getUserPermissions()`
  - **查询约束（禁止 N+1）**：`hasPermission()` 必须用单条 JOIN 查询，禁止在循环内逐角色查权限
  - 推荐 SQL 模式：用户→角色→菜单 一次 JOIN，或 MyBatis nested resultMap
  - 路径：`mango-rbac-starter/src/main/java/io/mango/rbac/starter/RbacPermissionChecker.java`
- [ ] A5. **重命名 `IAuthService` → `AuthApi`**
  - 文件重命名 + implements/extends 引用
- [ ] A6. **修改 `AuthServiceImpl`**
  - 删除 `SysMenuApi`、`SysRoleApi`、`ISysRoleService` 字段
  - 新增 `IPermissionChecker permissionChecker` 字段
  - `loadUserRolesAndPermissions()` 改为调用 `IPermissionChecker`
- [ ] A7. **修改 `mango-auth-core/pom.xml`**
  - 删除 `mango-permission-core` 依赖
  - 删除 `mango-permission-api` 依赖（不再需要）
- [ ] A8. **删除冗余文件**
  - `ISysMenuService.java`、`ISysRoleService.java`
  - `SysMenuServiceImpl.java`、`SysRoleServiceImpl.java`
- [ ] A9. **重写/删除相关测试文件**
  - **重写** `AuthServiceImplTest.java`（`mango-auth-core/src/test/`）
    - 删除 `SysMenuApi`、`ISysRoleService` 的 mock
    - 改为 mock `IPermissionChecker`
    - 验证 `loadUserRolesAndPermissions()` 调用路径正确
  - **删除** `SysMenuServiceImplTest.java`（对应已删除的 `SysMenuServiceImpl`）
  - **删除** `SysRoleServiceImplTest.java`（对应已删除的 `SysRoleServiceImpl`）
  - **新增** `RbacPermissionCheckerTest.java`（`mango-rbac-starter/src/test/`）
    - 测试场景见"Phase A RbacPermissionChecker 测试场景"节
- [ ] A10. `cd mango && mvn clean compile` 验证编译通过

### Phase B: 模块重命名（依赖 Phase A 完成）

- [ ] B1. 重命名模块目录
- [ ] B2. 更新所有 pom.xml 中的模块引用
- [ ] B3. 全局搜索 `io.mango.permission` → `io.mango.rbac`
- [ ] B4. Flyway 迁移：`V{version}__rename_perm_to_rbac.sql`

### Phase C: 验证

- [ ] C1. `mvn clean verify` 确保编译 + 测试通过
- [ ] C2. 验证单体部署：启动应用，确认 `@Perm` 注解正常工作

---

## 验证命令

```bash
# === Phase A 验证 ===

# 1. 确认 mango-auth-core 不依赖任何 permission/rbac 模块
grep -r "mango-permission\|mango-rbac" mango/mango-auth/mango-auth-core/pom.xml
# 预期：无输出

# 2. 确认 IPermissionChecker 在 mango-auth-api 中
find mango/mango-auth-api -name "IPermissionChecker.java"

# 3. 确认 RbacPermissionChecker 在 mango-rbac-starter 中
find mango/mango-rbac-starter -name "RbacPermissionChecker.java"

# 4. 确认 AuthServiceImpl 使用 IPermissionChecker
grep -r "IPermissionChecker" mango/mango-auth/mango-auth-core/src/main/java
# 预期：只有 @Autowired IPermissionChecker

# 5. 确认 AuthServiceImpl 不使用 SysMenuApi / SysRoleApi
grep -r "SysMenuApi\|SysRoleApi" mango/mango-auth/mango-auth-core/src/main/java
# 预期：无输出

# 6. 确认 AuthServiceImpl 不依赖 ISysMenuService / ISysRoleService
grep -r "ISysMenuService\|ISysRoleService" mango/mango-auth/mango-auth-core/src
# 预期：无输出

# 7. 编译验证
cd mango && mvn clean compile

# 8. 测试验证
mvn clean verify

# === Phase B 验证 ===

# 9. 确认模块已重命名
ls -d mango/mango-rbac*

# 10. 确认 package 已重命名
grep -r "io.mango.rbac" mango/mango-rbac*/src/main/java | head -5

# 11. 确认 Flyway 迁移文件存在
find mango -path "*/db/migration/*" -name "*rbac*.sql"
```

---

## NOT in Scope

- `mango-infra-security` 改名或合并（层级不同）
- `mango-auth` 改名（auth 是业界标准命名）
- `mango-dal` 相关工作
- `mango-crypto` 相关工作（已完成）

---

## 参考

- `mango/.claude/rules/05-module.md` — 模块分层规范（§6 接口命名）
- `mango/.claude/rules/03-api.md` — API 规范
- `~/.gstack/projects/HardyDou-mango/hardy-main-design-2026-04-08-001.md` — 设计文档

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 0 | — | — |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 1 | CLEAR | 3 issues, 3 fixed |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | — | — |
| DX Review | `/plan-devex-review` | Developer experience gaps | 0 | — | — |

**VERDICT:** ENG REVIEW CLEARED — 3 issues identified and fixed in review session
