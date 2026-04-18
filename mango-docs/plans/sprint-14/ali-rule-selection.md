# Sprint 14 阿里规则筛选

## 1. 保留

| Rule Set | Decision | Reason |
|---|---|---|
| `ali-concurrent.xml` | 保留 | 并发问题适合静态检查 |
| `ali-exception.xml` | 保留 | 异常吞掉、错误捕获适合静态检查 |
| `ali-set.xml` | 保留 | 集合误用适合静态检查 |
| `ali-flowcontrol.xml` | 保留 | 控制流问题适合静态检查 |
| `ali-constant.xml` | 保留 | 常量滥用适合静态检查 |
| `ali-orm.xml` | 保留 | ORM 常见错误适合静态检查 |
| `ali-other.xml` | 保留 | 先保留，按误报逐条裁剪 |

## 2. 裁剪

| Rule Set | Decision | Constraint |
|---|---|---|
| `ali-naming.xml` | 裁剪 | 保留 Java 基础命名；Mango API 命名以后端规范为准 |
| `ali-oop.xml` | 裁剪 | 保留确定性规则；对架构风格有侵入的规则转人工 |
| Checkstyle `LineLength` | 裁剪 | 保持 300；不因换行噪声阻塞 AI 重构 |
| Checkstyle `CyclomaticComplexity` | 裁剪 | 保持 30；超过阈值阻断或告警按模块阶段决定 |
| Checkstyle `NPathComplexity` | 裁剪 | 保持 500；先用于发现复杂方法 |
| Checkstyle `MagicNumber` | 裁剪 | 保持 info；不作为阻断规则 |
| P3C `ClassMustHaveAuthorRule` | 裁剪 | `@author` 不提供 AI 可用契约信息 |
| P3C `CommentsMustBeJavadocFormatRule` | 裁剪 | 形式化 Javadoc 容易制造无信息注释 |
| P3C `AvoidCommentBehindStatementRule` | 裁剪 | 行尾注释不是 Mango 当前质量风险，避免噪声 |

## 3. 去除

| Rule | Decision | Reason |
|---|---|---|
| 强制方法 Javadoc | 去除 | 容易制造无信息注释 |
| 强制类型 Javadoc | 去除 | 容易制造无信息注释 |
| 强制字段 Javadoc | 去除 | 简单字段注释通常只是复述命名 |
| Javadoc 首句句号 | 去除 | 格式收益低，不服务 AI 理解 |
| 低价值词法限制 | 去除 | 与业务语义无关，误报高 |
| 与 Mango 后缀冲突的规则 | 去除 | 项目规范优先 |
| 对生成代码误报高的规则 | 去除 | 不产生质量收益 |

## 4. Javadoc 契约优先

| Target | Decision |
|---|---|
| API / SPI / Annotation / Properties | 必须描述业务语义、参数约束、返回语义或边界条件 |
| public 契约方法 | 必须说明调用方需要依赖的行为契约 |
| Enum constants | 必须说明业务含义 |
| Getter / Setter / 简单字段 / 显然方法 | 不强制 Javadoc |

## 5. Mango 优先规则

以下规则覆盖阿里规则时，以 Mango 规则为准：

| Area | Mango Rule |
|---|---|
| API 返回 | 统一 `XxxVO` |
| API 入参 | 写操作 `Command`，查询 `Query` |
| 仓内业务 API | 禁止默认使用 `DTO` |
| 模块契约 | `*-api` 禁止 `@FeignClient` |
| 远程调用 | `starter-remote` 不硬编码服务发现名 |
| 前置条件 | 业务逻辑入口使用 `Require` |

## 6. 后续处理

- 当前 P3C 规则集不直接扩大。
- 每条去除规则必须有误报样例或项目冲突说明。
- 每条新增规则必须有正例、反例、测试样例。
- 自动规则先进入 `report` 模式，再进入 `migration` 或 `strict`。
