---
title: "feat: Mango - For AI Agent 的 Java SpringBoot 脚手架"
type: feat
status: draft
date: 2026-03-26
deepened: 2026-03-26
---

# Mango（芒果）- For AI Agent 的 Java SpringBoot 脚手架

---

## 第一步：Mango 定位

### 1.1 核心定位

**Mango 是一个面向 AI Agent 的 Java SpringBoot 脚手架，让 AI Agent 能够高效率地基于它实现业务需求。**

### 1.2 与现有框架的对比

| 框架 | 目标用户 | 核心方式 | AI 友好度 |
|------|---------|---------|-----------|
| PigX | 人类开发者 | 浏览器操作 → 下载代码 → 手动放置 | ❌ 不友好 |
| Ruoyi | 人类开发者 | 浏览器操作 → 下载代码 → 手动放置 | ❌ 不友好 |
| **Mango** | **AI Agent** | **一句话需求 → AI 自动实现** | **✅ 原生友好** |

### 1.3 核心价值主张

```
用户: "添加一个供应商管理模块"
        ↓
AI Agent:
1. 解析需求 → 生成符合 Mango 规范的规格说明书
2. 调用 Mango CLI 生成代码 → 直接写入目标位置
3. 调用 Mango CLI 生成权限SQL → 自动执行入库
4. 验证结果 → 完成
```

### 1.4 定位总结

- **Who**: AI Agent（Claude Code、Cursor、Copilot 等）
- **What**: Java SpringBoot 脚手架
- **How**: AI 一句话需求 → 自动化实现
- **Why**: 让 AI 能够独立完成符合规范的代码交付

---

## 第二步：面临的困难

### 2.1 AI Agent 开发的完整困难列表

#### A. AI 能力限制

| 编号 | 困难 | 影响程度 |
|------|------|---------|
| D1 | **上下文窗口限制** - AI 单次只能处理有限 tokens，大型项目无法一次性理解 | 🔴 高 |
| D2 | **长程记忆缺失** - 跨会话 AI 无法保持一致理解，规范/风格可能漂移 | 🔴 高 |
| D3 | **自我纠错能力弱** - AI 难以及时发现自己的错误，错误发现往往滞后 | 🟡 中 |
| D4 | **输出不确定性** - 同样需求可能产生不同质量的代码 | 🔴 高 |
| D5 | **规划能力有限** - 复杂任务 AI 容易迷失方向，难以分解 | 🔴 高 |

#### B. 代码质量问题

| 编号 | 困难 | 影响程度 |
|------|------|---------|
| D6 | **不抽象/复用差** - AI 容易复制粘贴而非抽象，重复代码多 | 🔴 高 |
| D7 | **方法/类过大** - 一个方法几百行，一个类几千行 | 🔴 高 |
| D8 | **命名不规范** - 方法名、变量名随意，不符合团队约定 | 🟡 中 |
| D9 | **错误处理缺失** - 无 try-catch，无异常处理 | 🔴 高 |
| D10 | **安全问题** - SQL 注入、硬编码密码/密钥等 | 🔴 高 |
| D11 | **测试覆盖不足** - 只写基本测试，边界条件忽略 | 🔴 高 |
| D12 | **JavaDoc 缺失** - 方法无注释，代码可读性差 | 🟡 中 |

#### C. 架构与设计问题

| 编号 | 困难 | 影响程度 |
|------|------|---------|
| D13 | **数据库事务处理** - 单体/微服务下事务处理方式不同，AI 难以正确处理 | 🔴 高 |
| D14 | **数据库设计随意** - 字段命名混乱，缺索引，缺外键关联 | 🟡 中 |
| D15 | **API 设计不一致** - 返回格式混乱，HTTP 状态码随意 | 🟡 中 |
| D16 | **模块边界模糊** - 模块间耦合严重，依赖关系不清晰 | 🔴 高 |
| D17 | **分布式事务** - 微服务下跨服务调用的事务一致性难以保证 | 🔴 高 |

#### D. 用户体验问题

| 编号 | 困难 | 影响程度 |
|------|------|---------|
| D18 | **UI/UX 质量差** - AI 生成的界面不美观、交互差 | 🟡 中 |
| D19 | **前端代码质量** - HTML/CSS/JS 结构混乱，不易维护 | 🟡 中 |

#### E. 测试问题

| 编号 | 困难 | 影响程度 |
|------|------|---------|
| D20 | **E2E 测试困难** - 无法进行端到端自动化测试 | 🟡 中 |
| D21 | **集成测试复杂** - 微服务间依赖 Mock 困难 | 🟡 中 |
| D22 | **测试数据准备** - AI 难以生成合理的测试数据 | 🟡 中 |

#### F. 工具与流程问题

| 编号 | 困难 | 影响程度 |
|------|------|---------|
| D23 | **规范执行难** - AI 可能忽略或不理解项目规范 | 🔴 高 |
| D24 | **工具使用障碍** - 现有脚手架（PigX/Ruoyi）需要浏览器手动操作 | 🔴 高 |
| D25 | **部署方式适配** - 单体/微服务部署方式不同，AI 难以适配 | 🔴 高 |
| D26 | **权限配置复杂** - 菜单、按钮、API 权限配置需要手动操作 | 🟡 中 |

#### G. 项目管理问题

