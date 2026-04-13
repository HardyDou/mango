# Mango 脚手架 - AI 原生 Java 开发规范

## 项目概述

Mango 是一个 For AI Agent 的 Java SpringBoot 脚手架，目标让 AI Agent 高效率实现业务需求。

## 技术栈

| 组件 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus |
| 后端 | Java 17 + Spring Boot 3.x + MyBatis-Plus |
| 数据库 | H2/MySQL/可扩展 |
| KV存储 | Redis/db/memory |
| 注册/配置 | Nacos |

---

## ⚠️ 强制规范

规范在 `.claude/rules/`，**违反即错误**。

| 任务 | 规范 |
|------|------|
| Java 代码 | `.claude/rules/01-code.md` |
| 命名规范 | `.claude/rules/02-naming.md` |
| API 设计 | `.claude/rules/03-api.md` |
| 数据库 | `.claude/rules/04-db.md` |
| 模块分层 | `.claude/rules/05-module.md` |
| 安全规范 | `.claude/rules/06-security.md` |
| 事务处理 | `.claude/rules/07-persistence.md` |
| 测试 | `.claude/rules/08-test.md` |
| UI 组件 | `.claude/rules/09-ui.md` |
| 开发流程 | `.claude/rules/10-dev-flow.md` |

---

## 代码验证流程

```
生成代码 → 自检规范 → 修复问题 → 提交
              ↓
        mvn clean verify
        (必须通过才能提交)
```

---

## 模块结构

```
mango/
├── CLAUDE.md                    # 本文件
├── README.md                    # 模块说明
├── .claude/
│   ├── rules/                   # 规范（按路径加载）
│   ├── skills/                  # 可复用工作流
│   └── sessions/                 # 共识沉淀
│
├── 公共层
│   └── mango-common/            # 公共代码（工具类/常量/注解）
│
├── 基础设施层
│   └── mango-infra/             # 技术组件
│       ├── mango-gateway/      # 网关
│       ├── mango-infra-crypto/ # 国密算法
│       ├── mango-infra-security/ # 权限安全
│       ├── mango-infra-dal/    # 数据访问抽象
│       └── mango-infra-*/      # 其他技术组件
│
├── 平台能力层
│   └── mango-platform/          # 通用业务能力
│       ├── mango-ai/          # AI 能力
│       ├── mango-auth/        # 认证
│       ├── mango-rbac/        # 权限
│       ├── mango-org/         # 组织
│       ├── mango-system/     # 系统
│       ├── mango-i18n/       # 国际化
│       ├── mango-area/       # 区域
│       ├── mango-captcha/     # 验证码
│       └── mango-message/     # 消息
│
├── 应用层
│   └── mango-app/              # 部署单元
│       └── mango-admin-app/    # 管理后台
│
├── mango-parent/                # Maven 父项目
└── mango-tools/                 # Maven 插件 & 代码规则
    └── mango-maven-plugin/      # Mango CLI 插件（含模板和规则）
        └── src/main/resources/
            ├── templates/       # 代码生成模板
            └── rulesets/       # Checkstyle/PMD/SpotBugs 规则集
```

### 层次结构

| 层级 | 模块前缀 | 说明 |
|------|---------|------|
| 公共层 | mango-common | 项目级公共代码（工具类/常量/注解） |
| 基础设施层 | mango-infra-* | 技术组件（Redis/Cache/加密/可观测性/网关） |
| 平台能力层 | mango-xxx | 通用业务能力（用户/权限/认证/组织/消息等） |
| 应用层 | mango-*-app | 部署单元（管理后台/Web/移动端） |

---

## 常用命令

```bash
# 代码生成
mvn mango:gen-module -Dname=<name>
mvn mango:gen-crud -Dmodule=<module> -Dentity=<Entity> -Dtable=<table>

# 代码检查
mvn clean verify                    # 所有检查
mvn checkstyle:check               # 代码风格
mvn spotbugs:check                 # Bug 检测
mvn pmd:check                      # PMD 代码分析
```

---

## 核心规范要点

### 原则 1: 业务与部署拓扑分离

同一份代码支持单体/微服务/任意聚合部署：
每个服务包含 4 个子模块：

- `-api`
  - 业务对外提供API以及所需po/vo/util/dip接口定义（po/vo/dto/XxxApi）
- `-core`
  - 核心实现（entity/service/mapper）
  - 纯业务逻辑，不知道怎么被调用
- `-starter`
  - 本地调用（单体部署时注入），利用Controller 实现api中的接口
  - 本地调用启动器
- `-starter-remote`
  - 远程调用
  - Feign 调用（微服务部署时注入）

### 原则 2: Gateway 协议无关

Gateway 不绑定技术栈。业务代码感知不到网关存在。

### 原则 3: 禁止条件分支

不要 `if (deployment == microservices)`。用 SPI 注入替代：
```java
// ✅ 正确
@Autowired ILocker locker;

// ❌ 禁止
if (isMicroservice()) {
    redisLocker.lock(...);
} else {
    localLocker.lock(...);
}
```

### 原则 4: SPI 注入替代条件

`@Autowired IXxxService` + `@ConditionalOnProperty` 自动决定实现。

### 原则 5: 数据访问层抽象（强制）

**所有 Redis/DB/Memory 场景必须通过 IUseCase 接口，禁止直接调用实现。**

| 接口 | 用途 | 禁止直接调用 |
|------|------|------------|
| ICache | 缓存 | `redisTemplate.opsForValue()` |
| ILocker | 分布式锁 | `RedissonClient.getLock()` |
| ITokenStore | Token 存储 | 直接操作 Redis String |
| IIdempotent | 防重复提交 | 直接 setnx |
| ITimeWindow | 时间窗口限流 | `zadd` + `zrangebyscore` |
| IRateLimiter | 令牌桶限流 | 任何限流实现 |

**TTL 是每个接口的第一等公民**，禁止硬编码 TTL。

**强制检查**：`mvn mango:check` 检测以下违规，违者构建失败：
1. `new RedisLockerImpl()` — 必须 `@Autowired ILocker`
2. `redisTemplate.opsForZSet()` — 必须注入 IZSet
3. 硬编码 TTL — 必须从配置或参数传入



### 事务配置

| 部署方式 | 配置 | 注解 |
|---------|------|------|
| 单体 | `mango.transaction.mode = local` | @Transactional |
| 微服务 | `mango.transaction.mode = seata` | @MangoTransactional |

### 权限码格式

```
{model}:{module}:{action}
```

---

## 上下文管理

| 条件 | AI 行为 |
|------|---------|
| 超过 60% | 提示用户"上下文快满了" |
| 超过 80% | 强制建议总结 |
| 超过 90% | 必须总结后才能继续 |

共识沉淀位置：`.claude/sessions/consensus/`
