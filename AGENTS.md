# Mango Agent 入口

本文件面向 Codex 和通用 Agent。PMO preflight 只在进入正式交付流程时强制执行；简单问答、只读定位、快速查看、纯仓库同步和本地运维不触发。

## 1. 唯一规范源

- `mango-pmo` 是唯一长期规范源。
- `mango-docs` 只放设计文档、Sprint 计划、交付记录和历史设计，不作为规范源。
- `AGENTS.md`、`CLAUDE.md`、`GEMINI.md` 只做入口和路由，不复制长期规则正文。

## 2. PMO preflight 触发规则

PMO preflight 的首要目的不是审批命令，而是在正式交付前刷新 Agent 对本项目规范的记忆。

完整触发边界见 [Mango PMO 总流程](./mango-pmo/rules/00-dev-flow.md)。Agent 必须先识别用户意图和规范遗漏风险，再决定是否进入 PMO。

开工前自检提示词：

```text
我是否要交付一个会影响代码、配置、接口、数据库、测试、页面、发布物料、规范或交付结论的结果？
如果是，我可能会忘记哪些 Mango 规范？必须先执行 PMO preflight，并阅读 Must read 文件。
如果只是查看、解释、定位、纯同步仓库或本地运维，且不形成交付结论，则不进入 PMO。
过程中一旦需要改文件、修复问题、解决冲突、判断验收或发布状态，立即停止低流程并进入 PMO。
```

以下情况必须执行：

- 交付代码、配置、接口、数据库、测试、页面、发布物料、规范、设计、验收、提交或 PR。
- 低风险操作过程中需要人工改文件、解决冲突、修复问题或形成交付结论。
- 用户明确要求“按 PMO / 按计划 / 按设计文档 / 按交付台账”推进。

以下情况不需要执行：

- 简单问答、概念解释、使用说明。
- 只读定位文件、类、接口、配置或快速查看模块现状。
- 查看 git 状态、目录结构、日志、报错片段，但不进入修复或交付。
- 执行纯仓库同步命令，例如 `git fetch`、`git pull --ff-only`、`git remote update`，且不人工编辑文件、不解决冲突、不做交付验证。
- 查看或刷新 GitHub Issue、PR、分支、tag、release 列表，但不创建、修改或发布。
- 安装依赖、清理本地缓存、停止本地进程、查看端口占用等本地运维操作，且不修改受版本控制文件。
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
