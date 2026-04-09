# Mango 脚手架

## 项目定位

Mango 是一个面向 AI Agent 的 Java SpringBoot 脚手架，目标让 AI Agent 能够高效率地基于它实现业务需求。

**核心技术栈：** Java 17 + Spring Boot 3.x + MyBatis-Plus + Vue 3 + Element Plus

**核心价值：** AI 一句话需求 → 自动化实现（代码生成 + 规范检查）

---

## 文档索引

### 顶层设计

| 文档 | 说明 |
|------|------|
| [mango-architecture-design.md](./mango-architecture-design.md) | 顶层设计（D1-D29 困难）、模块清单（15个）、SPI 接口清单、技术栈版本、CLI 命令索引 |

### 实施计划

| 计划 | 状态 | 说明 |
|------|------|------|
| [sprint-00](./plans/2026-04-07-sprint-00-mango-module-architecture-plan.md) | 已完成 | 模块架构总纲 |
| [sprint-03](./plans/2026-04-08-sprint-03-mango-infra-dal-iucase-refactor.md) | 已完成 | mango-infra-dal IUCASE 重构 |
| [sprint-04](./plans/2026-04-08-sprint-04-mango-infra-dal-memoryxistore-fix.md) | 已完成 | dal MemoryXistore 修复 |
| [sprint-04b](./plans/2026-04-08-sprint-04b-dal-naming-fix.md) | 已完成 | dal 命名修复（kv→dal） |
| [sprint-05](./plans/2026-04-08-sprint-05-mango-infra-crypto.md) | 已完成 | 国密算法实现（SM2/SM3/SM4） |
| [sprint-06](./plans/2026-04-08-sprint-06-mango-infra-security.md) | 已完成 | 权限注解、AOP 切面 |
| [sprint-07](./plans/2026-04-08-sprint-07-mango-rbac-refactor.md) | 待执行 | mango-auth × mango-rbac DIP 重构 + 命名规范化 |

---

## 规范文件

位于 `mango/.claude/rules/`：

| 规范 | 内容 |
|------|------|
| 01-code.md | C1-C10 代码规范 |
| 02-naming.md | 命名规范 |
| 03-api.md | RESTful API 规范 |
| 04-db.md | 数据库规范 |
| 05-module.md | 模块分层规范 |
| 06-security.md | 安全规范 |
| 07-persistence.md | 事务规范 |
| 08-test.md | 测试规范 |
| 09-ui.md | UI 组件规范 |
| 10-dev-flow.md | Sprint + PRD + 人类介入 |

---

## 核心原则

1. **业务与部署拓扑分离** — 同一份代码支持单体/微服务/聚合部署
2. **Gateway 协议无关** — WebFlux 和 Servlet Filter 共存，业务不感知
3. **SPI 注入替代条件分支** — `@Autowired IXxxService` + `@ConditionalOnProperty`
4. **数据访问层抽象** — ICache/ILocker/ITokenStore/IIdempotent 等接口
5. **人类介入时机** — PRD 完成后 + 最终验收

---

## 技术栈版本

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.3 |
| Spring Cloud | 2023.0.0 |
| MyBatis-Plus | 3.5.5 |

---

## 已实现模块（15个业务域 + 10个 Infra）

**业务域**：`user` `auth` `permission` `org` `i18n` `area` `system` `captcha` `message` `ai`

**基础设施**：`infra-dal` `infra-crypto` `infra-security` `infra-redis` `infra-db` `infra-feign` `infra-web` `infra-observability` `infra-sse` `infra-websocket` `infra-doc`

**部署层**：`bff-admin` `gateway`

---

## 相关项目

| 项目 | 路径 | 说明 |
|------|------|------|
| 电子保函系统 | `docs/guarantee/` | 基于 Mango 脚手架实现的 B2B 业务系统 |
