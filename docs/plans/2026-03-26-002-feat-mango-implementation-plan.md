---
title: "feat: Mango 脚手架 - 分阶段实施计划"
type: feat
status: active
date: 2026-03-26
origin: docs/plans/2026-03-26-001-feat-aiagent-springboot-scaffold-plan.md
---

# Mango 脚手架 - 分阶段实施计划

## 概述

基于已确认的 Mango 设计，制定分阶段实施计划。技术栈已确认：
- **前端**：Vue 3 + Element Plus
- **后端**：Java + Spring Boot + Spring Cloud Alibaba (Pig 生态)

---

## 已确认技术决策

| 决策 | 确认内容 |
|------|---------|
| 前端框架 | Vue 3 + Element Plus |
| 后端框架 | Java + Spring Boot + Spring Cloud Alibaba |
| 脚手架定位 | For AI Agent（AI 原生友好） |
| 困难数量 | 29 个（已识别） |
| 解决方案 | 26 已解决 / 2 部分 / 1 高风险 |
| 代码规范 | Alibaba P3C + SonarQube（替代 C1-C10 自建规则） |
| UI/UX 方案 | M* 组件包装 + ESLint + CSS 变量 |
| E2E 测试 | Playwright 组件级截图 + 验收标准 |

---

## 已确认决策

| 决策 | 确认内容 |
|------|---------|
| 代码检查工具 | **SonarQube + Alibaba P3C** ✅ 已改 |
| 评估阈值 | SonarQube 内置阈值（可配置） |
| 规范格式 | Markdown |
| Generator 分离 | 可选（保留灵活性） |
| 前端规范 | 两者结合 |
| 部署策略 | 优先单体，渐进拆分 |
| **模块命名** | `api` / `core` / `starter` / `starter-remote` |

---

## 架构设计：SPI + Starter 机制

### 模块命名规则

```
mango-order/
├── mango-order-api/                    # 接口定义
├── mango-order-core/                   # 核心业务（只注入其他服务接口）
├── mango-order-starter/               # 本地调用启动器（@Primary）
└── mango-order-starter-remote/        # 远程调用启动器（Feign）
```

### 核心原则

```
┌─────────────────────────────────────────────────────────────┐
│  Core 只依赖其他服务的 API，不依赖任何 Provider/Starter           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  App 模块的 pom.xml 决定：依赖 starter 还是 starter-remote     │
│                                                              │
│  改 App 的 pom.xml 依赖，不动 Core 代码                        │
└─────────────────────────────────────────────────────────────┘
```

### 调用关系

```
mango-order-core（业务实现）
  → 注入 InventoryService (接口)
  → 注入 PaymentService (接口)
  → 完全不知道是本地还是远程

运行时：Spring Boot 自动配置注入实现
  → 依赖 mango-inventory-starter → 注入 LocalInventoryService
  → 依赖 mango-inventory-starter-remote → 注入 InventoryRemoteClient
```

### App 依赖示例

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

### 部署场景

| 场景 | App | 依赖 |
|------|-----|------|
| 单体 | mango-app-all | 所有 core + 所有 starter |
| 订单独立 | mango-app-order | order-core + inventory-starter-remote + payment-starter-remote |
| 库存独立 | mango-app-inventory | inventory-core + order-starter-remote + payment-starter-remote |
| 支付独立 | mango-app-payment | payment-core + order-starter-remote + inventory-starter-remote |

---

## 实施阶段

### 阶段 1：基础规范体系建设（优先级 🔴 高）

**目标**：建立 For AI 的规范体系，是所有后续工作的基础

#### 1.1 创建 rules/ 规范文件

| 文件 | 内容 | 优先级 |
|------|------|--------|
| `rules/dev-flow-rules.md` | Sprint 机制、任务分解、人工 Review 时机 | 🔴 高 |
| `rules/code-rules.md` | AI-Executable 规范（参考 Alibaba P3C，差异化补充） | 🔴 高 |
| `rules/naming-rules.md` | Java/Database 命名规范 | 🔴 高 |
| `rules/api-rules.md` | RESTful API 设计规范 | 🔴 高 |
| `rules/security-rules.md` | 安全规范（无硬编码、SQL 安全） | 🔴 高 |
| `rules/test-rules.md` | 测试覆盖率、边界条件 | 🔴 高 |
| `rules/db-rules.md` | 数据库设计规范 | 🟡 中 |
| `rules/persistence-rules.md` | @MangoMode + 事务处理规范 | 🟡 中 |
| `rules/module-rules.md` | **模块分层 + SPI 机制 + Starter 命名规则** | 🔴 高 |
| `rules/ui-rules.md` | M* 组件使用规则、ESLint 配置 | 🟡 中 |

