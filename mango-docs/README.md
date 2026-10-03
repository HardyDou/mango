# Mango 文档

Mango 是面向业务开发者、架构师和交付人员的业务系统研发底座。本页同时作为 Mango 能力地图、业务接入场景手册与文档资产归档边界的总入口。文档按读者任务组织：先找到目标，再进入能力、场景或规则，不按仓库源码目录堆叠内容。

## 先选任务

| 我要做什么 | 入口 | 适合谁 |
|---|---|---|
| 了解 Mango 有哪些模块 | [能力地图](./capabilities/README.md) | 业务开发者、架构师 |
| 接入文件、审批或按钮能力 | [业务接入场景](./guides/business-integration/README.md) | 业务开发者 |
| 菜单、权限或租户数据异常 | [常见问题与排障](./guides/faq/README.md) | 开发、测试、运维 |
| 构建、升级、发布或治理项目 | [运维、升级与交付](./guides/operations/README.md) | 交付、运维、技术负责人 |
| 理解系统边界和设计取舍 | [架构设计](./mango-architecture-design.md) | 架构师、技术负责人 |
| 编写 BRD、SRS、TDD 或计划 | [PMO 模板选择](../mango-pmo/templates/README.md) | PM、技术负责人、开发 |
| 执行研发流程和质量门禁 | [PMO 总流程](../mango-pmo/rules/00-dev-flow.md) | 所有交付角色 |

## 文档层次

| 层次 | 负责回答 | 主要资产 |
|---|---|---|
| 入口 | 从哪里开始、下一步看什么 | 本页、[文档资产索引](./index.md) |
| 能力 | Mango 提供什么、谁负责、边界是什么 | 能力地图、模块/package README |
| 场景 | 怎样接入一个真实业务目标 | `guides/business-integration/**` |
| FAQ | 已接入后如何定位异常 | `guides/faq/**` |
| 运维 | 怎样构建、升级、发布和恢复 | `guides/operations/**` |
| 决策 | 为什么采用当前架构和迁移方案 | `designs/**` |
| 规则 | 研发阶段、质量和提交要求是什么 | `mango-pmo/rules/**` |
| 追溯 | 某次变更的计划、证据和历史上下文 | `plans/**`、`evidence/**`、`CHANGELOG.md` |

## 推荐阅读顺序

1. 从[能力地图](./capabilities/README.md)或任务入口确认目标模块。
2. 进入对应场景或 FAQ，了解前置条件、步骤和验证闭环。
3. 回到模块 README，查完整配置、API、边界和模块级验证。
4. 需要设计、开发或交付时，再阅读架构文档和 PMO 规则。
5. 只在需要追溯时查看设计、计划、证据和变更日志。

## 产品文档怎么选

```text
业务问题与规则 -> BRD -> SRS -> TDD -> Implementation Plan -> 验收证据
```

- 业务目标、范围、流程、规则和业务验收：使用 [BRD](../mango-pmo/templates/business-requirements.md)。
- 用户可观察的系统行为、页面、字段、动作、失败边界和系统验收：使用 [SRS](../mango-pmo/templates/system-requirements.md)。
- 技术边界、接口、数据、风险和验证：使用 [TDD](../mango-pmo/templates/technical-design.md)。
- 可执行任务、依赖和证据：使用 [Implementation Plan](../mango-pmo/templates/implementation-plan.md)。
- 完整选择说明见[产品文档模板选择](../mango-pmo/templates/README.md)。

## 文档版本与本地预览

GitHub Pages 根路径发布当前 `main` 的 Latest 文档。历史版本通过 Git tag 追溯，不把旧快照混入当前入口。

```bash
npm --prefix mango-docs install
npm --prefix mango-docs run docs:dev
npm --prefix mango-docs run docs:build
```

## 事实来源

- 当前能力事实：对应后端模块或前端 package README。
- 当前业务步骤：[业务接入场景](./guides/business-integration/README.md)。
- 当前排障路径：[常见问题与排障](./guides/faq/README.md)。
- 当前交付路径：[运维、升级与交付](./guides/operations/README.md)。
- 长期规则和模板：`mango-pmo/rules/**`、`mango-pmo/templates/**`。
- 架构和决策：`mango-docs/designs/**`。
- 交付计划和证据：`mango-docs/plans/**`、`mango-docs/evidence/**`。

当前使用说明不复制长期规则；历史变更应进入模块 README、根 CHANGELOG、Release、设计文档或交付证据，而不是继续堆在场景入口。
