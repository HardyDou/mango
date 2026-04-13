# Mango 后端规范

Java 后端脚手架，AI Agent 高效率编码指南。

---

## 核心原则

| 原则 | 说明 |
|------|------|
| SPI + Starter | SPI 机制 + Starter 模式，模块可插拔切换 |
| 模块拆分 | 模块划分为 api/core/starter/starter-remote，通过 -app 依赖 starter（内存）或 starter-remote（RPC）切换部署形态 |
| DAL 层抽象 | Redis/DB 必须通过 ICache/ILocker 接口 |

| 禁止条件分支 | 统一 SPI 注入，不用 if |
| TTL 配置化 | 缓存超时禁止硬编码 |
| DDL Flyway | 数据库变更必须 migration 文件 |

---

## 规范文件

| 规范 | 文件 |
|------|------|
| 代码规范 | `@mango-pmo/rules/backend/01-code.md` |
| 命名规范 | `@mango-pmo/rules/backend/02-naming.md` |
| API 规范 | `@mango-pmo/rules/backend/03-api.md` |
| 数据库规范 | `@mango-pmo/rules/backend/04-db.md` |
| 模块分层 | `@mango-pmo/rules/backend/05-module.md` |
| 安全规范 | `@mango-pmo/rules/backend/06-security.md` |
| 事务规范 | `@mango-pmo/rules/backend/07-persistence.md` |
| 测试规范 | `@mango-pmo/rules/backend/08-test.md` |

---

## 目录结构

```
mango/
├── mango-common/           # 公共代码（注解/工具类）
├── mango-infra/             # 基础设施
│   ├── mango-infra-dal/     # DAL 层（dal-api/dal-core/dal-starter）
│   ├── mango-infra-redis/   # Redis 封装
│   ├── mango-infra-security/# 安全（security-api/security-core/security-starter）
│   ├── mango-infra-web/     # Web 封装
│   ├── mango-gateway/       # 网关（api/core/starter/starter-remote）
│   └── ...
├── mango-platform/          # 平台能力
│   ├── mango-auth/          # 认证（api/core/starter/starter-remote）
│   ├── mango-rbac/          # 权限（api/core/starter/starter-remote）
│   ├── mango-system/        # 系统（api/core/starter）
│   ├── mango-org/           # 组织（api/core/starter/starter-remote）
│   ├── mango-area/          # 地区（api/core/starter/starter-remote）
│   └── ...
├── mango-app/               # 部署单元
├── mango-parent/            # 父 POM
└── mango-tools/             # 工具
```

---

## 常用命令

```bash
mvn spring-boot:run    # 启动
mvn mango:check        # 全部规范检查
```
