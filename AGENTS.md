# Mango Agent 入口

本文件面向 Codex 和通用 Agent。任何需求、设计、开发、测试、验收或规范治理任务，都必须先执行 PMO preflight。

## 1. 唯一规范源

- `mango-pmo` 是唯一长期规范源。
- `mango-docs` 只放设计文档、Sprint 计划、交付记录和历史设计，不作为规范源。
- `AGENTS.md`、`CLAUDE.md`、`GEMINI.md` 只做入口和路由，不复制长期规则正文。

## 2. 开工前强制步骤

开始实质分析、计划或改文件前，必须执行：

```bash
node mango-pmo/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<用户任务>" \
  --paths "<可能影响的路径，逗号分隔>"
```

然后读取输出中 `Must read` 的每一个文件原文。

无法判断角色或路径时，使用 `--role dev --phase develop`，并用任务关键词触发规则；只有影响范围确实无法判断时才问用户。

## 3. 任务路由

| 任务类型 | role | phase |
|----------|------|-------|
| 需求、PRD、验收标准 | `pm` | `requirement` |
| 架构、边界、API、数据库设计 | `tech-lead` | `design` |
| 编码、修复、重构 | `dev` | `develop` |
| 测试、E2E、验收 | `qa` | `verify` |
| 规范、流程、Agent 入口治理 | `pmo` | `governance` |

## 4. 开工回显

执行 preflight 后，开始编码或编辑前必须简短说明：

- 任务类型。
- 已加载的 PMO 文件。
- 本次预期验证方式。

## 5. 交付报告

最终回复必须包含：

- 改动范围。
- 实际加载的 PMO 文件。
- 执行的验证命令。
- 未验证项和风险。
- PMO 例外说明；没有例外则写“无”。
