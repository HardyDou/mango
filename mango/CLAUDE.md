# Mango 后端规范

Java 后端脚手架，AI Agent 高效率编码指南。

---

## 核心原则

| 原则 | 说明 |
|------|------|
| SPI + Starter | SPI 机制 + Starter 模式，模块可插拔切换 |
| 模块拆分 | 业务能力通常划分为 api/core/starter/starter-remote；边界入口等运行时基础设施按实际形态拆分为 web-starter、gateway-starter 等，不滥用 remote 命名 |
| DAL 层抽象 | Redis/DB 必须通过 ICache/ILocker 接口 |

| 禁止条件分支 | 统一 SPI 注入，不用 if |
| TTL 配置化 | 缓存超时禁止硬编码 |
| DDL Flyway | 数据库变更必须 migration 文件 |
| 中文注释 | 新增和修改的代码注释、JavaDoc、README 与交付文档默认使用中文 |
| 作者标识 | 新增 Java 类型如需 `@author`，必须使用当前系统用户；代码生成器自动从系统用户提取 |

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
│   ├── mango-infra-kv/      # KV 存储（kv-api/kv-core/kv-starter）
│   ├── mango-infra-web/     # Web 封装
│   └── ...
├── mango-platform/          # 平台能力
│   ├── mango-access/        # 边界入口（core/web-starter/gateway-starter）
│   ├── mango-auth/          # 认证（api/core/starter/starter-remote）
│   ├── mango-identity/      # 身份（api/core/starter/starter-remote）
│   ├── mango-authorization/ # 授权与安全基础适配（api/core/security-starter/starter/starter-remote）
│   ├── mango-system/        # 系统（配置/字典/租户/区域/国际化）
│   ├── mango-org/           # 组织（api/core/starter/starter-remote）
│   └── ...
├── mango-extension/         # 可选扩展（如 mango-ai）
├── mango-app/               # 部署单元
├── mango-parent/            # 父 POM
└── mango-tools/             # 工具
```

---

## 常用命令

```bash
mvn spring-boot:run    # 启动
mvn mango:check        # 全部规范检查
mvn mango:check -Drule=persistence-schema    # 检查业务表标准字段
```