| 编号 | 困难 | 影响程度 |
|------|------|---------|
| D27 | **Sprint 任务分解** - 复杂需求难以分解为可执行的微任务 | 🟡 中 |
| D28 | **多 Agent 协作** - 多个 Agent 同时工作可能产生冲突 | 🟡 中 |
| D29 | **人类介入时机** - 何时需要人类 Review 不明确 | 🟡 中 |

### 2.2 困难分类汇总

| 类别 | 困难数量 | 关键困难 |
|------|---------|---------|
| AI 能力限制 | 5 | D1,D2,D4,D5 |
| 代码质量 | 7 | D6,D7,D9,D10,D11 |
| 架构与设计 | 5 | D13,D16,D17 |
| 用户体验 | 2 | D18,D19 |
| 测试问题 | 3 | D20,D21 |
| 工具与流程 | 4 | D23,D24,D25 |
| 项目管理 | 3 | D27,D28,D29 |

**合计：29 个困难**

---

## 第三步：解决方案

### 3.1 解决方案总览

| 困难 | 解决方案 | 方案类别 | Mango 组件 |
|------|---------|---------|------------|
| **A. AI 能力限制** | | | |
| D1: 上下文限制 | CLAUDE.md 精炼 + 模块化上下文 | 规范体系 | CLAUDE.md |
| D2: 长程记忆缺失 | 状态文件持久化 (.mango/state.json) | 规范体系 | .mango/ |
| D3: 自我纠错弱 | Generator-Evaluator 架构 + CLI 验证 | 流程机制 | Evaluator |
| D4: 输出不确定性 | 模板强制 + 四维评估 | 工具链 | templates/ |
| D5: 规划能力有限 | Sprint 机制 + 强制任务分解 | 流程机制 | dev-flow-rules.md |
| **B. 代码质量** | | | |
| D6: 不抽象/复用差 | 重复代码检测规则 (C1) | 规范体系 | rules/code-rules.md |
| D7: 方法/类过大 | 方法/类长度限制 (C2,C3) | 规范体系 | rules/code-rules.md |
| D8: 命名不规范 | 命名规范强制 (C4) | 规范体系 | rules/naming-rules.md |
| D9: 错误处理缺失 | 错误处理规范强制 (C5) | 规范体系 | rules/code-rules.md |
| D10: 安全问题 | 安全规范扫描 (C6) | 规范体系 | rules/security-rules.md |
| D11: 测试覆盖不足 | 测试覆盖率门禁 (C8) | 规范体系 | rules/test-rules.md |
| D12: JavaDoc 缺失 | 文档规范强制 (C9) | 规范体系 | rules/code-rules.md |
| **C. 架构与设计** | | | |
| D13: 数据库事务 | @MangoMode 注解 + 事务规范 | 规范体系 | rules/persistence-rules.md |
| D14: 数据库设计 | DB 设计规范强制 (C10) | 规范体系 | rules/db-rules.md |
| D15: API 设计不一致 | API 契约规范强制 (C7) | 规范体系 | rules/api-rules.md |
| D16: 模块边界模糊 | 模块边界检查 + 接口契约 | 规范体系 | rules/module-rules.md |
| D17: 分布式事务 | Seata AT 模式 + 开关控制 | 架构设计 | mango-common/ |
| **D. 用户体验** | | | |
| D18: UI/UX 质量差 | M*组件包装 + ESLint规则 + CSS变量约束 | 规范体系 | rules/ui-rules.md, src/components/common/M*.vue |
| D19: 前端代码质量 | 组件使用强制 + 禁止原生HTML生成 | 规范体系 | rules/ui-rules.md, .eslintrc-auto.json |
| **E. 测试问题** | | | |
| D20: E2E 测试困难 | Playwright组件级截图 + 截图优先验收标准 | 工具链 | tools/playwright/, rules/ui-rules.md |
| D21: 集成测试复杂 | 微服务 Mock 规范 | 规范体系 | rules/test-rules.md |
| D22: 测试数据准备 | 测试数据生成器 | 工具链 | tools/test-data-gen/ |
| **F. 工具与流程** | | | |
| D23: 规范执行难 | AI-Executable 规范 + CLI 强制检查 | 规范体系 | rules/*.md |
| D24: 工具使用障碍 | Mango CLI 工具链 | 工具链 | tools/mango-cli |
| D25: 部署方式适配 | SPI 机制 + App POM 依赖切换（local/remote starter） | 架构设计 | 每个服务：api/core/starter/starter-remote |
| D26: 权限配置复杂 | permission-gen CLI 自动生成 | 工具链 | tools/gen/permission-gen |
| **G. 项目管理** | | | |
| D27: Sprint 任务分解 | Sprint 规范 + 任务模板 | 流程机制 | rules/dev-flow-rules.md |
| D28: 多 Agent 协作 | 状态锁 + 冲突解决机制 | 流程机制 | .mango/state.lock |
| D29: 人类介入时机 | 关键节点强制人工 Review | 流程机制 | rules/dev-flow-rules.md |

### 3.2 解决方案分类

| 方案类别 | 组件 | 解决的困难 |
|---------|------|----------|
| **规范体系** | CLAUDE.md, rules/*.md, .mango/ | D1,D2,D5,D6,D7,D8,D9,D10,D11,D12,D13,D14,D15,D16,D18,D19,D21,D23,D26,D27,D29 |
| **工具链** | tools/mango-cli, tools/gen/*, tools/check/* | D4,D6,D10,D20,D22,D24 |
| **流程机制** | Generator-Evaluator, Sprint, Review | D3,D4,D5,D27,D28,D29 |
| **架构设计** | SPI + App POM 切换, Seata, 接口契约 | D13,D16,D17,D25 |

### 3.2 规范体系

#### S1: CLAUDE.md 精炼设计

**目标**: 让 AI 在有限上下文中快速理解项目核心

**原则**:
- 总 tokens < 2000
- 包含：技术栈、模块边界、核心命令索引
- 按需加载，而非全量

**内容结构**:
```markdown
# 项目概述 (< 100 tokens)
# 技术栈 (< 200 tokens)
# 模块结构 (< 300 tokens)
# 核心规范摘要 (< 500 tokens)
# 常用命令索引 (< 500 tokens)
# AI 协作流程 (< 400 tokens)
```

#### S2: AI-Executable 规范 (rules/)

**目标**: 规范可被 AI 解析和执行，不是给人看的参考文档

**文件结构**:
```
rules/
├── dev-flow-rules.md      # 开发流程
├── module-rules.md        # 模块规范
├── api-rules.md           # API 规范
├── persistence-rules.md    # 持久化规范
└── security-rules.md      # 安全规范
```

**规范格式**:
```markdown
## MODULE-001: 命名规范
- Pattern: `mango-{domain}`
- Valid: mango-auth, mango-supplier
- Invalid: auth-service, supplier_module
- Check: `find . -name 'pom.xml' -path '*/mango-*' | xargs basename`
```

#### S3: 状态持久化

**目标**: 跨会话保持项目状态

**文件结构**:
```
.mango/
├── state.json            # 当前项目状态
├── specs/               # 规格说明历史
└── reports/            # 评估报告历史
```

### 3.3 架构设计：D25 部署方式适配（SPI + Starter 机制）

#### 核心原则

```
┌─────────────────────────────────────────────────────────────┐
│  每个服务 = api + core + starter + starter-remote              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Core 只依赖其他服务的 API，不依赖任何 Provider/Starter          │
│                                                              │
│  App 模块的 pom.xml 决定：依赖 starter 还是 starter-remote     │
│                                                              │
│  改 App 的 pom.xml 依赖，不动 Core 代码                        │
└─────────────────────────────────────────────────────────────┘
```

#### 模块结构

```
mango-order/
├── mango-order-api/                    # 接口定义
├── mango-order-core/                   # 核心业务（只注入其他服务接口）
├── mango-order-starter/               # 本地调用启动器（@Primary）
└── mango-order-starter-remote/         # 远程调用启动器（Feign）

