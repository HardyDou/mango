# Understanding-first 能力入口试点

## 这是什么

这是 Mango 的“先理解，再设计或开发”试点入口。它不替代模块 README、PMO 规则或正式交付文档；它只帮助读者先回答：

- 这件事解决什么问题？
- 一个真实场景怎么走？
- 谁负责什么？
- 数据或状态怎样变化？
- 失败后先看哪里？

## 先读哪一页

| 你要做的事 | 先读 |
|---|---|
| 把初始化配置、菜单或字典声明交给目标模块 | [Resource Registry 快速用法](./resource-registry-quickstart.md) |
| 表单上传附件并在详情页预览 | [File 上传到预览快速用法](./file-upload-preview-quickstart.md) |
| 让业务单据发起审批并展示进度 | [Workflow 审批快速用法](./workflow-approval-quickstart.md) |

## 阅读顺序

1. 先看本页对应的一个具体场景。
2. 只记住“谁负责什么”和最小接入路径。
3. 再回到模块 README 查看完整 API、配置和边界。
4. 最后查看业务指南、权限/租户说明和验证入口。

这些页面不新增长期规范。遇到强制约束、权限、租户、测试或交付门禁时，以 `mango-pmo/rules/**` 和当前模块 README 为准。文本输出遵循 [Agent 文本输出规范](../../../mango-pmo/rules/13-agent-text-output.md)；需要作出方案、验证或交付决定时，按 [决策专家评审规范](../../../mango-pmo/rules/14-decision-expert-review.md)组织意见和同行评审。

输出形式按理解成本选择：一个术语用受控文字；跨模块或状态变化用文字流程图；多分支排障才考虑临时 HTML。图、HTML 或视频同时提供文字版和当前源码入口。
