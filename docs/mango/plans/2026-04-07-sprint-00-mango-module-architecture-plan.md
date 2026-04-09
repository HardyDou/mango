# Mango 模块架构规划

## 一、模块总览

### 1.1 三层模块体系

```
┌─────────────────────────────────────────────────────────────┐
│                      基础设施层 INFRA                         │
│  mango-common | mango-gateway | mango-tools | mango-generator │
└─────────────────────────────────────────────────────────────┘
                              ↓ 复用
┌─────────────────────────────────────────────────────────────┐
│                     通用业务层 BUSINESS                       │
│  mango-auth | mango-permission | mango-user | mango-captcha  │
│  mango-org | mango-system | mango-area | mango-message       │
│  mango-i18n | mango-ai                                   │
└─────────────────────────────────────────────────────────────┘
                              ↓ 复用
┌─────────────────────────────────────────────────────────────┐
│                    电子保函层 GUARANTEE                      │
│  mango-guarantee | mango-project | mango-institution        │
│  mango-payment | mango-document                            │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 基础设施层（可跨项目复用）

| 模块 | 类型 | 职责 | 成熟度 | README 状态 |
|------|------|------|--------|----------|
| `mango-common` | JAR | 纯数据结构、Result、Exception、校验注解、工具类 | 100% | ❌ 待重构 |
| `mango-gateway` | JAR | API 网关（AuthFilter + JWT + 白名单） | 100% | ✅ 已有 |
| `mango-tools` | Maven Plugin | CLI（gen-module / gen-crud / gen-permission / check） | 100% | ❌ 缺失 |
| `mango-generator` | JAR | Velocity 模板（被 tools 调用） | 100% | ✅ 已有 |

### 1.3 通用业务层（电子保函系统可直接复用）

| 模块 | 类型 | 职责 | 成熟度 | README 状态 |
|------|------|------|--------|----------|
| `mango-auth` | 业务 | 登录/登出/JWT/刷新 token/防暴力破解 | 100% | ✅ 已有 |
| `mango-permission` | 业务 | RBAC 权限码、菜单树、角色 | 100% | ✅ 已有 |
| `mango-user` | 业务 | 用户 CRUD + 个人信息 | 100% | ❌ 缺失 |
| `mango-captcha` | 业务 | 图形/短信/邮件验证码 | 100% | ✅ 已有 |
| `mango-org` | 业务 | 组织、租户、部门、岗位 | 100% | ❌ 缺失 |
| `mango-system` | 业务 | 字典、配置、日志、租户 | 100% | ❌ 缺失 |
| `mango-area` | 业务 | 中国行政区划 | 100% | ❌ 缺失 |
| `mango-message` | 业务 | WebSocket/SSE 实时推送 | 100% | ❌ 缺失 |
| `mango-i18n` | 业务 | 国际化 | 100% | ❌ 缺失 |
| `mango-ai` | 业务 | AI 对话（DeepSeek） | 100% | ❌ 缺失 |

### 1.4 应用层（部署分组）

**注意：mango 使用"部署分组"概念，不是 BFF 层。BFF 是前端调用接口的聚合层，部署分组是把哪些 `*-starter` 打包到一个进程。**

| 模块 | 类型 | 职责 | 成熟度 | README 状态 |
|------|------|------|--------|----------|
| `mango-admin-app` | 部署分组 | 打包 auth/permission/user 等通用业务模块到一个进程 | 100% | ✅ 已有 |
| `guarantee-app` | 部署分组 | **电子保函业务部署分组**（打包 guarantee + 通用业务 starter） | 0% | ❌ 缺失 |

**部署分组命名规范**：
- `*-app` = 部署分组，把多个 `*-starter` 打包成一个 Spring Boot 应用
- `*-bff` = 前端聚合接口（如果有独立 BFF 进程）

### 1.5 基础设施子模块（mango-infra-*）

| 模块 | 职责 | 成熟度 | README 状态 |
|------|------|--------|----------|
| `mango-infra-db` | MyBatis-Plus + Druid + Flyway | 60% | ✅ 已有 |
| `mango-infra-redis` | Redisson 客户端配置 | 80% | ✅ 已有 |
| `mango-infra-feign` | OpenFeign + 重试 + 拦截器 | 70% | ✅ 已有 |
| `mango-infra-dal` | **DAL 抽象层（IUseCase 体系）** | 90% | ✅ 已完成 | → commit 8908595c |
| `mango-infra-crypto` | **SM2/SM4/AES/RSA 加密实现** | 80% | ✅ 已合并 | → sprint-05 merge, commit 5152d585 |
| `mango-infra-security` | **权限 AOP 切面 + JWT Token** | 80% | ✅ 开发中 | → sprint-06, PR#3 |
| `mango-infra-context` | **上下文传递（ThreadLocal 封装）** | 0% | ❌ 新建 |
| `mango-infra-observability` | Micrometer + OTLP 追踪 | 50% | ✅ 已有 |
| `mango-infra-doc` | SpringDoc OpenAPI 3.0 | 60% | ✅ 已有 |
| `mango-infra-web` | CORS + Actuator + 全局异常 | 40% | ✅ 已有 |
| `mango-infra-sse` | Server-Sent Events | 30% | ❌ 缺失 |
| `mango-infra-websocket` | WebSocket | 40% | ❌ 缺失 |

**mango-infra-dal 子模块结构（4 层）**：

```
mango-infra-dal/
├── mango-infra-dal-api/           ← IKvStore/ILocker/... 接口定义
├── mango-infra-dal-core/          ← RedisXivStore/DbXivStore/MemoryXivStore 实现
├── mango-infra-dal-starter/        ← @ConditionalOnBean + @ConditionalOnMissingBean 注入
└── mango-infra-dal-starter-remote/ ← Feign Client（微服务时跨进程调用）
```

**SPI 注入机制（部署时选择，无运行时降级）**：

| 配置 | 效果 |
|------|------|
| `mango.dal.kvstore.type=redis` | 强制使用 RedisXivStore（无 RedissonClient 则启动失败） |
| `mango.dal.kvstore.type=db` | 强制使用 DbXivStore（无 DataSource 则启动失败） |
| `mango.dal.kvstore.type=memory` | 强制使用 MemoryXivStore |
| 不配置 type | 自动检测：RedissonClient → DataSource → Memory |

**实现示例**：

```java
@Bean
@ConditionalOnProperty(prefix = "mango.dal.kvstore", name = "type", havingValue = "redis")
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnMissingBean(IKvStore.class)
public IKvStore redisXivStore(RedissonClient redisson) {
    return new RedisXivStore(redisson);
}
```

> 注意：**无运行时降级**。通过 Spring `@ConditionalOnBean` / `@ConditionalOnMissingBean` 在启动时确定用哪个实现，部署拓扑变更需重启服务。

### 1.6 框架分层原则（接口归属判断标准）

**判断一个接口该放哪层的唯一标准：它的实现依赖什么？**

| 层次 | 位置 | 依赖 | 接口举例 |
|------|------|------|---------|
| **Layer 0** | `mango-common` | 零运行时依赖（只依赖 JDK + 纯注解 JAR） | `IConverter`、`ISerializer`、`BizCode`、`@Perm`、`@Log` |
| **Layer 1** | `mango-infra-*` | 依赖中间件（Redis/MySQL/Kafka/Spring AOP 等） | `ICache`（Redis 实现）、`PermAspect`（AOP）、`TenantContextHolder`（ThreadLocal） |
| **Layer 2** | 业务模块 | 依赖业务逻辑 | `IUserService`、`IProjectService` |

**常见误区澄清：**

```
❌ "IUseCase 体系" 和 "MyBatis-Plus ORM" 是重复的？
✅ 完全不重复，在不同维度：

