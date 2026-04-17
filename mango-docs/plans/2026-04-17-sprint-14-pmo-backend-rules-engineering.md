# Sprint 14: PMO 后端规则工程化

- 起始日期：2026-04-17
- 状态：已完成
- 所属任务：T14
- 关联规范：
  - `mango-pmo/rules/backend/*.md`
  - `mango/mango-tools/mango-maven-plugin`

---

## 1. 目标

把 `mango-pmo` 后端规范拆成三类：

- 自动检查
- 半自动检查
- 人工检查

本 Sprint 只做规则盘点、分类、映射和实施计划。

---

## 2. 边界

### In Scope

- 盘点 `mango-pmo/rules/backend` 全部规则
- 梳理当前 P3C / PMD / Checkstyle 规则
- 判断阿里规则的保留、裁剪、去除
- 明确 Mango 自定义规则落点
- 输出第一批自动检查实施清单

### Out of Scope

- 不批量修改业务代码
- 不一次性实现所有规则
- 不删除 `mango:check` 既有能力
- 不以“AI 友好”为理由降低质量门禁

---

## 3. 交付物

- `backend-rule-inventory.md`
- `ali-rule-selection.md`
- `auto-check-mapping.md`
- `manual-review-rules.md`
- `sprint-15-implementation-plan.md`

---

## 4. 规则分类

### 自动检查

规则必须满足：

- 不依赖业务语义
- 可通过代码结构、目录、命名、注解判断
- 误报可控
- 能写测试验证

落点：

- PMD / P3C
- Checkstyle
- `mango-maven-plugin`

### 半自动检查

规则必须满足：

- 程序可筛出候选
- 最终需要人工确认

落点：

- `mango-maven-plugin` 报告
- PR checklist

### 人工检查

规则满足以下任一条件：

- 依赖业务判断
- 依赖架构判断
- 静态检查误报高

落点：

- PR checklist
- Sprint 验收

---

## 5. 第一批规则

| 规则 ID | 规则 | 分类 | 落点 |
|---------|------|------|------|
| BE-API-001 | 仓内业务 API 禁止 `PO` / `Entity` 直出直入 | 自动 | PMD |
| BE-API-002 | 仓内业务 API 禁止 `DTO` 作为默认入参或返回 | 自动 | PMD |
| BE-API-003 | 返回对象统一 `XxxVO` | 自动 | PMD |
| BE-API-004 | 写入参统一 `CreateXxxCommand` / `UpdateXxxCommand` | 自动 | PMD |
| BE-API-005 | 查询入参统一 `XxxQuery` / `XxxPageQuery` | 自动 | PMD |
| BE-API-006 | API 参数必须使用 Bean Validation 注解 | 半自动 | `mango-maven-plugin` |
| BE-API-007 | Controller / Api 必须开启 `@Validated` 或等效校验 | 自动 | PMD |
| BE-API-008 | `*-api` 禁止声明 `@FeignClient` | 自动 | `mango-maven-plugin` |
| BE-CODE-001 | 业务前置条件统一使用 `Require` | 半自动 | `mango-maven-plugin` |
| BE-CODE-002 | 禁止业务入口散写参数 `if/throw` | 半自动 | `mango-maven-plugin` |
| BE-MOD-001 | `api` 模块禁止出现 `entity` / `mapper` / `service` / `controller` | 自动 | `mango-maven-plugin` |
| BE-MOD-002 | `core` 模块禁止出现 `controller` | 自动 | `mango-maven-plugin` |
| BE-MOD-003 | 禁止直接依赖其他域 `core` | 自动 | `mango-maven-plugin` |
| BE-REM-001 | `starter-remote` 禁止硬编码服务发现名 | 半自动 | `mango-maven-plugin` |
| BE-REM-002 | `starter` 对外能力必须注册 | 半自动 | `mango-maven-plugin` |
| BE-SEC-001 | 禁止硬编码密钥 / 口令 / token | 自动 | PMD |
| BE-EX-001 | 禁止吞异常 | 自动 | P3C / PMD |
| BE-EX-002 | 禁止业务代码直接捕获 `Throwable` | 自动 | P3C / PMD |

---

## 6. 阿里规则处理

### 保留

- 异常处理
- 并发
- 集合
- 基础控制流
- 基础安全

### 裁剪

- 命名规则
- 注释规则
- OOP 规则
- 方法长度
- 类长度
- 复杂度

### 去除

- 强制无意义 Javadoc
- 低价值词法限制
- 与 Mango 命名冲突的规则
- 对生成代码误报高且无质量收益的规则

---

## 7. 实施步骤

### Task A: 规则盘点

- [x] 为 `mango-pmo/rules/backend` 每条规则分配规则 ID
- [x] 记录规则来源文件
- [x] 记录规则原文
- [x] 记录规则目标

### Task B: 规则分类

- [x] 标记自动 / 半自动 / 人工
- [x] 标记 PMD / Checkstyle / `mango-maven-plugin` / PR checklist
- [x] 标记误报风险
- [x] 标记是否进入第一批实现

### Task C: 阿里规则筛选

- [x] 列出现有 P3C 规则集
- [x] 标记保留 / 裁剪 / 去除
- [x] 每个去除项写明原因
- [x] 每个裁剪项写明阈值或范围

### Task D: 自动检查设计

- [x] 设计 PMD 规则文件结构
- [x] 设计 `mango-maven-plugin` 检查项
- [x] 保留 `duplicate` / `method-length` / `class-length` 既有能力
- [x] 明确严格模式和迁移模式

### Task E: 验收计划

- [x] 明确测试命令
- [x] 明确规则样例工程
- [x] 明确误报回归样例
- [x] 确认 Sprint 15 依赖的自动检查候选规则

---

## 8. 输出格式

规则盘点表必须包含：

| 字段 | 要求 |
|------|------|
| Rule ID | 必填 |
| Source | 必填 |
| Rule | 必填 |
| Type | 自动 / 半自动 / 人工 |
| Tool | PMD / Checkstyle / mango-maven-plugin / PR checklist |
| Risk | 低 / 中 / 高 |
| Action | 保留 / 裁剪 / 去除 / 新增 |

---

## 9. 验收标准

- [x] 后端规则已全部编号
- [x] 每条规则都有分类
- [x] 每条自动规则都有落点
- [x] 每条半自动规则都有人工确认方式
- [x] 每条人工规则都进入 PR checklist 或 Sprint 验收
- [x] 阿里规则已完成保留 / 裁剪 / 去除判断
- [x] 第一批实施规则已确认
- [x] Sprint 15 依赖的规则已纳入自动化候选清单

---

## 10. 禁止事项

- 禁止直接删除现有 `mango:check` 能力
- 禁止用空规则替代失败规则
- 禁止无测试新增规则
- 禁止把工具试验混入业务 Sprint
- 禁止把人工判断规则强行写成硬检查
