# Mango - AI Native 开发底座

> 让 AI Agent 能够高效率、高质量地实现业务需求

## 项目组成

```
mango/
├── mango-app/          # 应用层
│   └── mango-admin-app/  # 管理后台
├── mango-common/       # 公共代码
├── mango-generator/    # 代码生成器
├── mango-infra/        # 基础设施
│   ├── mango-gateway/  # 网关
│   └── mango-infra-*/  # 技术组件（Redis/MyBatis/Cache 等）
├── mango-platform/     # 平台能力
│   ├── mango-ai/      # AI 能力
│   ├── mango-auth/    # 认证
│   ├── mango-rbac/    # 权限
│   ├── mango-org/     # 组织
│   ├── mango-system/  # 系统
│   ├── mango-i18n/    # 国际化
│   ├── mango-area/    # 区域
│   ├── mango-captcha/ # 验证码
│   └── mango-message/ # 消息
├── mango-parent/      # 父 POM
├── mango-tools/       # Maven 插件
├── mango-web/         # Vue 3 管理前端
└── mango-docs/        # 设计文档 & Sprint 计划
```

### 层次结构

| 层级 | 模块 | 说明 |
|------|------|------|
| 应用层 | mango-app | 部署单元 |
| 平台能力层 | mango-platform | 通用业务能力（用户/权限/认证等） |
| 基础设施层 | mango-infra | 技术组件（Redis/Cache/加密/网关） |
| 公共层 | mango-common | 项目级公共代码 |

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

| 特性 | 说明 |
|------|------|
| SPI + Starter | 部署拓扑无关，单体/微服务无缝切换 |
| DAL 抽象 | Redis/MySQL/Memory 通过接口访问 |
| @MangoTransactional | 本地/分布式事务配置切换 |
| M* 组件库 | 封装 Element Plus，统一前端规范 |
| AI Native | For AI Agent 的代码生成和检查 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.x + Spring Cloud Alibaba |
| 前端 | Vue 3 + Element Plus + Vite + TypeScript |
| 数据库 | MySQL + MyBatis-Plus + Flyway |
| 注册/配置 | Nacos |
| 分布式事务 | Seata |
| 国密算法 | SM2/SM3/SM4 |

## 快速开始

### 后端

```bash
cd mango
# 查看规范
cat .claude/rules/01-code.md

# 代码检查
mvn mango:check
```

### 前端

```bash
cd mango-web
npm install
npm run dev
```

## 文档

- [后端规范](./mango/.claude/rules/)
- [前端规范](./mango-web/.claude/rules/)
- [Sprint 计划](./mango-docs/plans/)