#### 1.2 搭建 SonarQube + Alibaba P3C 检查体系

| 任务 | 说明 | 优先级 |
|------|------|--------|
| 搭建 SonarQube 环境 | Docker 部署 SonarQube + 安装 `sonar-p3c-plugin` | 🔴 高 |
| 配置 `mango-parent/pom.xml` | 集成 `sonar-maven-plugin`，配置项目质量门禁 | 🔴 高 |
| 编写 `tools/sonar/p3c-rules.xml` | Alibaba P3C 补充规则（如有需差异化） | 🟡 中 |
| 编写 `skills/mango-evaluator/SKILL.md` | Evaluator Agent 读取 SonarQube 报告进行质量评估 | 🔴 高 |

#### 1.3 rules/module-rules.md 规范内容（实施时创建）

```markdown
# 模块分层规范

## MODULE-001: 服务模块分层

每个服务必须包含以下 4 个模块：

| 后缀 | 说明 | 示例 |
|------|------|------|
| `-api` | 接口定义 | `mango-order-api` |
| `-core` | 核心业务实现 | `mango-order-core` |
| `-starter` | 本地调用启动器 | `mango-order-starter` |
| `-starter-remote` | 远程调用启动器 | `mango-order-starter-remote` |

## MODULE-002: Core 依赖原则

**Core 只依赖其他服务的 API，不依赖任何 Provider/Starter**

✅ 正确：
```xml
<dependency>
    <groupId>com.mango</groupId>
    <artifactId>mango-inventory-api</artifactId>  <!-- 只依赖 API -->
</dependency>
```

❌ 错误：
```xml
<dependency>
    <groupId>com.mango</groupId>
    <artifactId>mango-inventory-starter</artifactId>  <!-- 禁止依赖 Starter! -->
</dependency>
```

## MODULE-003: 部署规则

App 模块的 pom.xml 决定依赖 starter 还是 starter-remote

| 场景 | 依赖 |
|------|------|
| 单体部署 | 所有 starter |
| 独立部署 | 对应服务的 starter-remote |

## MODULE-004: 调用透明性

Core 层代码不知道被调用时是本地还是远程
```

#### 1.4 创建 CLAUDE.md 模板

```
mango/CLAUDE.md
- 项目概述 (< 100 tokens)
- 技术栈 (< 200 tokens)
- 模块结构 + 规范索引 (< 300 tokens)  ← 包含 rules/ 路径
- 核心规范摘要 (< 500 tokens)          ← 包含 module-rules.md 要点
- 常用命令索引 (< 500 tokens)
- AI 协作流程 (< 400 tokens)
```

**CLAUDE.md 必须包含的规范索引**：
```markdown
## 规范文件索引

### 模块分层规范（必读）
- rules/module-rules.md  → 服务模块分层、Starter 机制

### 代码规范（必读）
- rules/code-rules.md    → C1-C10 代码规范

### 其他规范
- rules/naming-rules.md
- rules/api-rules.md
- rules/security-rules.md
- ...

### AI 协作规范
- .mango/sessions/       → 会话共识记录
```

---

### D2: SESSION_CONSENSUS 机制（已确认规范）

```markdown
## 上下文管理

### 触发条件（AI 必须遵守）

满足以下任一条件时，AI 必须执行 summarize：
1. 用户明确说"总结"
2. 对话超过 30 分钟
3. 上下文超过 70%
4. 完成阶段性产出（PRD/代码/测试）

### 执行流程
1. 生成 .mango/sessions/consensus/{timestamp}_consensus.md
2. 识别需要下沉的规范，下沉到 rules/*.md
3. 清理上下文

### 存储位置
.mango/sessions/consensus/

### 内容格式
```markdown
# SESSION CONSENSUS
**时间**: {timestamp}

## 本次共识
- [共识内容]

## 下沉识别
- [x] 需要下沉 → rules/xxx.md
- [ ] 不需要下沉

