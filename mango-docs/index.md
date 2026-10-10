# Mango 文档资产索引

这是文档资产的分类索引。第一次阅读请从[Mango 文档首页](./README.md)开始；需要选择平台能力时进入[能力地图](./capabilities/README.md)。

## 当前使用入口

| 章节 | 回答的问题 | 入口 |
|---|---|---|
| 能力地图 | Mango 有哪些能力、模块边界和源码入口？ | [能力地图](./capabilities/README.md) |
| 业务接入场景 | 怎样把文件、审批或按钮能力接入业务？ | [业务接入场景](./guides/business-integration/README.md) |
| 常见问题与排障 | 菜单、按钮、租户或基础数据异常怎么定位？ | [FAQ 与排障](./guides/faq/README.md) |
| 运维、升级与交付 | 怎样构建、升级、发布和恢复业务项目？ | [运维指南](./guides/operations/README.md) |
| Understanding-first | 怎样先建立能力理解，再开始实现？ | [Understanding-first](./guides/understanding-first/README.md) |

## 设计与治理入口

| 资产 | 职责 | 入口 |
|---|---|---|
| 架构设计 | 记录分层、边界、取舍和迁移设计 | [架构设计](./mango-architecture-design.md) |
| 设计文档 | 记录一个能力或问题的方案决策 | `designs/**` |
| 计划与交付 | 记录实施计划、交付结果和验收证据 | `plans/**`、`evidence/**` |
| PMO 规范与模板 | 记录研发流程、角色、规则和模板 | [PMO](../mango-pmo/README.md) |
| 发布历史 | 记录版本和变更摘要 | 根 [CHANGELOG](../CHANGELOG.md)、Git tag、Release |

## 目录职责

- **当前入口**只说明现在如何使用。
- **模块 README**说明能力事实、配置、API、边界和模块验证。
- **场景/FAQ/运维指南**说明任务路径，不复制长期规则。
- **设计、计划、证据和 CHANGELOG**承担历史追溯，不作为当前能力的唯一入口。
- **PMO** 是长期流程规则和模板的唯一规范源。