MyBatis-Plus ORM  → 管"数据库 CRUD、SQL 映射、事务"
IUseCase 体系     → 管"缓存、锁、计数、防重、ID 生成"

类比：超市 vs 超市里的生鲜区 — 完全不同的广度
```

**接口放错层的问题：**

| 错误 | 问题 | 后果 |
|------|------|------|
| 把 `ICache` 放 `common` | 实现依赖 Redis | common 引入 Redis 依赖，所有项目被迫装 Redis |
| 把加密实现留 `common` | 实现带 `@Component` | 业务系统 Bean 命名冲突 |
| 把 AOP 切面放 `common` | 依赖 Spring AOP | common 丧失跨项目复用能力 |

### 1.7 电子保函业务层（待建）

| 模块 | 职责 | 优先级 |
|------|------|--------|
| `mango-project` | 上游招标平台项目同步 | P0 |
| `mango-guarantee` | 保函申请/审批/出函/签章/验真/注销/索赔 | P0 |
| `mango-institution` | 下游金融机构对接 | P1 |
| `mango-payment` | 支付流程 | P1 |
| `mango-document` | 电子保函文档/模板/签章 | P2 |

---

## 二、每个模块的 README 标准模板

每个模块根目录必须有 `README.md`，结构如下：

```markdown
# {模块名}

