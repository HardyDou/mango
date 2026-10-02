# Mango 文档

这是 Mango 面向业务开发者、架构师和交付人员的使用入口。这里也提供 [Mango 能力地图](./capabilities/README.md) 和 [业务接入场景手册](./guides/business-integration/README.md)。

先按“我要完成什么”选择章节；模块 README 负责能力事实，场景指南负责接入步骤，PMO 规则负责流程约束，设计与计划文档负责决策记录。

## 先选任务

| 我要做什么 | 先看哪里 | 你会得到什么 |
|---|---|---|
| 接入一个业务能力 | [能力地图](./capabilities/README.md) | 能力定位、责任模块、最小路径和源码入口 |
| 按场景接入或排障 | [业务接入场景](./guides/business-integration/README.md) | 前置条件、操作步骤、失败分支和验证入口 |
| 写产品需求 | [产品文档模板选择](../mango-pmo/templates/README.md) | BRD/SRS/TDD/Plan 的选择和顺序 |
| 创建业务项目 | [Business Starter](../mango-business-starter/README.md) | 项目初始化、拓扑、开发和交付入口 |
| 理解架构边界 | [架构设计](./mango-architecture-design.md) | 分层、依赖边界和关键设计决策 |
| 执行研发流程 | [PMO 总流程](../mango-pmo/rules/00-dev-flow.md) | 风险、阶段、审批和验证要求 |

## 章节职责

| 章节 | 只负责什么 | 不负责什么 |
|---|---|---|
| 开始 | 说明 Mango 定位、阅读顺序、版本和入口选择 | 不展开模块 API，不堆历史变更 |
| 示例场景 | 用真实任务说明接入、排障、输入、输出和失败边界 | 不复制长期规范，不替代模块 README |
| 产品文档输出 | 选择 BRD、SRS、TDD、Plan 和交付记录 | 不描述平台能力，不生成混合 PRD |
| 基础能力 | 说明基础设施和公共装配的能力边界 | 不承担业务模块接入说明 |
| 平台能力 | 按业务能力索引后端模块、前端包、责任边界和入口 | 不承载 Issue 日志和交付台账 |
| 架构设计 | 记录稳定架构、边界、取舍和迁移设计 | 不替代使用手册和操作排障 |
| PMO 规范与模板 | 提供流程规则、角色、模板和检查工具 | 不重复能力 README 和业务场景 |

## 产品文档怎么选

```text
业务问题与规则 -> BRD -> SRS -> TDD -> Implementation Plan
```

- 业务问题、范围、流程、规则和业务验收：使用 [BRD](../mango-pmo/templates/business-requirements.md)。
- 用户可观察的系统行为、页面、字段、动作、失败边界和系统验收：使用 [SRS](../mango-pmo/templates/system-requirements.md)。
- 同一任务同时涉及业务和系统：分别填写 BRD、SRS，不生成混合 PRD。
- 完整选择说明见[产品文档模板选择](../mango-pmo/templates/README.md)。

## 阅读顺序

1. 先看能力地图或场景指南，确认责任模块和使用边界。
2. 再读对应后端模块 README 和前端包 README。
3. 需要设计、开发或验收时，再进入架构文档和 PMO 规则。
4. 历史 Issue、交付记录和变更日志只用于追溯，不作为当前能力入口。

## 文档版本

GitHub Pages 根路径发布当前 `main` 的 Latest 文档。历史静态快照已移除，版本内容通过 Git tag 追溯，避免旧业务文档继续进入文档站和仓库。需要锁定版本时，请检出对应 Git tag 构建文档。

## 本地预览

```bash
npm --prefix mango-docs install
npm --prefix mango-docs run docs:dev
npm --prefix mango-docs run docs:build
```

## 文档资产归档边界

当前使用说明、模块能力和场景指南面向使用者；Issue、计划、交付记录和验收证据只承担历史追溯，不替代当前入口。长期规则统一维护在 `mango-pmo`，不在本章复制规则正文。

## 文档事实来源

- 当前能力事实：模块或 package README。
- 业务接入步骤：`mango-docs/guides/business-integration/**`。
- 长期规则：`mango-pmo/rules/**`。
- 模板：`mango-pmo/templates/**`。
- 架构和决策：`mango-docs/designs/**`。
- 交付证据：`mango-docs/plans/**`、`mango-docs/evidence/**`。
