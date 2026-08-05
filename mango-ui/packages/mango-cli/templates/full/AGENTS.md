# Mango Business Agent 入口

本项目由 `mango-cli init --preset {{preset}}` 生成。

## 1. 唯一规范源

- `business-pmo/mango-baseline` 是当前业务仓的 Mango baseline 规范快照。
- `business-docs` 只放业务设计文档、Sprint 计划、交付记录和历史设计，不作为长期规范源。
- `AGENTS.md` 只做入口和路由，不复制长期规则正文。

## 2. PMO preflight

正式任务先确认本仓 baseline 没有漂移：

```bash
mango pmo check --project-dir . --locked
```

该检查同时校验 `business-pmo/pmo-lock.json`、baseline manifest 和 `.agents/skills` 中由 PMO bundle 管理的项目 Skill。项目 Skill 同步不代表用户级 Codex plugin 已安装。

正式开发、验证、发布、提交前执行：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<任务>" \
  --paths "<影响路径，逗号分隔>"
```

`References` 只在边界不明确时定向查阅；代码生成和新增实现优先使用输出中的 `Code baselines`。

涉及业务需求、设计、台账或验收时，只定位并使用本次任务直接依赖的 `business-docs/**` 契约；没有明确路径时先定位或要求补齐，不批量阅读其它历史文档，也不得只按 Mango baseline 自行推断需求。

发布、发布验证、发布恢复和发布收尾只使用项目内 `.agents/skills/mango-release`（规范源为 `business-pmo/mango-baseline/skills/mango-release`）。不要调用用户级、插件级或其它外部通用 release skill。

## 3. 交付报告

最终回复必须包含：

- 改动范围。
- 实际采用的代码 baseline 及版本，以及为具体边界实际查阅的参考资料（未查阅时写“无”）。
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

后端验证统一执行完整 reactor：

```bash
mvn -f backend/pom.xml verify
```

`backend/architecture-verification` 必须保持为最后一个模块；它负责聚合检查生成后的业务 `api/core/starter/starter-remote` 边界，并执行阻断式 P3C/PMD、Checkstyle、SpotBugs 检查。禁止用 `changedOnly`、`codeLevelExcludedModules`、缩小 Reactor 或关闭静态失败阻断来规避全量校验。

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

启动前必须确认并在交付记录中报告 `.mango/dev-workspace.env` 中的 `MANGO_WORKSPACE_ID`、`MANGO_MAVEN_REVISION_QUALIFIER`、`MANGO_BACKEND_PORT`、`MANGO_FRONTEND_PORT`、`MANGO_DB_NAME`。不要交叉使用其它 worktree 的 Maven revision、服务、端口或数据库。

不要用 `java -jar` 或手写 Maven reactor 命令作为开发启动入口；这些细节由 Mango CLI 封装。