## 职责
一句话描述：本模块负责 XXX，业务代码通过 XXX 接口使用。

## 技术实现
- 核心框架：XXX
- 数据存储：XXX
- 通信方式：XXX

## 模块结构（4 层）

```
{module-name}/
├── {module}-api/          ← 接口定义（PO/VO/DTO/Api 接口）
├── {module}-core/          ← 核心业务逻辑（Service/Mapper/Entity）
├── {module}-starter/       ← 本地调用启动器（@Autowired 直接用）
└── {module}-starter-remote/ ← 远程调用（Feign Client，微服务时注入）
```

## 核心接口

### Api 接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `XxxApi` | `xxx()` | XXX |

### Service 接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `IXxxService` | `xxx()` | XXX |

## 依赖关系

```
本模块依赖：
├── mango-common              ← 基础对象
├── mango-infra-dal          ← DAL（Cache/Locker 等 SPI）
└── mango-{other-business}  ← 业务间依赖（如需要）

本模块被依赖：
├── mango-bff-admin          ← 前端 BFF
└── mango-{other-service}   ← 其他业务服务
```

## 单体部署
直接 `@Autowired IXxxService`，本地方法调用。

## 微服务部署
通过 `*-starter-remote` 中的 Feign Client 调用：
```java
@Autowired
private XxxFeignClient xxxFeignClient;
```

## 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.xxx.enabled` | `true` | 是否启用 |
| `mango.xxx.timeout` | `3000` | 超时 ms |

## 使用示例

### Backend
```java
@Autowired
private IXxxService xxxService;
```

### Frontend
```bash
POST /xxx/xxx
```

## 约束（强制）

- ✅ 必须通过 SPI 注入使用
- ✅ 禁止直接 `new XxxImpl()`
- ✅ TTL 必须显式传入
- ❌ 禁止硬编码 TTL
```

---

## 三、每个模块职责边界

### mango-common（基础设施）

**职责边界**：
- 提供：`BasePO`、`BaseVO`、`PageVO`、`PageParam`、`PageResult<T>`
- 提供：统一结果封装 `R<T>` 和错误码接口 `BizCode`、断言工具 `Require`
- 提供：通用校验注解（`@NotBlank`、`@Size`、`@IdCard`、`@Phone` 等）
- 提供：业务特有注解（`@Perm`、`@Log`、`@Sensitive`、`@Version`、`@Encrypt`）
- 提供：通用枚举（`StatusEnum`、`YesNoEnum`、`DeleteFlagEnum`）
- 提供：SPI 接口（`IConverter`、`ISerializer`、`IEncryptor` 接口）
- 提供：纯工具类（`JacksonUtils`）
- **不提供**：任何加密实现（→ `mango-infra-crypto`）
- **不提供**：任何权限 AOP 逻辑（→ `mango-infra-security`）
- **不提供**：任何上下文实现（→ `mango-infra-context`）
- **不提供**：任何业务逻辑、任何存储操作、任何 RPC 调用

**核心原则：零运行时依赖。** `mango-common` 只依赖 JDK 和纯注解 JAR（如 `jakarta.validation.constraints`），可被任何 Java 项目直接引用。

**被依赖**：所有业务模块

---

### mango-infra-crypto（基础设施子模块）✅ 已完成

**职责边界**：
- 提供：SM2/SM4/AES/RSA 加密解密实现（带 `@Component`）
- 提供：`ICryptoService`、`ISignService` 接口 + `Sm2SignService`、`Sm3CryptoService`、`Sm4CryptoService` 实现
- 提供：`CryptoProperties`、`CryptoAutoConfiguration`（SPI 注入）
- 依赖：BouncyCastle（BC Provider）
- **不提供**：加密接口定义（→ `mango-common` 的 `ISymmetricCipher`/`IAsymmetricCipher`/`ISigner`/`IDigester` SPI 接口）

**子模块结构**：

```
mango-infra-crypto/                      ← 4 层结构
├── mango-infra-crypto-api/             ← 接口定义
│   └── io/mango/infra/crypto/api/
│       ├── ICryptoService.java          ← 加密服务接口（encrypt/decrypt）
│       └── ISignService.java           ← 签名服务接口（sign/verify）
├── mango-infra-crypto-core/             ← 核心实现
│   └── io/mango/infra/crypto/impl/
│       ├── sm/Sm2SignService.java      ← SM2 签名实现
│       ├── sm/Sm3CryptoService.java    ← SM3 摘要实现
│       ├── sm/Sm4CryptoService.java     ← SM4 加解密实现
│       └── sm/BouncyCastleLoader.java  ← BC Provider 加载器
├── mango-infra-crypto-starter/          ← SPI 自动配置
│   └── io/mango/infra/crypto/starter/
│       ├── CryptoProperties.java        ← @ConfigurationProperties
│       └── CryptoAutoConfiguration.java ← @AutoConfiguration
└── (无 starter-remote — crypto 无远程调用场景)

