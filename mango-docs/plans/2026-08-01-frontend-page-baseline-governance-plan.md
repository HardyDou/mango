# 前端页面最新组件基线治理实施计划

## 1. 基线

- 设计：`mango-docs/designs/2026-08-01-frontend-page-baseline-governance.md`
- 最终风险：L3
- 模式：FULL 治理
- 分支：`governance/frontend-page-baseline`

## 2. 实施顺序

1. 将页面骨架、分页和弹框的根导出与深路径导出登记为 C4，并补齐 Common README。
2. 更新列表、详情、表单、弹框 PMO 规则和规则索引描述。
3. 更新 Business Starter canonical CRUD 页面为 `MangoDialog`，补齐语义锚点。
4. 同步 CLI 业务模块投影并更新 Starter/CLI 模板断言。
5. 新增可复用的前端页面增量 checker、正反例测试和 changed-file 分类。
6. 接入 Mango 前端 workflow 与业务项目 GitHub/Gitea workflow。
7. 同步 Business Starter PMO baseline 和能力说明。

## 3. 验证映射

| 验收项 | 措施 | 命令 |
| --- | --- | --- |
| AC-001、AC-002 | M09、M10 | component contract checker、Common 组件测试、包构建 |
| AC-003 | M09、M11 | Starter 检查、CLI 投影、CLI module template 测试 |
| AC-004 | M09 | PMO preflight/checker、baseline sync check |
| AC-005 | M10 | 页面基线 checker 单元测试与当前 diff 检查 |
| AC-006 | M09、M14 | workflow 文本合同、治理自复核 |

## 4. 完成条件

- 所有定向命令退出码为 0。
- `git diff` 不包含未说明的发布版本、锁文件或生成产物。
- 不执行 npm、CLI、Maven 或其它制品发布；提交、Push、PR 和合并仅在用户明确授权后执行。