## 待处理
- [待确认问题]
```

### AI 行为规则

| 条件 | AI 行为 |
|------|---------|
| 超过 60% | 提示用户"上下文快满了" |
| 超过 80% | 强制建议总结 |
| 超过 90% | 必须总结后才能继续 |
```

---

### D3: Generator-Evaluator 机制（已确认规范）

```markdown
## 代码质量检查

### 架构
Generator（AI写代码）→ Evaluator（静态检测）→ 通过/失败

### Evaluator 工具
| 用途 | 工具 |
|------|------|
| 代码规范 | Alibaba P3C |
| 重复代码 | PMD CPD |
| Bug检测 | SpotBugs（可选） |

### 触发流程
```
Generator 生成代码
  ↓
CLI 调用: mango check
  ↓
静态检测工具运行（Alibaba P3C + PMD CPD）
  ↓
  ├─ 失败 → 返回错误详情 → Generator 重试（最多3次）
  └─ 通过 → 输出
```

### Mango CLI 包装
```bash
mango check          # 运行所有检查
mango check pmd     # 只运行 P3C 规范检查
mango check cpd     # 只运行重复代码检查
mango check security # 安全检查
```

### 下沉位置
- rules/code-rules.md → 检查规则配置
- tools/mango-cli/    → CLI 实现
```

---

### D5: 规划能力有限 - Sprint 机制（已确认规范）

```markdown
## 规划规范

### 分解规则
| 规则 | 说明 |
|------|------|
| 用户故事格式 | "作为...我想要...以便..." |
| 只描述 WHAT | 不描述怎么实现 |
| 完整功能 | 每个 Sprint 可独立交付 |
| MVP 优先 | 先做最小可用版本 |
| 渐进式开发 | 每次迭代完善功能 |

### Sprint 定义
- 每个 Sprint = 小且完整的可交付功能
- 不能交付一半就停止
- 例：用户登录 = 输入用户名 + 输入密码 + 提交 + 验证 + 成功跳转 + 失败提示（一起交付）

---

### PRD 规范（产品规格说明书）

每个 PRD 必须包含：

| 章节 | 内容 |
|------|------|
| **1. 用户故事** | "作为...我想要...以便..." |
| **2. 功能描述** | 具体要做什么 |
| **3. 字段设计** | 字段名、类型、长度、必填、校验规则 |
| **4. API 设计** | 请求/响应格式、状态码 |
| **5. 数据库设计** | 表结构、索引 |
| **6. UI/UX 规范** | 组件引用、规范引用、原型链接 |
| **7. 业务流程** | 正常流程、异常流程 |
| **8. 边界情况** | 空值、最大长度、特殊字符 |
| **9. 非功能需求** | 性能、安全、兼容性 |

### 字段设计示例
```markdown
### 字段设计：用户注册
| 字段 | 类型 | 长度 | 必填 | 校验规则 |
|------|------|------|------|---------|
| username | String | 3-20 | 是 | 字母数字下划线 |
| password | String | 6-20 | 是 | 加密存储 |
| mobile | String | 11 | 否 | 手机号格式 |
```

### PRD UI/UX 规范结构（已确认）

PRD 中 UI/UX 分为**全局风格规范** + **模块特定规范**：

```markdown
## 3. 统一 UI/UX 风格规范（全局）

### 3.1 设计系统
- 主色调：#409EFF
- 边框圆角：4px
- 间距基准：16px

### 3.2 通用组件规范
- 表单 → MForm
- 按钮 → MButton
- 提示 → MTag（danger/success/warning）
- 列表 → MTable
- 弹窗 → MDialog

### 3.3 通用交互规范
- loading 状态：MButton 加 loading
- 空状态：显示空状态插图
- 错误状态：MDialog 弹窗提示
- 成功状态：Toast 轻提示

---

## 4. 功能模块：用户注册

### 4.1 功能描述

### 4.2 UI/UX（引用全局规范 + 模块特定）

| 字段 | 组件 | 约束 |
|------|------|------|
| 用户名 | MInput | 必填，错误提示用 MTag(danger) |
| 密码 | MInput | type="password" |
| 提交 | MButton | type="primary" |

**交互**：验证失败 → MTag(danger) 显示错误信息

