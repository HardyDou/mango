# Mango - AI Native 开发底座

## 项目定位

Mango 是一个 **AI Native（AI 原生）** 的 Java Spring Cloud Alibaba 开发底座。

目标：让 AI Agent 能够高效率、高质量地实现业务需求。

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
company02/
├── CLAUDE.md                    # 本文件
├── mango/                       # Java 后端底座
│   ├── CLAUDE.md               # 底座详细规范
│   └── .claude/rules/         # 10 项技术规范
│       ├── 01-code.md          # 代码规范
│       ├── 02-naming.md        # 命名规范
│       ├── 03-api.md           # API 设计
│       ├── 04-db.md            # 数据库规范
│       ├── 05-module.md        # 模块分层（核心）
│       ├── 06-security.md      # 安全规范
│       ├── 07-persistence.md   # 事务规范
│       ├── 08-test.md          # 测试规范
│       ├── 09-ui.md            # UI 组件
│       └── 10-dev-flow.md      # 开发流程
├── mango-web/                   # 管理前端（配套）
│   └── CLAUDE.md              # 前端规范
└── mango-docs/                  # 设计文档 & Sprint 计划
    └── plans/                   # Sprint 计划
```

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
