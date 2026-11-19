---
'@mango/pmo': patch
---

新增 `mango-pmo/tools/workflow-contract-fingerprint.mjs`：从上一个发布批次标签算出 `workflow-contract-fingerprint`（`api`、`web`、`changed`、`since`）与 `workflow-compatible-additions`（按类列出新增 DTO 字段与枚举值），并支持 `--verify-release-notes` 校验发布说明里的契约块。`mango-release` Skill 的 Readiness 要求触碰 `mango-workflow` 的批次必须携带该块，`pmo-doc-check` 增加对应校验入口，避免手工复制哈希或用 `# WARN` 当已知状态。
