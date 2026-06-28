# Mango Business Agent 入口

本项目由 `mango-cli init --preset {{preset}}` 生成。

## 1. 唯一规范源

- `business-pmo/mango-baseline` 是当前业务仓的 Mango baseline 规范快照。
- `business-docs` 只放业务设计文档、Sprint 计划、交付记录和历史设计，不作为长期规范源。
- `AGENTS.md` 只做入口和路由，不复制长期规则正文。

## 2. PMO preflight

正式任务先确认本仓 baseline 没有漂移：

```bash
mango pmo check --project-dir .
```

正式开发、验证、发布、提交前执行：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<任务>" \
  --paths "<影响路径，逗号分隔>"
```

然后读取输出中 `Must read` 的每一个文件原文。

涉及业务需求、设计、台账或验收时，还必须读取本次任务对应的 `business-docs/**` 文件；没有明确路径时先定位或要求补齐，不得只按 Mango baseline 自行推断需求。

## 3. 交付报告

最终回复必须包含：

- 改动范围。
- 实际加载的 Mango baseline 文件。
- 执行的验证命令。
- 未验证项和风险。
- PMO 例外说明；没有例外则写“无”。

## 4. 验收证据

涉及页面、接口、权限、数据或 E2E 验收时，必须填写验收证据，并执行：

```bash
node business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs \
  --evidence "<验收证据文件路径>"
```

禁止只用“接口 200”“页面无异常”“截图正常”声明验收通过。

## 5. 本地开发启动

同一任务返工、Review 修改、CI 修复或验收缺陷修复前，先执行：

```bash
git worktree list
```

如果已有当前任务分支对应的 worktree，必须复用；不要为同一个任务再开新 worktree。

后端开发只使用：

```bash
mango workspace init
mango workspace status
mango dev start
```

启动前必须确认并在交付记录中报告 `.mango/dev-workspace.env` 中的 `MANGO_WORKSPACE_ID`、`MANGO_BACKEND_PORT`、`MANGO_FRONTEND_PORT`、`MANGO_DB_NAME`。不要交叉使用其它 worktree 的服务、端口或数据库。

不要用 `java -jar` 或手写 Maven reactor 命令作为开发启动入口；这些细节由 Mango CLI 封装。
