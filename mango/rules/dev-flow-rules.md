# 开发流程规范 (dev-flow-rules)

## 1. Sprint 机制

### 1.1 Sprint 定义

- 每个 Sprint = 小且完整的可交付功能
- 不能交付一半就停止
- Sprint 周期：建议 1-2 周

### 1.2 用户故事格式

```
作为 [角色]
我想要 [功能]
以便 [价值]
```

### 1.3 分解规则

| 规则 | 说明 |
|------|------|
| 只描述 WHAT | 不描述怎么实现 |
| 完整功能 | 每个 Sprint 可独立交付 |
| MVP 优先 | 先做最小可用版本 |
| 渐进式开发 | 每次迭代完善功能 |

### 1.4 Sprint 验收

每个 Sprint 交付必须包含：
- ✅ 功能代码
- ✅ 单元测试
- ✅ E2E 截图验证
- ✅ 代码规范检查通过

### 1.5 数据库迁移规范

**DDL 变更必须通过 Flyway migration 文件，禁止直接修改生产数据库。**

#### 迁移文件命名

```
db/migration/{module}/V{version}__{description}.sql
```

**命名规范：**
- `{module}`：领域模块名（小写，如 `user`、`area`、`org`）
- `V{version}`：版本号，从 1 开始，每次变更递增
- `description`：描述性名称，使用下划线分隔

| 示例 | 说明 |
|------|------|
| `V1__init.sql` | 初始化建表 |
| `V2__add_column.sql` | 新增字段 |
| `V3__seed_data.sql` | 种子数据 |
| `V4__rename_column.sql` | 重命名字段 |
| `V5__create_index.sql` | 创建索引 |

#### 迁移文件创建规则

1. **新增不修改**：每次 DDL 变更新增一个 migration 文件，不修改历史文件
2. **版本递增**：V1 → V2 → V3，持续递增
3. **模块隔离**：每个领域模块独立子目录，禁止跨域表依赖
4. **不可逆变更**：删除表、删除列等高风险操作需在迁移步骤中明确标注

#### 本地开发

```bash
# 开发时自动执行（应用启动时）
mvn spring-boot:run

# 或手动执行迁移
mvn flyway:migrate

# 查看迁移状态
mvn flyway:info

# 清理本地数据库（慎用）
mvn flyway:clean
```

#### PRD 中的数据库设计

PRD 必须包含完整的表结构设计（对应 §5 数据库设计），格式：

```markdown
## 5. 数据库设计

### sys_user（用户表）

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | bigint | - | 是 | 主键 |
| username | varchar | 50 | 是 | 用户名 |
| tenant_id | bigint | - | 是 | 租户ID |

### 索引设计

| 索引名 | 字段 | 类型 |
|--------|------|------|
| uk_username | username | UNIQUE |
| idx_tenant_id | tenant_id | NORMAL |
```

---

## 2. 人工介入时机

### 2.1 强制人工 Review 点

| 时机 | 触发条件 | 等待动作 |
|------|---------|---------|
| PRD 完成后 | 规格说明书初稿完成 | 人类确认业务方向 |
| 最终验收 | 代码 + 测试报告完成 | 人类最终验收 |

### 2.2 人类介入流程

```
1. Planner 生成 PRD
2. ↓
3. 人类 Review：确认业务方向 ⏸️
4. ↓ (确认后)
5. Generator 生成代码
6. ↓
7. Evaluator 质检
8. ↓ (通过后)
9. 人类最终验收 ⏸️
10. ↓ (验收后)
11. 完成
```

---

## 3. PRD 模板

每个 PRD 必须包含以下章节：

| 章节 | 内容 | 必填 |
|------|------|------|
| 1. 用户故事 | "作为...我想要...以便..." | ✅ |
| 2. 功能描述 | 具体要做什么 | ✅ |
| 3. 字段设计 | 字段名、类型、长度、必填、校验规则 | ✅ |
| 4. API 设计 | 请求/响应格式、状态码 | ✅ |
| 5. 数据库设计 | 表结构、索引 | ✅ |
| 6. UI/UX 规范 | 组件引用、规范引用、原型链接 | ✅ |
| 7. 业务流程 | 正常流程、异常流程 | ✅ |
| 8. 边界情况 | 空值、最大长度、特殊字符 | ✅ |
| 9. 非功能需求 | 性能、安全、兼容性 | ❌ |

### 3.1 字段设计示例

```markdown
### 字段设计：用户注册
| 字段 | 类型 | 长度 | 必填 | 校验规则 |
|------|------|------|------|---------|
| username | String | 3-20 | 是 | 字母数字下划线 |
| password | String | 6-20 | 是 | 加密存储 |
| mobile | String | 11 | 否 | 手机号格式 |
```

### 3.2 UI/UX 规范结构

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

---

## 4. 上下文管理 (SESSION_CONSENSUS)

### 4.1 触发条件

满足以下任一条件时，AI 必须执行 summarize：
1. 用户明确说"总结"
2. 对话超过 30 分钟
3. 上下文超过 70%
4. 完成阶段性产出（PRD/代码/测试）

### 4.2 执行流程

1. 生成 `.mango/sessions/consensus/{timestamp}_consensus.md`
2. 识别需要下沉的规范，下沉到 `rules/*.md`
3. 清理上下文

### 4.3 存储位置

`.mango/sessions/consensus/`

### 4.4 内容格式

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

### 4.5 AI 行为规则

| 条件 | AI 行为 |
|------|---------|
| 超过 60% | 提示用户"上下文快满了" |
| 超过 80% | 强制建议总结 |
| 超过 90% | 必须总结后才能继续 |

---

## 5. AI 角色预设

### 5.1 触发机制

在 `CLAUDE.md` 中内嵌角色索引，AI 自动识别用户意图触发对应角色：

```markdown
## 角色预设

当用户意图匹配时，自动激活对应角色：

| 触发词/意图 | 角色 | 文件 |
|------------|------|------|
| "写PRD"、"分析需求" | 产品经理 | .mango/roles/product-manager.md |
| "架构设计"、"技术方案" | 架构师 | .mango/roles/architect.md |
| "写代码"、"实现功能" | 研发工程师 | .mango/roles/engineer.md |
| "测试用例"、"怎么测试" | 测试工程师 | .mango/roles/tester.md |
| "项目计划"、"排期" | 项目经理 | .mango/roles/project-manager.md |
```

### 5.2 角色文件结构

```
.mango/roles/
├── product-manager.md    # 产品经理角色
├── architect.md          # 架构师角色
├── engineer.md          # 研发工程师角色
├── tester.md            # 测试工程师角色
├── project-manager.md   # 项目经理角色
└── evaluator.md         # 质检员角色
```

### 5.3 三执行器架构

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

### 5.4 Evaluator 质检员角色

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
|      | rules/ui-rules.md（UI/UX） |
|      | rules/api-rules.md（API设计） |
|      | rules/db-rules.md（数据库设计） |
| 代码 | rules/code-rules.md |
|      | rules/naming-rules.md |
|      | rules/security-rules.md |
|      | rules/api-rules.md |
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
