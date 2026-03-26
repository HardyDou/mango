# Mango 脚手架 - AI 原生 Java 开发规范

## 项目概述

Mango 是一个 For AI Agent 的 Java SpringBoot 脚手架，目标让 AI Agent 高效率实现业务需求。

## 技术栈

| 组件 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus |
| 后端 | Java + Spring Boot + Spring Cloud Alibaba |
| 数据库 | MySQL |
| 缓存 | Redis |
| 注册/配置 | Nacos |

## 模块结构

```
mango/
├── rules/                       # AI-Executable 规范
├── .mango/roles/               # AI 角色定义
├── tools/                       # Mango CLI 工具链
├── mango-parent/               # Maven 父项目
└── mango-common/               # 公共模块
```

## 规范文件索引

### 模块分层规范（必读）
- `rules/module-rules.md` → 服务模块分层、Starter 机制

### 代码规范（必读）
- `rules/code-rules.md` → C1-C5 代码规范
- `rules/naming-rules.md` → Java/Database 命名规范
- `rules/security-rules.md` → 安全规范
- `rules/api-rules.md` → RESTful API 规范

### 开发流程
- `rules/dev-flow-rules.md` → Sprint 机制、人工介入、PRD 模板

### 测试
- `rules/test-rules.md` → 测试覆盖率、边界条件

### 持久化
- `rules/persistence-rules.md` → 事务规范

### UI/UX
- `rules/ui-rules.md` → M* 组件使用规范

### 其他规范
- `rules/db-rules.md` → 数据库设计规范

---

## 核心规范要点

### 模块分层
每个服务包含 4 个模块：
- `-api` - 接口定义
- `-core` - 核心业务实现（只依赖其他服务 API）
- `-starter` - 本地调用启动器
- `-starter-remote` - 远程调用启动器

### 事务配置
| 部署方式 | 配置 | 注解 |
|---------|------|------|
| 单体 | `mango.transaction.mode = local` | @Transactional |
| 微服务 | `mango.transaction.mode = seata` | @MangoTransactional |

### 权限码格式
```
{model}:{module}:{action}
```

### AI 角色
在 `.mango/roles/` 定义角色，触发对应角色后按角色思维执行。

---

## 常用命令

```bash
# 代码生成
mvn mango:gen-module -Dname=<name>
mvn mango:gen-crud -Dmodule=<module>

# 代码检查
mvn mango:check
mvn mango:check -Drule=duplicate

# 质检评估
mvn mango:evaluate -Dartifact=prd
mvn mango:evaluate -Dartifact=code
```

---

## AI 协作流程

```
用户需求 → Planner(PRD+Sprint) → Evaluator质检 → Generator(代码+测试) → Evaluator质检 → 人类验收
```

人类介入点：
1. PRD 完成后 - 确认业务方向
2. 最终验收 - 系统 + 测试报告
