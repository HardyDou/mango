# 业务接入场景

本章按“我要把什么业务能力接入系统”组织内容。每篇指南只保留当前接入事实、前置条件、步骤、验证和失败边界；平台能力的完整 API、配置和实现边界以对应模块 README 为准。

## 使用方式

1. 先从[能力地图](../../capabilities/README.md)确认责任模块和最小能力组合。
2. 选择一个业务场景，按“阅读顺序”依次查看模块 README。
3. 准备前置条件，完成接入检查点和最小闭环。
4. 使用模块验证命令、场景验收点和真实环境证据完成验收。
5. 遇到“已接入但行为异常”，转到[常见问题与排障](../faq/README.md)，不要把排障记录混入场景步骤。

## 场景索引

### 业务能力接入

| 场景 | 适合任务 | 主要能力 |
|---|---|---|
| [文件上传表单](./file-upload-form.md) | 表单上传附件、回显、预览、下载、打包或 PDF 归档 | File、Fileproc、File Preview、Frontend File |
| [业务审批接入](./workflow-business-approval.md) | 发起审批、处理任务、回写业务状态和查看流程记录 | Workflow、Workflow Frontend、Workflow Example |
| [按钮展示规则](./permission-button-display-rule.md) | 根据业务状态、行数据或页面上下文控制按钮显示 | Access、Authorization、Admin Shell |

## 场景与排障的边界

| 读者问题 | 进入 |
|---|---|
| 我还没有接入能力，不知道依赖什么 | 本章场景指南和[能力地图](../../capabilities/README.md) |
| 能力已接入，但菜单、按钮或数据异常 | [常见问题与排障](../faq/README.md) |
| 我要构建、升级、发布或治理业务项目 | [运维、升级与交付](../operations/README.md) |
| 我要了解模块 API、配置、边界和验证 | 对应后端或前端模块 README |
| 我要查某次发布为什么这样改 | [CHANGELOG](../../../CHANGELOG.md)、设计文档、计划和验收证据 |

## 新场景最小结构

新增场景时，至少说明：

- 目标用户、适用条件和不适用边界；
- 后端 starter/remote starter、前端包和配置前置条件；
- 菜单、权限、租户和初始化数据的责任归属；
- 按顺序可执行的接入步骤；
- 正向、反向和跨租户最小验收闭环；
- 常见失败、事实来源和模块验证入口。

场景指南不复制长期规则，不把 Issue 时间线当作章节正文。长期研发规则以 [PMO 规则](../../../mango-pmo/rules/00-dev-flow.md) 为准。

## 关联入口

- [Mango 能力地图](../../capabilities/README.md)
- [常见问题与排障](../faq/README.md)
- [运维、升级与交付](../operations/README.md)
- [业务项目开发指南](../../designs/business-project-development-guide.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
