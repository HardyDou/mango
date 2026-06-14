# Mango Gemini 入口

本文件只做 Gemini 入口和路由。长期规则只维护在 `mango-pmo`，不要在入口文件复制规则正文。

## 1. 规范源

- PMO 总流程：@./mango-pmo/rules/00-dev-flow.md
- 文档资产边界：@./mango-pmo/rules/06-document-assets.md
- 能力说明维护：@./mango-pmo/rules/08-capability-docs.md
- PM Agent：@./mango-pmo/agents/01-pm-agent.md
- Tech Lead Agent：@./mango-pmo/agents/02-tech-lead-agent.md
- Dev Agent：@./mango-pmo/agents/03-dev-agent.md
- QA Agent：@./mango-pmo/agents/04-qa-agent.md
- PMO Agent：@./mango-pmo/agents/05-pmo-agent.md

## 2. Preflight

按 PMO 总流程判断是否执行 preflight。需要执行时使用：

```bash
node mango-pmo/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<用户任务>" \
  --paths "<可能影响的路径，逗号分隔>"
```

然后读取输出中 `Must read` 的每一个文件原文。

## 3. 子项目入口

- 后端：进入 `mango/` 时遵守 `mango/AGENTS.md` 和 `mango/GEMINI.md`。
- 前端：进入 `mango-ui/` 时遵守 `mango-ui/AGENTS.md` 和 `mango-ui/GEMINI.md`。
- PMO：进入 `mango-pmo/` 时遵守 `mango-pmo/AGENTS.md`。
