# Mango - AI Native 开发底座

## 项目定位

Mango 是一个 **AI Native（AI 原生）** 的 Java Spring Cloud Alibaba 开发底座。

目标：让 AI Agent 能够高效率、高质量地实现业务需求。

---

## 基准原则

> 所有实现必须遵循以下原则，不可违背。

| 原则 | 说明 |
|------|------|
| **SPI + Starter** | 创建 `xxx-app` 模块并修改其 pom.xml 依赖（选 starter 或 starter-remote），即可切换单体/微服务部署，不改业务代码 |
| **DAL 层抽象** | Redis/DB/Memory 必须通过 `ICache`、`ILocker` 等接口访问，禁止直接调用底层客户端 |
| **禁止条件分支** | 不得用 `if (isMicroservice())` 切换实现，统一 SPI 注入 |
| **Gateway 协议无关** | 业务代码感知不到网关存在，不绑定技术栈 |
| **TTL 第一等公民** | 缓存超时禁止硬编码，必须配置化 |
| **DDL 通过 Flyway** | 数据库变更必须创建 migration 文件，禁止直敲 SQL |
| **国密算法** | 涉及加密必须使用 SM2/SM3/SM4 |
| **代码质量** | 方法 ≤50 行（复杂 ≤100），类 ≤500 行，重复率 ≤3% |

---

## 核心特性

### 1. SPI + Starter 机制

每个业务域包含 4 个子模块，通过 SPI 实现**部署拓扑无关**：

```
mango-xxx/
├── mango-xxx-api           # 接口定义（po/vo/dto/XxxApi）
├── mango-xxx-core          # 核心实现（entity/service/mapper）
├── mango-xxx-starter       # 本地调用（Controller 实现 XxxApi）
└── mango-xxx-starter-remote # 远程调用（FeignClient 继承 XxxApi）
```

**关键规则：**
- BFF 禁止依赖 `api`、`core`，只依赖 `starter` 或 `starter-remote`
- 切换部署方式：修改 pom.xml 依赖，**不改动业务代码**
- `core` 禁止有 controller，Mapper 禁止跨域 SQL

### 2. 协议无关 Gateway

Gateway 不绑定技术栈，业务代码感知不到网关存在。

### 3. 禁止条件分支

```java
// ❌ 禁止
if (isMicroservice()) {
    redisLocker.lock(...);
} else {
    localLocker.lock(...);
}

// ✅ 正确
@Autowired
ILocker locker;  // SPI 注入，由配置决定实现
```

### 4. DAL 层抽象（强制）

**所有 Redis/DB/Memory 操作必须通过 IUseCase 接口：**

| 接口 | 用途 | 禁止直接调用 |
|------|------|-------------|
| ICache | 缓存 | `redisTemplate.opsForValue()` |
| ILocker | 分布式锁 | `RedissonClient.getLock()` |
| ITokenStore | Token 存储 | 直接操作 Redis |
| IIdempotent | 防重复提交 | 直接 setnx |
| ITimeWindow | 时间窗口限流 | `zadd` + `zrangebyscore` |
| IRateLimiter | 令牌桶限流 | 任何限流实现 |

**TTL 是第一等公民，禁止硬编码。**

### 5. @MangoTransactional 事务

配置切换单体/分布式事务：

```yaml
mango:
  transaction:
    mode: local  # 单体部署
    # mode: seata # 微服务部署
```

### 6. 代码质量 CLI

```bash
mvn mango:check              # 全部检查
mvn mango:gen-module -Dname=xxx       # 生成模块
mvn mango:gen-crud -Dmodule=xxx -Dentity=User -Dtable=usr_user  # 生成 CRUD
```

---

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 17+ |
| 框架 | Spring Boot 3.x + Spring Cloud Alibaba |
| 数据库 | MyBatis-Plus + Flyway Migration |
| KV 存储 | Redis/db/memory（可切换） |
| 注册/配置 | Nacos |
| 分布式事务 | Seata |
| 国密算法 | SM2/SM3/SM4 |
| 前端 | Vue 3 + Element Plus + Vite + TypeScript |

---

## 目录结构

```
mango/
├── CLAUDE.md                    # 本文件
├── README.md                    # 项目总览
├── mango/                       # Java 后端底座
│   ├── CLAUDE.md               # 底座详细规范
│   ├── .claude/rules/         # 10 项技术规范
│   │       ├── 01-code.md          # 代码规范
│   │       ├── 02-naming.md        # 命名规范
│   │       ├── 03-api.md           # API 设计
│   │       ├── 04-db.md            # 数据库规范
│   │       ├── 05-module.md        # 模块分层（核心）
│   │       ├── 06-security.md      # 安全规范
│   │       ├── 07-persistence.md   # 事务规范
│   │       ├── 08-test.md          # 测试规范
│   │       ├── 09-ui.md            # UI 组件
│   │       └── 10-dev-flow.md      # 开发流程
│   │
│   ├── 公共层
│   │   └── mango-common/       # 公共代码
│   │
│   ├── 基础设施层
│   │   └── mango-infra/        # 技术组件
│   │       ├── mango-gateway/  # 网关
│   │       └── mango-infra-*/  # Redis/MyBatis/Cache 等
│   │
│   ├── 平台能力层
│   │   └── mango-platform/     # 通用业务能力
│   │       ├── mango-ai/      # AI 能力
│   │       ├── mango-auth/    # 认证
│   │       ├── mango-rbac/    # 权限
│   │       ├── mango-org/     # 组织
│   │       ├── mango-system/  # 系统
│   │       ├── mango-i18n/    # 国际化
│   │       ├── mango-area/    # 区域
│   │       ├── mango-captcha/ # 验证码
│   │       └── mango-message/ # 消息
│   │
│   ├── 应用层
│   │   └── mango-app/          # 部署单元
│   │       └── mango-admin-app/  # 管理后台
│   │
│   ├── mango-parent/           # 父 POM
│   └── mango-tools/            # Maven 插件
│
├── mango-web/                   # Vue 3 管理前端
│   └── CLAUDE.md              # 前端规范
└── mango-docs/                  # 设计文档 & Sprint 计划
    └── plans/                   # Sprint 计划
```

### 层次结构

| 层级 | 模块 | 说明 |
|------|------|------|
| 应用层 | mango-app | 部署单元（管理后台/Web/移动端） |
| 平台能力层 | mango-platform | 通用业务能力（用户/权限/认证/组织/消息等） |
| 基础设施层 | mango-infra | 技术组件（Redis/Cache/加密/可观测性/网关） |
| 公共层 | mango-common | 项目级公共代码（工具类/常量/注解） |

---

## 强制规范

### 代码规范要点

| 规则 | 限制 |
|------|------|
| 方法长度 | 普通 ≤50 行，复杂 ≤100 行 |
| 类长度 | 普通 ≤500 行，Controller ≤200 行，Service ≤300 行 |
| 重复率 | ≤ 3%（PMD CPD） |
| 异常捕获 | 禁止捕获 Exception/Throwable，禁止生吞异常 |

### 安全规范

- 禁止硬编码密钥（API_KEY、密码等）
- 禁止 SQL 拼接（必须参数化查询）
- 输入校验（@NotBlank、@Size 等）

### 数据库迁移

DDL 变更必须通过 Flyway migration 文件：

```
db/migration/{module}/V{version}__{description}.sql
```

---

## 上下文管理

| 条件 | AI 行为 |
|------|---------|
| 超过 60% | 提示用户 |
| 超过 80% | 建议总结 |
| 超过 90% | 强制总结 |

共识沉淀：`.mango/sessions/consensus/`