**验收**：
- /golden/用户注册-default.png
- /golden/用户注册-error.png
```

### 下沉位置
- `rules/dev-flow-rules.md` → PRD 模板（含 UI/UX 规范）
- `rules/ui-rules.md` → 详细 UI/UX 规范
- `prd/原型/` → 验收截图目录

### 下沉位置
- rules/dev-flow-rules.md → Sprint 分解规范 + PRD 模板
- rules/ui-rules.md → UI/UX 规范
```

---

### D6: 不抽象/复用差 - 角色预设机制（已确认规范）

```markdown
## AI 角色预设

### 触发机制

在 `CLAUDE.md` 中内嵌角色索引，AI 自动识别用户意图触发对应角色：

```markdown
## 角色预设

当用户意图匹配时，自动激活对应角色：

| 触发词/意图 | 角色 | 文件 |
|------------|------|------|
| "写PRD"、"分析需求"、"用户故事" | 产品经理 | .mango/roles/product-manager.md |
| "架构设计"、"技术方案"、"怎么设计" | 架构师 | .mango/roles/architect.md |
| "写代码"、"实现功能"、"写接口" | 研发工程师 | .mango/roles/engineer.md |
| "测试用例"、"怎么测试"、"边界条件" | 测试工程师 | .mango/roles/tester.md |
| "项目计划"、"排期"、"里程碑" | 项目经理 | .mango/roles/project-manager.md |

激活角色后：
1. 读取对应角色文件
2. 按角色思维执行
3. 产出符合角色定位的成果
```

### 角色文件结构

```
.mango/roles/
├── product-manager.md    # 产品经理角色
├── architect.md          # 架构师角色
├── engineer.md          # 研发工程师角色
├── tester.md            # 测试工程师角色
└── project-manager.md   # 项目经理角色
```

### 角色文件示例：architect.md

```markdown
# 架构师角色

## 思维模式
- 先抽象后实现
- 关注扩展性、复用性、性能
- 优先复用现有模块

## 执行流程
1. 理解需求 → 2. 设计模块结构 → 3. 定义接口 → 4. 评审方案

## 输出格式
- 模块拆分方案
- 接口定义
- 技术决策点

## 复用检查清单
- [ ] 能否复用现有模块？
- [ ] 抽象是否过度？
- [ ] 接口是否稳定？
```

### 角色文件示例：engineer.md

```markdown
# 研发工程师角色

## 思维模式
- 遵循规范，代码即文档
- 方法单一职责，类高内聚
- 写可测试的代码

## 执行流程
1. 理解需求 → 2. 查看规范 → 3. 实现代码 → 4. 自测

## 代码规范要点
- 方法长度 ≤ 50 行
- 类长度 ≤ 500 行
- 重复代码率 ≤ 3%
- 异常必须捕获处理

## 下沉位置
- .mango/roles/engineer.md
```

### 下沉位置
- `mango/CLAUDE.md` → 角色触发索引
- `.mango/roles/` → 角色定义文件
```

---

### 三执行器架构（已确认）

Mango 核心架构由三个执行器组成：

```
┌─────────────────────────────────────────────┐
│ 执行器（我们定义的）                           │
├─────────────────────────────────────────────┤
│                                             │
│ Planner（调度器）                            │
│ └── 内置角色能力                            │
│     ├── product-manager → PRD、原型         │
│     ├── architect → 技术方案、模块拆分       │
│     ├── engineer → 代码实现                  │
│     ├── tester → 测试用例                   │
│     └── project-manager → Sprint、任务分解  │
│                                             │
│ Generator（执行器）                          │
│ └── 内置角色能力                            │
│     ├── engineer → 生成代码                 │
│     └── tester → 生成测试                   │
│                                             │
│ Evaluator（质检员）                         │
│ └── 客观找问题、按规则判断                   │
│                                             │
└─────────────────────────────────────────────┘
```

### Evaluator 质检员角色

```markdown
# 质检员（Evaluator）

## 身份

一个独立的、客观的第三方质检员。

## 职责

检查产出物是否符合规范，找出问题。

---

## 产出物 → 规范映射

| 产出物 | 遵从规范 |
|--------|---------|
| PRD | rules/dev-flow-rules.md（PRD格式） |
| | rules/ui-rules.md（UI/UX） |
| | rules/api-rules.md（API设计） |
| | rules/db-rules.md（数据库设计） |
| 代码 | rules/code-rules.md |
| | rules/naming-rules.md |
| | rules/security-rules.md |
| | rules/api-rules.md |
| 测试 | rules/test-rules.md |

---

## 工作流程

```
1. 识别产出物类型（PRD/代码/测试）
2. 加载对应规范（见上表）
3. 按规范逐项检查
4. 输出问题列表
```

---

## 输出格式

```markdown
## 质检报告