App 模块：
├── mango-app-all/                    # 单体部署：依赖所有 starter
├── mango-app-order/                  # 订单独立：依赖其他服务的 starter-remote
└── ...
```

#### 调用关系

```
mango-order-core（业务实现）
  → 注入 InventoryService (接口)
  → 注入 PaymentService (接口)
  → 完全不知道是本地还是远程

运行时：Spring Boot 自动配置注入实现
  → 依赖 mango-inventory-starter → 注入 LocalInventoryService
  → 依赖 mango-inventory-starter-remote → 注入 InventoryRemoteClient
```

#### App 依赖示例

```xml
<!-- 单体部署：所有本地调用 -->
<dependency>mango-order-core</dependency>
<dependency>mango-inventory-starter</dependency>  <!-- 本地 -->
<dependency>mango-payment-starter</dependency>    <!-- 本地 -->

<!-- 独立部署：其他服务远程调用 -->
<dependency>mango-order-core</dependency>
<dependency>mango-inventory-starter-remote</dependency>  <!-- 远程 -->
<dependency>mango-payment-starter-remote</dependency>    <!-- 远程 -->
```

---

### 3.4 工具链

#### S5: Mango CLI 工具

**目标**: AI 可以通过 CLI 调用所有功能，无需浏览器手动操作

**工具列表**:
```bash
# 代码生成
mango gen module --name <name>           # 生成模块
mango gen crud --module <module>         # 生成 CRUD
mango gen permission --module <module>   # 生成权限SQL

# 执行
mango exec sql --file <path>            # 执行SQL

