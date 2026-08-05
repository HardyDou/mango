# Mango PMO Agent 入口

本文件只做 `mango-pmo` 子目录入口和路由，长期规则链接到规范源。

## 1. 规范源

- PMO 总流程：[rules/00-dev-flow.md](./rules/00-dev-flow.md)
- 文档资产边界：[rules/06-document-assets.md](./rules/06-document-assets.md)
- 能力说明维护：[rules/08-capability-docs.md](./rules/08-capability-docs.md)
- PMO Agent：[agents/05-pmo-agent.md](./agents/05-pmo-agent.md)

## 2. Preflight

只读任务直接处理。治理变更执行：

```bash
node tools/pmo-preflight.mjs \
  --role pmo \
  --phase governance \
  --task "<用户任务>" \
  --paths "mango-pmo/**"
```

`References` 是按需查阅入口，不要求批量阅读；涉及代码时优先使用 `Code baselines`。