已合并至 origin/main，commit 5152d585
```

---

### mango-infra-security（基础设施子模块）✅ 开发中

**职责边界**：
- 提供：`PermAspect`（`@Aspect` + `@Component`）
- 提供：`IPermissionService` + `DefaultPermissionServiceImpl` 实现
- 提供：`ITokenService` + `JjwtTokenServiceImpl` 实现（JWT 管理）
- 依赖：Spring AOP、JJWT 0.12.x、可选 `mango-infra-dal`（IKvStore 用于 refresh token 黑名单）
- **不提供**：`IPermissionService` 接口（留在 `mango-infra-security-api`）

**子模块结构**：

```
mango-infra-security/                      ← 4 层结构
├── mango-infra-security-api/              ← 接口定义
│   └── io/mango/infra/security/api/
│       ├── IPermissionService.java       ← 权限校验接口
│       └── ITokenService.java           ← JWT 服务接口
│           record TokenPair(String accessToken, String refreshToken) {}
├── mango-infra-security-core/              ← 核心实现
│   └── io/mango/infra/security/core/impl/
│       ├── DefaultPermissionServiceImpl.java ← @Component 权限实现
│       └── JjwtTokenServiceImpl.java      ← @Component JWT 实现
│           - generateAccessToken / generateRefreshToken
│           - validateToken / getUserId / getUsername / getTokenType
│           - refresh (带 jti 黑名单防重放，可选 IKvStore)
│           - 支持 mango.security.jwt.secret(新) + mango.jwt.secret(旧) 兼容
├── mango-infra-security-starter/            ← SPI 自动配置
│   └── io/mango/infra/security/starter/
│       ├── SecurityAutoConfiguration.java  ← @AutoConfiguration
│       ├── TokenAutoConfiguration.java   ← @ComponentScan 注入
│       └── aspect/PermAspect.java        ← @Aspect 权限切面
└── mango-infra-security-starter-remote/    ← (空壳，待微服务场景)

JWT 配置：
  mango.security.jwt.secret   ← 新配置路径（优先）
  mango.jwt.secret           ← 旧配置路径（兼容 fallback）
  mango.security.jwt.access-token-validity-seconds   ← Access Token 有效期
  mango.security.jwt.refresh-token-validity-seconds ← Refresh Token 有效期