# 检查
mango check module-boundary             # 检查模块边界
mango check api-contract                # 检查 API 契约
mango check test-coverage               # 检查测试覆盖率
```

**技术选型**:
- CLI 框架: Python Click（跨平台、快速开发）
- 代码生成: Jinja2 模板
- SQL 生成: sqlglot（SQL 解析验证）

### 3.4 流程机制

#### S5: Generator-Evaluator 架构

**参考**: Anthropic "Harness Design for Long-Running Apps"

**核心思想**: 分离"生成"和"评判"，避免 AI 自我评估偏差

```
┌─────────────────────────────────────────────┐
│              Generator-Evaluator              │
├─────────────────────────────────────────────┤
│                                              │
│  Planner → 生成规格说明书 (spec.json)        │
│      ↓                                       │
│  Generator → 按 Sprint 生成代码              │
│      ↓                                       │
│  Evaluator → 运行 CLI 检查质量               │
│      ↓                                       │
│  反馈循环 → 失败则返回改进建议 (最多3次)     │
│                                              │
└─────────────────────────────────────────────┘
```

**三层 Agent 职责**:

| Agent | 输入 | 输出 |
|-------|------|------|
| Planner | 一句话需求 | 规格说明书 (spec.json) |
| Generator | spec.json | 代码文件 |
| Evaluator | 代码文件 | 评估报告 |

**Sprint 机制**:
- 任务分解为 Sprint
- 每个 Sprint 有明确的验收标准
- 失败则 Generator 收到详细反馈重试

#### S6: 四维评估标准

| 维度 | 说明 | 权重 | 阈值 |
|------|------|------|------|
| **Design Quality** | 整体架构感，模块边界清晰 | 30% | ≥7/10 |
| **Originality** | 规避模板痕迹 | 20% | ≥6/10 |
| **Craft** | 技术执行质量 | 25% | ≥7/10 |
| **Functionality** | 功能正确性 | 25% | ≥8/10 |

> 任一指标低于阈值 → Sprint 失败

---

## 第四步：复盘验证

### 4.1 困难 → 解决方案验证

#### A. AI 能力限制（5个）

| 困难 | 解决方案 | 验证结果 | 风险等级 |
|------|---------|---------|---------|
| D1: 上下文限制 | CLAUDE.md 精炼 (<2000 tokens) + 模块化加载 | ✅ 可解决 | 🟡 中 |
| D2: 长程记忆缺失 | .mango/state.json 持久化 | ✅ 可解决 | 🟡 中 |
| D3: 自我纠错弱 | Generator-Evaluator 架构 | ✅ 可解决 | 🟢 低 |
| D4: 输出不确定性 | 模板强制 + 四维评估 | ✅ 可解决 | 🟢 低 |
| D5: 规划能力有限 | Sprint 机制 + 强制任务分解 | ✅ 可解决 | 🟡 中 |

#### B. 代码质量问题（7个）

| 困难 | 解决方案 | 验证结果 | 风险等级 |
|------|---------|---------|---------|
| D6: 不抽象/复用差 | C1 重复代码检测规则 | ✅ 可解决 | 🟢 低 |
| D7: 方法/类过大 | C2/C3 长度限制规则 | ✅ 可解决 | 🟢 低 |
| D8: 命名不规范 | C4 命名规范强制 | ✅ 可解决 | 🟢 低 |
| D9: 错误处理缺失 | C5 错误处理规范 | ✅ 可解决 | 🟢 低 |
| D10: 安全问题 | C6 安全规范扫描 | ✅ 可解决 | 🟢 低 |
| D11: 测试覆盖不足 | C8 测试覆盖率门禁 | ✅ 可解决 | 🟡 中 |
| D12: JavaDoc 缺失 | C9 文档规范强制 | ✅ 可解决 | 🟡 中 |

#### C. 架构与设计问题（5个）

| 困难 | 解决方案 | 验证结果 | 风险等级 |
|------|---------|---------|---------|
| D13: 数据库事务 | @MangoMode + 事务规范 | ✅ 可解决 | 🟡 中 |
| D14: 数据库设计 | C10 DB 设计规范 | ✅ 可解决 | 🟢 低 |
| D15: API 设计不一致 | C7 API 契约规范 | ✅ 可解决 | 🟢 低 |
| D16: 模块边界模糊 | 模块边界检查 + 接口契约 | ✅ 可解决 | 🟡 中 |
| D17: 分布式事务 | Seata AT + 开关控制 | ✅ 可解决 | 🔴 高 |

#### D. 用户体验问题（2个）

| 困难 | 解决方案 | 验证结果 | 风险等级 |
|------|---------|---------|---------|
| D18: UI/UX 质量差 | M*组件包装 + CSS变量约束 + ESLint规则 | ✅ 可解决 | 🟢 低 |
| D19: 前端代码质量 | 组件使用强制 + 禁止原生HTML生成 | ✅ 可解决 | 🟢 低 |

#### E. 测试问题（3个）

| 困难 | 解决方案 | 验证结果 | 风险等级 |
|------|---------|---------|---------|
| D20: E2E 测试困难 | Playwright组件级截图 + 人类验收标准格式 | ✅ 可解决 | 🟡 中 |
| D21: 集成测试复杂 | 微服务 Mock 规范 | ⚠️ 部分解决 | 🟡 中 |
| D22: 测试数据准备 | 测试数据生成器 | ✅ 可解决 | 🟡 中 |

#### F. 工具与流程问题（4个）

| 困难 | 解决方案 | 验证结果 | 风险等级 |
|------|---------|---------|---------|
| D23: 规范执行难 | AI-Executable 规范 + CLI 检查 | ✅ 可解决 | 🟢 低 |
| D24: 工具使用障碍 | Mango CLI 工具链 | ✅ 可解决 | 🟢 低 |
| D25: 部署方式适配 | SPI 机制 + App POM 依赖切换 | ✅ 可解决 | 🟢 低 |
| D26: 权限配置复杂 | permission-gen CLI | ✅ 可解决 | 🟢 低 |

#### G. 项目管理问题（3个）

| 困难 | 解决方案 | 验证结果 | 风险等级 |
|------|---------|---------|---------|
| D27: Sprint 任务分解 | Sprint 规范 + 任务模板 | ✅ 可解决 | 🟡 中 |
| D28: 多 Agent 协作 | 状态锁 + 冲突解决机制 | ⚠️ 部分解决 | 🔴 高 |
| D29: 人类介入时机 | 关键节点强制人工 Review | ✅ 可解决 | 🟡 中 |

### 4.2 验证汇总

| 类别 | 已解决 | 部分解决 | 未解决 | 合计 |
|------|--------|---------|--------|------|
| AI 能力限制 | 5 | 0 | 0 | 5 |
| 代码质量 | 7 | 0 | 0 | 7 |
| 架构与设计 | 4 | 0 | 1 | 5 |
| 用户体验 | 2 | 0 | 0 | 2 |
| 测试问题 | 3 | 0 | 0 | 3 |
| 工具与流程 | 4 | 0 | 0 | 4 |
| 项目管理 | 2 | 1 | 0 | 3 |
| **合计** | **26** | **2** | **1** | **29** |

### 4.3 高风险项目

| 困难 | 原因 | 建议 |
|------|------|------|
| D17: 分布式事务 | Seata AT 模式有局限性，复杂场景可能失效 | 限制微服务数量，优先聚合部署 |
| D28: 多 Agent 协作 | 状态竞争、冲突解决机制复杂 | 初期限制单 Agent 模式 |

### 4.4 剩余风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| **规范编写质量** | 规范如果写不好，AI 理解会偏差 | 参考 Anthropic prompt 调优方法，持续迭代 |
| **评估标准主观性** | 四维评估可能有主观偏差 | 引入硬阈值 + 人工抽检 |
| **CLI 工具覆盖度** | 不可能覆盖所有场景 | 保留 AI 直接生成代码的能力作为补充 |

### 4.5 结论

**核心困难（26/29）已有完整解决方案，2个部分解决需要持续迭代，1个高风险需要架构约束。**

**整体方案可行，但需要注意：**
1. 分布式事务需要架构约束（优先聚合部署）
2. 多 Agent 协作需要渐进式引入

---

## 第五步：最终确认

### 5.1 Mango 定位（确认）

> **Mango 是一个面向 AI Agent 的 Java SpringBoot 脚手架，让 AI Agent 能够高效率地基于它实现业务需求。**

### 5.2 困难与解决方案总览（确认）

| 类别 | 困难数 | 解决方案 |
|------|--------|---------|
| AI 能力限制 | 5 | CLAUDE.md, 状态持久化, Generator-Evaluator |
| 代码质量 | 7 | 10 条代码规范 (C1-C10) |
| 架构与设计 | 5 | @MangoMode, Seata, 接口契约 |
| 用户体验 | 2 | M*组件包装 + ESLint + CSS变量约束 |
| 测试问题 | 3 | Playwright组件级截图 + 人类验收标准格式 |
| 工具与流程 | 4 | Mango CLI, permission-gen |
| 项目管理 | 3 | Sprint 机制, 状态锁, 人工 Review |

### 5.3 核心架构（确认）

```
mango/
├── CLAUDE.md                    # AI 核心上下文 (<2000 tokens)
├── rules/                       # AI-Executable 规范
│   ├── dev-flow-rules.md        # 开发流程规范
│   ├── code-rules.md           # 代码规范 (C1-C10)
│   ├── naming-rules.md         # 命名规范
│   ├── api-rules.md           # API 设计规范
│   ├── security-rules.md       # 安全规范
│   ├── test-rules.md          # 测试规范
│   ├── db-rules.md            # 数据库规范
│   └── ui-rules.md            # UI/UX 规范
├── tools/                       # Mango CLI 工具链
│   ├── mango-cli              # 主命令入口
│   ├── gen/                   # 代码生成
│   │   ├── module-gen.py
│   │   ├── crud-gen.py
│   │   └── permission-gen.py
│   ├── check/                 # 检查工具
│   │   ├── duplicate-check
│   │   ├── method-length-check
│   │   └── security-scan
│   └── test/                  # 测试工具
│       └── playwright-mcp
├── templates/                  # 代码模板
├── prompts/                    # AI 提示词
│   ├── requirement-expand.md
│   └── code-generate.md
├── .mango/                    # 状态持久化
│   ├── state.json
│   └── specs/
├── mango-parent/               # Maven 父项目
└── mango-common/              # 公共模块
    └── mango-common-core/     # @MangoMode 注解
