# Mango Agent 入口

本文件面向 Codex 和通用 Agent，只做入口和路由。长期规则只维护在 `mango-pmo`，不要在入口文件复制规则正文。

## 1. 规范源

- PMO 总流程：[mango-pmo/rules/00-dev-flow.md](./mango-pmo/rules/00-dev-flow.md)
- 文档资产边界：[mango-pmo/rules/06-document-assets.md](./mango-pmo/rules/06-document-assets.md)
- 能力说明维护：[mango-pmo/rules/08-capability-docs.md](./mango-pmo/rules/08-capability-docs.md)
- 规则索引：[mango-pmo/rules/index.json](./mango-pmo/rules/index.json)

## 2. Preflight

正式交付、验证、发布、提交、PR、规范治理或需要改变受版本控制文件时，先按 PMO 总流程判断是否执行 preflight。

```bash
node mango-pmo/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<用户任务>" \
  --paths "<可能影响的路径，逗号分隔>"
```

执行后读取输出中 `Must read` 的每一个文件原文。

## 3. 角色路由

| 任务类型 | role | phase |
|----------|------|-------|
| 需求、PRD、验收标准 | `pm` | `requirement` |
| 架构、边界、API、数据库设计 | `tech-lead` | `design` |
| 编码、修复、重构 | `dev` | `develop` |
| 测试、E2E、验收 | `qa` | `verify` |
| 规范、流程、Agent 入口治理 | `pmo` | `governance` |
