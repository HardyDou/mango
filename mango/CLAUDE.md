# Mango 脚手架 - AI 原生 Java 开发规范

## 项目概述

Mango 是一个 For AI Agent 的 Java SpringBoot 脚手架，目标让 AI Agent 高效率实现业务需求。

## 技术栈

| 组件 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus |
| 后端 | Java 17 + Spring Boot 3.x + MyBatis-Plus |
| 数据库 | MySQL |
| 缓存 | Redis |
| 注册/配置 | Nacos |

## 已实现模块

| 模块 | 说明 |
|------|------|
| `mango-parent` | Maven 父项目，统一管理依赖和插件版本 |
| `mango-common` | 公共基础组件 (R/Require/BizCode/BasePO/BaseVO/PageVO) |
| `mango-generator` | 代码生成模板 (Velocity 模板) |
| `mango-tools` | Mango Maven 插件 (gen-module/gen-crud/gen-permission/check/evaluate) |

## 模块结构

```
mango/
├── CLAUDE.md                    # 本文件 - 项目说明
├── rules/                       # AI-Executable 规范 (code/api/db/security 等)
├── .mango/roles/               # AI 角色定义
├── mango-parent/                # Maven 父项目（依赖版本管理）
├── mango-generator/             # 代码生成模板
├── mango-tools/                 # Mango CLI 工具链
│   └── mango-maven-plugin/     # Maven 插件 (gen/check/evaluate)
└── mango-common/                # 公共模块
    └── src/main/java/io/mango/common/
        ├── result/              # R, BizCode, Require
        ├── exception/           # BizException, GlobalExceptionHandler
        ├── po/                  # BasePO (请求参数基类)
        ├── vo/                  # BaseVO, PageVO (返回参数基类)
        ├── valid/               # Phone, IdCard 校验注解
        └── annotation/          # @Perm, @Log
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

### AI 角色 Subagent

使用 `/mango-evaluator`、`/mango-engineer` 等调用独立 Agent：

| Agent | 用途 | 调用方式 |
|-------|------|---------|
| mango-evaluator | 质检评估 | `/mango-evaluator` |
| mango-engineer | 生成代码 | `/mango-engineer` |
| mango-product-manager | 编写 PRD | `/mango-product-manager` |
| mango-architect | 架构设计 | `/mango-architect` |
| mango-tester | 编写测试 | `/mango-tester` |

Agent 定义在 `.mango/agents/*.md`

---

## 常用命令

```bash
# 代码生成
mvn mango:gen-module -Dname=<name>          # 生成模块脚手架
mvn mango:gen-crud -Dmodule=<module> -Dentity=<Entity> -Dtable=<table>  # 生成 CRUD
mvn mango:gen-permission -Dmodule=<module> # 生成权限菜单 SQL

# 代码检查
mvn mango:check              # 检查所有规则
mvn mango:check -Drule=duplicate  # 检查重复代码

# 质检评估
mvn mango:evaluate -Dartifact=prd   # PRD 质量评估
mvn mango:evaluate -Dartifact=code  # 代码质量评估
```

---

## AI 协作流程

```
用户需求 → Planner(PRD+Sprint) → Evaluator质检 → Generator(代码+测试) → Evaluator质检 → 人类验收
```

人类介入点：
1. PRD 完成后 - 确认业务方向
2. 最终验收 - 系统 + 测试报告
