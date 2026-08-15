# Pull Request 提交规范

## 1. 适用范围

- 本规范统一约束 Mango 主仓和安装 Mango PMO baseline 的业务仓如何提交任务 Pull Request。
- “提交 PR”包括提交前本地核验、必要的本地 Commit、Push 当前任务分支、创建或更新 PR，以及远端 PR 回读。
- 本规范不负责实现、PR Review、审批、合并、Mango 组件发布、业务应用发布或部署。

## 2. 授权边界

- 用户明确要求“提交 PR”或“创建 PR”时，视为授权在当前任务 worktree 完成必要的 Commit、Push 和创建或更新 PR，不再把三个机械步骤拆开重复询问。
- 用户只要求“提交代码”时只允许本地 Commit；只要求“Push”时只允许 Push 已授权分支。
- PR 提交授权不包含 PR 合并、force push、Tag、GitHub Release、registry 发布、业务部署、流量切换或回滚。
- 外部写入前必须确认远端仓库、base、head 和当前任务范围；事实不明时执行 `ASK`。

## 3. 提交前检查

必须执行：

1. 确认当前目录、Git 根、任务 worktree、当前分支、目标 base、远端仓库和关联 Issue；禁止在 `main` 或主 worktree 直接提交任务 PR。
2. 查看 tracked、untracked、staged 和 unstaged 变化，逐项归属当前任务；保留用户无关改动，不得用 `git add -A`、`git add .` 或通配路径把无关文件混入。
3. 按当前仓 PMO preflight 和任务基线完成本地检查。对外能力变化必须完成能力文档门禁；任何失败、阻塞或未解释的跳过都禁止提交。
4. 检查 staged diff、文件模式、生成物、凭据、token、密码、私钥和本地运行文件；敏感信息或不应提交的产物命中时立即停止。
5. 本地任务变化可以先形成当前任务 Commit，再 fetch 并把最新远端 base 合入任务分支；已发布分支默认使用非破坏性 merge，禁止未经授权改写远端历史。
6. base 合入后重新运行受影响检查，确认最终 head 上的结果；合并冲突返回原任务 worktree 修复，不创建新任务或新 worktree。

### 3.1 本地优先与 Runner 对等

- 仓库必须为每个 required check 提供可在开发机执行的同源入口；本地入口与 Runner 必须复用同一 checker、配置和锁文件，不得维护一套只在 CI 生效的隐藏判断。
- Push 或创建/更新 PR 前，必须在准备提交的最终候选变化上执行全部适用本地入口，修复失败并从头重跑受影响集合；只读历史结果、较早 Commit 的绿灯和口头声明都不能作为提交证据。
- fetch 并合入最新 base 后必须在最终 head 再执行一次适用本地入口。只有命令、环境版本、最终 head SHA 和结果均已记录且全部通过，才允许 Push 或创建/更新 PR。
- Runner 只负责对同一 head 做独立复核和保护远端，禁止把 Push、等待 Runner、读取失败、补一个提交再 Push 作为开发调试循环。
- Runner 发现本地未暴露的确定性失败时，必须先在原任务 worktree 建立本地复现入口并修复；本地全绿后才能再次 Push。确属 Runner 环境、权限或外部服务差异时，保留远端证据并修复环境或门禁，不得关闭检查、缩小范围或盲目重跑。

## 4. Commit、Push 与 PR

- 只暂存已确认的任务路径，并在 Commit 前再次检查 `git diff --cached`。
- Commit message 必须描述本次任务结果，不使用“update”“fix stuff”等无范围信息；关联 Issue 时保留可追踪引用。
- Push 只使用当前任务分支的明确远端，不使用 `--force` 或 `--force-with-lease`，除非用户对具体分支明确授权历史改写并接受风险。
- 创建 PR 时以仓库模板为准，填写目标、范围、风险、能力文档、验证命令、结果、未验证项、例外和关联 Issue；禁止保留模板占位符或伪造检查结果。
- 已存在同 base/head PR 时更新该 PR，不重复创建。
- Release PR 的计划、版本、制品和 prepare 证据由 Mango 主仓 `mango-release` 提供；Commit、Push 和 PR 创建仍由 `mango-submit-pr` 执行。普通 Mango PR 携带 Changeset 不进入发布状态机。

## 5. 远端回读与状态

- Push 后回读远端 head SHA，必须等于本地提交 SHA。
- 创建或更新 PR 后回读 PR URL、编号、仓库、base、head、head SHA、文件清单和检查状态。
- PR 成功建立但 required checks 尚未结束时，只能报告 `SUBMITTED_CHECKS_PENDING`；检查失败时报告 `SUBMITTED_CHECKS_FAILED`，不得声明可合并。
- required checks 通过只证明机器门禁通过；Review、会话解决、治理模式和 Owner 决定仍按仓库策略执行。
- PR 提交完成状态为 `SUBMITTED`，不等于 `REVIEWED`、`MERGED`、`RELEASED` 或 `DEPLOYED`。

## 6. 禁止事项

- 禁止把提交 PR 路由到 `mango-release`。
- 禁止提交或 Push `main` 代替 PR。
- 禁止混入其它任务、其它 worktree 或用户未授权文件。
- 禁止为通过检查而关闭 required check、缩小 workflow 触发范围、修改基线或删除失败测试。
- 禁止仅凭 Commit/Push/PR 命令退出码声称远端提交成功。
- 禁止在 PR 提交 Skill 内自动 Review、Approve、Merge、发布或部署。
