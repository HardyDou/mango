# Mango Gemini 入口

@./mango-pmo/rules/00-dev-flow.md
@./mango-pmo/agents/01-pm-agent.md
@./mango-pmo/agents/02-tech-lead-agent.md
@./mango-pmo/agents/03-dev-agent.md
@./mango-pmo/agents/04-qa-agent.md
@./mango-pmo/agents/05-pmo-agent.md

## 1. 唯一规范源

- `mango-pmo` 是唯一长期规范源。
- `mango-docs` 只放设计文档、Sprint 计划、交付记录和历史设计，不作为规范源。
- 本文件只做 Gemini 入口，不复制长期规则正文。

## 2. 开工前强制步骤

开始实质分析、计划或改文件前，必须执行 PMO preflight：

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