```

### 5.4 AI 执行流程（确认）

```
1. 需求输入 → 一句话描述
        ↓
2. Planner: 生成规格说明书 (spec.json)
        ↓
3. Sprint 执行循环 (Generator-Evaluator):
   ├─ Generator: 生成代码 (≤200行/文件)
   ├─ Evaluator: CLI 检查
   │   ├─ check duplicate
   │   ├─ check method-length
   │   ├─ check security
   │   └─ check coverage
   └─ 反馈循环 → 最多3次重试
        ↓
4. 权限配置 → mango gen permission
        ↓
5. 最终验证 → mango check all
        ↓
6. 人工 Review → 关键节点确认
```

### 5.5 待确认事项

| 事项 | 选项 | 建议 |
|------|------|------|
| **CLI 技术栈** | Python / Java | Python（开发效率高） |
| **评估阈值** | 固定 / 可配置 | 可配置（通过 config.yaml） |
| **规范格式** | Markdown / JSON Schema | Markdown（兼顾可读性） |
| **Generator 分离** | 必须 / 可选 | 可选（保留灵活性） |
| **前端规范** | 组件库优先 / 规范约束 | 两者结合 |
| **部署策略** | 优先单体 / 优先微服务 | 优先单体，渐进拆分 |

### 5.6 下一步行动计划

| 阶段 | 任务 | 优先级 |
|------|------|--------|
| 1 | 编写完整的 rules/ 规范文件 | 🔴 高 |
| 2 | 实现 Mango CLI 核心命令 | 🔴 高 |
| 3 | 搭建 Mango 脚手架项目结构 | 🔴 高 |
| 4 | 原型验证 Generator-Evaluator 流程 | 🔴 高 |
| 5 | 实现代码检查工具 | 🟡 中 |
| 6 | 实现权限生成工具 | 🟡 中 |
| 7 | 前端组件库集成 | 🟡 中 |
| 8 | Playwright E2E 集成 | 🟡 中 |
| 9 | 迭代优化 | 🟢 低 |

---

## 第六步：制定 For AI 的代码规范

> 结合 AI 写代码的常见问题，制定针对性的代码规范

### 6.1 AI 写代码常见问题

| 问题类别 | 具体表现 | 严重程度 |
|---------|---------|---------|
| **P1: 不抽象/复用** | 重复代码多，缺乏抽象 | 🔴 高 |
| **P2: 方法/类过大** | 一个方法几百行，一个类几千行 | 🔴 高 |
| **P3: 命名不规范** | 方法名、变量名随意 | 🟡 中 |
| **P4: 错误处理缺失** | 无 try-catch，无异常处理 | 🔴 高 |
| **P5: 安全问题** | SQL注入、硬编码密码等 | 🔴 高 |
| **P6: API设计不一致** | 返回格式混乱，状态码随意 | 🟡 中 |
| **P7: 数据库设计随意** | 字段命名混乱，缺索引 | 🟡 中 |
| **P8: 注释/文档缺失** | 方法无 JavaDoc | 🟡 中 |
| **P9: 过度工程化** | 明明简单却用复杂设计模式 | 🟡 中 |
| **P10: 测试不足** | 只写基本测试，边界条件忽略 | 🔴 高 |

### 6.2 For AI 的代码规范设计原则

#### 原则 1: 强制约束 > 建议引导

| 人类规范 | For AI 规范 |
|---------|------------|
| "建议使用接口编程" | **禁止**直接依赖实现类 |
| "尽量减少重复代码" | **检测到3次以上重复，强制提取** |
| "方法不要太长" | **方法最大50行，超出则拒绝提交** |

#### 原则 2: 可执行检查 > 模糊描述

| 人类规范（模糊） | For AI 规范（可执行） |
|----------------|---------------------|
| "命名要规范" | `命名必须符合: [a-z][a-z0-9]*` |
| "异常要处理" | `所有外部调用必须包裹在 try-catch 中` |
| "要有注释" | `所有 public 方法必须有 JavaDoc` |

#### 原则 3: 模板强制 > 自由发挥

- **强制使用模板生成代码**
- **禁止偏离模板结构**
- **特殊情况需明确标注 `// @Mango: deviation`**