**产出物**: PRD
**遵从规范**: rules/dev-flow-rules.md, rules/ui-rules.md
**结果**: ❌ 不通过

### 问题列表

| # | 问题 | 规则依据 | 严重程度 |
|---|------|---------|---------|
| 1 | 用户故事格式不符合 | rules/dev-flow-rules.md | 🔴 高 |
| 2 | 缺少字段设计 | rules/dev-flow-rules.md | 🔴 高 |

### 建议

请 Planner 按规则修复
```

---

## 下沉位置

- `.mango/roles/evaluator.md` → 质检员角色定义
```

---

### 阶段 2：Mango Maven 插件开发（优先级 🔴 高）

**目标**：让 AI 可以通过 Maven 命令完成代码生成、权限配置、代码检查

#### 2.1 Mango Maven 插件命令

```bash
# 代码生成
mvn mango:gen-module -Dname=<name>                    # 生成模块
mvn mango:gen-crud -Dmodule=<module>                  # 生成 CRUD
mvn mango:gen-permission -Dmodule=<module>            # 生成权限SQL

# 执行
mvn mango:exec-sql -Dfile=<path>                     # 执行SQL

# 检查
mvn mango:check -Drule=duplicate -Dthreshold=3         # 重复代码检测
mvn mango:check -Drule=method-length -Dmax=50         # 方法长度检测
mvn mango:check -Drule=class-length -Dmax=500         # 类长度检测
mvn mango:check -Drule=naming -Dstandard=java         # 命名检测
mvn mango:check -Drule=exception-handling              # 异常处理检测
mvn mango:check -Drule=security                       # 安全扫描
mvn mango:check -Drule=api-contract                    # API 契约检测
mvn mango:check -Drule=test-coverage -Dmin=80         # 测试覆盖率
mvn mango:check -Drule=db-design -Dstandard=mysql    # 数据库设计
mvn mango:check -Drule=component-usage               # 组件使用合规

# 质检评估
mvn mango:evaluate -Dartifact=prd                     # 评估 PRD
mvn mango:evaluate -Dartifact=code                    # 评估代码
mvn mango:evaluate -Dartifact=test                    # 评估测试
```

#### 2.2 技术选型

| 组件 | 选择 | 理由 |
|------|------|------|
| CLI 框架 | **Java (Picocli)** | 统一 Java 技术栈 |
| 代码生成 | Java 模板引擎 | 灵活、可扩展 |
| 技术栈 | 统一 Java | 减少维护复杂度 |

#### 2.3 权限码规范（RBAC）

```markdown
## 权限码规范

### 格式
```
{model}:{module}:{action}
```

### 标准操作码

| 操作 | 代码 | 说明 |
|------|------|------|
| 列表 | list | 查询列表 |
| 详情 | view | 查看单条 |
| 新增 | add | 新增 |
| 修改 | edit | 修改 |
| 删除 | delete | 删除 |
| 提交 | submit | 提交申请 |
| 审批 | approve | 审批通过 |
| 驳回 | reject | 审批驳回 |
| 取消 | cancel | 取消操作 |
| 导出 | export | 导出数据 |
| 导入 | import | 导入数据 |
| 启用 | enable | 启用 |
| 禁用 | disable | 禁用 |

### 权限 SQL 模板

```sql
-- 菜单
INSERT INTO sys_menu (name, code, path, component)
VALUES ('订单管理', 'order:order', '/order', 'order/index');

-- 按钮权限
INSERT INTO sys_menu (name, code, perms, parent_id)
VALUES ('查看', 'order:order:view', 'order:order:view', @parent_id);

-- API 权限
INSERT INTO sys_permission (name, code, path, method)
VALUES ('查询订单', 'order:order:list', '/api/order', 'GET');
```

#### 2.4 工具结构

```
mango-tools/
├── mango-maven-plugin/    # Maven 插件
│   ├── src/main/java/
│   │   ├── MangoGenCrudMojo.java
│   │   ├── MangoGenPermissionMojo.java
│   │   └── MangoCheckMojo.java
│   └── pom.xml
└── templates/              # 代码模板
    ├── crud/              # CRUD 模板
    │   ├── Controller.java
    │   ├── Service.java
    │   ├── Mapper.java
    │   └── Vue.vue
    └── permission/         # 权限 SQL 模板
        └── menu.sql
