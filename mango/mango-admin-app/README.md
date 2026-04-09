# Mango Admin App

> 管理后台应用 - 聚合用户、权限、国际化和组织架构服务

## 模块职责

| 职责 | 说明 |
|------|------|
| 数据聚合 | 聚合多个后端服务的数据，提供前端所需格式 |
| 协议转换 | 将内部服务协议转换为 RESTful API |
| 粗粒度数据 | 提供前端使用的粗粒度数据，减少前端计算 |
| 认证委托 | 委托给 gateway-starter 进行认证 |

## 架构定位

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                          │
└─────────────────────────┬───────────────────────────────────┘
                          │ RESTful API
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    BFF Admin (BFF层)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ User        │  │ Permission  │  │ I18n                │ │
│  │ Aggregation │  │ Aggregation │  │ Aggregation         │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ Gateway Starter (认证委托)                               ││
│  │  - AuthFilter (JWT验证)                                 ││
│  │  - WhiteList (白名单)                                   ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ User     │    │Permission │    │ I18n     │
    │ Service  │    │ Service   │    │ Service  │
    └──────────┘    └──────────┘    └──────────┘
```

## 依赖关系

```
mango-admin-app
├── mango-gateway-starter     # 认证委托
│   └── mango-auth-starter    # 认证配置
│       └── mango-auth-core   # 认证业务
├── mango-user-starter        # 用户服务
├── mango-permission-starter  # 权限服务
├── mango-i18n-starter        # 国际化服务
├── mango-org-starter         # 组织架构服务
├── mango-area-starter        # 地区服务
├── mango-ai-starter          # AI服务
└── mango-captcha-starter    # 验证码服务
```

## 单体模式 vs 微服务模式

### 单体模式

```
┌─────────────────────────────────────────────────────────────┐
│                    Mango Admin App                            │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Spring Boot Application                                 │ │
│  │                                                         │ │
│  │  ┌──────────────────────────────────────────────────┐   │ │
│  │  │ Auto Configurations                               │   │ │
│  │  │  - AuthAutoConfiguration                         │   │ │
│  │  │  - UserAutoConfiguration                         │   │ │
│  │  │  - PermissionAutoConfiguration                   │   │ │
│  │  │  - I18nAutoConfiguration                         │   │ │
│  │  │  - OrgAutoConfiguration                          │   │ │
│  │  │  - AreaAutoConfiguration                         │   │ │
│  │  │  - AiAutoConfiguration                           │   │ │
│  │  │  - CaptchaAutoConfiguration                      │   │ │
│  │  └──────────────────────────────────────────────────┘   │ │
│  │                                                         │ │
│  │  ┌──────────────────────────────────────────────────┐   │ │
│  │  │ REST Controllers (聚合层)                         │   │ │
│  │  │  - MenuController (菜单管理)                       │   │ │
│  │  │  - UserController (用户管理)                     │   │ │
│  │  │  - RoleController (角色管理)                     │   │ │
│  │  │  - DictController (字典管理)                     │   │ │
│  │  └──────────────────────────────────────────────────┘   │ │
│  │                                                         │ │
│  │  ┌──────────────────────────────────────────────────┐   │ │
│  │  │ Service Layer (本地调用)                          │   │ │
│  │  │  - UserService → IUserService (本地)             │   │ │
│  │  │  - MenuService → ISysMenuService (本地)         │   │ │
│  │  └──────────────────────────────────────────────────┘   │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**特点**:
- 所有服务打包在同一个应用中
- 本地方法调用，无网络开销
- 认证通过 AuthSecurityConfig 处理
- 数据库共享（同一 MySQL 实例）

### 微服务模式

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ AuthFilter (JWT验证)                                     │ │
│  │  - 验证通过后，传递 X-User-Id, X-Tenant-Id 头            │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────────────┐
        ▼               ▼                       ▼
┌───────────────┐ ┌───────────────┐      ┌───────────────┐
│ BFF Admin     │ │ BFF Portal    │      │ BFF Open      │
│ (用户管理)     │ │ (投标门户)     │      │ (开放平台)     │
│               │ │               │      │               │
│ REST Controllers│ REST Controllers│    REST Controllers│
└───────┬───────┘ └───────┬───────┘      └───────┬───────┘
        │ Feign            │ Feign              │ Feign
        │ Client           │ Client             │ Client
        └───────┬──────────┴───────┬────────────┘
                ▼                  ▼
        ┌───────────────┐  ┌───────────────┐
        │ User Service  │  │Permission Svc │
        │               │  │               │
        │ MySQL + Redis │  │ MySQL + Redis │
        └───────────────┘  └───────────────┘