### 6.3 AI 代码规范清单

#### 规范 C1: 重复代码检测

```markdown
## C1: 重复代码规范

- 规则: 检测到 3 次以上重复代码块，必须提取为方法
- 检测: `mango check duplicate --threshold 3`
- 违规: 拒绝提交，要求重构
- 提取指引:
  1. 找出重复代码的共同模式
  2. 提取为私有方法 (private method)
  3. 方法命名: `extract{Action}From{Source}`
```

#### 规范 C2: 方法长度限制

```markdown
## C2: 方法长度规范

- 规则: 单个方法最大 50 行 (含空行、注释)
- 检测: `mango check method-length --max 50`
- 违规: 拒绝提交，要求拆分
- 拆分策略:
  1. 超过 50 行 → 拆分为多个私有方法
  2. 提取条件判断为卫语句
  3. 提取循环体为独立方法
```

#### 规范 C3: 类长度限制

```markdown
## C3: 类长度规范

- 规则: 单个类最大 500 行
- 检测: `mango check class-length --max 500`
- 违规: 拒绝提交，要求拆分
- 拆分策略:
  1. 超过 500 行 → 拆分为多个类
  2. 按职责拆分 (Single Responsibility)
  3. 使用组合而非继承
```

#### 规范 C4: 命名规范

```markdown
## C4: 命名规范

### Java 命名
- 类名: PascalCase, 名词, 如 `UserService`
- 方法名: camelCase, 动词, 如 `getUserById()`
- 变量名: camelCase, 名词, 如 `userName`
- 常量: UPPER_SNAKE_CASE, 如 `MAX_RETRY_COUNT`
- 包名: lowercase, 如 `com.company.module`

### 数据库命名
- 表名: snake_case, 复数, 如 `user_accounts`
- 列名: snake_case, 如 `created_at`
- 索引: `idx_{table}_{column}`, 如 `idx_users_email`

### 检测命令
`mango check naming --standard java`
```

#### 规范 C5: 错误处理规范

```markdown
## C5: 错误处理规范

- 规则: 所有外部调用必须显式处理异常
- 检测: `mango check exception-handling`

### 合规示例
```java
try {
    userRepository.save(user);
} catch (DataAccessException e) {
    log.error("保存用户失败: {}", user, e);
    throw new BusinessException("USER_SAVE_FAILED", "保存用户失败");
}
```

### 不合规示例 (违规)
```java
userRepository.save(user);  // 无异常处理
```

### 异常层次
- `BusinessException`: 业务异常，可恢复
- `SystemException`: 系统异常，不可恢复
- `ValidationException`: 参数校验异常
```

