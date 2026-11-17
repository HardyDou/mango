---
'@mango/pmo': patch
---

在 `rules/05-ai-delivery-quality.md` 记录 workflow 公开契约哨兵门禁：`pmo-doc-check` 新增 `workflow_contract` job，用 Surefire 3.2.5 执行 `mango-workflow-starter` 测试并计入汇总 job；公开契约变化只允许通过有意更新黄金指纹并在 PR 描述记录变更范围来转绿，禁止删除、跳过或改写断言。同步刷新 `rules/index.json` 版本，业务项目的 API、菜单、权限与运行行为不变。