开发中：PR#3 open，sprint-06 分支
```

---

### mango-infra-context（基础设施子模块）⚠️ 新建

**职责边界**：
- 提供：`TenantContextHolder`、`TokenContextHolder`、`TraceContextHolder` 的 Spring 托管实现
- 提供：上下文传递（支持 TransmittableThreadLocal，可跨线程池传递）
- **不提供**：静态 `ThreadLocal`（从 common 移除）

**子模块结构**：

```
mango-infra-context/
├── TenantContextHolderImpl ← 从 common 移入，@Component
├── TokenContextHolderImpl   ← 从 common 移入，@Component
├── TraceContextHolderImpl   ← 从 common 移入，@Component
└── ContextProperties        ← @ConfigurationProperties
```

---

### mango-infra-dal（基础设施子模块）

**职责边界**：
- 提供：IUseCase 体系（`ICache`、`ILocker`、`ICounter`、`IRateLimiter`、`IIdempotent`、`ITokenStore`、`IIdGenerator` 等）
- 提供：Redis / DB / Memory 三种实现，**部署时通过 `@Conditional*` 选定**
- **无运行时降级** — 拓扑变更需重启服务
- **与 MyBatis-Plus ORM 的关系**：完全不同维度。前者管缓存/锁/计数，后者管数据库 CRUD SQL。

**4 层结构**：

| 子模块 | 内容 |
|-------|------|
| `-api` | IKvStore / ILocker / ICache / ... 接口定义 |
| `-core` | RedisXivStore / DbXivStore / MemoryXivStore 实现 |
| `-starter` | `@Conditional*` 注入选中的实现 |
| `-starter-remote` | Feign Client（微服务时跨进程调用） |

**SPI 注入**：

```java
// 显式指定（mango.dal.kvstore.type=redis）
@ConditionalOnProperty(prefix = "mango.dal.kvstore", name = "type", havingValue = "redis")
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnMissingBean(IKvStore.class)

