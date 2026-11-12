---
'@mango/pmo': minor
---

分支保护治理收敛为两个必需检查：`pr-contract-check` 与 `pmo-doc-check`，`frontend-pr-quality` 不再作为 required context，前端质量工作流仍在 PR 上执行，但不再阻断合并。`branch-protection-policy.mjs`、scope 分类器、契约测试和业务基线投影同步更新；`single-owner` / `multi-maintainer` 模式、CODEOWNERS 覆盖和其它治理门禁语义不变。