```

**特点**:
- 每个服务独立部署
- 通过 Feign Client 进行远程调用
- 认证在网关层统一处理
- BFF 无需引入安全依赖

## BFF 实现原则

> **核心原则**: BFF 不实现任何业务逻辑，只做数据聚合和协议转换

### 应该做的

```java
// ✅ 聚合多个服务的数据
@Service
public class MenuAggregationService {
    public MenuTreeVO getMenuTree() {
        // 调用权限服务获取菜单
        List<SysMenuVO> menus = sysMenuService.listMenus();
        // 调用国际化服务获取翻译
        Map<String, String> i18nMap = i18nService.getTranslations();
        // 聚合返回
        return buildMenuTree(menus, i18nMap);
    }
}
```

### 不应该做的

```java
// ❌ BFF 不应该实现业务逻辑
@Service
public class MenuAggregationService {
    // 错误：不应该在这里实现 CRUD
    public void createMenu(SysMenu menu) {
        sysMenuMapper.insert(menu);
    }

    // 错误：不应该在这里实现复杂业务规则
    public boolean hasPermission(Long userId, String permission) {
        // ... 复杂的权限计算逻辑
    }
}
```

## 核心聚合接口

### 菜单聚合

| 接口 | 说明 |
|------|------|
| `GET /permission/menu` | 获取菜单树（含国际化） |
| `GET /permission/menu/{id}` | 获取单个菜单详情 |

### 用户聚合

| 接口 | 说明 |
|------|------|
| `GET /admin/user` | 分页查询用户（含组织信息） |
| `GET /admin/user/{id}` | 获取用户详情（含角色、岗位） |

## 配置

### 认证配置

BFF 通过 gateway-starter 委托认证：

```yaml
mango:
  gateway:
    auth-enabled: true
    jwt-secret: ${JWT_SECRET:mango-secret-key-must-be-at-least-256-bits}
    token-expire-seconds: 7200
```

### 服务扫描

```java
@SpringBootApplication
@MapperScan({
    "io.mango.user.core.mapper",
    "io.mango.permission.core.mapper",
    "io.mango.i18n.core.mapper",
    "io.mango.org.core.mapper",
    "io.mango.area.core.mapper"
})
public class MangoBffAdminApplication {
}
```

## 开发指南

### 新增聚合接口

1. 在对应的 `-api` 模块定义 VO
2. 在 BFF 创建 Controller
3. 注入需要的 Service Starter
4. 聚合数据返回

```java
@RestController
@RequestMapping("/admin/example")
public class ExampleController {

    private final UserService userService;
    private final I18nService i18nService;

    @GetMapping("/{id}")
    public R<ExampleAggregationVO> getExample(@PathVariable Long id) {
        // 聚合多个服务的数据
        UserVO user = userService.getUser(id);
        Map<String, String> i18n = i18nService.getTranslations();
        return R.ok(buildAggregation(user, i18n));
    }
}
```

### 禁用认证（仅开发环境）

```yaml
mango:
  gateway:
    auth-enabled: false  # 禁用认证过滤器
```

## 常见问题

### Q: BFF 和 直接调用微服务的区别？

| 对比 | BFF 模式 | 直接调用 |
|------|---------|---------|
| 数据聚合 | BFF 聚合 | 前端多次调用 |
| 网络开销 | 内部通信 | 客户端多次请求 |
| 协议转换 | BFF 转换 | 需要前端处理 |
| 复杂度 | BFF 维护成本 | 前端复杂度高 |

### Q: 微服务模式下 BFF 需要认证吗？

**不需要**。认证在 API Gateway 层完成，BFF 信任网关传递的用户信息（`X-User-Id` Header）。

### Q: BFF 如何选择单体还是微服务？

| 场景 | 推荐模式 |
|------|---------|
| 创业初期、快速迭代 | 单体模式 |
| 多团队并行开发 | 微服务模式 |
| 需要独立扩缩容 | 微服务模式 |
| 简单后台系统 | 单体模式 |