// 自动检测（type 未配置时）
@ConditionalOnProperty(..., notHasValue = "auto", matchIfMissing = true)
@ConditionalOnMissingBean(IKvStore.class)
```

**强制约束**：业务模块禁止直接 `new RedisXivStoreImpl()`，必须 `@Autowired IKvStore`

**强制约束**：业务模块禁止直接 `new RedisLockerImpl()`，必须 `@Autowired ILocker`

---

### mango-gateway（基础设施）

**职责边界**：
- 入口：统一接收所有外部请求
- 认证：`AuthFilter` 验证 JWT，解析 `TenantContext` / `UserContext`
- 白名单：`SysPublicPathApi` 放过特定路径
- 协议无关：同时支持 WebFlux `WebFilter` 和 Servlet `Filter`
- **不提供**：业务逻辑、数据库操作

**被依赖**：外部调用方（前端、移动端、第三方系统）

**SPI 扩展**：
- `AuthFilter` 可通过 `@Order` 扩展
- 白名单路径通过 `SysPublicPathApi` 管理

---

### mango-auth（通用业务）

**职责边界**：
- 提供：登录（用户名密码/验证码）、登出、Token 刷新
- 提供：登录防暴力破解（`LoginAttemptTracker`）
- 提供：JWT 生成与验证
- **不提供**：用户注册（`mango-permission` 提供 `SysUser`）
- **不提供**：权限校验（`mango-permission` 提供）

**API**：
- `POST /auth/login` → `LoginResponse`（含 token）
- `POST /auth/logout`
- `POST /auth/refresh`

**SPI 扩展**：
- 防暴力破解：`ILocker`（复用 `mango-infra-dal`）
- 登录方式：可扩展短信/邮件/扫码登录

---

### mango-permission（通用业务）

**职责边界**：
- 提供：用户管理（CRUD）、菜单管理、角色管理、权限码管理
- 提供：功能权限校验（`@RequirePermission`）
- 提供：数据权限过滤（租户/部门维度）
- **不提供**：认证（JWT/登录）— 由 `mango-auth` 提供

**API**：
- `SysUserApi`：用户 CRUD
- `SysMenuApi`：菜单 CRUD
- `SysRoleApi`：角色 CRUD
- `SysPublicPathApi`：白名单路径管理

**SPI 扩展**：
- 权限校验：`IPermissionService` 可扩展数据权限
- 菜单加载：`IMenuLoader` 可扩展动态菜单

---

### mango-user（通用业务）

**职责边界**：
- 提供：用户档案管理（个人信息、头像、联系方式）
- **不提供**：认证（`mango-auth`）、权限（`mango-permission`）
- 与 `mango-permission` 的 `SysUser` 的关系：`SysUser` 是权限侧的用户实体，`User` 是业务侧的档案实体

---

### mango-captcha（通用业务）

**职责边界**：
- 提供：图形验证码（算术/滑块）、短信验证码、邮件验证码
- 提供：SPI 存储抽象（`CaptchaStorage`）— Redis/DB/Memory 自动选择
- **不提供**：业务校验（发送频率由调用方通过 `ITimeWindow` 控制）

**SPI 扩展**：
- 存储：`CaptchaStorage`（Redis > DB > Memory）
- 发送：`SmsProvider`、`EmailProvider`（可扩展运营商对接）

---

### mango-org（通用业务）

**职责边界**：
- 提供：组织架构（租户/公司/部门/岗位）
- 提供：多租户隔离（通过 `TenantContextHolder`）
- **不提供**：人员管理（`mango-permission`）

---

### mango-system（通用业务）

**职责边界**：
- 提供：系统参数配置、字典数据、操作日志、登录日志
- 提供：租户管理（创建/禁用/配额）
- **不提供**：业务配置（业务配置放在对应业务模块）

---

### mango-message（通用业务）

**职责边界**：
- 提供：实时消息推送（WebSocket / SSE）
- 提供：消息通道抽象（`MessageChannel`）— 支持多通道
- **不提供**：消息存储（由调用方处理）

**SPI 扩展**：
- 通道：`MessageChannel`（WebSocket / SSE）

---

### mango-area（通用业务）

**职责边界**：
- 提供：中国行政区划（省/市/区/县）数据
- 提供：行政区划树查询
- **不提供**：业务逻辑

---

### mango-i18n（通用业务）

**职责边界**：
- 提供：国际化文本存储与查询
- 提供：多语言切换
- **不提供**：前端语言切换逻辑

---

### mango-ai（通用业务）

**职责边界**：
- 提供：AI 对话（DeepSeek provider）
- 提供：对话上下文管理
- **不提供**：AI 应用场景逻辑（由业务模块调用）

---

### mango-bff-admin（应用）

**职责边界**：
- 提供：聚合前端所有请求（聚合 `*-api` 接口）
- 提供：协议转换（内部 RPC → 外部 HTTP REST）
- 提供：单体部署时 `*-starter` 本地调用聚合
- **不提供**：任何独立业务逻辑

---

### mango-infra-db（基础设施子模块）

**职责边界**：
- 提供：MyBatis-Plus 配置、Druid 连接池、Flyway 迁移
- **不提供**：多数据源（待实现）、分库分表（待实现）

---

### mango-infra-redis（基础设施子模块）

**职责边界**：
- 提供：RedissonClient 自动配置
- **不提供**：业务层面的缓存/锁逻辑（由 `mango-infra-dal` 提供）

---

### mango-infra-feign（基础设施子模块）

**职责边界**：
- 提供：OpenFeign 自动配置、重试策略、请求拦截器（自动注入 Tenant/Trace ID）
- **不提供**：FallbackFactory（待实现）

---

### mango-infra-observability（基础设施子模块）

**职责边界**：
- 提供：`@Timed` 注解、OTLP 追踪埋点、Micrometer 指标
- **不提供**：日志聚合（ELK）、告警

---

### mango-infra-doc（基础设施子模块）

**职责边界**：
- 提供：SpringDoc OpenAPI 3.0 文档生成
- **不提供**：Knife4j UI（待实现）、API 认证

---

### mango-infra-web（基础设施子模块）⚠️ 需重构

**职责边界**：
- 提供：CORS、Actuator
- **缺失**：全局异常处理器（`GlobalExceptionHandler`）、统一个人感觉 `R<T>` 封装、限流

---

### mango-infra-sse（基础设施子模块）⚠️ 需完善

**职责边界**：
- 提供：基础 SSE 支持
- **缺失**：断线重连、消息聚合、心跳

---

### mango-infra-websocket（基础设施子模块）⚠️ 需完善

**职责边界**：
- 提供：基础 WebSocket 配置
- **缺失**：消息代理（RabbitMQ/Kafka）、房间管理、Session 集群

---

## 四、模块依赖关系图

### 4.1 部署分组概念（单体 vs 微服务）

```
单体部署（一个进程包含所有模块）
┌─────────────────────────────────────────────────────────────┐
│                    guarantee-app (单进程)                     │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Controller Layer（直接调用各 *-starter）               │ │
│  │  - /portal/*, /admin/*, /open/*                      │ │
│  └───────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Service Layer（*-starter，本地调用）                   │ │
│  │  - guarantee-starter                                  │ │
│  │  - auth-starter ←────────┐                          │ │
│  │  - permission-starter    │                           │ │
│  │  - user-starter         ─┼── 通用业务模块复用        │ │
│  │  - org-starter          ──┤                          │ │
│  │  - captcha-starter     ──┘                          │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

微服务部署（每个 *-app 是独立进程）
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  guarantee-app   │  │   auth-app      │  │  permission-app │
│  (保函业务)      │  │  (认证服务)      │  │  (权限服务)    │
│  4个模块打包     │  │  独立扩缩容     │  │  独立扩缩容    │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │
         └─────────────────────┴──────────────────────┘
                              ↓
                    ┌──────────────────┐
                    │  gateway-app     │
                    │  (统一入口)       │
                    └──────────────────┘
```

### 4.2 电子保函业务部署分组

```
guarantee-app（电子保函业务部署分组）
├── guarantee-starter     → 保函核心业务（项目/保函/机构/支付/文档）
├── auth-starter         → 认证服务
├── permission-starter   → 权限服务
├── user-starter         → 用户档案
├── org-starter         → 组织架构
├── captcha-starter      → 验证码
└── ...

微服务部署时：
guarantee-app（独立进程）←──Feign──→ auth-app（独立进程）
                                    permission-app
                                    user-app
                                    ...
```

### 4.3 部署分组命名规范

| 概念 | 命名规范 | 说明 |
|------|---------|------|
| 部署分组 | `*-app` | 把多个 `*-starter` 打包成一个 Spring Boot 应用 |
| 服务 | `*-service` | 同上，部署分组别名 |
| BFF（仅当有独立聚合进程时） | `*-bff` | 前端聚合接口进程 |

**注意**：`mango-bff-admin` 已改名为 `mango-admin-app`，是 mango 脚手架的部署分组，不是 BFF 层。

---

## 五、电子保函业务模块（待建）

### 5.1 电子保函部署分组

```
guarantee-app（电子保函业务部署分组，打包多个 starter 到一个进程）
├── guarantee-starter     → 保函核心业务（项目/保函/机构/支付/文档）
├── auth-starter         → 认证服务
├── permission-starter   → 权限服务
├── user-starter         → 用户档案
├── org-starter         → 组织架构
├── captcha-starter     → 验证码
└── ...
```

### 5.2 电子保函业务服务（*-core）

```
电子保函业务层（每个 *-core 是独立可复用的业务模块）
├── mango-project    → 上游招标平台项目同步（业主发布项目 → 平台）
├── mango-guarantee  → 保函全生命周期（申请 → 支付 → 出函 → 签章 → 验真 → 注销/索赔）
├── mango-institution→ 下游金融机构对接（银行/保险/担保公司出函）
├── mango-payment    → 支付流程（保证金支付）
└── mango-document   → 电子保函文档（模板/签章/验真）
```

### 5.3 复用关系

这些电子保函模块依赖 mango 脚手架的通用业务层：

```
guarantee-app
    ├── guarantee-core      → 保函核心业务
    ├── auth-starter        → 复用 mango-auth
    ├── permission-starter  → 复用 mango-permission
    ├── user-starter        → 复用 mango-user
    ├── org-starter         → 复用 mango-org
    ├── captcha-starter     → 复用 mango-captcha
    ├── area-starter        → 复用 mango-area
    └── message-starter     → 复用 mango-message
```

---

## 六、README 创建计划

| 模块 | 操作 | 模板章节重点 |
|------|------|------------|
| `mango-common` | 重构 | 纯粹化 + API 速查 |
| `mango-infra-crypto` | ✅ 已完成 | 加密实现 + ICryptoService/ISignService |
| `mango-infra-security` | 🔄 开发中 | 权限 AOP + IPermissionService + ITokenService |
| `mango-infra-context` | 新建 | 上下文传递 |
| `mango-infra-dal` | 重写 | IUseCase 接口体系 + 实现选择 |
| `mango-tools` | 新建 | CLI 命令速查 |
| `mango-user` | 新建 | UserApi / IUserService |
| `mango-org` | 新建 | OrgApi（租户/部门/岗位） |
| `mango-system` | 新建 | DictApi / SysConfigApi |
| `mango-area` | 新建 | AreaApi（行政区划） |
| `mango-message` | 新建 | MessageChannel / WebSocket-SSE |
| `mango-i18n` | 新建 | I18nApi |
| `mango-ai` | 新建 | AiApi |
| `mango-infra-sse` | 新建 | SSE 使用方式 |
| `mango-infra-websocket` | 新建 | WebSocket 使用方式 |
| `mango-infra-web` | 更新 | 全局异常/R\<T\> 封装 |
| `mango-auth` | 更新 | SPI 扩展点（防暴力→ITimeWindow） |
| `mango-permission` | 更新 | 数据权限扩展 |
| `mango-captcha` | 更新 | 存储 SPI |
| `mango-gateway` | 更新 | WebFlux+Servlet 双协议 |
| `mango-generator` | 更新 | 模板变量说明 |
| `guarantee-app` | 新建 | 电子保函业务部署分组 |

---

## 七、强制规范

每个模块 **必须** 遵守：
1. `*-api` 只定义接口和 PO/VO/DTO，不写业务逻辑
2. `*-core` 只写业务逻辑，不知道被谁调用（本地还是远程）
3. `*-starter` 提供本地调用实现（单体部署时生效）
4. `*-starter-remote` 提供 Feign 调用实现（微服务部署时生效）
5. 所有 Redis 场景必须通过 `IUseCase` 接口，禁止直接操作 `RedisTemplate`
6. `mvn mango:check` 检测违规，构建失败禁止提交

---

## 八、待执行任务

| # | 任务 | 类型 | 状态 | 说明 |
|---|------|------|------|------|
| T1 | `mango-infra-kv` → `mango-infra-dal` 重构：4 层结构 + `@Conditional*` 注入 | 架构重构 | ✅ 已完成 | commit c0b7df41 |
| T2 | `mango-bff-admin` → `mango-admin-app` 重命名 | 重构 | 待执行 | 目录重命名 + pom artifactId 更新 + import 修正 |
| T3 | IUseCase 9 个接口定义 | 新增 | 🔄 已拆分 | 拆分为 `plans/2026-04-08-sprint-03-mango-infra-dal-iucase-refactor.md` |
| T4 | MemoryXivStore TTL=0 清理逻辑修复 | Bug修复 | 🔄 已拆分 | 拆分为 `plans/2026-04-08-sprint-04-mango-infra-dal-memoryxistore-fix.md` |
| T5 | `mango-infra-crypto` 新建：crypto/ 从 common 移入 | 新建模块 | ✅ 已完成 | 13 个文件从 common/crypto 移入，包含 Sm4Cipher/AesCipher/RsaSigner/Sm2Signer/CryptoFactory → sprint-05 merge commit 5152d585 |
| T6 | `mango-infra-security` 新建：permission/ 从 common 移入 + JWT 合并 | 新建模块 | 🔄 开发中 | PermAspect + IPermissionService + ITokenService(JJWT) → sprint-06, PR#3 |
| T7 | `mango-infra-context` 新建：context/ 从 common 移入 | 新建模块 | 待执行 | TenantContextHolder + TokenContextHolder + TraceContextHolder 从 common 移入，支持 TransmittableThreadLocal |
| T8 | `mango-common` 纯粹化：新增注解/枚举/SPI 接口 | 重构 | 待执行 | 新增 @Sensitive/@Version/@Encrypt、StatusEnum/YesNoEnum/DeleteFlagEnum、IConverter、ISerializer、PageParam、PageResult |
