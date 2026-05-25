# Mango Claude 入口

@mango-pmo/rules/00-dev-flow.md
@mango-pmo/agents/01-pm-agent.md
@mango-pmo/agents/02-tech-lead-agent.md
@mango-pmo/agents/03-dev-agent.md
@mango-pmo/agents/04-qa-agent.md
@mango-pmo/agents/05-pmo-agent.md

## 1. 唯一规范源

- `mango-pmo` 是唯一长期规范源。
- `mango-docs` 只放设计文档、Sprint 计划、交付记录和历史设计，不作为规范源。
- 本文件只做 Claude 入口，不复制长期规则正文。

## 2. PMO preflight 触发规则

PMO preflight 用于进入正式交付流程前确定角色、阶段、必读规范和验证口径。

以下情况必须执行 PMO preflight：

- 按设计文档、plan、Sprint 计划或交付台账进行开发、验证、发布或治理。
- 新增或修改代码、接口、数据库、测试、前端页面、构建配置。
- 新增或修改设计文档、Sprint 计划、交付记录、规范或 Agent 入口。
- 做架构设计、技术方案、测试方案、验收、发布、提交或 PR。
- 用户明确要求“按 PMO / 按计划 / 按设计文档 / 按交付台账”推进。

以下情况不需要执行 PMO preflight：

- 简单问答、概念解释、使用说明。
- 只读定位文件、类、接口、配置或快速查看模块现状。
- 查看 git 状态、目录结构、日志、报错片段，但不进入修复或交付。
- 用户明确要求“先快速看看 / 暂不进入 PMO 流程”。

需要执行时使用：

```bash
node mango-pmo/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<用户任务>" \
  --paths "<可能影响的路径，逗号分隔>"
```

然后读取输出中 `Must read` 的每一个文件原文。

角色按任务自动推断：需求用 PM，设计用 Tech Lead，开发用 Dev，测试验收用 QA，规范治理用 PMO。只有无法判断时才询问用户。

## 3. 子项目入口

- 后端：进入 `mango/` 时遵守 `mango/AGENTS.md` 和 `mango/CLAUDE.md`。
- 前端：进入 `mango-ui/` 时遵守 `mango-ui/AGENTS.md` 和 `mango-ui/CLAUDE.md`。
- PMO：进入 `mango-pmo/` 时遵守 `mango-pmo/AGENTS.md`。

## 4. 交付报告

最终回复必须包含改动范围、实际加载的 PMO 文件、验证命令、未验证项和 PMO 例外说明。
