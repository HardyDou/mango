---
name: mango:evaluator
description: Mango 质检评估器 - 基于 SonarQube + Alibaba P3C 评估代码质量
argument-hint: "<projectPath> [moduleName]"
---

# Mango Evaluator - 基于 SonarQube 的代码质检

## 工作流程

```
1. 解析参数：projectPath, moduleName（可选）
2. 执行 mvn sonar:sonar（或读取已生成的报告）
3. 解析 SonarQube Web API 获取问题列表
4. 按 Alibaba P3C 规则分类问题
5. 生成质检报告
```

## 核心规则集：Alibaba P3C

### 阻断级（Blocker）- 必须修复

| 规则ID | 类别 | 说明 |
|--------|------|------|
| p3c001 | 可靠性 | 所有外部输入必须校验 |
| p3c002 | 可靠性 | 禁止空的 catch 块 |
| p3c003 | 可靠性 | 禁止捕获 NullPointerException |
| p3c004 | 安全性 | 禁止硬编码密码/密钥 |
| p3c005 | 安全性 | SQL 必须使用参数化查询 |
| p3c006 | 并发 | 禁止使用 Thread.stop() |
| p3c007 | OOP | 禁止使用实例计数判断生命周期 |

### 严重级（Critical）- 建议修复

| 规则ID | 类别 | 说明 |
|--------|------|------|
| p3c101 | 命名 | POJO 类必须使用包装类型 |
| p3c102 | 命名 | 常量必须用 static final 或 enum |
| p3c103 | 格式 | 缩进必须为4空格 |
| p3c104 | OOP | 禁止用 BigDecimal 的 equals 比较值 |
| p3c105 | 集合 | 禁止用 size()==0 判断空集合 |
| p3c106 | 并发 | 共享变量必须用 volatile 或 Atomic* |

### 规则级（Major）- 建议优化

| 规则ID | 类别 | 说明 |
|--------|------|------|
| p3c201 | 命名 | 方法命名禁止用 is/has/get 前缀混用 |
| p3c202 | 注释 | 所有 public 方法必须有 JavaDoc |
| p3c203 | 格式 | 每行不超过 120 字符 |
| p3c204 | 控制语句 | 禁止 if/for/while 不加大括号 |

## 评估维度

### 1. 可靠性（Reliability）

- 缺陷密度 < 1/千行
- 无 Block/Critical 问题

### 2. 安全性（Security）

- 无硬编码密码/密钥
- SQL 注入防护
- XSS 防护

### 3. 可维护性（Maintainability）

- 重复率 < 3%
- 类长度 < 500 行
- 方法长度 < 50 行
- 圈复杂度 < 10

### 4. 测试覆盖（Coverage）

- 行覆盖率 ≥ 60%
- 分支覆盖率 ≥ 50%

## 使用方式

### 在 Claude Code 中执行

```bash
# 扫描整个项目
/mango:evaluator /Users/hardy/Work/company02/mango

# 扫描指定模块
/mango:evaluator /Users/hardy/Work/company02/mango mango-common
```

### API 调用

```bash
# 获取项目问题列表
curl -s "http://localhost:9000/api/issues/search?component=mango-common&severities=BLOCKER,CRITICAL&ps=100"

# 获取质量门禁状态
curl -s "http://localhost:9000/api/qualitygates/project_status?project=mango"
```

## 质检报告格式

```markdown
## Mango 质检报告

**项目**: mango-common
**扫描时间**: 2026-03-30 14:00
**SonarQube**: http://localhost:9000

### 总体评分

| 维度 | 评级 | 分数 |
|------|------|------|
| 可靠性 | A | 4.2 |
| 安全性 | B | 3.8 |
| 可维护性 | A | 4.5 |
| 覆盖率 | C | 2.9 |

### 问题汇总

| 级别 | 数量 | 阻断 |
|------|------|------|
| Blocker | 0 | ✅ |
| Critical | 2 | ⚠️ 建议修复 |
| Major | 15 | 🟡 可优化 |
| Minor | 42 | ℹ️ 参考 |

### P3C 规则违规

#### 可靠性（2项）
1. [p3c002] 禁止空的 catch 块
   - 文件：`src/main/java/io/mango/common/xxx.java`
   - 行号：42
   - 说明：catch 块不能为空，应记录日志或抛出异常

#### 安全性（0项）✅

#### 可维护性（15项）
1. [p3c204] if 语句必须使用大括号
   - 文件：`src/main/java/io/mango/common/xxx.java`
   - 行号：56

### 质量门禁

**状态**: ❌ FAILED

| 指标 | 实际值 | 要求值 | 状态 |
|------|--------|--------|------|
| 可靠性评级 | A | ≥ B | ✅ |
| 安全性评级 | B | ≥ B | ✅ |
| 覆盖率 | 58% | ≥ 60% | ❌ |
| 重复率 | 2.1% | ≤ 3% | ✅ |

### 修复建议

1. **[Critical]** `xxx.java:42` - catch 块不能为空
2. **[Major]** `xxx.java:56` - if 语句必须使用大括号
3. **[Coverage]** 当前覆盖率 58%，需要提升至 60%

### 报告链接

- SonarQube 控制台：http://localhost:9000/dashboard?id=mango-common
- 问题列表：http://localhost:9000/project/issues?id=mango-common
```