```

---

### 阶段 3：Mango 脚手架项目结构（优先级 🔴 高）

**目标**：搭建完整的脚手架项目骨架

```
mango/
├── CLAUDE.md                    # AI 核心上下文
├── rules/                       # AI-Executable 规范
│   ├── dev-flow-rules.md
│   ├── code-rules.md
│   ├── naming-rules.md
│   ├── api-rules.md
│   ├── security-rules.md
│   ├── test-rules.md
│   ├── db-rules.md
│   ├── persistence-rules.md
│   ├── module-rules.md
│   └── ui-rules.md
├── tools/                       # Mango CLI 工具链
│   ├── mango-cli/              # 主入口
│   │   ├── cli.py
│   │   ├── commands/
│   │   │   ├── gen.py
│   │   │   ├── check.py
│   │   │   └── exec.py
│   │   └── utils/
│   ├── templates/              # 代码模板
│   │   ├── module/
│   │   ├── crud/
│   │   └── permission/
│   └── check/                  # 检查工具
│       ├── duplicate_check.py
│       ├── method_length_check.py
│       ├── security_scan.py
│       └── component_usage_check.py
├── mango-parent/               # Maven 父项目
│   └── pom.xml
├── mango-common/               # 公共模块
│   └── mango-common-core/
│       └── src/main/java/
│           └── com/mango/common/
│               └── annotation/
│                   └── MangoMode.java
├── mango-generator/            # 代码生成器
│   └── pom.xml
├── src/                       # 前端源码
│   ├── components/            # M* 组件库
│   │   └── common/
│   │       ├── MButton.vue
│   │       ├── MInput.vue
│   │       ├── MSelect.vue
│   │       ├── MTable.vue
│   │       ├── MForm.vue
│   │       ├── MDialog.vue
│   │       ├── MDrawer.vue
│   │       ├── MTag.vue
│   │       └── MCard.vue
│   ├── styles/
│   │   └── variables.css     # CSS 变量定制点
│   └── tests/
│       └── components/
│           └── __snapshots__/ # 金色截图
└── .mango/                   # 状态持久化
    ├── state.json
    ├── specs/
    └── reports/
```

---

### 阶段 4：Generator-Evaluator 原型验证（优先级 🔴 高）

**目标**：验证 Planner → Generator → Evaluator 循环的可行性

#### 4.1 三层 Agent 职责

| Agent | 输入 | 输出 |
|-------|------|------|
| Planner | 一句话需求 | 规格说明书 (spec.json) |
| Generator | spec.json | 代码文件 |
| Evaluator | 代码文件 | 评估报告 |

#### 4.2 Sprint 机制

```
每个 Sprint:
1. Generator 生成代码（≤200行/文件）
2. Evaluator 运行 CLI 检查
3. 如果违规 → 返回详细错误 → Generator 重试（最多3次）
4. 如果通过 → 下一个 Sprint
```

#### 4.3 四维评估标准

| 维度 | 权重 | 阈值 |
|------|------|------|
| Design Quality | 30% | ≥7/10 |
| Originality | 20% | ≥6/10 |
| Craft | 25% | ≥7/10 |
| Functionality | 25% | ≥8/10 |

---

### 阶段 5：M* 组件库开发（优先级 🟡 中）

**目标**：建立 AI 友好的前端组件使用体系

#### 5.1 组件列表

| 组件 | 包装 | 说明 |
|------|------|------|
| MButton | el-button | Props 白名单化 |
| MInput | el-input | 预配置校验 |
| MSelect | el-select | 支持远程搜索 |
| MTable | el-table | 预配置分页、排序 |
| MForm | el-form | 预配置布局规则 |
| MDialog | el-dialog | 预配置 footer |
| MDrawer | el-drawer | 预配置 footer |
| MTag | el-tag | Props 白名单化 |
| MCard | el-card | 标准化卡片 |

#### 5.2 ESLint 规则

```javascript
{
  "rules": {
    "vue/no-div-as-component": "warn",
    "vue/require-use-component-styles": "error",
    "no-restricted-imports": ["error", {
      "patterns": [{ "group": ["el-button", "el-input", "el-select"] }]
    }]
  }
}
```

---

### 阶段 6：Playwright E2E 集成（优先级 🟡 中）

**目标**：建立 AI 可执行、人类可验证的测试体系

#### 6.1 组件级截图测试

```typescript
test('OrderTable renders correctly', async ({ mount }) => {
  const component = await mount(OrderTable, {
    props: { orders: mockOrdersData }
  });
  await expect(component).toHaveScreenshot('OrderTable-default.png');
});
```

#### 6.2 像素差异阈值

```typescript
// playwright.config.ts
export default defineConfig({
  expect: {
    toHaveScreenshot: {
      maxDiffPixels: 150,  // AI 生成代码容忍度
    }
  }
});
```

#### 6.3 人类验收标准格式

```markdown
## [页面名] 验收标准