#### 规范 C6: 安全规范

```markdown
## C6: 安全规范

- 规则: 禁止硬编码敏感信息，禁止 SQL 拼接

### 禁止项
- ❌ `password = "123456"`
- ❌ `sql = "SELECT * FROM users WHERE id=" + id`
- ✅ `password = environment.get("DB_PASSWORD")`
- ✅ `sql = "SELECT * FROM users WHERE id = ?", id`

### 检测命令
`mango check security --scan`
```

#### 规范 C7: API 设计规范

```markdown
## C7: API 设计规范

### RESTful 标准
- GET: 查询，无副作用
- POST: 创建资源
- PUT: 更新资源（完整）
- PATCH: 更新资源（部分）
- DELETE: 删除资源

### 响应格式
```json
{
  "code": 0,        // 0=成功，非0=失败
  "message": "ok",  // 错误信息
  "data": {}        // 业务数据
}
```

### HTTP 状态码
- 200: 成功
- 400: 参数错误
- 401: 未认证
- 403: 无权限
- 404: 资源不存在
- 500: 服务器错误

### 检测命令
`mango check api-contract`
```

#### 规范 C8: 测试规范

```markdown
## C8: 测试规范

- 规则: 单元测试覆盖率 ≥ 80%，关键路径 100%

### 测试要求
- 所有 public 方法必须有对应测试
- 边界条件必须覆盖 (null, 空, 0, 负数, 极大值)
- 异常路径必须覆盖

### 命名规范
- 测试类: `{ClassName}Test`
- 测试方法: `test{MethodName}_{Scenario}_{Expected}`
- 示例: `testGetUserById_whenUserExists_returnUser()`

### 检测命令
`mango check test-coverage --min 80`
```

#### 规范 C9: 文档规范

```markdown
## C9: 文档规范

- 规则: 所有 public 方法必须有 JavaDoc

### JavaDoc 要求
```java
/**
 * 根据用户ID获取用户信息
 *
 * @param userId 用户ID
 * @return 用户信息，不存在返回 null
 * @throws IllegalArgumentException when userId is null
 */
public UserDTO getUserById(Long userId) { ... }
```

### 检测命令
`mango check javadoc --require-public`
```

#### 规范 C10: 数据库设计规范

```markdown
## C10: 数据库设计规范

### 表设计
- 必须有主键: `id BIGINT PRIMARY KEY AUTO_INCREMENT`
- 必须有创建/更新时间: `created_at`, `updated_at`
- 必须有逻辑删除: `deleted` (TINYINT, 默认0)
- 必须有索引: 查询字段必须有索引

### 字段规范
- 状态字段: TINYINT, 用常量类而非魔法数字
- 外键: 必须有索引
- 文本: VARCHAR(255) 起步，超长用 TEXT

### 检测命令
`mango check db-design --standard mysql`
```

### 6.4 规范检查矩阵

| 规范 | CLI 检查命令 | 阈值 | 违规处理 |
|------|-------------|------|---------|
| C1: 重复代码 | `check duplicate` | ≥3次 | 🔴 拒绝 |
| C2: 方法长度 | `check method-length` | ≤50行 | 🔴 拒绝 |
| C3: 类长度 | `check class-length` | ≤500行 | 🔴 拒绝 |
| C4: 命名规范 | `check naming` | 100%合规 | 🟡 警告 |
| C5: 错误处理 | `check exception` | 必须处理 | 🔴 拒绝 |
| C6: 安全规范 | `check security` | 0违规 | 🔴 拒绝 |
| C7: API规范 | `check api-contract` | 100%合规 | 🔴 拒绝 |
| C8: 测试覆盖 | `check coverage` | ≥80% | 🟡 警告 |
| C9: JavaDoc | `check javadoc` | public方法 | 🟡 警告 |
| C10: DB设计 | `check db-design` | 100%合规 | 🟡 警告 |

### 6.5 规范文件结构

```
mango/rules/
├── code-rules.md              # 核心代码规范
├── naming-rules.md             # 命名规范
├── api-rules.md                # API 设计规范
├── security-rules.md            # 安全规范
├── test-rules.md               # 测试规范
└── db-rules.md                 # 数据库规范
```

### 6.6 规范与 Generator-Evaluator 集成

```
Sprint 执行流程:

Generator 生成代码
        ↓
Evaluator 运行 CLI 检查
  ├─ mango check duplicate
  ├─ mango check method-length
  ├─ mango check security
  └─ mango check coverage
        ↓
如果有违规 → 返回详细错误 → Generator 重试
        ↓
如果通过 → 下一个 Sprint
```

---

### 6.7 UI/UX 一致性 + 人类验收解决方案（强化版）

> 针对 D18、D19（UI/UX质量）、D20（E2E测试）的具体实现方案

#### 6.7.1 核心问题分析

| 问题 | 根因 | 现有方案不足 |
|------|------|-------------|
| AI生成界面风格不统一 | AI自由生成HTML/CSS | 仅靠规范无法强制 |
| 人类验收时失望 | 无客观验收标准 | 仅靠截图对比不够系统 |
| E2E测试难以执行 | Playwright脚本需要人工编写 | AI难以生成可维护的测试 |

#### 6.7.2 解决方案：M* 组件包装策略

**原理**：不禁止AI生成代码，而是让正确使用组件成为**唯一简单的方式**。

```
src/components/common/          # M* 组件是AI唯一合理的选择
├── MButton.vue                 # el-button 包装
├── MInput.vue                  # el-input 包装
├── MSelect.vue                 # el-select 包装
├── MTable.vue                  # el-table 包装（预配置）
├── MForm.vue                   # el-form 包装（预配置布局）
├── MDialog.vue                 # el-dialog 包装
├── MDrawer.vue                 # el-drawer 包装
├── MTag.vue                    # el-tag 包装
└── MCard.vue                   # el-card 包装
```

**MButton.vue 示例**：
```vue
<template>
  <!-- AI 必须使用 MButton，不得使用 el-button 或原生 <button> -->
  <el-button v-bind="$attrs" :type="type" :size="size" :loading="loading">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
// Props 白名单化，防止AI添加自定义样式
defineProps<{
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'text'
  size?: 'large' | 'default' | 'small'
  loading?: boolean
}>()
</script>
<!-- 严格：禁止 <style> 块，所有定制通过 CSS 变量 -->
```

#### 6.7.3 ESLint 规则强制组件使用

```javascript
// .eslintrc.auto.json
{
  "rules": {
    // 禁止 AI 使用原生 HTML 元素代替组件
    "vue/no-div-as-component": "warn",
    // 禁止 .vue 文件中的 <style> 块（除 M* 包装组件外）
    "vue/require-use-component-styles": "error",
    // 强制组件来源只能是 @/components/common
    "no-restricted-imports": ["error", {
      "patterns": [{ "group": ["el-button", "el-input", "el-select"] }]
    }]
  }
}
```

#### 6.7.4 CSS 变量定制策略

**AI 定制样式的唯一合法方式**：

```css
/* src/styles/variables.css - AI 唯一可修改的定制点 */
:root {
  /* 颜色定制 */
  --el-color-primary: #409EFF;
  /* 圆角定制 */
  --el-border-radius-base: 4px;
  /* 间距定制 */
  --el-spacing-base: 16px;
}
```

**违规示例（AI不应生成）**：
```vue
<!-- 禁止：内联样式 -->
<el-button style="background: blue; border-radius: 20px">

<!-- 禁止：<style> 块 -->
<style>
.my-button { background: blue; }
</style>
```

#### 6.7.5 Playwright 组件级截图测试

**对比：全页面 vs 组件级测试**

| 类型 | 速度 | 隔离性 | 人类验证 | 推荐场景 |
|------|------|--------|---------|---------|
| 全页面 | 慢 | 低 | 繁琐 | 集成冒烟测试 |
| **组件级** | 快 | 高 | 快速 | **AI生成代码首选** |

**组件级测试结构**：
```
tests/components/
├── OrderTable/
│   ├── OrderTable.spec.ts        # Playwright 测试
│   └── __snapshots__/
│       ├── OrderTable-default.png    # 金色截图
│       ├── OrderTable-empty.png      # 空状态
│       └── OrderTable-loading.png     # 加载状态
```

**测试生成 + 执行流程**：
```typescript
// AI 生成组件时，同步生成测试
test('OrderTable renders correctly', async ({ mount }) => {
  const component = await mount(OrderTable, {
    props: { orders: mockOrdersData }
  });
  // 组件级截图，隔离性好
  await expect(component).toHaveScreenshot('OrderTable-default.png');
});
```

**像素差异阈值（AI生成代码场景）**：
```typescript
// playwright.config.ts
export default defineConfig({
  expect: {
    toHaveScreenshot: {
      maxDiffPixels: 150,  // AI生成代码会有渲染差异
    }
  }
});
```

#### 6.7.6 人类验收标准格式

**截图优先验收单**（AI生成，人类快速验证）：

```markdown
## [页面名] 验收标准

### 视觉检查点
- [ ] 截图: `/golden/页面名-默认状态.png`
- [ ] 截图: `/golden/页面名-数据填充.png`

### 交互测试
- [ ] 点击主按钮 → Modal 打开
- [ ] 表单填充 → 校验错误显示
- [ ] 提交有效表单 → 成功提示

### 组件使用合规
- [ ] 使用 MButton（不是 el-button 或原生 button）
- [ ] 使用 MTable（不是 el-table）
- [ ] 使用 MForm（不是 el-form）
- [ ] 无 <style> 块（除 M* 组件）
```

#### 6.7.7 更新后验证矩阵

| 困难 | 原评估 | 强化后评估 | 风险 |
|------|--------|-----------|------|
| D18: UI/UX质量差 | ⚠️ 部分解决 | ✅ 可解决 | 🟢 低 |
| D19: 前端代码质量 | ⚠️ 部分解决 | ✅ 可解决 | 🟢 低 |
| D20: E2E测试困难 | ⚠️ 部分解决 | ✅ 可解决 | 🟡 中 |

---

## 参考资料

- [Anthropic - Harness Design for Long-Running Apps](https://www.anthropic.com/engineering/harness-design-long-running-apps) ⭐ 核心参考
- [Click - Python CLI](https://click.palletsprojects.com/)
- [sqlglot - SQL Parser](https://github.com/tobymao/sqlglot)
- [Pig4Cloud](https://www.pig4cloud.com/)
- [Ruoyi](https://www.ruoyi.vip/)