### 视觉检查点
- [ ] 截图: `/golden/页面名-默认状态.png`

### 交互测试
- [ ] 点击主按钮 → Modal 打开

### 组件使用合规
- [ ] 使用 MButton（不是 el-button）
- [ ] 无 <style> 块（除 M* 组件）
```

---

### 阶段 7：高风险项处理（优先级 ⚠️）

#### 7.1 D13/D17: 分布式事务（已确认方案）

**核心原则**：优先聚合部署，减少跨服务调用

---

### 事务配置切换

| 部署方式 | 配置 | 注解 |
|---------|------|------|
| 单体/聚合部署 | `mango.transaction.mode = local` | @Transactional |
| 微服务部署 | `mango.transaction.mode = seata` | @MangoTransactional |

---

### 自定义注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MangoTransactional {
}
```

### AOP 切面实现

```java
@Aspect
@Component
public class MangoTransactionAspect {

    @Around("@annotation(MangoTransactional)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String mode = getTransactionMode();  // 读取配置

        if ("seata".equals(mode)) {
            return handleGlobalTransaction(point);   // Seata 模式
        } else {
            return handleLocalTransaction(point);    // 本地模式
        }
    }
}
```

### Seata AT 原理

| 特性 | 说明 |
|------|------|
| 分支 commit | 每个分支都 commit，但通过 undo_log 可回滚 |
| 全局协调 | TC 协调所有分支，要么一起提交，要么一起回滚 |
| 性能 | 聚合部署性能更好（减少跨服务调用） |

### 下沉位置

- `rules/persistence-rules.md` → 事务规范
- `mango-common/mango-common-core/` → @MangoTransactional 注解 + AOP 实现

#### 7.2 D28: 多 Agent 协作（🟡 中风险）

**策略**：渐进式引入

- 阶段 1-4：单 Agent 模式
- 阶段 5+：引入状态锁机制
- 状态锁文件：`.mango/state.lock`

---

## 实施优先级总结

| 阶段 | 任务 | 优先级 | 状态 |
|------|------|--------|------|
| **1** | rules/ 规范文件 | 🔴 高 | ✅ 完成 |
| **2** | SonarQube + Alibaba P3C 集成 | 🔴 高 | 🔄 进行中 |
| **3** | 脚手架项目结构（mango-parent/common/generator/tools） | 🔴 高 | ✅ 完成 |
| **4** | Generator-Evaluator 原型（Maven Plugin + Agent Skill） | 🔴 高 | ✅ 完成 |
| **5** | M* 组件库（mango-web Vue3 管理后台） | 🟡 中 | ✅ 完成 |
| **6** | Playwright E2E 集成（mango-web） | 🟡 中 | ✅ 完成 |
| **7** | Evaluator Agent Skill 完善（读取 SonarQube 报告） | 🔴 高 | 🔄 进行中 |
| **8** | 高风险项处理（D17 分布式事务/D28 多Agent） | ⚠️ | ⏸ 搁置 |

---

## 下一步行动

**需要你确认以下事项后开始实施：**

1. **CLI 技术栈**：Python Click ✅ 建议 vs Java ❓
2. **评估阈值**：可配置 ✅ 建议 vs 固定 ❓
3. **其他选项**（可接受建议值直接开始）

---

## 相关文档

- **设计文档**：`docs/plans/2026-03-26-001-feat-aiagent-springboot-scaffold-plan.md`
- **技术栈**：Vue 3 + Element Plus（前端）/ Java + Spring Boot + Spring Cloud Alibaba（后端）
